package optimization.hillclimbing;

import java.util.ArrayList;
import java.util.List;

import optimization.OVarStringRep;
import optimization.OptVariables;
import optimization.Optimization;
import optimization.VarEvaluaiton;
import optimization.VariableClamper;
import optimization.VariableRandomGenerator;

public class NPointHillClimbing implements Optimization {

	protected List<OptVariables>			evalPoints;
	protected List<Double>					pointFitness;
	
	protected OptVariables					best;
	protected double						bestFitness;
	
	protected int							dim;
	
	protected int							nFEvals;
	
	//modules
	protected VarEvaluaiton					evaluator;
	protected VariableRandomGenerator		varGen;
	protected VariableClamper				clamper;
	protected NeighborhoodGenerator			baseNeighborhoodGenerator;
	protected List <NeighborhoodGenerator>	neighborhoodGenerators;
	
	
	//parameters
	protected int							nPoints;
	protected int							nRestarts;
	protected int							nIterationsUntilStop;
	

	
	public NPointHillClimbing(VarEvaluaiton evaluator, VariableRandomGenerator varGen, VariableClamper clamper, NeighborhoodGenerator neighborGen, int nPoints, int dim){
		this.HCInit(evaluator, varGen, clamper, neighborGen, nPoints, dim, 2, 0);
	}
	
	
	public NPointHillClimbing(VarEvaluaiton evaluator, VariableRandomGenerator varGen, VariableClamper clamper, NeighborhoodGenerator neighborGen, int nPoints, int dim,
			int nIters, int nRestarts){
		this.HCInit(evaluator, varGen, clamper, neighborGen, nPoints, dim, nIters, nRestarts);
	}
	
	
	
	protected void HCInit(VarEvaluaiton evaluator, VariableRandomGenerator varGen, VariableClamper clamper, NeighborhoodGenerator neighborGen, int nPoints, int dim,
			int nIters, int nRestarts){
		
		this.evaluator = evaluator;
		this.varGen = varGen;
		this.clamper = clamper;
		this.baseNeighborhoodGenerator = neighborGen;
		this.nPoints = nPoints;
		this.dim = dim;
		this.nIterationsUntilStop = nIters;
		this.nRestarts = nRestarts;
		
		evalPoints = new ArrayList<OptVariables>(nPoints);
		neighborhoodGenerators = new ArrayList<NeighborhoodGenerator>(nPoints);
		
		
		for(int i = 0; i < nPoints; i++){
			evalPoints.add(varGen.getVars(dim));
			neighborhoodGenerators.add(baseNeighborhoodGenerator.copy());
		}
		
		this.clampPoints(evalPoints);
		
		
	}
	
	
	
	@Override
	public void optimize() {
		
		nFEvals = 0;
		
		pointFitness = evaluator.evaluate(evalPoints); //initialize fitness
		bestFitness = Double.NEGATIVE_INFINITY;
		this.updateBestFitness();
		
		
		
		int numUnChanged = 0;
		int numRestarts = 0;
		do{
			
			boolean changed = false;
			
			List <OptVariables> nextEvalPoints = new ArrayList<OptVariables>(nPoints);
			double bestFit = 0.;
			
			for(int i = 0; i < nPoints; i++){
				
				OptVariables point = evalPoints.get(i);
				double pFitness = pointFitness.get(i);
				List <OptVariables> compareAgainst = this.getAllEvalPointsExcept(i);
				
				List <OptVariables> neighbors = this.neighborhoodGenerators.get(i).neighborhood(point);
				this.clampPoints(neighbors);
				int bestN = -1;
				bestFit = pFitness;
				for(int j = 0; j < neighbors.size(); j++){
					OptVariables neighbor = neighbors.get(j);
					compareAgainst.add(neighbor);
					double neighborFitness = this.evaluator.evaluate(compareAgainst).get(nPoints-1);
					nFEvals++;
					if(neighborFitness > bestFit){
						bestN = j;
						bestFit = neighborFitness;
					}
					compareAgainst.remove(nPoints-1);
				}
				
				if(bestN != -1){
					 nextEvalPoints.add(neighbors.get(bestN));
					 changed = true;
				}
				else{
					nextEvalPoints.add(point);
				}
				
				this.neighborhoodGenerators.get(i).selectedNeighbor(bestN);
				
			}
			
			evalPoints = nextEvalPoints;
			
			if(changed){
				numUnChanged = 0;
				if(nPoints == 1){
					this.pointFitness.clear();
					this.pointFitness.add(bestFit);
				}
				else{
					this.pointFitness = evaluator.evaluate(evalPoints);
					nFEvals++;
				}
				
				this.updateBestFitness();
			}
			else{
				numUnChanged++;
			}
			
			if(numUnChanged >= nIterationsUntilStop){
				numUnChanged = 0;
				numRestarts++;
				if(numRestarts <= nRestarts){
					//then restart
					evalPoints = new ArrayList<OptVariables>(nPoints);
					neighborhoodGenerators = new ArrayList<NeighborhoodGenerator>(nPoints);
					for(int i = 0; i < nPoints; i++){
						OptVariables point = varGen.getVars(dim);
						clamper.clamp(point);
						evalPoints.add(point);
						neighborhoodGenerators.add(baseNeighborhoodGenerator.copy());
					}
					this.pointFitness = evaluator.evaluate(evalPoints);
					nFEvals++;
					this.updateBestFitness();
					
					
				}
			}
			
			this.printStats();
			
			
		}while(numRestarts <= nRestarts);
		

	}
	
	protected void updateBestFitness(){
		
		int bestInd = -1;
		for(int i = 0; i < nPoints; i++){
			double f = this.pointFitness.get(i);
			if(f > bestFitness){
				bestFitness = f;
				bestInd = i;
			}
		}
		
		if(bestInd != -1){
			best = evalPoints.get(bestInd);
		}
	}
	
	protected List <OptVariables> getAllEvalPointsExcept(int ind){
		
		List <OptVariables> res = new ArrayList<OptVariables>(nPoints);
		
		for(int i = 0; i < nPoints; i++){
			if(i != ind){
				res.add(evalPoints.get(i));
			}
		}
		
		return res;
	}
	
	
	protected void clampPoints(List <OptVariables> points){
		for(OptVariables p : points){
			clamper.clamp(p);
		}
	}
	
	
	public void printStats(){
		System.out.println("Num Function Evals: " + nFEvals);
		for(int i = 0; i < nPoints; i++){
			System.out.println(pointFitness.get(i) + "\t");
		}
		System.out.println("-----------------");
	}

	@Override
	public OptVariables getBest() {
		if(best == null){
			System.out.println("problem");
		}
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
