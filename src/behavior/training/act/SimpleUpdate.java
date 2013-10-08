package behavior.training.act;

import behavior.training.TrainerModel;
import behavior.training.act.ExpectedTrainerSATIRF.ActionReward;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class SimpleUpdate implements TrainerModel {

	
	protected ExpectedTrainerSATIRF		modeledRF;
	
	public SimpleUpdate(StateHashFactory hashingFactory) {
		modeledRF = new ExpectedTrainerSATIRF(hashingFactory);
	}

	@Override
	public int updateWithFeedback(int curTime, double feedback, State s, GroundedAction ga, State sprime) {
		
		if(feedback == 0.){
			return 0; //nothing to update
		}
		
		ActionReward ar = modeledRF.getActionRewardFor(curTime, s, ga);
		if(ar == null){
			modeledRF.setReward(curTime, s, ga, feedback);
		}
		
		return 0;
		

	}

	@Override
	public RewardFunction getTrainerRewardFunction() {
		return modeledRF;
	}

	@Override
	public int updateWithoutFeedback(int curTime, State s, GroundedAction ga, State sprime) {
		return 0;
	}

}
