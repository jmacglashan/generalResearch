package ethics.experiments.tbforagesteal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation;

import optimization.OptVariables;

public class ESSS {

	protected FullyCachedMatchEvaluation 	evaluator;
	
	protected double 						lower = -1.5;
	protected double 						upper = 2.5;
	protected double 						inc = 0.5;
	protected List<OptVariables>			allRFs;
	protected Map <String, Integer>			rfToInd;
	
	protected Map<Integer, Set<Integer>>	invadedByMap;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1){
			System.out.println("Format:\n\tpathToCacheFile");
			System.exit(0);
		}
		
		
		double [] params = new double[] {0.0, 1.5, 0.0};
		OptVariables ov = new OptVariables(params);
		
		ESSS esss = new ESSS(args[0]);
		
		
		/*
		List <OptVariables> invadedBy = esss.getInvadedBySet(ov);
		System.out.println(ov.toString() + " is invaded by set of size " + invadedBy.size() + " and consists of:");
		for(OptVariables v : invadedBy){
			System.out.println(v.toString());
		}
		*/
		
		
		
		for(OptVariables v : esss.allRFs){
		
			
			List <OptVariables> varsInESSS = esss.getESSSContaining(v);
			if(varsInESSS == null){
				System.out.println("No ESSS containing " + v.toString());
			}
			else{
				System.out.println("ESSS of size " + varsInESSS.size() + " containing " + v.toString());
				if(varsInESSS.size() < 729){
					for(OptVariables vp : varsInESSS){
						System.out.println(vp.toString());
					}
				}
			}
			
		}
		

	}

	
	
	public ESSS(String cachePath) {
		System.out.println("Loading Cache file...");
		evaluator = new FullyCachedMatchEvaluation(cachePath);
		System.out.println("Finished Loading Cache file.\n\n");
		
		this.enumerateRFs();
		this.computedInvadedMap();
	}

	
	public List <OptVariables> getInvadedBySet(OptVariables v){
		
		int ind = this.rfToInd.get(v.toString());
		
		return this.convertIndexSetToVarsList(this.invadedByMap.get(ind));
		
	}
	
	public List <OptVariables> getESSSContaining(OptVariables v){
		
		int ind = this.rfToInd.get(v.toString());
		Set <Integer> esss = this.getESSSContaining(ind);
		
		if(esss == null){
			return null;
		}
		
		return this.convertIndexSetToVarsList(esss);
		
	}
	
	protected void enumerateRFs(){
		
		this.allRFs = new ArrayList<OptVariables>();
		this.rfToInd = new HashMap<String, Integer>();
		
		int n = (int)((upper-lower)/inc) + 1;
		for(int i = 0; i < n; i++){
			double pi = lower + inc*i;
			for(int j = 0; j < n; j++){
				double pj = lower + inc*j;
				for(int k = 0; k < n; k++){
					double pk = lower + inc*k;
					double [] params = new double []{pi, pj, pk};
					OptVariables vars = new OptVariables(params);
					rfToInd.put(vars.toString(), this.allRFs.size());
					this.allRFs.add(vars);
				}
			}
		}
		
		
		
	}
	
	
	protected void computedInvadedMap(){
		
		System.out.println("Computing invaded by set");
		
		//initialize
		this.invadedByMap = new HashMap<Integer, Set<Integer>>(this.allRFs.size());
		for(int i = 0; i < this.allRFs.size(); i++){
			Set<Integer> set = new HashSet<Integer>();
			this.invadedByMap.put(i, set);
		}
		
		for(int i = 0; i < this.allRFs.size(); i++){
			
			OptVariables v1 = this.allRFs.get(i);
			Set<Integer> v1set = this.invadedByMap.get(i);
			for(int j = 0; j < this.allRFs.size(); j++){
				if(i == j){
					continue;
				}
				OptVariables v2 = this.allRFs.get(j);
				if(!this.ESSPairTest(v1, v2)){
					v1set.add(j);
				}
			}
			
		}
		
		System.out.println("Finished computing invaded by set");
		
		
	}
	
	
	/**
	 * 
	 * @param v1
	 * @param v2
	 * @return returns whether v1 is invulnerable to invasion by v2
	 */
	protected boolean ESSPairTest(OptVariables v1, OptVariables v2){
		
		String s1 = v1.toString();
		String s2 = v2.toString();
		
		double es1s1 = this.evaluator.evaluateMatchWithAverage(s1, s1)[0];
		double [] s1s2Match = this.evaluator.evaluateMatch(s1, s2);
		double es2s1 = s1s2Match[1];
		
		if(es1s1 > es2s1){
			return true;
		}
		
		if(es1s1 == es2s1){
			
			double es1s2 = s1s2Match[0];
			double es2s2 = this.evaluator.evaluateMatch(s2, s2)[0];
			if(es1s2 > es2s2){
				return true;
			}
			
		}
		
		return false;
	}
	
	
	protected List <OptVariables> convertIndexSetToVarsList(Set <Integer> indices){
		
		List <OptVariables> res = new ArrayList<OptVariables>(indices.size());
		
		for(int i : indices){
			res.add(this.allRFs.get(i));
		}
		
		return res;
		
	}
	
	protected Set <Integer> getESSSContaining(int i){
		
		Set <Integer> cands = new HashSet<Integer>();
		cands.add(i);
		
		Set <Integer> visited = new HashSet<Integer>();
		visited.add(i);
		
		this.expandISet(cands, visited);
		
		//now make sure that the initial cand invades someone in the final set (everyone else guaranteed)
		boolean iInvadesSomeone = false;
		for(int j : visited){
			Set <Integer> invadedBy = this.invadedByMap.get(j);
			if(invadedBy.contains(i)){
				iInvadesSomeone = true;
				break;
			}
		}
		
		if(iInvadesSomeone){
			return visited;
		}
		
		return null;
	}
	
	
	protected void expandISet(Set <Integer> cands, Set <Integer> visited){
		
		Set <Integer> next = new HashSet<Integer>();
		for(int c : cands){
			Set <Integer> ci = this.invadedByMap.get(c);
			for(int cii : ci){
				if(!visited.contains(cii)){
					next.add(cii);
					visited.add(cii);
				}
			}
		}
		
		if(next.size() != 0){
			this.expandISet(next, visited);
		}
	}
	
}
