package tests.rmaxvtest;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learning.modellearning.artdp.ARTDP;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class ARTDPTest {

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

		List<State> allStates = StateReachability.getReachableStates(s, (SADomain) domain, hashingFactory);


		ARTDP agent = new ARTDP(domain, rf, tf, 0.99, hashingFactory, 1.0);

		agent.setPolicy(new BoltzmannQPolicy(0.1));

		for(int i = 0; i < 20; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(s);
			System.out.println(i + ": " + ea.numTimeSteps());
		}



		for(int i = 20; i < 40; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(s);
			System.out.println(i + ": " + ea.numTimeSteps());
		}


		//ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, agent, new GreedyQPolicy(agent));
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, agent, new BoltzmannQPolicy(agent, 0.01));
		gui.initGUI();

	}

}
