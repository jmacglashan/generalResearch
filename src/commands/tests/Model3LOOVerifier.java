package commands.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

public class Model3LOOVerifier {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1){
			System.out.println("Format:\n\tpathToPredictedDir");
			System.exit(0);
		}
		
		Map<String, String> labels = Model3ControllerTest.getOriginalDatasetRFLabels();
		Map<String, String> predicted = getPredictions(args[0], "txt");
		
		int c = 0;
		int i = 0;
		for(Map.Entry<String, String> e : predicted.entrySet()){
			String pred = e.getValue();
			String ident = e.getKey();
			String label = labels.get(ident);
			System.out.print(i + " ");
			if(pred.equals(label)){
				System.out.print("Correct ");
				c++;
			}
			else{
				System.out.print("Incorrect ");
			}
			System.out.println(ident);
			i++;
		}
		
		System.out.println(c + "/" + predicted.size() + "; " + ((double)c/(double)predicted.size()));

	}
	
	
	public static Map<String, String> getPredictions(String predictedDir, String extension){
		
		//get rid of trailing /
		if(predictedDir.charAt(predictedDir.length()-1) == '/'){
			predictedDir = predictedDir.substring(0, predictedDir.length());
		}
		
		File dir = new File(predictedDir);
		final String ext = new String(extension);
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(ext)){
					return true;
				}
				return false;
			}
		};
		String[] children = dir.list(filter);
		
		Map<String, String> predicted = new HashMap<String, String>(children.length);
		for(String child : children){
			String path = predictedDir + "/" + child;
			try {
				BufferedReader in = new BufferedReader(new FileReader(path));
				String res = in.readLine().trim();
				predicted.put(child, res);
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Could not open/read file: " + path);
			}
		}
		
		return predicted;
	}

}
