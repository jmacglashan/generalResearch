package commands.auxiliarytools.languagegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdealLanguageGen {

	
	Map<String, SemanticGenerator>		sgs;
	
	
	public IdealLanguageGen(){
		this.sgs = new HashMap<String, IdealLanguageGen.SemanticGenerator>();
	}
	
	public void add(String sgname, String output){
		SemanticGenerator sg = sgs.get(sgname);
		if(sg == null){
			sg = new SemanticGenerator(sgname);
			sgs.put(sgname, sg);
		}
		sg.add(output);
		
	}
	
	
	public String parseIntoSampledSentence(String structure){
		
		String semRegEx = "<.*?>";
		List <String> res = getAllRegExMatchs(semRegEx, structure);
		String converted = structure;
		for(String sem : res){
			SemanticGenerator gen = this.sgs.get(this.getSemGen(sem));
			converted = converted.replaceFirst(sem, gen.sample());
		}
		
		return converted;
	}
	
	public String getSemSentence(String structure){
		
		String semRegEx = "<.*?>";
		List <String> res = getAllRegExMatchs(semRegEx, structure);
		StringBuffer buf = new StringBuffer(structure.length());
		for(int i = 0; i < res.size(); i++){
			String jsem = this.getSemGen(res.get(i));
			if(i > 0){
				buf.append(" ");
			}
			buf.append(jsem);
		}
		
		return buf.toString();
	}
	
	public String getSemGen(String taggedSem){
		String mFront = taggedSem.substring(1);
		String mEnd = mFront.substring(0, mFront.length()-1);
		return mEnd;
	}
	
	public static List <String> getAllRegExMatchs(String regExPattern, String searchContent){
		List<String> result = new ArrayList<String>();
		
		Pattern p = Pattern.compile(regExPattern);
		Matcher matcher = p.matcher(searchContent);
		while(matcher.find()){
			result.add(matcher.group());
		}
		
		return result;
	}
	
	
	class SemanticGenerator{
		
		String name;
		List <String> possibleOutputs;
		Random rand;
		
		public SemanticGenerator(String name){
			this.name = name;
			rand = new Random();
			this.possibleOutputs = new ArrayList<String>();
		}
		
		public void add(String item){
			this.possibleOutputs.add(item);
		}
		
		public String sample(){
			int r = this.rand.nextInt(possibleOutputs.size());
			return this.possibleOutputs.get(r);
		}
		

	}
	
	
	public void writeSampleToFile(String filePath, String semanticSentence, String structure){
		String sampled = this.parseIntoSampledSentence(structure);
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
			out.write(semanticSentence + "\n");
			out.write(sampled + "\n");
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void generateSampleFiles(String baseFilePath, int n, String semanticSentence, String structure){
		
		for(int i = 0; i < n; i++){
			String filePath = baseFilePath + i + ".txt";
			this.writeSampleToFile(filePath, semanticSentence, structure);
		}
		
	}
	
	public static void main(String [] args){
		
		IdealLanguageGen idl = new IdealLanguageGen();
		
		idl.add("block", "block");
		idl.add("block", "star");
		idl.add("block", "box");
		idl.add("block", "piece");
		
		idl.add("red", "red");
		idl.add("red", "rose");
		idl.add("red", "crimson");
		idl.add("red", "cherry");
		
		idl.add("green", "green");
		idl.add("green", "jade");
		idl.add("green", "olive");
		idl.add("green", "emerald");
		
		idl.add("blue", "blue");
		idl.add("blue", "navy");
		idl.add("blue", "sapphire");
		idl.add("blue", "cobalt");
		
		idl.add("room", "room");
		idl.add("room", "place");
		idl.add("room", "area");
		idl.add("room", "location");
		
		idl.add("go", "go to");
		idl.add("go", "arrive at");
		idl.add("go", "enter");
		idl.add("go", "leave for");
		
		idl.add("push", "push");
		idl.add("push", "pull");
		idl.add("push", "take");
		idl.add("push", "move");
		
		
		//String dir = "dataFiles/commands/simpleSemanticLabeled/";
		String dir = "dataFiles/commands/simpleSemanticLabeledDoubleColor/";
		
		System.out.println("Starting.");
		
		/*
		idl.generateSampleFiles(dir+"agentToRed", 40, "isRed agentInRoom", "<go> the <red> <room>");
		idl.generateSampleFiles(dir+"agentToGreen", 40, "isGreen agentInRoom", "<go> the <green> <room>");
		idl.generateSampleFiles(dir+"agentToBlue", 40, "isBlue agentInRoom", "<go> the <blue> <room>");
		
		idl.generateSampleFiles(dir+"blockToRed", 40, "isRed blockInRoom", "<push> the <block> to the <red> <room>");
		idl.generateSampleFiles(dir+"blockToGreen", 40, "isGreen blockInRoom", "<push> the <block> to the <green> <room>");
		idl.generateSampleFiles(dir+"blockToBlue", 40, "isBlue blockInRoom", "<push> the <block> to the <blue> <room>");
		*/
		
		/*
		idl.generateSampleFiles(dir+"agentToRed", 40, "isRed agentInRoom", "<go> the <red> <room>");
		idl.generateSampleFiles(dir+"agentToGreen", 40, "isGreen agentInRoom", "<go> the <green> <room>");
		idl.generateSampleFiles(dir+"agentToBlue", 40, "isBlue agentInRoom", "<go> the <blue> <room>");
		*/
		
		
		idl.generateSampleFiles(dir+"blockGreenToRed", 40, "isGreen isRed blockInRoom", "<push> the <block> in the <green> <room> to the <red> <room>");
		idl.generateSampleFiles(dir+"blockBlueToRed", 40, "isBlue isRed blockInRoom", "<push> the <block> in the <blue> <room> to the <red> <room>");
		
		idl.generateSampleFiles(dir+"blockRedToGreen", 40, "isRed isGreen blockInRoom", "<push> the <block> in the <red> <room> to the <green> <room>");
		idl.generateSampleFiles(dir+"blockBlueToGreen", 40, "isBlue isGreen blockInRoom", "<push> the <block> in the <blue> <room> to the <green> <room>");
		
		idl.generateSampleFiles(dir+"blockRedToBlue", 40, "isRed isBlue blockInRoom", "<push> the <block> in the <red> <room> to the <blue> <room>");
		idl.generateSampleFiles(dir+"blockGreenToBlue", 40, "isGreen isBlue blockInRoom", "<push> the <block> in the <green> <room> to the <blue> <room>");
		
		
		
		
		System.out.println("Finished.");
		
		
		
	}
	
	
	public static void simpleTest(){
		IdealLanguageGen idl = new IdealLanguageGen();
		for(int i = 0; i < 100; i++){
			idl.add("sem1", "s1"+i);
		}
		for(int i = 0; i < 5; i++){
			idl.add("sem2", "s2"+i);
		}
		
		String tstS = "hi <sem2> there <sem1> <sem2>";
		for(int i = 0; i < 10; i++){
			System.out.println(idl.parseIntoSampledSentence(tstS));
		}
		
		System.out.println("-------------");
		System.out.println(idl.getSemSentence(tstS));
		
	}
	
	
	
	
}
