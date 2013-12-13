package commands.mttest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import burlap.debugtools.DPrint;



/*
 * q(j | i,l,m), the distortion, is the probability of word i in the produced text being
 * aligned with word j in the generating text and where l is the length of
 * the generating text and m the length of the produced text.
 * 
 * t(p | g) is the probability of the produced word p being generated
 * by word g.
 * 
 */



public class IBM2EM {

	protected List<MTDataInstance> dataset;
	
	protected DistortionParam dp;
	protected WordParam wp;
	protected LengthParam lp;
	
	protected Map <String, Double> genWordProbs;
	
	protected Set<TokenedString> allGeneratingText;
	protected Set <String> genDictionary;
	protected Set <String> prodDictionary;
	protected int maxL;
	protected int maxM;
	
	protected double wordAdditiveConstant = 0.1;
	
	protected boolean factorInAlignment = true;
	protected int numWithWorParamAlone = 5;
	
	public int debugCode = 83724;
	
	
	protected Random rand = new Random(1);
	
	public IBM2EM(List<MTDataInstance> dataset){
		this.dataset = dataset;
		
		this.maxL = 0;
		this.maxM = 0;
		this.allGeneratingText = new HashSet<TokenedString>();
		this.genDictionary = new HashSet<String>();
		this.prodDictionary = new HashSet<String>();
		
		this.genDictionary.add(TokenedString.NULLTOKEN);
		
		HashedAggregator<IntTupleHash> jlcounts = new HashedAggregator<IBM2EM.IntTupleHash>();
		HashedAggregator<Integer> slcounts = new HashedAggregator<Integer>();
		HashedAggregator<String> gWordCounts = new HashedAggregator<String>();
		int tgwCount = 0;
		
		for(MTDataInstance d : dataset){
			allGeneratingText.add(d.genLangText);
			
			int l = d.genLangText.size();
			int m = d.prodLangText.size();
			
			this.maxL = Math.max(this.maxL, l);
			this.maxM = Math.max(this.maxM, m);
			
			jlcounts.add(new IntTupleHash(l, m), 1.);
			slcounts.add(l, 1.);
			
			//use base-1 index
			for(int i = 1; i <= d.genLangText.size(); i++){
				this.genDictionary.add(d.genLangText.t(i));
				gWordCounts.add(d.genLangText.t(i), 1.);
				tgwCount++;
			}
			
			for(int i = 1; i <= d.prodLangText.size(); i++){
				this.prodDictionary.add(d.prodLangText.t(i));
			}
		}
		
		//set gen word probs
		this.genWordProbs = new HashMap<String, Double>(genDictionary.size());
		for(String gWord : this.genDictionary){
			double c = gWordCounts.v(gWord);
			double p = c / (double)tgwCount;
			this.genWordProbs.put(gWord, p);
		}
		
		
		//initialize the parameters to the uniform
		this.wp = new WordParam();
		this.dp = new DistortionParam();
		this.lp = new LengthParam();
		
		double uniWord = 1. / this.prodDictionary.size();
		for(String p : this.prodDictionary){
			for(String g : this.genDictionary){
				this.wp.set(uniWord, p, g);
			}
		}
		
		
		for(int l = 1; l <= this.maxL; l++){
			
			double sl = slcounts.v(l);
			for(int m = 1; m <= this.maxM; m++){
							
				/* the first two loops are to iterate over all possible text length pairs
				 * the next to loops will be to iterate over all possible assignments for them
				 */
				
				double jl = jlcounts.v(new IntTupleHash(l, m));
				double np = jl > 0. ? jl / sl : 0.;
				this.lp.set(np, l, m);
				
				for(int i = 1; i <= m; i++){
					double uniDist = 1./(this.maxL+1);
					for(int j = 0; j <= l; j++){
						this.dp.set(uniDist, j, i, l, m);
					}
				}
				
			}
		}
		
	}
	
	
	public Set <TokenedString> getAllGeneratingText(){
		return this.allGeneratingText;
	}
	
