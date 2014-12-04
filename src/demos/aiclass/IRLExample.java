package demos.aiclass;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class IRLExample {

	GridWorldDomain gwd;
	Domain domain;
	StateGenerator sg;
	Visualizer v;



	public IRLExample(){
		this.gwd = new GridWorldDomain(5 ,5);
		this.gwd.setNumberOfLocationTypes(5);
		gwd.makeEmptyMap();
		this.domain = gwd.generateDomain();
		State bs = this.basicState();
		this.sg = new LeftSideGen(5, bs);
		this.v = GridWorldVisualizer.getVisualizer(this.gwd.getMap());

	}

	public void launchExplorer(){
		VisualExplorer exp = new VisualExplorer(this.domain, this.v, this.sg, 800, 800);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);

		StateParser sp = new GridWorldStateParser(this.domain);
		exp.enableEpisodeRecording("r", "f", new NullRewardFunction(), "oomdpResearch/irlDemo", sp);

		exp.initGUI();
	}

	public void launchSavedEpisodeSequenceVis(){

		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(this.v, this.domain, new GridWorldStateParser(this.domain), "oomdpResearch/irlDemo");

	}

	public void runIRL(){

		LocationFV fvg = new LocationFV(this.domain, 5);
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvg, 5);

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList("oomdpResearch/irlDemo", domain, new GridWorldStateParser(this.domain));
		MLIRLRequest request = new MLIRLRequest(domain, episodes, rf, new DiscreteStateHashFactory());
		request.setBoltzmannBeta(20);
		MLIRL irl = new MLIRL(request, 0.1, 0.1, 10);
		irl.performIRL();


		List<State> allStates = StateReachability.getReachableStates(basicState(), (SADomain)this.domain, new DiscreteStateHashFactory());

		//request.getPlanner().setGamma(0.0);
		//request.getPlanner().resetPlannerResults();

		//ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, (QComputablePlanner)request.getPlanner(), new GreedyQPolicy((QComputablePlanner)request.getPlanner()));
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(
				allStates,
				new StateRewardFunctionValue(domain, request.getRf()),
				new GreedyQPolicy((QComputablePlanner)request.getPlanner()));

		gui.initGUI();


	}


	protected State basicState(){

		State s = GridWorldDomain.getOneAgentNLocationState(this.domain, 9);
		GridWorldDomain.setAgent(s, 0, 0);

		//goals
		GridWorldDomain.setLocation(s, 0, 0, 0, 1);
		GridWorldDomain.setLocation(s, 1, 0, 4, 2);
		GridWorldDomain.setLocation(s, 2, 4, 4, 3);
		GridWorldDomain.setLocation(s, 3, 4, 0, 4);

		GridWorldDomain.setLocation(s, 4, 1, 0, 0);
		GridWorldDomain.setLocation(s, 5, 1, 2, 0);
		GridWorldDomain.setLocation(s, 6, 1, 4, 0);

		GridWorldDomain.setLocation(s, 7, 3, 1, 0);
		GridWorldDomain.setLocation(s, 8, 3, 3, 0);

		return s;
	}


	public static class LeftSideGen implements StateGenerator{


		protected int height;
		protected State sourceState;


		public LeftSideGen(int height, State sourceState){
			this.setParams(height, sourceState);
		}

		public void setParams(int height, State sourceState){
			this.height = height;
			this.sourceState = sourceState;
		}


		@Override
		public State generateState() {

			State s = this.sourceState.copy();

			int h = RandomFactory.getDefault().nextInt(this.height);
			GridWorldDomain.setAgent(s, 0, h);

			return s;
		}
	}


	public static class LocationFV implements StateToFeatureVectorGenerator{

		protected int numLocations;
		PropositionalFunction inLocaitonPF;


		public LocationFV(Domain domain, int numLocations){
			this.numLocations = numLocations;
			this.inLocaitonPF = domain.getPropFunction(GridWorldDomain.PFATLOCATION);
		}


		@Override
		public double[] generateFeatureVectorFrom(State s) {

			double [] fv = new double[this.numLocations];

			int aL = this.getActiveLocationVal(s);
			if(aL != -1){
				fv[aL] = 1.;
			}

			return fv;
		}


		protected int getActiveLocationVal(State s){

			List<GroundedProp> gps = this.inLocaitonPF.getAllGroundedPropsForState(s);
			for(GroundedProp gp : gps){
				if(gp.isTrue(s)){
					ObjectInstance l = s.getObject(gp.params[1]);
					int lt = l.getDiscValForAttribute(GridWorldDomain.ATTLOCTYPE);
					return lt;
				}
			}

			return -1;
		}
	}


	public static class StateRewardFunctionValue implements QComputablePlanner{

		protected RewardFunction rf;
		protected Domain domain;

		public StateRewardFunctionValue(Domain domain, RewardFunction rf){
			this.rf = rf;
			this.domain = domain;
		}

		@Override
		public List<QValue> getQs(State s) {

			List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.domain.getActions(), s);
			List<QValue> qs = new ArrayList<QValue>(gas.size());
			for(GroundedAction ga : gas){
				double r = this.rf.reward(s, ga, s);
				qs.add(new QValue(s, ga, r));
			}

			return qs;
		}

		@Override
		public QValue getQ(State s, AbstractGroundedAction a) {

			double r = this.rf.reward(s, (GroundedAction)a, s);
			QValue q = new QValue(s, a, r);

			return q;
		}
	}


	public static void main(String [] args){

		IRLExample ex = new IRLExample();

		//ex.launchExplorer();
		//ex.launchSavedEpisodeSequenceVis();
		ex.runIRL();

		//System.out.println(String.format("%03d", 50));




	}

}
