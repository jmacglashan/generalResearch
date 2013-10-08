package optimization.geneticalgorithm.gamodules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import optimization.geneticalgorithm.MateSelector;

public class TopInstanceMateSelector implements MateSelector {

	protected Random 		rand;
	
	public TopInstanceMateSelector(){
		rand = RandomFactory.getMapped(0);
	}
	
	@Override
	public List<MatePair> selectMates(List<Double> fitness, int nmatePairs) {
		
		List <Double> sortedFitness = new ArrayList<Double>(fitness);
		Collections.sort(sortedFitness);
		
		List <Integer> selections = new ArrayList<Integer>();
		
		double minFitness = sortedFitness.get(sortedFitness.size()-(nmatePairs*2));
		for(int i = 0; i < fitness.size(); i++){
			double f = fitness.get(i);
			if(f >= minFitness){
				selections.add(i);
				if(selections.size() >= nmatePairs*2){
					break;
				}
			}
		}
		
		List <MatePair> mates = new ArrayList<MateSelector.MatePair>();
		
		//randomly choose matches
		while(selections.size() > 0){
			int ind = rand.nextInt(selections.size());
			int m = selections.get(ind);
			selections.remove(ind);
			
			ind = rand.nextInt(selections.size());
			int f = selections.get(ind);
			selections.remove(ind);
			
			mates.add(new MatePair(m, f));
		}
		
		
		return mates;
	}

}
