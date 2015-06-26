package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.rbf.FVRBF;
import burlap.behavior.singleagent.vfa.rbf.FVRBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.RBF;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.FVGaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class FVRBFTest {

	MountainCar mc;
	Domain domain;
	RewardFunction rf;
	TerminalFunction tf;
	State s;


	public FVRBFTest(){
		this.mc = new MountainCar();
		this.domain = this.mc.generateDomain();
		this.rf = new UniformCostRF();
		this.tf = new MountainCar.ClassicMCTF();
		this.s = this.mc.getCleanState(domain);
	}


	public void runClassicRBF(){

		StateGridder gridder = new StateGridder();
		gridder.gridEntireDomainSpace(domain, 5);
		List<State> gridStates = gridder.gridInputState(this.s);
		List <RBF> rbfs = GaussianRBF.generateGaussianRBFsForStates(gridStates, new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT)), 0.4);
		RBFFeatureDatabase rbf = new RBFFeatureDatabase(true);
		rbf.addRBFs(rbfs);
		ValueFunctionApproximation vfa = rbf.generateVFA(0.);

		GradientDescentSarsaLam sarsa = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, 0.8);

		MyTimer timer = new MyTimer();
		timer.start();
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = sarsa.runLearningEpisodeFrom(this.s);
			System.out.println(i + ": " + ea.maxTimeStep());
		}
		timer.stop();
		System.out.println(timer.getTime());

	}

	public void runFVRBF(){

		StateGridder gridder = new StateGridder();
		gridder.gridEntireDomainSpace(domain, 5);
		List<State> gridStates = gridder.gridInputState(this.s);
		StateToFeatureVectorGenerator fvGen = new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT);
		List <FVRBF> rbfs = FVGaussianRBF.generateGaussianRBFsForStates(gridStates, fvGen, 0.4);
		FVRBFFeatureDatabase rbf = new FVRBFFeatureDatabase(fvGen, true);
		rbf.addRBFs(rbfs);
		ValueFunctionApproximation vfa = rbf.generateVFA(0.);

		GradientDescentSarsaLam sarsa = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, 0.8);

		MyTimer timer = new MyTimer();
		timer.start();
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = sarsa.runLearningEpisodeFrom(this.s);
			System.out.println(i + ": " + ea.maxTimeStep());
		}
		timer.stop();
		System.out.println(timer.getTime());

	}


	public static void main(String[] args) {
		FVRBFTest test = new FVRBFTest();
		//test.runClassicRBF();
		test.runFVRBF();
	}

}
