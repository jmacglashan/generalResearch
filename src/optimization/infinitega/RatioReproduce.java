package optimization.infinitega;

import java.util.List;

public interface RatioReproduce {
	
	public RepResult ratioReproduce(List<GenomeRatioFitness> popDist);
}
