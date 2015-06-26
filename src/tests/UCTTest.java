package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.montecarlo.uct.UCT;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.*;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.visualizer.Visualizer;

/**
 * @author James MacGlashan.
 */
public class UCTTest {

	public static void main(String [] args){

		//create domain
		GridWorldDomain gwd = new GridWorldDomain(5, 5);
		gwd.makeEmptyMap();
		Domain domain = gwd.generateDomain();

		//set up task to solve in the domain
		GridWorldRewardFunction rf = new GridWorldRewardFunction(domain, 0);
		rf.setReward(4, 4, 1.);
		TerminalFunction tf = new GridWorldTerminalFunction(4, 4);
		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		//set up planner/policy
		//uct using a planning horizon of 12, 1000 rollouts each time it plans, and an exploration bias constant of 2
		UCT uct = new UCT(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 12, 1000, 2);
		GreedyQPolicy p = new GreedyQPolicy(uct);
		uct.toggleDebugPrinting(false);

		//run it
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 30);
		System.out.println(ea.getActionSequenceString("\n"));

		//save results and view it
		StateParser sp = new GridWorldStateParser(domain);
		ea.writeToFile("uct/policy", sp);
		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, "uct");


	}


}
