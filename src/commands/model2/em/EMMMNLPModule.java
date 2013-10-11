package commands.model2.em;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import commands.model2.gm.CommandsModelConstructor;
import commands.model2.gm.IRLModule;
import commands.model2.gm.MMNLPModule;
import commands.model2.gm.MMNLPModule.StringValue;
import commands.model2.gm.StateRVValue;
import commands.model2.gm.TAModule;
import commands.model2.gm.IRLModule.BehaviorValue;
import commands.model2.gm.TAModule.TaskDescriptionValue;
import em.EMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import generativemodel.common.MultiNomialRVPI;

public class EMMMNLPModule extends EMModule {

	protected RVariable												goalRV;
	protected RVariable												constraintRV;
	protected RVariable												pfRV;
	protected RVariable												wordRV;
	protected RVariable												commandRV;
	
	
	protected PDataManager											pdManager;
	protected MMNLPModule											gmNLPMod;
	
	protected Map<GMQuery, Double>									jointCounts;
	protected Map<RVariableValue, Double>							pfCounts;
	
	
	protected double												additiveConstnat = 0.0;
	
	protected boolean												updateConstantParams = false;
	
	public EMMMNLPModule(PDataManager pdManager) {
		this.pdManager = pdManager;
	}
	
	
	@Override
	public void setGenerativeModelSrc(GenerativeModel gm){
		super.setGenerativeModelSrc(gm);
		
		
		this.goalRV = gm.getRVarWithName(TAModule.GNAME);
		this.constraintRV = gm.getRVarWithName(TAModule.CNAME);
		this.pfRV = gm.getRVarWithName(MMNLPModule.PNAME);
		this.wordRV = gm.getRVarWithName(MMNLPModule.WNAME);
		this.commandRV = gm.getRVarWithName(MMNLPModule.CNAME);
		
		this.gmNLPMod = (MMNLPModule)this.gm.getModuleWithName(CommandsModelConstructor.NLPMODNAME);
		
		this.initializeCountDatastructures();
		
	}
	
	
	protected void initializeCountDatastructures(){
		
		jointCounts = new HashMap<GMQuery, Double>();
		pfCounts = new HashMap<RVariableValue, Double>();
		
	}
	

	@Override
	public void runEStep(int dataInstanceId, List<RVariableValue> observables) {
		
		double pData = this.pdManager.getProbForData(dataInstanceId, observables);
		
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StringValue command = (StringValue)this.getCommandValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		Map<StringValue, Integer> commandWordCount = this.gmNLPMod.getWordCounts(command.s);
		
		GCIterator gcIter = new GCIterator(srvv);
		while(gcIter.hasNext()){
			
			GoalConstraintPair gcp = gcIter.next();
			TaskDescriptionValue goal = gcp.goal;
			TaskDescriptionValue constraint = gcp.constraint;
			
			//get probability of behavior side
			double rbMarg = this.getRewardBehaviorProb(goal, constraint, srvv, behavior);
			
			//get full command probability
			GMQuery commandQuery = new GMQuery();
			commandQuery.addQuery(command);
			commandQuery.addCondition(goal);
			commandQuery.addCondition(constraint);
			double probOfFullCommand = this.gm.getProb(commandQuery, true).probability;
			
			//get probability of command minus a single occurance of each word in it
			Map<StringValue, Double> pcmso = this.probOfCommandMinusOneOccurance(probOfFullCommand, commandWordCount.keySet(), goal, constraint);
			
			//iterate over pf values
			List <RVariableValue> gcConditions = new ArrayList<RVariableValue>();
			gcConditions.add(goal);
			gcConditions.add(constraint);
			Iterator<GMQueryResult> pfIter = this.gm.getNonZeroIterator(pfRV, gcConditions, true);
			
			
			while(pfIter.hasNext()){
				
				GMQueryResult pfRes = pfIter.next();
				StringValue pfsv = (StringValue)pfRes.getSingleQueryVar();
				
				
				double sumPFVal = 0.;
				for(Map.Entry<StringValue, Integer> cwc : commandWordCount.entrySet()){
					StringValue word = cwc.getKey();
					int woccur = cwc.getValue();
					
					GMQuery wordProbQuery = new GMQuery();
					wordProbQuery.addQuery(word);
					wordProbQuery.addCondition(pfsv);
					
					GMQueryResult wordProbRes = this.gm.getProb(wordProbQuery, true);
					double wp = wordProbRes.probability;
					
					double numerator = woccur * gcp.jointProb * rbMarg * pfRes.probability * wp * pcmso.get(word);
					sumPFVal += numerator;
					
					
					double jointCountVal = numerator / pData;
					GMQuery jointCountIndex = new GMQuery();
					jointCountIndex.addQuery(word);
					jointCountIndex.addQuery(pfsv);
					this.accumulateJoint(jointCountVal, jointCountIndex, jointCounts);
					
				}
				
				double parentCount = sumPFVal / pData;
				this.accumulateParent(parentCount, pfsv, pfCounts);
				
				
			}
			
			
			
			
		}
		

	}

