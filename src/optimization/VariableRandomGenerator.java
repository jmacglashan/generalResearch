package optimization;

public abstract class VariableRandomGenerator {
	public abstract double valueForVar(int i);
	
	public OptVariables getVars(int m){
		OptVariables vars = new OptVariables(m);
		for(int i = 0; i < m; i++){
			vars.vars[i] = this.valueForVar(i);
		}
		return vars;
	}
	
}
