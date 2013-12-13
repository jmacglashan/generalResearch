package commands.mttest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;


public class MTDataInstance {

	/**
	 * Text in the language which generates the other language in the generative model
	 */
	public TokenedString genLangText;
	
	/**
	 * Text in the language that is produced by the generating language in the generative model
	 */
	public TokenedString prodLangText;
	
	
	public MTDataInstance(TokenedString genLangText, TokenedString prodLangText){
		this.genLangText = genLangText;
		this.prodLangText = prodLangText;
	}
	
	public MTDataInstance(Tokenizer tokenizer, String genLenString, String prodLangString){
		this.genLangText = tokenizer.tokenize(genLenString);
		this.prodLangText = tokenizer.tokenize(prodLangString);
	}
	
	
	public static MTDataInstance getDataInstanceFromFile(Tokenizer tokenizer, String pathToFile){
		
		MTDataInstance d = null;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(pathToFile));
			String l1 = in.readLine().trim();
			String l2 = in.readLine().trim();
			
			d = new MTDataInstance(tokenizer.tokenize(l1), tokenizer.tokenize(l2));
			
			in.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return d;
		
	}
	
	public static List<MTDataInstance> getDatsetFromDir(Tokenizer tokenizer, String pathToDataDir, String dataFileExtension){
		
		List <MTDataInstance> dataset = new ArrayList<MTDataInstance>();
		
		//get rid of trailing /
		if(pathToDataDir.charAt(pathToDataDir.length()-1) == '/'){
			pathToDataDir = pathToDataDir.substring(0, pathToDataDir.length());
		}
		
		File dir = new File(pathToDataDir);
		final String ext = new String(dataFileExtension);
		
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
			String path = pathToDataDir + "/" + children[i];
			dataset.add(getDataInstanceFromFile(tokenizer, path));
		}
		
		return dataset;
	}
	
	
}
