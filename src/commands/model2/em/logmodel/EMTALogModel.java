package commands.model2.em.logmodel;

import em.EMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.LogSumExp;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import generativemodel.common.MultiNomialRVPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import commands.model2.gm.CommandsModelConstructor;
import commands.model2.gm.IRLModule;
import commands.model2.gm.MMNLPModule;
import commands.model2.gm.StateRVValue;
import commands.model2.gm.TAModule;
import commands.model2.gm.IRLModule.BehaviorValue;
import commands.model2.gm.MMNLPModule.StringValue;

public class EMTALogModel extends EMModule {

	
	protected RVariable												hollowRV;
	protected RVariable												aConstraintRV;
	protected RVariable												aGoalRV;
	protected RVariable												goalRV;
	
	protected LogPDataManager										lpdManager;
	
	protected Map<RVariable, Map<GMQuery, List<Double>>>			jointCounts;
	protected Map<RVariable, Map<RVariableValue, List<Double>>>		parentCounts;
	
	protected Map <MultiVarValIndex, Double>						MGamChi;
	protected Map <MultiVarValIndex, Double>						MXR;
	
	
	protected boolean												updateAbstractGoal = true;
	protected boolean												updateAbstractConstraint = true;
	protected boolean												updateGoal = true;
	
	
	public EMTALogModel(LogPDataManager lpdManager) {
		this.lpdManager = lpdManager;
	}

	@Override
	public void setGenerativeModelSrc(GenerativeModel gm){
		super.setGenerativeModelSrc(gm);
		
		this.hollowRV = gm.getRVarWithName(TAModule.HTNAME);
		this.aConstraintRV = gm.getRVarWithName(TAModule.ACNAME);
		this.aGoalRV = gm.getRVarWithName(TAModule.AGNAME);
		this.goalRV = gm.getRVarWithName(TAModule.GNAME);
		
		this.initializeCountDatastructures();
		
	}
	
	
	protected void initializeCountDatastructures(){
		jointCounts = new HashMap<RVariable, Map<GMQuery,List<Double>>>();
		parentCounts = new HashMap<RVariable, Map<RVariableValue,List<Double>>>();
		
		jointCounts.put(aConstraintRV, new HashMap<GMQuery, List<Double>>());
		parentCounts.put(aConstraintRV, new HashMap<RVariableValue, List<Double>>());
		
		jointCounts.put(aGoalRV, new HashMap<GMQuery, List<Double>>());
		parentCounts.put(aGoalRV, new HashMap<RVariableValue, List<Double>>());
		
		jointCounts.put(goalRV, new HashMap<GMQuery, List<Double>>());
		parentCounts.put(goalRV, new HashMap<RVariableValue, List<Double>>());
		
	}
	
	
	
	@Override
	public void runEStep(int dataInstanceId, List<RVariableValue> observables) {
		
		this.computeMValues(observables);
		double lpData = this.lpdManager.getLogProbForData(dataInstanceId, observables);
		
		
		this.computeH(observables, lpData);
		this.computeAGJointAndSingle(observables, lpData);
		this.computeACJoint(observables, lpData);
		this.computeGJoint(observables, lpData);

	}

