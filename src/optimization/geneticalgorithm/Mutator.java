package optimization.geneticalgorithm;

import java.util.List;

import optimization.OptVariables;

public interface Mutator {
	public void mutate(List<OptVariables> instances, double mutationRate);
}
