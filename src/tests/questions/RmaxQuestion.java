package tests.questions;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModelPlanner;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.learning.modellearning.modelplanners.VIModelPlanner;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class RmaxQuestion {

	public static void main(String[] args) {


		//set up domain and problem
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();

		int gx = 10;
		int gy = 10;
		RewardFunction rf = new WallRF(gx, gy);
		//RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(gx, gy);

		State initialState = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		//set up agent
		PotentialShapedRMax rmax = new PotentialShapedRMax(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), new PotentialShapedRMax.RMaxPotential(1000), 1, 0.01, 100);

		RandomFactory.seedMapped(0, 100);

		//run agent for 40 learning episodes
		for(int i = 0; i < 4; i++){

			EpisodeAnalysis ea = rmax.runLearningEpisodeFrom(initialState, 5000);
			//average reward is undiscounted cumulative reward divided by number of actions (num time steps -1)
			double avgReward = ea.getDiscountedReturn(1.) / (ea.numTimeSteps() -1);
			System.out.println(avgReward + " average reward for episode " + (i+1));

			ValueFunctionPlanner planner = ((VIModelPlanner)rmax.getModelPlanner()).getValueIterationPlanner().getCopyOfValueFunction();
			//List<State> allStates = StateReachability.getReachableStates(initialState, (SADomain)domain, new DiscreteStateHashFactory());
			List <State> knownStates = filterRMaxAndUnknownStateTransitions(planner.getAllStates(), rmax.getModel());
			ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(knownStates, planner, new GreedyQPolicy(planner));
			gui.initGUI();
		}


		/*
		//only use the below after updating BURLAP to visualize modeled value function

		//get the model planner. You can use the commented out one below, but if you plan on visualizing the value function
		//at multiple stages, you should make a copy of the value function (using the second line)
		//VIModelPlanner planner = (VIModelPlanner)rmax.getModelPlanner();
		ValueFunctionPlanner planner = ((VIModelPlanner)rmax.getModelPlanner()).getValueIterationPlanner().getCopyOfValueFunction();

		//create a value function visualizer
		List<State> allStates = StateReachability.getReachableStates(initialState, (SADomain)domain, new DiscreteStateHashFactory());
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, planner, new GreedyQPolicy(planner));
		gui.initGUI();
		*/

	}

	public static List<State> filterRMax(List<State> states){
		List<State> filtered = new ArrayList<State>(states.size());
		for(State s : states){
			if(s.getFirstObjectOfClass(ModeledDomainGenerator.RMAXFICTIOUSSTATENAME) == null){
				filtered.add(s);
			}
		}
		return filtered;
	}

	public static List<State> filterRMaxAndUnknownStateTransitions(List<State> states, Model model){
		List<State> filtered = new ArrayList<State>(states.size());
		for(State s : states){
			if(s.getFirstObjectOfClass(ModeledDomainGenerator.RMAXFICTIOUSSTATENAME) != null){
				continue;
			}
			if(!model.stateTransitionsAreModeled(s)){
				continue;
			}

			filtered.add(s);
		}
		return filtered;
	}


	/**
	 * Define a reward function where the agent gets +1000 for reaching a goal location, -100 for running into a wall,
	 * and -1 for all other transitions.
	 */
	public static class WallRF implements RewardFunction{

		int gx;
		int gy;

		public WallRF(int gx, int gy){
			this.gx = gx;
			this.gy = gy;
		}


		@Override
		public double reward(State s, GroundedAction a, State sprime) {

			ObjectInstance nagent = sprime.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int nx = nagent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ny = nagent.getIntValForAttribute(GridWorldDomain.ATTY);

			//did agent reach goal location?
			if(nx == this.gx && ny == this.gy){
				return 1000.;
			}

			ObjectInstance pagent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int px = pagent.getIntValForAttribute(GridWorldDomain.ATTX);
			int py = pagent.getIntValForAttribute(GridWorldDomain.ATTY);

			//if agent didn't change position, they must have hit a wall
			if(px == nx && py == ny){
				return -100.;
			}

			return -1.;
		}
	}

}
