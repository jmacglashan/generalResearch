package tests.irltests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learnbydemo.apprenticeship.ApprenticeshipLearning;
import burlap.behavior.singleagent.learnbydemo.apprenticeship.ApprenticeshipLearningRequest;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.RandomStartStateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.visualizer.Visualizer;

public class IRLGridWorldDemo {

	MacroGridWorld 				irlgw;
	Domain						domain;
	StateParser 				sp;
	RewardFunction 				rf;
	TerminalFunction			tf;
	State 						initialState;
	DiscreteStateHashFactory	hashingFactory;
	PropositionalFunction[]		featureFunctions;
	Map<String, Double>			rewardMap;
	static double				GAMMA = .99;
	RandomStartStateGenerator	startStateGenerator;

	public IRLGridWorldDemo() {

		irlgw = new MacroGridWorld(); //create an 11x11 grid world
		domain = irlgw.generateDomain();
		sp = new GridWorldStateParser(domain); //for writing states to a file


		//set up the initial state
		initialState = MacroGridWorld.getOneAgentState(domain);
		MacroGridWorld.setAgent(initialState, 0,0);
		this.startStateGenerator = new RandomStartStateGenerator((SADomain)domain, initialState);

		//rf = new IRLGridRF(irlgw.getMacroCellRewards(initialState));

		tf = new IRLGridTF();

		//set up the state hashing system
		//this class will compute a hash value based on the discrete values of the attributes of objects
		hashingFactory = new DiscreteStateHashFactory();

		//in particular, tell the hashing function to compute hash codes with respect to the attributes of the agent class only
		//when computing hash values this will ignore the attributes of the location objects. since location objects cannot be moved
		//by any action, there is no reason to include the in the computation for our task.
		//if the below line was not included, the hashingFactory would use every attribute of every object class
		hashingFactory.setAttributesForClass(MacroGridWorld.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);
	}


	/**
	 * launch an interactive visualizer of our domain/task
	 */
	public List<EpisodeAnalysis> interactive(){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, irlgw.getMap());
		VisualExplorerRecorder exp = new VisualExplorerRecorder(domain, v, initialState);

		exp.addKeyAction("w", MacroGridWorld.ACTIONNORTH);
		exp.addKeyAction("s", MacroGridWorld.ACTIONSOUTH);
		exp.addKeyAction("d", MacroGridWorld.ACTIONEAST);
		exp.addKeyAction("a", MacroGridWorld.ACTIONWEST);

