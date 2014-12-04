package behavior.AMDP.blacklistAffordance;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

/**
 * @author James MacGlashan.
 */
public abstract class AffordanceBlackList {

	public String associatedActionName;

	public AffordanceBlackList(String associatedActionName){
		this.associatedActionName = associatedActionName;
	}

	public abstract void setCurrentGoal(StateConditionTest gc);
	public abstract boolean filter(State s, GroundedAction ga);




	public static class NullAffordanceBlackList extends AffordanceBlackList{


		public NullAffordanceBlackList(String associatedActionName) {
			super(associatedActionName);
		}


		@Override
		public void setCurrentGoal(StateConditionTest gc) {
			//nothing
		}

		@Override
		public boolean filter(State s, GroundedAction ga) {
			return false;
		}
	}

}
