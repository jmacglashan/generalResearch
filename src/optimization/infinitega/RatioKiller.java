package optimization.infinitega;

import java.util.List;

public interface RatioKiller {
	public void kill(List<GenomeRatioFitness> oldPop, RepResult nextPop);
}
