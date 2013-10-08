package commands.model2;

import em.Dataset;
import em.EMAlgorithm;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

import commands.data.TrainingElement;
import commands.data.TrainingElementParser;
import commands.data.Trajectory;
import commands.model2.em.CommandsEMConstructor;
import commands.model2.gm.CommandsModelConstructor;
import commands.model2.gm.IRLModule.ConjunctiveGroundedPropTF;
import commands.model2.gm.MMNLPModule;
import commands.model2.gm.MMNLPModule.StringValue;
import commands.model2.gm.StateRVValue;
import commands.model2.gm.TAModule;
import commands.model2.gm.TAModule.AbstractConditionsValue;
import commands.model2.gm.TAModule.HollowTaskValue;
import commands.model2.gm.TAModule.RFConVariableValue;

public class CommandsLearningDriver {

	protected Domain							oomdpDomain;
	protected List <HollowTaskValue>			hollowTasks;
	protected List <String>						constraintPFClasses;
	protected List <AbstractConditionsValue>	goalConditionValues;
	protected StateParser						stateParser;
	protected List <TrainingElement>			commandsDataset;
	protected StateHashFactory					hashingFactory;
	protected boolean							addTerminateAction;
	
	
	
	protected GenerativeModel					gm;
	protected EMAlgorithm						em;
	protected Dataset							gmDataset;
	
	
	public CommandsLearningDriver(Domain oomdpDomain, List <HollowTaskValue> hollowTasks, List <String> constraintPFClasses, 
			List <AbstractConditionsValue> goalConditionValues, String pathToTrainingDataset, StateParser stateParser, StateHashFactory hashingFactory, boolean addTerminateAction) {
		
		
		this.oomdpDomain = oomdpDomain;
		this.hollowTasks = hollowTasks;
		this.constraintPFClasses = constraintPFClasses;
		this.goalConditionValues = goalConditionValues;
		this.stateParser = stateParser;
		this.commandsDataset = this.getCommandsDataset(pathToTrainingDataset, stateParser);
		this.hashingFactory = hashingFactory;
		this.addTerminateAction = addTerminateAction;
		
		
	}
	
	
	public TrainingElement getCommandsTrainingElement(int i){
		return this.commandsDataset.get(i);
	}
	
	public List <TrainingElement> getCommandsDataset(String path, StateParser sp){
		
		TrainingElementParser teparser = new TrainingElementParser(oomdpDomain, sp);
		
		//get dataset
		List<TrainingElement> dataset = teparser.getTrainingElementDataset(path, ".txt");
		
		return dataset;
	}
	
	
	public void initializeGMandEM(){
		
		this.gm = CommandsModelConstructor.generateModel(oomdpDomain, hollowTasks, constraintPFClasses, goalConditionValues, commandsDataset, hashingFactory, addTerminateAction);
		this.gmDataset = CommandsModelConstructor.convertCommandsDatasetToGMDataset(gm, commandsDataset);
		this.em = CommandsEMConstructor.getCommandsEMAlgorithm(this.gm, this.gmDataset);
		
	}
	
	public void initializeGMandEMForLOO(int i){
		List <TrainingElement> looCommandsDataset = new ArrayList<TrainingElement>(commandsDataset);
		looCommandsDataset.remove(i);
		
		this.gm = CommandsModelConstructor.generateModel(oomdpDomain, hollowTasks, constraintPFClasses, goalConditionValues, looCommandsDataset, hashingFactory, addTerminateAction);
		this.gmDataset = CommandsModelConstructor.convertCommandsDatasetToGMDataset(gm, looCommandsDataset);
		this.em = CommandsEMConstructor.getCommandsEMAlgorithm(this.gm, this.gmDataset);
	}
	
	
	public void runEM(int nIterations){
		this.em.runEM(nIterations);
	}
	
	public LOORewardResult performLOOTest(int looInstance, int nIterations, List <GroundedProp> possibleGoalFeatures){
		
		this.initializeGMandEMForLOO(looInstance);
		this.em.runEM(nIterations);
		//this.printResultsOnTrainingDataset();
		
		//get test results
		TrainingElement te = this.commandsDataset.get(looInstance);
		State s = te.trajectory.getState(0);
		GroundedProp actualGoalFeature = this.getGoalFeature(te.trajectory, possibleGoalFeatures);
		List <GMQueryResult> rDist = this.getRewardFunctionProbabilityDistributionForCommand(te.command, s);
		
		return new LOORewardResult(te.command, actualGoalFeature, rDist);
		
	}
	
	
	public List<LOORewardResult> performTrainingDataTest(int nIterations, List <GroundedProp> possibleGoalFeatures){
		
		this.initializeGMandEM();
		this.em.runEM(nIterations);
		
		List <LOORewardResult> results = new ArrayList<CommandsLearningDriver.LOORewardResult>(this.commandsDataset.size());
		for(TrainingElement te : this.commandsDataset){
			State s = te.trajectory.getState(0);
			GroundedProp actualGoalFeature = this.getGoalFeature(te.trajectory, possibleGoalFeatures);
			List <GMQueryResult> rDist = this.getRewardFunctionProbabilityDistributionForCommand(te.command, s);
			results.add(new LOORewardResult(te.command, actualGoalFeature, rDist));
		}
		
		
		return results;
		
	}
	
	
	
