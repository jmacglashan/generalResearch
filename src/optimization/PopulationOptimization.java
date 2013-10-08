package optimization;

import java.util.List;

public interface PopulationOptimization extends Optimization {

	public List<VarFitnessPair> getPopulation();
	
}
