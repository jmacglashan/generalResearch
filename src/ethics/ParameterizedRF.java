package ethics;

import burlap.oomdp.stochasticgames.JointReward;

public interface ParameterizedRF extends JointReward {

	public void setParameters(double [] params);
	public int parameterSize();
	public double [] getParameters();
	public void printParameters();

}
