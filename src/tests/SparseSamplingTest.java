package tests;



import java.util.List;

import behavior.planning.FSSS;
import behavior.planning.FSSSPolicy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.MyTimer;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import domain.paintpolish.PaintPolish;
import domain.paintpolish.PaintPolish.PaintPolishRF;
import domain.paintpolish.PaintPolish.PaintPolishTF;

public class SparseSamplingTest {

	public static void main(String [] args){
		
		//ss();
		//fsss();
		//fsssSimple();
		ppComparison();
	}
	
	public static void ss(){
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		gwd.setProbSucceedTransitionDynamics(0.8);
		Domain d = gwd.generateDomain();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		RewardFunction rf = new UniformCostRF();
		
		SparseSampling ss = new SparseSampling(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 25, -1);
		Policy p = new GreedyQPolicy(ss);
		
		State s = GridWorldDomain.getOneAgentNoLocationState(d);
		GridWorldDomain.setAgent(s, 0, 0);
		
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 50);
		System.out.println(ea.getActionSequenceString("\n"));
		
		List<QValue> qs = ss.getQs(s);
		for(QValue q : qs){
			System.out.println(q.a.toString() + ": " + q.q);
		}
	}
	
	public static void fsss(){
		
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain d = gwd.generateDomain();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		RewardFunction rf = new UniformCostRF();
		
		FSSS fsss = new FSSS(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 21, 1, 0, -1);
		Policy p = new FSSSPolicy(fsss);
		
		State s = GridWorldDomain.getOneAgentNoLocationState(d);
		GridWorldDomain.setAgent(s, 0, 0);
		
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 50);
		System.out.println(ea.getActionSequenceString("\n"));
		
	}
	
	public static void fsssSimple(){
		
		RandomFactory.seedMapped(0, 727);
		
		GridWorldDomain gwd = new GridWorldDomain(3, 3);
		gwd.makeEmptyMap();
		Domain d = gwd.generateDomain();
		TerminalFunction tf = new GridWorldTerminalFunction(2, 2);
		RewardFunction rf = new UniformCostRF();
		
		FSSS fsss = new FSSS(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 5, 1, 0, -1);
		Policy p = new FSSSPolicy(fsss);
		
		State s = GridWorldDomain.getOneAgentNoLocationState(d);
		GridWorldDomain.setAgent(s, 0, 0);
		
		//System.out.println(p.getAction(s));
		
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 10);
		System.out.println(ea.getActionSequenceString("\n"));
		
	}
	
	
	public static void ppComparison(){
		
		//RandomFactory.seedMapped(0, 848);
		
		PaintPolish pp = new PaintPolish();
		Domain domain = pp.generateDomain();
		TerminalFunction tf = new PaintPolishTF();
		RewardFunction rf = new PaintPolishRF();
		//RewardFunction rf = new UniformCostRF();
		
		State s = PaintPolish.getInitialState(domain, 6);
		
		
		//SS
		SparseSampling ss = new SparseSampling(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 10, -1);
		Policy p = new GreedyQPolicy(ss);
		
		
		MyTimer ssTimer = new MyTimer();
		ssTimer.start();
		p.getAction(s);
		ssTimer.stop();
		System.out.println("Num SS State nodes: " + ss.getNumberOfStateNodesCreated());
		System.out.println("SS time: " + ssTimer.getTime());
		
		
		
		EpisodeAnalysis eass = p.evaluateBehavior(s, rf, tf, 50);
		System.out.println(eass.getActionSequenceString("\n"));
		System.out.println("Num SS Steps: " + eass.numTimeSteps());
		System.out.println("SS Average Reward: " + eass.getDiscountedReturn(1.));
		
		
		/*
		//FSSS
		FSSS fsss = new FSSS(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 10, 20, 10, -1);
		Policy fsp = new FSSSPolicy(fsss);
		
		MyTimer fsssTimer = new MyTimer();
		fsssTimer.start();
		fsp.getAction(s);
		fsssTimer.stop();
		System.out.println("Num FSSS State nodes: " + fsss.getNumberOfStateNodesCreated());
		System.out.println("FSSS time: " + fsssTimer.getTime());
		*/
		
		/*
		EpisodeAnalysis eafsss = fsp.evaluateBehavior(s, rf, tf, 50);
		System.out.println(eafsss.getActionSequenceString("\n"));
		System.out.println("Num FSSS Steps: " + eafsss.numTimeSteps());
		System.out.println("FSSS Average Reward: " + eafsss.getDiscountedReturn(1.));
		*/
		
		
	}
	
}
