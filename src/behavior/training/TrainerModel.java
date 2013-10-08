package behavior.training;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public interface TrainerModel {
	public int updateWithFeedback(int curTime, double feedback, State s, GroundedAction ga, State sprime);
	public int updateWithoutFeedback(int curTime, State s, GroundedAction ga, State sprime);
	public RewardFunction getTrainerRewardFunction();
}
