package optimization.hillclimbing;

import java.util.List;

import optimization.OptVariables;

public interface NeighborhoodGenerator {
	public List<OptVariables> neighborhood(OptVariables startPoint);
	public void selectedNeighbor(int i);
	public NeighborhoodGenerator copy();
}
