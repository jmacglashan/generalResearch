package commands.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import burlap.debugtools.DPrint;

import commands.mttest.IBM2EM;
import commands.mttest.IBM2EMWordFilterWrap;
import commands.mttest.MTDataInstance;
import commands.mttest.TokenedString;
import commands.mttest.Tokenizer;
import commands.mttest.IBM2EM.DecodeResult;

public class SokoLabeledMTTest {

	
	
	//dataFiles/commands/allTurkSemanticLabeled
	//dataFiles/commands/agentTurkSemanticLabeled
	//dataFiles/commands/blockTurkSemanticLabeled
	//dataFiles/commands/blockTurkSemanticLabeledClean
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//standardTest("dataFiles/commands/blockTurkSemanticLabeled");
		//looTest("dataFiles/commands/allTurkSemanticLabeled", false);
		//getAllGenTest("dataFiles/commands/blockTurkSemanticLabeled");
		//getWordParamDist("dataFiles/commands/allTurkSemanticLabeled");
		//wordFilterTest("dataFiles/commands/allTurkSemanticLabeled");
		//wordFilterLearnTest("dataFiles/commands/blockTurkSemanticLabeled");
		//looWordFilterTest("dataFiles/commands/allTurkSemanticLabeled", false);
		//looProbe("dataFiles/commands/allTurkSemanticLabeled", 1);
		
