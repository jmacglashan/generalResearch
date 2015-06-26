package classexercises.ex1;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class Exercise1 {

	Domain						domain;
	GridWorldDomain				gwd;
	
	
	public Exercise1() {
		
		gwd = new GridWorldDomain(4, 3);
		gwd.setObstacleInCell(1, 1);
		
		double [][] transitionDynamics = new double [][]{{0.8, 0., 0.1, 0.1},{0., 0.8, 0.1, 0.1},{0.1, 0.1, 0.8, 0.},{0.1, 0.1, 0., 0.8}};
		gwd.setTransitionDynamics(transitionDynamics);
		domain = gwd.generateDomain();
		
		
		
	}
	
	public void visualExplorer(){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwd.getMap());
		
		State s = GridWorldDomain.getOneAgentNLocationState(domain, 0);
		GridWorldDomain.setAgent(s, 0, 0);
		
		VisualExplorer exp = new VisualExplorer(domain, v, s);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		
		exp.initGUI();
		
		
	}
	
	public void evaluatePolicy(){
		
		TerminalFunction tf = new LocTF(3, 2, 3, 1);
		RewardFunction rf = new LocRF(3, 2, 3, 1, 1., -1., -0.5);
		
		Policy p = new Policy() {
			
			@Override
			public boolean isStochastic() {
				return false;
			}
			
			@Override
			public List<ActionProb> getActionDistributionForState(State s) {
				return this.getDeterministicPolicy(s);
			}
			
			@Override
			public GroundedAction getAction(State s) {
				return new GroundedAction(domain.getAction(GridWorldDomain.ACTIONNORTH), "");
			}
			
			@Override
			public boolean isDefinedFor(State s) {
				return true;
			}
		};
		
		State s = GridWorldDomain.getOneAgentNLocationState(domain, 0);
		GridWorldDomain.setAgent(s, 0, 0);
		
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 1000);
		
		double pReturn = ea.getDiscountedReturn(1.0);
		
		System.out.println(pReturn);
		
		ea.writeToFile("ex1Out/try", new GridWorldStateParser(domain));
		
		
		
	}
	
	public void visualizeEpisode(){
		EpisodeSequenceVisualizer esv = new EpisodeSequenceVisualizer(GridWorldVisualizer.getVisualizer(domain, gwd.getMap()), domain, new GridWorldStateParser(domain), "ex1Out");
	}
	
	
	
	class LocTF implements TerminalFunction{

		int gx,gy,px,py;
		
		public LocTF(int gx, int gy, int px, int py){
			this.gx = gx;
			this.gy = gy;
			this.px = px;
			this.py = py;
		}
		
		@Override
		public boolean isTerminal(State s) {
			
			ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			return (ax==this.gx && ay==this.gy) || (ax==this.px && ay==this.py);
		}
		

	}
	
	
	class LocRF implements RewardFunction{

		
		int gx,gy,px,py;
		double goalR,pitR,stepR;
		
		public LocRF(int gx, int gy, int px, int py, double goalR, double pitR, double stepR){
			this.gx = gx;
			this.gy = gy;
			this.px = px;
			this.py = py;
			
			this.goalR = goalR;
			this.pitR = pitR;
			this.stepR = stepR;
			
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			ObjectInstance agent = sprime.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			if(ax==this.gx && ay==this.gy){
				return goalR;
			}
			
			if(ax==this.px && ay==this.py){
				return pitR;
			}
			
			return stepR;
		}
		
		
		
		
	}
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

		Exercise1 ex = new Exercise1();
		//ex.visualExplorer();
		//ex.evaluatePolicy();
		ex.visualizeEpisode();
		
	}

}
