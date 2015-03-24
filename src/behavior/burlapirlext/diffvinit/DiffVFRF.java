package behavior.burlapirlext.diffvinit;

import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

/**
 * @author James MacGlashan.
 */
public class DiffVFRF extends DifferentiableRF{

	protected RewardFunction objectiveRF;
	protected DifferentiableVInit.ParamedDiffVInit diffVInit;


	public DiffVFRF(RewardFunction objectiveRF, DifferentiableVInit.ParamedDiffVInit diffVinit){
		this.objectiveRF = objectiveRF;
		this.diffVInit = diffVinit;

		this.dim = diffVinit.getParameterDimension();
		this.parameters = diffVinit.getParameters();
	}

	@Override
	public double[] getGradient(State s, GroundedAction ga, State sp) {
		return new double[this.dim];
	}

	@Override
	protected DifferentiableRF copyHelper() {
		return null;
	}

	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		return this.objectiveRF.reward(s, a, sprime);
	}


	@Override
	public void setParameters(double[] parameters) {
		super.setParameters(parameters);
		this.diffVInit.setParameters(parameters);
	}




}
