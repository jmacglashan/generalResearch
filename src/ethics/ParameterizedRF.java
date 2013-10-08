package ethics;

import burlap.oomdp.stocashticgames.JointReward;

public interface ParameterizedRF extends JointReward {

	public void setParameters(double [] params);
	public int parameterSize();
	public double [] getParameters();
	public void printParameters();

}
