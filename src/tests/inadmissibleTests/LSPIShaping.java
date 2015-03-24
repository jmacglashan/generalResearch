package tests.inadmissibleTests;

import behavior.planning.SRTDP;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.lspi.LSPI;
import burlap.behavior.singleagent.learning.lspi.SARSCollector;
import burlap.behavior.singleagent.learning.lspi.SARSData;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyDeterministicQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.rtdp.BoundedRTDP;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.fourier.FourierBasis;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.domain.singleagent.mountaincar.MCRandomStateGenerator;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

/**
 * @author James MacGlashan.
 */
public class LSPIShaping {

	protected MountainCar mc;
	protected Domain domain;
	protected RewardFunction rf;
	protected TerminalFunction tf;
	protected State valleyState;


	public LSPIShaping(){
		this.mc = new MountainCar();
		this.domain = this.mc.generateDomain();
		this.tf = this.mc.new ClassicMCTF();
		this.rf = new GoalBasedRF(this.tf, 100.);
		this.valleyState = this.mc.getCleanState(this.domain);
	}


	public void evalLSPI(int nSamples, int nTries){

		double avgSteps = 0;
		for(int i = 0; i < nTries; i++) {
			LSPI lspi = this.getLSPI(nSamples);
			Policy p = new GreedyQPolicy(lspi);
			EpisodeAnalysis ea = p.evaluateBehavior(this.valleyState, this.rf, this.tf, 500);
			int steps = ea.maxTimeStep();
			avgSteps += steps;
			System.out.println("Steps: " + steps);
		}
		avgSteps /= nTries;
		System.out.println("-----------------\nAverage Steps: " + avgSteps);
	}

	public void brtdpTest(int nSamples){
		LSPI lspi = this.getLSPI(nSamples);

		Policy lp = new GreedyQPolicy(lspi);
		int lpSteps = lp.evaluateBehavior(this.valleyState, this.rf, this.tf, 500).maxTimeStep();
		System.out.println("LSPI seed quality: " + lpSteps);


		int numRollsouts = 5000;

		BoundedRTDP brtdp = new BoundedRTDP(this.domain, this.rf, this.tf, 0.99, new NameDependentStateHashFactory(),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.),
				new QPlannerValueInit(lspi),
				0.01, 1);
		brtdp.setMaxRolloutDepth(500);
		Policy p  = new GreedyQPolicy(brtdp);

		System.out.println("Starting BRTDP");

		int lastTotal = 0;
		int brtdpSteps = 0;
		for(int i = 0; i < numRollsouts; i++){
			brtdp.runRollout(this.valleyState);
			int newTotal = brtdp.getNumberOfSteps();
			int lastSize = newTotal - lastTotal;
			lastTotal = newTotal;
			EpisodeAnalysis test = p.evaluateBehavior(this.valleyState, this.rf, this.tf, 500);
			brtdpSteps = test.maxTimeStep();
			System.out.println(i + ": " + brtdpSteps + " (" + lastSize + ")");
		}

		brtdp.resetPlannerResults();

		SRTDP srtdp = new SRTDP(this.domain, this.rf, this.tf, 0.99, new NameDependentStateHashFactory(),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.),
				new QPlannerValueInit(lspi),
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(100.),
				0.5, 0.01, 1);
		srtdp.setMaxRolloutDepth(500);
		Policy sp = new GreedyQPolicy(srtdp);

		System.out.println("Starting SRTDP");

		lastTotal = 0;
		int srtdpSteps = 0;
		for(int i = 0; i < numRollsouts; i++){
			srtdp.runRollout(this.valleyState);
			int newTotal = srtdp.getNumberOfSteps();
			int lastSize = newTotal - lastTotal;
			lastTotal = newTotal;
			EpisodeAnalysis test = sp.evaluateBehavior(this.valleyState, this.rf, this.tf, 500);
			srtdpSteps = test.maxTimeStep();
			System.out.println(i + ": " + srtdpSteps + " (" + lastSize + ")");
		}

		System.out.println("\n" + lpSteps + " " + brtdpSteps + " " + srtdpSteps);

	}


	public LSPI getLSPI(int nSamples){
		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT), 1);

		StateGenerator rStateGen = new MCRandomStateGenerator(domain);

		SARSCollector collector = new SARSCollector.UniformRandomSARSCollector(domain);

		System.out.println("Beginning data collection");
		SARSData dataset = collector.collectNInstances(rStateGen, rf, nSamples, 20, tf, null);
		System.out.println("Ending data collection");

		LSPI lspi = new LSPI(domain, rf, tf, 0.99, fb);
		lspi.setDataset(dataset);

		System.out.println("Beginning PI");
		lspi.runPolicyIteration(30, 1e-6);
		System.out.println("Finished PI");

		return lspi;
	}


	public static class QPlannerValueInit implements ValueFunctionInitialization{

		protected QComputablePlanner planner;

		public QPlannerValueInit(QComputablePlanner planner) {
			this.planner = planner;
		}

		@Override
		public double value(State s) {
			return QComputablePlanner.QComputablePlannerHelper.getOptimalValue(this.planner, s);
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return this.planner.getQ(s, a).q;
		}
	}



	public static void main(String [] args){
		LSPIShaping exp = new LSPIShaping();
		//exp.evalLSPI(3000, 5);
		exp.brtdpTest(3000);
	}



}