	public void addGeneratingText(TokenedString generatingText){
		this.allGeneratingText.add(generatingText);
	}
	
	public void addGeneratingText(Collection <TokenedString> generatingText){
		this.allGeneratingText.addAll(generatingText);
	}
	
	
	public Set <String> getGeneratingDictionary(){
		return this.genDictionary;
	}
	
	public Set <String> getProducedDictionary(){
		return this.prodDictionary;
	}
	
	public double wordParam(String producedWord, String genWord){
		return this.wp.prob(producedWord, genWord);
	}
	
	public double genWordProb(String genWord){
		return this.genWordProbs.get(genWord);
	}
	
	public void setNumIterationsToSolelyUpdateWordParams(int n){
		this.numWithWorParamAlone = n;
	}
	
	/**
	 * Finds argMax_g N_{l_g,m} \sum_{a \in alignments} \prod_{k=1}^m d_{a_k,k,l,m} t{e_a_k,f_k}
	 * @param input
	 * @return
	 */
	public DecodeResult decode(TokenedString input){
		
		int m = input.size();
		double sum = 0.;
		double maxN = 0.;
		TokenedString maxDecode = null;
		
		for(TokenedString gText : this.allGeneratingText){
			
			int l = gText.size();
			
			double n = this.lp.prob(l, m);
			double alignMarg = 0.;
			
			if(n > 0){
				
				alignMarg = this.sampleMargAlign(input, gText, 1000);
				/*double alignMarg = this.enumMargAlign(input, gText, 0.0);
				if(alignMarg > 0.000001){
					System.out.println("diff: " + (Math.abs(alignMarg - sAlignMarg) / alignMarg) + " " + alignMarg + " " + sAlignMarg);
				}*/
			}
			else{
				alignMarg = this.m1MaximumAlignment(input, gText);
			}
		
			double pG = alignMarg;
			if(n > 0){
				pG *= n;
			}
			sum += pG;
			if(pG > maxN){
				maxN = pG;
				maxDecode = gText;
			}
		
			
		}
		
		
		double p = maxN / sum;
		DecodeResult dr = new DecodeResult(input, maxDecode, p);
		
		return dr;
	}
	
	
	public List<DecodeResult> probDist(TokenedString input){
		
		List <DecodeResult> result = new ArrayList<DecodeResult>();
		
		int m = input.size();
		double sum = 0.;
		
		for(TokenedString gText : this.allGeneratingText){
			
			int l = gText.size();
			
			double n = this.lp.prob(l, m);
			double alignMarg = 0.;
			if(n > 0){
				alignMarg = this.sampleMargAlign(input, gText, 1000);
			}
			else{
				alignMarg = this.m1MaximumAlignment(input, gText);
			}
			
			
			double pG = alignMarg;
			if(n > 0){
				pG *= n;
			}
			sum += pG;
			DecodeResult dr = new DecodeResult(input, gText, pG);
			result.add(dr);
			
		}
		
		//Normalize
		for(DecodeResult dr : result){
			dr.prob /= sum;
		}
		
		
		
		return result;
		
	}
	
	protected double enumMargAlign(TokenedString input, TokenedString gText, double minDistProb){
		
		int l = gText.size();
		int m = input.size();
		
		double alignMarg = 0.;
		//List <List<Integer>> allAlignments = getAllExAlignents(l, m);
		List <List<Integer>> allAlignments = this.getLikelyAlignments(l, m, minDistProb);
		//System.out.println(allAlignments.size());
		for(List <Integer> alignment : allAlignments){
			
			double prod = 1.;
			for(int k = 1; k <= alignment.size(); k++){
				int ak = alignment.get(k-1); //note that alignment array is in 0-base index
				String pWord = input.t(k);
				String gWord = gText.t(ak);
				
				double dist = this.dp.prob(ak, k, l, m);
				double word = this.wp.prob(pWord, gWord);
				
				prod *= dist*word;
			}
			
			alignMarg += prod;
			
		}
		
		return alignMarg;
		
	}
	
