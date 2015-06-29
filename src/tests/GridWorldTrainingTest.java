package tests;

import java.util.ArrayList;
import java.util.List;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.learning.MultiVFPassAC;
import behavior.training.DynamicFeedbackGUI;
import behavior.training.TrainerModel;
import behavior.training.act.ActorCriticTrainer;
import behavior.training.act.EarliestStatePropagationUpdate;
import behavior.training.act.SimulatedTrainerRF;
import behavior.training.prl.PMDirectPolicy;
import behavior.training.prl.PartialReinforcementLearning;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskInductionTraining;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.actorcritic.ActorCritic;
import burlap.behavior.singleagent.learning.actorcritic.actor.BoltzmannActor;
import burlap.behavior.singleagent.learning.actorcritic.critics.TDLambda;
import burlap.behavior.singleagent.learning.actorcritic.critics.TimeIndexedTDLambda;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.singleagent.common.SingleGoalPFRF;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.visualizer.Visualizer;

public class GridWorldTrainingTest {

	
	GridWorldDomain 			gwdg;
	Domain						domain;
	StateParser 				sp;
	RewardFunction 				rf;
	TerminalFunction			tf;
	StateConditionTest			goalCondition;
	State 						initialState;
	DiscreteStateHashFactory	hashingFactory;
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		GridWorldTrainingTest test = new GridWorldTrainingTest();
		String outputPath = "output"; //directory to record results
		
		//test.performTraining(outputPath);
		//test.ACLearningExample(outputPath);
		//test.MultiACLearningExample(outputPath);
		//test.prlTraining(outputPath);
		
