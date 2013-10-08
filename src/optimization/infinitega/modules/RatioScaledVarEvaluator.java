package optimization.infinitega.modules;

import java.util.List;

import optimization.OptVariables;
import optimization.VarEvaluaiton;
import optimization.infinitega.GenomeRatio;
import optimization.infinitega.InfiniteGA;

public class RatioScaledVarEvaluator implements VarEvaluaiton {

	protected VarEvaluaiton			source;
	protected InfiniteGA			infGA;
	
	public RatioScaledVarEvaluator(VarEvaluaiton source){
		this.source = source;
	}
	
	public RatioScaledVarEvaluator(VarEvaluaiton source, InfiniteGA infGA){
		this.source = source;
		this.infGA = infGA;
	}
	
	public void setInfGA(InfiniteGA infGA){
		this.infGA = infGA;
	}
	
	@Override
	public List<Double> evaluate(List<OptVariables> instances) {
		
		List <Double> srcFitness = this.source.evaluate(instances);
		
		for(int i = 0; i < instances.size(); i++){
			GenomeRatio gr = this.infGA.ratioForGenome(i);
			srcFitness.set(i, srcFitness.get(i)*gr.ratio);
		}
		
		
		return srcFitness;
	}

}
