package behavior.training.taskinduction;

import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class TaskDescription{
	
	public RewardFunction rf;
	public TerminalFunction tf;
	
	public TaskDescription(){
		
	}
	
	public TaskDescription(RewardFunction rf, TerminalFunction tf) {
		this.rf = rf;
		this.tf = tf;
	}
	
}
