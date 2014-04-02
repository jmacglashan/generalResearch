package optimization;

public class OptVariables {

	public double [] vars;
	
	public OptVariables(int n){
		vars = new double[n];
	}
	
	public OptVariables(double [] vars){
		this.vars = vars.clone();
	}
	
	public OptVariables(OptVariables ovs){
		this.vars = ovs.vars.clone();
	}
	
	public double v(int i){
		return vars[i];
	}
	
	public void set(int i, double v){
		vars[i] = v;
	}
	
	public int size(){
		return vars.length;
	}
	
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer(vars.length*10);
		
		for(int i = 0; i < vars.length; i++){
			if(i > 0){
				buf.append(",");
			}
			buf.append(vars[i]);
		}
		
		return buf.toString();
	}
	
	@Override
	public boolean equals(Object o){
		OptVariables ov = (OptVariables)o;
		return ov.toString().equals(this.toString());
	}
	
	
	
}