	@Override
	public void runMStep() {
		
		Map<GMQuery, List<Double>> agJointCounts = jointCounts.get(aGoalRV);
		Map<GMQuery, List<Double>> acJointCounts = jointCounts.get(aConstraintRV);
		Map<GMQuery, List<Double>> gJointCounts = jointCounts.get(goalRV);
		
		Map <RVariableValue, List<Double>> hCounts = parentCounts.get(aGoalRV);
		Map <RVariableValue, List<Double>> agCounts = parentCounts.get(goalRV);
		
		//update the abstract parameters
		Iterator <RVariableValue> hIter = this.gm.getRVariableValuesFor(hollowRV);
		while(hIter.hasNext()){
			
			RVariableValue htval = hIter.next();
			
			List <Double> hdvals = hCounts.get(htval);
			double logHCount = LogSumExp.logSumOfExponentials(hdvals);
			
			//set up for abstract goal and goal parameter updates
			Iterator <RVariableValue> agIter = this.gm.getRVariableValuesFor(aGoalRV);
			while(agIter.hasNext()){
				
				RVariableValue agv = agIter.next();
				
				//manage parameter update for abstract goal
				GMQuery jaghQuery = new GMQuery();
				jaghQuery.addQuery(htval);
				jaghQuery.addQuery(agv);
				
				double nagParam = Double.NEGATIVE_INFINITY;
				
				List <Double> jaghdval = agJointCounts.get(jaghQuery);
				if(jaghdval.size() > 0){
					double logJAGCount = LogSumExp.logSumOfExponentials(jaghdval);
					nagParam = logJAGCount - logHCount;
				}
				MultiNomialRVPI agIndex = new MultiNomialRVPI(agv);
				agIndex.addConditional(htval);
				if(this.updateAbstractGoal){
					aGoalRV.setParam(agIndex, nagParam);
				}
				
				
				
				
				//start iteration for goal parameters
				List <Double> agdval = agCounts.get(agv);
				double logAGCount = LogSumExp.logSumOfExponentials(agdval);
				
				Iterator <RVariableValue> gIter = this.gm.getRVariableValuesFor(goalRV);
				while(gIter.hasNext()){
					
					RVariableValue gv = gIter.next();
					
					GMQuery jgagQuery = new GMQuery();
					jgagQuery.addQuery(agv);
					jgagQuery.addQuery(gv);
					
					double ngParam = Double.NEGATIVE_INFINITY;
					
					List <Double> jgagdval = gJointCounts.get(jgagQuery);
					if(jgagdval.size() > 0){
						double logJGAGCount = LogSumExp.logSumOfExponentials(jgagdval);
						ngParam = logJGAGCount - logAGCount;
					}
					
					MultiNomialRVPI gIndex = new MultiNomialRVPI(gv);
					gIndex.addConditional(agv);
					if(this.updateGoal){
						goalRV.setParam(gIndex, ngParam);
					}
					
				}
				
				
				
			} //end abstract goal iteration
			
			
			
			//setup for abstract constraint parameters
			Iterator<RVariableValue> acIter = this.gm.getRVariableValuesFor(aConstraintRV);
			while(acIter.hasNext()){
				
				RVariableValue acv = acIter.next();
				
				GMQuery jachQuery = new GMQuery();
				jachQuery.addQuery(htval);
				jachQuery.addQuery(acv);
				
				double nacparam = Double.NEGATIVE_INFINITY;
				
				List <Double> jacdval = acJointCounts.get(jachQuery);
				if(jacdval.size() > 0){
					double logJACCount = LogSumExp.logSumOfExponentials(jacdval);
					nacparam = logJACCount - logHCount;
				}
				
				MultiNomialRVPI acIndex = new MultiNomialRVPI(acv);
				acIndex.addConditional(htval);
				if(this.updateAbstractConstraint){
					aConstraintRV.setParam(acIndex, nacparam);
				}
				
			}
			
			
		}
		
		
		this.initializeCountDatastructures();
		

	}
	
	
	
	
	public void computeH(List<RVariableValue> observables, double lpData){
		
		Map<RVariableValue, List<Double>> gParentCount = parentCounts.get(aGoalRV);
		
		
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
		
		//iterate hollow task
		Iterator<GMQueryResult> htIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.HTNAME), sconds, true);
		while(htIterRes.hasNext()){
			GMQueryResult hres = htIterRes.next();
			double hParentTerm = hres.probability;
			hParentTerm -= lpData;
			
			RVariableValue htv = hres.getSingleQueryVar();
			List <RVariableValue> hsConds = new ArrayList<RVariableValue>();
			hsConds.add(srvv);
			hsConds.add(htv);
			
			
			List <Double> agTerms = new ArrayList<Double>();
			Iterator<GMQueryResult> abGIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.AGNAME), hsConds, true);
			while(abGIterRes.hasNext()){
				GMQueryResult abgRes = abGIterRes.next();
				double agTerm = abgRes.probability;
				
				List <Double> acTerms = new ArrayList<Double>();
				Iterator<GMQueryResult> abConIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.ACNAME), hsConds, true);
				while(abConIterRes.hasNext()){
					GMQueryResult abcRes = abConIterRes.next();
					double acTerm = abcRes.probability;
					
					MultiVarValIndex mInd = new MultiVarValIndex();
					mInd.addVal(abcRes.getSingleQueryVar());
					mInd.addVal(abgRes.getSingleQueryVar());
	
					Double m = MGamChi.get(mInd);
					acTerm += m;
					acTerms.add(acTerm);
					
				}
				
				
				double acLogSum = LogSumExp.logSumOfExponentials(acTerms);
				agTerm += acLogSum;
				agTerms.add(agTerm);
				
				
			}
			
			hParentTerm += LogSumExp.logSumOfExponentials(agTerms);
			this.addParentDataTerm(hParentTerm, htv, gParentCount);	
			
		}
		
		
		
	}
	
	
	public void computeAGJointAndSingle(List<RVariableValue> observables, double lpData){
		
		Map<GMQuery, List<Double>> gJointCounts = jointCounts.get(aGoalRV);
		Map<RVariableValue, List<Double>> gParentCount = parentCounts.get(goalRV);
	
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
		
		
		Iterator<RVariableValue> agValIter = this.gm.getRVariableValuesFor(aGoalRV);
		while(agValIter.hasNext()){
			RVariableValue agval = agValIter.next();
			
			List <Double> hTerms = new ArrayList<Double>();
			Iterator<GMQueryResult> hIterRes = this.gm.getNonInfiniteLogProbIterator(hollowRV, sconds, true);
			while(hIterRes.hasNext()){
				GMQueryResult hRes = hIterRes.next();
				
				GMQuery agquery = new GMQuery();
				agquery.addQuery(agval);
				agquery.addCondition(hRes.getSingleQueryVar());
				agquery.addCondition(srvv);
				
				GMQueryResult agres = this.gm.getLogProb(agquery, true);
				if(agres.probability == Double.NEGATIVE_INFINITY){
					continue ;
				}
				
				double jTerm = hRes.probability + agres.probability - lpData;
				double hTerm = hRes.probability + agres.probability;
				
				List <Double> acTerms = new ArrayList<Double>();
				Iterator<GMQueryResult> acIterRes = this.gm.getNonInfiniteLogProbIterator(aConstraintRV, new ArrayList<RVariableValue>(agquery.getConditionValues()), true);
				while(acIterRes.hasNext()){
					GMQueryResult acRes = acIterRes.next();
					double acTerm = acRes.probability;
					
					MultiVarValIndex mInd = new MultiVarValIndex();
					mInd.addVal(acRes.getSingleQueryVar());
					mInd.addVal(agval);
	
					Double m = MGamChi.get(mInd);
					acTerm += m;
					acTerms.add(acTerm);
					
				}
				
				double lse = LogSumExp.logSumOfExponentials(acTerms);
				jTerm += lse;
				hTerm += lse;
				
				GMQuery jointIndex = new GMQuery();
				jointIndex.addQuery(hRes.getSingleQueryVar());
				jointIndex.addQuery(agval);
				this.addJointDataTerm(jTerm, jointIndex, gJointCounts);
				
				hTerms.add(hTerm);
				
			}
			
			
			double aglog = LogSumExp.logSumOfExponentials(hTerms) - lpData;
			this.addParentDataTerm(aglog, agval, gParentCount);
			
			
		}
		
		
	}
	
	
	public void computeGJoint(List <RVariableValue> observables, double lpData){
		
		Map<GMQuery, List<Double>> gJointCounts = jointCounts.get(goalRV);
	
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
		
		
		Iterator<RVariableValue> agValIter = this.gm.getRVariableValuesFor(aGoalRV);
		while(agValIter.hasNext()){
			RVariableValue agval = agValIter.next();
			
			List <RVariableValue> gConds = new ArrayList<RVariableValue>();
			gConds.add(agval);
			
			Iterator<GMQueryResult> gIter = this.gm.getNonInfiniteLogProbIterator(goalRV, gConds, true);
			while(gIter.hasNext()){
				GMQueryResult gRes = gIter.next();
				
				
				List <Double> hTerms = new ArrayList<Double>();
				Iterator<GMQueryResult> hIterRes = this.gm.getNonInfiniteLogProbIterator(hollowRV, sconds, true);
				while(hIterRes.hasNext()){
					GMQueryResult hRes = hIterRes.next();
					double hTerm = hRes.probability;
					
					GMQuery agQuery = new GMQuery();
					agQuery.addQuery(agval);
					agQuery.addCondition(hRes.getSingleQueryVar());
					agQuery.addCondition(srvv);
					
					GMQueryResult agRes = this.gm.getLogProb(agQuery, true);
					
					if(agRes.probability == Double.NEGATIVE_INFINITY){
						continue;
					}
					
					hTerm += agRes.probability + gRes.probability;
					
					List <Double> acTerms = new ArrayList<Double>();
					Iterator<GMQueryResult> acIterRes = this.gm.getNonInfiniteLogProbIterator(aConstraintRV, new ArrayList<RVariableValue>(agQuery.getConditionValues()), true);
					while(acIterRes.hasNext()){
						GMQueryResult acRes = acIterRes.next();
						double acTerm = acRes.probability;
						
						MultiVarValIndex mInd = new MultiVarValIndex();
						mInd.addVal(acRes.getSingleQueryVar());
						mInd.addVal(gRes.getSingleQueryVar());
		
						Double m = this.MXR.get(mInd);
						acTerm += m;
						acTerms.add(acTerm);
						
					}
					
					double lse = LogSumExp.logSumOfExponentials(acTerms);
					hTerm += lse;
					hTerms.add(hTerm);
					
					
				}
				
				
				
				double logSumH = LogSumExp.logSumOfExponentials(hTerms);
				double jointTerm = logSumH - lpData;
				GMQuery jointQuery = new GMQuery();
				jointQuery.addQuery(agval);
				jointQuery.addQuery(gRes.getSingleQueryVar());
				
				this.addJointDataTerm(jointTerm, jointQuery, gJointCounts);
				
			}
			
		}
		
		
	}
	
	
	
	public void computeACJoint(List<RVariableValue> observables, double lpData){
		
		Map<GMQuery, List<Double>> gJointCounts = jointCounts.get(aGoalRV);
	
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
	
		Iterator<RVariableValue> acValIter = this.gm.getRVariableValuesFor(aConstraintRV);
		while(acValIter.hasNext()){
			RVariableValue acval = acValIter.next();
			
			
			Iterator<GMQueryResult> hIterRes = this.gm.getNonInfiniteLogProbIterator(hollowRV, sconds, true);
			while(hIterRes.hasNext()){
				GMQueryResult hRes = hIterRes.next();
				
				GMQuery acquery = new GMQuery();
				acquery.addQuery(acval);
				acquery.addCondition(hRes.getSingleQueryVar());
				acquery.addCondition(srvv);
				
				GMQueryResult acres = this.gm.getLogProb(acquery, true);
				if(acres.probability == Double.NEGATIVE_INFINITY){
					continue ;
				}
				
				double jTerm = hRes.probability + acres.probability - lpData;
				
				List <Double> agTerms = new ArrayList<Double>();
				Iterator<GMQueryResult> agIterRes = this.gm.getNonInfiniteLogProbIterator(aGoalRV, new ArrayList<RVariableValue>(acquery.getConditionValues()), true);
				while(agIterRes.hasNext()){
					GMQueryResult agRes = agIterRes.next();
					double acTerm = agRes.probability;
					
					MultiVarValIndex mInd = new MultiVarValIndex();
					mInd.addVal(agRes.getSingleQueryVar());
					mInd.addVal(acval);
	
					Double m = MGamChi.get(mInd);
					acTerm += m;
					agTerms.add(acTerm);
					
				}
				
				jTerm += LogSumExp.logSumOfExponentials(agTerms);
				GMQuery jointIndex = new GMQuery();
				jointIndex.addQuery(hRes.getSingleQueryVar());
				jointIndex.addQuery(acval);
				this.addJointDataTerm(jTerm, jointIndex, gJointCounts);
				
			}
			
		}
		
		
		
	}
	
	
	
	
	
	
	
	
	
	protected void computeMValues(List<RVariableValue> observables){
		
		
		MGamChi = new HashMap<MultiVarValIndex, Double>();
		MXR = new HashMap<MultiVarValIndex, Double>();
		
		StringValue command = (StringValue)this.getCommandValue(observables);
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
		
		
		//iterate hollow task
		Iterator<GMQueryResult> htIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.HTNAME), sconds, true);
		while(htIterRes.hasNext()){
			GMQueryResult hres = htIterRes.next();
			RVariableValue htv = hres.getSingleQueryVar();
			List <RVariableValue> hsConds = new ArrayList<RVariableValue>();
			hsConds.add(srvv);
			hsConds.add(htv);
			
			//iterate abstract constraint
			Iterator<GMQueryResult> abConIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.ACNAME), hsConds, true);
			while(abConIterRes.hasNext()){
				GMQueryResult abcRes = abConIterRes.next();

				//iterate abstract goal
				Iterator<GMQueryResult> abGIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.AGNAME), hsConds, true);
				while(abGIterRes.hasNext()){
					GMQueryResult abgRes = abGIterRes.next();

					
					List <RVariableValue> abConds = new ArrayList<RVariableValue>();
					abConds.add(abcRes.getSingleQueryVar());
					abConds.add(abgRes.getSingleQueryVar());
					abConds.add(srvv);
					
					
					List <Double> gTerms = new ArrayList<Double>();
					Iterator<GMQueryResult> gIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.GNAME), abConds, true);
					while(gIterRes.hasNext()){
						GMQueryResult gRes = gIterRes.next();
						double gTerm = gRes.probability;
						
						
						List <Double> cTerms = new ArrayList<Double>();
						Iterator<GMQueryResult> cIterRes = gm.getNonZeroIterator(gm.getRVarWithName(TAModule.CNAME), abConds, true);
						while(cIterRes.hasNext()){
							GMQueryResult cRes = cIterRes.next();
							double cTerm = cRes.probability;
							
							List <RVariableValue> taskConds = new ArrayList<RVariableValue>();
							taskConds.add(cRes.getSingleQueryVar());
							taskConds.add(gRes.getSingleQueryVar());
							taskConds.add(srvv);
							
							
							GMQuery commandQuery = new GMQuery();
							commandQuery.addQuery(command);
							commandQuery.setConditions(taskConds);
							
							//add command log prob
							GMQueryResult commandRes = this.gm.getLogProb(commandQuery, true);
							cTerm += commandRes.probability;
							
							List <Double> rTerms = new ArrayList<Double>();
							Iterator<GMQueryResult> rIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.RNAME), taskConds, true);
							while(rIterRes.hasNext()){
								GMQueryResult rRes = rIterRes.next();
								double rTerm = rRes.probability;
								
								GMQuery bQuery = new GMQuery();
								bQuery.addQuery(behavior);
								bQuery.addCondition(rRes.getSingleQueryVar());
								bQuery.addCondition(srvv);
								
								GMQueryResult bRes = this.gm.getLogProb(bQuery, true);
								rTerm += bRes.probability;
								
								
								rTerms.add(rTerm);
								
							}
							
							
							cTerm += LogSumExp.logSumOfExponentials(rTerms);
							cTerms.add(cTerm);
							
							
						}
						
						
						
						double logsumMXR = LogSumExp.logSumOfExponentials(cTerms);
						gTerm += logsumMXR;
						gTerms.add(gTerm);
						
						MultiVarValIndex mchirindex = new MultiVarValIndex();
						mchirindex.addVal(abcRes.getSingleQueryVar());
						mchirindex.addVal(gRes.getSingleQueryVar());
						MXR.put(mchirindex, logsumMXR);
						
						
					}
					
					
					
					//get log sum
					double logGamChiSum = LogSumExp.logSumOfExponentials(gTerms);
					MultiVarValIndex mgamchiindex = new MultiVarValIndex();
					mgamchiindex.addVal(abcRes.getSingleQueryVar());
					mgamchiindex.addVal(abgRes.getSingleQueryVar());
					MGamChi.put(mgamchiindex, logGamChiSum);
					
				}
				
				
			}
			
		}
		
		
	}
	
	
	
	protected void addJointDataTerm(double v, GMQuery query, Map <GMQuery, List<Double>> index){
		List <Double> internal = index.get(query);
		if(internal == null){
			internal = new ArrayList<Double>();
			index.put(query, internal);
		}
		
		internal.add(v);	
	}
	
	protected void addParentDataTerm(double v, RVariableValue query, Map <RVariableValue, List<Double>> index){
		List <Double> internal = index.get(query);
		if(internal == null){
			internal = new ArrayList<Double>();
			index.put(query, internal);
		}
		internal.add(v);
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
	
	
	
	
	
	
	
	
	
	
	
	
	class MultiVarValIndex{
		
		List <RVariableValue>		els;
		boolean 					needsToComputeHashCode;
		int 						hashCode;
		
		public MultiVarValIndex(){
			els = new ArrayList<RVariableValue>();
			needsToComputeHashCode = true;
		}
		
		public void addVal(RVariableValue val){
			els.add(val);
			needsToComputeHashCode = true;
		}
		
		@Override
		public boolean equals(Object o){
			if(this == o){
				return true;
			}
			if(!(o instanceof MultiVarValIndex)){
				return false;
			}
			
			MultiVarValIndex that = (MultiVarValIndex)o;
			
			for(RVariableValue v : els){
				boolean foundMatch = false;
				for(RVariableValue v2 : that.els){
					if(v.equals(v2)){
						foundMatch = true;
						break;
					}
				}
				if(!foundMatch){
					return false;
				}
			}
			
			return true;
			
			
		}
		
		@Override
		public int hashCode(){
			if(needsToComputeHashCode){
				this.computeHashCode();
			}
			return hashCode;
		}
		
		@Override
		public String toString(){
			List <String> sReps = new ArrayList<String>();
			
			for(RVariableValue v : els){
				sReps.add(v.toString());
			}
			
			Collections.sort(sReps);
			
			String joinDelim = "&&^^";
			StringBuffer buf = new StringBuffer();
			for(String s : sReps){
				buf.append(s).append(joinDelim);
			}
			
			return buf.toString();
		}
		
		protected void computeHashCode(){
			
			this.hashCode = this.toString().hashCode();
			
			
		}
		
		
	}

}
