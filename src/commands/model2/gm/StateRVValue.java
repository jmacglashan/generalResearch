package commands.model2.gm;

import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class StateRVValue extends RVariableValue {

	public State			s;
	
	
	public StateRVValue(State s, RVariable owner){
		this.s = s;
		this.setOwner(owner);
	}
	
	
	@Override
	public boolean valueEquals(RVariableValue other) {
		
		if(this == other){
			return true;
		}
		
		if(!(other instanceof StateRVValue)){
			return false;
		}
		
		return ((StateRVValue)other).s.equals(this.s);
		
	}

	@Override
	public String stringRep() {
		
		
		return s.getCompleteStateDescription();
		
		//the other method would provide order invariance but is slower and unncessary since states will never be iterated over or be an index in a multinomial parameter
	}
	
	
	protected String orderInvariantStringRep(){
		
		List<List<ObjectInstance>> objectsByClass = s.getAllObjectsByTrueClass();
		
		List <String> orderedByClassAndString = new ArrayList<String>();
		int size = 0;
		for(List<ObjectInstance> objects : objectsByClass){
			
			List <String> sReps = new ArrayList<String>(objects.size());
			for(ObjectInstance o : objects){
				String rep = o.getObjectDescription();
				size += rep.length();
				sReps.add(rep);
			}
			
			Collections.sort(sReps); //sort for order invariance
			
			orderedByClassAndString.addAll(sReps);
			
		}
		
		StringBuffer buf = new StringBuffer(size+orderedByClassAndString.size());
		for(String or : orderedByClassAndString){
			buf.append(or).append("\n");
		}
		
		
		return buf.toString();
		
	}

}
