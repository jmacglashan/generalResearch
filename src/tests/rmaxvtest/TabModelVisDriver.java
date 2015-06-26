package tests.rmaxvtest;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learning.modellearning.ModelPlanner;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.learning.modellearning.modelplanners.VIModelPlanner;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.singleagent.shaping.potential.PotentialFunction;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class TabModelVisDriver {

	public static void main(String [] args){

		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();

		Domain domain = gwd.generateDomain();

		//RewardFunction rf = new UniformCostRF();
		GridWorldRewardFunction rf = new GridWorldRewardFunction(11, 11, 0.);
		rf.setReward(10, 10, 1.);
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);

		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		StateHashFactory hashingFactory = new DiscreteStateHashFactory();

		List<State> allStates = StateReachability.getReachableStates(s, (SADomain)domain, hashingFactory);

		TabModelVis model = new TabModelVis(domain, hashingFactory, 1, allStates);
		//PotentialFunction potential = new PotentialShapedRMax.RMaxPotential(1, 0.99);
		PotentialFunction potential = new PotentialShapedRMax.RMaxPotential(0, 0.99);
		VIPlannerGenerator modelGen = new VIPlannerGenerator(hashingFactory);


		//PotentialShapedRMax rmax = new PotentialShapedRMax(domain, rf, tf, 0.99, hashingFactory, potential, model, modelGen);
		PotentialShapedRMax rmax = new PotentialShapedRMax(domain, rf, tf, 0.99, hashingFactory, 1., 1, 0.01, 100);


		PotentialShapedRMaxRFLocal shapedRF = new PotentialShapedRMaxRFLocal(model.getModelRF(), potential, 0.99);
		PotentialShapedRMaxTerminalLocal shapedTF = new PotentialShapedRMaxTerminalLocal(model.getModelTF());


		ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, model, true);
		Domain modeledDomain = mdg.generateDomain();

		//model.launchVisualizer();
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = rmax.runLearningEpisodeFrom(s);
			System.out.println(i + ": " + ea.numTimeSteps());
			//System.out.println(ea.getActionSequenceString(" "));
			//model.launchVisualizer();
		}
		//model.launchVisualizer();
		//launchVisualizer(modeledDomain, allStates, shapedRF, shapedTF, hashingFactory, s);



	}

	public static void launchVisualizer(Domain domain, List<State> allStates, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory, State is){
		ValueIteration planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.01, 100);
		planner.planFromState(is);

		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, planner, new GreedyQPolicy(planner));
		gui.initGUI();
	}


	public static class VIPlannerGenerator implements ModelPlanner.ModelPlannerGenerator{


		protected StateHashFactory hashingFactory;

		public VIPlannerGenerator(StateHashFactory hashingFactory){
			this.hashingFactory = hashingFactory;
		}

		@Override
		public ModelPlanner getModelPlanner(Domain modelDomain, RewardFunction modeledRewardFunction, TerminalFunction modeledTerminalFunction, double discount) {
			return new VIModelPlanner(modelDomain, modeledRewardFunction, modeledTerminalFunction, 0.99, hashingFactory, 0.01, 100);
		}
	}


	/**
	 * This class is a special version of a potential shaped reward function that does not remove the potential value for transitions to states with uknown action transitions
	 * that are followed. This is accomplished by returning a value of zero when the fictious RMax state is recached, rather than subtracting off the previous
	 * states potential.
	 * @author James MacGlashan
	 *
	 */
	protected static class PotentialShapedRMaxRFLocal implements RewardFunction{

		/**
		 * The source reward function
		 */
		protected RewardFunction sourceRF;

		/**
		 * The state potential function
		 */
		protected PotentialFunction potential;


		protected double gamma;


		/**
		 * Initializes.
		 * @param sourceRF the source reward function to which the potential is added.
		 * @param potential the state potential function
		 */
		public PotentialShapedRMaxRFLocal(RewardFunction sourceRF, PotentialFunction potential, double gamma){
			this.sourceRF = sourceRF;
			this.potential = potential;
			this.gamma = gamma;
		}

		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			if(ModeledDomainGenerator.isRmaxFictitiousState(sprime)){
				return 0.; //transitions to fictitious state end potential bonus, but also do not remove potential of previous unknown state
			}

			return this.sourceRF.reward(s, a, sprime)
					+ (this.gamma * this.potential.potentialValue(sprime)) - this.potential.potentialValue(s);

		}



	}


	/**
	 * A Terminal function that treats transitions to RMax fictious nodes as terminal states as well as what the model reports as terminal states.
	 * @author James MacGlashan
	 *
	 */
	public static class PotentialShapedRMaxTerminalLocal implements TerminalFunction{

		/**
		 * The modeled terminal function
		 */
		TerminalFunction sourceModelTF;


		/**
		 * Initializes with a modeled terminal function
		 * @param sourceModelTF the model terminal function.
		 */
		public PotentialShapedRMaxTerminalLocal(TerminalFunction sourceModelTF){
			this.sourceModelTF = sourceModelTF;
		}

		@Override
		public boolean isTerminal(State s) {

			//RMaxStates are terminal states
			if(s.getObjectsOfClass(ModeledDomainGenerator.RMAXFICTIOUSSTATENAME).size() > 0){
				return true;
			}

			return this.sourceModelTF.isTerminal(s);
		}


	}


}
