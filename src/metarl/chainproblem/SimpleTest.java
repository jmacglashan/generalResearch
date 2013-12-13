package metarl.chainproblem;

import java.util.ArrayList;
import java.util.List;

import metarl.EnvironmentAndTask;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class SimpleTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ChainGenerator cg = new ChainGenerator(new int[]{0,1,2,3,4}, 0.2);
		EnvironmentAndTask et = cg.generateChainET();
		AlgorithmFactory algg = new AlgorithmFactory(0.1, 0.1, constantDoubleArray(5, 2, 0.));
		QLearning ql = algg.generateAlgorithm(et, 1000);
		EpisodeAnalysis ea = ql.runLearningEpisodeFrom(et.initialStateGenerator.generateState());
		System.out.println(ea.getDiscountedReturn(1.));
		System.out.println(ea.getActionSequenceString("\n"));
		System.out.println("----------");
		
		Policy opt = new OptimalPolicy(et.domain);
		EpisodeAnalysis eaO = opt.evaluateBehavior(et.initialStateGenerator.generateState(), et.rf, 1000);
		System.out.println("optimal policy score: " + eaO.getDiscountedReturn(1.));
		
		ValueIteration vi = new ValueIteration(et.domain, et.rf, et.tf, et.discount, new DiscreteStateHashFactory(), 0.01, 200);
		vi.planFromState(et.initialStateGenerator.generateState());
		Policy vip = new GreedyQPolicy(vi);
		EpisodeAnalysis eav = vip.evaluateBehavior(et.initialStateGenerator.generateState(), et.rf, 1000);
		System.out.println("VI policy score: " + eav.getDiscountedReturn(1.));
		
		//System.out.println("VI to opt: " + isOptimalPolicy(et.domain, vip, opt));
		System.out.println("VI to QL: " + isOptimalPolicy(et.domain, vip, new GreedyQPolicy(ql)));
		

	}
	
	public static boolean isOptimalPolicy(Domain d, Policy opt, Policy query){
		
		for(int i = 0; i < 5; i++){
			State s = GraphDefinedDomain.getState(d, i);
			List<ActionProb> optDist = remove0Probs(opt.getActionDistributionForState(s));
			List<ActionProb> qDist = remove0Probs(query.getActionDistributionForState(s));
			if(optDist.size() != qDist.size()){
				return false;
			}
			for(ActionProb oap : optDist){
				boolean found = false;
				for(ActionProb qap : qDist){
					if(oap.ga.action.getName().equals(qap.ga.action.getName())){
						found = true;
						break;
					}
				}
				if(!found){
					return false;
				}
				
			}
		}
		
		return true;
	}
	
	
	public static List <ActionProb> remove0Probs(List <ActionProb> input){
		List <ActionProb> res = new ArrayList<Policy.ActionProb>(input.size());
		for(ActionProb ap : input){
			if(ap.pSelection > 0.){
				res.add(ap);
			}
		}
		return res;
	}
	
	public static double[][] constantDoubleArray(int r, int c, double v){
		double [][] m = new double[r][c];
		for(int i = 0; i < r; i++){
			for(int j = 0; j < c; j++){
				m[i][j] = v;
			}
		}
		return m;
	}
	
	
	public static class OptimalPolicy extends Policy{

		Domain domain;
		GroundedAction ga;
		
		public OptimalPolicy(Domain domain){
			this.domain = domain;
			ga = new GroundedAction(domain.getAction(GraphDefinedDomain.BASEACTIONNAME+0), "");
		}
		
		@Override
		public GroundedAction getAction(State s) {
			return ga;
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

}
