package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.options.DeterminisitcTerminationOption;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class OptionsExample {

	public static void main(String[] args) {

		//testOptions();
		qLearning();

	}


	public static void testOptions(){

		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();

		Option swToNorth = createRoomOption("swToNorth", domain, 1, 5, 0, 0, 4, 4);
		Option swToEast = createRoomOption("swToEast", domain, 5, 1, 0, 0, 4, 4);

		Option seToWest = createRoomOption("seToWest", domain, 5, 1, 6, 0, 10, 3);
		Option seToNorth = createRoomOption("seToNorth", domain, 8, 4, 6, 0, 10, 3);

		Option neToSouth = createRoomOption("neToSouth", domain, 8, 4, 6, 5, 10, 10);
		Option neToWest = createRoomOption("neToWest", domain, 5, 8, 6, 5, 10, 10);

		Option nwToEast = createRoomOption("nwToEast", domain, 5, 8, 0, 6, 4, 10);
		Option nwToSouth = createRoomOption("nwToSouth", domain, 1, 5, 0, 6, 4, 10);

		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();

		episodes.add(optionExecuteResult(swToNorth, GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0)));
		episodes.add(optionExecuteResult(swToEast, GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0)));

		episodes.add(optionExecuteResult(seToWest, GridWorldDomain.getOneAgentNoLocationState(domain, 10, 0)));
		episodes.add(optionExecuteResult(seToNorth, GridWorldDomain.getOneAgentNoLocationState(domain, 10, 0)));

		episodes.add(optionExecuteResult(neToSouth, GridWorldDomain.getOneAgentNoLocationState(domain, 10, 10)));
		episodes.add(optionExecuteResult(neToWest, GridWorldDomain.getOneAgentNoLocationState(domain, 10, 10)));

		episodes.add(optionExecuteResult(nwToEast, GridWorldDomain.getOneAgentNoLocationState(domain, 0, 10)));
		episodes.add(optionExecuteResult(nwToSouth, GridWorldDomain.getOneAgentNoLocationState(domain, 0, 10)));

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, episodes);


	}

	public static EpisodeAnalysis optionExecuteResult(Option o, State s){
		o.performAction(s, "");
		return o.getLastExecutionResults();
	}

	public static void qLearning(){

		//set up four rooms learning problem with the goal in the most north-east cell (10,10) and initial
		//state in the most south-west cell (0,0).
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);

		//pessimistic Q-learning with 0.1 learning rate and implicit default 0.1 epsilon greedy policy.
		QLearning ql = new QLearning(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), -99, 1.0);


		//create and add options to Q-learning's set of abilities; change the below boolean values to
		//affect whether options are added and whether the detrimental north-east room options are added
		boolean addOptions = true;
		boolean addNorthEasyOptions = false;
		if(addOptions) {
			ql.addNonDomainReferencedAction(createRoomOption("swToNorth", domain, 1, 5, 0, 0, 4, 4));
			ql.addNonDomainReferencedAction(createRoomOption("swToEast", domain, 5, 1, 0, 0, 4, 4));

			ql.addNonDomainReferencedAction(createRoomOption("seToWest", domain, 5, 1, 6, 0, 10, 3));
			ql.addNonDomainReferencedAction(createRoomOption("seToNorth", domain, 8, 4, 6, 0, 10, 3));

			if(addNorthEasyOptions) {
				ql.addNonDomainReferencedAction(createRoomOption("neToSouth", domain, 8, 4, 6, 5, 10, 10));
				ql.addNonDomainReferencedAction(createRoomOption("neToWest", domain, 5, 8, 6, 5, 10, 10));
			}

			ql.addNonDomainReferencedAction(createRoomOption("nwToEast", domain, 5, 8, 0, 6, 4, 10));
			ql.addNonDomainReferencedAction(createRoomOption("nwToSouth", domain, 1, 5, 0, 6, 4, 10));
		}


		//run 100 learning episodes, report the number of steps taken, and save them
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		int totalSteps = 0;
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s);
			episodes.add(ea);
			System.out.println(i + ": " + ea.maxTimeStep());
			totalSteps += ea.maxTimeStep();
		}
		System.out.println("Total steps: " + totalSteps);

		//visualize the learning episodes
		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, episodes);
	}

	/**
	 * Method for creating a four rooms option
	 * @param optionName the name of the option
	 * @param domain the burlap domain to which it is associated
	 * @param doorx the x position of the doorway the option will go to
	 * @param doory the y position of the doorway the option will go to
	 * @param minX the minimum x value of the room
	 * @param minY the minimum y value of the room
	 * @param maxX the maximum x value of the room
	 * @param maxY the maximum y value of the room
	 * @return an option take the agent anywhere within the specified room to the designated doorway
	 */
	public static Option createRoomOption(String optionName, final Domain domain, final int doorx, final int doory, final int minX, final int minY, final int maxX, final int maxY){


		//initiation conditions for options are anywhere in the defined room region
		final StateConditionTest initiationConditions = new StateConditionTest() {
			@Override
			public boolean satisfies(State s) {
				ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
				int x = agent.getIntValForAttribute(GridWorldDomain.ATTX);
				int y = agent.getIntValForAttribute(GridWorldDomain.ATTY);

				return x >= minX && x <= maxX && y>= minY && y <= maxY;
			}
		};

		//termination conditions are any states not in the initiation set
		StateConditionTest terminationConditions = new StateConditionTest() {
			@Override
			public boolean satisfies(State s) {
				return !initiationConditions.satisfies(s);
			}
		};

		//a goal condition so we can use a planning algorithm to generate the option policy
		StateConditionTest goalCondition = new StateConditionTest() {
			@Override
			public boolean satisfies(State s) {
				ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
				int x = agent.getIntValForAttribute(GridWorldDomain.ATTX);
				int y = agent.getIntValForAttribute(GridWorldDomain.ATTY);
				return x == doorx && y == doory;
			}
		};


		//for simplicity of the demonstration of using options, I will compute an option's policy using a planning algorithm
		//if you're trying to solve an RL problem, in practice you wouldn't be able to do this since
		//you assume that the transition dynamics are unknown to the agent.
		//BFS is sufficient for generating the policy to navigate to a hallway when grid world is deterministic
		BFS bfs = new BFS(domain, goalCondition, new DiscreteStateHashFactory());
		bfs.toggleDebugPrinting(false);

		//using a dynamic deterministic planner policy allows BFS to be lazily called to compute the policy of each state in the room
		//BFS will also automatically cache the solution for states it's already seen.
		Policy optionPolicy = new DDPlannerPolicy(bfs);

		//now that we have the parts of our option, instantiate it
		DeterminisitcTerminationOption option = new DeterminisitcTerminationOption(optionName, optionPolicy, initiationConditions, terminationConditions);

		return option;
	}


}
