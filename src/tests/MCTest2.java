package tests;

import burlap.behavior.learningrate.ConstantLR;
import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.learningrate.SoftTimeInverseDecayLR;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase.TilingArrangement;
import burlap.behavior.singleagent.vfa.cmac.FVCMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.fourier.FourierBasis;
import burlap.behavior.singleagent.vfa.fourier.FourierBasisLearningRateWrapper;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBF;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarStateParser;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

public class MCTest2 {

	protected MountainCar			mcGen;
	protected Domain				domain;
	protected RewardFunction		rf;
	protected TerminalFunction		tf;
	protected StateParser			sp;
	protected State					initialState;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MCTest2 mctest = new MCTest2();
		
		String outputPath = "MCTest/";
		
		//mctest.runCMACExperimenter();
		mctest.runCMACVFA(outputPath, false);
		//mctest.fourierTest(outputPath, false);
		mctest.visualizeCachedFiles(outputPath);

	}
	
	
	
	public MCTest2(){
		mcGen = new MountainCar();
		domain = mcGen.generateDomain();
		rf = new UniformCostRF();
		tf = new MountainCar.ClassicMCTF();
		//rf = new GoalBasedRF(tf, 1, 0.);
		//rf = new GoalBasedRF(new TFGoalCondition(tf), 100);
		sp = new MountainCarStateParser(domain);
		initialState = mcGen.getCleanState(domain);
	}
	
	
	/**
	 * Launches and episode visual explorer for this domain.
	 * @param outputPath
	 */
	public void visualizeCachedFiles(String outputPath){
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	
	/**
	 * Runs gradient descent sarsa lambda with CMAC VFA for 200 learning episodes. If renderLive is set to true
	 * it will showing the learning live at 60FPS and records the results to disk for later viewing. Note that not rendering live will
	 * run *MUCH* faster than 60FPS.
	 * @param outputPath the path in which the saved episodes will be stored.
	 * @param renderLive if true then show learning as it happens capped at a speed of 60FPS; if false do not render live; only cache results to disk.
	 */
	public void runCMACVFA(String outputPath, boolean renderLive){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		
		//add live viewer if set to
		if(renderLive){
			VisualActionObserver observer = new VisualActionObserver(domain, MountainCarVisualizer.getVisualizer(mcGen));
			((SADomain)domain).addActionObserverForAllAction(observer);
			observer.initGUI();
		}
		
		
		//create CMAC specification that divides the position/velocity attributes into 5 10x10 tilings
		int nTilings = 10;
		
		double resolution = 5.;
		/*
		CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, TilingArrangement.RANDOMJITTER);
		cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTX), (mcGen.xmax - mcGen.xmin)/resolution);
		cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTV), (mcGen.vmax - mcGen.vmin)/resolution);
		*/
		FVCMACFeatureDatabase cmac = new FVCMACFeatureDatabase(new ConcatenatedObjectFeatureVectorGenerator(MountainCar.CLASSAGENT));
		cmac.addTilingsForAllDimensionsWithWidths(new double[]{(mcGen.physParams.xmax - mcGen.physParams.xmin)/resolution, (mcGen.physParams.vmax - mcGen.physParams.vmin)/resolution},
				nTilings, TilingArrangement.RANDOMJITTER);
		
		//create linear VFA over CMAC
		double defaultQ = 0.;
		ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);
		
		//Initialize gradient descent sarsa lambda (discount 0.99, 0.02 learning rate, max number of steps per episode, lambda = 0.2
		//GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, Integer.MAX_VALUE, 0.5);
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 1.0, vfa, 0.001, Integer.MAX_VALUE, 0.9);
		agent.setLearningPolicy(new EpsilonGreedy(agent, 0.));
		//agent.setLearningRate(new SoftTimeInverseDecayLR(0.001, 1000, 0.00001));
		//agent.setLearningRate(new ExponentialDecayLR(0.001, 0.9999999, 0.00001));
		agent.setUseReplaceTraces(true);
		
		//run learning
		EpisodeAnalysis ea = null;
		for(int i = 0; i < 300; i++){
			ea = agent.runLearningEpisodeFrom(initialState, 10000); //run learning episode
			ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
		System.out.println(ea.getAction(0).toString());
		
		Policy p = new GreedyQPolicy(agent);
		//Policy p = new EpsilonGreedy(agent, 0.1);
		
		System.out.println("----------------\nEvaluating");
		for(int i = 0; i < 1; i++){
			ea = p.evaluateBehavior(initialState, rf, tf, 5000);
			System.out.println(ea.numTimeSteps());
			ea.writeToFile(outputPath+"eval", sp);
		}
	
		
		
		
	}
	
	public void fourierTest(String outputPath, boolean renderLive){
		
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		
		//add live viewer if set to
		if(renderLive){
			VisualActionObserver observer = new VisualActionObserver(domain, MountainCarVisualizer.getVisualizer(mcGen));
			((SADomain)domain).addActionObserverForAllAction(observer);
			observer.initGUI();
		}
		
		
		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT), 4);
		ValueFunctionApproximation vfa = fb.generateVFA(0.);
		
		//Initialize gradient descent sarsa lambda (discount 0.99, 0.02 learning rate, max number of steps per episode, lambda = 0.2
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 1.0, vfa, 0.001, Integer.MAX_VALUE, 0.9);
		agent.setLearningRate(new FourierBasisLearningRateWrapper(new ConstantLR(0.001), fb));
		agent.setLearningPolicy(new GreedyQPolicy(agent));
		
		
		//run learning
		for(int i = 0; i < 300; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, 10000); //run learning episode
			ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	public void runCMACExperimenter(){
		
		LearningAgentFactory cmacFact = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "CMAC";
			}
			
			@Override
			public LearningAgent generateAgent() {
				
				//create CMAC specification that divides the position/velocity attributes into 5 10x10 tilings
				int nTilings = 5;
				CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, TilingArrangement.RANDOMJITTER);
				double resolution = 10.;
				cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTX), (mcGen.physParams.xmax - mcGen.physParams.xmin)/resolution);
				cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTV), (mcGen.physParams.vmax - mcGen.physParams.vmin)/resolution);
				
				//create linear VFA over CMAC
				double defaultQ = 0.0;
				ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);
				
				//Initialize gradient descent sarsa lambda (discount 0.99, 0.02 learning rate, max number of steps per episode, lambda = 0.2
				GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.05, Integer.MAX_VALUE, 0.5);
				
				return agent;
			}
		};
		
		
		LearningAgentFactory cmacDecayFact = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "CMAC Decay";
			}
			
			@Override
			public LearningAgent generateAgent() {
				
				//create CMAC specification that divides the position/velocity attributes into 5 10x10 tilings
				int nTilings = 5;
				CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, TilingArrangement.RANDOMJITTER);
				double resolution = 10.;
				cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTX), (mcGen.physParams.xmax - mcGen.physParams.xmin)/resolution);
				cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTV), (mcGen.physParams.vmax - mcGen.physParams.vmin)/resolution);
				
				//create linear VFA over CMAC
				double defaultQ = 0.0;
				ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);
				
				//Initialize gradient descent sarsa lambda (discount 0.99, 0.02 learning rate, max number of steps per episode
				GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, Integer.MAX_VALUE, 0.5);
				
				LearningRate lr = new ExponentialDecayLR(0.1, 0.9999, 0.003);
				agent.setLearningRate(lr);
				
				return agent;
			}
		};
		
		
		LearningAgentFactory rbfDecayFact = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "RBF Decay";
			}
			
			@Override
			public LearningAgent generateAgent() {
				
				//create CMAC specification that divides the position/velocity attributes into 5 10x10 tilings
				RBFFeatureDatabase rbfdb = new RBFFeatureDatabase(true);
				
				EuclideanDistance dist = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(MountainCar.CLASSAGENT));
				
				double xRange = mcGen.physParams.xmax - mcGen.physParams.xmin;
				double vRange = mcGen.physParams.vmax - mcGen.physParams.vmin;
				
				double distRatio = 200;
				double bandwidth = Math.sqrt((xRange*xRange/distRatio) + (vRange*vRange/distRatio));
				
				addRBFs(rbfdb, dist, 10, bandwidth);
				
				ValueFunctionApproximation vfa = rbfdb.generateVFA(0.);
				
				//Initialize gradient descent sarsa lambda (discount 0.99, 0.02 learning rate, max number of steps per episode
				GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, Integer.MAX_VALUE, 0.8);
				
				LearningRate lr = new ExponentialDecayLR(0.02, 0.9999, 0.003);
				agent.setLearningRate(lr);
				
				return agent;
			}
		};
		
		
		
		
		
		StateGenerator sg = new ConstantStateGenerator(initialState);
		
		LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter((SADomain)domain, rf, sg, 10, 100, cmacDecayFact, rbfDecayFact);
		exp.setUpPlottingConfiguration(500, 300, 2, 800, TrialMode.MOSTRECENTANDAVERAGE, PerformanceMetric.CUMULATIVESTEPSPEREPISODE, PerformanceMetric.STEPSPEREPISODE);
		
		exp.startExperiment();
		
	}
	
	
	protected void addRBFs(RBFFeatureDatabase rbfdb, DistanceMetric dist, int resolution, double bandwidth){
		
		double windowX = (this.mcGen.physParams.xmax - this.mcGen.physParams.xmin) / resolution;
		double windowV = (this.mcGen.physParams.vmax - this.mcGen.physParams.vmin) / resolution;
		
		double x = this.mcGen.physParams.xmin;
		while(x <= this.mcGen.physParams.xmax){
			double v = this.mcGen.physParams.vmin;
			while(v <= this.mcGen.physParams.vmax){
				
				State c = this.mcGen.getState(this.domain, x, v);
				RBF rbf = new GaussianRBF(c, dist, bandwidth);
				rbfdb.addRBF(rbf);
				
				v += windowV;
			}
			
			x += windowX;
		}
		
		
	}

}
