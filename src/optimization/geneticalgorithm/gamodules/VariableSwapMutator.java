package optimization.geneticalgorithm.gamodules;

import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import optimization.OptVariables;
import optimization.VariableRandomGenerator;
import optimization.geneticalgorithm.Mutator;

public class VariableSwapMutator implements Mutator {

	protected VariableRandomGenerator				varGen;
	protected Random								rand;
	
	public VariableSwapMutator(VariableRandomGenerator varGen){
		this.varGen = varGen;
		this.rand = RandomFactory.getMapped(0);
	}
	
	
	@Override
	public void mutate(List<OptVariables> instances, double mutationRate) {
		
		int nVars = instances.get(0).size();
		
		int nVarSwap = (int)(mutationRate * instances.size() * nVars);
		for(int i = 0; i < nVarSwap; i++){
			int ind = rand.nextInt(instances.size());
			OptVariables instance = instances.get(ind);
			int vind = rand.nextInt(nVars);
			instance.set(vind, varGen.valueForVar(vind));
		}

	}

}
