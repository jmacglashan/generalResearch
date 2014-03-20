package optimization.infinitega.modules;

import java.util.ArrayList;
import java.util.List;

import optimization.infinitega.GenomeRatio;
import optimization.infinitega.GenomeRatioFitness;
import optimization.infinitega.RatioReproduce;
import optimization.infinitega.RepResult;
import burlap.datastructures.BoltzmannDistribution;
import burlap.debugtools.DPrint;

public class InfGASoftMaxReproduce implements RatioReproduce {

	public static final	int		debugCode = 94723;
	
	protected boolean			normalize = false;
	protected double			temperature = 1.;
	protected boolean			usePopulationRatio = true;
	
	
	public InfGASoftMaxReproduce(double temp){
		this.temperature = temp;
	}
	
	public InfGASoftMaxReproduce(double temp, boolean normalize){
		this.temperature = temp;
		this.normalize = normalize;
	}
	
	@Override
	public RepResult ratioReproduce(List<GenomeRatioFitness> popDist) {
		
		double [] fitArray = this.getFitnessArray(popDist);
		BoltzmannDistribution bd = new BoltzmannDistribution(fitArray, this.temperature);
		double [] fitProb = bd.getProbabilities();
		
		RepResult res = new RepResult(0., new ArrayList<GenomeRatioFitness>(popDist));
		double sumPopIncrease = 0.;
		for(int i = 0; i < fitProb.length; i++){
			//double children = fitProb[i] * popDist.get(i).gr.ratio;
			double children = fitProb[i];
			if(this.usePopulationRatio){
				children *= popDist.get(i).gr.ratio;
			}
			sumPopIncrease += children;
			GenomeRatio gr = res.nextPop.get(i).gr;
			gr.ratio += children;
			
			DPrint.cf(debugCode, "%.3f %.2f\n", fitProb[i], res.nextPop.get(i).fitness);
		}
		
		res.repChange = sumPopIncrease;
		
		//renormalize
		double newSum = 0.;
		for(int i = 0; i < popDist.size(); i++){
			res.nextPop.get(i).gr.ratio /= (1. + sumPopIncrease);
			newSum += res.nextPop.get(i).gr.ratio;
		}
		
		if(Math.abs(1. - newSum) > 0.000001){
			throw new RuntimeException("New population doesn't sum to 1...");
		}
		
		return res;
	}
	
	
	protected double[] getFitnessArray(List<GenomeRatioFitness> pop){
		
		double [] res = new double[pop.size()];
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		for(int i = 0; i < pop.size(); i++){
			double f = pop.get(i).fitness;
			res[i] = f;
			max = Math.max(max, f);
			min = Math.min(min, f);
		}
		
		double range = max - min;
		
		if(this.normalize){
			for(int i = 0; i < res.length; i++){
				res[i] = (res[i] - min) / range;
			}
		}
		
		return res;
		
	}

}
