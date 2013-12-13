package metarl.chainproblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnvionrmentSampler {

	static int randomSeed = 284563927;
	
	
	
	public static List <ChainGenerator> sampleClass(int i, int n){
		if(i == 1){
			return M1();
		}
		else if(i == 2){
			return sampleM2(n);
		}
		else if(i == 3){
			return sampleM3(n);
		}
		else if(i == 4){
			return sampleM4(n);
		}
		return null;
	}
	
	public static List <ChainGenerator> M1(){
		ChainGenerator cg = new ChainGenerator(new int[]{0,1,2,3,4}, 0.2);
		List <ChainGenerator> sample = new ArrayList<ChainGenerator>(1);
		sample.add(cg);
		return sample;
	}
	
	public static List<ChainGenerator> sampleM2(int n){
		
		List <ChainGenerator> result = new ArrayList<ChainGenerator>(n);
		
		Random rand = new Random(randomSeed);
		
		double slipLower = 0.19;
		double slipUpper = 0.21;
		
		for(int i = 0; i < n; i++){
			ChainGenerator cg = new ChainGenerator(new int[]{0,1,2,3,4}, uniformInRange(rand, slipLower, slipUpper));
			result.add(cg);
		}
		
		return result;
		
	}
	
	public static List<ChainGenerator> sampleM3(int n){
		
		List <ChainGenerator> result = new ArrayList<ChainGenerator>(n);
		
		Random rand = new Random(randomSeed);
		
		double slipLower = 0.0;
		double slipUpper = 0.5;
		
		for(int i = 0; i < n; i++){
			ChainGenerator cg = new ChainGenerator(new int[]{0,1,2,3,4}, uniformInRange(rand, slipLower, slipUpper));
			result.add(cg);
		}
		
		return result;
		
	}
	
	
	public static List<ChainGenerator> sampleM4(int n){
		
		List <ChainGenerator> result = new ArrayList<ChainGenerator>(n);
		
		Random rand = new Random(randomSeed);
		
		double slipLower = 0.0;
		double slipUpper = 0.5;
		
		for(int i = 0; i < n; i++){
			ChainGenerator cg = new ChainGenerator(shuffle(rand, new int[]{0,1,2,3,4}), uniformInRange(rand, slipLower, slipUpper));
			result.add(cg);
		}
		
		return result;
		
	}
	
	
	public static double uniformInRange(Random rand, double lower, double upper){
		return rand.nextDouble()*(upper-lower) + lower;
	}
	
	public static int [] shuffle(Random rand, int [] states){
		for(int i = 0; i < states.length; i++){
			int v1 = states[i];
			int np = rand.nextInt(states.length);
			int v2 = states[np];
			states[i] = v2;
			states[np] = v1;
		}
		return states;
	}
	
}
