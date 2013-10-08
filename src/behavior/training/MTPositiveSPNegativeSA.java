package behavior.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class MTPositiveSPNegativeSA implements RewardFunction, TrainerModel{

	protected StateHashFactory						hashingFactory;
	
	protected Map<StateHashTuple, Double>			positiveRewards;
	protected Map<StateHashTuple, StateActionNode>	negativeRewards;
	
	
	
	public MTPositiveSPNegativeSA(StateHashFactory hashingFactory) {
		this.hashingFactory = hashingFactory;
		this.positiveRewards = new HashMap<StateHashTuple, Double>();
		this.negativeRewards = new HashMap<StateHashTuple, MTPositiveSPNegativeSA.StateActionNode>();
	}

	@Override
	public int updateWithFeedback(int curTime, double feedback, State s,
			GroundedAction ga, State sprime) {
		
		if(feedback > 0){
			StateHashTuple sh = this.hashingFactory.hashState(sprime);
			Double stored = this.positiveRewards.get(sh);
			if(stored == null || stored != feedback){
				this.positiveRewards.put(sh, feedback);
				return 1;
			}
			
		}
		else if(feedback < 0){
			
			StateHashTuple sh = this.hashingFactory.hashState(s);
			StateActionNode storedNode = this.negativeRewards.get(sh);
			if(storedNode == null){
				storedNode = new StateActionNode(sh);
				storedNode.addActionReward(sh, ga, feedback);
				this.negativeRewards.put(sh, storedNode);
				return 1;
			}
			else{
				ActionReward ar = storedNode.getActionReward(sh, ga);
				if(ar == null){
					storedNode.addActionReward(sh, ga, feedback);
					return 1;
				}
				else{
					double oldVal = ar.r;
					ar.r = feedback;
					if(oldVal != feedback){
						return 1;
					}
				}
			}
			
		}
		

		return 0;
	}

	@Override
	public int updateWithoutFeedback(int curTime, State s, GroundedAction ga, State sprime) {
		return 0; //do nothing
	}

	@Override
	public RewardFunction getTrainerRewardFunction() {
		return this;
	}
	
	
	public RewardFunction getNegativeRewardFunction(){
		return new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				return getNegativeReward(s, a);
			}
		};
	}
	
	public RewardFunction getPositiveRewardFunction(){
		return new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				return getPositiveReward(sprime);
			}
		};
	}
	
	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		
		double neg = this.getNegativeReward(s, a);
		double pos = this.getPositiveReward(sprime);
		
		
		return neg+pos;
	}
	
	
	
	
	protected double getNegativeReward(State s, GroundedAction ga){
		StateHashTuple sh = this.hashingFactory.hashState(s);
		StateActionNode storedNode = this.negativeRewards.get(sh);
		if(storedNode == null){
			return 0.;
		}
		ActionReward ar = storedNode.getActionReward(sh, ga);
		if(ar == null){
			return 0.;
		}
		return ar.r;
		
	}
	
	protected double getPositiveReward(State sprime){
		StateHashTuple sh = this.hashingFactory.hashState(sprime);
		Double stored = this.positiveRewards.get(sh);
		if(stored == null){
			return 0.;
		}
		return stored;
	}
	
	
	class StateActionNode{
		
		StateHashTuple 			sh;
		List<ActionReward> 		actionRewards;
		
		
		public StateActionNode(StateHashTuple sh){
			this.sh = sh;
			this.actionRewards = new ArrayList<MTPositiveSPNegativeSA.ActionReward>();
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
	
	
	class ActionReward{
		
		public GroundedAction		ga;
		public double				r;
		
		
		public ActionReward(GroundedAction ga, double r){
			this.ga = ga;
			this.r = r;
		}
		
		
	}


	

}
