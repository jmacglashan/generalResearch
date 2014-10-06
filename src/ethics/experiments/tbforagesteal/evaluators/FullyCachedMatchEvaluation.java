package ethics.experiments.tbforagesteal.evaluators;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import optimization.OptVariables;
import optimization.VarEvaluaiton;
import burlap.debugtools.RandomFactory;

public class FullyCachedMatchEvaluation implements VarEvaluaiton {

	protected Map<MatchKey, MatchPerformanceGenerator>				performanceGenerators;
	protected boolean												playAgainstSelf = false;													
	
	
	public static void main(String [] args){
		
		String path = "Results/cacheRes/cachelr5.txt";
		
		
		//1.5,-1.0,-0.5 -0.5,-2.0,-1.5
		
		System.out.println("Parsing File");
		FullyCachedMatchEvaluation me = new FullyCachedMatchEvaluation(path);
		System.out.println("Finished Parsing File");
		
		//String v1 = "0.0,0.0,0.0";
		//String v2 = "0.0,1.5,1.5";
		
		String v1 = "1.5,-1.0,-0.5";
		String v2 = "-0.5,-2.0,-1.5";
		
		for(int i = 0; i < 10; i++){
			double [] res = me.evaluateMatch(v1, v2);
			System.out.println(v1 + ": " + String.format("%.2f", res[0]) + "\t" + v2 + ": " + String.format("%.2f", res[1]));
		}
		
	}
	
	
	public FullyCachedMatchEvaluation(String cacheFilePath) {
		
		performanceGenerators = new HashMap<FullyCachedMatchEvaluation.MatchKey, FullyCachedMatchEvaluation.MatchPerformanceGenerator>(27000);
		this.loadCacheFile(cacheFilePath);
	}
	
	public FullyCachedMatchEvaluation(String cacheFilePath, boolean playAgainstSelf) {
		
		performanceGenerators = new HashMap<FullyCachedMatchEvaluation.MatchKey, FullyCachedMatchEvaluation.MatchPerformanceGenerator>(27000);
		this.loadCacheFile(cacheFilePath);
		this.playAgainstSelf = playAgainstSelf;
	}
	
	public void setSelfPlay(boolean selfPlay){
		this.playAgainstSelf = selfPlay;
	}

	@Override
	public List<Double> evaluate(List<OptVariables> instances) {
		
		int pairOffset = 1;
		if(this.playAgainstSelf){
			pairOffset = 0;
		}
		
		List <Double> sumRR = new ArrayList<Double>(instances.size());
		for(int i = 0; i < instances.size(); i++){
			sumRR.add(0.);
		}
		
		for(int i = 0; i < instances.size(); i++){
			OptVariables v1 = instances.get(i);
			for(int j = i+pairOffset; j < instances.size(); j++){
				OptVariables v2 = instances.get(j);
				
				String v1key = v1.toString();
				String v2key = v2.toString();
				
				double [] perf = this.evaluateMatch(v1key, v2key);
				
				double v1p = perf[0];
				double v2p = perf[1];
				
				sumRR.set(i, sumRR.get(i)+v1p);
				sumRR.set(j, sumRR.get(j)+v2p);
	
			}
			
		}
		
		return sumRR;
	}
	
	
	public List<Double> evaluateWithAverage(List<OptVariables> instances) {
		
		int pairOffset = 1;
		if(this.playAgainstSelf){
			pairOffset = 0;
		}
		
		List <Double> sumRR = new ArrayList<Double>(instances.size());
		for(int i = 0; i < instances.size(); i++){
			sumRR.add(0.);
		}
		
		for(int i = 0; i < instances.size(); i++){
			OptVariables v1 = instances.get(i);
			for(int j = i+pairOffset; j < instances.size(); j++){
				OptVariables v2 = instances.get(j);
				
				String v1key = v1.toString();
				String v2key = v2.toString();
				
				double [] perf = this.evaluateMatchWithAverage(v1key, v2key);
				
				double v1p = perf[0];
				double v2p = perf[1];
				
				sumRR.set(i, sumRR.get(i)+v1p);
				sumRR.set(j, sumRR.get(j)+v2p);
	
			}
			
		}
		
		return sumRR;
	}
	
	
	
	public double [] evaluateMatch(String v1key, String v2key){
		
		return this.evaluateMatchWithSample(v1key, v2key);
		
	}
	
	public double [] evaluateMatchWithSample(String v1key, String v2key){
		
		MatchKey key = new MatchKey(v1key, v2key);
		MatchPerformanceGenerator pgen = performanceGenerators.get(key);
		if(pgen == null){
			throw new RuntimeErrorException(new Error("Error; cache map did not have entry for: " + v1key + " " + v2key));
		}
		
		Map <String, Double> perf = pgen.samplePerformance();
		
		return this.performanceFromMap(v1key, v2key, perf);
		
	}
	
	public double [] evaluateMatchWithAverage(String v1key, String v2key){
		
		MatchKey key = new MatchKey(v1key, v2key);
		MatchPerformanceGenerator pgen = performanceGenerators.get(key);
		if(pgen == null){
			throw new RuntimeErrorException(new Error("Error; cache map did not have entry for: " + v1key + " " + v2key));
		}
		
		Map <String, Double> perf = pgen.averagePerformance();
		
		return this.performanceFromMap(v1key, v2key, perf);
		
	}
	
