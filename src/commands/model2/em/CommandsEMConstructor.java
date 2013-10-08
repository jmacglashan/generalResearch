package commands.model2.em;

import em.Dataset;
import em.EMAlgorithm;
import generativemodel.GenerativeModel;

public class CommandsEMConstructor {

	public static EMAlgorithm getCommandsEMAlgorithm(GenerativeModel gm, Dataset ds){
		
		PDataManager pdm = new PDataManager(gm);
		
		EMTAModule tamod = new EMTAModule(pdm);
		EMMMNLPModule nlpmod = new EMMMNLPModule(pdm);
		
		EMAlgorithm em = new EMAlgorithm(gm, ds);
		em.addEMModule(tamod);
		em.addEMModule(nlpmod);
		
		return em;
	}
	
}
