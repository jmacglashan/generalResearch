package behavior.training.experiments.interactive.soko;

import behavior.planning.DeterministicGoalDirectedPartialVI;
import behavior.planning.DynamicDVIPolicy;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class DDVIPG implements PolicyGenerator {

	@Override
	public Policy getPolicy(Domain domain, State initialState, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory) {
		DeterministicGoalDirectedPartialVI planner = new DeterministicGoalDirectedPartialVI(domain, rf, tf, 0.99, hashingFactory);
		planner.planFromState(initialState);
		Policy p = new DynamicDVIPolicy(planner, 0.0); //used to be 0.002
		return p;
	}



}
