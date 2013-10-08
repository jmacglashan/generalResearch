package behavior.shaping;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;



public abstract class ShapedRewardFunction implements RewardFunction {

	protected RewardFunction		baseRF;
	

	
	public abstract double additiveReward(State s, GroundedAction a, State sprime);
	
	public ShapedRewardFunction(RewardFunction baseRF) {
		this.baseRF = baseRF;
	}

	
	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		return this.baseRF.reward(s, a, sprime) + this.additiveReward(s, a, sprime);
	}

	
	
	
	

}
