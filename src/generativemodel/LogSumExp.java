package generativemodel;

import java.util.List;

public class LogSumExp {
	
	
	public static void main(String [] args){
		
		double [] pTerms = new double[]{0.3, 0.2, 0.7};
		double [] lTerms = new double[pTerms.length];
		double psum = 0.;
		for(int i = 0; i < pTerms.length; i++){
			psum += pTerms[i];
			lTerms[i] = Math.log(pTerms[i]);
		}
		
		System.out.println(Math.log(psum) + " " + logSumOfExponentials(lTerms));
		
	}
	
	
	public static double logSumOfExponentials(double [] exponentialTerms){
		
		double maxTerm = Double.NEGATIVE_INFINITY;
		for(double d : exponentialTerms){
			if(d > maxTerm){
				maxTerm = d;
			}
		}
		
		double sum = 0.;
		for(double d : exponentialTerms){
			sum += Math.exp(d - maxTerm);
		}
		
		return maxTerm + Math.log(sum);
	}
	
	
	
	
	public static double logSumOfExponentials(List<Double> exponentialTerms){
		
		double maxTerm = Double.NEGATIVE_INFINITY;
		for(double d : exponentialTerms){
			if(d > maxTerm){
				maxTerm = d;
			}
		}
		
		double sum = 0.;
		for(double d : exponentialTerms){
			sum += Math.exp(d - maxTerm);
		}
		
		return maxTerm + Math.log(sum);
	}
	
	
}
