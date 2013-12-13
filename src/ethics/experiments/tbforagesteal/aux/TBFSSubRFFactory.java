package ethics.experiments.tbforagesteal.aux;

import burlap.oomdp.stochasticgames.JointReward;
import ethics.ParameterizedRF;
import ethics.ParameterizedRFFactory;

public class TBFSSubRFFactory implements ParameterizedRFFactory {

	JointReward objectiveRF;
	
	public TBFSSubRFFactory(JointReward objectiveRF){
		this.objectiveRF = objectiveRF;
	}
	
	@Override
	public ParameterizedRF generateRF(double[] params) {
		return new TBFSSubjectiveRF(objectiveRF, params);
	}

	@Override
	public int parameterSize() {
		return 3;
	}

}
