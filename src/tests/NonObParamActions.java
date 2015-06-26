package tests;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;

public class NonObParamActions {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		GridWorldDomain dgen = new GridWorldDomain(11, 11);
		dgen.setMapToFourRooms();
		Domain d = dgen.generateDomain();
		new JumpAction(d, dgen.getMap());
		
		RewardFunction rf = new UniformCostRF();
		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		
		State initialState = GridWorldDomain.getOneAgentNoLocationState(d);
		GridWorldDomain.setAgent(initialState, 0, 0);
		
		QLearning agent = new QLearning(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 0., 1.);
		
		String outputPath = "outputNew/";
		
		StateParser sp = new StateYAMLParser(d);
		
		for(int i = 0; i < 200; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, 10000);
			ea.writeToFile(String.format(outputPath+"ep%03d", i), sp);
		}
		
		ValueIteration vi = new ValueIteration(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 0.01, 100);
		vi.planFromState(initialState);
		Policy ppolicy = new GreedyQPolicy(vi);
		EpisodeAnalysis ea = ppolicy.evaluateBehavior(initialState, rf, tf);
		ea.writeToFile(outputPath+"planResult", sp);
		
		Visualizer v = GridWorldVisualizer.getVisualizer(dgen.getMap());
		new EpisodeSequenceVisualizer(v, d, sp, outputPath);

		
	}
	
	
	public static class JumpAction extends Action{

		protected int [][] map;
		protected int width;
		protected int height;
		
		public JumpAction(Domain domain, int [][] map){
			super("jump", domain, "*Int*");
			this.map = map;
			this.width = map.length;
			this.height = map[0].length;
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			int yd = Integer.parseInt(params[0]);
			int xd = 0;
			
			ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			int nx = ax+xd;
			int ny = ay+yd;
			
			//hit wall, so do not change position
			if(nx < 0 || nx >= this.width || ny < 0 || ny >= this.height || this.map[nx][ny] == 1 ||
					(xd > 0 && (this.map[ax][ay] == 3 || this.map[ax][ay] == 4)) || (xd < 0 && (this.map[nx][ny] == 3 || this.map[nx][ny] == 4)) ||
					(yd > 0 && (this.map[ax][ay] == 2 || this.map[ax][ay] == 4)) || (yd < 0 && (this.map[nx][ny] == 2 || this.map[nx][ny] == 4)) ){
				nx = ax;
				ny = ay;
			}
			
			agent.setValue(GridWorldDomain.ATTX, nx);
			agent.setValue(GridWorldDomain.ATTY, ny);
			
			
			return s;
		}
		
		@Override
		public boolean parametersAreObjects(){
			return false;
		}
		
		
		@Override
		public List<GroundedAction> getAllApplicableGroundedActions(State s){
			
			List<GroundedAction> res = new ArrayList<GroundedAction>(2);
			res.add(new GroundedAction(this, new String[]{"2"}));
			res.add(new GroundedAction(this, new String[]{"3"}));
			
			return res;
			
		}
		
		
		
		
	}

}
