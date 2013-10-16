package commands.model2.em;

import em.EMAuxiliaryCode;

public class PDataEMAux implements EMAuxiliaryCode {

	protected PDataManager pd;
	
	public PDataEMAux(PDataManager pd){
		this.pd = pd;
	}
	
	@Override
	public void preEStep() {
		this.pd.resetTrainingDataProbs();
	}

}
