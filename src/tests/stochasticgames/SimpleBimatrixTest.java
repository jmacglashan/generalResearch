package tests.stochasticgames;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.history.SGQWActionHistoryFactory;
import burlap.domain.stochasticgames.normalform.SingleStageNormalFormGame;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;
import burlap.oomdp.stochasticgames.common.StaticRepeatedGameActionModel;

public class SimpleBimatrixTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		longForm();
		shortForm();
		
		
	}
	
	
	public static void longForm(){
		
		//Payoff matrices in Bimatrix game for chicken
		double [][] chickenRow = new double[][]{
				{0, -1},
				{1, -10}
		};
		
		double [][] chickenCol= new double[][]{
				{0, 1},
				{-1, -10}
		};
		
		SingleStageNormalFormGame game = new SingleStageNormalFormGame(chickenRow, chickenCol);
		SGDomain domain = (SGDomain)game.generateDomain();
		
		//Bonus: chicken is a default setup in BURLAP; the above could have be done without defining the payoff matrices with the lines:
		//SingleStageNormalFormGame game = SingleStageNormalFormGame.getChicken();
		//SGDomain domain = (SGDomain)game.generateDomain();
		
		//action model for repeating single stage games; just returns to the same state
		JointActionModel jam = new StaticRepeatedGameActionModel(); 
		
		//grab the joint reward function from our bimatrix game in the more general BURLAP joint reward function interface
		JointReward jr = game.getJointRewardFunction(); 
		
		//game repeats forever unless manually stopped after T times.
		TerminalFunction tf = new NullTermination();
		
		//set up the initial state generator for the world, which for a bimatrix game is trivial
		SGStateGenerator sg = new ConstantSGStateGenerator(SingleStageNormalFormGame.getState(domain));
		
		//agent type defines the action set of players and OO-MDP class associated with their state information
		//in this case that's just their player number. We can use the same action type for all players, regardless of wether
		//each agent can play a different number of actions, because the actions have preconditions that prevent a player from taking actions
		//that don't belong to them.
		AgentType at = SingleStageNormalFormGame.getAgentTypeForAllPlayers(domain);
		
		
		//create a world to synchronize the actions of agents in this domain and record results
		World w = new World(domain, jam, jr, tf, sg);
		
		//create an N history (where N will = 1) Q-learning agent factor (factories are good if you want to run lots of experiments!)
		//you may want to create your own factor for greater control of generated agent parameters
		//gamma = 0.99, alpha = 0.1
		SGQWActionHistoryFactory agentFactory = new SGQWActionHistoryFactory(domain, 0.99, 0.1, new DiscreteStateHashFactory(), 1);
		agentFactory.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(1.)); //optional Qinit value to something other than 0
		agentFactory.setEpsilon(0.1); //optional epsilon greedy parameter setting
		
		
		//create our agents
		Agent a1 = agentFactory.generateAgent();
		Agent a2 = agentFactory.generateAgent();
		
		//have our agents join the world
		a1.joinWorld(w, at);
		a2.joinWorld(w, at);
		
		//have our world run for 1000 time steps
		w.runGame(1000);
		
		//print final performance (as cumulative reward)
		System.out.println("Agent 1 scored: " + w.getCumulativeRewardForAgent(a1.getAgentName()));
		System.out.println("Agent 2 scored: " + w.getCumulativeRewardForAgent(a2.getAgentName()));
		
	}
	
	
	public static void shortForm(){
		
		//Payoff matrices in Bimatrix game for chicken
		double [][] chickenRow = new double[][]{
				{0, -1},
				{1, -10}
		};
		
		double [][] chickenCol= new double[][]{
				{0, 1},
				{-1, -10}
		};
		
		SingleStageNormalFormGame game = new SingleStageNormalFormGame(chickenRow, chickenCol);
		SGDomain domain = (SGDomain)game.generateDomain();
		
		//Bonus: chicken is a default setup in BURLAP; the above could have be done without defining the payoff matrices with the lines:
		//SingleStageNormalFormGame game = SingleStageNormalFormGame.getChicken();
		//SGDomain domain = (SGDomain)game.generateDomain();
		
		//create an N history (where N will = 1) Q-learning agent factor (factories are good if you want to run lots of experiments!)
		//you may want to create your own factor for greater control of generated agent parameters
		//gamma = 0.99, alpha = 0.1
		SGQWActionHistoryFactory agentFactory = new SGQWActionHistoryFactory(domain, 0.99, 0.1, new DiscreteStateHashFactory(), 1);
		agentFactory.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(1.)); //optional Qinit value to something other than 0
		agentFactory.setEpsilon(0.1); //optional epsilon greedy parameter setting
		
		//create our agents
		Agent a1 = agentFactory.generateAgent();
		Agent a2 = agentFactory.generateAgent();
		
		World w = game.createRepeatedGameWorld(domain, a1, a2);
		
		//have our world run for 1000 time steps
		w.runGame(1000);
		
		//print final performance (as cumulative reward)
		System.out.println("Agent 1 scored: " + w.getCumulativeRewardForAgent(a1.getAgentName()));
		System.out.println("Agent 2 scored: " + w.getCumulativeRewardForAgent(a2.getAgentName()));
		
	}

}
