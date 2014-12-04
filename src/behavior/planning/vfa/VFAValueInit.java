package behavior.planning.vfa;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.vfa.ActionApproximationResult;
import burlap.behavior.singleagent.vfa.ApproximationResult;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class VFAValueInit implements ValueFunctionInitialization{

	protected ValueFunctionApproximation vfa;


	public VFAValueInit(ValueFunctionApproximation vfa){
		this.vfa = vfa;
	}

	@Override
	public double value(State s) {

		ApproximationResult approx = this.vfa.getStateValue(s);
		return approx.predictedValue;
	}

	@Override
	public double qValue(State s, AbstractGroundedAction a) {

		List<GroundedAction> wrapper = new ArrayList<GroundedAction>(1);
		wrapper.add((GroundedAction)a);
		ActionApproximationResult approx = this.vfa.getStateActionValues(s, wrapper).get(0);

		return approx.approximationResult.predictedValue;
	}
}
