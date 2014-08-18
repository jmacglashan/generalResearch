package tests;



import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;

public class SparseSamplingTest {

	public static void main(String [] args){
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain d = gwd.generateDomain();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		RewardFunction rf = new UniformCostRF();
		
		SparseSampling ss = new SparseSampling(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 21, 1);
		Policy p = new GreedyQPolicy(ss);
		
		State s = GridWorldDomain.getOneAgentNoLocationState(d);
		GridWorldDomain.setAgent(s, 0, 0);
		
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 50);
		System.out.println(ea.getActionSequenceString("\n"));
		
	}
	
}
