package ethics.experiments.tbforagesteal.matchcaching;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import optimization.OptVariables;
import ethics.experiments.tbforagesteal.aux.RFParamVarEnumerator;
import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation;
import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation.MatchPerformanceGenerator;

public class CacheQuery {

	protected FullyCachedMatchEvaluation 	evaluator;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		if(args.length != 1){
			System.out.println("Format:\n\tCacheQuery pathToCacheFile");
			System.exit(0);
		}
		
		CacheQuery q = new CacheQuery(args[0]);
		q.interactivePairwiseMode();
		

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
	
	public CacheQuery(String path){
		
		System.out.println("Loading Cache file...");
		evaluator = new FullyCachedMatchEvaluation(path);
		System.out.println("Finished Loading Cache file.\n\n");
		
	}
	
	public void printSummaryStats(){
		System.out.println("Computing Summary");
		RFParamVarEnumerator penum = new RFParamVarEnumerator();
		double maxPerformance = Double.NEGATIVE_INFINITY;
		double minPerformance = Double.POSITIVE_INFINITY;
		OptVariables maxRF = null;
		OptVariables minRF = null;
		for(OptVariables rf1 : penum.allRFs){
			double sumPerformance = 0.;
			for(OptVariables rf2 : penum.allRFs){
				sumPerformance += (this.avgPerformance(rf1, rf2));
			}
			if(sumPerformance > maxPerformance){
				maxPerformance = sumPerformance;
				maxRF = rf1;
			}
			if(sumPerformance < minPerformance){
				minPerformance = sumPerformance;
				minRF = rf1;
			}
		}
		
		System.out.println("Max: " + maxRF.toString() + ": " + maxPerformance);
		System.out.println("Min: " + minRF.toString() + ": " + minPerformance);
		
	}
	
	
	public void interactivePairwiseMode(){
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		do{
			
			System.out.println("Enter first reward funciton parameterization (sBias,psBias,ppBias) or \"summary\" for summary stats");
			try{
				line = reader.readLine();
				line.trim();
				if(line.equals("q")){
					break;
				}
				if(line.equals("summary")){
					this.printSummaryStats();
					continue;
				}
				OptVariables rf1 = CacheQuery.parseIntoOptVariables(line);
				
				System.out.println("Enter second reward funciton parameterization (sBias,psBias,ppBias)");
				line = reader.readLine();
				OptVariables rf2 = CacheQuery.parseIntoOptVariables(line);
				
				this.printPairwiseData(rf1, rf2);
				
				System.out.println("\n\n\n");
			}catch(Exception e){
				System.out.println(e);
			}
			
		}while(true);
		
		
	}
	
	public double avgPerformance(OptVariables rf1, OptVariables rf2){
		
		MatchPerformanceGenerator gen = this.evaluator.getAllMatchPerformanceDataFor(rf1, rf2);
		
		String k1 = rf1.toString();
		String k2 = rf2.toString();
		
		Map <String, Double> averages = gen.averagePerformance();
		Map <String, List<Double>> performances = gen.allPerformances();
		
		String k1a = k1;
		String k2a = k2;
		if(k1.equals(k2)){
			k1a = "A" + k1;
			k2a = "B" + k2;
		}
		
		return averages.get(k1a);
		
	}
	
	public void printPairwiseData(OptVariables rf1, OptVariables rf2){
		
		MatchPerformanceGenerator gen = this.evaluator.getAllMatchPerformanceDataFor(rf1, rf2);
		
		String k1 = rf1.toString();
		String k2 = rf2.toString();
		
		Map <String, Double> averages = gen.averagePerformance();
		Map <String, List<Double>> performances = gen.allPerformances();
		
		String k1a = k1;
		String k2a = k2;
		if(k1.equals(k2)){
			k1a = "A" + k1;
			k2a = "B" + k2;
		}
		
		
		System.out.println(k1 + " versus. " + k2);
		System.out.println("Aveage Performances:");
		System.out.println(averages.get(k1a) + " " + averages.get(k2a));
		
		List <Double> p1 = performances.get(k1a);
		List <Double> p2 = performances.get(k2a);
		
		System.out.println("Individual Match Performances");
		for(int i = 0; i < p1.size(); i++){
			System.out.print("(" + p1.get(i) + ", " + p2.get(i) + "); ");
		}
		System.out.println();
		
		
	}
	
	

}
