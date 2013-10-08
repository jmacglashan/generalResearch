package ethics.experiments.tbforagesteal.evaluators;

import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import optimization.VarEvaluaiton;
import optimization.infinitega.GenomeRatio;
import optimization.infinitega.GenomeRatioFitness;
import optimization.infinitega.InfiniteGA;

public class InfGACachedVarEval implements VarEvaluaiton {

	protected FullyCachedMatchEvaluation			srcEval;
	protected InfiniteGA							infGA;
	
	
	public InfGACachedVarEval(String cacheFilePath){
		this.srcEval = new FullyCachedMatchEvaluation(cacheFilePath);
	}
	
	public void setInfGA(InfiniteGA infGA){
		this.infGA = infGA;
	}
	
	@Override
	public List<Double> evaluate(List<OptVariables> instances) {
		
		List <Double> sumRR = new ArrayList<Double>(instances.size());
		for(int i = 0; i < instances.size(); i++){
			sumRR.add(0.);
		}
		
		for(int i = 0; i < instances.size(); i++){
			
			GenomeRatio gr = this.infGA.ratioForGenome(i);
			if(gr.ratio > 0.){
				
				String v1key = gr.var.toString();
				
				for(int j = i; j < instances.size(); j++){
					GenomeRatio gr2 = this.infGA.ratioForGenome(j);
					if(gr2.ratio > 0.){
						
						String v2key = gr2.var.toString();
						
						double [] perf = this.srcEval.evaluateMatchWithAverage(v1key, v2key);
						
						sumRR.set(i, sumRR.get(i) + perf[0] * gr2.ratio);
						
						if(i != j){
							sumRR.set(j, sumRR.get(j) + perf[1] * gr.ratio);
						}
						
					}
					
				}
				
			}
			else{
				//since it doesn't exist, just give it the worst possible score
				sumRR.set(i, Double.NEGATIVE_INFINITY);
			}
			
		}
		
		return sumRR;
	}

}
