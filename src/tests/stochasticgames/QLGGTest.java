package tests.stochasticgames;

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.interfacing.singleagent.SALearningAgentFactoryForSG;
import burlap.behavior.stochasticgame.agents.interfacing.singleagent.SingleAgentInterface;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.debugtools.DPrint;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;

public class QLGGTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new QLGGTest();
	}
	
	public QLGGTest(){
		
		//create domain
		GridGame domainGen = new GridGame();
		final SGDomain domain = (SGDomain)domainGen.generateDomain();
		
		//create hashing factory that only hashes on the agent positions (ignores wall attributes)
		final DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTX));
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTY));
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTPN));
		
		//parameters for q-learning
		final double discount = 0.99;
		final double learningRate = 0.1;
		final double defaultQ = 1.;
		
		final State s = GridGame.getCleanState(domain, 2, 3, 3, 2, 5, 5);
		GridGame.setAgent(s, 0, 0, 0, 0);
		GridGame.setAgent(s, 1, 4, 0, 1);
		GridGame.setGoal(s, 0, 0, 3, 1);
		GridGame.setGoal(s, 1, 2, 4, 0);
		GridGame.setGoal(s, 2, 4, 3, 2);
		GridGame.setHorizontalWall(s, 2, 4, 1, 3, 1);
		
		
		//create a factory for Q-learning, since we're going to make both of our agents a Q-learning agent with the same algorithm parameters
		//(alternatively, we could have just used the Q-learning constructor twice for each agent)
		AgentFactory af = new SGQFactory(domain, discount, learningRate, defaultQ, hashingFactory);
		
		
		//create our world
		World w = new World(domain, new GridGameStandardMechanics(domain), new GGJointRewardFunction(domain), new GGTerminalFunction(domain), 
				new ConstantSGStateGenerator(s));
		
		
		//make a single agent type that can use all actions and refers to the agent class of grid game that we will use for both our agents
		AgentType at = new AgentType("default", domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		
		
		SALearningAgentFactoryForSG qlearningFactory = new SALearningAgentFactoryForSG() {
			
			@Override
			public LearningAgent generateAgentForRLDomain(Domain domain,
					RewardFunction rf, TerminalFunction tf) {
				QLearning agent = new QLearning(domain, rf, tf, discount, hashingFactory, defaultQ, learningRate);
				return agent;
			}
		};
		
		
		SingleAgentInterface a0 = new SingleAgentInterface(domain, qlearningFactory);
		SingleAgentInterface a1 = new SingleAgentInterface(domain, qlearningFactory);
		
		
		//generate our agents using our factory
		//Agent a0 = af.generateAgent();
		//Agent a1 = af.generateAgent();
		
		
		//have the agents join the world
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		
		//don't have the world print out debug info (comment out if you want to see it!)
		DPrint.toggleCode(w.getDebugId(), false);
		
		
		System.out.println("Starting training");
		int ngames = 1000;
		for(int i = 0; i < ngames; i++){
			if(i % 10 == 0){
				System.out.println("Game: " + i);
			}
			w.runGame();
		}
		
		System.out.println("Finished training");
		
		
		//turn debug back on if we want to observe the behavior of agents after they have already learned how to behave
		DPrint.toggleCode(w.getDebugId(), true);
		
		//run game to observe behavior
		w.runGame();
		
	}

}
