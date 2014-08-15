package irl.mlirl;

import irl.mlirl.DifferentiableRF.LinearStateDifferentiableRF;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class MLIRLTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("LOG: " + Math.log(-0.07278339112565912));
		
		GridWorldDomain gwd = new GridWorldDomain(3, 3);
		gwd.makeEmptyMap();
		Domain domain = gwd.generateDomain();
		State s = GridWorldDomain.getOneAgentNoLocationState(domain);
		GridWorldDomain.setAgent(s, 0, 1);
		
		//make trajectory
		EpisodeAnalysis ea = new EpisodeAnalysis(s);
		
		GroundedAction tmpGA = new GroundedAction(domain.getAction(GridWorldDomain.ACTIONNORTH), "");
		State tmpS = tmpGA.executeIn(s);
		ea.recordTransitionTo(tmpGA, tmpS, 0.);
		
		tmpGA = new GroundedAction(domain.getAction(GridWorldDomain.ACTIONEAST), "");
		tmpS = tmpGA.executeIn(tmpS);
		ea.recordTransitionTo(tmpGA, tmpS, 0.);
		
		tmpS = tmpGA.executeIn(tmpS);
		ea.recordTransitionTo(tmpGA, tmpS, 0.);
		
		tmpGA = new GroundedAction(domain.getAction(GridWorldDomain.ACTIONSOUTH), "");
		tmpS = tmpGA.executeIn(tmpS);
		ea.recordTransitionTo(tmpGA, tmpS, 0.);
		
		
		
		
		//make bad test trajectory
		//make trajectory
		EpisodeAnalysis eaBad = new EpisodeAnalysis(s);
		
		tmpGA = new GroundedAction(domain.getAction(GridWorldDomain.ACTIONEAST), "");
		tmpS = tmpGA.executeIn(s);
		eaBad.recordTransitionTo(tmpGA, tmpS, 0.);
		
		tmpS = tmpGA.executeIn(tmpS);
		eaBad.recordTransitionTo(tmpGA, tmpS, 0.);
		

		
		
		
		
		
		
		
		
		
		
		
		List<EpisodeAnalysis> trajectories = new ArrayList<EpisodeAnalysis>();
		trajectories.add(ea);
		
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(new PuddleFV(new Point(2, 1), new Point(1, 1)), 2);
		rf.setParameters(new double[]{-1.22324615291754, 1.600363058210203});
		
		
		MLIRL mlirl = new MLIRL(rf, trajectories, domain, 0.99, 0.5, new DiscreteStateHashFactory());
		
		mlirl.runGradientAscent(0.1, 1);
		
		/*
		mlirl.runPlanner();
		System.out.println("Liklihood: " + Math.exp(mlirl.logLikelihoodOfTrajectory(ea)));
		
		System.out.println("Liklihood: " + Math.exp(mlirl.logLikelihoodOfTrajectory(eaBad)));
		*/
		
	}
	
	
	
	public static class PuddleFV implements StateToFeatureVectorGenerator{

		Point[] puddles;
		Point goal;
		
		public PuddleFV(Point goal, Point...puddles){
			this.goal = goal;
			this.puddles = puddles;
		}
		
		@Override
		public double[] generateFeatureVectorFrom(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int x = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int y = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
			
			double [] fv = new double[]{0., 0.};
			if(this.inPuddle(x, y)){
				fv[0] = 1.;
			}
			
			if(this.inGoal(x, y)){
				fv[1] = 1.;
			}
			
			return fv;
		}
		
		public boolean inGoal(int x, int y){
			return this.inPoint(x, y, this.goal);
		}
		
		public boolean inPuddle(int x, int y){
			for(Point p : puddles){
				if(this.inPoint(x, y, p)){
					return true;
				}
			}
			return false;
		}
		
		public boolean inPoint(int x, int y, Point p){
			return x==p.x && y==p.y;
		}
		
		
		
	}
	
	
	public static class Point{
		
		public int x;
		public int y;
		
		public Point(int x, int y){
			this.x = x;
			this.y = y;
		}
		
	}
}
