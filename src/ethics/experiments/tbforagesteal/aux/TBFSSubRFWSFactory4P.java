package ethics.experiments.tbforagesteal.aux;

import burlap.oomdp.stochasticgames.JointReward;
import ethics.ParameterizedRF;
import ethics.ParameterizedRFFactory;

public class TBFSSubRFWSFactory4P implements ParameterizedRFFactory {

	JointReward objectiveRF;
	
	public TBFSSubRFWSFactory4P(JointReward objectiveRF) {
		this.objectiveRF = objectiveRF;
	}

	@Override
	public ParameterizedRF generateRF(double[] params) {
		return new TBFSSRFWS4Param(this.objectiveRF, params);
	}

	@Override
	public int parameterSize() {
		return 4;
	}

}
