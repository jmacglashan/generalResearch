package optimization.crossentropy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import burlap.debugtools.DPrint;
import optimization.OVarStringRep;
import optimization.OptVariables;
import optimization.PopulationOptimization;
import optimization.VarEvaluaiton;
import optimization.VarFitnessPair;
import optimization.VariableClamper;
import optimization.VariableRandomGenerator;

public class CrossEntropy implements PopulationOptimization {

	protected MultiVariateNormalGenerator		distributionGenerator;
	protected int								dim;
	protected OptVariables						best;
	protected double							bestFitness;
	protected int								nEvals;
	protected int								debugCode;
	protected List <VarFitnessPair>				population;
	
	
	//modules
	protected VariableRandomGenerator			initialGenerator;
	protected VariableClamper					varClamp;
	protected VarEvaluaiton						evaluator;
	
	
	//parameters
	protected int								sampleSize;
	protected int								eliteSize;
	protected boolean							maintainAbsoluteBest;
	protected int								maxIterations;
	protected double							maxVar;
	
	
	
	public CrossEntropy(VarEvaluaiton evaluator, VariableRandomGenerator initialGen, VariableClamper clamp, int dim, int sampleSize, int eliteSize, int maxIterations, double maxVar){
		this.initCE(evaluator, initialGen, clamp, dim, sampleSize, eliteSize, maxIterations, maxVar, true);
	}
	
	public CrossEntropy(VarEvaluaiton evaluator, VariableRandomGenerator initialGen, VariableClamper clamp, int dim, int sampleSize, int eliteSize, int maxIterations, double maxVar, boolean maintainAbsoluteBest){
		this.initCE(evaluator, initialGen, clamp, dim, sampleSize, eliteSize, maxIterations, maxVar, maintainAbsoluteBest);
	}
	
	
	protected void initCE(VarEvaluaiton evaluator, VariableRandomGenerator initialGen, VariableClamper clamp, int dim, int sampleSize, int eliteSize, int maxIterations, double maxVar, boolean maintainAbsoluteBest){
		
		this.evaluator = evaluator;
		this.initialGenerator = initialGen;
		this.varClamp = clamp;
		
		this.dim = dim;
		this.sampleSize = sampleSize;
		this.eliteSize = eliteSize;
		this.maxIterations = maxIterations;
		this.maxVar = maxVar;
		this.maintainAbsoluteBest = maintainAbsoluteBest;
		
		this.debugCode = 0;
		
	}
	
	@Override
	public void optimize() {
		
		nEvals = 0;
		
		//seed the initial sample
		List<OptVariables> sampleSet = new ArrayList<OptVariables>(sampleSize);
		for(int i = 0; i < sampleSize; i++){
			OptVariables s = initialGenerator.getVars(dim);
			varClamp.clamp(s);
			sampleSet.add(s);
		}
		
		population = VarFitnessPair.getPairList(sampleSet, evaluator.evaluate(sampleSet));
		List <VarFitnessPair> elite = this.getElite(population);
		nEvals += sampleSize;
		VarFitnessPair bpair = elite.get(elite.size()-1);
		best = bpair.var;
		bestFitness = bpair.fitness;
		
		
		DPrint.cl(debugCode, nEvals + ": " + bestFitness);
		
		MeanCovariance mc = new MeanCovariance(elite);
		distributionGenerator = new MultiVariateNormalGenerator(mc.means, mc.covariance);
		
		int iterations = 1;
		double maxVar = this.maxVariance(mc);
		while(iterations < this.maxIterations && maxVar > this.maxVar){
			
			sampleSet = new ArrayList<OptVariables>(sampleSize);
			for(int i = 0; i < sampleSize; i++){
				OptVariables s = distributionGenerator.getVars(dim); //generate from new distribution
				varClamp.clamp(s);
				sampleSet.add(s);
			}
			
			population = VarFitnessPair.getPairList(sampleSet, evaluator.evaluate(sampleSet));
			elite = this.getElite(population);
			nEvals += sampleSize;
			bpair = elite.get(elite.size()-1);
			if(bpair.fitness > bestFitness || !maintainAbsoluteBest){
				best = bpair.var;
				bestFitness = bpair.fitness;
			}
			DPrint.cl(debugCode, nEvals + ": " + bestFitness);
			
			mc = new MeanCovariance(elite);
			distributionGenerator.setParams(mc.means, mc.covariance);
			
			iterations++;
			maxVar = this.maxVariance(mc);
			
		}

	}

	@Override
	public OptVariables getBest() {
		return best;
	}

	@Override
	public double getBestFitness() {
		return bestFitness;
	}
	
	@Override
	public List<VarFitnessPair> getPopulation() {
		return population;
	}
	
	
	protected List <VarFitnessPair> getElite(List <VarFitnessPair> pairs){
		
		
		Collections.sort(pairs);
		
		List <VarFitnessPair> elite = new ArrayList<VarFitnessPair>(eliteSize);
		for(int i = pairs.size() - eliteSize; i < pairs.size(); i++){
			elite.add(pairs.get(i));
		}
		
		
		return elite;
	}
	
	
	//gets the max single variable variance (just the diagonal)
	protected double maxVariance(MeanCovariance mc){
		double m = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < mc.means.length; i++){
			if(mc.covariance[i][i] > m){
				m = mc.covariance[i][i];
			}
		}
		return m;
	}
	
	
	
	
	
	protected class MeanCovariance{
		
		double [] means;
		double [][] covariance;
		
		public MeanCovariance(List <VarFitnessPair> sample){
			
			this.computeMeans(sample);
			this.computeCovariance(sample);
			
		}
		
		private void computeMeans(List <VarFitnessPair> sample){
			
			int m = sample.get(0).var.size();
			
			means = new double[m];
			for(int i = 0; i < m; i++){
				means[0] = 0.; //initialize
			}
			
			int n = sample.size();
			for(int i = 0; i < n; i++){
				double [] v = sample.get(i).var.vars;
				for(int j = 0; j < m; j++){
					double nm = means[j] + v[j]/n;
					if(Double.isNaN(nm)){
						System.out.println("problem");
					}
					means[j] += v[j]/n;
				}
			}
			
		}
		
		private void computeCovariance(List <VarFitnessPair> sample){
			
			int m = means.length;
			covariance = new double[m][m];
			//initialize
			for(int i = 0; i < m; i++){
				for(int j = 0; j < m; j++){
					covariance[i][j] = 0.;
				}
			}
			
			int n = sample.size();
			for(int i = 0; i < n; i++){
				double [] v = sample.get(i).var.vars;
				double [] d = this.diff(v, means);
				for(int j = 0; j < m; j++){
					for(int k = 0; k < m; k++){
						//if(j == k){
							covariance[j][k] += (d[j]*d[k]) / (n); //measure independence because the jstat package for sampling the distribution seems to easily fail with small correlated variables :-/
						//}
					}
				}
			}
			
		}
		
		
		private double [] diff(double [] a, double [] b){
			double [] d = new double[a.length];
			for(int i = 0; i < d.length; i++){
				d[i] = a[i] - b[i];
			}
			
			return d;
		}
		
		
	}





	@Override
	public void enableOptimzationFileRecording(int recordMode, OVarStringRep rep,
			String outputPath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableOptimizationFileRecording() {
		// TODO Auto-generated method stub
		
	}





	

}