		printParams("dataFiles/commands/allTurkSemanticLabeled");
		
	}
	
	
	public static void printParams(String path){
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		IBM2EM m2 = new IBM2EM(dataset);
		m2.runEM(10);
		m2.printLPParams();
	}
	
	public static void getAllGenTest(String path){
		Tokenizer tokenizer = new Tokenizer(true, true);
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		IBM2EM m2 = new IBM2EM(dataset);
		
		Set<TokenedString> genText = m2.getAllGeneratingText();
		for(TokenedString ts : genText){
			System.out.println(ts.toString());
		}
		
	}
	
	public static void standardTest(String path){
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		IBM2EM m2 = new IBM2EM(dataset);
		
		System.out.println("Starting.");
		
		m2.runEM(10);
		
		int c = 0;
		int pc = 0;
		for(MTDataInstance d : dataset){
			TokenedString s = d.prodLangText;
			DecodeResult dr = m2.decode(s);
			if(dr.decoded.equals(d.genLangText)){
				//System.out.println("Correct: " + d.prodLangText + " -> " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				c++;
				pc++;
			}
			else{
				
				String incLabel = "Incorrect";
				if(dr.decoded.t(4).equals(d.genLangText.t(4))){
					pc++;
					incLabel = "Pseudo";
				}
				
				//System.out.println(incLabel + ": " + d.prodLangText + " -> " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println(incLabel + ": " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
			}
		}
		
		System.out.println("correct: " + c + "/" + dataset.size() + "; " + ((double)c/(double)dataset.size()));
		System.out.println("pesudeo correct: " + pc + "/" + dataset.size() + "; " + ((double)pc/(double)dataset.size()));
		
	}
	
	public static void looTest(String path, boolean countTranspose){
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		int c = 0;
		int pc = 0;
		int tc = 0;
		for(int i = 100; i < dataset.size(); i++){
			MTDataInstance testInst = dataset.get(i);
			List<MTDataInstance> looDataset = leaveOneOutDataset(dataset, i);
			IBM2EM m2 = new IBM2EM(looDataset);
			DPrint.toggleCode(m2.debugCode, false);
			m2.runEM(10);
			TokenedString s = testInst.prodLangText;
			DecodeResult dr = m2.decode(s);
			
			if(dr.decoded.equals(testInst.genLangText)){
				//System.out.println("Correct: " + testInst.prodLangText + " -> " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println("Correct: " + testInst.prodLangText + " -> " + dr.decoded.toString() + "::" + dr.prob + "\n");
				c++;
				pc++;
			}
			else{
				
				String incLabel = "Incorrect";
				if(dr.decoded.t(1).equals(testInst.genLangText.t(1)) && dr.decoded.t(4).equals(testInst.genLangText.t(4))){
					pc++;
					incLabel = "Pseudo";
				}
				else if(countTranspose){
					if(testInst.genLangText.size() == 10){
						if(dr.decoded.t(4).equals(testInst.genLangText.t(9))){
							tc++;
							incLabel = "Transpose";
						}
					}
				}
				
				//System.out.println(incLabel + ": " + testInst.prodLangText + " -> " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				//System.out.println(incLabel + ": " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println(incLabel + ": " + testInst.prodLangText + " -> " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println("Actual: " + testInst.genLangText.toString() + "\n");
				
			}
			
		}
		
		System.out.println("correct: " + c + "/" + dataset.size() + "; " + ((double)c/(double)dataset.size()));
		System.out.println("pesudeo correct: " + pc + "/" + dataset.size() + "; " + ((double)pc/(double)dataset.size()));
		System.out.println("num transposes: " + tc);
		
		
	}
	
	public static void looProbe(String path, int dataInstance){
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		MTDataInstance testInst = dataset.get(dataInstance);
		List<MTDataInstance> looDataset = leaveOneOutDataset(dataset, dataInstance);
		IBM2EM m2 = new IBM2EM(looDataset);
		DPrint.toggleCode(m2.debugCode, false);
		m2.runEM(10);
		TokenedString s = testInst.prodLangText;
		
		DecodeResult dr = m2.decode(s);
		m2.probDistMLProbe(s);
		
		
		if(dr.decoded.equals(testInst.genLangText)){
			//System.out.println("Correct: " + testInst.prodLangText + " -> " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
			System.out.println("Correct: " + testInst.prodLangText + " -> " + dr.decoded.toString() + "::" + dr.prob + "\n");

		}
		else{
			
			String incLabel = "Incorrect";
			if(dr.decoded.t(1).equals(testInst.genLangText.t(1)) && dr.decoded.t(4).equals(testInst.genLangText.t(4))){
				incLabel = "Pseudo";
			}
			
			
			//System.out.println(incLabel + ": " + testInst.prodLangText + " -> " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
			//System.out.println(incLabel + ": " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
			System.out.println(incLabel + ": " + testInst.prodLangText + " -> " + dr.decoded.toString() + "::" + dr.prob);
			System.out.println("Actual: " + testInst.genLangText.toString() + "\n");
			
		}
		
	}
	
	public static void getWordParamDist(String path){
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		IBM2EM m2 = new IBM2EM(dataset);
		m2.setNumIterationsToSolelyUpdateWordParams(100);
		m2.runEM(10);
		
		Set<String> genDict = m2.getGeneratingDictionary();
		Set<String> prodDict = m2.getProducedDictionary();
		
		
		List <WordScorePair> wordScores = new ArrayList<SokoLabeledMTTest.WordScorePair>(prodDict.size());
		
		for(String pw : prodDict){
			
			System.out.println(pw+"\n=================================");
			double [] pvals = new double[genDict.size()-1];
			double sum = 0.;
			int i = 0;
			for(String gw : genDict){
				double p = m2.wordParam(pw, gw);
				System.out.printf("%.5f\t%s\n", p, gw);
				if(!gw.equals(TokenedString.NULLTOKEN)){
					pvals[i] = p;
					sum += p;
					i++;
				}
			}
			double score = computeScore(pvals, sum);
			wordScores.add(new WordScorePair(pw, score));
			System.out.println("--------------\nscore: " + score);
			
			
			System.out.println("\n\n\n\n\n");
			
		}
		
		Collections.sort(wordScores);
		for(WordScorePair wsp : wordScores){
			System.out.printf("%.4f %s\n", wsp.score, wsp.word);
		}
		
	}
	
	
	public static void wordFilterTest(String path){
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		
		IBM2EMWordFilterWrap m2 = new IBM2EMWordFilterWrap(dataset);
		m2.findImportantWords(dataset, 10);
		
	}
	
	
	public static void wordFilterLearnTest(String path){
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		IBM2EMWordFilterWrap m2 = new IBM2EMWordFilterWrap(dataset);
		
		System.out.println("Starting.");
		
		m2.runEM(10);
		
		int c = 0;
		int pc = 0;
		for(MTDataInstance d : dataset){
			TokenedString s = d.prodLangText;
			DecodeResult dr = m2.decode(s);
			if(dr.decoded.equals(d.genLangText)){
				//System.out.println("Correct: " + d.prodLangText + " -> " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				c++;
				pc++;
			}
			else{
				
				String incLabel = "Incorrect";
				if(dr.decoded.t(4).equals(d.genLangText.t(4))){
					pc++;
					incLabel = "Pseudo";
				}
				
				//System.out.println(incLabel + ": " + d.prodLangText + " -> " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println(incLabel + ": " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
			}
		}
		
		System.out.println("correct: " + c + "/" + dataset.size() + "; " + ((double)c/(double)dataset.size()));
		System.out.println("pesudeo correct: " + pc + "/" + dataset.size() + "; " + ((double)pc/(double)dataset.size()));
		
	}
	
	
	public static void looWordFilterTest(String path, boolean countTranspose){
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, path, "txt");
		
		int c = 0;
		int pc = 0;
		int tc = 0;
		DPrint.toggleCode(83724, false);
		DPrint.toggleCode(236434, false);
		for(int i = 0; i < dataset.size(); i++){
			MTDataInstance testInst = dataset.get(i);
			List<MTDataInstance> looDataset = leaveOneOutDataset(dataset, i);
			IBM2EMWordFilterWrap m2 = new IBM2EMWordFilterWrap(looDataset);
			
			m2.runEM(10);
			TokenedString s = testInst.prodLangText;
			DecodeResult dr = m2.decode(s);
			
			if(dr.decoded.equals(testInst.genLangText)){
				System.out.println("Correct: " + testInst.prodLangText + " -> " + dr.decoded.toString() + "::" + dr.prob + "\n");
				c++;
				pc++;
			}
			else{
				
				String incLabel = "Incorrect";
				if(dr.decoded.t(1).equals(testInst.genLangText.t(1)) && dr.decoded.t(4).equals(testInst.genLangText.t(4))){
					pc++;
					incLabel = "Pseudo";
				}
				else if(countTranspose){
					if(testInst.genLangText.size() == 10){
						if(dr.decoded.t(4).equals(testInst.genLangText.t(9))){
							tc++;
							incLabel = "Transpose";
						}
					}
				}
				
				//System.out.println(incLabel + ": " + testInst.prodLangText + " -> " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				//System.out.println(incLabel + ": " + testInst.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println(incLabel + ": " + testInst.prodLangText + " -> " + dr.decoded.toString() + "::" + dr.prob);
				System.out.println("Actual: " + testInst.genLangText.toString() + "\n");
			}
			
		}
		
		System.out.println("correct: " + c + "/" + dataset.size() + "; " + ((double)c/(double)dataset.size()));
		System.out.println("pesudeo correct: " + pc + "/" + dataset.size() + "; " + ((double)pc/(double)dataset.size()));
		System.out.println("num transposes: " + tc);
		
		
	}
	
	public static List <MTDataInstance> leaveOneOutDataset(List<MTDataInstance> dataset, int instanceInd){
		List <MTDataInstance> looDataset = new ArrayList<MTDataInstance>(dataset);
		looDataset.remove(instanceInd);
		return looDataset;
	}
	
	
	public static double computeScore(double [] pvals, double sum){
		
		double ent = 0.01;
		double c = 0.;
		for(int i = 0; i < pvals.length; i++){
			double p = pvals[i] / sum;
			ent += p*Math.log(p);
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
