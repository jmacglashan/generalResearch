package domain.stocasticgames.foragesteal;

import java.util.List;

import burlap.oomdp.auxiliary.StateAbstraction;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class TBForageStealFAbstraction implements StateAbstraction {

	@Override
	public State abstraction(State s) {
		
		State sc = s.copy();
		List<ObjectInstance> falts = sc.getObjectsOfClass(TBForageSteal.CLASSFALT);
		for(ObjectInstance fa : falts){
			sc.removeObject(fa);
		}
		
		
		return sc;
	}

}
