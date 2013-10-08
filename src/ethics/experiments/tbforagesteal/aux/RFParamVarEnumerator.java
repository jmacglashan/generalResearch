package ethics.experiments.tbforagesteal.aux;

import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;

public class RFParamVarEnumerator {

	public List<OptVariables>		allRFs;
	public double					lower = -1.5;
	public double					upper = 2.5;
	public double 					inc = 0.5;
	
	
	public RFParamVarEnumerator(){
		
		int n = (int)((upper-lower)/inc) + 1;
		allRFs = new ArrayList<OptVariables>(n);
		
		for(int i = 0; i < n; i++){
			double pi = lower + inc*i;
			for(int j = 0; j < n; j++){
				double pj = lower + inc*j;
				for(int k = 0; k < n; k++){
					double pk = lower + inc*k;
					double [] params = new double []{pi, pj, pk};
					OptVariables vars = new OptVariables(params);
					this.allRFs.add(vars);
				}
			}
		}
		
		
	}
	
}
