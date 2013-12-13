package ethics.experiments.tbforagesteal.aux;

import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;

public class RFParamVarEnumerator {

	public List<OptVariables>		allRFs;
	public double					lower = -1.5;
	public double					upper = 2.5;
	public double 					inc = 0.5;
	
	
	public static void main(String [] args){
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator();
		RFParamVarEnumerator rfenum = new RFParamVarEnumerator(-1, 3, 1., 4);
		for(OptVariables v : rfenum.allRFs){
			System.out.println(v);
		}
		System.out.println(rfenum.allRFs.size());
	}
	
	
	public RFParamVarEnumerator(){
		this.generate(3);
	}
	
	public RFParamVarEnumerator(double lower, double upper, double inc, int nParams){
		this.lower = lower;
		this.upper = upper;
		this.inc = inc;
		this.generate(nParams);
	}
	
	public void generate(int nParam){
		
		int n = (int)((upper-lower)/inc) + 1;
		allRFs = new ArrayList<OptVariables>(n);
		double [] params = new double[nParam];
		this.recursivelyGenerateParams(params, 0, n);
		
	}
	
	protected void recursivelyGenerateParams(double [] params, int ind, int n){
		for(int i = 0; i < n; i++){
			params[ind] = lower + inc*i;
			if(ind == params.length-1){
				OptVariables vars = new OptVariables(params.clone());
				this.allRFs.add(vars);
			}
			else{
				recursivelyGenerateParams(params, ind+1, n);
			}
		}
	}
	
}
