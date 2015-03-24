package behavior.burlapirlext.diffvinit;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

/**
 * @author James MacGlashan.
 */
public class ZeroGradientVinit implements DifferentiableVInit{

	protected ValueFunctionInitialization vinit;
	protected DifferentiableRF rf;

	public ZeroGradientVinit(ValueFunctionInitialization vinit, DifferentiableRF rf) {
		this.vinit = vinit;
		this.rf = rf;
	}

	@Override
	public double[] getVGradient(State s) {
		return new double[rf.getParameterDimension()];
	}

	@Override
	public double[] getQGradient(State s, AbstractGroundedAction ga) {
		return new double[rf.getParameterDimension()];
	}

	@Override
	public double value(State s) {
		return this.vinit.value(s);
	}

	@Override
	public double qValue(State s, AbstractGroundedAction a) {
		return this.vinit.qValue(s, a);
	}
}
