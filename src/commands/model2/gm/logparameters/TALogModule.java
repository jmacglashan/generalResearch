package commands.model2.gm.logparameters;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import generativemodel.common.LPInfRejectQRIterator;
import generativemodel.common.MultiNomialRVPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import burlap.oomdp.core.Domain;

import commands.model2.gm.StateRVValue;
import commands.model2.gm.TAModule;

public class TALogModule extends TAModule {
	
	
	
	public TALogModule(String name, RVariable stateInput, Domain oomdpDomain, List <HollowTaskValue> hollowTasks, List <String> constraintPFClasses, List <AbstractConditionsValue> goalConditionValues){
		super(name, stateInput, oomdpDomain, hollowTasks, constraintPFClasses, goalConditionValues);

	}
	
	
	protected void initializeGoalParameters(){
		Iterator<RVariableValue> agoalIter = new AbstractGoalConditionIterator();
		while(agoalIter.hasNext()){
			AbstractConditionsValue agoalCond = (AbstractConditionsValue)agoalIter.next();
			
			//first count how many there are
			int n = 0;
			Iterator<RVariableValue> goalCondIter = new TaskDescriptionConditionedIterator(agoalCond, goalRV);
			while(goalCondIter.hasNext()){
				goalCondIter.next();
				n++;
			}
			
			//now set log probability for each
			double p = Math.log(1./(double)n);
			goalCondIter = new TaskDescriptionConditionedIterator(agoalCond, goalRV);
			while(goalCondIter.hasNext()){
				TaskDescriptionValue goalCond = (TaskDescriptionValue)goalCondIter.next();
				MultiNomialRVPI index = new MultiNomialRVPI(goalCond);
				index.addConditional(agoalCond);
				goalRV.setParam(index, p);
			}
			
			
		}
	}
	
	
	protected void initializeAbstractParameters(RVariable var, Iterator<RVariableValue> iter){
		
		//first do abstract constraints
		List <List <RVariableValue>> permissibleCValuesForHTs = new ArrayList<List<RVariableValue>>(hollowTasks.size());
		List <List <RVariableValue>> immissibleCValuesForHTs = new ArrayList<List<RVariableValue>>(hollowTasks.size());
		this.populatePermissibleAndImmisibleForAbstractParams(permissibleCValuesForHTs, immissibleCValuesForHTs, iter);
		
		for(int i = 0; i < hollowTasks.size(); i++){
			
			HollowTaskValue htv = hollowTasks.get(i);
			
			List <RVariableValue> pset = permissibleCValuesForHTs.get(i);
			double param = Math.log(1. / (double)pset.size());
			for(RVariableValue v : pset){
				MultiNomialRVPI pi = new MultiNomialRVPI(v);
				pi.addConditional(htv);
				
				var.setParam(pi, param);
			}
			
			
			
			List <RVariableValue> iset = immissibleCValuesForHTs.get(i);
			for(RVariableValue v : iset){
				MultiNomialRVPI pi = new MultiNomialRVPI(v);
				pi.addConditional(htv);
				
				var.setParam(pi, Double.NEGATIVE_INFINITY);
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
	public ModelTrackedVarIterator getNonInfiniteLogProbIterator(RVariable queryVar, List<RVariableValue> conditions) {
		
		if(queryVar.equals(hollowTaskRV)){
			Iterator<RVariableValue> iter = new HollowTaskVarIterator();
			return new LPInfRejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(abstractConstraintRV)){
			RVariableValue hollowVal = this.extractValueForVariableFromConditions(hollowTaskRV, conditions);
			//Iterator<RVariableValue> iter = new AbstractConstraintSinglePFVarIterator();
			Iterator<RVariableValue> iter = new AbstractConstraintSinglePFVarConditionedIterator((HollowTaskValue)hollowVal);
			return new LPInfRejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(abstractGoalRV)){
			Iterator<RVariableValue> iter = new AbstractGoalConditionIterator();
			return new LPInfRejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(constraintRV) ){
			RVariableValue aConstraintValue = this.extractValueForVariableFromConditions(abstractConstraintRV, conditions);
			Iterator<RVariableValue> iter = new TaskDescriptionConditionedIterator((AbstractConditionsValue)aConstraintValue, constraintRV);
			
			return new LPInfRejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(goalRV)){
			RVariableValue aGoalValue = this.extractValueForVariableFromConditions(abstractGoalRV, conditions);
			Iterator<RVariableValue> iter = new TaskDescriptionConditionedIterator((AbstractConditionsValue)aGoalValue, goalRV);
			
			return new LPInfRejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(rewardRV)){
			
			RVariableValue stateValue = this.extractValueForVariableFromConditions(stateRV, conditions);
			RVariableValue constraintValue = this.extractValueForVariableFromConditions(constraintRV, conditions);
			RVariableValue goalValue = this.extractValueForVariableFromConditions(goalRV, conditions);
			
			Iterator<RVariableValue> iter = new RFConditionedIterator((StateRVValue)stateValue, (TaskDescriptionValue)constraintValue, (TaskDescriptionValue)goalValue);
			
			return new LPInfRejectQRIterator(iter, conditions, this.owner);
			
		}
		
		
		return null;
	}
	
	
	public GMQueryResult computeLogProb(GMQuery query) {
		
		RVariableValue qvv = query.getSingleQueryVar();
		double p = 0.;
		
		if(qvv.isValueFor(hollowTaskRV)){
			p = this.computeHTLogProb(query);
		}
		else if(qvv.isValueFor(abstractConstraintRV) || qvv.isValueFor(abstractGoalRV)){
			p = this.computeABLogProb(query);
		}
		else if(qvv.isValueFor(constraintRV)){
			p = this.computeConstraintLogProb(query);
		}
		else if(qvv.isValueFor(goalRV)){
			p = this.computeGoalLogProb(query);
		}
		else if(qvv.isValueFor(rewardRV)){
			p = this.computeRewardLogProb(query);
		}
		
		
		
		GMQueryResult res = new GMQueryResult(query, p);
		
		return res;
	}
	
	
	
	
	protected double computeHTLogProb(GMQuery query){
		
		HollowTaskValue htv = (HollowTaskValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		StateRVValue srv = (StateRVValue)this.extractValueForVariableFromConditions(stateRV, conditions);
		
		if(!htSupportedInState(htv, srv.s)){
			return Double.NEGATIVE_INFINITY;
		}
		
		//otherwise check to see how many are supported in this state
		int n = 0;
		for(HollowTaskValue htvp : hollowTasks){
			if(this.htSupportedInState(htvp, srv.s)){
				n++;
			}
		}
		
		//return uniform over suported hollow tasks
		double p = Math.log(1./(double)n);
		
		return p;
	}
	
	protected double computeABLogProb(GMQuery query){
		
		AbstractConditionsValue abv = (AbstractConditionsValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		HollowTaskValue htv = (HollowTaskValue)this.extractValueForVariableFromConditions(hollowTaskRV, conditions);
		List <RVariableValue> localConds = new ArrayList<RVariableValue>();
		localConds.add(htv);
		
		MultiNomialRVPI index = new MultiNomialRVPI(abv, localConds);
		
		return abv.getOwner().getParameter(index);
		
	}
	
	
	
	protected double computeGoalLogProb(GMQuery query){
		
		TaskDescriptionValue tdv = (TaskDescriptionValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		AbstractConditionsValue abstractGoalValue = (AbstractConditionsValue)this.extractValueForVariableFromConditions(abstractGoalRV, conditions);
		List <RVariableValue> localConds = new ArrayList<RVariableValue>();
		localConds.add(abstractGoalValue);
		
		MultiNomialRVPI index = new MultiNomialRVPI(tdv, localConds);
		
		//double param = tdv.getOwner().getParameter(index, Double.NEGATIVE_INFINITY);
		double param = tdv.getOwner().getParameter(index);
		
		return param;
		
		
	}
	
	
	protected double computeConstraintLogProb(GMQuery query){
		
		TaskDescriptionValue tdv = (TaskDescriptionValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		AbstractConditionsValue abstractConstrainsValue = (AbstractConditionsValue)this.extractValueForVariableFromConditions(abstractConstraintRV, conditions);
		StateRVValue srv = (StateRVValue)this.extractValueForVariableFromConditions(stateRV, conditions);
		
		if(!this.constraintSupportedByAbstract(tdv, abstractConstrainsValue)){
			return Double.NEGATIVE_INFINITY;
		}
		
		if(!this.constraintSupportedInState(tdv, srv.s)){
			return Double.NEGATIVE_INFINITY;
		}
		

		Iterator<RVariableValue> tdciter = new TaskDescriptionConditionedIterator(abstractConstrainsValue, constraintRV);
		int np = 0;
		while(tdciter.hasNext()){
			TaskDescriptionValue possibleTdv = (TaskDescriptionValue)tdciter.next();
			if(this.constraintSupportedInState(possibleTdv, srv.s)){
				np++;
			}
		}
		
		
		double p = Math.log(1. / (double)np);
		
		
		return p;
	}
	
	
	
	protected double computeRewardLogProb(GMQuery query){
		
		RFConVariableValue rval = (RFConVariableValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues(); 
		
		RVariableValue stateValue = this.extractValueForVariableFromConditions(stateRV, conditions);
		RVariableValue constraintValue = this.extractValueForVariableFromConditions(constraintRV, conditions);
		RVariableValue goalValue = this.extractValueForVariableFromConditions(goalRV, conditions);
		
		Iterator<RVariableValue> iter = new RFConditionedIterator((StateRVValue)stateValue, (TaskDescriptionValue)constraintValue, (TaskDescriptionValue)goalValue);
		
		int n = 0;
		boolean found = false;
		while(iter.hasNext()){
			RVariableValue v = iter.next();
			if(rval.equals(v)){
				found = true;
			}
			n++;
		}
		
		double p = Math.log(1. / (double)n);
		
		if(!found){
			p = Double.NEGATIVE_INFINITY;
		}
		
		
		return p;
	}
	
	
	
	
}
