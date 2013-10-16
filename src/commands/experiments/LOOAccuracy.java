package commands.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LOOAccuracy {

	protected Map<String, Integer>				confusionMatrix;
	protected int								n;
	protected String []							features;
	
	
	
	public static void main(String[] args) {
		
		String [] features = new String[] {"agentInRoom(agent0, room0)",
											"agentInRoom(agent0, room1)",
											"agentInRoom(agent0, room2)",
											"blockInRoom(block0, room0)",
											"blockInRoom(block0, room1)",
											"blockInRoom(block0, room2)"};
		
		
		
		LOOAccuracy ac = new LOOAccuracy();
		ac.initializeCMWithPossibleFeatures(features);
		ac.computeConfusionMatrix("dataFiles/commands/nmLimited5IterNormCorrect/");
		ac.printConfusionMatrix();
		System.out.println("Accruacy: " + ac.getAccuracy());
		

	}
	
	
	
	public void initializeCMWithPossibleFeatures(String [] features){
		this.features = features;
		this.confusionMatrix = new HashMap<String, Integer>(features.length*features.length);
		
		for(int i = 0; i < features.length; i++){
			for(int j = 0; j < features.length; j++){
				String code = this.confusionCode(features[i], features[j]);
				confusionMatrix.put(code, 0);
			}
		}
		
	}
	
	
	
	public void printConfusionMatrix(){
		System.out.println("Matrix size: " + this.n);
		System.out.print("-----------");
		for(int i = 0; i < features.length; i++){
			System.out.print("\t" + features[i]);
		}
		System.out.println("");
		
		for(int i = 0; i < features.length; i++){
			System.out.print(features[i]);
			for(int j = 0; j < features.length; j++){
				String code = this.confusionCode(features[i], features[j]);
				System.out.print("\t" + confusionMatrix.get(code));
			}
			System.out.println("");
		}
	}
	
	
	public double getAccuracy(){
		
		int nc = 0;
		for(int i = 0; i < features.length; i++){
			String f = features[i];
			String code = this.confusionCode(f, f);
			int cc = confusionMatrix.get(code);
			nc += cc;
		}
		
		return (double)nc / (double)n;
	}
	
	public void computeConfusionMatrix(String pathToDir){
		
		n = 0;
		
		if(!pathToDir.endsWith("/")){
			pathToDir = pathToDir + "/";
		}
		
		File dir = new File(pathToDir);
		final String ext = new String("txt");
		
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
			String file = children[i];
			String path = pathToDir + file;
			
			String code = this.getConfusionCodeForResult(path);
			if(code.equals(this.confusionCode("null", "null"))){
				continue;
			}
			
			Integer cc = confusionMatrix.get(code);
			n++;
			
			confusionMatrix.put(code, cc+1);
		}
		
	}
	
	
	
	protected String getConfusionCodeForResult(String filePath){
		
		BufferedReader in = open(filePath);
		
		readLine(in); //clear command
		String actual = readLine(in);
		String predicted = readLine(in);
		
		close(in);
		
		
		return actual + "," + predicted;
	}
	
	
	protected String confusionCode(String actual, String predicted){
		return actual + "," + predicted;
	}
	
	
	public static String readLine(BufferedReader in){
		try {
			String line = in.readLine();
			return line;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static BufferedReader open(String filePath){
	
		try {
			BufferedReader in = new BufferedReader(new FileReader(filePath));
			return in;
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}

	
	public static void close(BufferedReader in){
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
