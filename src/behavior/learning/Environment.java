package behavior.learning;

import burlap.oomdp.core.State;


public abstract class Environment {

	protected State curState;
	
	
	public void setCurStateTo(State s){
		this.curState = s;
	}
	
	public State getCurState(){
		return curState;
	}
	
	public abstract State executeAction(String aname, String [] params);
	public abstract double getLastReward();
	public abstract boolean curStateIsTerminal();

}