		//test.visualize(outputPath);

		
		//test.interactivePRLTraining();
		test.interactiveTaskInduceTraining();
		
	}
	
	public GridWorldTrainingTest() {
		
		gwdg = new GridWorldDomain(11, 11);
		gwdg.setMapToFourRooms(); //will use the standard four rooms layout
		domain = gwdg.generateDomain();
		Action noop = new NullAction("noop", domain, ""); //add noop to the domain
		sp = new GridWorldStateParser(domain); //for writing states to a file
		
		int goalX = 10;
		int goalY = 10;
		
		
		tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION)); //ends when the agent reaches a location
		goalCondition = new TFGoalCondition(tf); //create a goal condition that is synonymous with the termination criteria; this is used with deterministic planners
		
		//set up the initial state
		initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
		GridWorldDomain.setAgent(initialState, 0, 0);
		GridWorldDomain.setLocation(initialState, 0, goalX, goalY);
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList); //optional code line; uses only the agent position to perform hash calculations instead of the agent and all locations
		
		
		//this.initializeToTrainerRF(goalX, goalY);
		rf = new SingleGoalPFRF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
		//rf = new UniformCostRF();
		
	}
	
	public void visualize(String outputPath){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	public void prlTraining(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		PMDirectPolicy p = new PMDirectPolicy(domain, hashingFactory, 0.3);
		//PMBoltzmannPolicy p = new PMBoltzmannPolicy(domain, hashingFactory, 0.5);
		PartialReinforcementLearning agent = new PartialReinforcementLearning(domain, rf, tf, 0.99, this.hashingFactory, p);
		
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState);
			ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	public void interactiveTaskInduceTraining(){
		
		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(domain);
		Domain domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		DynamicFeedbackGUI gui = new DynamicFeedbackGUI(v, env);
		env.setGUI(gui);
		
		List <TaskDescription> tasks = this.getFourCornersTasks();
		//List <TaskDescription> tasks = this.getCornerDoorCenterTasks();
		TaskInductionTraining agent = new TaskInductionTraining(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, tasks, new MAPMixtureModelPolicy());
		//set priors
		for(int i = 0; i < tasks.size(); i++){
			agent.setProbFor(i, 1./(double)tasks.size());
		}
		/*
		for(int i = 0; i < 4; i++){
			agent.setProbFor(i, 10./52.);
		}
		for(int i = 4; i < 8; i++){
			agent.setProbFor(i, 2./52.);
		}
		for(int i = 8; i < 12; i++){
			agent.setProbFor(i, 1./52.);
		}
		*/
		
		agent.useSeperatePlanningDomain(domain);
		agent.planPossibleTasksFromSeedState(initialState);
		
		boolean hasInitedGUI = false;
		
		for(int i = 0; i < 20; i++){
			env.setCurStateTo(initialState);
			if(!hasInitedGUI){
				hasInitedGUI = true;
				gui.initGUI();
				gui.launch();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("Starting episode");
			//now start learning episode
			agent.runLearningEpisodeFrom(initialState);
		}
		
		System.out.println("finished training");
		
	}
	
	public void interactivePRLTraining(){
		
		
		
		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(domain);
		Domain domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		DynamicFeedbackGUI gui = new DynamicFeedbackGUI(v, env);
		env.setGUI(gui);
		
		
		PMDirectPolicy p = new PMDirectPolicy(domainEnvWrapper, hashingFactory, 1.);
		PartialReinforcementLearning agent = new PartialReinforcementLearning(domainEnvWrapper, trainerRF, trainerTF, 0.99, this.hashingFactory, p);
		agent.useSeperatePlanningDomain(domain);
		
		boolean hasInitedGUI = false;
		
		for(int i = 0; i < 20; i++){
			env.setCurStateTo(initialState);
			if(!hasInitedGUI){
				hasInitedGUI = true;
				gui.initGUI();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("Starting episode");
			//now start learning episode
			agent.runLearningEpisodeFrom(initialState);
		}
		
		System.out.println("finished training");
		
	}
	
	
	public void performTraining(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		int maxEpisodeSize = 100;
		TimeIndexedTDLambda critic = new TimeIndexedTDLambda(null, tf, 0.99, hashingFactory, 0.5, 0.5, 1., maxEpisodeSize);
		BoltzmannActor ba = new BoltzmannActor(domain, hashingFactory, 0.1);
		//TrainerModel tm = new SimpleUpdate(hashingFactory);
		//TrainerModel tm = new EarliestOccuranceUpdate(hashingFactory);
		TrainerModel tm = new EarliestStatePropagationUpdate(hashingFactory);
		
		ActorCriticTrainer act = new ActorCriticTrainer(domain, rf, tf, 0.99, ba, critic, tm, maxEpisodeSize);
		
		for(int i = 0; i < 5000; i++){
			EpisodeAnalysis ea = act.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	public void ACLearningExample(String outputPath){
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		int maxEpisodeSize = 5000;
		
		//gamma = 0.99, learning rate = 0.5, vinit = 0.; lambda = 0.9 
		//TDLambda td = new TDLambda(rf, tf, 0.99, hashingFactory, 0.5, 0., 0.9);
		TDLambda td = new TimeIndexedTDLambda(rf, tf, 0.99, hashingFactory, 0.5, 0.5, 1.0, maxEpisodeSize);
		BoltzmannActor ba = new BoltzmannActor(domain, hashingFactory, 0.3);
		ActorCritic agent = new ActorCritic(domain, rf, tf, 0.99, ba, td, maxEpisodeSize);
		
		for(int i = 0; i < 500; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			//System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
			System.out.println(ea.numTimeSteps() + ",");
		}
		
	}
	
	
	public void MultiACLearningExample(String outputPath){
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		int maxEpisodeSize = Integer.MAX_VALUE;
		
		//gamma = 0.99, learning rate = 0.5, vinit = 0.; lambda = 0.9 
		//TDLambda td = new TDLambda(rf, tf, 0.99, hashingFactory, 0.5, 0., 0.9);
		TDLambda td = new TimeIndexedTDLambda(rf, tf, 0.99, hashingFactory, 0.5, 0.0, 1.0, maxEpisodeSize);
		BoltzmannActor ba = new BoltzmannActor(domain, hashingFactory, 0.3);
		ActorCritic agent = new MultiVFPassAC(domain, rf, tf, 0.99, ba, td, maxEpisodeSize, 10);
		
		for(int i = 0; i < 500; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			//System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
			System.out.println(ea.numTimeSteps() + ",");
		}
		
	}
	
	
	public Policy getOptimalPolicy(){
		
		RewardFunction objectiveRF = new SingleGoalPFRF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
		
		//Value iteration computing for discount=0.99 with stopping criteria either being a maximum change in value less then 0.001 or 100 passes over the state space (which ever comes first)
		OOMDPPlanner planner = new ValueIteration(domain, objectiveRF, tf, 0.99, hashingFactory, 0.0001, 100);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QFunction)planner);
		
		return p;
		
	}
	
	protected void initializeToTrainerRF(int goalX, int goalY){
		rf = new SimulatedTrainerRF(this.getOptimalPolicy());
		
		((SimulatedTrainerRF) rf).setProbability(SimulatedTrainerRF.FeedbackType.NEGATIVE, SimulatedTrainerRF.FeedbackType.NEGATIVE, 1.0);
		((SimulatedTrainerRF) rf).setProbability(SimulatedTrainerRF.FeedbackType.NEUTRAL, SimulatedTrainerRF.FeedbackType.NEGATIVE, 0.0);
		
		((SimulatedTrainerRF) rf).setProbability(SimulatedTrainerRF.FeedbackType.POSITIVE, SimulatedTrainerRF.FeedbackType.POSITIVE, 0.0);
		((SimulatedTrainerRF) rf).setProbability(SimulatedTrainerRF.FeedbackType.NEUTRAL, SimulatedTrainerRF.FeedbackType.POSITIVE, 1.0);
		
		//((SimulatedTrainerRF) rf).addConditionTest(new GWLTest(1, 5));
		//((SimulatedTrainerRF) rf).addConditionTest(new GWLTest(5, 8));
		//((SimulatedTrainerRF) rf).addConditionTest(new GWLTest(goalX, goalY));
	}
	
	
	protected List <TaskDescription> getCornerDoorCenterTasks(){
		List <TaskDescription> all = new ArrayList<TaskDescription>(12);
		all.addAll(this.getFourCornersTasks());
		all.addAll(this.getFourDoorsTasks());
		all.addAll(this.getCenterTasks());
		return all;
	}
	
	protected List <TaskDescription> getFourCornersTasks(){
		List <TaskDescription> tasks = new ArrayList<TaskDescription>();
		tasks.add(this.taskDescriptionForGridLocation(0, 0));
		tasks.add(this.taskDescriptionForGridLocation(0, 10));
		tasks.add(this.taskDescriptionForGridLocation(10, 0));
		tasks.add(this.taskDescriptionForGridLocation(10, 10));
		
		return tasks;
	}
	
	protected List <TaskDescription> getFourDoorsTasks(){
		List <TaskDescription> tasks = new ArrayList<TaskDescription>();
		tasks.add(this.taskDescriptionForGridLocation(1, 5));
		tasks.add(this.taskDescriptionForGridLocation(5, 1));
		tasks.add(this.taskDescriptionForGridLocation(5, 8));
		tasks.add(this.taskDescriptionForGridLocation(8, 4));
		
		return tasks;
	}
	
	protected List <TaskDescription> getCenterTasks(){
		List <TaskDescription> tasks = new ArrayList<TaskDescription>();
		tasks.add(this.taskDescriptionForGridLocation(2, 2));
		tasks.add(this.taskDescriptionForGridLocation(2, 8));
		tasks.add(this.taskDescriptionForGridLocation(8, 8));
		tasks.add(this.taskDescriptionForGridLocation(8, 1));
		
		return tasks;
	}
	
	protected TaskDescription taskDescriptionForGridLocation(int x, int y){
		final GWLTest cond = new GWLTest(x, y);
		RewardFunction rf = new GoalBasedRF(cond);
		TerminalFunction tf = new TerminalFunction() {
			
			
			@Override
			public boolean isTerminal(State s) {
				return cond.satisfies(s);
			}
		};
		
		return new TaskDescription(rf, tf);
		
	}
	
	
	class GWLTest implements StateConditionTest{

		protected int x;
		protected int y;
		
		public GWLTest(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public boolean satisfies(State s) {
			
			ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0);
			
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			if(ax == x && ay == y){
				return true;
			}
			
			return false;
		}
		
		
		
		
	}
	

}
