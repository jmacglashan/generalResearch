package behavior.vfa.heterogenousafd.stateenumerators;

import java.util.HashMap;
import java.util.Map;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.State;

public class HashingFactoryEnumerator implements StateEnumerator {

	protected StateHashFactory								hashingFactory;
	protected Map<StateHashTuple, EnumeratedState>			enumeration;
	
	public HashingFactoryEnumerator(StateHashFactory hashingFactory) {
		this.hashingFactory = hashingFactory;
		this.enumeration = new HashMap<StateHashTuple, EnumeratedState>();
	}

	@Override
	public EnumeratedState getEnumeratedStateValue(State s) {
		StateHashTuple sh = hashingFactory.hashState(s);
		EnumeratedState enumerated = enumeration.get(sh);
		if(enumerated == null){
			enumerated = new EnumeratedState(s, enumeration.size());
			//System.out.println("enum: " + enumerated.enumeratedValue);
			enumeration.put(sh, enumerated);
		}
		
		return enumerated;
	}

}
