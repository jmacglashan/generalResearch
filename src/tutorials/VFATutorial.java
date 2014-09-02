package tutorials;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.lspi.LSPI;
import burlap.behavior.singleagent.learning.lspi.SARSCollector;
import burlap.behavior.singleagent.learning.lspi.SARSData;
import burlap.behavior.singleagent.learning.lspi.SARSData.SARS;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.behavior.singleagent.vfa.fourier.FourierBasis;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.domain.singleagent.cartpole.CartPoleDomain;
import burlap.domain.singleagent.cartpole.CartPoleStateParser;
import burlap.domain.singleagent.cartpole.CartPoleVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.visualizer.Visualizer;

public class VFATutorial {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//SSCartPole();
		LSPICartPole();
		//SARSACartPole();
		//PCartPole();

	}

	
	public static void SSCartPole(){
		
		CartPoleDomain cpd = new CartPoleDomain();
		cpd.isFiniteTrack = false;
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
		cpd.isFiniteTrack = false;
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
