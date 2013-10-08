package optimization.optmodules;

import optimization.OptVariables;
import optimization.VariableClamper;

public class DiscStepVarClamp implements VariableClamper {

	protected double [] low;
	protected double [] high;
	protected double interval;
	
	public DiscStepVarClamp(double [] low, double [] high, double interval) {
		this.low = low;
		this.high = high;
		this.interval = interval;
	}

	@Override
	public void clamp(OptVariables instance) {
		
		for(int i = 0; i < low.length; i++){
			
			double l = low[i];
			double h = high[i];

			int mxi = (int) ((h-l)/interval);
			
			double rv = instance.v(i);
			
			double nrintervals = (rv - l) / interval;
			int nintervals = (int)nrintervals;
			
			double cval = nintervals*interval + l;
			if(rv - cval > (0.5*interval)){ //then round up
				cval += interval;
			}
			
			if(nintervals > mxi){
				cval = mxi*interval + l;
			}
			else if(nintervals < 0){
				cval = l;
			}
			
			instance.set(i, cval);
			
		}

	}

}
