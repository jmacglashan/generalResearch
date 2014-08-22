package behavior.planning;

import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.PlannerDerivedPolicy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

public class FSSSPolicy extends Policy implements PlannerDerivedPolicy{

	FSSS planner;
	
	
	public FSSSPolicy(){
		
	}
	
	public FSSSPolicy(FSSS planner){
		this.planner = planner;
	}
	
	@Override
	public AbstractGroundedAction getAction(State s) {
		return this.planner.getAction(s).translateParameters(this.planner.getStoredStateRepresentation(s), s);
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		return this.getDeterministicPolicy(s);
	}

	@Override
	public boolean isStochastic() {
		return false;
	}

	@Override
	public boolean isDefinedFor(State s) {
		return true;
	}

	@Override
	public void setPlanner(OOMDPPlanner planner) {
		
		if(!(planner instanceof FSSS)){
			throw new RuntimeException("FSSSPolicy only accepts planners that are instances of FSSS");
		}
		
		this.planner = (FSSS)planner;
		
	}

}
