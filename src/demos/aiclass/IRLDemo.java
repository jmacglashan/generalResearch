package demos.aiclass;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.differentiableplanners.DifferentiableSparseSampling;
import burlap.behavior.singleagent.learnbydemo.mlirl.differentiableplanners.DifferentiableVI;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
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
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class IRLDemo {

	GridWorldDomain gwd;
	Domain domain;
	StateGenerator sg;
	Visualizer v;



	public IRLDemo(){
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

		//create reward function features to use
		LocationFV fvg = new LocationFV(this.domain, 5);

		//create a reward function that is linear with respect to those features
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvg, 5);

		//load our saved demonstrations from disk
		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList("irlDemo", domain, new GridWorldStateParser(this.domain));

		//using differentiable value iteration as the planner
		double beta = 10.;
		DifferentiableVI dplanner = new DifferentiableVI(this.domain, rf, new NullTermination(), 0.99, 8, new DiscreteStateHashFactory(), 0.01, 100);

		//alternatively to differentiable vi, use differentiable sparse sampling, which performs receding horizon IRL (comment the above line and uncomment the below)
		//DifferentiableSparseSampling dplanner = new DifferentiableSparseSampling(this.domain, rf, new NullTermination(), 0.99, new DiscreteStateHashFactory(), 10, -1, beta);


		dplanner.toggleDebugPrinting(false);

		//define the IRL problem
		MLIRLRequest request = new MLIRLRequest(domain, dplanner, episodes, rf);
		request.setBoltzmannBeta(beta);

		//run MLIRL on it
		MLIRL irl = new MLIRL(request, 0.1, 0.1, 10);
		irl.performIRL();


		//get all states in the domain so we can visualize the learned reward function for them
		List<State> allStates = StateReachability.getReachableStates(basicState(), (SADomain) this.domain, new DiscreteStateHashFactory());

		//get a standard grid world value function visualizer, but give it StateRewardFunctionValue which returns the
		//reward value received upon reaching each state which will thereby let us render the reward function that is
		//learned rather than the value function for it.
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(
				allStates,
				new StateRewardFunctionValue(domain, request.getRf()),
				new GreedyQPolicy((QFunction)request.getPlanner()));

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
	public static class LocationFV implements StateToFeatureVectorGenerator {

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
					int lt = l.getIntValForAttribute(GridWorldDomain.ATTLOCTYPE);
					return lt;
				}
			}

			return -1;
		}
	}


	/**
	 * A "planning" algorithm that sets the value of the state to the reward function value. This is useful
	 * for visualizing the learned reward function weights from IRL.
	 */
	public static class StateRewardFunctionValue implements QFunction{

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

		IRLDemo ex = new IRLDemo();

		//only have one of the below uncommented

		ex.launchExplorer(); //choose this to record demonstrations
		//ex.launchSavedEpisodeSequenceVis(); //choose this review the demonstrations that you've recorded
		//ex.runIRL(); //choose this to run MLIRL on the demonstrations and visualize the learned reward function and policy


	}

}
