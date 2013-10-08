package ethics.experiments.tbforagesteal.aux;

import optimization.OVarStringRep;
import optimization.OptVariables;
import ethics.ParameterizedRF;
import ethics.ParameterizedRFFactory;

public class OVarSRStringRep implements OVarStringRep {

protected ParameterizedRFFactory		sourceRFactory;
	
	public OVarSRStringRep(ParameterizedRFFactory sourceRFFactory) {
		this.sourceRFactory = sourceRFFactory;
	}

	@Override
	public String getStringRep(OptVariables vars) {
		
		ParameterizedRF rf = sourceRFactory.generateRF(vars.vars);
		
		return rf.toString();
	}

}
