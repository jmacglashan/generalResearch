package commands.tests;

import burlap.oomdp.core.State;

/**
 * @author James MacGlashan.
 */
public class StateAndGoal {

	public State initialState;
	public String goal;

	public StateAndGoal(State initialState, String goal){
		this.initialState = initialState;
		this.goal = goal;
	}


}