	protected double sampleMargAlign(TokenedString input, TokenedString gText, int nSamples){
		
		int l = gText.size();
		int m = input.size();
		
		double alignMarg = 0.;
		for(int i = 0; i < nSamples; i++){
			List <Integer> alignment = this.sampleAlignment(l, m);
			double prod = 1.;
			for(int k = 1; k <= alignment.size(); k++){
				int ak = alignment.get(k-1); //note that alignment array is in 0-base index
				String pWord = input.t(k);
				String gWord = gText.t(ak);
				
				if(!this.prodDictionary.contains(pWord)){
					continue;
				}
				
				double dist = this.dp.prob(ak, k, l, m);
				double word = this.wp.prob(pWord, gWord);
				
				prod *= word;
			}
			alignMarg += prod;

		}
		
		
		alignMarg /= (double)nSamples;
		
		return alignMarg;
		
	}
	
	protected double m1MaximumAlignment(TokenedString input, TokenedString gText){
		
		int l = gText.size();
		int m = input.size();
		
		double prod = 1.;
		for(int k = 1; k <= m; k++){
			
			String pWord = input.t(k);
			if(!this.prodDictionary.contains(pWord)){
				continue;
			}
			
			//find best match
			double bestMatch = 0.;
			for(int j = 0; j <= l; j++){
				double word = this.wp.prob(pWord, gText.t(j));
				if(word > bestMatch){
					bestMatch = word;
				}
			}
			
			prod *= bestMatch;
			
		}
		
		//normalize by how many possible alignments there are
		double prob = prod * Math.pow(l+1, m);
		
		return prob;
		
	}
	
	protected List <Integer> sampleAlignment(int l, int m){
		List <Integer> alignment = new ArrayList<Integer>(m);
		
		for(int i = 1; i <= m; i++){
			double r = this.rand.nextDouble();
			double sumP = 0.;
			boolean added = false;
			for(int j = 0; j <= l; j++){
				double p = this.dp.prob(j, i, l, m);
				sumP += p;
				if(r < sumP){
					alignment.add(j);
					added = true;
					break;
				}
			}
			if(!added){
				sumP = 0.;
				for(int j = 0; j < l; j++){
					sumP += this.dp.prob(j, i, l, m);
					if(r < sumP){
						alignment.add(j);
						added = true;
						break;
					}
				}
			}
		}
		
		
		return alignment;
	}
	

	public void runEM(int iterations){
		
		for(int i = 0; i < iterations; i++){
			this.runEMIteration();
			DPrint.cl(debugCode, "EM Iteration: " + i + " complete.");
			if(i > numWithWorParamAlone){
				this.factorInAlignment = true;
			}
		}
		
	}
	
	public void runEMIteration(){
		
		HashedAggregator<String> jointWordCounts = new HashedAggregator<String>(wordAdditiveConstant);
		HashedAggregator<String> singleWordCounts = new HashedAggregator<String>(wordAdditiveConstant*prodDictionary.size());
		
		HashedAggregator<IntTupleHash> jointDistortionCounts = new HashedAggregator<IBM2EM.IntTupleHash>(0);
		HashedAggregator<IntTupleHash> singleDistortionCounts = new HashedAggregator<IntTupleHash>(0);
		
		
		//E-step
		for(MTDataInstance d : this.dataset){
			
			int m = d.prodLangText.size();
			int l = d.genLangText.size();
			
			for(int i = 1; i <= m; i++){
				for(int j = 0; j <= l; j++){
					
					double delta = this.expectedVal(d.prodLangText, d.genLangText, i, j, m, l);
					String pWord = d.prodLangText.t(i);
					String gWord = d.genLangText.t(j);
					
					jointWordCounts.add(tokenCombine(pWord, gWord), delta);
					singleWordCounts.add(gWord, delta);
					
					jointDistortionCounts.add(new IntTupleHash(j, i, l, m), delta);
					singleDistortionCounts.add(new IntTupleHash(i, l, m), delta);
					
				}
			}
			
		}
		
		
		//M-step
		
		//M-step for word params
		for(String gWord : this.genDictionary){
			double sc = singleWordCounts.v(gWord);
			for(String pWord : this.prodDictionary){
				double jc = jointWordCounts.v(tokenCombine(pWord, gWord));
				
				double p = jc > 0. ? jc / sc : 0.;
				this.wp.set(p, pWord, gWord);
				
			}
		}
		
		//M step for distortion params
		for(int m = 1; m <= this.maxM; m++){
			for(int l = 1; l <= this.maxL; l++){
				
				/* the first two loops are to iterate over all possible text length pairs
				 * the next to loops will be to iterate over all possible assignments for them
				 */
				
				for(int i = 1; i <= m; i++){

					double sc = singleDistortionCounts.v(new IntTupleHash(i, l, m));
					for(int j = 0; j <= l; j++){
						double jc = jointDistortionCounts.v(new IntTupleHash(j, i, l, m));
						
						double p = jc > 0. ? jc / sc : 0.;
						this.dp.set(p, j, i, l, m);
						
					}
					
				}
				
				
			}
		}
		
		
		
		
		
	}
	
