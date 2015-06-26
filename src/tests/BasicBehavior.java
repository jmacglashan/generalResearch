package tests;

import java.awt.Color;
import java.util.List;

import burlap.behavior.learningrate.SoftTimeInverseDecayLR;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D.PolicyGlyphRenderStyle;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.actorcritic.ActorCritic;
import burlap.behavior.singleagent.learning.actorcritic.actor.BoltzmannActor;
import burlap.behavior.singleagent.learning.actorcritic.critics.TDLambda;
import burlap.behavior.singleagent.learning.actorcritic.critics.TimeIndexedTDLambda;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.informed.Heuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.singleagent.planning.deterministic.uninformed.dfs.DFS;
import burlap.behavior.singleagent.planning.stochastic.policyiteration.PolicyIteration;
import burlap.behavior.singleagent.planning.stochastic.rtdp.RTDP;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;
//import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
//import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;



public class BasicBehavior {

	
	
	GridWorldDomain 			gwdg;
	SADomain					domain;
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
		
		
		BasicBehavior example = new BasicBehavior();
		String outputPath = "output"; //directory to record results
		
		
		//uncomment the example you want to see (and comment-out the rest)
		
		example.QLearningExample(outputPath);
		//example.SarsaLearningExample(outputPath);
		//example.BFSExample(outputPath);
		//example.DFSExample(outputPath);
		//example.AStarExample(outputPath);
		//example.ValueIterationExample(outputPath);
		//example.RTDPExample(outputPath);
		//example.ACLearningExample(outputPath);
		//example.PIExample(outputPath);
		//example.RMaxExample(outputPath);
		
		//run the visualizer
		//example.visualize(outputPath);

		
		//example.valueIterationAndVisualizeValueFunction();
		//example.qLearningAndVisualizeValueFunction();
		
