package tutorials;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.lspi.LSPI;
import burlap.behavior.singleagent.learning.lspi.SARSCollector;
import burlap.behavior.singleagent.learning.lspi.SARSData;
import burlap.behavior.singleagent.learning.lspi.SARSData.SARS;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.behavior.singleagent.vfa.fourier.FourierBasis;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.cartpole.*;
import burlap.domain.singleagent.lunarlander.*;
import burlap.domain.singleagent.mountaincar.MCRandomStateGenerator;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarStateParser;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

public class VFATutorial {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//SSCartPole();
		//LSPICartPole();
		//LSPIPend();
		//LSPIMC();
		SARSALL();
		//SSPend();
		//SARSACartPole();
		//PCartPole();

	}


	public static void SARSALL(){

		LunarLanderDomain lld = new LunarLanderDomain();
		Domain domain = lld.generateDomain();
		RewardFunction rf = new LunarLanderRF(domain);
		TerminalFunction tf = new LunarLanderTF(domain);

		StateParser sp = new LLStateParser(domain);

		State s = LunarLanderDomain.getCleanState(domain, 0);
		LunarLanderDomain.setAgent(s, 0., 5.0, 0.0);
		LunarLanderDomain.setPad(s, 75., 95., 0., 10.);


		int nTilings = 5;
		CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, CMACFeatureDatabase.TilingArrangement.RANDOMJITTER);
		double resolution = 10.;
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.AATTNAME), 2*lld.getAngmax() / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.XATTNAME), (lld.getXmax() - lld.getXmin()) / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.YATTNAME), (lld.getYmax() - lld.getYmin()) / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.VXATTNAME), 2*lld.getVmax() / resolution);
		cmac.addSpecificationForAllTilings(LunarLanderDomain.AGENTCLASS, domain.getAttribute(LunarLanderDomain.VYATTNAME), 2*lld.getVmax() / resolution);

		double defaultQ = 0.5;

		ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);

		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, vfa, 0.02, 10000, 0.5);


		for(int i = 0; i < 5000; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(s); //run learning episode
			ea.writeToFile(String.format("lunarLander/e%04d", i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}


		Visualizer v = LLVisualizer.getVisualizer(lld);
		new EpisodeSequenceVisualizer(v, domain, sp, "lunarLander");


	}

	public static void LSPIMC(){

		final MountainCar mcGen = new MountainCar();
		final Domain domain = mcGen.generateDomain();
		final TerminalFunction tf = new MountainCar.ClassicMCTF();
		final RewardFunction rf = new GoalBasedRF(new TFGoalCondition(tf), 100);
		final StateParser sp = new MountainCarStateParser(domain);
		State s = mcGen.getCleanState(domain);

		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT), 4);

		RBFFeatureDatabase rbfs = new RBFFeatureDatabase(true);
		StateGridder gridder = new StateGridder();
		gridder.gridEntireDomainSpace(domain, 5);
		List <State> griddedStates = gridder.gridInputState(s);
		DistanceMetric metric = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT));
		for(State g : griddedStates){
			rbfs.addRBF(new GaussianRBF(g, metric, .2));
		}

		StateGenerator rStateGen = new MCRandomStateGenerator(domain);

		SARSCollector collector = new SARSCollector.UniformRandomSARSCollector(domain);

		System.out.println("Beginning data collection");
		SARSData dataset = collector.collectNInstances(rStateGen, rf, 5000, 20, tf, null);
		System.out.println("Ending data collection");

		LSPI lspi = new LSPI(domain, rf, tf, 0.99, rbfs);
		lspi.setDataset(dataset);

		System.out.println("Beginning PI");
		lspi.runPolicyIteration(30, 1e-6);
		System.out.println("Finished PI");

		System.out.println("Will now visualize Mountain Car for using estimated value function from valley 10 times");

		final GreedyQPolicy p = new GreedyQPolicy(lspi);


		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		VisualActionObserver vexp = new VisualActionObserver(domain, v);
		vexp.initGUI();
		((SADomain)domain).addActionObserverForAllAction(vexp);

		for(int i = 0; i < 10; i++){
			p.evaluateBehavior(s, rf, tf);
		}

		System.out.println("Finished.");


	}


	public static void SSPend(){

		InvertedPendulum ip = new InvertedPendulum();
		ip.physParams.actionNoise = 0.;
		Domain domain = ip.generateDomain();
		RewardFunction rf = new InvertedPendulum.InvertedPendulumRewardFunction(Math.PI/8.);
		TerminalFunction tf = new InvertedPendulum.InvertedPendulumTerminalFunction(Math.PI/8.);
		State initialState = InvertedPendulum.getInitialState(domain);

		SparseSampling ss = new SparseSampling(domain, rf, tf, 1, new NameDependentStateHashFactory(), 10, 1);
		ss.setForgetPreviousPlanResults(true);
		//ss.setValueForLeafNodes(new CPVInit());
		Policy p = new GreedyQPolicy(ss);

		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rf, tf, 500);
		StateParser sp = new InvertedPendulumStateParser(domain);

		ea.writeToFile("cartPole/ssPlan", sp);
		System.out.println("Num Steps: " + ea.numTimeSteps());

		Visualizer v = InvertedPendulumVisualizer.getInvertedPendulumVisualizer();
		new EpisodeSequenceVisualizer(v, domain, sp, "cartPole");

	}

	public static void LSPIPend(){

		InvertedPendulum ip = new InvertedPendulum();
		ip.physParams.actionNoise = 0.;
		Domain domain = ip.generateDomain();
		RewardFunction rf = new InvertedPendulum.InvertedPendulumRewardFunction(Math.PI/8);
		TerminalFunction tf = new InvertedPendulum.InvertedPendulumTerminalFunction(Math.PI/8);
		State initialState = InvertedPendulum.getInitialState(domain);

		FourierBasis fb = new FourierBasis(new ConcatenatedObjectFeatureVectorGenerator(true, InvertedPendulum.CLASSPENDULUM), 5);

		SARSCollector collector = new SARSCollector.UniformRandomSARSCollector(domain);

		System.out.println("Beginning collection");
		SARSData dataset = collector.collectNInstances(new ConstantStateGenerator(initialState), rf, 5000, 100, tf, null);
		System.out.println("Ending collection");


		for(SARS sars : dataset.dataset){
			if(sars.r != 0.){
				System.out.println("Found reward " + sars.r);
				break;
			}
		}
		System.out.println("End data scan");

		LSPI lspi = new LSPI(domain, rf, tf, 0.95, fb);
		lspi.setDataset(dataset);

		System.out.println("Beginning PI");
		lspi.runPolicyIteration(30, 1e-6);
		System.out.println("Finished PI");

		List<QValue> qs = lspi.getQs(initialState);
		for(QValue q : qs){
			System.out.println(q.a.toString() + ": " + q.q);
		}

		Policy p = new GreedyQPolicy(lspi);

		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rf, tf, 500);
		System.out.println("Episode size: " + ea.numTimeSteps());
		StateParser sp = new InvertedPendulumStateParser(domain);

		ea.writeToFile("cartPole/lspiPlan", sp);

		Visualizer v = InvertedPendulumVisualizer.getInvertedPendulumVisualizer();
		new EpisodeSequenceVisualizer(v, domain, sp, "cartPole");

	}
	
	public static void SSCartPole(){
		
		CartPoleDomain cpd = new CartPoleDomain();
		cpd.physParams.isFiniteTrack = false;
		Domain domain = cpd.generateDomain();
		RewardFunction rf = new CartPoleDomain.CartPoleRewardFunction();
		TerminalFunction tf = new CartPoleDomain.CartPoleTerminalFunction();
		State initialState = CartPoleDomain.getInitialState(domain);
		
		SparseSampling ss = new SparseSampling(domain, rf, tf, 1, new NameDependentStateHashFactory(), 10, 1);
		ss.setForgetPreviousPlanResults(true);
		//ss.setValueForLeafNodes(new CPVInit());
		Policy p = new GreedyQPolicy(ss);
		
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rf, tf, 500);
		StateParser sp = new CartPoleStateParser(domain);
		
		ea.writeToFile("cartPole/ssPlan", sp);
		System.out.println("Num Steps: " + ea.numTimeSteps());
		
		Visualizer v = CartPoleVisualizer.getCartPoleVisualizer();
		new EpisodeSequenceVisualizer(v, domain, sp, "cartPole");
		
		
	}
	
	public static void LSPICartPole(){
		
		CartPoleDomain cpd = new CartPoleDomain();
		cpd.physParams.isFiniteTrack = false;
		Domain domain = cpd.generateDomain();
		RewardFunction rf = new CartPoleDomain.CartPoleRewardFunction(Math.PI/2.-0.01);
		TerminalFunction tf = new CartPoleDomain.CartPoleTerminalFunction(Math.PI/2.-0.01);
		State initialState = CartPoleDomain.getInitialState(domain);
		
		FourierBasis fb = new FourierBasis(new CartPoleFV(), 5);
		
		SARSCollector collector = new SARSCollector.UniformRandomSARSCollector(domain);
		
		System.out.println("Beginning collection");
		SARSData dataset = collector.collectNInstances(new ConstantStateGenerator(initialState), rf, 10000, 100, tf, null);
		System.out.println("Ending collection");
		
		
		for(SARS sars : dataset.dataset){
			if(sars.r != 0.){
				System.out.println("Found reward " + sars.r);
				break;
			}
		}
		System.out.println("End data scan");
		
		LSPI lspi = new LSPI(domain, rf, tf, 0.95, fb);
		lspi.setDataset(dataset);
		
		System.out.println("Beginning PI");
		lspi.runPolicyIteration(30, 1e-6);
		System.out.println("Finished PI");
		
		List<QValue> qs = lspi.getQs(initialState);
		for(QValue q : qs){
			System.out.println(q.a.toString() + ": " + q.q);
		}
		
		Policy p = new GreedyQPolicy(lspi);
		
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rf, tf, 500);
		StateParser sp = new CartPoleStateParser(domain);
		
		ea.writeToFile("cartPole/lspiPlan", sp);
		
		Visualizer v = CartPoleVisualizer.getCartPoleVisualizer();
		new EpisodeSequenceVisualizer(v, domain, sp, "cartPole");
		

	}

	public static void SARSACartPole(){
		
		CartPoleDomain cpd = new CartPoleDomain();
		Domain domain = cpd.generateDomain();
		RewardFunction rf = new CartPoleDomain.CartPoleRewardFunction(Math.PI/2.-0.01);
		TerminalFunction tf = new CartPoleDomain.CartPoleTerminalFunction(Math.PI/2.-0.01);
		State initialState = CartPoleDomain.getInitialState(domain);
		
		FourierBasis fb = new FourierBasis(new CartPoleFV(), 4);
		LinearVFA vfa = (LinearVFA)fb.generateVFA(0.);
		
		GradientDescentSarsaLam sarsa = new GradientDescentSarsaLam(domain, rf, tf, 0.95, vfa, 0.01, 0.5);
		
		EpisodeAnalysis ea = null;
		for(int i = 0; i < 10000; i++){
			ea = sarsa.runLearningEpisodeFrom(initialState, 2000);
			System.out.println("Episode " + i + " ran for " + ea.numTimeSteps());
		}
		
		StateParser sp = new CartPoleStateParser(domain);
		ea.writeToFile("cartPole/sarsaPlan", sp);
		
		Visualizer v = CartPoleVisualizer.getCartPoleVisualizer();
		new EpisodeSequenceVisualizer(v, domain, sp, "cartPole");

	}

	
	public static void PCartPole(){
		CartPoleDomain cpd = new CartPoleDomain();
		Domain domain = cpd.generateDomain();
		RewardFunction rf = new CartPoleDomain.CartPoleRewardFunction(Math.PI/2.-0.01);
		TerminalFunction tf = new CartPoleDomain.CartPoleTerminalFunction(Math.PI/2.-0.01);
		State initialState = CartPoleDomain.getInitialState(domain);
		
		Policy p = new CartPoleBaselinePolicy(domain);
		
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rf, tf, 500);
		StateParser sp = new CartPoleStateParser(domain);
		
		ea.writeToFile("cartPole/baselinePlan", sp);
		
		Visualizer v = CartPoleVisualizer.getCartPoleVisualizer();
		new EpisodeSequenceVisualizer(v, domain, sp, "cartPole");
	}
	
	public static class CartPoleFV implements StateToFeatureVectorGenerator{

		@Override
		public double[] generateFeatureVectorFrom(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(CartPoleDomain.CLASSCARTPOLE);
			double ang = agent.getRealValForAttribute(CartPoleDomain.ATTANGLE);
			double anv = agent.getRealValForAttribute(CartPoleDomain.ATTANGLEV);
			
			ObjectClass oclass = agent.getObjectClass();
			Attribute angAtt = oclass.getAttribute(CartPoleDomain.ATTANGLE);
			Attribute angVAtt = oclass.getAttribute(CartPoleDomain.ATTANGLEV);
			
			double normedAng = (ang - angAtt.lowerLim) / (angAtt.upperLim - angAtt.lowerLim);
			double normedAngV = (anv - angVAtt.lowerLim) / (angVAtt.upperLim - angVAtt.lowerLim);
			
			double [] fv = new double[]{normedAng, normedAngV};
			
			return fv;
		}
		
		
		
	}
	
	
	public static class CartPoleBaselinePolicy extends Policy{

		GroundedAction right;
		GroundedAction left;
		
		
		public CartPoleBaselinePolicy(Domain domain){
			this.right = new GroundedAction(domain.getAction(CartPoleDomain.ACTIONRIGHT), "");
			this.left = new GroundedAction(domain.getAction(CartPoleDomain.ACTIONLEFT), "");
		}
		
		@Override
		public AbstractGroundedAction getAction(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(CartPoleDomain.CLASSCARTPOLE);
			double ang = agent.getRealValForAttribute(CartPoleDomain.ATTANGLE);
			if(ang > 0.){
				return right;
			}
			return left;
			
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			return this.getDeterministicPolicy(s);
		}

		@Override
		public boolean isStochastic() {
			return false;
		}

		@Override
		public boolean isDefinedFor(State s) {
			return true;
		}
		
		
		
	}
	
	
	
	public static class CPVInit implements ValueFunctionInitialization{

		@Override
		public double value(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(CartPoleDomain.CLASSCARTPOLE);
			double ang = Math.abs(agent.getRealValForAttribute(CartPoleDomain.ATTANGLE));
			double adist = -ang/(Math.PI/2.);

			return adist;
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return this.value(s);
		}
		
		
		
	}
	
	
	
}
