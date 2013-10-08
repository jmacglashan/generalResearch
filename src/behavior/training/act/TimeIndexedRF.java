package behavior.training.act;

import burlap.oomdp.singleagent.RewardFunction;

public abstract class TimeIndexedRF implements RewardFunction {

	protected int curTime = 0;
	
	public TimeIndexedRF() {
		
	}
	
	public void setCurTime(int curTime){
		this.curTime = curTime;
	}

	

}
