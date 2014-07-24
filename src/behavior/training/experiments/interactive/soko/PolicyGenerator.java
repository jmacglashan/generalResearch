package behavior.training.experiments.interactive.soko;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public interface PolicyGenerator {
	public Policy getPolicy(Domain domain, State initialState, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory);
}
