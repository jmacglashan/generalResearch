package ethics.experiments.tbforagesteal.aux;

import burlap.oomdp.stocashticgames.JointReward;
import ethics.ParameterizedRF;
import ethics.ParameterizedRFFactory;

public class TBFSSubRFWSFactory implements ParameterizedRFFactory {

	JointReward objectiveRF;
	
	public TBFSSubRFWSFactory(JointReward objectiveRF){
		this.objectiveRF = objectiveRF;
	}

	@Override
	public ParameterizedRF generateRF(double[] params) {
		return new TBFSSubjectiveRFWS(objectiveRF, params);
	}

	@Override
	public int parameterSize() {
		return 3;
	}

}
