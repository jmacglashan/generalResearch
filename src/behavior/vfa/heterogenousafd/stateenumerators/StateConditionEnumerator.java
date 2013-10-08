package behavior.vfa.heterogenousafd.stateenumerators;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.core.State;

public class StateConditionEnumerator implements StateEnumerator {

	protected List<StateConditionTest>		conditions;
	
	public StateConditionEnumerator() {
		this.conditions = new ArrayList<StateConditionTest>();
	}
	
	public void addStateCondition(StateConditionTest sct){
		this.conditions.add(sct);
	}

	@Override
	public EnumeratedState getEnumeratedStateValue(State s) {
		
		for(int i = 0; i < conditions.size(); i++){
			StateConditionTest sgt = conditions.get(i);
			if(sgt.satisfies(s)){
				return new EnumeratedState(s, i);
			}
		}
		
		return new EnumeratedState(s, conditions.size());
	}

}
