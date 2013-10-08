package optimization.geneticalgorithm.gamodules;

import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import optimization.OptVariables;
import optimization.VariableRandomGenerator;
import optimization.geneticalgorithm.Mutator;

public class RandomOrganismMutate implements Mutator {
	
	protected VariableRandomGenerator				varGen;
	protected Random								rand;
	
	public RandomOrganismMutate(VariableRandomGenerator varGen) {
		this.varGen = varGen;
		this.rand = RandomFactory.getMapped(0);
	}

	@Override
	public void mutate(List<OptVariables> instances, double mutationRate) {
		
		for(int i = 0; i < instances.size(); i++){
			int m = instances.get(i).size();
			double roll = rand.nextDouble();
			if(roll < mutationRate){
				OptVariables inst = varGen.getVars(m);
				instances.set(i, inst);
			}
		}

	}

}
