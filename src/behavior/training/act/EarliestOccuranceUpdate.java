package behavior.training.act;

import java.util.Map;

import behavior.training.TrainerModel;
import behavior.training.act.ExpectedTrainerSATIRF.ActionReward;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class EarliestOccuranceUpdate implements TrainerModel {

	protected ExpectedTrainerSATIRF		modeledRF;
	
	public EarliestOccuranceUpdate(StateHashFactory hashingFactory) {
		modeledRF = new ExpectedTrainerSATIRF(hashingFactory);
	}

	@Override
	public int updateWithFeedback(int curTime, double feedback, State s, GroundedAction ga, State sprime) {
		
		int updateState = 0;
		
		if(feedback == 0.){
			return 0; //nothing to update
		}
		
		Map<Integer, ActionReward> timeIndexedReward = this.modeledRF.getTimeStampReward(s, ga);
		
		int earliestTime = Integer.MAX_VALUE;
		ActionReward ar = null;
		for(Map.Entry<Integer, ActionReward> e : timeIndexedReward.entrySet()){
			if(e.getKey() < earliestTime){
				earliestTime = e.getKey();
				ar = e.getValue();
			}
		}
		
		if(curTime < earliestTime){
			if(ar != null){
				ar.r = 0.;
				updateState = 1;
			}
			modeledRF.setReward(curTime, s, ga, feedback);
		}
		else if(curTime == earliestTime){
			ar.r = feedback;
		}
		
		
		return updateState;

	}

	@Override
	public RewardFunction getTrainerRewardFunction() {
		return modeledRF;
	}

	@Override
	public int updateWithoutFeedback(int curTime, State s, GroundedAction ga, State sprime) {
		
		int updateState = 0;
		
		return 0;
	}

}
