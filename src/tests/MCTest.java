package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase.TilingArrangement;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarStateParser;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

public class MCTest {

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
		MCTest mctest = new MCTest();
		
		String outputPath = "MCTest/";
		
		mctest.runCMACVFA(outputPath, false);
		mctest.visualizeCachedFiles(outputPath);

	}
	
	
	
	public MCTest(){
		mcGen = new MountainCar();
		domain = mcGen.generateDomain();
		rf = new UniformCostRF();
		tf = mcGen.new ClassicMCTF();
		sp = new MountainCarStateParser(domain);
		initialState = mcGen.getCleanState(domain);
	}
	
	
	/**
	 * Launches and episode visual explorer for this domain.
	 * @param outputPath
	 */
	public void visualizeCachedFiles(String outputPath){
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
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
			((SADomain)domain).setActionObserverForAllAction(observer);
			observer.initGUI();
		}
		
		
		//create CMAC specification that divides the position/velocity attributes into 5 10x10 tilings
		int nTilings = 5;
		CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, TilingArrangement.RANDOMJITTER);
		double resolution = 10.;
		cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTX), (mcGen.xmax - mcGen.xmin)/resolution);
		cmac.addSpecificationForAllTilings(MountainCar.CLASSAGENT, domain.getAttribute(MountainCar.ATTV), (mcGen.vmax - mcGen.vmin)/resolution);
		
		//create linear VFA over CMAC
		double defaultQ = 0.0;
		ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);
		
		//Initialize gradient descent sarsa lambda (discount 0.99, 0.02 learning rate, max number of steps per episode, lambda = 0.2
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, Integer.MAX_VALUE, 0.5);
		
		
		//run learning
		for(int i = 0; i < 1000; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%04d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
		
		
	}

}
