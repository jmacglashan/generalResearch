package behavior.training.act;

import java.util.Map;

import behavior.training.act.ExpectedTrainerSATIRF.ActionReward;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class EarliestStatePropagationUpdate extends EarliestOccuranceUpdate {

	public EarliestStatePropagationUpdate(StateHashFactory hashingFactory) {
		super(hashingFactory);
	}
	
	@Override
	public int updateWithFeedback(int curTime, double feedback, State s, GroundedAction ga, State sprime) {
		
		int updateState = 0;
		
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
				if(ar.r != 0. && feedback == 0.){
					feedback = ar.r;
				}
				ar.r = 0.;
				updateState = 1;
			}
			
			if(feedback != 0.){
				modeledRF.setReward(curTime, s, ga, feedback);
			}
		}
		else if(curTime == earliestTime){
			ar.r = feedback;
		}
		

		return updateState;
		
	}
	
	
	@Override
	public int updateWithoutFeedback(int curTime, State s, GroundedAction ga, State sprime) {
		
		int updateState = 0;
		Map<Integer, ActionReward> timeIndexedReward = this.modeledRF.getTimeStampReward(s, ga);
		
		int earliestTime = Integer.MAX_VALUE;
		ActionReward ar = null;
		for(Map.Entry<Integer, ActionReward> e : timeIndexedReward.entrySet()){
			if(e.getKey() < earliestTime){
				earliestTime = e.getKey();
				ar = e.getValue();
			}
		}
		
		double feedback = 0.;
		if(curTime < earliestTime){
			
			if(ar != null){
				if(ar.r != 0.){
					feedback = ar.r;
					ar.r = 0.;
					updateState = 1;
					modeledRF.setReward(curTime, s, ga, feedback);
				}
				
			}
			
		}
		

		return updateState;
	}


}