	@Override
	public void runMStep() {
		
		int vsize = this.gmNLPMod.vocabSize();
		
		Iterator<RVariableValue> pfIter = this.gm.getRVariableValuesFor(pfRV);
		while(pfIter.hasNext()){
			RVariableValue pfv = pfIter.next();
			
			if(!this.updateConstantParams && ((StringValue)pfv).s.equals(this.gmNLPMod.constantFeatureName())){
				continue;
			}
			
			double nPF = 0.;
			Double DnPF = this.pfCounts.get(pfv);
			if(DnPF != null){
				nPF = DnPF;
			}
			
			Iterator<RVariableValue> wordIter = this.gm.getRVariableValuesFor(wordRV);
			while(wordIter.hasNext()){
				RVariableValue wv = wordIter.next();
				
				GMQuery jointQuery = new GMQuery();
				jointQuery.addQuery(wv);
				jointQuery.addQuery(pfv);
				
				double nPFW = 0.;
				Double DnPFW = this.jointCounts.get(jointQuery);
				if(DnPFW != null){
					nPFW = DnPFW;
				}
				
				double newParam = 0.;
				if(nPF != 0.){
					newParam = (nPFW + additiveConstnat)/(nPF + (additiveConstnat*vsize));
					//newParam = (nPFW)/(nPF);
				}
				if(Double.isNaN(newParam)){
					//System.out.println("Error: nan parameter set for word: " + wv.toString() + " and PF: " + pfv.toString());
				}
				
				if(pfv.toString().contains("agentInRoom")){
					//System.out.println(pfv.toString() + ";" + wv.toString() + " " + newParam);
				}
				
				MultiNomialRVPI paramIndex = new MultiNomialRVPI(wv);
				paramIndex.addConditional(pfv);
				wordRV.setParam(paramIndex, newParam);
				
				//System.out.println(pfv.toString() + " " + wv.toString() + " " + newParam);
				
			}
			
		}
		
		this.initializeCountDatastructures(); //setup datastructures for next E step
	}
	
	
	
