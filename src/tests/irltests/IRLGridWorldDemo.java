package tests.irltests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learnbydemo.apprenticeship.ApprenticeshipLearning;
import burlap.behavior.singleagent.learnbydemo.apprenticeship.ApprenticeshipLearningRequest;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.PFFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.domain.singleagent.gridworld.macro.MacroCellGridWorld;
import burlap.domain.singleagent.gridworld.macro.MacroCellGridWorld.LinearInPFRewardFunction;
import burlap.domain.singleagent.gridworld.macro.MacroCellVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.RandomStartStateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;


public class IRLGridWorldDemo {

	public static final int 	MCELL_FILLED = 5;
	
	MacroCellGridWorld 				irlgw;
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

		
		irlgw = new MacroCellGridWorld(); //create an 11x11 grid world
		domain = irlgw.generateDomain();
		sp = new GridWorldStateParser(domain); //for writing states to a file

		this.featureFunctions = MacroCellGridWorld.getMacroCellPropositionalFunctions(this.domain, this.irlgw);
		this.rewardMap = MacroCellGridWorld.generateRandomRewardsMap(featureFunctions);
		
		
		//set up the initial state
		initialState = MacroCellGridWorld.getOneAgentNoLocationState(domain);
		MacroCellGridWorld.setAgent(initialState, 0,0);
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
		hashingFactory.setAttributesForClass(MacroCellGridWorld.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);
		
	}


	/**
	 * launch an interactive visualizer of our domain/task
	 */
	public List<EpisodeAnalysis> interactive(){
		Visualizer v = GridWorldVisualizer.getVisualizer(irlgw.getMap());
		VisualExplorerRecorder exp = new VisualExplorerRecorder(domain, v, initialState);

		exp.addKeyAction("w", MacroCellGridWorld.ACTIONNORTH);
		exp.addKeyAction("s", MacroCellGridWorld.ACTIONSOUTH);
		exp.addKeyAction("d", MacroCellGridWorld.ACTIONEAST);
		exp.addKeyAction("a", MacroCellGridWorld.ACTIONWEST);

		List<EpisodeAnalysis> recordedEpisodes = new ArrayList<EpisodeAnalysis>();
		exp.initGUIAndRecord(recordedEpisodes);
		return recordedEpisodes;
	}
	
	public void testNewInteractive(){
		Visualizer v = GridWorldVisualizer.getVisualizer(irlgw.getMap());
		
		final VisualExplorer exp = new VisualExplorer(domain, v, initialState);
		
		exp.addKeyAction("w", MacroCellGridWorld.ACTIONNORTH);
		exp.addKeyAction("s", MacroCellGridWorld.ACTIONSOUTH);
		exp.addKeyAction("d", MacroCellGridWorld.ACTIONEAST);
		exp.addKeyAction("a", MacroCellGridWorld.ACTIONWEST);
		
		exp.enableEpisodeRecording("r", "f");
		exp.initGUI();
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				while(exp.isRecording()){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		});
		
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<EpisodeAnalysis> episodes = exp.getRecordedEpisodes();
		
		System.out.println(episodes.size());
		
		for(int i = 0; i < episodes.size(); i++){
			episodes.get(i).writeToFile("myTestRecorder/" + i, this.sp);
		}
		
		Visualizer v2 = GridWorldVisualizer.getVisualizer(irlgw.getMap());
		
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v2, domain, sp, "myTestRecorder");
		
	}


	/**
	 * launch an episode viewer for episodes saved to files .
	 * @param outputPath the path to the directory containing the saved episode files
	 */
	public void visualizeEpisodeWithFeatures(String outputPath){
		
		MacroCellGridWorld.InMacroCellPF[] macroCellFunctions = new MacroCellGridWorld.InMacroCellPF[this.featureFunctions.length];
		for (int i =0; i < this.featureFunctions.length; i++) {
			macroCellFunctions[i] = (MacroCellGridWorld.InMacroCellPF)this.featureFunctions[i];
		}


		Visualizer v = MacroCellVisualizer.getVisualizer(irlgw.getMap(), macroCellFunctions, this.rewardMap);
		new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}

	public void visualizeEpisode(String outputPath) {
		Visualizer v = GridWorldVisualizer.getVisualizer(irlgw.getMap());
		new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
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

		
		StateToFeatureVectorGenerator featureFunction = new PFFeatureVectorGenerator(featureFunctions);
		RewardFunction randomReward = new LinearInPFRewardFunction(featureFunctions, this.rewardMap);
		rf = randomReward;

		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 25);		

		
		//run planner from our initial state
		planner.planFromState(initialState);

		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QFunction)planner);

		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < 10; ++i) {
			EpisodeAnalysis episode = p.evaluateBehavior(this.startStateGenerator.generateState(), randomReward, tf,100);
			episodes.add(episode);
		}

		this.runALviaIRL(outputPath, planner, featureFunction, episodes, randomReward);
		
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

		PropositionalFunction[] featureFunctions = MacroCellGridWorld.getMacroCellPropositionalFunctions(this.domain, this.irlgw);
		StateToFeatureVectorGenerator featureFunction = new PFFeatureVectorGenerator(featureFunctions);
		Map<String, Double> rewards = MacroCellGridWorld.generateRandomRewardsMap(featureFunctions);
		RewardFunction randomReward = new LinearInPFRewardFunction(featureFunctions, rewards);

		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		

		//run planner from our initial state
		planner.planFromState(initialState);


		this.runALviaIRL(outputPath, planner, featureFunction, expertEpisodes, randomReward);
		
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

	public void runALviaIRL(String outputPath, ValueIteration planner, StateToFeatureVectorGenerator featureFunctions, List<EpisodeAnalysis> expertEpisodes, RewardFunction randomReward) {
		
		
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
		request.setMaxIterations(3);
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
		
		tester.testNewInteractive();
		
		
		/*String outputPath = "output_macro"; //directory to record results


		tester.runALviaIRLRandomlyGeneratedEpisodes(outputPath);*/
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
		//tester.visualizeEpisodeWithFeatures(outputPath); //visualizers the policy sample
		

	}

	class IRLGridTF implements TerminalFunction{	
		public IRLGridTF(){ }
		@Override
		public boolean isTerminal(State s) {
			return false;
		}
	}
	
	
	
	
	
	
	
	
}
