package optimization;

import java.util.ArrayList;
import java.util.List;


public class VarFitnessPair implements Comparable<VarFitnessPair>{

	public OptVariables var;
	public Double fitness;
	
	public VarFitnessPair(OptVariables var, double fitness){
		this.var = var;
		this.fitness = fitness;
	}
	
	@Override
	public int compareTo(VarFitnessPair o) {
		return fitness.compareTo(o.fitness);
	}
	
	
	public static List<VarFitnessPair> getPairList(List <OptVariables> vars, List<Double> fitness){
		
		List <VarFitnessPair> pairs = new ArrayList<VarFitnessPair>(vars.size());
		for(int i = 0; i < vars.size(); i++){
			pairs.add(new VarFitnessPair(vars.get(i), fitness.get(i)));
		}
		
		return pairs;
		
	}
	
}
