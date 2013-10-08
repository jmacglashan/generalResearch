package optimization.geneticalgorithm.gamodules;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import burlap.debugtools.RandomFactory;
import optimization.geneticalgorithm.KillSelector;

public class RandomKiller implements KillSelector {

	protected Random rand;
	
	public RandomKiller() {
		rand = RandomFactory.getMapped(0);
	}

	@Override
	public Set<Integer> selectInstancesToKill(List<Double> fitness) {
		return this.selectInstancesToKill(fitness, 1);
	}

	@Override
	public Set<Integer> selectInstancesToKill(List<Double> fitness, int n) {
		Set <Integer> selected = new HashSet<Integer>(n);
		
		while(selected.size() < n){
			selected.add(rand.nextInt(fitness.size()));
		}
		
		return selected;
	}

}
