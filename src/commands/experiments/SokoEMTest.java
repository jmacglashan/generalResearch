package commands.experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;

import commands.PFClassTemplate;
import commands.model2.CommandsLearningDriver;
import commands.model2.CommandsLearningDriver.LOORewardResult;
import commands.model2.gm.TAModule.AbstractCondition;
import commands.model2.gm.TAModule.AbstractConditionsValue;
import commands.model2.gm.TAModule.HollowTaskValue;

import domain.singleagent.sokoban.SokobanDomain;
import domain.singleagent.sokoban.SokobanParser;

public class SokoEMTest {

		//public static String 						DATASETTESTPATH = "code/sokoDataForEM";
		//public static String 						DATASETTESTPATH = "code/allTurkTrainLimitedCommand";
		public static String 						DATASETTESTPATH = "commands/mySimpleSokoData";
		//public static String 						DATASETTESTPATH = "code/allTurkTrainBlockLimitedCommand";
		//public static String 						DATASETTESTPATH = "code/allTurkTrainAgentLimitedCommand";

		
		protected String							datasetPath;
		protected Domain							oomdpDomain;
		protected List <HollowTaskValue>			hollowTasks;
		protected List <String>						constraintPFClasses;
		protected List <AbstractConditionsValue>	goalConditionValues;
		protected StateParser						stateParser;
		protected StateHashFactory					hashingFactory;
		
		public CommandsLearningDriver				driver;
		
		
		
		
		/**
		 * @param args
		 */
		public static void main(String[] args) {
			
			
			//hardCodedTests();
			trainingDataTest();
			//looTest(args);

		}
		
		
		public static void looTest(String [] args){
			
			if(args.length != 4){
				System.out.println("Format:\n\tpathToTrainingDataset leaveOutTest pathToOutput numIterations");
				System.exit(-1);
			}
			
			String datasetPath = args[0];
			int looInstance = Integer.parseInt(args[1]);
			int numIters = Integer.parseInt(args[3]);
			
			
			SokoEMTest tester = new SokoEMTest(datasetPath);
			
			
			String outPath = args[2];
			if(!outPath.endsWith("/")){
				outPath = outPath + "/";
			}
			outPath = outPath + tester.driver.getCommandsTrainingElement(looInstance).identifier;
			
			
			
			tester.runLOOTestAndWrite(looInstance, numIters, outPath);
			
			
		}
		
		public static void hardCodedTests(){
			
			//System.out.println("color(r)&&^^position(a r)&&^^".hashCode());
			
			SokoEMTest tester = new SokoEMTest(DATASETTESTPATH);
			//tester.runLOOTestAndPrint(0, 5);
			
			
			
			tester.driver.initializeGMandEM();
			tester.driver.runEM(1);
			tester.driver.printResultsOnTrainingDataset();
			
			
		}
		
		public static void trainingDataTest(){
			
			SokoEMTest tester = new SokoEMTest(DATASETTESTPATH);
			List <GroundedProp> goalFeatures = tester.getPossibleGoalFeatures();
			
			Map<String, Map<String, Integer>> confusionMatrix = new HashMap<String, Map<String,Integer>>();
			for(GroundedProp gp : goalFeatures){
				Map<String, Integer> cols = new HashMap<String, Integer>();
				confusionMatrix.put(gp.toString(), cols);
				for(GroundedProp gp2 : goalFeatures){
					cols.put(gp2.toString(), 0);
				}
			}
			
			List<LOORewardResult> results = tester.driver.performTrainingDataTest(5, tester.getPossibleGoalFeatures());
			
			int n = 0;
			int c = 0;
			for(LOORewardResult r : results){
				String actual = r.actualRFFeatures.get(0).toString();
				String predicted = r.predictedRFFeatures.get(0).toString();
				n++;
				if(actual.equals(predicted)){
					c++;
				}
				
				Map <String, Integer> cols = confusionMatrix.get(actual);
				int curN = cols.get(predicted);
				cols.put(predicted, curN+1);
				
			}
		
			for(GroundedProp gp : goalFeatures){
				Map<String, Integer> cols = confusionMatrix.get(gp.toString());
				System.out.print(gp.toString());
				for(GroundedProp gp2 : goalFeatures){
					System.out.print("\t" + cols.get(gp2.toString()));
				}
				System.out.println("");
			}
			System.out.println("-----");
			System.out.println("accuracy: " + ((double)c/(double)n));
			
			
			
		}
		
		
		
