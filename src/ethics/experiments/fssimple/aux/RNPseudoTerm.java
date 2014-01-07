package ethics.experiments.fssimple.aux;

import domain.stocasticgames.foragesteal.simple.FSSimple;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.core.State;

public class RNPseudoTerm implements StateConditionTest {

	@Override
	public boolean satisfies(State s) {
		return FSSimple.isRootNode(s);
	}

}
