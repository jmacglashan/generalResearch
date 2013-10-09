package commands.experiments;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

import commands.PFClassTemplate;
import commands.model2.CommandsLearningDriver;
import commands.model2.gm.TAModule.AbstractCondition;
import commands.model2.gm.TAModule.AbstractConditionsValue;
import commands.model2.gm.TAModule.HollowTaskValue;

import domain.singleagent.dogtraining.DTStateParser;
import domain.singleagent.dogtraining.DogTraining;

public class DogTrainingTest {

	
	//public static String 						DATASETTESTPATH = "dataFiles/commands/dogTrainingData";
	public static String 						DATASETTESTPATH = "dataFiles/commands/dogTrainingData2";

	
	protected String							datasetPath;
	protected Domain							oomdpDomain;
	protected List <HollowTaskValue>			hollowTasks;
	protected List <String>						constraintPFClasses;
	protected List <AbstractConditionsValue>	goalConditionValues;
	protected StateParser						stateParser;
	protected DiscreteStateHashFactory			hashingFactory;
	
	public CommandsLearningDriver				driver;
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		DogTrainingTest tester = new DogTrainingTest(DATASETTESTPATH);
		
		tester.driver.initializeGMandEM();
		tester.driver.runEM(5);
		tester.driver.printResultsOnTrainingDataset();
		

	}
	
	
	
	public DogTrainingTest(String datasetPath){
		
		this.datasetPath = datasetPath;
		
		DogTraining dt = new DogTraining(5, 5, true);
		this.oomdpDomain = dt.generateDomain();
		PropositionalFunction constant = new ConstantPF("constantPF", oomdpDomain, new String[]{DogTraining.CLASSDOG}, "constantClass");
		
		this.stateParser = new DTStateParser(oomdpDomain);
		
		hollowTasks = new ArrayList<HollowTaskValue>();
		
		HollowTaskValue htd = new HollowTaskValue();
		htd.addVaraible(DogTraining.CLASSDOG, "d");
		hollowTasks.add(htd);
		
		HollowTaskValue htdtl = new HollowTaskValue();
		htdtl.addVaraible(DogTraining.CLASSDOG, "d");
		htdtl.addVaraible(DogTraining.CLASSLOCATION, "l");
		hollowTasks.add(htdtl);
		
		HollowTaskValue htttl = new HollowTaskValue();
		htttl.addVaraible(DogTraining.CLASSDOG, "d");
		htttl.addVaraible(DogTraining.CLASSTOY, "t");
		htttl.addVaraible(DogTraining.CLASSLOCATION, "l");
		hollowTasks.add(htttl);
		
		
		constraintPFClasses = new ArrayList<String>();
		constraintPFClasses.add("constantClass");
		constraintPFClasses.add(DogTraining.PFCLASSID);
		
		
		goalConditionValues = new ArrayList<AbstractConditionsValue>();
		
		AbstractConditionsValue atWallC = new AbstractConditionsValue();
		atWallC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSWALL), new String[]{"d"}));
		atWallC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSWAIT), new String[]{"d"}));
		goalConditionValues.add(atWallC);
		
		AbstractConditionsValue atHasToyC = new AbstractConditionsValue();
		atHasToyC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSTC), new String[]{"d"}));
		atHasToyC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSWAIT), new String[]{"d"}));
		goalConditionValues.add(atHasToyC);
		
		AbstractConditionsValue atLocC = new AbstractConditionsValue();
		atLocC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSLOC), new String[]{"d", "l"}));
		atLocC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSWAIT), new String[]{"d"}));
		goalConditionValues.add(atLocC);
		
		AbstractConditionsValue toyAtLocC = new AbstractConditionsValue();
		toyAtLocC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSTLOC), new String[]{"t", "l"}));
		toyAtLocC.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, DogTraining.PFCLASSWAIT), new String[]{"d"}));
		goalConditionValues.add(toyAtLocC);
		
		
		this.hashingFactory = new DiscreteStateHashFactory();
		this.hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, this.oomdpDomain.getAttribute(DogTraining.ATTX));
		this.hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, this.oomdpDomain.getAttribute(DogTraining.ATTY));
		this.hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, this.oomdpDomain.getAttribute(DogTraining.ATTHOLDING));
		this.hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, this.oomdpDomain.getAttribute(DogTraining.ATTWAITING));
		
		this.hashingFactory.addAttributeForClass(DogTraining.CLASSTOY, this.oomdpDomain.getAttribute(DogTraining.ATTX));
		this.hashingFactory.addAttributeForClass(DogTraining.CLASSTOY, this.oomdpDomain.getAttribute(DogTraining.ATTY));
		
		
		driver = new CommandsLearningDriver(oomdpDomain, hollowTasks, constraintPFClasses, goalConditionValues, this.datasetPath, stateParser, this.hashingFactory, false);
		
		
	}
	
	
	
	
	
	
	public static class ConstantPF extends PropositionalFunction{

		public ConstantPF(String name, Domain domain,
				String[] parameterClasses, String pfClassName) {
			super(name, domain, parameterClasses, pfClassName);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			return true;
		}
		
		
		
		
		
	}

}
