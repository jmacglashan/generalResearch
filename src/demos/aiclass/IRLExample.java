package demos.aiclass;

import behavior.burlapirlext.DifferentiableSparseSampling;
import behavior.burlapirlext.NMLIRL;
import behavior.burlapirlext.diffvinit.LinearStateDiffVF;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.differentiableplanners.DifferentiableVI;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	/**
	 * Creates a visual explorer that you can use to to record trajectories. Use the "`" key to reset to a random initial state
	 * Use the wasd keys to move north south, east, and west, respectively. To record the last trajectory since the "`" key was pressed
	 * (or the program launched) press the "r". To actually save all recorded trajectories to disk, press the "f" key.
	 */
	public void launchExplorer(){
		VisualExplorer exp = new VisualExplorer(this.domain, this.v, this.sg, 800, 800);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);

		StateParser sp = new GridWorldStateParser(this.domain);
		exp.enableEpisodeRecording("r", "f", new NullRewardFunction(), "irlDemo", sp);

		exp.initGUI();
	}


	/**
	 * Launch a episode sequence visualizer to display the saved trajectories in the folder "irlDemo"
	 */
	public void launchSavedEpisodeSequenceVis(){

		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(this.v, this.domain, new GridWorldStateParser(this.domain), "irlDemo");

	}

	/**
	 * Runs MLIRL on the trajectories stored in the "irlDemo" directory and then visualizes the learned reward function.
	 */
	public void runIRL(){

		LocationFV fvg = new LocationFV(this.domain, 5);
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvg, 5);

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList("irlDemo", domain, new GridWorldStateParser(this.domain));

		//differentiable sparse sampling is not yet in BURLAP proper
		double beta = 10.;
		//double beta = 10.;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(this.domain, rf, new NullTermination(), 1., new DiscreteStateHashFactory(), 8, -1, beta);
		dss.toggleDebugPrinting(false);
		//DifferentiableVI dvi = new DifferentiableVI(this.domain, rf, new NullTermination(), 0.99, 8, new DiscreteStateHashFactory(), 0.01, 100);


		MLIRLRequest request = new MLIRLRequest(domain, dss, episodes, rf);
		request.setBoltzmannBeta(beta);
		//MLIRLRequest request = new MLIRLRequest(domain, episodes, rf, new DiscreteStateHashFactory());
		//request.setBoltzmannBeta(20);

		MLIRL irl = new MLIRL(request, 0.1, 0.1, 10);
		irl.performIRL();


		List<State> allStates = StateReachability.getReachableStates(basicState(), (SADomain)this.domain, new DiscreteStateHashFactory());

		//ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, (QComputablePlanner)request.getPlanner(), new GreedyQPolicy((QComputablePlanner)request.getPlanner()));
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(
				allStates,
				new StateRewardFunctionValue(domain, request.getRf()),
				new GreedyQPolicy((QComputablePlanner)request.getPlanner()));

		gui.initGUI();


	}

	/**
	 * Runs MLIRL on the trajectories stored in the "irlDemo" directory and then visualizes the learned reward function on a novel environment.
	 */
	public void runIRLGeneralize(){

		LocationFV fvg = new LocationFV(this.domain, 5);
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvg, 5);

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList("irlDemo", domain, new GridWorldStateParser(this.domain));

		//differentiable sparse sampling is not yet in BURLAP proper
		double beta = 10.;
		//double beta = 10.;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(this.domain, rf, new NullTermination(), 1., new DiscreteStateHashFactory(), 8, -1, beta);
		dss.toggleDebugPrinting(false);
		//DifferentiableVI dvi = new DifferentiableVI(this.domain, rf, new NullTermination(), 0.99, 8, new DiscreteStateHashFactory(), 0.01, 100);


		MLIRLRequest request = new MLIRLRequest(domain, dss, episodes, rf);
		request.setBoltzmannBeta(beta);
		//MLIRLRequest request = new MLIRLRequest(domain, episodes, rf, new DiscreteStateHashFactory());
		//request.setBoltzmannBeta(20);

		MLIRL irl = new MLIRL(request, 0.1, 0.1, 10);
		irl.performIRL();

		VisualExplorer exp = new VisualExplorer(this.domain, this.v, this.transferState());
		exp.initGUI();

		ValueIteration vi = new ValueIteration(this.domain, request.getRf(), new NullTermination(), 0.9, new DiscreteStateHashFactory(), 0.0001, 100);
		vi.planFromState(transferState());

		List<State> allStates = StateReachability.getReachableStates(transferState(), (SADomain)this.domain, new DiscreteStateHashFactory());

		//ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, (QComputablePlanner)request.getPlanner(), new GreedyQPolicy((QComputablePlanner)request.getPlanner()));
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(
				allStates,
				new StateRewardFunctionValue(domain, request.getRf()),
				//vi,
				new GreedyQPolicy(vi));

		gui.initGUI();


	}


	/**
	 * Runs MLIRL on the trajectories stored in the "irlDemo" directory and then visualizes the learned reward function.
	 */
	public void runNIRL(){

		LocationFV fvg = new LocationFV(this.domain, 5);
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvg, 5);
		rf.randomizeParameters(-2, 2., new Random());

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList("irlDemo", domain, new GridWorldStateParser(this.domain));

		//differentiable sparse sampling is not yet in BURLAP proper
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(this.domain, rf, new NullTermination(), 1., new DiscreteStateHashFactory(), 8, -1, 0.5);
		dss.toggleDebugPrinting(false);
		//DifferentiableVI dvi = new DifferentiableVI(this.domain, rf, new NullTermination(), 0.99, 8, new DiscreteStateHashFactory(), 0.01, 100);


		MLIRLRequest request = new MLIRLRequest(domain, dss, episodes, rf);
		request.setBoltzmannBeta(0.5);
		//MLIRLRequest request = new MLIRLRequest(domain, episodes, rf, new DiscreteStateHashFactory());
		//request.setBoltzmannBeta(20);

		NMLIRL irl = new NMLIRL(request, 0.0001, 0.0, 30);
		irl.performIRL();


		List<State> allStates = StateReachability.getReachableStates(basicState(), (SADomain)this.domain, new DiscreteStateHashFactory());

		//ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, (QComputablePlanner)request.getPlanner(), new GreedyQPolicy((QComputablePlanner)request.getPlanner()));
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(
				allStates,
				new StateRewardFunctionValue(domain, request.getRf()),
				new GreedyQPolicy((QComputablePlanner)request.getPlanner()));

		gui.initGUI();


	}



	/**
	 * Creates a grid world state with the agent in (0,0) and various different grid cell types scattered about.
	 * @return a grid world state with the agent in (0,0) and various different grid cell types scattered about.
	 */
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

	protected State transferState(){

		State s = GridWorldDomain.getOneAgentNLocationState(this.domain, 8);
		GridWorldDomain.setAgent(s, 0, 0);

		GridWorldDomain.setLocation(s, 0, 3, 4, 1);
		GridWorldDomain.setLocation(s, 1, 2, 4, 2);
		GridWorldDomain.setLocation(s, 2, 1, 4, 3);
		GridWorldDomain.setLocation(s, 3, 0, 4, 4);

		GridWorldDomain.setLocation(s, 4, 0, 1, 0);
		GridWorldDomain.setLocation(s, 5, 1, 1, 0);
		GridWorldDomain.setLocation(s, 6, 2, 1, 0);
		GridWorldDomain.setLocation(s, 7, 3, 1, 0);

		return s;

	}


	/**
	 * State generator that produces initial agent states somewhere on the left side of the grid.
	 */
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


	/**
	 * A state feature vector generator that create a binary feature vector where each element
	 * indicates whether the agent is in a cell of of a different type. All zeros indicates
	 * that the agent is in an empty cell.
	 */
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


	public static class MacroCellFV implements StateToFeatureVectorGenerator{

		int gridWidth;
		int gridHeight;
		int macroCellWidth;
		int macroCellHeight;

		public MacroCellFV(int gridWidth, int gridHeight, int macroCellWidth, int macroCellHeight) {
			this.gridWidth = gridWidth;
			this.gridHeight = gridHeight;
			this.macroCellWidth = macroCellWidth;
			this.macroCellHeight = macroCellHeight;
		}

		@Override
		public double[] generateFeatureVectorFrom(State s) {

			double [] fv = new double[this.gridWidth*this.gridHeight];

			//agents position in the world
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int x = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int y = agent.getDiscValForAttribute(GridWorldDomain.ATTY);

			//figure out in which macro cell that x-y and is in
			int macroX = x / this.macroCellWidth;
			int macroY = y / this.macroCellHeight;

			//what index in our feature vector array is this macro cell?
			int index = macroX + (macroY * this.gridWidth);
			fv[index] = 1.;

			return fv;
		}
	}


	/**
	 * A "planning" algorithm that sets the value of the state to the reward function value. This is useeful
	 * for visualizing the learned reward function weights from IRL.
	 */
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
		//ex.runIRL();
		ex.runIRLGeneralize();
		//ex.runNIRL();

	}

}
