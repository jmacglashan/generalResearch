package optimization.optmodules;

import java.util.Random;

import burlap.debugtools.RandomFactory;
import optimization.VariableRandomGenerator;

public class ContinuousBoundedVarGen extends VariableRandomGenerator {

	protected double [] low;
	protected double [] high;
	
	protected Random rand;
	
	public ContinuousBoundedVarGen(double [] low, double [] high){
		this.low = low;
		this.high = high;
		rand = RandomFactory.getMapped(0);
	}
	
	@Override
	public double valueForVar(int i) {
		
		double l = low[i];
		double h = high[i];
		
		return rand.nextDouble()*(h-l) + l;
	}

}
