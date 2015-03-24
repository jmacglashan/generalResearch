package tests;

import behavior.planning.vfa.td.GradientDescentTDLambdaLookahead;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase.TilingArrangement;
import burlap.behavior.singleagent.vfa.cmac.FVCMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.lunarlander.LLStateParser;
import burlap.domain.singleagent.lunarlander.LLVisualizer;
import burlap.domain.singleagent.lunarlander.LunarLanderDomain;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.visualizer.Visualizer;


import java.util.List;

public class VFATest {

	protected LunarLanderDomain		lld;
	protected Domain				domain;
	protected RewardFunction		rf;
	protected TerminalFunction		tf;
	protected StateParser			sp;
	protected State					initialState;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		VFATest example = new VFATest();
		String outputPath = "lunarLander"; //directory to record results
		
		//example.runCMACVFA(outputPath);
		//example.runFVCMACVFA(outputPath);
		example.runRBF(outputPath);
		//example.visualize(outputPath);

	}
	
	
	public VFATest() {
		
		lld = new LunarLanderDomain();

		//possible remove
		lld.setToStandardLunarLander();
		this.lld.setAngmax(Math.PI/6.);
		this.lld.setAnginc(Math.PI/6.);
		this.lld.setVmax(2.5);

		domain = lld.generateDomain();
		rf = new LLRF(domain);
		tf = new SinglePFTF(domain.getPropFunction(LunarLanderDomain.PFONPAD));
		sp = new LLStateParser(domain);
		
		initialState = LunarLanderDomain.getCleanState(domain, 0);
		LunarLanderDomain.setAgent(initialState, 0., 5.0, 0.0);
		LunarLanderDomain.setPad(initialState, 75., 95., 0., 10.);
		
	}
	
	
	public void visualize(String outputPath){
		Visualizer v = LLVisualizer.getVisualizer(lld);
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	
	public void runCMACVFA(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//VisualActionObserver observer = new VisualActionObserver(domain, LLVisualizer.getVisualizer(lld));
		//((SADomain)domain).setActionObserverForAllAction(observer);
		//observer.initGUI();
		
		int nTilings = 5;
		CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, TilingArrangement.RANDOMJITTER);
		double resolution = 10.;
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.AATTNAME), 2*lld.getAngmax() / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.XATTNAME), (lld.getXmax() - lld.getXmin()) / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.YATTNAME), (lld.getYmax() - lld.getYmin()) / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.VXATTNAME), 2*lld.getVmax() / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.VYATTNAME), 2*lld.getVmax() / resolution);
		
		double defaultQ = 0.5;
		
		ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);
		
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, 10000, 0.5);
		
		MyTimer timer = new MyTimer();
		timer.start();
		
		for(int i = 0; i < 5000; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			//ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
		timer.stop();
		
		System.out.println("time orignal: " + timer.getTime());
		
		
		
	}


	public void runRBF(String outputPath){

		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}

		ConcatenatedObjectFeatureVectorGenerator sfv = new ConcatenatedObjectFeatureVectorGenerator(LunarLanderDomain.AGENTCLASS);
		ConcatenatedObjectFeatureVectorGenerator nsfv = new ConcatenatedObjectFeatureVectorGenerator(true, LunarLanderDomain.AGENTCLASS);
		StateGridder gridder = new StateGridder();
		gridder.gridEntireObjectClass(this.domain.getObjectClass(LunarLanderDomain.AGENTCLASS), 3);
		List<State> griddedStates = gridder.gridInputState(this.initialState);
		RBFFeatureDatabase rbf = new RBFFeatureDatabase(true);
		DistanceMetric metric = new EuclideanDistance(nsfv);
		for(State s : griddedStates){
			rbf.addRBF(new GaussianRBF(s, metric, 0.2));
		}
		rbf.addRBF(new GaussianRBF(this.getOnPadCenterState(), metric, 0.2));

		double defaultQ = 0.5;
		ValueFunctionApproximation vfa = new LinearVFA(rbf, defaultQ/griddedStates.size());

		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.002, 5000, 0.0);
		//GradientDescentTDLambdaLookahead agent = new GradientDescentTDLambdaLookahead(domain, rf, tf, 0.99, vfa, 0.002, 0.5, 6000, 5000);

		MyTimer timer = new MyTimer();
		timer.start();

		//agent.planFromState(initialState);


		int totalSteps = 0;


		for(int i = 0; i < 6000; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			//ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
			totalSteps += ea.numTimeSteps()-1;
		}


		timer.stop();

		System.out.println("time: " + timer.getTime());
		System.out.println("samples: " + totalSteps);

	}
	

	public void runFVCMACVFA(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		ConcatenatedObjectFeatureVectorGenerator sfv = new ConcatenatedObjectFeatureVectorGenerator(LunarLanderDomain.AGENTCLASS);
		double resolution = 10.;
		double [] widths = new double[]{
				(lld.getXmax() - lld.getXmin()) / resolution,
				(lld.getYmax() - lld.getYmin()) / resolution,
				2*lld.getVmax() / resolution,
				2*lld.getVmax() / resolution,
				2*lld.getAngmax() / resolution};
		
		FVCMACFeatureDatabase cmac = new FVCMACFeatureDatabase(sfv);
		int nTilings = 5;
		cmac.addTilingsForAllDimensionsWithWidths(widths, nTilings, TilingArrangement.RANDOMJITTER);
		
		double defaultQ = 0.5;
		ValueFunctionApproximation vfa = new LinearVFA(cmac, defaultQ/nTilings);
		
		//GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, 10000, 0.5);
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, 10000, 0.0);
		
		MyTimer timer = new MyTimer();
		timer.start();

		int totalSteps = 0;
		for(int i = 0; i < 6000; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			//ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
			totalSteps += ea.numTimeSteps()-1;
		}
		
		timer.stop();
		
		System.out.println("time: " + timer.getTime());
		System.out.println("samples: " + totalSteps);
		
		
	}


	public State getOnPadCenterState(){


		ObjectInstance a = this.initialState.getFirstObjectOfClass(LunarLanderDomain.PADCLASS);
		double x = (a.getRealValForAttribute(LunarLanderDomain.LATTNAME) + a.getRealValForAttribute(LunarLanderDomain.RATTNAME))/2.;
		double y = a.getRealValForAttribute(LunarLanderDomain.TATTNAME);

		State padState = this.initialState.copy();
		ObjectInstance agent = padState.getFirstObjectOfClass(LunarLanderDomain.AGENTCLASS);
		agent.setValue(LunarLanderDomain.XATTNAME, x);
		agent.setValue(LunarLanderDomain.YATTNAME, y);

		return padState;


	}

	
	class LLRF implements RewardFunction{

		
		double							goalReward = 1000.0;
		double							collisionReward = -100.0;
		double							defaultReward = -1.0;
		
		PropositionalFunction			onGround;
		PropositionalFunction			touchingSurface;
		PropositionalFunction			touchingPad;
		PropositionalFunction			onPad;
		
		
		public LLRF(Domain domain){
			
			this.onGround = domain.getPropFunction(LunarLanderDomain.PFONGROUND);
			this.touchingSurface = domain.getPropFunction(LunarLanderDomain.PFTOUCHSURFACE);
			this.touchingPad = domain.getPropFunction(LunarLanderDomain.PFTPAD);
			this.onPad = domain.getPropFunction(LunarLanderDomain.PFONPAD);
			
		}
		
		
		public LLRF(Domain domain, double goalReward, double collisionReward, double defaultReward){
			this.goalReward = goalReward;
			this.collisionReward = collisionReward;
			this.defaultReward = defaultReward;
			
			this.onGround = domain.getPropFunction(LunarLanderDomain.PFONGROUND);
			this.touchingSurface = domain.getPropFunction(LunarLanderDomain.PFTOUCHSURFACE);
			this.touchingPad = domain.getPropFunction(LunarLanderDomain.PFTPAD);
			this.onPad = domain.getPropFunction(LunarLanderDomain.PFONPAD);
		}
		
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			if(sprime.somePFGroundingIsTrue(onPad)){
				return goalReward;
			}
			
			if(sprime.somePFGroundingIsTrue(onGround) || sprime.somePFGroundingIsTrue(touchingPad) || sprime.somePFGroundingIsTrue(touchingSurface)){
				return collisionReward;
			}
			
			return defaultReward;
		}
		
		
		
		
	}

}
