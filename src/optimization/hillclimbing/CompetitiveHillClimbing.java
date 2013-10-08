package optimization.hillclimbing;

import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import optimization.Optimization;
import optimization.VarEvaluaiton;
import optimization.VariableClamper;
import optimization.VariableRandomGenerator;

public class CompetitiveHillClimbing extends NPointHillClimbing {

	
	public CompetitiveHillClimbing(VarEvaluaiton evaluator, VariableRandomGenerator varGen, VariableClamper clamper,
			NeighborhoodGenerator neighborGen, int nPoints, int dim) {
		super(evaluator, varGen, clamper, neighborGen, nPoints, dim);
	}
	
	public CompetitiveHillClimbing(VarEvaluaiton evaluator, VariableRandomGenerator varGen, VariableClamper clamper, NeighborhoodGenerator neighborGen, int nPoints, int dim,
			int nIters, int nRestarts){
		super(evaluator, varGen, clamper, neighborGen, nPoints, dim, nIters, nRestarts);
	}

	@Override
	public void optimize() {
		
		nFEvals = 0;
		
		pointFitness = evaluator.evaluate(evalPoints); //initialize fitness
		nFEvals++;
		bestFitness = Double.NEGATIVE_INFINITY;
		this.updateBestFitness();
		int numRestarts = 0;
		
		do{
			
			//optimize until a local optimum is reached
			List <OptVariables> nextEvalPoints = new ArrayList<OptVariables>(nPoints);
			boolean somePointChanged = false;
			for(int i = 0; i < nPoints; i++){
				
				OptVariables point = evalPoints.get(i);
				double pFitness = pointFitness.get(i);
				List <OptVariables> compareAgainst = this.getAllEvalPointsExcept(i);
				
				int numUnchanged = 0;
				do{
					
					List <OptVariables> neighbors = this.neighborhoodGenerators.get(i).neighborhood(point);
					this.clampPoints(neighbors);
					int bestN = -1;
					double bestFit = pFitness;
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
						point = neighbors.get(bestN);
						pFitness = bestFit;
						numUnchanged = 0;
						somePointChanged = true;
					}
					else{
						numUnchanged++;
					}
					
					System.out.println("Climb on " + i + " " + pFitness);
					
					
				}while(numUnchanged < nIterationsUntilStop);
				
				nextEvalPoints.add(point);
				
			}
			
			this.printStats();
			
			if(!somePointChanged){
				numRestarts++;
				
				if(numRestarts <= nRestarts){
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
					
					this.printStats();
				}
				
			}
			else{
				this.evalPoints = nextEvalPoints;
				this.pointFitness = this.evaluator.evaluate(evalPoints);
				nFEvals++;
				
			}
			
			this.updateBestFitness();
			
		}while(numRestarts <= nRestarts);
		

	}

	

}
