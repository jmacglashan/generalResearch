package optimization.geneticalgorithm;

import java.util.List;

import optimization.OptVariables;

public interface Mate {
	public List<OptVariables> mate(OptVariables m, OptVariables f, int nOffspring);
}
