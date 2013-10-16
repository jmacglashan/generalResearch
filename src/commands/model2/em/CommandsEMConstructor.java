package commands.model2.em;

import commands.model2.em.logmodel.EMMMNLPLogModule;
import commands.model2.em.logmodel.EMTALogModel;
import commands.model2.em.logmodel.LogPDataManager;

import em.Dataset;
import em.EMAlgorithm;
import generativemodel.GenerativeModel;

public class CommandsEMConstructor {

	public static EMAlgorithm getCommandsPEMAlgorithm(GenerativeModel gm, Dataset ds){
		
		PDataManager pdm = new PDataManager(gm);
		PDataEMAux aux = new PDataEMAux(pdm);
		
		EMTAModule tamod = new EMTAModule(pdm);
		EMMMNLPModule nlpmod = new EMMMNLPModule(pdm);
		
		EMAlgorithm em = new EMAlgorithm(gm, ds);
		em.addEMModule(tamod);
		em.addEMModule(nlpmod);
		em.addAux(aux);
		
		return em;
	}
	
	public static EMAlgorithm getCommandsLogEMAlgorithm(GenerativeModel gm, Dataset ds){
		
		LogPDataManager lpdm = new LogPDataManager(gm);
		PDataEMAux aux = new PDataEMAux(lpdm);
		
		EMTALogModel tamod = new EMTALogModel(lpdm);
		EMMMNLPLogModule nlpmod = new EMMMNLPLogModule(lpdm);
		
		EMAlgorithm em = new EMAlgorithm(gm, ds);
		em.addEMModule(tamod);
		em.addEMModule(nlpmod);
		em.addAux(aux);
		
		return em;
	}
	
	
}
