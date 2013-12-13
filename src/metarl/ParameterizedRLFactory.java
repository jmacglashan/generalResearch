package metarl;

import burlap.behavior.singleagent.learning.LearningAgent;

public interface ParameterizedRLFactory {


		
	public int nParams();
	public double [] paramLowerLimits();
	public double [] paramUpperLimits();
	
	public LearningAgent generateLearningAgentWithParams(double [] params);
	
	
}
