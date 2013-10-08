package commands.model2.gm;

import domain.singleagent.sokoban.SokobanDomain;
import em.Dataset;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import behavior.irl.DGDIRLFactory;
import behavior.irl.TabularIRLPlannerFactory;
import behavior.vfa.heterogenousafd.stateenumerators.HashingFactoryEnumerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;

import commands.data.TrainingElement;
import commands.model2.gm.IRLModule.BehaviorValue;
import commands.model2.gm.MMNLPModule.StringValue;
import commands.model2.gm.TAModule.AbstractCondition;
import commands.model2.gm.TAModule.AbstractConditionsValue;
import commands.model2.gm.TAModule.HollowTaskValue;

public class CommandsModelConstructor {

	public static final String				STATERVNAME = "state";
	public static final String				CLENRVNAME = "commandLen";
	
	public static final String				TAMODNAME = "TA";
	public static final String				IRLMODNAME = "IRL";
	public static final String				NLPMODNAME = "NLP";
	
	
	
	public static GenerativeModel generateModel(Domain oomdpDomain, List <HollowTaskValue> hollowTasks, List <String> constraintPFClasses, List <AbstractConditionsValue> goalConditionValues, List <TrainingElement> dataset, StateHashFactory hashingFactory, boolean useTerminateAction){
		
		RVariable stateRV = new RVariable(STATERVNAME);
		RVariable commandLenRV = new RVariable(CLENRVNAME);
		
		List<RVariable> inputVariables = new ArrayList<RVariable>(2);
		inputVariables.add(stateRV);
		inputVariables.add(commandLenRV);
		
		GenerativeModel gm = new GenerativeModel(inputVariables);
		
		TAModule ta = new TAModule(TAMODNAME, stateRV, oomdpDomain, hollowTasks, constraintPFClasses, goalConditionValues);
		gm.addGMModule(ta);
		
		
		TabularIRLPlannerFactory irlPlannerFactory = getIRLPlannerFactory(oomdpDomain, hashingFactory);
		IRLModule irl = new IRLModule(IRLMODNAME, stateRV, gm.getRVarWithName(TAModule.RNAME), oomdpDomain, irlPlannerFactory, useTerminateAction, true);
		gm.addGMModule(irl);
		
		
		
		List <String> commands = extractCommands(dataset);
		List <String> pfNames = getAllPossiblePFNames(oomdpDomain, constraintPFClasses, goalConditionValues);
		MMNLPModule nlp = new MMNLPModule(NLPMODNAME, commandLenRV, ta.constraintRV, ta.goalRV, pfNames, commands);
		gm.addGMModule(nlp);
		
		
		return gm;
	}
	
	
	
	
	public static Dataset convertCommandsDatasetToGMDataset(GenerativeModel gm, List <TrainingElement> cDataset){
		
		Dataset dataset = new Dataset();
		
		for(TrainingElement te : cDataset){
			
			RVariableValue crvv = getVarValOfCommands(gm, te);
			RVariableValue brvv = getVarValOfTrajectory(gm, te);
			
			List <RVariableValue> instance = new ArrayList<RVariableValue>(2);
			instance.add(crvv);
			instance.add(brvv);
			
			dataset.addDataInstance(instance);
			
		}
		
		return dataset;
		
	}
	
	public static RVariableValue getVarValOfCommands(GenerativeModel gm, TrainingElement te){
		StringValue sv = new StringValue(te.command, gm.getRVarWithName(MMNLPModule.CNAME));
		return sv;
	}
	
	public static RVariableValue getVarValOfTrajectory(GenerativeModel gm, TrainingElement te){
		BehaviorValue bv = new BehaviorValue(te.trajectory, gm.getRVarWithName(IRLModule.BNAME));
		return bv;
	}
	
	
	private static TabularIRLPlannerFactory getIRLPlannerFactory(Domain d, StateHashFactory hashingFactory){
		
		DGDIRLFactory plannerFactory = new DGDIRLFactory(d, 0.99, hashingFactory);
		
		return plannerFactory;
		
	}
	
	
	private static List <String> extractCommands(List <TrainingElement> dataset){
		
		List <String> commands = new ArrayList<String>(dataset.size());
		for(TrainingElement te : dataset){
			commands.add(te.command);
		}
		
		return commands;
		
	}
	
	private static List <String> getAllPossiblePFNames(Domain domain, List <String> constraintPFClasses, List <AbstractConditionsValue> goalConditionValues){
		
		Map <String, Set<PropositionalFunction>> pfClassMap = domain.getPropositionlFunctionsMap();
		
		
		Set <String> pfNames = new HashSet<String>();
		
		for(AbstractConditionsValue gv : goalConditionValues){
			for(AbstractCondition ac : gv.conditions){
				String cname = ac.pfclass.className;
				Set <PropositionalFunction> pfsForClass = pfClassMap.get(cname);
				for(PropositionalFunction pf : pfsForClass){
					pfNames.add(pf.getName());
				}
			}
		}
		
		for(String cname : constraintPFClasses){
			Set <PropositionalFunction> pfsForClass = pfClassMap.get(cname);
			for(PropositionalFunction pf : pfsForClass){
				pfNames.add(pf.getName());
			}
		}
		
		
		return new ArrayList<String>(pfNames);
	}
	
	
	
	
	
}
