package optimization;

public class DefaultOVarSRep implements OVarStringRep {


	@Override
	public String getStringRep(OptVariables vars) {
		return vars.toString();
	}


}
