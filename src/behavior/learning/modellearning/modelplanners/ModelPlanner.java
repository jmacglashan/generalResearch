package behavior.learning.modellearning.modelplanners;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.State;

public interface ModelPlanner {
	
	public void initializePlannerIn(State s);
	public void modelChanged(State changedState);
	public Policy modelPlannedPolicy();
}
