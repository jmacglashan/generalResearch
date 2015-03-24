package tests.inadmissibleTests;

import behavior.planning.SRTDP;
import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.rtdp.BoundedRTDP;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.singleagent.shaping.potential.PotentialFunction;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class InadmissibleTest {

	GridWorldDomain gwd;
	Domain domain;
	RewardFunction sourceRF = new UniformCostRF();
	TerminalFunction tf;
	State initialState;
	double discount = 1.;
	Visualizer v;
	List<State> allStates;
	StateParser sp;

	public InadmissibleTest(){
		gwd = new GridWorldDomain(10, 10);
		gwd.horizontalWall(1, 7, 5);
		domain = gwd.generateDomain();
		initialState = GridWorldDomain.getOneAgentNoLocationState(domain, 5, 0);
		tf = new GridWorldTerminalFunction(5, 8);
		v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		allStates = StateReachability.getReachableStates(initialState, (SADomain)domain, new DiscreteStateHashFactory());
		sp = new GridWorldStateParser(10, 10);
	}

	public ValueIteration optimalValues(){
		ValueIteration vi = new ValueIteration(domain, sourceRF, tf, discount, new DiscreteStateHashFactory(), 0.001, 200);
		vi.planFromState(initialState);
		return vi;
	}


	public BoundedRTDP runBRTDP(RewardFunction rf, ValueFunctionInitialization vinitLower, ValueFunctionInitialization vinitUpper, String expDir, String baseName){

		BoundedRTDP brtdp = new BoundedRTDP(domain, rf, tf, discount, new DiscreteStateHashFactory(), vinitLower, vinitUpper, 0.01, 1);
		Policy p = new GreedyQPolicy(brtdp);

		String partialPath = expDir + "/" + baseName;

		int lastTotal = 0;
		for(int i = 0; i < 30; i++){
			brtdp.runRollout(initialState);
			int newTotal = brtdp.getNumberOfSteps();
			int lastSize = newTotal - lastTotal;

			String indexFormat = String.format("%02d", i);

			System.out.println("rollout " + indexFormat + ": " + lastSize);

			EpisodeAnalysis curPolicy = p.evaluateBehavior(initialState, rf, tf, 500);
			curPolicy.writeToFile(partialPath + "_" + indexFormat, sp);
			System.out.println("policy " + indexFormat + ": " + (curPolicy.numTimeSteps()-1));
			System.out.println("");

			lastTotal = newTotal;
		}

		new EpisodeSequenceVisualizer(v, domain, sp, expDir);

		return brtdp;


	}

	public SRTDP runSRTDP(RewardFunction rf, ValueFunctionInitialization vinitLower, ValueFunctionInitialization vShaped, ValueFunctionInitialization vinitUpper, String expDir, String baseName){

		SRTDP srtdp = new SRTDP(domain, rf, tf, discount, new DiscreteStateHashFactory(), vinitLower, vShaped, vinitUpper, 0.1, 0.01, 1);

		Policy p = new GreedyQPolicy(srtdp);

		String partialPath = expDir + "/" + baseName;

		int lastTotal = 0;
		for(int i = 0; i < 30; i++){
			srtdp.runRollout(initialState);
			int newTotal = srtdp.getNumberOfSteps();
			int lastSize = newTotal - lastTotal;

			String indexFormat = String.format("%02d", i);

			System.out.println("rollout " + indexFormat + ": " + lastSize);

			EpisodeAnalysis curPolicy = p.evaluateBehavior(initialState, rf, tf, 500);
			curPolicy.writeToFile(partialPath + "_" + indexFormat, sp);
			System.out.println("policy " + indexFormat + ": " + (curPolicy.numTimeSteps()-1));
			System.out.println("");

			lastTotal = newTotal;
		}

		new EpisodeSequenceVisualizer(v, domain, sp, expDir);


		return srtdp;

	}

	public BoundedRTDP queryBRTDP(RewardFunction rf, ValueFunctionInitialization vinitLower, ValueFunctionInitialization vinitUpper){
		BoundedRTDP brtdp = new BoundedRTDP(domain, rf, tf, discount, new DiscreteStateHashFactory(), vinitLower, vinitUpper, 0.01, 1);
		brtdp.setValueFunctionToUpperBound();
		List<QValue> qs = brtdp.getQs(initialState);
		for(QValue q : qs){
			System.out.println(q.a.actionName() + ": " + q.q);
		}
		return brtdp;
	}

	public void visualizerPlanner(ValueFunctionPlanner planner){
		Policy p = new GreedyQPolicy(planner);
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, planner, p);
		gui.initGUI();
	}

	public void runExplorer(){
		VisualExplorer exp = new VisualExplorer(domain, v, initialState);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		exp.initGUI();
	}


	public static class BiasedPotential implements PotentialFunction{

		protected Map<StateHashTuple, Double> value;
		protected StateHashFactory hashingFactory;

		public double min = Double.POSITIVE_INFINITY;
		public double max = Double.NEGATIVE_INFINITY;

		public BiasedPotential(ValueFunctionPlanner values, List<State> allStates, StateHashFactory hashingFactory, TerminalFunction tf, int axSplit, double bias){

			this.hashingFactory = hashingFactory;
			this.value = new HashMap<StateHashTuple, Double>();

			for(State s : allStates){

				if(tf.isTerminal(s)){
					this.value.put(hashingFactory.hashState(s), 0.);
					continue;
				}

				double trueV = values.value(s);
				ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
				int ax = agent.getDiscValForAttribute(GridWorldDomain.ATTX);

				double v = trueV;
				if(ax >= axSplit){
					v += bias;
				}

				if(v < min){
					min = v;
				}
				if(v > max){
					max = v;
				}

				this.value.put(hashingFactory.hashState(s), v);

			}

		}

		@Override
		public double potentialValue(State s) {
			return this.value.get(this.hashingFactory.hashState(s));
		}
	}


	public static class PotentialVInit implements ValueFunctionInitialization{

		protected double offSet = 0.;
		protected PotentialFunction pf;


		public PotentialVInit(PotentialFunction pf){
			this.pf = pf;
		}

		public PotentialVInit(PotentialFunction pf, double offset){
			this.pf = pf;
			this.offSet = offset;
		}


		@Override
		public double value(State s) {
			return this.pf.potentialValue(s) + this.offSet;
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return 0;
		}
	}


	public static void main(String[] args) {
		InadmissibleTest test = new InadmissibleTest();


		//test.runExplorer();
		ValueIteration vi = test.optimalValues();
		//test.visualizerPlanner(vi);

		BiasedPotential pf = new BiasedPotential(vi, test.allStates, new DiscreteStateHashFactory(), test.tf, 5, -5);


		test.runBRTDP(test.sourceRF,
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0),
				"oomdpResearch/inadmissible", "uninformed");




		BoundedRTDP brtdp = null;
		SRTDP srtdp = null;

		/*brtdp = test.runBRTDP(test.sourceRF,
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16 + pf.min),
				new PotentialVInit(pf),
				"oomdpResearch/inadmissible", "inadV");*/




		/*brtdp = test.runBRTDP(new PotentialShapedRF(test.sourceRF, pf, test.discount),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0),
				"oomdpResearch/inadmissible", "inadRS");*/

		//System.out.println("min max: " + (-16 - pf.max) + " " + (0 - pf.min));

		/*brtdp = test.runBRTDP(new PotentialShapedRF(test.sourceRF, pf, test.discount),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16 - pf.max),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0 - pf.min),
				"oomdpResearch/inadmissible", "informed");
		*/

		/*
		srtdp = test.runSRTDP(test.sourceRF,
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16 + pf.min),
				new PotentialVInit(pf),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.),
				"oomdpResearch/inadmissible", "srtdp");
		*/

		//test.visualizerPlanner(brtdp);
		//test.visualizerPlanner(srtdp);

		//new EpisodeSequenceVisualizer(test.v, test.domain, test.sp, "oomdpResearch/inadmissible");

		double shapedMin = -16 - pf.max;
		double shapedMax = 0 - pf.min;

		//System.out.println("min max: " + shapedMin + " " + shapedMax);


		/*
		test.queryBRTDP(new PotentialShapedRF(test.sourceRF, pf, test.discount),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(7000));

		System.out.println("--------");
		*/


		/*
		brtdp = test.queryBRTDP(new PotentialShapedRF(test.sourceRF, pf, test.discount),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-16 - pf.max),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0 - pf.min));

		test.visualizerPlanner(brtdp);

		try {
			Thread.sleep(2000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		brtdp.runRollout(test.initialState);
		System.out.println("steps: " + brtdp.getNumberOfSteps());
		*/
	}


}
