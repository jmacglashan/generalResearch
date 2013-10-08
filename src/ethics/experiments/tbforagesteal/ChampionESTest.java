package ethics.experiments.tbforagesteal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation;

import optimization.OptVariables;

public class ChampionESTest {

	protected FullyCachedMatchEvaluation evaluator;
	protected double lower = -1.5;
	protected double upper = 2.5;
	protected double inc = 0.5;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2 && args.length != 1){
			System.out.println("Use format:\n\tcacheFile [subjectiveRF]");
			System.exit(-1);
		}
		
		
		ChampionESTest test = new ChampionESTest(args[0]);
		
		if(args.length == 2){
			OptVariables vars = ChampionESTest.parseIntoOptVariables(args[1]);
			test.compareAgainstNeighborhood(vars);
		}
		else{
			test.interactiveMode();
		}
		

	}

	
	public static OptVariables parseIntoOptVariables(String srep){
		String [] comps = srep.split(",");
		double [] vars = new double[comps.length];
		for(int i = 0; i < comps.length; i++){
			double v = Double.parseDouble(comps[i]);
			vars[i] = v;
		}
		
		OptVariables ovars = new OptVariables(vars);
		return ovars;
	}
	
	public ChampionESTest(String cachePath) {
		System.out.println("Loading Cache file...");
		evaluator = new FullyCachedMatchEvaluation(cachePath);
		System.out.println("Finished Loading Cache file.\n\n");
	}
	
	
	public void interactiveMode(){
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		do{
			
			System.out.println("Enter a reward funciton parameterization to test (sBias,psBias,ppBias)");
			try{
				line = reader.readLine();
				line.trim();
				if(line.equals("q")){
					break;
				}
				this.compareAgainstNeighborhood(ChampionESTest.parseIntoOptVariables(line));
				System.out.println("\n\n\n");
			}catch(Exception e){
				System.out.println(e);
			}
			
		}while(true);
		
		
	}
	
	public void compareAgainstNeighborhood(OptVariables var){
		
		List <OptVariables> allVars = new ArrayList<OptVariables>();
		allVars.add(var);
		List <OptVariables> neighborhood = this.getNeighborhood(var);
		allVars.addAll(neighborhood);
		
		System.out.println("----------------------------------\n- Report for " + var.toString() + "\n----------------------------------\n");
		
		System.out.println("Query vs. Neighbors");
		String queryKey = var.toString();
		int wins = 0;
		for(OptVariables n : neighborhood){
			String nKey = n.toString();
			double [] matchRes = this.evaluator.evaluateMatchWithAverage(queryKey, nKey);
			System.out.println(nKey + ": " + matchRes[0] + ", " + matchRes[1]);
			if(matchRes[0] >= matchRes[1]){
				wins++;
			}
		}
		System.out.println("Wins: " + wins + "; losses: " + (neighborhood.size() - wins));
		
		System.out.println("\n\nVersus Self");
		
		double [] qSelfRes = this.evaluator.evaluateMatchWithAverage(queryKey, queryKey);
		System.out.println(queryKey + ": " + qSelfRes[0]);
		for(OptVariables n : neighborhood){
			String nKey = n.toString();
			double [] matchRes = this.evaluator.evaluateMatchWithAverage(nKey, nKey);
			System.out.println(nKey + ": " + matchRes[0]);
		}
		
		
		System.out.println("\n\nPairwise Sum (no versus self)");
		this.pairWiseEval(var, allVars, false);
		
		System.out.println("\n\nPairwise Sum (with versus self)");
		this.pairWiseEval(var, allVars, true);
		
		
	}
	
	protected void pairWiseEval(OptVariables query, List<OptVariables> allVars, boolean selfPlay){
		
		
		this.evaluator.setSelfPlay(selfPlay);
		
		List <Double> performances = evaluator.evaluateWithAverage(allVars);
		
		List <VarPerformancePair> pairs = VarPerformancePair.combine(allVars, performances);
		Collections.sort(pairs);
		
		for(VarPerformancePair p : pairs){
			String highlight = "";
			if(p.var.toString().equals(query.toString())){
				highlight = " <";
			}
			System.out.println(p.var.toString() + ": " + p.performance + highlight);
		}
		
	}
	
	public List<OptVariables> getNeighborhood(OptVariables var){
		
		List <OptVariables> neighbors = new ArrayList<OptVariables>(var.size()*2);
		for(int i = 0; i < var.size(); i++){
			double v = var.v(i);
			if(v > this.lower){
				OptVariables n = new OptVariables(var);
				n.set(i, v-inc);
				neighbors.add(n);
			}
			if(v < this.upper){
				OptVariables n = new OptVariables(var);
				n.set(i, v+inc);
				neighbors.add(n);
			}
		}
		
		return neighbors;
	}
	
	
	static class VarPerformancePair implements Comparable<VarPerformancePair>{
		OptVariables var;
		Double performance;
		
		
		public static List <VarPerformancePair> combine(List <OptVariables> vars, List <Double> performances){
			List <VarPerformancePair> pairs = new ArrayList<ChampionESTest.VarPerformancePair>(vars.size());
			for(int i = 0; i < vars.size(); i++){
				VarPerformancePair pair = new VarPerformancePair(vars.get(i), performances.get(i));
				pairs.add(pair);
			}
			return pairs;
		}
		
		public VarPerformancePair(OptVariables var, Double performance){
			this.var = var;
			this.performance = performance;
		}
		
		
		@Override
		public int compareTo(VarPerformancePair o) {
			return -performance.compareTo(o.performance);
		}
	}
	
}
