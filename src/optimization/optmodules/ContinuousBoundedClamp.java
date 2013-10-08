package optimization.optmodules;

import optimization.OptVariables;
import optimization.VariableClamper;

public class ContinuousBoundedClamp implements VariableClamper {

	protected double [] low;
	protected double [] high;
	
	public ContinuousBoundedClamp(double [] low, double [] high){
		this.low = low;
		this.high = high;
	}
	
	@Override
	public void clamp(OptVariables instance) {
		
		for(int i = 0; i < low.length; i++){
			double v = instance.v(i);
			if(v < low[i]){
				instance.set(i, low[i]);
			}
			else if(v > high[i]){
				instance.set(i, high[i]);
			}
		}

	}

}
