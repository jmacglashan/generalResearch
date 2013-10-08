package behavior.vfa.heterogenousafd.stateenumerators;

import burlap.oomdp.core.State;

public class EnumeratedState {

	public State s;
	public int enumeratedValue;
	
	public EnumeratedState(State s, int enumeratedValue) {
		this.s = s;
		this.enumeratedValue = enumeratedValue;
	}

	@Override
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(!(other instanceof EnumeratedState)){
			return false;
		}
		
		return this.enumeratedValue == ((EnumeratedState)other).enumeratedValue;
	}
	
	@Override
	public int hashCode(){
		return this.enumeratedValue;
	}
}
