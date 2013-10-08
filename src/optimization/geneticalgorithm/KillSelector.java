package optimization.geneticalgorithm;

import java.util.List;
import java.util.Set;

public interface KillSelector {
	public Set <Integer> selectInstancesToKill(List <Double> fitness);
	public Set <Integer> selectInstancesToKill(List <Double> fitness, int n);
}