		//example.experimenterAndPlotter();
		
	}
	
	
	public BasicBehavior(){
		
		//gwdg = new GridWorldDomain(15, 15);
		gwdg = new GridWorldDomain(11, 11);
		//gwdg.setProbSucceedTransitionDynamics(0.8);
		gwdg.setMapToFourRooms(); //will use the standard four rooms layout
		domain = (SADomain)gwdg.generateDomain();
		sp = new GridWorldStateParser(domain); //for writing states to a file
		
		rf = new UniformCostRF(); //reward always returns -1 (no positive reward on goal state either; but since the goal state ends action it will still be favored)
		//rf = new SingleGoalPFRF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
		tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION)); //ends when the agent reaches a location
		goalCondition = new TFGoalCondition(tf); //create a goal condition that is synonymous with the termination criteria; this is used with deterministic planners
		
		//set up the initial state
		//initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
		initialState = GridWorldDomain.getOneAgentNLocationState(domain, 1);
		GridWorldDomain.setAgent(initialState, 0, 0);
		//GridWorldDomain.setAgent(initialState, 10, 5);
		GridWorldDomain.setLocation(initialState, 0, 10, 10);
		//GridWorldDomain.setLocation(initialState, 1, 10, 7, 1);
		//GridWorldDomain.setLocation(initialState, 0, 0, 0);
		//GridWorldDomain.setLocation(initialState, 0, 14, 14);
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList); //optional code line; uses only the agent position to perform hash calculations instead of the agent and all locations
		
		
		System.out.println(this.sp.stateToString(initialState));
		
	}
	
	
	public void visualize(String outputPath){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	
	
	////////////////////////////////////////////BEGIN BEAHVIOR EXAMPLES/////////////////////////////////////////////////////////
	
	public void QLearningExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//VisualActionObserver observer = new VisualActionObserver(domain, GridWorldVisualizer.getVisualizer(domain, gwdg.getMap()));
		//this.domain.addActionObserverForAllAction(observer);
		//observer.initGUI();
				
		//creating the learning algorithm object; discount= 0.99; initialQ=0.0; learning rate=0.9
		QLearning agent = new QLearning(domain, rf, tf, 0.99, hashingFactory, 0.3, 0.9);
		agent.setLearningRateFunction(new SoftTimeInverseDecayLR(1., 100, hashingFactory, false));
		
		
		//run learning for 100 episodes
		for(int i = 0; i < 200; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	public void RMaxExample(String outputPath){
		/*
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		PotentialFunction potential = new PotentialFunction() {
			
			@Override
			public double potentialValue(State s) {
				
				if(s.getObjectsOfClass(ModeledDomainGenerator.RMAXFICTIOUSSTATENAME).size() > 0){
					return 0.;
				}
				
				ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0); //assume one agent
				ObjectInstance location = s.getObjectsOfClass(GridWorldDomain.CLASSLOCATION).get(0); //assume one goal location in state
				
				//get agent position
				int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
				int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
				
				//get location position
				int lx = location.getIntValForAttribute(GridWorldDomain.ATTX);
				int ly = location.getIntValForAttribute(GridWorldDomain.ATTY);
				
				//compute Manhattan distance
				double mdist = Math.abs(ax-lx) + Math.abs(ay-ly);
				
				return -mdist; //return the negative value since we use reward functions and negative reward is equivalent to cost
			}
		};
		
		LearningAgent agent = new PotentialShapedRMax(domain, rf, tf, 0.99, hashingFactory, 0., 5, 0.01, 100);
		//LearningAgent agent = new PotentialShapedRMax(domain, rf, tf, 0.99, hashingFactory, potential, 5, 0.01, 100);
		
		//run learning for 100 episodes
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		*/
		
	}
	
	
	public void SarsaLearningExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//VisualActionObserver observer = new VisualActionObserver(domain, GridWorldVisualizer.getVisualizer(domain, gwdg.getMap()));
		//this.domain.addActionObserverForAllAction(observer);
		//observer.initGUI();
		
		//creating the learning algorithm object; discount= 0.99; initialQ=0.0; learning rate=0.5; lambda=1.0 (online Monte carlo at 1.0, one step at 0.0)
		SarsaLam agent = new SarsaLam(domain, rf, tf, 0.99, hashingFactory, -50, 0.5, 1.0);
		agent.setLearningRateFunction(new SoftTimeInverseDecayLR(0.5, 100, hashingFactory, false));
		
		//run learning for 100 episodes
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	public void ACLearningExample(String outputPath){
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		int maxEpisodeSize = 100;
		
		//gamma = 0.99, learning rate = 0.5, vinit = 0.; lambda = 0.9 
		//TDLambda td = new TDLambda(rf, tf, 0.99, hashingFactory, 0.5, 0., 0.9);
		TDLambda td = new TimeIndexedTDLambda(rf, tf, 0.99, hashingFactory, 0.5, 0., 1.0, maxEpisodeSize);
		BoltzmannActor ba = new BoltzmannActor(domain, hashingFactory, 0.3);
		ActorCritic agent = new ActorCritic(domain, rf, tf, 0.99, ba, td, maxEpisodeSize);
		
		for(int i = 0; i < 500; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	public void BFSExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//BFS ignores reward; it just searches for a goal condition satisfying state
		DeterministicPlanner planner = new BFS(domain, goalCondition, hashingFactory);
		planner.planFromState(initialState);
		
		//capture the computed plan in a partial policy
		Policy p = new SDPlannerPolicy(planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	
	public void DFSExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//DFS ignores reward; it just searches for a goal condition satisfying state
		DeterministicPlanner planner = new DFS(domain, goalCondition, hashingFactory);
		planner.planFromState(initialState);
		
		//capture the computed plan in a partial policy
		Policy p = new SDPlannerPolicy(planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	
	public void AStarExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//A* will need a heuristic function; lets use the Manhattan distance between the agent an the goal as an example
		Heuristic mdistHeuristic = new Heuristic() {
			
			@Override
			public double h(State s) {
				
				ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0); //assume one agent
				ObjectInstance location = s.getObjectsOfClass(GridWorldDomain.CLASSLOCATION).get(0); //assume one goal location in state
				
				//get agent position
				int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
				int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
				
				//get location position
				int lx = location.getIntValForAttribute(GridWorldDomain.ATTX);
				int ly = location.getIntValForAttribute(GridWorldDomain.ATTY);
				
				//compute Manhattan distance
				double mdist = Math.abs(ax-lx) + Math.abs(ay-ly);
				
				return -mdist; //return the negative value since we use reward functions and negative reward is equivalent to cost
			}
		};
		
		VisualActionObserver observer = new VisualActionObserver(domain, GridWorldVisualizer.getVisualizer(domain, gwdg.getMap()));
		//this.domain.addActionObserverForAllAction(observer);
		observer.initGUI();
		
		//A* will search for a goal condition satisfying state, but also uses the reward function to keep track of the cost of states; A* expects the RF to always return negative values representing the cost
		DeterministicPlanner planner = new AStar(domain, rf, goalCondition, hashingFactory, mdistHeuristic);
		planner.planFromState(initialState);
		
		
		//capture the computed plan in a partial policy
		Policy p = new SDPlannerPolicy(planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	
	public void ValueIterationExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		
		//Value iteration computing for discount=0.99 with stopping criteria either being a maximum change in value less then 0.001 or 100 passes over the state space (which ever comes first)
		ValueIteration planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.001, 100);
		//planner.toggleUseCachedTransitionDynamics(false);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	public void RTDPExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		ValueFunctionInitialization manDist = new ValueFunctionInitialization() {
			
			@Override
			public double value(State s) {
				ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0); //assume one agent
				ObjectInstance location = s.getObjectsOfClass(GridWorldDomain.CLASSLOCATION).get(0); //assume one goal location in state
				
				//get agent position
				int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
				int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
				
				//get location position
				int lx = location.getIntValForAttribute(GridWorldDomain.ATTX);
				int ly = location.getIntValForAttribute(GridWorldDomain.ATTY);
				
				//compute Manhattan distance
				double mdist = Math.abs(ax-lx) + Math.abs(ay-ly);
				
				return -mdist; //return the negative value since we use reward functions and negative reward is equivalent to cost
			}
			
			@Override
			public double qValue(State s, AbstractGroundedAction a) {
				//not defined
				return 0;
			}
		};
		
		VisualActionObserver observer = new VisualActionObserver(domain, GridWorldVisualizer.getVisualizer(domain, gwdg.getMap()));
		//this.domain.addActionObserverForAllAction(observer);
		//observer.initGUI();
		
		RTDP planner = new RTDP(domain, rf, tf, 0.99, hashingFactory, 0, 100, 0.001, 300);
		//RTDP planner = new RTDP(domain, rf, tf, 0.99, hashingFactory, manDist, 100, 0.001, 300);
		//planner.toggleUseCachedTransitionDynamics(false);
		planner.planFromState(initialState);
		
		Policy p = new GreedyQPolicy(planner);
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
		
	}
	
	
	public void PIExample(String outputPath){
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		PolicyIteration planner = new PolicyIteration(domain, rf, tf, 0.99, hashingFactory, 0.001, 0.1, 100, 100);
		planner.planFromState(initialState);
		
		Policy p = new GreedyQPolicy(planner);
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
	}
	
	
	public void valueIterationAndVisualizeValueFunction(){
		
		//Value iteration computing for discount=0.99 with stopping criteria either being a maximum change in value less then 0.001 or 100 passes over the state space (which ever comes first)
		ValueIteration planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.00001, 100);
		planner.planFromState(initialState);
		this.valueFunctionVisualize(planner);
		
		
	}
	
	public void qLearningAndVisualizeValueFunction(){
		
		//creating the learning algorithm object; discount= 0.99; initialQ=0.0; learning rate=0.9
		LearningAgent agent = new QLearning(domain, rf, tf, 0.99, hashingFactory, 0., 0.9);
		
		//run learning for 100 episodes
		for(int i = 0; i < 200; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
		this.valueFunctionVisualize((QComputablePlanner)agent);
		
	}
	
	
	public void valueFunctionVisualize(QComputablePlanner planner){
		//Policy p = new BoltzmannQPolicy(planner, 0.1);
		Policy p = new GreedyQPolicy(planner);
		List <State> allStates = StateReachability.getReachableStates(initialState, (SADomain)domain, hashingFactory);
		LandmarkColorBlendInterpolation rb = new LandmarkColorBlendInterpolation();
		rb.addNextLandMark(0., Color.RED);
		rb.addNextLandMark(1., Color.BLUE);
		
		StateValuePainter2D svp = new StateValuePainter2D(rb);
		svp.setXYAttByObjectClass(GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTX, GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTY);
		
		PolicyGlyphPainter2D spp = new PolicyGlyphPainter2D();
		spp.setXYAttByObjectClass(GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTX, GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTY);
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONNORTH, new ArrowActionGlyph(0));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONSOUTH, new ArrowActionGlyph(1));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONEAST, new ArrowActionGlyph(2));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONWEST, new ArrowActionGlyph(3));
		spp.setRenderStyle(PolicyGlyphRenderStyle.MAXACTIONSOFTTIE);
		//spp.setSoftTieRenderStyleDelta(0.1);
		
		
		ValueFunctionVisualizerGUI gui = new ValueFunctionVisualizerGUI(allStates, svp, planner);
		gui.setSpp(spp);
		gui.setPolicy(p);
		gui.setBgColor(Color.GRAY);
		gui.initGUI();
	}
	
	
	public void experimenterAndPlotter(){
		
		//custom reward function for more interesting results
		final RewardFunction rf = new GoalBasedRF(this.goalCondition, 5., -0.1);
		
		/**
		 * Create factories for Q-learning agent and SARSA agent to compare
		 */
		
		LearningAgentFactory qLearningFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "Q-learning";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new QLearning(domain, rf, tf, 0.99, hashingFactory, 0.3, 0.1);
			}
		};
		
		
		LearningAgentFactory sarsaLearningFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "SARSA";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new SarsaLam(domain, rf, tf, 0.99, hashingFactory, 0.0, 0.1, 1.);
			}
		};
		
		
		/*
		 * Create experiment, run it, and save data to csv
		 */
		
		StateGenerator sg = new ConstantStateGenerator(this.initialState);
		
		
		
		LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter(this.domain, rf, sg, 10, 100, qLearningFactory, sarsaLearningFactory);
		exp.setUpPlottingConfiguration(500, 250, 2, 1000, TrialMode.MOSTRECENTANDAVERAGE, PerformanceMetric.CUMULATIVESTEPSPEREPISODE, PerformanceMetric.AVERAGEEPISODEREWARD);
		
		exp.startExperiment();
		
		exp.writeStepAndEpisodeDataToCSV("expData");
		
		
	}
	
	
	public static class HallwayTerminal implements TerminalFunction{

		@Override
		public boolean isTerminal(State s) {
			
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			if(ax == 1 && ay == 5){
				return true;
			}
			
			return false;
		}
		
		
		
		
	}
	
	
	public class TmpRF implements RewardFunction{

		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			double positiveR = 1.;
			double negativeR = -1.;
			double defaultR = 0.;
			
			List<GroundedProp> possibleInLocationPFs = domain.getPropFunction(GridWorldDomain.PFATLOCATION).getAllGroundedPropsForState(sprime);
			for(GroundedProp gp : possibleInLocationPFs){
				if(gp.isTrue(sprime)){
					//what is the name of the location where the agent is?
					String locName = gp.params[1];
				}
			}
			
			return defaultR;
			
		}
		
	}
	
	
}