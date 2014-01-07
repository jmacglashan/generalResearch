package commands.mttest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import commands.mttest.IBM2EM.DecodeResult;
import datastructures.HashedAggregator;

public class MTDriver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//pullOutHumanSetences();
		//simpleGeneralization();
		//simpleTest();
		trainingFlowTest();

	}
	
	
	
	public static void simpleGeneralization(){
		
		Tokenizer tokenizer = new Tokenizer(true);
		List <MTDataInstance> dataset = new ArrayList<MTDataInstance>();
		
		/*
		MTDataInstance d1 = new MTDataInstance(tokenizer, "wallEast a wallNorth a inArea a0", "go to the top right corner of the bottom left room");
		MTDataInstance d2 = new MTDataInstance(tokenizer, "wallWest a wallSouth a inArea a2", "go to the bottom left corner of the top right room");
		
		MTDataInstance t1 = new MTDataInstance(tokenizer, "wallEast a wallNorth a inArea a2", "go to the top right corner of the top right room");
		*/
		
		/*
		MTDataInstance d1 = new MTDataInstance(tokenizer, "wallEast wallNorth inAreaa0", "go to the top right corner of the bottom left room");
		MTDataInstance d2 = new MTDataInstance(tokenizer, "wallWest wallSouth inAreaa2", "go to the bottom left corner of the top right room");
		
		MTDataInstance t1 = new MTDataInstance(tokenizer, "wallEast wallNorth inAreaa2", "go to the top right corner of the top right room");
		*/
		
		
		MTDataInstance d1 = new MTDataInstance(tokenizer, "wallEast agent0 wallNorth agent0 inArea area0", "go to the top right corner of the red room");
		MTDataInstance d2 = new MTDataInstance(tokenizer, "wallWest agent0 wallSouth agent0 inArea area2", "go to the bottom left corner of the blue room");
		
		MTDataInstance t1 = new MTDataInstance(tokenizer, "wallEast agent0 wallNorth agent0 inArea area2", "go to the top right corner of the blue room");
		
		
		
		dataset.add(d1);
		dataset.add(d2);
		
		IBM2EM m2 = new IBM2EM(dataset);
		m2.addGeneratingText(t1.genLangText);
		
		m2.runEM(10);
		
		System.out.println("Decoding...");
		
		DecodeResult drd1 = m2.decode(d1.prodLangText);
		System.out.println(d1.prodLangText.toString() + " -> " + drd1.decoded.toString());
		
		DecodeResult drd2 = m2.decode(d2.prodLangText);
		System.out.println(d2.prodLangText.toString() + " -> " + drd2.decoded.toString());
		
		DecodeResult drt1 = m2.decode(t1.prodLangText);
		System.out.println(t1.prodLangText.toString() + " -> " + drt1.decoded.toString());
		
		
		
		
	}
	
	
	public static void trainingFlowTest(){
		
		
		Tokenizer tokenizer = new Tokenizer(true);
		
		List <MTDataInstance> fullDataset = new ArrayList<MTDataInstance>();
		
		for(int i = 0; i < 4; i++){
			
			String roomName = "room zero";
			roomName = "the bottom left room";
			if(i == 1){
				roomName = "room one";
				roomName = "the top left room";
			}
			else if(i == 2){
				roomName = "room two";
				roomName = "the top right room";
			}
			else if(i == 3){
				roomName = "room three";
				roomName = "the bottom right room";
			}
			
			fullDataset.add(new MTDataInstance(tokenizer, "wallToWest agent0 wallToSouth agent0 inArea agent0 area"+i, "go to the bottom left corner of " + roomName));
			fullDataset.add(new MTDataInstance(tokenizer, "wallToWest agent0 wallToNorth agent0 inArea agent0 area"+i, "go to the top left corner of " + roomName));
			fullDataset.add(new MTDataInstance(tokenizer, "wallToEast agent0 wallToNorth agent0 inArea agent0 area"+i, "go to the top right corner of " + roomName));
			fullDataset.add(new MTDataInstance(tokenizer, "wallToEast agent0 wallToSouth agent0 inArea agent0 area"+i, "go to the bottom right corner of " + roomName));
			
		}
		
		
		List <MTDataInstance> dataset = new ArrayList<MTDataInstance>();
		IBM2EM m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		printDatasetResultsFor(m2, fullDataset, 2);
		
		dataset.add(fullDataset.get(2));
		m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		m2.runEM(10);
		printDatasetResultsFor(m2, fullDataset, 2,8);
		
		dataset.add(fullDataset.get(8));
		m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		m2.runEM(10);
		printDatasetResultsFor(m2, fullDataset, 2,8,10);
		
		dataset.add(fullDataset.get(10));
		m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		m2.runEM(10);
		printDatasetResultsFor(m2, fullDataset, 2,8,10,6);
		
		dataset.add(fullDataset.get(6));
		m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		m2.runEM(10);
		printDatasetResultsFor(m2, fullDataset, 2,8,10,6,4);
		
		dataset.add(fullDataset.get(4));
		m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		m2.runEM(10);
		printDatasetResultsFor(m2, fullDataset, 2,8,10,6,4,14);
		
		dataset.add(fullDataset.get(14));
		m2 = new IBM2EM(dataset);
		addGeneratingFromDataset(m2, fullDataset);
		m2.runEM(10);
		printDatasetResultsFor(m2, fullDataset, 2,8,10,6,4,14,12);
		
		
		
	}
	
	public static void printDatasetResultsUpTo(IBM2EM m2, List <MTDataInstance> dataset, int n){
		
		for(int i = 0; i < n; i++){
			MTDataInstance d1 = dataset.get(i);
			DecodeResult dr = m2.decode(d1.prodLangText);
			System.out.println(d1.prodLangText.toString() + " -> " + dr.decoded.toString() + " (" + dr.prob + ")");
		}
		
	}
	
	public static void printDatasetResultsFor(IBM2EM m2, List <MTDataInstance> dataset, int... inds){
		
		for(int i : inds){
			MTDataInstance d1 = dataset.get(i);
			DecodeResult dr = m2.decode(d1.prodLangText);
			System.out.println(d1.prodLangText.toString() + " -> " + dr.decoded.toString() + " (" + dr.prob + ")");
		}
		
	}
	
	public static void addGeneratingFromDataset(IBM2EM m2, List<MTDataInstance> dataset){
		for(MTDataInstance d : dataset){
			m2.addGeneratingText(d.genLangText);
		}
	}
	
	
	public static void pullOutHumanSetences(){
		
		String inputDir = "dataFiles/commands/allTurkTrain";
		String outputDir = "dataFiles/commands/allTurkSemanticLabeled";
		
		File dir = new File(inputDir);
		final String ext = ".txt";
		
		HashedAggregator<Integer> uniqueLengths = new HashedAggregator<Integer>();
		Tokenizer tokenizer = new Tokenizer(true);
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(ext)){
					return true;
				}
				return false;
			}
		};
		String[] children = dir.list(filter);
		for(int i = 0; i < children.length; i++){
			try {
				BufferedReader in = new BufferedReader(new FileReader(inputDir + "/" + children[i]));
				String command = in.readLine().trim();
				
				TokenedString ts = tokenizer.tokenize(command);
				uniqueLengths.add(ts.size(), 1.);
				
				//BufferedWriter out = new BufferedWriter(new FileWriter(outputDir + "/" + children[i]));
				//out.write(command + "\n");
				
				//out.close();
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Unique lengths: " + uniqueLengths.size() + "; num instances: " + children.length);
		for(int i = 0; i < 100; i++){
			double c = uniqueLengths.v(i);
			if(c > 0){
				System.out.println(i + ": " + (int)c);
			}
		}
		
	}
	
	public static void simpleTest(){
		
		Tokenizer tokenizer = new Tokenizer(true);
		//List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, "dataFiles/commands/simpleSemanticLabeled", "txt");
		//List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, "dataFiles/commands/simpleSemanticLabeledDoubleColor", "txt");
		List<MTDataInstance> dataset = MTDataInstance.getDatsetFromDir(tokenizer, "dataFiles/commands/simpleSemanticLabeledCombined", "txt");
		
		IBM2EM m2 = new IBM2EM(dataset);
		
		m2.runEM(10);
		
		int c = 0;
		for(MTDataInstance d : dataset){
			TokenedString s = d.prodLangText;
			DecodeResult dr = m2.decode(s);
			if(dr.decoded.equals(d.genLangText)){
				System.out.println("Correct: " + d.prodLangText + " -> " + d.genLangText.toString() + "; " + dr.decoded.toString() + "::" + dr.prob);
				c++;
				//break;
			}
		}
		
		System.out.println(c + "/" + dataset.size() + "; " + ((double)c/(double)dataset.size()));
		
		
	}

}
