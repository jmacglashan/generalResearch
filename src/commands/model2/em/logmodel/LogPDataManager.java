package commands.model2.em.logmodel;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.LogSumExp;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import commands.model2.em.PDataManager;
import commands.model2.gm.CommandsModelConstructor;
import commands.model2.gm.StateRVValue;
import commands.model2.gm.TAModule;
import commands.model2.gm.IRLModule.BehaviorValue;
import commands.model2.gm.MMNLPModule.StringValue;

public class LogPDataManager extends PDataManager{
	
	protected Map <Integer, Double>		trainingDataLogProbs;
	
	public LogPDataManager(GenerativeModel gm){
		super(gm);
		this.trainingDataLogProbs = new HashMap<Integer, Double>();
	}
	
	public double getLogProbForData(int dataInstanceId, List<RVariableValue> observables){
		
		Double cached = trainingDataLogProbs.get(dataInstanceId);
		if(cached != null){
			return cached;
		}
		
		
		//if not already cached then we need to compute it
		StringValue command = (StringValue)this.getCommandValue(observables);
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
		
		
		
		List <Double> hExpTerms = new ArrayList<Double>();
		Iterator<GMQueryResult> htIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.HTNAME), sconds, true);
		while(htIterRes.hasNext()){
			GMQueryResult hres = htIterRes.next();
			double hExpTerm = hres.probability;
			
			
			RVariableValue htv = hres.getSingleQueryVar();
			List <RVariableValue> hsConds = new ArrayList<RVariableValue>();
			hsConds.add(srvv);
			hsConds.add(htv);
			
			List <Double> acExpTerms = new ArrayList<Double>();
			Iterator<GMQueryResult> abConIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.ACNAME), hsConds, true);
			while(abConIterRes.hasNext()){
				GMQueryResult abcRes = abConIterRes.next();
				double acExpTerm = abcRes.probability;
				
				List <Double> agExpTerms = new ArrayList<Double>();
				Iterator<GMQueryResult> abGIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.AGNAME), hsConds, true);
				while(abGIterRes.hasNext()){
					GMQueryResult abgRes = abGIterRes.next();
					double agExpTerm = abgRes.probability;
					
					
					List <RVariableValue> abConds = new ArrayList<RVariableValue>();
					abConds.add(abcRes.getSingleQueryVar());
					abConds.add(abgRes.getSingleQueryVar());
					abConds.add(srvv);
					
					List <Double> cExpTerms = new ArrayList<Double>();
					Iterator<GMQueryResult> cIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.CNAME), abConds, true);
					while(cIterRes.hasNext()){
						GMQueryResult cRes = cIterRes.next();
						double cExpTerm = cRes.probability;
						
						
						List <Double> gExpTerms = new ArrayList<Double>();
						Iterator<GMQueryResult> gIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.GNAME), abConds, true);
						while(gIterRes.hasNext()){
							GMQueryResult gRes = gIterRes.next();
							double gExpTerm = gRes.probability;
							
							List <RVariableValue> taskConds = new ArrayList<RVariableValue>();
							taskConds.add(cRes.getSingleQueryVar());
							taskConds.add(gRes.getSingleQueryVar());
							taskConds.add(srvv);
							
							GMQuery commandQuery = new GMQuery();
							commandQuery.addQuery(command);
							commandQuery.setConditions(taskConds);
							
							//add command log prob
							GMQueryResult commandRes = this.gm.getLogProb(commandQuery, true);
							gExpTerm += commandRes.probability;
							
							List <Double> rExpTerms = new ArrayList<Double>();
							Iterator<GMQueryResult> rIterRes = gm.getNonInfiniteLogProbIterator(gm.getRVarWithName(TAModule.RNAME), taskConds, true);
							while(rIterRes.hasNext()){
								GMQueryResult rRes = rIterRes.next();
								double rExpTerm = rRes.probability;
								
								GMQuery bQuery = new GMQuery();
								bQuery.addQuery(behavior);
								bQuery.addCondition(rRes.getSingleQueryVar());
								bQuery.addCondition(srvv);
								
								GMQueryResult bRes = this.gm.getLogProb(bQuery, true);
								rExpTerm += bRes.probability;
								
								
								rExpTerms.add(rExpTerm);
								//System.out.println("rTerm: " + rExpTerm);
								
							}
							
							
							gExpTerm += LogSumExp.logSumOfExponentials(rExpTerms);
							gExpTerms.add(gExpTerm);
							//System.out.println("gTerm: " + gExpTerm);
							
						}
						
						
						
						cExpTerm += LogSumExp.logSumOfExponentials(gExpTerms);
						cExpTerms.add(cExpTerm);
						//System.out.println("cTerm: " + cExpTerm);
						
					}
					
					
					agExpTerm += LogSumExp.logSumOfExponentials(cExpTerms);
					agExpTerms.add(agExpTerm);
					//System.out.println("agTerm: " + agExpTerm);
					
				}
				
				
				acExpTerm += LogSumExp.logSumOfExponentials(agExpTerms);
				acExpTerms.add(acExpTerm);
				//System.out.println("acTerm: " + acExpTerm);
				
			}
			
			
			hExpTerm += LogSumExp.logSumOfExponentials(acExpTerms);
			hExpTerms.add(hExpTerm);
			//System.out.println("hTerm: " + hExpTerm);
			
		}
		
		
		return LogSumExp.logSumOfExponentials(hExpTerms);
	}
	
}