	public MatchPerformanceGenerator getAllMatchPerformanceDataFor(OptVariables v1, OptVariables v2){
		MatchKey key = new MatchKey(v1.toString(), v2.toString());
		return this.performanceGenerators.get(key);
	}
	
	
	
	protected double [] performanceFromMap(String v1key, String v2key, Map <String, Double> perf){
		double [] res = new double[2];
		if(!v1key.equals(v2key)){
			res[0] = perf.get(v1key);
			res[1] = perf.get(v2key);
		}
		else{
			res[0] = perf.get("A" + v1key);
			res[1] = perf.get("B" + v2key);
		}
		return res;
	}
	
	
	protected void loadCacheFile(String fpath){
		
		try{
			
			BufferedReader in = new BufferedReader(new FileReader(fpath));
			
			String line = in.readLine();
			while(line != null){
				
				//line is first line of 4 line performance pair
				String [] comps1 = line.split("::");
				String [] comps2 = comps1[0].split(";");
				
				String a1key = comps2[0];
				String a2key = comps2[1];
				
				//get performance results for a1 on next line
				line = in.readLine();
				String [] perf1Comps = line.split(",");
				
				List <Double> perf1 = new ArrayList<Double>(perf1Comps.length);
				for(String p : perf1Comps){
					perf1.add(Double.parseDouble(p));
				}
				
				//skip summary statistics of reverse direction on next line
				line = in.readLine();
				
				//grab performance for a2 on next line
				line = in.readLine();
				String [] perf2Comps = line.split(",");
				
				List <Double> perf2 = new ArrayList<Double>(perf2Comps.length);
				for(String p : perf2Comps){
					perf2.add(Double.parseDouble(p));
				}
				
				
				//index this information
				MatchKey mk = new MatchKey(a1key, a2key);
				MatchPerformanceGenerator perfGen = new MatchPerformanceGenerator(a1key, perf1, a2key, perf2);
				this.performanceGenerators.put(mk, perfGen);
				
				//grab line of next entry
				line = in.readLine();

			}
			
			
			
			in.close();
			
		}catch(Exception e){
			System.out.println(e);
			throw new RuntimeErrorException(new Error("Parsing cache file failed"));
		}
		
		
	}
	
	
	
	class MatchKey{
		
		String key;
		
		public MatchKey(OptVariables a1, OptVariables a2){
			
			String key1, key2;
			
			String a1s = a1.toString();
			String a2s = a2.toString();
			
			if(a1s.compareTo(a2s) <= 0){
				key1 = a1s;
				key2 = a2s;
			}
			else{
				key2 = a1s;
				key1 = a2s;
			}
			
			this.key = key1 + ';' + key2;
			
		}
		
		
		public MatchKey(String a1s, String a2s){
			
			String key1, key2;
			
			if(a1s.compareTo(a2s) <= 0){
				key1 = a1s;
				key2 = a2s;
			}
			else{
				key2 = a1s;
				key1 = a2s;
			}
			
			this.key = key1 + ';' + key2;
			
		}
		
		
		@Override
		public int hashCode(){
			return key.hashCode();
		}
		
		@Override
		public boolean equals(Object other){
			MatchKey that = (MatchKey)other;
			return this.key.equals(that.key);
		}
		
	}
	
	
	
	public class MatchPerformanceGenerator{
		
		public String [] agentIndices;
		public List <Double> performanceOfA1;
		public List <Double> performanceOfA2;
		
		Random rand;
		
		public MatchPerformanceGenerator(String a1key, List <Double> a1p, String a2key, List <Double> a2p){
			agentIndices = new String[2];
			agentIndices[0] = a1key;
			agentIndices[1] = a2key;
			
			if(a1key.equals(a2key)){
				agentIndices[0] = "A" + agentIndices[0];
				agentIndices[1] = "B" + agentIndices[1];
			}
			
			performanceOfA1 = new ArrayList<Double>(a1p);
			performanceOfA2 = new ArrayList<Double>(a2p);
			
			rand = RandomFactory.getMapped(0);
		}
		
		
		public Map <String, Double> samplePerformance(){
			Map <String, Double> pf = new TreeMap<String, Double>();
			int ind = rand.nextInt(performanceOfA1.size());
			pf.put(agentIndices[0], performanceOfA1.get(ind));
			pf.put(agentIndices[1], performanceOfA2.get(ind));
			
			return pf;
		}
		
		
		public Map <String, Double> averagePerformance(){
			
			Map <String, Double> pf = new TreeMap<String, Double>();
			pf.put(agentIndices[0], this.average(performanceOfA1));
			pf.put(agentIndices[1], this.average(performanceOfA2));
			
			return pf;
			
		}
		
		public double averagePerformance(int ind){
			if(ind == 0){
				return this.average(performanceOfA1);
			}
			return this.average(performanceOfA2);
		}
		
		public Map <String, List<Double>> allPerformances(){
			Map <String, List <Double>> allp = new TreeMap<String, List<Double>>();
			allp.put(agentIndices[0], performanceOfA1);
			allp.put(agentIndices[1], performanceOfA2);
			return allp;
		}
		
		
		protected double average(List <Double> performance){
			double sum = 0.;
			for(double d : performance){
				sum += d;
			}
			double avg = sum / performance.size();
			
			return avg;
		}
		
	}

}
