package tests.stochasticgames;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.GameSequenceVisualizer;
import burlap.behavior.stochasticgame.PolicyFromJointPolicy;
import burlap.behavior.stochasticgame.agents.maql.MultiAgentQLearning;
import burlap.behavior.stochasticgame.agents.mavf.MultiAgentVFPlanningAgent;
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


/**
 * @author James MacGlashan.
 */
public class ExampleSG {

	//grid game domain generator
	protected GridGame domainGen;

	//stochastic games domain
	protected SGDomain domain;

	//joint action model we want to use for grid game
	protected JointActionModel jam;

	//the type of agent for both agents (specifies their object class defining their personal state and their action set)
	protected AgentType at;

	//discrete state hashing factory for state look up and equality checks
	protected DiscreteStateHashFactory hashingFactory;

	//the joint reward function defining the objective
	protected JointReward rf;

	//a terminal function for defining end states of the game
	protected TerminalFunction tf;

	//stochastic game discount factor
	protected double discount = 0.99;

	//initial state of the game
	protected State initialStste;

	//a world of agents that will interact in a grid world
	protected World w;

	//Grid Game visualizer so we can watch examples of the policy
	Visualizer v;


	public static void main(String[] args) {

		ExampleSG esg = new ExampleSG();

		esg.MAValueIteration();
		//esg.MAQLearning();

	}



	public ExampleSG(){

		this.domainGen = new GridGame();
		this.domain = (SGDomain)domainGen.generateDomain(); //type cast output to stochastic games domain
		this.jam = new GridGameStandardMechanics(this.domain);
		this.rf = new GridGame.GGJointRewardFunction(this.domain);
		this.tf = new GridGame.GGTerminalFunction(this.domain);

		//set the grid game agents will play to turkey
		this.initialStste = GridGame.getTurkeyInitialState(this.domain);
		//this.initialStste = GridGame.getPrisonersDilemmaInitialState(this.domain);

		this.hashingFactory = new DiscreteStateHashFactory();

		//both agents are the same type in grid games with the same action set available
		this.at = new AgentType(GridGame.CLASSAGENT, domain.getObjectClass(GridGame.CLASSAGENT), domain.getSingleActions());

		this.w = new World(domain, new GridGameStandardMechanics(domain), this.rf, new GridGame.GGTerminalFunction(this.domain),
				new ConstantSGStateGenerator(this.initialStste));


		this.v = GGVisualizer.getVisualizer(9, 9);

	}



	public void MAValueIteration(){

		//create a multi-agent planning algorithm for the CoCo-Q solution concept (promotes cooperation)
		//initialize value function to zero and stop planning after either 50 iterations or until the
		//the max change in the value function is less than 0.00015
		MAValueIteration vi = new MAValueIteration(domain, jam, rf, tf, this.discount, hashingFactory, 0., new CoCoQ(), 0.00015, 50);


		//create agents that will follow the solved CoCo-Q values. CoCo-Q agents should follow a maximum welfare policy.
		//Therefore, set up a joint policy for maximum welfare and set the local policy for each agent to be derived
		//from it. Don't break ties randomly so that we get consistency between the agent's actions.
		EGreedyMaxWellfare ja0 = new EGreedyMaxWellfare(0.0);
		EGreedyMaxWellfare ja1 = new EGreedyMaxWellfare(0.0);
		ja0.setBreakTiesRandomly(false);
		ja1.setBreakTiesRandomly(false);

		MultiAgentVFPlanningAgent a0 = new MultiAgentVFPlanningAgent(domain, vi, new PolicyFromJointPolicy(ja0));
		MultiAgentVFPlanningAgent a1 = new MultiAgentVFPlanningAgent(domain, vi, new PolicyFromJointPolicy(ja0));

		//have our CoCo-Q agents join the world
		a0.joinWorld(this.w, this.at);
		a1.joinWorld(this.w, this.at);

		//if we want to watch our agents visually, then we can set up a visual world observer that delays actions
		//for 1 second
		VisualWorldObserver wob = new VisualWorldObserver(domain, v);
		wob.setFrameDelay(1000);
		wob.initGUI();
		this.w.addWorldObserver(wob);

		//continually restart the game after it ends so we can keep watching until we quit.
		//adda  delay
		while(true){

			//update our visualizer to the start state of the game
			v.updateState(this.initialStste);

			//sleep for one second so we can observe the start state
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			//run the game.
			w.runGame();

		}


	}


	public void MAQLearning(){

		//set parameters for our learning agents
		//we will use a semi-optimistic q-value initialization which we will then couple with an epsilon-greedy
		//policy to promote exploration, but not be overly conservative which takes a long time to learn
		double learningRate = 0.1;
		double defaultQ = 1.0;
		double epsilon = 0.1;

		//create CoCo-Q (promotes cooperation) learning agents
		MultiAgentQLearning a0 = new MultiAgentQLearning(this.domain, this.discount, learningRate,
				this.hashingFactory, defaultQ, new CoCoQ(), true);
		MultiAgentQLearning a1 = new MultiAgentQLearning(this.domain, this.discount, learningRate,
				this.hashingFactory, defaultQ, new CoCoQ(), true);

		//have them joint the world
		a0.joinWorld(this.w, this.at);
		a1.joinWorld(this.w, this.at);


		//set their policies to be a epsilon greedy maxwelfare (which CoCo-Q uses) policy over the joint actions
		//with ties broken randomly
		EGreedyMaxWellfare ja0 = new EGreedyMaxWellfare(a0, epsilon);
		EGreedyMaxWellfare ja1 = new EGreedyMaxWellfare(a1, epsilon);
		ja0.setBreakTiesRandomly(true);
		ja1.setBreakTiesRandomly(true);

		a0.setLearningPolicy(new PolicyFromJointPolicy(a0.getAgentName(), ja0));
		a1.setLearningPolicy(new PolicyFromJointPolicy(a1.getAgentName(), ja1));


		//we're going to run a lot of learning episodes, so lets disable the world debug console print outs
		DPrint.toggleCode(w.getDebugId(), false);


		//run 2500 learning episodes. We'll let these run without using our visualizer for speed.
		//However, we'll save each learning episode to disk so we can review them afterwards.

		//first create a standard state parser for saving data to disk
		StateParser sp = new StateYAMLParser(this.domain);

		//now run training; games will be saved in a directory
		//that will automatically be created called "sgTests"
		System.out.println("Starting learning");
		int ngames = 2500;
		for(int i = 0; i < ngames; i++){

			GameAnalysis ga = w.runGame();
			if(i % 10 == 0){
				System.out.println("Game: " + i + ": " + ga.numTimeSteps());
			}
			ga.writeToFile(String.format("sgTests/%4d", i), sp);
		}

		System.out.println("Finished learning");

		//now load the game sequence visualizer so we can review what happened during learning
		new GameSequenceVisualizer(v, domain, sp, "sgTests/");


	}


}
