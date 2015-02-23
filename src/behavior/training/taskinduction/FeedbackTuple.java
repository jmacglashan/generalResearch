package behavior.training.taskinduction;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

/**
 * @author James MacGlashan.
 */
public class FeedbackTuple {

	public State s;
	public GroundedAction a;
	public double feedback;

	public FeedbackTuple(State s, GroundedAction a, double feedback){
		this.s = s;
		this.a = a;
		this.feedback = feedback;
	}

}
