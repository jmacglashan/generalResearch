package ethics.experiments;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import optimization.OptVariables;
import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation;
import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation.MatchPerformanceGenerator;

public class CacheBrowser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1){
			System.out.println("Format:\n\tpathToCacheFile");
			System.exit(0);
		}
		
		String path = args[0];
		
		System.out.println("Parsing File");
		FullyCachedMatchEvaluation me = new FullyCachedMatchEvaluation(path);
		System.out.println("Finished Parsing File");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		do{
			
			VarComparison vars = getVarsFromInput();
			MatchPerformanceGenerator match = me.getAllMatchPerformanceDataFor(vars.v1, vars.v2);
			
			System.out.println("Summary: " + match.averagePerformance(0) + " vs. " + match.averagePerformance(1));
			System.out.println(vars.v1.toString() + "\n-----------------------------");
			for(Double d : match.performanceOfA1){
				System.out.println(d);
			}
			System.out.println("\n");
			System.out.println(vars.v2.toString() + "\n-----------------------------");
			for(Double d : match.performanceOfA2){
				System.out.println(d);
			}
			
			System.out.println("\n==============================\n\n");
			
			
		}while(true);
		

	}
	
	
	public static VarComparison getVarsFromInput(){
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		VarComparison result = null;
		
		try{
			
			System.out.println("Enter a reward funciton parameterization for first genome (sBias,pBias)");
			line = reader.readLine();
			line.trim();
			if(line.equals("q")){
				System.exit(0);
			}
			
			String sv1 = line;
			
			System.out.println("Enter a reward funciton parameterization for second genome (sBias,pBias)");
			line = reader.readLine();
			line.trim();
			
			String sv2 = line;
			
			result = new VarComparison(sv1, sv2);
			
			
		}catch(Exception e){
			System.out.println(e);
		}
		
		
		return result;
		
		
	}
	
	
	public static class VarComparison{
	
		public OptVariables v1;
		public OptVariables v2;
		
		public VarComparison(String sv1, String sv2){
			
			this.v1 = this.parseVars(sv1);
			this.v2 = this.parseVars(sv2);
		
		}
		
		
		protected OptVariables parseVars(String srep){
			
			String [] comps = srep.split(",");
			double [] vars = new double[comps.length];
			for(int i = 0; i < comps.length; i++){
				double v = Double.parseDouble(comps[i]);
				vars[i] = v;
			}
			
			OptVariables ovars = new OptVariables(vars);
			return ovars;
			
		}
		
	}

}