	protected Map<StringValue, Double> probOfCommandMinusOneOccurance(double probOfFullComamnd, Collection<StringValue> words, TaskDescriptionValue goal, TaskDescriptionValue constraints){
		
		Map <StringValue, Double> probs = new HashMap<MMNLPModule.StringValue, Double>();
		Set <String> pfs = this.gmNLPMod.getUniquePFNames(constraints, goal);
		
		for(StringValue v : words){
			double pv = this.gmNLPMod.probWGivenProps(pfs, v);
			double pcmv = 0.;
			if(pv > 0. || probOfFullComamnd > 0.){ //use "or" because it will break the code for us (which is good there is a problem we should know about!) if we get a weird result like the numerator being > 0 but not the denominator
				pcmv = probOfFullComamnd / pv;
			}
			probs.put(v, pcmv);
		}
		
		
		return probs;
	}
	
	
	protected double getRewardBehaviorProb(RVariableValue goal, RVariableValue constraint, RVariableValue state, RVariableValue behavior){
		
		List <RVariableValue> taskConds = new ArrayList<RVariableValue>();
		taskConds.add(goal);
		taskConds.add(constraint);
		taskConds.add(state);
		
		double sumRB = 0.;
		Iterator<GMQueryResult> rIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.RNAME), taskConds, true);
		while(rIterRes.hasNext()){
			GMQueryResult rRes = rIterRes.next();
			
			GMQuery bQuery = new GMQuery();
			bQuery.addQuery(behavior);
			bQuery.addCondition(rRes.getSingleQueryVar());
			bQuery.addCondition(state);
			
			GMQueryResult bRes = this.gm.getProb(bQuery, true);
			sumRB += rRes.probability*bRes.probability;
			
		}
		
		return sumRB;
		
	}
	
	
	protected void accumulateJoint(double v, GMQuery query, Map <GMQuery, Double> index){
		Double curD = index.get(query);
		double cur = 0.;
		if(curD != null){
			cur = curD;
		}
		index.put(query, cur+v);
		
	}
	
	protected void accumulateParent(double v, RVariableValue query, Map <RVariableValue, Double> index){
		Double curD = index.get(query);
		double cur = 0.;
		if(curD != null){
			cur = curD;
		}
		index.put(query, cur+v);
		
	}
	
	
	public RVariableValue getCommandValue(List<RVariableValue> observables){
		RVariable command = gm.getRVarWithName(MMNLPModule.CNAME);
		
		for(RVariableValue rv : observables){
			if(rv.isValueFor(command)){
				return rv;
			}
		}
		
		return null;
		
	}
	
	public RVariableValue getBehaviorValue(List<RVariableValue> observables){
		RVariable behavior = gm.getRVarWithName(IRLModule.BNAME);
		
		for(RVariableValue rv : observables){
			if(rv.isValueFor(behavior)){
				return rv;
			}
		}
		
		return null;
		
	}
	
	
	
	protected class GoalConstraintPair{
		
		public TaskDescriptionValue			goal;
		public TaskDescriptionValue			constraint;
		public double						jointProb;
		
		
		
		public GoalConstraintPair(TaskDescriptionValue goal, TaskDescriptionValue constraint, double jointProb){
			this.goal = goal;
			this.constraint = constraint;
			this.jointProb = jointProb;
		}
		
		@Override
		public boolean equals(Object other){
			GoalConstraintPair that = (GoalConstraintPair)other;
			return this.goal.equals(that.goal) && this.constraint.equals(that.constraint);
		}
		
		@Override
		public int hashCode(){
			return (goal.toString() + "^&^" + constraint.toString()).hashCode();
		}
		
	}
	
	
	protected class GCIterator implements Iterator<GoalConstraintPair>{

		List<GoalConstraintPair>		possibleValues;
		Iterator<GoalConstraintPair>	gcLocalIter;
		
		
		public GCIterator(RVariableValue srvv){
			
			possibleValues = new ArrayList<EMMMNLPModule.GoalConstraintPair>();
			
			
			Map<GMQuery, Double> probSums = new HashMap<GMQuery, Double>();
			
			List <RVariableValue> sconds = new ArrayList<RVariableValue>();
			sconds.add(srvv);
			
			//iterate hollow task
			Iterator<GMQueryResult> htIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.HTNAME), sconds, true);
			while(htIterRes.hasNext()){
				GMQueryResult hres = htIterRes.next();
				RVariableValue htv = hres.getSingleQueryVar();
				List <RVariableValue> hsConds = new ArrayList<RVariableValue>();
				hsConds.add(srvv);
				hsConds.add(htv);
				
				//iterte absctract constraint
				Iterator<GMQueryResult> abConIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.ACNAME), hsConds, true);
				while(abConIterRes.hasNext()){
					GMQueryResult abcRes = abConIterRes.next();

					//iterate abstract goal
					Iterator<GMQueryResult> abGIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.AGNAME), hsConds, true);
					while(abGIterRes.hasNext()){
						GMQueryResult abgRes = abGIterRes.next();

						
						List <RVariableValue> abConds = new ArrayList<RVariableValue>();
						abConds.add(abcRes.getSingleQueryVar());
						abConds.add(abgRes.getSingleQueryVar());
						abConds.add(srvv);
						
						//iterate constraint
						Iterator<GMQueryResult> cIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.CNAME), abConds, true);
						while(cIterRes.hasNext()){
							GMQueryResult cRes = cIterRes.next();

							//iterate goal
							Iterator<GMQueryResult> gIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.GNAME), abConds, true);
							while(gIterRes.hasNext()){
								GMQueryResult gRes = gIterRes.next();
								
								double p = hres.probability * abcRes.probability * abgRes.probability * cRes.probability * gRes.probability;
								GMQuery cgJoint = new GMQuery();
								cgJoint.addQuery(cRes.getSingleQueryVar());
								cgJoint.addQuery(gRes.getSingleQueryVar());
								
								Double storedP = probSums.get(cgJoint);
								if(storedP == null){
									probSums.put(cgJoint, p);
								}
								else{
									probSums.put(cgJoint, p+storedP);
								}
								
							}
							
							
						}
						
						
					}
					
				}
				
			}
			
			for(GMQuery q : probSums.keySet()){
				TaskDescriptionValue goal = (TaskDescriptionValue)q.getQueryForVariable(goalRV);
				TaskDescriptionValue constraint = (TaskDescriptionValue)q.getQueryForVariable(constraintRV);
				double p = probSums.get(q);
				GoalConstraintPair gcp = new GoalConstraintPair(goal, constraint, p);
				possibleValues.add(gcp);
			}
			
			gcLocalIter = possibleValues.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return gcLocalIter.hasNext();
		}

		@Override
		public GoalConstraintPair next() {
			return gcLocalIter.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
		
	}
	

}