	protected void print52(){
		int m = 5;
		int l = 2;
		double max = 0.;
		for(int i = 1; i <= m; i++){
			for(int j = 0; j <= l; j++){
				double p = this.dp.prob(j, i, l, m);
				if(p > max){
					max = p;
				}
			}
			System.out.print(max + " ");
		}
		System.out.println("");
	}
	
	
	protected double expectedVal(TokenedString prodText, TokenedString genText, int i, int j, int m, int l){
		
		double sum = 0.;
		for(int u = 0; u <= l; u++){
			double d = 1.;
			if(this.factorInAlignment){
				d = dp.prob(u, i, l, m);
			}
			double w = wp.prob(prodText.t(i), genText.t(u));
			sum += d*w;
		}
		
		double d = 1.;
		if(this.factorInAlignment){
			d = dp.prob(j, i, l, m);
		}
		double w = wp.prob(prodText.t(i), genText.t(j));
		
		double num = d*w;
		double p = num / sum;
		
		return p;
		
	}
	
	
	protected static String tokenCombine(String prodWord, String genWord){
		return prodWord + "+++" + genWord;
	}
	
	
	
	
	
	protected List<List<Integer>> getLikelyAlignments(int l, int m, double minDistProb){
		
		/*
		List <List<Integer>> result = new ArrayList<List<Integer>>();
		getAllAlignmentsHelper(l, new int[m], 0, result);
		
		return result;
		*/
		
		
		List <List<Integer>> viableDistortions = this.getAllLikelyDistortions(l, m, minDistProb);
		List <List<Integer>> result = new ArrayList<List<Integer>>();
		this.getAllLikelyAlignmentsHelper(l, new int[m], 0, result, viableDistortions);
		
		return result;
		
		
	}
	
	
	protected List <List<Integer>> getAllLikelyDistortions(int l, int m, double minDistProb){
		
		List <List<Integer>> res = new ArrayList<List<Integer>>(m);
		for(int i = 1; i <= m; i++){
			List <Integer> viableDistortions = new ArrayList<Integer>();
			for(int j = 0; j <= l; j++){
				double p = this.dp.prob(j, i, l, m);
				if(p > minDistProb){
					viableDistortions.add(j);
				}
			}
			res.add(viableDistortions);
		}
		
		return res;
	}
	
	
	protected void getAllLikelyAlignmentsHelper(int l, int [] alignment, int ind, List<List<Integer>> allAlignments, List <List<Integer>> viableDistortions){
		
		if(ind == alignment.length){
			List <Integer> lAlignement = new ArrayList<Integer>(alignment.length);
			for(int i : alignment){
				lAlignement.add(i);
			}
			allAlignments.add(lAlignement);
		}
		else{
			
			
			List <Integer> vdForInd = viableDistortions.get(ind);
			for(int i : vdForInd){
				alignment[ind] = i;
				this.getAllLikelyAlignmentsHelper(l, alignment, ind+1, allAlignments, viableDistortions);
			}
			
			
			/*
			for(int i = 0; i <= l; i++){
				alignment[ind] = i;
				this.getAllLikelyAlignmentsHelper(l, alignment, ind+1, allAlignments, viableDistortions);
			}
			*/
			
		}
		
	}
	
	
	
