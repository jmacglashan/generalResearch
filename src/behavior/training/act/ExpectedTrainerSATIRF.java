package behavior.training.act;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class ExpectedTrainerSATIRF extends TimeIndexedRF{

	protected StateHashFactory											hashingFactory;
	
	protected Map<StateHashTuple, Map<Integer, StateActionNode>>		rewardFunction;
	protected double													defaultReward = 0.;
	
	public ExpectedTrainerSATIRF(StateHashFactory hashingFactory) {
		this.hashingFactory = hashingFactory;
		this.rewardFunction = new HashMap<StateHashTuple, Map<Integer,StateActionNode>>();
	}
	

	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		
		ActionReward ar = this.getActionRewardFor(curTime, s, a);
		if(ar != null){
			return ar.r;
		}
		
		return defaultReward;
	}

	
	public ActionReward getActionRewardFor(int timeIndex, State s, GroundedAction ga){
		return this.getActionRewardFor(timeIndex, this.hashingFactory.hashState(s), ga);
	}
	
	public ActionReward getActionRewardFor(int timeIndex, StateHashTuple sh, GroundedAction ga){
		Map<Integer,StateActionNode> timeIndexed = this.rewardFunction.get(sh);
		if(timeIndexed == null){
			return null;
		}
		StateActionNode sanode = timeIndexed.get(timeIndex);
		if(sanode == null){
			return null;
		}
		
		ActionReward ar = sanode.getActionReward(sh, ga);
		return ar;
		
	}
	
	public Map <Integer, ActionReward> getTimeStampReward(State s, GroundedAction ga){
		Map <Integer, ActionReward> result = new HashMap<Integer, ExpectedTrainerSATIRF.ActionReward>();
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		Map<Integer,StateActionNode> timeIndexed = this.rewardFunction.get(sh);
		if(timeIndexed != null){
			for(Map.Entry<Integer, StateActionNode> e : timeIndexed.entrySet()){
				StateActionNode san = e.getValue();
				ActionReward ar = san.getActionReward(sh, ga);
				if(ar != null){
					result.put(san.timeIndex, ar);
				}
			}
		}
		
		
		return result;
	}
	
	public void setReward(int timeIndex, State s, GroundedAction ga, double r){
		StateHashTuple sh = this.hashingFactory.hashState(s);
		Map<Integer,StateActionNode> timeIndexed = this.rewardFunction.get(sh);
		if(timeIndexed == null){
			timeIndexed = new HashMap<Integer, ExpectedTrainerSATIRF.StateActionNode>();
			this.rewardFunction.put(sh, timeIndexed);
		}
		
		StateActionNode san = timeIndexed.get(timeIndex);
		if(san == null){
			san = new StateActionNode(sh, timeIndex);
			timeIndexed.put(timeIndex, san);
		}
		ActionReward ar = san.getActionReward(sh, ga);
		if(ar == null){
			ar = new ActionReward(ga, r);
			san.addActionReward(sh, ar);
		}
		else{
			ar.r = r;
		}
		
	}
	
	
	
	public class StateActionNode{
		
		public StateHashTuple		sh;
		public int					timeIndex;
		public List<ActionReward>	actionRewards;
		
		
		
		public StateActionNode(StateHashTuple sh, int timeIndex){
			this.sh = sh;
			this.timeIndex = timeIndex;
			this.actionRewards = new ArrayList<ExpectedTrainerSATIRF.ActionReward>();
		}
		
		public void addActionReward(StateHashTuple queryState, GroundedAction ga, double r){
			if(ga.params.length > 0){
				throw new RuntimeErrorException(new Error("StateActionNode currently does not support parameterized actions"));
			}
			this.actionRewards.add(new ActionReward(ga, r));
		}
		
		public void addActionReward(StateHashTuple queryState, ActionReward ar){
			if(ar.ga.params.length > 0){
				throw new RuntimeErrorException(new Error("StateActionNode currently does not support parameterized actions"));
			}
			this.actionRewards.add(ar);
		}
		
		
		public ActionReward getActionReward(StateHashTuple queryState, GroundedAction queryAction){
			
			if(queryAction.params.length > 0){
				throw new RuntimeErrorException(new Error("StateActionNode currently does not support parameterized actions"));
			}
			
			for(ActionReward ar : this.actionRewards){
				if(ar.ga.equals(queryAction)){
					return ar;
				}
			}
			
			return null;
		}
		
		
		@Override
		public boolean equals(Object other){
			if(this == other){
				return true;
			}
			
			if(!(other instanceof StateActionNode)){
				return false;
			}
			
			StateActionNode that = (StateActionNode)other;
			return this.sh.equals((that.sh));
		}
		
		@Override
		public int hashCode(){
			return sh.hashCode();
		}
		
		
	}
	
	
	public class ActionReward{
		
		public GroundedAction		ga;
		public double				r;
		
		
		public ActionReward(GroundedAction ga, double r){
			this.ga = ga;
			this.r = r;
		}
		
		
	}
	
}
