package optimization.generateAndTest;

import java.util.ArrayList;
import java.util.List;

import optimization.OVarStringRep;
import optimization.OptVariables;
import optimization.Optimization;
import optimization.VarEvaluaiton;
import optimization.VariableClamper;
import optimization.VariableRandomGenerator;

public class GenerateAndTest implements Optimization {

	protected VarEvaluaiton					evaluator;
	protected VariableRandomGenerator		varGen;
	protected VariableClamper				clamper;
	
	protected int							numSamples;
	protected int							dim;
	
	protected OptVariables					best;
	protected double						bestFitness;
	
	
	public GenerateAndTest(VarEvaluaiton evaluator, VariableRandomGenerator varGen, VariableClamper clamper, int numSamples, int dim){
		this.evaluator = evaluator;
		this.varGen = varGen;
		this.clamper = clamper;
		this.numSamples = numSamples;
		this.dim = dim;
	}
	
	
	@Override
	public void optimize() {
		
		best = varGen.getVars(dim);
		clamper.clamp(best);
		bestFitness = evaluator.evaluate(this.listWrapper(best)).get(0);
		System.out.println("1: " + bestFitness);
		for(int i = 1; i < numSamples; i++){
			OptVariables vars = varGen.getVars(dim);
			clamper.clamp(vars);
			double fitness = evaluator.evaluate(this.listWrapper(vars)).get(0);
			if(fitness > bestFitness){
				bestFitness = fitness;
				best = vars;
			}
			System.out.println((i+1) + " " + bestFitness);
		}
		
	}

	protected List<OptVariables> listWrapper(OptVariables var){
		List<OptVariables> res = new ArrayList<OptVariables>(1);
		res.add(var);
		return res;
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
	public void enableOptimzationFileRecording(int recordMode, OVarStringRep rep,
			String outputPath) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void disableOptimizationFileRecording() {
		// TODO Auto-generated method stub
		
	}
	
	

}
