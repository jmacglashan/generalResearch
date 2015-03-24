package behavior.burlapirlext.diffvinit;

import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

/**
 * @author James MacGlashan.
 */
public class LinearStateDiffVF extends DifferentiableVInit.ParamedDiffVInit {

	StateToFeatureVectorGenerator fvgen;


	public LinearStateDiffVF(StateToFeatureVectorGenerator fvgen, int dim){
		this.dim = dim;
		this.parameters = new double[dim];
		this.fvgen = fvgen;
	}

	@Override
	public double[] getVGradient(State s) {
		return this.fvgen.generateFeatureVectorFrom(s);
	}

	@Override
	public double[] getQGradient(State s, AbstractGroundedAction ga) {
		return this.fvgen.generateFeatureVectorFrom(s);
	}

	@Override
	public double value(State s) {

		double [] features = this.fvgen.generateFeatureVectorFrom(s);

		double sum = 0.;
		for(int i = 0; i < features.length; i++){
			sum += features[i] * this.parameters[i];
		}
		return sum;
	}

	@Override
	public double qValue(State s, AbstractGroundedAction a) {
		return this.value(s);
	}
}
