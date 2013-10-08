package optimization;

public interface Optimization {
	public void optimize();
	public OptVariables getBest();
	public double getBestFitness();
	public void enableOptimzationFileRecording(int recordMode, OVarStringRep rep, String outputPathDirectory);
	public void disableOptimizationFileRecording();
}
