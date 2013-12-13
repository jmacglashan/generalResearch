package metarl.chainproblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AlgorithmSampler {

	static int randomSeed = 284563927;
	
	
	public static List <AlgorithmFactory> sampleClass(int i, int n){
		if(i == 1){
			return A1();
		}
		else if(i == 2){
			return sampleA2(n);
		}
		else if(i == 3){
			return sampleA3(n);
		}
		else if(i == 4){
			return sampleA4(n);
		}
		
		return null;
	}
	
	public static List<AlgorithmFactory> A1(){
		List <AlgorithmFactory> res = new ArrayList<AlgorithmFactory>(1);
		AlgorithmFactory alg = new AlgorithmFactory(0.1, 0.2, constantDoubleArray(5, 2, 0.));
		res.add(alg);
		
		return res;
	}
	
	public static List <AlgorithmFactory> sampleA2(int n){
		List <AlgorithmFactory> res = new ArrayList<AlgorithmFactory>(n);
		
		Random rand = new Random(randomSeed);
		
		double lowerAlpha = 0.09;
		double upperAlpha = 0.11;
		
		for(int i = 0; i < n; i++){
			AlgorithmFactory alg = new AlgorithmFactory(uniformInRange(rand, lowerAlpha, upperAlpha), 0.2, constantDoubleArray(5, 2, 0.));
			res.add(alg);
		}
		
		return res;
	}
	
	
	public static List <AlgorithmFactory> sampleA3(int n){
		List <AlgorithmFactory> res = new ArrayList<AlgorithmFactory>(n);
		
		Random rand = new Random(randomSeed);
		
		double lowerAlpha = 0.0;
		double upperAlpha = 0.5;
		
		double qLower = 0.0;
		double qUpper = 200.0;
		
		double epsilonLower = 0.;
		double epsilonUpper = 0.4;
		
		for(int i = 0; i < n; i++){
			AlgorithmFactory alg = new AlgorithmFactory(uniformInRange(rand, lowerAlpha, upperAlpha), uniformInRange(rand, epsilonLower, epsilonUpper), 
					constantDoubleArray(5, 2, uniformInRange(rand, qLower, qUpper)));
			
			res.add(alg);
		}
		
		return res;
	}
	
	
	public static List <AlgorithmFactory> sampleA4(int n){
		List <AlgorithmFactory> res = new ArrayList<AlgorithmFactory>(n);
		
		Random rand = new Random(randomSeed);
		
		double lowerAlpha = 0.0;
		double upperAlpha = 0.5;
		
		double qLower = 0.0;
		double qUpper = 200.0;
		
		double epsilonLower = 0.;
		double epsilonUpper = 0.4;
		
		for(int i = 0; i < n; i++){
			
			double [][] qm = new double[5][2];
			for(int j = 0; j < qm.length; j++){
				for(int k = 0; k < qm[j].length; k++){
					qm[j][k] = uniformInRange(rand, qLower, qUpper);
				}
			}
			
			AlgorithmFactory alg = new AlgorithmFactory(uniformInRange(rand, lowerAlpha, upperAlpha), uniformInRange(rand, epsilonLower, epsilonUpper), qm);
			res.add(alg);
			
		}
		
		return res;
	}
	
	
	
	public static double[][] constantDoubleArray(int r, int c, double v){
		double [][] m = new double[r][c];
		for(int i = 0; i < r; i++){
			for(int j = 0; j < c; j++){
				m[i][j] = v;
			}
		}
		return m;
	}
	
	public static double uniformInRange(Random rand, double lower, double upper){
		return rand.nextDouble()*(upper-lower) + lower;
	}
	
}
