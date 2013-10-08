package optimization.geneticalgorithm.gamodules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import optimization.geneticalgorithm.KillSelector;

public class KillWorst implements KillSelector {

	double percent;
	
	public KillWorst(){
		this.percent = 0.;
	}
	
	public KillWorst(double percent){
		this.percent = percent;
	}
	
	@Override
	public Set<Integer> selectInstancesToKill(List<Double> fitness) {
		
		int nk = (int)(this.percent * fitness.size());
		return this.selectInstancesToKill(fitness, nk);
		
		
	}

	@Override
	public Set<Integer> selectInstancesToKill(List<Double> fitness, int n) {
		List <Double> sortedFitness = new ArrayList<Double>(fitness);
		Collections.sort(sortedFitness);
		
		
		
		Set <Integer> selections = new HashSet<Integer>();
		if(n-1 < 0){
			return selections; //nothing is killed off
		}
		
		
		double minFitness = sortedFitness.get(n-1);
		for(int i = 0; i < fitness.size(); i++){
			double f = fitness.get(i);
			if(f <= minFitness){
				selections.add(i);
				if(selections.size() >= n){
					break;
				}
			}
		}
		
		
		return selections;
	}

}
