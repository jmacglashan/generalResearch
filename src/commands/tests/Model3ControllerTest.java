package commands.tests;

import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;

import commands.data.TrainingElement;
import commands.model3.GPConjunction;
import commands.model3.Model3Controller;
import commands.model3.TaskModule;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.RFConVariableValue;
import commands.model3.mt.Tokenizer;

import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.SokobanOldToNewParser;

public class Model3ControllerTest {

	public static String 						DATASETTESTPATH = "dataFiles/commands/allTurkTrain";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		uniformTest();

	}
	
	
	public static void uniformTest(){
		
		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		StateParser sp = new SokobanOldToNewParser(domain);
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);
		
		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);
		
		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);
		
		Model3Controller controller = new Model3Controller(domain, liftedTaskDescriptions, hashingFactory, true);
		GenerativeModel gm = controller.getGM();
		
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);
		
		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");
		
		controller.setToMTLanguageModel(trainingDataset, 17, tokenizer);
		
		TrainingElement te = trainingDataset.get(200);
		List<GMQueryResult> ldistro = controller.getLiftedRFAndBindingDistribution(te.trajectory.getState(0), te.command);
		for(GMQueryResult r : ldistro){
			LiftedVarValue lr = (LiftedVarValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.LIFTEDRFNAME));
			LiftedVarValue b = (LiftedVarValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.BINDINGNAME));
			
			System.out.printf("%.5f\t%s %s\n", r.probability, lr.toString(), b.toString());
		}
		
		System.out.println("===========================================================");
		
		List<GMQueryResult> rdistro = controller.getRFDistribution(te.trajectory.getState(0), te.command);
		for(GMQueryResult r : rdistro){
			RFConVariableValue gr = (RFConVariableValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
			
			
			System.out.printf("%.5f\t%s\n", r.probability, gr.toString());
		}
		
		
		System.out.println("Finished");
		
	}

}
