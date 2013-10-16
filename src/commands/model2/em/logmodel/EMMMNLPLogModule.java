package commands.model2.em.logmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import commands.model2.em.PDataManager;
import commands.model2.gm.CommandsModelConstructor;
import commands.model2.gm.IRLModule;
import commands.model2.gm.IRLModule.BehaviorValue;
import commands.model2.gm.MMNLPModule;
import commands.model2.gm.MMNLPModule.StringValue;
import commands.model2.gm.StateRVValue;
import commands.model2.gm.TAModule;
import commands.model2.gm.TAModule.TaskDescriptionValue;
import commands.model2.gm.logparameters.MMNLPLogModule;

import em.EMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.LogSumExp;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import generativemodel.common.MultiNomialRVPI;

public class EMMMNLPLogModule extends EMModule {

	
	protected RVariable												hollowRV;
	protected RVariable												aConstraintRV;
	protected RVariable												aGoalRV;
	protected RVariable												goalRV;
	protected RVariable												constraintRV;
	protected RVariable												pfRV;
	protected RVariable												wordRV;
	protected RVariable												commandRV;
	
	protected LogPDataManager										lpdManager;
	protected MMNLPLogModule										gmNLPMod;
	
	
	protected Map<GMQuery, List <Double>>							jointCounts;
	protected Map<RVariableValue, List<Double>>						pfCounts;
	
	
	protected boolean												updateConstantParams = false;
	
	
	public EMMMNLPLogModule(LogPDataManager lpdManager) {
		this.lpdManager = lpdManager;
	}
	
	
	@Override
	public void setGenerativeModelSrc(GenerativeModel gm){
		super.setGenerativeModelSrc(gm);
		
		
		this.hollowRV = gm.getRVarWithName(TAModule.HTNAME);
		this.aGoalRV = gm.getRVarWithName(TAModule.AGNAME);
		this.aConstraintRV = gm.getRVarWithName(TAModule.ACNAME);
		this.goalRV = gm.getRVarWithName(TAModule.GNAME);
		this.constraintRV = gm.getRVarWithName(TAModule.CNAME);
		this.pfRV = gm.getRVarWithName(MMNLPModule.PNAME);
		this.wordRV = gm.getRVarWithName(MMNLPModule.WNAME);
		this.commandRV = gm.getRVarWithName(MMNLPModule.CNAME);
		
		this.gmNLPMod = (MMNLPLogModule)this.gm.getModuleWithName(CommandsModelConstructor.NLPMODNAME);
		
		this.initializeCountDatastructures();
		
	}
	
	
	protected void initializeCountDatastructures(){
		
		jointCounts = new HashMap<GMQuery, List <Double>>();
		pfCounts = new HashMap<RVariableValue, List <Double>>();
		
	}
	
	
	@Override
	public void runEStep(int dataInstanceId, List<RVariableValue> observables) {
		
		double lpData = this.lpdManager.getLogProbForData(dataInstanceId, observables);
		
		BehaviorValue behavior = (BehaviorValue)this.getBehaviorValue(observables);
		StringValue command = (StringValue)this.getCommandValue(observables);
		StateRVValue srvv = new StateRVValue(behavior.t.getState(0), gm.getRVarWithName(CommandsModelConstructor.STATERVNAME));
		
		Map<StringValue, Integer> commandWordCount = this.gmNLPMod.getWordCounts(command.s);
		
		List <RVariableValue> sconds = new ArrayList<RVariableValue>();
		sconds.add(srvv);
		
		Iterator<RVariableValue> propIter = this.gm.getRVariableValuesFor(this.gm.getRVarWithName(MMNLPLogModule.PNAME));
		while(propIter.hasNext()){
			RVariableValue propVal = propIter.next();
			
			List <Double> propTerms = new ArrayList<Double>();
			
			for(Map.Entry<StringValue, Integer> wordEntries : commandWordCount.entrySet()){
				
				StringValue word = wordEntries.getKey();
				int wc = wordEntries.getValue();
				double propTerm = Math.log(wc);
				
				List <Double> hTerms = new ArrayList<Double>();
				//walk down the list of stuff
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
									
									GMQuery propQuery = new GMQuery();
									propQuery.addQuery(propVal);
									propQuery.addCondition(cRes.getSingleQueryVar());
									propQuery.addCondition(gRes.getSingleQueryVar());
									GMQueryResult propLPRes = this.gm.getLogProb(propQuery, true);
									gExpTerm += propLPRes.probability;
									
									GMQuery wordPropQuery = new GMQuery();
									wordPropQuery.addQuery(word);
									wordPropQuery.addCondition(propVal);
									GMQueryResult wordPropLPRes = this.gm.getLogProb(wordPropQuery, true);
									gExpTerm += wordPropLPRes.probability;
									
									gExpTerm -= this.getLogProbWordGivenTask(word, cRes.getSingleQueryVar(), gRes.getSingleQueryVar());
									
									
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
										
									}
									
									
									gExpTerm += LogSumExp.logSumOfExponentials(rExpTerms);
									gExpTerms.add(gExpTerm);
									
								}
								
								
								
								cExpTerm += LogSumExp.logSumOfExponentials(gExpTerms);
								cExpTerms.add(cExpTerm);
								
							}
							
							
							agExpTerm += LogSumExp.logSumOfExponentials(cExpTerms);
							agExpTerms.add(agExpTerm);
							
							
						}
						
						
						acExpTerm += LogSumExp.logSumOfExponentials(agExpTerms);
						acExpTerms.add(acExpTerm);
						
						
					}
					
					
					hExpTerm += LogSumExp.logSumOfExponentials(acExpTerms);
					hTerms.add(hExpTerm);
					
				} //end h iterate
				
				
				
				
				
				propTerm += LogSumExp.logSumOfExponentials(hTerms);
				propTerms.add(propTerm);
				double jointTerm = propTerm - lpData;
				GMQuery jointCountIndex = new GMQuery();
				jointCountIndex.addQuery(word);
				jointCountIndex.addQuery(propVal);
				this.addJointDataTerm(jointTerm, jointCountIndex, jointCounts);
				
			}
			
			
			//combine and subtract data term for parent value
			double parentVal = LogSumExp.logSumOfExponentials(propTerms) - lpData;
			this.addParentDataTerm(parentVal, propVal, this.pfCounts);
			
		}
		
		
		

	}

	@Override
	public void runMStep() {
		
		Iterator<RVariableValue> pfIter = this.gm.getRVariableValuesFor(pfRV);
		while(pfIter.hasNext()){
			RVariableValue pfv = pfIter.next();
			
			if(!this.updateConstantParams && ((StringValue)pfv).s.equals(this.gmNLPMod.constantFeatureName())){
				continue;
			}
			
			
			List <Double> pdTerms = this.pfCounts.get(pfv);
			double pdcount = LogSumExp.logSumOfExponentials(pdTerms);
			
			Iterator<RVariableValue> wordIter = this.gm.getRVariableValuesFor(wordRV);
			while(wordIter.hasNext()){
				RVariableValue wv = wordIter.next();
				
				GMQuery jointQuery = new GMQuery();
				jointQuery.addQuery(wv);
				jointQuery.addQuery(pfv);
				
				List <Double> jdTerms = this.jointCounts.get(jointQuery);
				
				double nwparam = Double.NEGATIVE_INFINITY;
				if(jdTerms != null){
					double jdcount = LogSumExp.logSumOfExponentials(jdTerms);
					nwparam = jdcount - pdcount;
				}
				
				MultiNomialRVPI paramIndex = new MultiNomialRVPI(wv);
				paramIndex.addConditional(pfv);
				wordRV.setParam(paramIndex, nwparam);
				
			}
			
		}
		
		this.initializeCountDatastructures(); //setup data structures for next E step
		

	}
	
	
	
	protected double getLogProbWordGivenTask(RVariableValue wordRVar, RVariableValue constaintsRVar, RVariableValue goalRVar){
		
		Set <String> pfNames = this.gmNLPMod.getUniquePFNames((TaskDescriptionValue)constaintsRVar, (TaskDescriptionValue)goalRVar);
		return this.gmNLPMod.logProbWGivenProps(pfNames, (StringValue)wordRVar);
		
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
	
	

}