		List<EpisodeAnalysis> recordedEpisodes = new ArrayList<EpisodeAnalysis>();
		exp.initGUIAndRecord(recordedEpisodes);
		return recordedEpisodes;
	}


	/**
	 * launch an episode viewer for episodes saved to files .
	 * @param outputPath the path to the directory containing the saved episode files
	 */
	public void visualizeEpisodeWithFeatures(String outputPath){
		MacroGridWorld.InMacroCellPF[] macroCellFunctions = new MacroGridWorld.InMacroCellPF[this.featureFunctions.length];
		for (int i =0; i < this.featureFunctions.length; i++) {
			macroCellFunctions[i] = (MacroGridWorld.InMacroCellPF)this.featureFunctions[i];
		}


		Visualizer v = MacroCellVisualizer.getVisualizer(domain, irlgw.getMap(), macroCellFunctions, this.rewardMap);
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}

	public void visualizeEpisode(String outputPath) {
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, irlgw.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}


	/**
	 * This will method will perform VI planning and save a sample of the policy.
	 * @param outputPath the path to the directory in which the policy sample will be saved
	 * 
	 */
	public void runALviaIRLRandomlyGeneratedEpisodes(String outputPath){

		//for consistency make sure the path ends with a '/'
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}

		this.featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		this.rewardMap = MacroGridWorld.generateRandomRewards(featureFunctions, MacroGridWorld.MCELL_FILLED);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, this.rewardMap);
		rf = randomReward;

		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		

		//run planner from our initial state
		planner.planFromState(initialState);

		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);

		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < 10; ++i) {
			EpisodeAnalysis episode = p.evaluateBehavior(this.startStateGenerator.generateState(), randomReward, tf,100);
			episodes.add(episode);
		}

		this.runALviaIRL(outputPath, planner, featureFunctions, episodes, randomReward);
	}

	/**
	 * This will method will perform VI planning and save a sample of the policy.
	 * @param outputPath the path to the directory in which the policy sample will be saved
	 */
	public void runALviaIRLWithEpisodes(String outputPath, List<EpisodeAnalysis> expertEpisodes){

		//for consistency make sure the path ends with a '/'
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}

		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions, MacroGridWorld.MCELL_FILLED);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);

		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		

		//run planner from our initial state
		planner.planFromState(initialState);

		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);

		this.runALviaIRL(outputPath, planner, featureFunctions, expertEpisodes, randomReward);
	}

	/**
	 * This method runs an example of the ALviaIRL algorithm on grid world 
	 * 
	 * @param outputPath
	 * @param planner
	 * @param featureFunctions
	 * @param expertEpisodes
	 * @param randomReward
	 */

	public void runALviaIRL(String outputPath, ValueIteration planner, PropositionalFunction[] featureFunctions, List<EpisodeAnalysis> expertEpisodes, RewardFunction randomReward) {
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		int index = 0;
		for (EpisodeAnalysis episode : expertEpisodes) {
			episode.writeToFile(outputPath + "expert" + index++, sp);
		}

		long start = System.currentTimeMillis();
		//Policy policy = ApprenticeshipLearning.maxMarginMethod(this.domain, planner, featureFunctions, expertEpisodes, 0.9, 0.01, 100);
		//EpisodeAnalysis resultEpisode = policy.evaluateBehavior(initialState, randomReward, tf, 100);
		//resultEpisode.writeToFile(outputPath + "MaxMargin", sp);
		long end = System.currentTimeMillis();
		System.out.println("Time to complete: " + (end - start)/1000F);


		start = System.currentTimeMillis();

		StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
		ApprenticeshipLearningRequest request = 
				new ApprenticeshipLearningRequest(this.domain, planner, featureFunctions, expertEpisodes, startStateGenerator);
		//request.setUsingMaxMargin(true);
		request.setPolicyCount(60);
		request.setMaxIterations(40);
		Policy projectionPolicy = ApprenticeshipLearning.getLearnedPolicy(request);
		//Policy projectionPolicy = ApprenticeshipLearning.projectionMethod(this.domain, planner, featureFunctions, expertEpisodes, 0.99, 0.01, 100);

		for (int i = 0; i < 10; i++) {
			EpisodeAnalysis projectionEpisode = projectionPolicy.evaluateBehavior(startStateGenerator.generateState(), randomReward, tf, 100);
			projectionEpisode.writeToFile(outputPath + "Projection" + i, sp);
		}
		end = System.currentTimeMillis();
		System.out.println("Time to complete projection: " + (end - start)/1000F);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IRLGridWorldDemo tester = new IRLGridWorldDemo();
		String outputPath = "output_macro"; //directory to record results


		tester.runALviaIRLRandomlyGeneratedEpisodes(outputPath);
		//tester.runALviaIRLWithEpisodes(outputPath, tester.interactive()); //performs planning and save a policy sample in outputPath

		/*
		Policy policy = new ExpertPolicy(20, 20, tester.domain.getAction(GridWorldDomain.ACTIONNORTH),
				tester.domain.getAction(GridWorldDomain.ACTIONSOUTH),
				tester.domain.getAction(GridWorldDomain.ACTIONWEST),
				tester.domain.getAction(GridWorldDomain.ACTIONEAST));
		RandomStartStateGenerator stateGenerator = 
				new RandomStartStateGenerator((SADomain)tester.domain, tester.initialState);
		
		for (int i = 0; i < 10; ++i) {
			State state = stateGenerator.generateState();
			EpisodeAnalysis epAnalysis = policy.evaluateBehavior(state, new UniformCostRF(), 50);
			epAnalysis.writeToFile(outputPath + "/random" + i, tester.sp);
		}
		*/
		//tester.visualizeEpisode(outputPath);
		tester.visualizeEpisodeWithFeatures(outputPath); //visualizers the policy sample

	}

	class IRLGridTF implements TerminalFunction{	
		public IRLGridTF(){ }
		@Override
		public boolean isTerminal(State s) {
			return false;
		}
	}
	
}
