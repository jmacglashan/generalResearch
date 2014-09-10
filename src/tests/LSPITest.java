package tests;

import java.util.Random;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.lspi.LSPI;
import burlap.behavior.singleagent.learning.lspi.SARSCollector;
import burlap.behavior.singleagent.learning.lspi.SARSData;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.fourier.FourierBasis;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBF;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.mountaincar.MCRandomStateGenerator;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarStateParser;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.domain.singleagent.mountaincar.MountainCar.ClassicMCTF;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

public class LSPITest {

public static void main(String [] args){
		
		//forcedSample();
		easyMC();
		//initialStatePlan();
		//learningRun();
		
	}
	
	public static void learningRun(){
		
		final MountainCar mcGen = new MountainCar();
		final Domain domain = mcGen.generateDomain();
		final TerminalFunction tf = mcGen.new ClassicMCTF();
		final RewardFunction rf = new GoalBasedRF(new TFGoalCondition(tf), 100);
		final StateParser sp = new MountainCarStateParser(domain);
		
		
		
		
		/*
		RBFFeatureDatabase rbfdb = new RBFFeatureDatabase(true);
		
		EuclideanDistance dist = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT));
		
		double xRange = mcGen.xmax - mcGen.xmin;
		double vRange = mcGen.vmax - mcGen.vmin;
		double xvRange = Math.sqrt(xRange*xRange + vRange*vRange);
		
		//double distRatio = 200;
		double distRatio = 50;
		//double bandwidth = Math.sqrt((xRange*xRange/distRatio) + (vRange*vRange/distRatio));
		//double bandwidth = xvRange / 4;
		double bandwidth = 0.2;
		System.out.println("bandwidth: " + bandwidth);
		
		//addRBFs(mcGen, domain, rbfdb, dist, 10, bandwidth);
		addRBFs(mcGen, domain, rbfdb, dist, 4, bandwidth);
		*/
		
		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT), 4);
		
		
		StateGenerator rStateGen = new StateGenerator() {
			
			@Override
			public State generateState() {
				
				Random rand = RandomFactory.getMapped(0);
				
				double rx = rand.nextDouble()*(mcGen.xmax - mcGen.xmin) + mcGen.xmin;
				double rv = rand.nextDouble()*(mcGen.vmax - mcGen.vmin) + mcGen.vmin;
				
				State s = mcGen.getState(domain, rx, rv);
				return s;
			}
		};
		
		
		//LSPI lspi = new LSPI(domain, rf, tf, 0.99, rbfdb);
		LSPI lspi = new LSPI(domain, rf, tf, 0.99, fb);
		
		for(int i = 0; i < 20; i++){
			EpisodeAnalysis ea = lspi.runLearningEpisodeFrom(rStateGen.generateState(), 20);
			System.out.println("Num learning steps: " + ea.numTimeSteps());
		}
		
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		
		VisualActionObserver obs = new VisualActionObserver(domain, v);
		((SADomain)domain).addActionObserverForAllAction(obs);
		
		obs.initGUI();
		
		final GreedyQPolicy p = new GreedyQPolicy(lspi);
		State s = mcGen.getCleanState(domain);
		MountainCar.setAgent(s, s.getFirstObjectOfClass(MountainCar.CLASSAGENT).getRealValForAttribute(MountainCar.ATTX)+(0.2*Math.random()*(mcGen.xmax-mcGen.xmin)), 0);
		p.evaluateBehavior(s, rf, tf);
		System.out.println("Done.");
		
	}
	
	public static void initialStatePlan(){
		final MountainCar mcGen = new MountainCar();
		final Domain domain = mcGen.generateDomain();
		final TerminalFunction tf = mcGen.new ClassicMCTF();
		final RewardFunction rf = new GoalBasedRF(new TFGoalCondition(tf), 100);
		final StateParser sp = new MountainCarStateParser(domain);
		
		RBFFeatureDatabase rbfdb = new RBFFeatureDatabase(true);
		
		EuclideanDistance dist = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT));
		
		double xRange = mcGen.xmax - mcGen.xmin;
		double vRange = mcGen.vmax - mcGen.vmin;
		double xvRange = Math.sqrt(xRange*xRange + vRange*vRange);
		
		//double distRatio = 200;
		double distRatio = 50;
		//double bandwidth = Math.sqrt((xRange*xRange/distRatio) + (vRange*vRange/distRatio));
		//double bandwidth = xvRange / 4;
		double bandwidth = 0.2;
		System.out.println("bandwidth: " + bandwidth);
		
		//addRBFs(mcGen, domain, rbfdb, dist, 10, bandwidth);
		addRBFs(mcGen, domain, rbfdb, dist, 4, bandwidth);
		
		LSPI lspi = new LSPI(domain, rf, tf, 0.99, rbfdb);
		lspi.planFromState(mcGen.getCleanState(domain));
		
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		
		VisualActionObserver obs = new VisualActionObserver(domain, v);
		((SADomain)domain).addActionObserverForAllAction(obs);
		
		obs.initGUI();
		
		final GreedyQPolicy p = new GreedyQPolicy(lspi);
		State s = mcGen.getCleanState(domain);
		MountainCar.setAgent(s, s.getFirstObjectOfClass(MountainCar.CLASSAGENT).getRealValForAttribute(MountainCar.ATTX)+(0.2*Math.random()*(mcGen.xmax-mcGen.xmin)), 0);
		p.evaluateBehavior(s, rf, tf);
		System.out.println("Done.");
	}
	
	
	public static void easyMC(){
		
		final MountainCar mcGen = new MountainCar();
		final Domain domain = mcGen.generateDomain();
		final TerminalFunction tf = mcGen.new ClassicMCTF();
		final RewardFunction rf = new GoalBasedRF(new TFGoalCondition(tf), 100);
		final StateParser sp = new MountainCarStateParser(domain);
		
		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT), 4);
		
		StateGenerator rStateGen = new MCRandomStateGenerator(domain);
		
		SARSCollector collector = new SARSCollector.UniformRandomSARSCollector(domain);
		
		System.out.println("Beginning data collection");
		SARSData dataset = collector.collectNInstances(rStateGen, rf, 5000, 20, tf, null);
		System.out.println("Ending data collection");
		
		LSPI lspi = new LSPI(domain, rf, tf, 0.99, fb);
		lspi.setDataset(dataset);
		
		System.out.println("Beginning PI");
		lspi.runPolicyIteration(30, 1e-6);
		System.out.println("Finished PI");
		
		System.out.println("Will now visualize Mountain Car for using estimated value function from valley 10 times");
		
		final GreedyQPolicy p = new GreedyQPolicy(lspi);
		State s = mcGen.getCleanState(domain);
		
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		VisualActionObserver vexp = new VisualActionObserver(domain, v);
		vexp.initGUI();
		((SADomain)domain).addActionObserverForAllAction(vexp);
		
		for(int i = 0; i < 10; i++){
			p.evaluateBehavior(s, rf, tf);
		}
		
		System.out.println("Finished.");
		
		
	}
	
	public static void forcedSample(){
		
		final MountainCar mcGen = new MountainCar();
		final Domain domain = mcGen.generateDomain();
		final TerminalFunction tf = mcGen.new ClassicMCTF();
		final RewardFunction rf = new GoalBasedRF(new TFGoalCondition(tf), 100);
		final StateParser sp = new MountainCarStateParser(domain);
		
		
		/*
		RBFFeatureDatabase rbfdb = new RBFFeatureDatabase(true);
		
		EuclideanDistance dist = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT));
		
		double xRange = mcGen.xmax - mcGen.xmin;
		double vRange = mcGen.vmax - mcGen.vmin;
		double xvRange = Math.sqrt(xRange*xRange + vRange*vRange);
		
		//double distRatio = 200;
		double distRatio = 50;
		//double bandwidth = Math.sqrt((xRange*xRange/distRatio) + (vRange*vRange/distRatio));
		//double bandwidth = xvRange / 4;
		double bandwidth = 0.2;
		System.out.println("bandwidth: " + bandwidth);
		
		//addRBFs(mcGen, domain, rbfdb, dist, 10, bandwidth);
		addRBFs(mcGen, domain, rbfdb, dist, 4, bandwidth);
		*/
		
		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT), 4);
		
		StateGenerator rStateGen = new StateGenerator() {
			
			@Override
			public State generateState() {
				
				Random rand = RandomFactory.getMapped(0);
				
				double rx = rand.nextDouble()*(mcGen.xmax - mcGen.xmin) + mcGen.xmin;
				double rv = rand.nextDouble()*(mcGen.vmax - mcGen.vmin) + mcGen.vmin;
				
				State s = mcGen.getState(domain, rx, rv);
				return s;
			}
		};
		
		SARSCollector collector = new SARSCollector.UniformRandomSARSCollector(domain);
		
		System.out.println("Beginning collection");
		SARSData dataset = collector.collectNInstances(rStateGen, rf, 5000, 20, tf, null);
		System.out.println("Ending collection");
		
		//LSPI lspi = new LSPI(domain, rf, tf, 0.99, rbfdb);
		LSPI lspi = new LSPI(domain, rf, tf, 0.99, fb);
		lspi.setDataset(dataset);
		
		System.out.println("Beginning PI");
		lspi.runPolicyIteration(30, 1e-6);
		System.out.println("Finished PI");
		
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
	
		//VisualActionObserver obs = new VisualActionObserver(domain, v);
		//((SADomain)domain).addActionObserverForAllAction(obs);
		
		//obs.initGUI();
		
		final GreedyQPolicy p = new GreedyQPolicy(lspi);
		State s = mcGen.getCleanState(domain);
		//MountainCar.setAgent(s, s.getFirstObjectOfClass(MountainCar.CLASSAGENT).getRealValForAttribute(MountainCar.ATTX)+(0.2*Math.random()*(mcGen.xmax-mcGen.xmin)), 0);
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf);
		System.out.println("Done.");
		System.out.println(ea.numTimeSteps());
		ea.writeToFile("lspiTest/FinalPolicy", new MountainCarStateParser(domain));
		
		new EpisodeSequenceVisualizer(v, domain, new MountainCarStateParser(domain), "lspiTest");
		
		/*
		SpecialExplorerAction spA = new SpecialExplorerAction() {
			
			@Override
			public State applySpecialAction(State curState) {
				GroundedAction ga = (GroundedAction)p.getAction(curState);
				return ga.executeIn(curState);
			}
		};
		
		VisualExplorer exp = new VisualExplorer(domain, v, mcGen.getCleanState(domain));
		
		exp.addKeyAction("d", MountainCar.ACTIONFORWARD);
		exp.addKeyAction("s", MountainCar.ACTIONCOAST);
		exp.addKeyAction("a", MountainCar.ACTIONBACKWARDS);
		exp.addSpecialAction("p", spA);
		
		exp.initGUI();
		*/
		
	}
	
	protected static void addRBFs(MountainCar mcGen, Domain domain, RBFFeatureDatabase rbfdb, DistanceMetric dist, int resolution, double bandwidth){
		
		double windowX = (mcGen.xmax - mcGen.xmin) / resolution;
		double windowV = (mcGen.vmax - mcGen.vmin) / resolution;
		
		double x = mcGen.xmin;
		while(x <= mcGen.xmax){
			double v = mcGen.vmin;
			while(v <= mcGen.vmax){
				
				State c = mcGen.getState(domain, x, v);
				RBF rbf = new GaussianRBF(c, dist, bandwidth);
				rbfdb.addRBF(rbf);
				
				v += windowV;
			}
			
			x += windowX;
		}
		
		
	}

}
