package commands.mttest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import commands.mttest.IBM2EM.DecodeResult;

import burlap.debugtools.DPrint;





public class IBM2EMWordFilterWrap {

	protected IBM2EM		wordImportanceModel;
	protected IBM2EM		fm2;
	
	protected Set<String>	whiteListWords;
	protected Set<String>	blackListWords;
	
	protected Tokenizer		whiteListTokenizer;
	
	protected int 			 debugId = 236434;
	
	protected List <MTDataInstance> dataset;
	protected List <MTDataInstance> filteredDataset;
	
	
	
	public IBM2EMWordFilterWrap(List<MTDataInstance> dataset){
		this.dataset = dataset;
	}
	
	
	public void runEM(int iterations){
		
		this.findImportantWords(dataset, iterations);
		
		this.filteredDataset = new ArrayList<MTDataInstance>(this.dataset.size());
		
		this.whiteListTokenizer = new Tokenizer(true);
		this.whiteListTokenizer.setToWhiteListFilter();
		for(String w : this.whiteListWords){
			this.whiteListTokenizer.addTokenFilter(w);
		}
		
		for(MTDataInstance d : this.dataset){
			
			MTDataInstance f = new MTDataInstance(d.genLangText, this.whiteListTokenizer.tokenize(d.prodLangText.toString()));
			if(f.prodLangText.size() > 0){
				this.filteredDataset.add(f);
			}
			
		}
		
		this.wordImportanceModel = null; //clear memory
		
		this.fm2 = new IBM2EM(filteredDataset);
		this.fm2.runEM(iterations);
		
	}
	
	public DecodeResult decode(TokenedString input){
		
		//translate to filtered model
		TokenedString f = this.whiteListTokenizer.tokenize(input.toString());
		DecodeResult dr = this.fm2.decode(f);
		
		//reassign result to original input
		dr.input = input;
		return dr;
		
	}
	
	public List<DecodeResult> probDist(TokenedString input){
		
		//translate to filtered model
		TokenedString f = this.whiteListTokenizer.tokenize(input.toString());
		List <DecodeResult> drs = this.fm2.probDist(f);
		
		//reassign result to original input
		for(DecodeResult dr : drs){
			dr.input = input;
		}
		return drs;
		
		
	}
	
	public void findImportantWords(List<MTDataInstance> dataset, int nEMIters){
		
		wordImportanceModel = new IBM2EM(dataset);
		wordImportanceModel.setNumIterationsToSolelyUpdateWordParams(nEMIters+1);
		wordImportanceModel.runEM(nEMIters);
		
		Set<String> genDict = wordImportanceModel.getGeneratingDictionary();
		Set<String> prodDict = wordImportanceModel.getProducedDictionary();
		
		
		List <WordScorePair> wordScores = new ArrayList<WordScorePair>(prodDict.size());
		
		for(String pw : prodDict){
			
			double [] pvals = new double[genDict.size()-1];
			double sum = 0.;
			int i = 0;
			for(String gw : genDict){
				//double p = wordImportanceModel.wordParam(pw, gw) * wordImportanceModel.genWordProb(gw);
				double p = wordImportanceModel.wordParam(pw, gw);
				if(!gw.equals(TokenedString.NULLTOKEN)){
					pvals[i] = p;
					sum += p;
					i++;
				}
			}
			double score = computeScore(pvals, sum, genDict.size()-1);
			wordScores.add(new WordScorePair(pw, score));
			
			
			
			
			
		}
		
		Collections.sort(wordScores);
		
		
		//int tindex = this.thresholdIndex(wordScores);
		int tindex = 100;
		
		whiteListWords = new HashSet<String>(wordScores.size() - tindex + 1);
		blackListWords = new HashSet<String>(tindex);
		
		DPrint.cl(this.debugId, "Black list words\n-----------------------");
		for(int i = 0; i < wordScores.size(); i++){
			
			if(i == tindex){
				DPrint.cl(this.debugId, "\nWhite list words\n-----------------------");
			}
			
			WordScorePair ws = wordScores.get(i);
			DPrint.cf(this.debugId, "%d %.4f %s\n", i, ws.score, ws.word);
			
			if(i < tindex){
				blackListWords.add(ws.word);
			}
			else{
				whiteListWords.add(ws.word);
			}
		}
		
	}
	
	
	
	protected int thresholdIndex(List<WordScorePair> wordScores){
		
		int n  = wordScores.size() - 1;
		
		double x0 = 0;
		double y0 = wordScores.get(0).score;
		
		double x1 = n;
		double y1 = wordScores.get(n).score;
		
		double maxD = Double.NEGATIVE_INFINITY;
		int maxInd = -1;
		
		for(int i = 0; i <= n; i++){
			
			double x = i;
			double y = wordScores.get(i).score;
			
			double d = distToLine(x0, y0, x1, y1, x, y, true);
			if(d > maxD){
				maxD = d;
				maxInd = i;
			}
			
		}
		
		return maxInd;
		
	}
	
	protected static double distToLine(double x0, double y0, double x1, double y1, double x, double y, boolean useAbs){
		
		
		double num = (y0 - y1)*x + (x1-x0)*y + (x0*y1 - x1*y0);
		double denom = Math.sqrt(Math.pow(x1-x0, 2) + Math.pow(y1-y0, 2));
		
		if(useAbs){
			num = Math.abs(num);
		}
		
		double d = num / denom;
		return d;
		
	}
	
	public static double computeScore(double [] pvals, double sum, double base){
		
		double ent = 0.01;
		double c = 0.;
		double logBaseChange = Math.log(base);
		for(int i = 0; i < pvals.length; i++){
			double p = pvals[i] / sum;
			ent += p*Math.log(p) / logBaseChange;
		}
		ent *= -1.;
		
		return sum/(ent + c);
	}
	
	static class WordScorePair implements Comparable<WordScorePair>{
		
		String word;
		double score;
		
		public WordScorePair(String word, double score) {
			this.word = word;
			this.score = score;
		}

		@Override
		public int compareTo(WordScorePair o) {
			return Double.compare(this.score, o.score);
		}
		
	}
	
}