	protected static List <List<Integer>> getAllExAlignents(int l, int m){
		List <List<Integer>> result = new ArrayList<List<Integer>>();
		getAllAlignmentsHelper(l, new int[m], 0, result);
		
		return result;
	}
	
	
	
	
	protected static void getAllAlignmentsHelper(int l, int [] alignment, int ind, List<List<Integer>> allAlignments){
		
		if(ind == alignment.length){
			List <Integer> lAlignement = new ArrayList<Integer>(alignment.length);
			for(int i : alignment){
				lAlignement.add(i);
			}
			allAlignments.add(lAlignement);
		}
		else{
			
			for(int i = 0; i <= l; i++){
				alignment[ind] = i;
				getAllAlignmentsHelper(l, alignment, ind+1, allAlignments);
			}
			
		}
		
	}
	
	class DistortionParam{
		
		Map<IntTupleHash, Double> paramValues;
		
		public DistortionParam(){
			this.paramValues = new HashMap<IBM2EM.IntTupleHash, Double>();
		}
		
		public double prob(int j, int i, int l, int m){
			Double P = this.paramValues.get(new IntTupleHash(j, i, l, m));
			double p = P != null ? P : 0.; 
			return p;
		}
		
		public void set(double p, int j, int i, int l, int m){
			this.paramValues.put(new IntTupleHash(j, i, l, m), p);
		}
		
		
	}
	

	
	class WordParam{
		
		Map <String, Double> paramValues;
		
		public WordParam(){
			this.paramValues = new HashMap<String, Double>();
		}
		
		public double prob(String prodWord, String genWord){
			Double P = this.paramValues.get(tokenCombine(prodWord, genWord));
			double p = P != null ? P : 0.;
			return p;
		}
		
		public void set(double p, String prodWord, String genWord){
			this.paramValues.put(tokenCombine(prodWord, genWord), p);
		}
		
	}
	
	class LengthParam{
		
		Map <IntTupleHash, Double> paramValues;
		
		public LengthParam(){
			this.paramValues = new HashMap<IBM2EM.IntTupleHash, Double>();
		}
		
		public double prob(int l, int m){
			Double P = this.paramValues.get(new IntTupleHash(l, m));
			double p = P != null ? P : 0.; 
			return p;
		}
		
		public void set(double p, int l, int m){
			this.paramValues.put(new IntTupleHash(l, m), p);
		}
		
	}
	
	
	public class IntTupleHash{
		
		int [] tuple;
		
		public IntTupleHash(int j, int i, int l, int m){
			this.tuple = new int[]{j,i,l,m};
		}
		
		public IntTupleHash(int i, int l, int m){
			this.tuple = new int[]{i,l,m};
		}
		
		public IntTupleHash(int l, int m){
			this.tuple = new int[]{l,m};
		}
		
		@Override
		public int hashCode(){
			
			int shift = 23;
			int prod = 1;
			int sum = 0;
			for(int i : tuple){
				sum += i*prod;
				prod *= shift;
			}
			
			return sum;
			
		}
		
		@Override 
		public boolean equals(Object o){
			
			if(!(o instanceof IntTupleHash)){
				return false;
			}
			
			IntTupleHash oo = (IntTupleHash)o;
			
			if(this.tuple.length != oo.tuple.length){
				return false;
			}
			
			for(int i = 0; i < this.tuple.length; i++){
				if(this.tuple[i] != oo.tuple[i]){
					return false;
				}
			}
			
			return true;
			
		}
		
	}
	
	
	
	
	public class DecodeResult{
		
		public TokenedString input;
		public TokenedString decoded;
		public double prob;
		
		
		public DecodeResult(TokenedString input, TokenedString decoded, double p){
			this.input = input;
			this.decoded = decoded;
			this.prob = p;
		}
		
		
	}
	
	
	
	
	
	
}
