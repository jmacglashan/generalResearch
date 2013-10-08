package optimization.geneticalgorithm.gamodules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.management.RuntimeErrorException;

import burlap.datastructures.BoltzmannDistribution;
import burlap.debugtools.RandomFactory;
import optimization.geneticalgorithm.MateSelector;

public class SoftMaxSingleParentMateSelector implements MateSelector {

	protected double	temperature;
	
	
	public SoftMaxSingleParentMateSelector(double temperature) {
		this.temperature = temperature;
	}

	@Override
	public List<MatePair> selectMates(List<Double> fitness, int nmatePairs) {
		
		Set<Integer> selected = new HashSet<Integer>(nmatePairs);
		
		double [] normFit = this.getNormalized(fitness);
		BoltzmannDistribution bd = new BoltzmannDistribution(normFit, this.temperature);
		
		while(selected.size() < nmatePairs){
			
			int s = bd.sample();
			selected.add(s);
			
		}
		
		List <MatePair> parents = new ArrayList<MateSelector.MatePair>(nmatePairs);
		for(int p : selected){
			MatePair mp = new MatePair(p, p);
			parents.add(mp);
		}
		
		return parents;
		
		
	}
	
	
	protected double [] getNormalized(List <Double> fitness){
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		
		for(double f : fitness){
			if(f < min){
				min = f;
			}
			if(f > max){
				max = f;
			}
		}
		
		double r = max - min;
		
		double [] res = new double[fitness.size()];
		for(int i = 0; i < fitness.size(); i++){
			double f = fitness.get(i);
			double nf = (f - min)/r;
			if(r == 0. && (f - min) == 0.){
				nf = 0.;
			}
			res[i] = nf;
		}
		
		
		return res;
	}
	
	
}