		public SokoEMTest(String datasetPath) {
			
			this.datasetPath = datasetPath;
			
			
			this.oomdpDomain = (new SokobanDomain()).generateDomain();
			this.stateParser = new SokobanParser();
			
			
			
			hollowTasks = new ArrayList<HollowTaskValue>();
			
			HollowTaskValue htar = new HollowTaskValue();
			htar.addVaraible(SokobanDomain.AGENTCLASS, "a");
			htar.addVaraible(SokobanDomain.ROOMCLASS, "r");
			
			HollowTaskValue htbr = new HollowTaskValue();
			htbr.addVaraible(SokobanDomain.BLOCKCLASS, "b");
			htbr.addVaraible(SokobanDomain.ROOMCLASS, "r");
			
			hollowTasks.add(htar);
			hollowTasks.add(htbr);
			
			constraintPFClasses = new ArrayList<String>();
			constraintPFClasses.add(SokobanDomain.PFRCOLORCLASS);
			constraintPFClasses.add(SokobanDomain.PFBCOLORCLASS);
			constraintPFClasses.add(SokobanDomain.PFSHAPECLASS);
			
			
			goalConditionValues = new ArrayList<AbstractConditionsValue>();
			
			AbstractConditionsValue ainr = new AbstractConditionsValue();
			ainr.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, SokobanDomain.PFPOSCLASS), new String[]{"a", "r"}));
			
			AbstractConditionsValue binr = new AbstractConditionsValue();
			binr.addCondition(new AbstractCondition(PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, SokobanDomain.PFBPOSCLASS), new String[]{"b", "r"}));
			
			goalConditionValues.add(ainr);
			goalConditionValues.add(binr);
			
			
			hashingFactory = new DiscreteStateHashFactory();
			((DiscreteStateHashFactory)hashingFactory).addAttributeForClass(SokobanDomain.AGENTCLASS, oomdpDomain.getAttribute(SokobanDomain.XATTNAME));
			((DiscreteStateHashFactory)hashingFactory).addAttributeForClass(SokobanDomain.AGENTCLASS, oomdpDomain.getAttribute(SokobanDomain.YATTNAME));
			((DiscreteStateHashFactory)hashingFactory).addAttributeForClass(SokobanDomain.BLOCKCLASS, oomdpDomain.getAttribute(SokobanDomain.XATTNAME));
			((DiscreteStateHashFactory)hashingFactory).addAttributeForClass(SokobanDomain.BLOCKCLASS, oomdpDomain.getAttribute(SokobanDomain.YATTNAME));
			
			
			driver = new CommandsLearningDriver(oomdpDomain, hollowTasks, constraintPFClasses, goalConditionValues, this.datasetPath, stateParser, hashingFactory, true);
			
			
		}
		
		
		
		public void runLOOTestAndPrint(int looInstance, int nIterations){
			
			System.out.println(this.runLOOTest(looInstance, nIterations).toString());
			
		}
		
		
		public void runLOOTestAndWrite(int looInstance, int nIterations, String outputPath){
			
			
			LOORewardResult res = this.runLOOTest(looInstance, nIterations);
			
			try {
				
				BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
			
				out.write(res.toString()+"\n");
				out.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
		
		
		public LOORewardResult runLOOTest(int looInstance, int nIterations){
			
			List <GroundedProp> goalFeatures = this.getPossibleGoalFeatures();
			
			return this.driver.performLOOTest(looInstance, nIterations, goalFeatures);
			
		}
		
		
		public List <GroundedProp> getPossibleGoalFeatures(){
			
			List <GroundedProp> goalFeatures = new ArrayList<GroundedProp>();
			
			PropositionalFunction agentInRoom = oomdpDomain.getPropFunction(SokobanDomain.PFAGENTINROOM);
			PropositionalFunction blockInRoom = oomdpDomain.getPropFunction(SokobanDomain.PFBLOCKINROOM);
			
			GroundedProp ar = new GroundedProp(agentInRoom, new String[]{"agent0","room1"});
			GroundedProp ag = new GroundedProp(agentInRoom, new String[]{"agent0","room0"});
			GroundedProp ab = new GroundedProp(agentInRoom, new String[]{"agent0","room2"});
			
			GroundedProp br = new GroundedProp(blockInRoom, new String[]{"block0","room1"});
			GroundedProp bg = new GroundedProp(blockInRoom, new String[]{"block0","room0"});
			GroundedProp bb = new GroundedProp(blockInRoom, new String[]{"block0","room2"});
			
			
			goalFeatures.add(ag);
			goalFeatures.add(ar);
			goalFeatures.add(ab);
			goalFeatures.add(bg);
			goalFeatures.add(br);
			goalFeatures.add(bb);
			
			
			return goalFeatures;
			
			
		}
	
}
