package commands.model2.gm.logparameters;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.LogSumExp;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import generativemodel.common.LPInfRejectQRIterator;
import generativemodel.common.MultiNomialRVPI;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.core.GroundedProp;

import commands.model2.gm.MMNLPModule;
import commands.model2.gm.TAModule.TaskDescriptionValue;

public class MMNLPLogModule extends MMNLPModule {

	public MMNLPLogModule(String name, RVariable commandLenRV,
			RVariable constraintRV, RVariable goalRV,
			List<String> propFunctionNames, List<String> trainingCommands) {
		super(name, commandLenRV, constraintRV, goalRV, propFunctionNames,
				trainingCommands);
		
	}
	
	
	@Override
	protected void initializeWordParameters(){
		
		double uniVocab = Math.log(1./(double)vocab.size());
		
		StringVarIter pfIter = new StringVarIter(this.propFunctionNames, this.propRV);
		while(pfIter.hasNext()){
			RVariableValue pfVal = pfIter.next();
			
			StringVarIter wIter = new StringVarIter(this.vocab, wordRV);
			while(wIter.hasNext()){
				RVariableValue wVal = wIter.next();
				
				MultiNomialRVPI index = new MultiNomialRVPI(wVal);
				index.addConditional(pfVal);
				
				wordRV.setParam(index, uniVocab);
				
			}
		}
		
	}
	
	@Override
	public GMQueryResult getProb(GMQuery query){
		
		GMQueryResult lRes = this.getLogProb(query);
		lRes.probability = Math.exp(lRes.probability);
		return lRes;
		
	}
	
	@Override
	public GMQueryResult getLogProb(GMQuery query){
		
		GMQueryResult cachedResult = owner.getCachedLoggedResultForQuery(query);
		if(cachedResult != null){
			return cachedResult;
		}
		
		return this.computeLogProb(query);
		
	}
	
	
	@Override
	public ModelTrackedVarIterator getNonInfiniteLogProbIterator(RVariable queryVar, List <RVariableValue> conditions){
		
		if(queryVar.equals(propRV)){
			RVariableValue constraints = this.extractValueForVariableFromConditions(constraintRV, conditions);
			RVariableValue goals = this.extractValueForVariableFromConditions(goalRV, conditions);
			Iterator<RVariableValue> condIter = new ConditionedPFVarIter(propRV, (TaskDescriptionValue)constraints, (TaskDescriptionValue)goals);
			
			return new LPInfRejectQRIterator(condIter, conditions, this.owner);
		}
		
		if(queryVar.equals(wordRV)){
			Iterator<RVariableValue> wIter = new StringVarIter(vocab, wordRV);
			return new LPInfRejectQRIterator(wIter, conditions, this.owner);
		}
		
		
		return null;
		
	}
	
	
	
	
	public GMQueryResult computeLogProb(GMQuery query) {
		
		double p = 0.;
		RVariableValue qv = query.getSingleQueryVar();
		
		if(qv.isValueFor(propRV)){
			p = this.computeLogProbForProp(query);
		}
		if(qv.isValueFor(wordRV)){
			p = this.computeMNParamProb(qv, query.getConditionValues());
		}
		if(qv.isValueFor(commandRV)){
			p = this.computeLogProbForCommand(query);
		}
		
		return new GMQueryResult(query, p);
	}
	
	
	public double computeLogProbForProp(GMQuery query){
		
		StringValue propVal = (StringValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		TaskDescriptionValue constraints = (TaskDescriptionValue)this.extractValueForVariableFromConditions(constraintRV, conditions);
		TaskDescriptionValue goals = (TaskDescriptionValue)this.extractValueForVariableFromConditions(goalRV, conditions);
		
		Set<String> pfNames = new HashSet<String>();
		boolean foundMatch = false;
		for(GroundedProp gp : constraints.props){
			String pfName = gp.pf.getName();
			pfNames.add(pfName);
			if(!foundMatch && pfName.equals(propVal.s)){
				foundMatch = true;
			}
		}
		for(GroundedProp gp : goals.props){
			String pfName = gp.pf.getName();
			pfNames.add(pfName);
			if(!foundMatch && pfName.equals(propVal.s)){
				foundMatch = true;
			}
		}
		
		if(!foundMatch && !propVal.s.equals(this.constantFeatureName)){
			return Double.NEGATIVE_INFINITY;
		}
		
		double n = pfNames.size();
		if(useConstantFeature){
			n += 1.;
		}
		
		double p = Math.log(1./n);
		
		return p;
	}
	
	
	public double computeLogProbForCommand(GMQuery query){
		
		
		StringValue commandValue = (StringValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		TaskDescriptionValue constraints = (TaskDescriptionValue)this.extractValueForVariableFromConditions(constraintRV, conditions);
		TaskDescriptionValue goals = (TaskDescriptionValue)this.extractValueForVariableFromConditions(goalRV, conditions);
		
		Map<StringValue, Integer> counts = getWordCounts(commandValue.s);
		Set <String> pfNames = this.getUniquePFNames(constraints, goals);
		
		double lp = 0.;
		for(StringValue wVal : counts.keySet()){
			lp += counts.get(wVal) * this.logProbWGivenProps(pfNames, wVal);
		}
		
		
		return lp;
	}
	
	
	
	public double logProbWGivenProps(Set <String> pfNames, StringValue wordValue){
		
		double [] termsForExponential = new double[pfNames.size()];
		int i = 0;
		double propLogP = Math.log(1./pfNames.size());
		for(String pfName : pfNames){
			StringValue pfVal = new StringValue(pfName, propRV);
			MultiNomialRVPI index = new MultiNomialRVPI(wordValue);
			index.addConditional(pfVal);
			double param = wordRV.getParameter(index); //param is logged param
			
			termsForExponential[i] = propLogP + param;
			
			i++;
		}
		
		return LogSumExp.logSumOfExponentials(termsForExponential);
		
	}
	
	
	

}