	public void printResultsOnTrainingDataset(){
		
		for(TrainingElement te : commandsDataset){
			List <GMQueryResult> rDist = this.getRewardFunctionProbabilityDistributionForCommand(te.command, te.trajectory.getState(0));
			double maxp = 0.0;
			for(GMQueryResult r : rDist){
				if(r.probability > maxp){
					maxp = r.probability;
				}
			}
			
			System.out.println(te.command);
			for(GMQueryResult r : rDist){
				if(r.probability == maxp){
					System.out.print("**");
				}
				System.out.println(r.probability + "\t" + r.getSingleQueryVar().toString());
			}
			System.out.println("-----------------------");
		}
		
	}
	
	
	public GMQueryResult getMaximiumLikelihoodRewardFunctionForCommand(String command, State s){
		
		List <GMQueryResult> results = this.getRewardFunctionProbabilityDistributionForCommand(command, s);
		GMQueryResult max = null;
		double maxp = 0.;
		
		for(GMQueryResult res : results){
			if(res.probability > maxp){
				maxp = res.probability;
				max = res;
			}
		}
		
		return max;
		
	}
	
	
	public List <GMQueryResult> getRewardFunctionProbabilityDistributionForCommand(String command, State s){
		
		StringValue crv = new StringValue(command, this.gm.getRVarWithName(MMNLPModule.CNAME));
		StateRVValue srv = new StateRVValue(s, this.gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		return this.getRewardFunctionProbabilityDistributionForCommand(crv, srv);
		
	}
	
	
	public RFTFPair getMaximiumLikelihoodTaskForCommand(String command, State s){
		
		GMQueryResult maxQuery = this.getMaximiumLikelihoodRewardFunctionForCommand(command, s);
		
		RFConVariableValue rval = (RFConVariableValue)maxQuery.getSingleQueryVar();
		ConjunctiveGroundedPropTF tf = new ConjunctiveGroundedPropTF(rval.rf);
		
		RFTFPair pair = new RFTFPair(rval.rf, tf);
		
		return pair;
		
	}
	
	
	public List <GMQueryResult> getRewardFunctionProbabilityDistributionForCommand(StringValue crv, StateRVValue srv){
		
		Map<RVariableValue, Double> rewardDist = new HashMap<RVariableValue, Double>();
		double sumCommandProb = 0.;
		
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srv);
		
		
		Iterator<GMQueryResult> htIterRes;
		htIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.HTNAME), sconds, true);
		while(htIterRes.hasNext()){
			GMQueryResult hres = htIterRes.next();
			RVariableValue htv = hres.getSingleQueryVar();
			List <RVariableValue> hsConds = new ArrayList<RVariableValue>();
			hsConds.add(srv);
			hsConds.add(htv);
			
			
			Iterator<GMQueryResult> abConIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.ACNAME), hsConds, true);
			while(abConIterRes.hasNext()){
				GMQueryResult abcRes = abConIterRes.next();
				

				double abcPStack = hres.probability*abcRes.probability;
				
				if(Double.isNaN(abcRes.probability)){
					System.out.println("NaN in abstract constraint");
				}
				
				Iterator<GMQueryResult> abGIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.AGNAME), hsConds, true);
				while(abGIterRes.hasNext()){
					GMQueryResult abgRes = abGIterRes.next();

					double abgPStack = abcPStack*abgRes.probability;
					
					if(Double.isNaN(abgRes.probability)){
						System.out.println("NaN in abstract goal");
					}
					
					List <RVariableValue> abConds = new ArrayList<RVariableValue>();
					abConds.add(abcRes.getSingleQueryVar());
					abConds.add(abgRes.getSingleQueryVar());
					abConds.add(srv);
					
					
					Iterator<GMQueryResult> cIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.CNAME), abConds, true);
					while(cIterRes.hasNext()){
						GMQueryResult cRes = cIterRes.next();
						
						double cPStack = abgPStack*cRes.probability;
						
						Iterator<GMQueryResult> gIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.GNAME), abConds, true);
						while(gIterRes.hasNext()){
							GMQueryResult gRes = gIterRes.next();
							
							double gPStack = cPStack*abgPStack;
							
							if(Double.isNaN(gRes.probability)){
								System.out.println("NaN in goal");
							}
							
							GMQuery commandQuery = new GMQuery();
							commandQuery.addQuery(crv);
							commandQuery.addCondition(cRes.getSingleQueryVar());
							commandQuery.addCondition(gRes.getSingleQueryVar());
							double commandProb = this.gm.getProb(commandQuery, true).probability;
							
							if(Double.isNaN(commandProb)){
								System.out.println("NaN in command prob");
							}
							
							double commandPStack = gPStack*commandProb;
							sumCommandProb += commandPStack;
							
							if(Double.isNaN(commandPStack)){
								System.out.println("NaN in command stack");
							}
							
							List <RVariableValue> rConds = new ArrayList<RVariableValue>();
							rConds.add(cRes.getSingleQueryVar());
							rConds.add(gRes.getSingleQueryVar());
							rConds.add(srv);
							
							
							
							Iterator<GMQueryResult> rIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.RNAME), rConds, true);
							while(rIterRes.hasNext()){
								GMQueryResult rRes = rIterRes.next();
								RVariableValue rVal = rRes.getSingleQueryVar();
								
								double rProb = commandPStack * rRes.probability;
								if(Double.isNaN(rProb)){
									System.out.println("NaN in reward stack");
								}
								
								
								if(rewardDist.containsKey(rVal)){
									double oldVal = rewardDist.get(rVal);
									rewardDist.put(rVal, oldVal+rProb);
								}
								else{
									rewardDist.put(rVal, rProb);
								}
								
								
							}
							
							
							
						}
					}
					
				}
				
			}
						
			
		}
		
		
		if(sumCommandProb == 0.){
			System.out.println("Error command prob is zero");
		}
		
		
		List <GMQueryResult> results = new ArrayList<GMQueryResult>();
		for(Map.Entry<RVariableValue, Double> e : rewardDist.entrySet()){
			double p = e.getValue() / sumCommandProb;
			GMQueryResult res = new GMQueryResult(p);
			res.addQuery(e.getKey());
			res.addCondition(crv);
			res.addCondition(srv);
			results.add(res);
		}
		
		
		
		return results;
		
	}
	
	
	
	protected GroundedProp getGoalFeature(Trajectory t, List <GroundedProp> possibleGoalFeatures){
		
		for(GroundedProp gp : possibleGoalFeatures){
			if(isGoalFeature(t, gp)){
				return gp;
			}
		}
		
		return null;
		
	}
	
	
	protected boolean isGoalFeature(Trajectory t, GroundedProp gp){
		
		
		State finalS = t.getState(t.numStates()-1);
		if(t.numStates() == 1){
			return gp.isTrue(finalS);
		}
		
		
		State penultimate = t.getState(t.numStates()-2);
		
		
		if(gp.isTrue(finalS) && !gp.isTrue(penultimate)){
			return true;
		}
		
		return false;
		
	}
	
	
	public static class RFTFPair{
		
		public RewardFunction		rf;
		public TerminalFunction		tf;
		
		public RFTFPair(RewardFunction rf, TerminalFunction tf){
			this.rf = rf;
			this.tf = tf;
		}
		
	}
	
	
	public static class LOORewardResult{
		
		public String				command;
		public List <GroundedProp>	actualRFFeatures;
		public List <GroundedProp>	predictedRFFeatures;
		public List <GMQueryResult>	rewardDistribution;
		
		public LOORewardResult(String command, GroundedProp actual, List<GMQueryResult> dist){
			
			this.command = command;
			
			this.actualRFFeatures = new ArrayList<GroundedProp>();			
			this.actualRFFeatures.add(actual);

			
			this.rewardDistribution = dist;
			
			
			Collections.sort(this.rewardDistribution, new Comparator<GMQueryResult>() {

				@Override
				public int compare(GMQueryResult o1, GMQueryResult o2) {
					
					RFConVariableValue rf1 = (RFConVariableValue)o1.getSingleQueryVar();
					RFConVariableValue rf2 = (RFConVariableValue)o2.getSingleQueryVar();
					
					return rf1.toString().compareTo(rf2.toString());

				}

				
			});
			
			
			double maxP = -1.;
			GMQueryResult maxR = null;
			for(GMQueryResult r : this.rewardDistribution){
				if(r.probability > maxP){
					maxP = r.probability;
					maxR = r;
				}
			}
			
			if(maxR == null){
				System.out.println("Error, no reward GPs; size " + this.rewardDistribution.size());
			}
			
			this.predictedRFFeatures = new ArrayList<GroundedProp>(((RFConVariableValue)maxR.getSingleQueryVar()).rf.gps);
			

		}
		
		
		@Override
		public String toString(){
			
			List <String> actualSReps = new ArrayList<String>(actualRFFeatures.size());
			List <String> predictedSReps = new ArrayList<String>(predictedRFFeatures.size());
			
			for(GroundedProp gp : this.actualRFFeatures){
				actualSReps.add(gp.toString());
			}
			
			for(GroundedProp gp : this.predictedRFFeatures){
				predictedSReps.add(gp.toString());
			}
			
			Collections.sort(actualSReps);
			Collections.sort(predictedSReps);
					
			StringBuffer buf = new StringBuffer();
			buf.append(this.command).append("\n");
			
			for(int i = 0; i < actualSReps.size(); i++){
				String f = actualSReps.get(i);
				if(i > 0){
					buf.append(" ");
				}
				buf.append(f);
			}
			buf.append("\n");
			
			for(int i = 0; i < predictedSReps.size(); i++){
				String f = predictedSReps.get(i);
				if(i > 0){
					buf.append(" ");
				}
				buf.append(f);
			}
			
			
			for(GMQueryResult r : this.rewardDistribution){
				buf.append("\n").append(r.probability).append(" ").append(r.getSingleQueryVar().toString());
			}
			
			
			return buf.toString();
			
		}
		
		
	}
	

}
