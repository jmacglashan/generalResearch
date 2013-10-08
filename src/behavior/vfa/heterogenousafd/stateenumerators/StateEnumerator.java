package behavior.vfa.heterogenousafd.stateenumerators;

import burlap.oomdp.core.State;

public interface StateEnumerator {
	public EnumeratedState getEnumeratedStateValue(State s);
}
