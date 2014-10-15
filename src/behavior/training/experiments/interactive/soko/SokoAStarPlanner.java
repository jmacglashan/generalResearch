package behavior.training.experiments.interactive.soko;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.informed.Heuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;

import commands.model3.TrajectoryModule.ConjunctiveGroundedPropTF;
import domain.singleagent.sokoban2.Sokoban2Domain;

public class SokoAStarPlanner implements PolicyGenerator {


	@Override
	public Policy getPolicy(Domain domain, State initialState,
			RewardFunction rf, TerminalFunction tf,
			StateHashFactory hashingFactory) {
		TFGoalCondition gc = new TFGoalCondition(tf);
		Heuristic h = new NullHeuristic();
		
		AStar planner = new AStar(domain, new UniformCostRF(), gc, hashingFactory, this.getHeuristic(tf));
		DPrint.toggleCode(planner.getDebugCode(), false);
		planner.planFromState(initialState);
		
		Policy p = new DDPlannerPolicy(planner);
		
		return p;
	}
	
	protected Heuristic getHeuristic(TerminalFunction tf){
		
		if(!(tf instanceof ConjunctiveGroundedPropTF)){
			throw new RuntimeException("Error; terminal function is not correct type.");
		}
		
		ConjunctiveGroundedPropTF ctf = (ConjunctiveGroundedPropTF)tf;
		GroundedProp gp = ctf.gps.get(0);
		if(gp.pf.getName().equals(Sokoban2Domain.PFAGENTINROOM)){
			return new ToRoomHeuristic(gp.params[1]);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFAGENTINDOOR)){
			return new ToRoomHeuristic(gp.params[1], 0);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFBLOCKINROOM)){
			return new BlockToRoomHeuristic(gp.params[0], gp.params[1]);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFBLOCKINDOOR)){
			return new BlockToRoomHeuristic(gp.params[0], gp.params[1]);
		}
		
		throw new RuntimeException("No heuristic for task defined with: " + gp.toString());
	}
	
	
	protected int manDistance(int x0, int y0, int x1, int y1){
		return Math.abs(x0-x1) + Math.abs(y0-y1);
	}


	/**
	 * Manhatten distance to a room or door.
	 * @param x
	 * @param y
	 * @param l
	 * @param r
	 * @param b
	 * @param t
	 * @param delta set to 1 for rooms because boundaries are walls which are not sufficient to be in room; 0 for doors
	 * @return
	 */
	protected int toRoomManDistance(int x, int y, int l, int r, int b, int t, int delta){
		int dist = 0;
		
		//use +1s because boundaries define wall, which is not sufficient to be in the room
		if(x <= l){
			dist += l-x + delta;
		}
		else if(x >= r){
			dist += x - r + delta;
		}
		
		if(y <= b){
			dist += b - y + delta;
		}
		else if(y >= t){
			dist += y - t + delta;
		}
		
		return dist;
	}
	
	
	public class ToRoomHeuristic implements Heuristic{

		String roomName;
		int delta = 1;
		
		public ToRoomHeuristic(String roomName){
			this.roomName = roomName;
		}

		public ToRoomHeuristic(String roomName, int delta){
			this.roomName = roomName;
			this.delta = delta;
		}
		
		@Override
		public double h(State s) {
			
			//get the agent
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);
			
			//get room
			ObjectInstance room = s.getObject(this.roomName);
			int l = room.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
			int r = room.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);
			int b = room.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int t = room.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
			
			int dist = toRoomManDistance(ax, ay, l, r, b, t, this.delta);
			
			//make negative because of negative reward
			return -dist;
		}
		

	}
	
	
	public class BlockToRoomHeuristic implements Heuristic{

		protected String blockName;
		protected String roomName;

		protected int delta = 1;
		
		public BlockToRoomHeuristic(String blockName, String roomName){
			this.blockName = blockName;
			this.roomName = roomName;
		}

		public BlockToRoomHeuristic(String blockName, String roomName, int delta){
			this.blockName = blockName;
			this.roomName = roomName;
			this.delta = delta;
		}
		
		@Override
		public double h(State s) {
			
			//get the agent
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);
			
			//get room
			ObjectInstance room = s.getObject(this.roomName);
			int l = room.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
			int r = room.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);
			int b = room.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int t = room.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
			
			//get the block
			ObjectInstance block = s.getObject(this.blockName);
			int bx = block.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int by = block.getDiscValForAttribute(Sokoban2Domain.ATTY);
			
			int dist = manDistance(ax, ay, bx, by)-1; //need to be one step away from block to push it
			
			//and then block needs to be at room
			dist += toRoomManDistance(bx, by, l, r, b, t, this.delta);
			
			//make negative because of negative reward
			return -dist;
		}
		
		
		
		
	}

}
