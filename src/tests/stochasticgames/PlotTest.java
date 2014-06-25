package tests.stochasticgames;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQFactory;
import burlap.behavior.stochasticgame.auxiliary.performance.AgentFactoryAndType;
import burlap.behavior.stochasticgame.auxiliary.performance.MultiAgentExperimenter;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGame.GGJointRewardFunction;
import burlap.domain.stochasticgames.gridgame.GridGame.GGTerminalFunction;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;
import burlap.oomdp.stochasticgames.tournament.common.ConstantWorldGenerator;

public class PlotTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
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
		
		TerminalFunction tf = new GGTerminalFunction(domain);
		
		//create a factory for Q-learning, since we're going to make both of our agents a Q-learning agent with the same algorithm parameters
		//(alternatively, we could have just used the Q-learning constructor twice for each agent)
		AgentFactory af = new SGNaiveQFactory(domain, discount, learningRate, defaultQ, hashingFactory);
		
		//make a single agent type that can use all actions and refers to the agent class of grid game that we will use for both our agents
		AgentType at = new AgentType("default", domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		
		
		ConstantWorldGenerator wg = new ConstantWorldGenerator(domain, new GridGameStandardMechanics(domain), new GGJointRewardFunction(domain), tf, new ConstantSGStateGenerator(s));
		
		MultiAgentExperimenter exp = new MultiAgentExperimenter(wg, tf, 5, 1000, new AgentFactoryAndType(af, at), new AgentFactoryAndType(af, at));
		
		exp.startExperiment();

	}

}
