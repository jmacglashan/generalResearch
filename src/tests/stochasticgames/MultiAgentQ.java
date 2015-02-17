package tests.stochasticgames;

import java.util.List;

import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.GameSequenceVisualizer;
import burlap.behavior.stochasticgame.PolicyFromJointPolicy;
import burlap.behavior.stochasticgame.agents.maql.MultiAgentQLearning;
import burlap.behavior.stochasticgame.agents.mavf.MultiAgentVFPlanningAgent;
import burlap.behavior.stochasticgame.mavaluefunction.AgentQSourceMap;
import burlap.behavior.stochasticgame.mavaluefunction.JAQValue;
import burlap.behavior.stochasticgame.mavaluefunction.MAQSourcePolicy;
import burlap.behavior.stochasticgame.mavaluefunction.QSourceForSingleAgent;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.CoCoQ;
import burlap.behavior.stochasticgame.mavaluefunction.policies.EGreedyMaxWellfare;
import burlap.behavior.stochasticgame.mavaluefunction.vfplanners.MAValueIteration;
import burlap.debugtools.DPrint;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.*;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;
import burlap.oomdp.stochasticgames.common.VisualWorldObserver;
import burlap.oomdp.visualizer.Visualizer;

public class MultiAgentQ {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		VITest();
		//QLearningTest();

	}
	
	
	public static void QLearningTest(){
		//create domain
		GridGame domainGen = new GridGame();
		final SGDomain domain = (SGDomain)domainGen.generateDomain();
		
		//create hashing factory that only hashes on the agent positions (ignores wall attributes)
		final DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTX));
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTY));
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTPN));
		
		//parameters for q-learning
		final double discount = 0.95;
		final double learningRate = 0.1;
		final double defaultQ = 100;
		
		/*
		final State s = GridGame.getCleanState(domain, 2, 3, 3, 2, 5, 5);
		GridGame.setAgent(s, 0, 0, 0, 0);
		GridGame.setAgent(s, 1, 4, 0, 1);
		GridGame.setGoal(s, 0, 0, 4, 1);
		GridGame.setGoal(s, 1, 2, 4, 0);
		GridGame.setGoal(s, 2, 4, 4, 2);
		GridGame.setHorizontalWall(s, 2, 4, 1, 3, 0);
		*/
		final State s = GridGame.getTurkeyInitialState(domain);
		
		JointReward rf = new GridGame.GGJointRewardFunction(domain, -1, 100, false);
		
		//create our world
		World w = new World(domain, new GridGameStandardMechanics(domain), rf, new GridGame.GGTerminalFunction(domain), 
				new ConstantSGStateGenerator(s));
		
		Visualizer v = GGVisualizer.getVisualizer(9, 9);
		VisualWorldObserver wob = new VisualWorldObserver(domain, v);
		wob.setFrameDelay(1000);
		//wob.initGUI();
		
		
		//make a single agent type that can use all actions and refers to the agent class of grid game that we will use for both our agents
		AgentType at = new AgentType("default", domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		
		/*
		MultiAgentQLearning a0 = new MultiAgentQLearning(domain, discount, learningRate, hashingFactory, defaultQ, new MaxBackup(), true);
		MultiAgentQLearning a1 = new MultiAgentQLearning(domain, discount, learningRate, hashingFactory, defaultQ, new MaxBackup(), true);
		*/
		
		MultiAgentQLearning a0 = new MultiAgentQLearning(domain, discount, learningRate, hashingFactory, defaultQ, new CoCoQ(), true);
		MultiAgentQLearning a1 = new MultiAgentQLearning(domain, discount, learningRate, hashingFactory, defaultQ, new CoCoQ(), true);
		
		/*
		SetStrategyAgent a1 = new SetStrategyAgent(domain, new Policy() {
			
			@Override
			public boolean isStochastic() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isDefinedFor(State s) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public List<ActionProb> getActionDistributionForState(State s) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public AbstractGroundedAction getAction(State s) {
				GroundedSingleAction gsas[] = new GroundedSingleAction[]{new GroundedSingleAction("me", domain.getSingleAction(GridGame.ACTIONNOOP), ""),
																		 new GroundedSingleAction("me", domain.getSingleAction(GridGame.ACTIONNORTH), ""),
																		 new GroundedSingleAction("me", domain.getSingleAction(GridGame.ACTIONSOUTH), ""),
																		 new GroundedSingleAction("me", domain.getSingleAction(GridGame.ACTIONEAST), ""),
																		 new GroundedSingleAction("me", domain.getSingleAction(GridGame.ACTIONWEST), "")};
				return gsas[RandomFactory.getMapped(0).nextInt(5)];
				//return gsas[0];
			}
		});*/
		
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		
		//don't have the world print out debug info (comment out if you want to see it!)
		DPrint.toggleCode(w.getDebugId(), false);
		
		StateParser sp = new StateYAMLParser(domain);
		
		
		System.out.println("Starting training");
		int ngames = 2500;
		for(int i = 0; i < ngames; i++){
			if(i % 10 == 0){
				System.out.println("Game: " + i);
			}
			GameAnalysis ga = w.runGame();
			ga.writeToFile(String.format("sgTests/%4d", i), sp);
		}
		
		System.out.println("Finished training");
		
		
		GameSequenceVisualizer gvis = new GameSequenceVisualizer(v, domain, sp, "sgTests/");
		
		/*
		v.updateState(s);
		w.addWorldObserver(wob);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//turn debug back on if we want to observe the behavior of agents after they have already learned how to behave
		DPrint.toggleCode(w.getDebugId(), true);
		
		a0.setLearningPolicy(new PolicyFromJointPolicy(a0.getAgentName(), new EGreedyMaxWellfare(a0, 0.0)));
		a1.setLearningPolicy(new PolicyFromJointPolicy(a1.getAgentName(), new EGreedyMaxWellfare(a0, 0.0)));
		
		//run game to observe behavior
		w.runGame();
		*/
	}
	
	
	
	public static void VITest(){
		
		//create domain
		GridGame domainGen = new GridGame();
		final SGDomain domain = (SGDomain)domainGen.generateDomain();
		
		//create hashing factory that only hashes on the agent positions (ignores wall attributes)
		final DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTX));
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTY));
		hashingFactory.addAttributeForClass(GridGame.CLASSAGENT, domain.getAttribute(GridGame.ATTPN));
		
		final State s = GridGame.getTurkeyInitialState(domain);
		//final State s = GridGame.getPrisonersDilemmaInitialState(domain);
		
		JointReward rf = new GridGame.GGJointRewardFunction(domain, -1, 100, false);
		TerminalFunction tf = new GridGame.GGTerminalFunction(domain);
		JointActionModel jam = new GridGameStandardMechanics(domain);
		
		//make a single agent type that can use all actions and refers to the agent class of grid game that we will use for both our agents
		AgentType at = new AgentType(GridGame.CLASSAGENT, domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());
		
		MAValueIteration vi = new MAValueIteration(domain, jam, rf, tf, 0.99, hashingFactory, 0., new CoCoQ(), 0.00015, 50);
		//MAValueIteration vi = new MAValueIteration(domain, jam, rf, tf, 0.99, hashingFactory, 0., new CorrelatedQ(CorrelatedEquilibriumObjective.UTILITARIAN), 0.0001, 30);
		
		//create our world
		World w = new World(domain, new GridGameStandardMechanics(domain), rf, new GridGame.GGTerminalFunction(domain), 
				new ConstantSGStateGenerator(s));
		
		Visualizer v = GGVisualizer.getVisualizer(9, 9);
		VisualWorldObserver wob = new VisualWorldObserver(domain, v);
		wob.setFrameDelay(100);
		wob.initGUI();
		
		
		
		EGreedyMaxWellfare jp0 = new EGreedyMaxWellfare(0.0);
		jp0.setBreakTiesRandomly(false);
		
		EGreedyMaxWellfare jp1 = new EGreedyMaxWellfare(0.0);
		jp1.setBreakTiesRandomly(false);
		
		
		
		//ECorrelatedQJointPolicy jp0 = new ECorrelatedQJointPolicy(0.0);
		//ECorrelatedQJointPolicy jp1 = new ECorrelatedQJointPolicy(0.0);
		
		
		MultiAgentVFPlanningAgent a0 = new MultiAgentVFPlanningAgent(domain, vi, new PolicyFromJointPolicy(jp0));
		MultiAgentVFPlanningAgent a1 = new MultiAgentVFPlanningAgent(domain, vi, new PolicyFromJointPolicy(jp1));
		
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		w.addWorldObserver(wob);
		
		EGreedyMaxWellfare jp = new EGreedyMaxWellfare(0.0);
		jp.setAgentsInJointPolicyFromWorld(w);
		jp.setQSourceProvider(vi);
		jp.setBreakTiesRandomly(false);
		
		DPrint.toggleCode(w.getDebugId(), false);

		GameAnalysis ga = null;
		for(int i = 0; i < 1; i++){
			v.updateState(s);
			if(i > 0){
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			ga = w.runGame();
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		AgentQSourceMap sources = vi.getQSources();
		QSourceForSingleAgent a0Qs = sources.agentQSource(a0.getAgentName());
		/*List<ActionProb> aps = jp.getActionDistributionForState(s);
		for(ActionProb ap : aps){
			JAQValue q = a0Qs.getQValueFor(s, (JointAction)ap.ga);
			System.out.println(ap.ga.toString() + "  :  " + q.q);
		}*/


		double maxQ = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < ga.numTimeSteps(); i++){
			State ts = ga.getState(i);
			System.out.println(i + "\n-------------");
			List<JointAction> jas = JointAction.getAllJointActions(ts, w.getRegisteredAgents());
			for(JointAction ja : jas){
				JAQValue q = a0Qs.getQValueFor(s,  ja);
				System.out.println(ja.toString() + "  :  " + q.q);
				maxQ = Math.max(maxQ, q.q);
			}
			System.out.println();
		}

		System.out.println("Max Q in all states: " + maxQ);

		/*
		List<ActionProb> aps = jp.getActionDistributionForState(s);
		
		for(ActionProb ap : aps){
			System.out.println(ap.pSelection + ": " + ap.ga.toString());
		}
		*/
		
	}

}
