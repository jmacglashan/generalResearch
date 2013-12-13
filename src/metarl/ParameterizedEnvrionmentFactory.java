package metarl;


public interface ParameterizedEnvrionmentFactory {

	public int nParams();
	public double [] paramLowerLimits();
	public double [] paramUpperLimits();
	
	public EnvironmentAndTask generatedEnvironment();
	
}
