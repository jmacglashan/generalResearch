package commands.model2.gm;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import generativemodel.common.MNPEMModule;
import generativemodel.common.MultiNomialRVPI;
import generativemodel.common.P0RejectQRIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import commands.PFClassTemplate;
import commands.model2.gm.TAModule.HollowTaskValue.ClassVarPair;

public class TAModule extends MNPEMModule {

	
	public final static String					HTNAME = "hollowTask";
	public final static String					ACNAME = "abstractConstraint";
	public final static String					AGNAME = "abstractGoal";
	public final static String					CNAME = "constraint";
	public final static String					GNAME = "goal";
	public final static String					RNAME = "rewardFunciton";
	
	
	protected RVariable 						hollowTaskRV;
	protected RVariable 						abstractConstraintRV;
	protected RVariable 						abstractGoalRV;
	protected RVariable 						constraintRV;
	protected RVariable 						goalRV;
	protected RVariable 						rewardRV;
	
	protected RVariable							stateRV;
	
	
	protected List<HollowTaskValue>				hollowTasks;
	protected List<String>						constraintPFClasses;
	protected List<AbstractConditionsValue>		goalConditionValues;

	protected Domain							oomdpDomain;
	
	
	
	
	
	
	public TAModule(String name, RVariable stateInput, Domain oomdpDomain, List <HollowTaskValue> hollowTasks, List <String> constraintPFClasses, List <AbstractConditionsValue> goalConditionValues) {
		super(name);
		this.oomdpDomain = oomdpDomain;
		this.externalDependencyList.add(stateInput);
		
		this.hollowTasks = hollowTasks;
		this.constraintPFClasses = constraintPFClasses;
		this.goalConditionValues = goalConditionValues;
		
		
		stateRV = stateInput;
		
		//note that the constructor of random variables will automatically add them to this module's index
		hollowTaskRV = new RVariable(HTNAME, this);
		hollowTaskRV.addDependency(stateInput);
		
		abstractConstraintRV = new RVariable(ACNAME, this);
		abstractConstraintRV.addDependency(hollowTaskRV);
		
		abstractGoalRV = new RVariable(AGNAME, this);
		abstractGoalRV.addDependency(hollowTaskRV);
		
		constraintRV = new RVariable(CNAME, this);
		constraintRV.addDependency(abstractConstraintRV);
		constraintRV.addDependency(stateInput);
		
		goalRV = new RVariable(GNAME, this);
		goalRV.addDependency(abstractGoalRV);
		
		rewardRV = new RVariable(RNAME, this);
		rewardRV.addDependency(goalRV);
		rewardRV.addDependency(constraintRV);
		rewardRV.addDependency(stateInput);
		
		
		
		for(HollowTaskValue htv : hollowTasks){
			htv.setOwner(hollowTaskRV);
		}
		
		for(AbstractConditionsValue abv : goalConditionValues){
			abv.setOwner(abstractGoalRV);
		}
		
		
		this.initializeAbstractParameters();
		this.initializeGoalParameters();
		
		
	}
	
	
	protected void initializeAbstractParameters(){
		
		
		Iterator<RVariableValue> constVIter = new AbstractConstraintSinglePFVarIterator();
		this.initializeAbstractParameters(abstractConstraintRV, constVIter);
		
		Iterator<RVariableValue> goalCIter = new AbstractGoalConditionIterator();
		this.initializeAbstractParameters(abstractGoalRV, goalCIter);
		
		
		
		
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
			
			//now set probability for each
			double p = 1./(double)n;
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
			double param = 1. / (double)pset.size();
			for(RVariableValue v : pset){
				MultiNomialRVPI pi = new MultiNomialRVPI(v);
				pi.addConditional(htv);
				
				var.setParam(pi, param);
			}
			
			
			
			List <RVariableValue> iset = immissibleCValuesForHTs.get(i);
			for(RVariableValue v : iset){
				MultiNomialRVPI pi = new MultiNomialRVPI(v);
				pi.addConditional(htv);
				
				var.setParam(pi, 0.);
			}
			
		}
		
		
	}
	
	
	protected void populatePermissibleAndImmisibleForAbstractParams(List <List <RVariableValue>> permissibleCValuesForHTs, List <List <RVariableValue>> immissibleCValuesForHTs, Iterator<RVariableValue> iter){
		for(int i = 0; i < hollowTasks.size(); i++){
			List <RVariableValue> set = new ArrayList<RVariableValue>();
			permissibleCValuesForHTs.add(set);
			
			List <RVariableValue> set2 = new ArrayList<RVariableValue>();
			immissibleCValuesForHTs.add(set2);
		}
		
		
		while(iter.hasNext()){
			AbstractConditionsValue value = (AbstractConditionsValue)iter.next();
			List <ClassVarPair> allcvps = value.getAllClassVarPairs();
			for(int i = 0; i < hollowTasks.size(); i++){
				boolean isMatch = true;
				HollowTaskValue htv = hollowTasks.get(i);
				for(ClassVarPair cvp : allcvps){
					if(!htv.vars.contains(cvp)){
						isMatch = false;
						break;
					}
				}
				if(isMatch){
					permissibleCValuesForHTs.get(i).add(value);
				}
				else{
					immissibleCValuesForHTs.get(i).add(value);
				}
			}
		}
	}
	
	
	

	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		RVariableValue qvv = query.getSingleQueryVar();
		double p = 0.;
		
		if(qvv.isValueFor(hollowTaskRV)){
			p = this.computeHTProb(query);
		}
		else if(qvv.isValueFor(abstractConstraintRV) || qvv.isValueFor(abstractGoalRV)){
			p = this.computeABProb(query);
		}
		else if(qvv.isValueFor(constraintRV)){
			p = this.computeConstraintProb(query);
		}
		else if(qvv.isValueFor(goalRV)){
			p = this.computeGoalProb(query);
		}
		else if(qvv.isValueFor(rewardRV)){
			p = this.computeRewardProb(query);
		}
		
		
		
		GMQueryResult res = new GMQueryResult(query, p);
		
		return res;
	}

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar, List<RVariableValue> conditions) {
		
		if(queryVar.equals(hollowTaskRV)){
			Iterator<RVariableValue> iter = new HollowTaskVarIterator();
			return new P0RejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(abstractConstraintRV)){
			RVariableValue hollowVal = this.extractValueForVariableFromConditions(hollowTaskRV, conditions);
			//Iterator<RVariableValue> iter = new AbstractConstraintSinglePFVarIterator();
			Iterator<RVariableValue> iter = new AbstractConstraintSinglePFVarConditionedIterator((HollowTaskValue)hollowVal);
			return new P0RejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(abstractGoalRV)){
			Iterator<RVariableValue> iter = new AbstractGoalConditionIterator();
			return new P0RejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(constraintRV) ){
			RVariableValue aConstraintValue = this.extractValueForVariableFromConditions(abstractConstraintRV, conditions);
			Iterator<RVariableValue> iter = new TaskDescriptionConditionedIterator((AbstractConditionsValue)aConstraintValue, constraintRV);
			
			return new P0RejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(goalRV)){
			RVariableValue aGoalValue = this.extractValueForVariableFromConditions(abstractGoalRV, conditions);
			Iterator<RVariableValue> iter = new TaskDescriptionConditionedIterator((AbstractConditionsValue)aGoalValue, goalRV);
			
			return new P0RejectQRIterator(iter, conditions, this.owner);
		}
		
		if(queryVar.equals(rewardRV)){
			
			RVariableValue stateValue = this.extractValueForVariableFromConditions(stateRV, conditions);
			RVariableValue constraintValue = this.extractValueForVariableFromConditions(constraintRV, conditions);
			RVariableValue goalValue = this.extractValueForVariableFromConditions(goalRV, conditions);
			
			Iterator<RVariableValue> iter = new RFConditionedIterator((StateRVValue)stateValue, (TaskDescriptionValue)constraintValue, (TaskDescriptionValue)goalValue);
			
			return new P0RejectQRIterator(iter, conditions, this.owner);
			
		}
		
		
		return null;
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		
		
		if(queryVar.equals(hollowTaskRV)){
			return new HollowTaskVarIterator();
		}
		
		if(queryVar.equals(abstractConstraintRV)){
			return new AbstractConstraintSinglePFVarIterator();
		}
		
		if(queryVar.equals(abstractGoalRV)){
			return new AbstractGoalConditionIterator();
		}
		
		if(queryVar.equals(constraintRV) || queryVar.equals(goalRV)){
			return new TaskDescriptionIterator(queryVar);
		}
		
		
		return null;
	}
	
	
	
	public static String get0PaddedBinary(int i, int nBits){
		
		String minB = Integer.toBinaryString(i);
		String result = minB;
		
		int n0s = nBits - minB.length();
		
		if(n0s > 0){
			
			StringBuffer buf = new StringBuffer(n0s);
			for(int j = 0; j < n0s; j++){
				buf.append("0");
			}
			
			result = buf + result;
			
		}
		
		
		return result;
	}
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////////
	// Probability Compution
	////////////////////////////////////////////////////////////////////////////////
	
	
	
	protected double computeHTProb(GMQuery query){
		
		HollowTaskValue htv = (HollowTaskValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		StateRVValue srv = (StateRVValue)this.extractValueForVariableFromConditions(stateRV, conditions);
		
		if(!htSupportedInState(htv, srv.s)){
			return 0.;
		}
		
		//otherwise check to see how many are supported in this state
		int n = 0;
		for(HollowTaskValue htvp : hollowTasks){
			if(this.htSupportedInState(htvp, srv.s)){
				n++;
			}
		}
		
		//return uniform over suported hollow tasks
		double p = 1./(double)n;
		
		return p;
	}
	
	protected double computeABProb(GMQuery query){
		
		AbstractConditionsValue abv = (AbstractConditionsValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		HollowTaskValue htv = (HollowTaskValue)this.extractValueForVariableFromConditions(hollowTaskRV, conditions);
		List <RVariableValue> localConds = new ArrayList<RVariableValue>();
		localConds.add(htv);
		
		MultiNomialRVPI index = new MultiNomialRVPI(abv, localConds);
		
		return abv.getOwner().getParameter(index);
		
	}
	
	
	
	protected double computeGoalProb(GMQuery query){
		
		TaskDescriptionValue tdv = (TaskDescriptionValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		AbstractConditionsValue abstractGoalValue = (AbstractConditionsValue)this.extractValueForVariableFromConditions(abstractGoalRV, conditions);
		List <RVariableValue> localConds = new ArrayList<RVariableValue>();
		localConds.add(abstractGoalValue);
		
		MultiNomialRVPI index = new MultiNomialRVPI(tdv, localConds);
		
		return tdv.getOwner().getParameter(index, 0.);
		
		
	}
	
	
	protected double computeConstraintProb(GMQuery query){
		
		TaskDescriptionValue tdv = (TaskDescriptionValue)query.getSingleQueryVar();
		Set<RVariableValue> conditions = query.getConditionValues();
		
		AbstractConditionsValue abstractConstrainsValue = (AbstractConditionsValue)this.extractValueForVariableFromConditions(abstractConstraintRV, conditions);
		StateRVValue srv = (StateRVValue)this.extractValueForVariableFromConditions(stateRV, conditions);
		
		if(!this.constraintSupportedByAbstract(tdv, abstractConstrainsValue)){
			return 0.;
		}
		
		if(!this.constraintSupportedInState(tdv, srv.s)){
			return 0.;
		}
		

		Iterator<RVariableValue> tdciter = new TaskDescriptionConditionedIterator(abstractConstrainsValue, constraintRV);
		int np = 0;
		while(tdciter.hasNext()){
			TaskDescriptionValue possibleTdv = (TaskDescriptionValue)tdciter.next();
			if(this.constraintSupportedInState(possibleTdv, srv.s)){
				np++;
			}
		}
		
		
		double p = 1. / (double)np;
		
		
		return p;
	}
	
	
	
	protected double computeRewardProb(GMQuery query){
		
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
		
		double p = 1. / (double)n;
		
		if(!found){
			p = 0.;
		}
		
		
		return p;
	}
	
	
	
	protected boolean htSupportedInState(HollowTaskValue htv, State s){
		for(ClassVarPair cvp : htv.vars){
			String className = cvp.varClass;
			if(s.getObjectsOfClass(className).size() == 0){
				return false;
			}
		}
		return true;
	}
	
	
	
	
	protected boolean constraintSupportedByAbstract(TaskDescriptionValue tdv, AbstractConditionsValue acv){
		
		if(tdv.props.size() != acv.conditions.size()){
			return false;
		}
		
		for(int i = 0; i < tdv.props.size(); i++){
			GroundedProp gp = tdv.props.get(i);
			AbstractCondition ac = acv.conditions.get(i);
			if(!gp.pf.getClassName().equals(ac.pfclass.className)){
				return false;
			}
			for(int j = 0; j < gp.params.length; j++){
				if(!gp.params[j].equals(ac.params[j])){
					return false;
				}
			}
		}
		
		return true;
	}
	
	
	/**
	 * This method will determine is single variable constraints has possible object bindings in the given state
	 * that are true. This method will not work if constraint popositonal functions operate on multiple variables
	 * @param tv
	 * @param s
	 * @return
	 */
	public boolean constraintSupportedInState(TaskDescriptionValue tv, State s){
		
		Map<ClassVarPair, List<GroundedProp>> classMap = new HashMap<TAModule.HollowTaskValue.ClassVarPair, List<GroundedProp>>();
		for(GroundedProp gp : tv.props){
			String [] paramClasses = gp.pf.getParameterClasses();
			for(int i = 0; i < gp.params.length; i++){
				ClassVarPair cvp = new ClassVarPair(paramClasses[i], gp.params[i]);
				List <GroundedProp> propsForThisCVP = classMap.get(cvp);
				if(propsForThisCVP == null){
					propsForThisCVP = new ArrayList<GroundedProp>();
					classMap.put(cvp, propsForThisCVP);
				}
				propsForThisCVP.add(gp);
			}
		}
		
		for(ClassVarPair cvp : classMap.keySet()){
			List <GroundedProp> propsForCVP = classMap.get(cvp);
			List <ObjectInstance> possibleObjectsBindings = s.getObjectsOfClass(cvp.varClass);
			boolean foundSatisfying = false;
			for(ObjectInstance o : possibleObjectsBindings){
				if(this.bindingSatisfies(o.getName(), propsForCVP, s)){
					foundSatisfying = true;
					break;
				}
			}
			if(!foundSatisfying){
				return false;
			}
			
		}
		
		return true;
	}
	
	protected boolean bindingSatisfies(String obName, List <GroundedProp> singleVarConstraints, State s){
		
		for(GroundedProp gp : singleVarConstraints){
			String [] boundParam = new String[]{obName};
			GroundedProp bgp = new GroundedProp(gp.pf, boundParam);
			if(!bgp.isTrue(s)){
				return false;
			}
		}
		
		return true;
	}
	
	
	
	
	
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////////
	// RVariableValue Wrappers
	////////////////////////////////////////////////////////////////////////////////
	
	
	public static class HollowTaskValue extends RVariableValue{

		public List <ClassVarPair> vars;
		
		public HollowTaskValue(){
			this.vars = new ArrayList<TAModule.HollowTaskValue.ClassVarPair>();
		}
		
		/**
		 * Adds an existentially quantified variable.
		 * @param objectClass The OO-MDP class of the variable
		 * @param varName The name of the existentially quantified variable
		 * @return False if the name is already being used (will not be added); true on success.
		 */
		public boolean addVaraible(String objectClass, String varName){
			ClassVarPair cvp = new ClassVarPair(objectClass, varName);
			if(this.vars.contains(cvp)){
				return false; //don't add the same variable name twice
			}
			this.vars.add(cvp);
			
			Collections.sort(this.vars);
			
			return true;
		}
		
		public String getClass(int i){
			return vars.get(i).varClass;
		}
		
		public String getVarName(int i){
			return vars.get(i).varName;
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(!(other instanceof HollowTaskValue)){
				return false;
			}
			
			HollowTaskValue htvo = (HollowTaskValue) other;
			
			
			//both are ordered so we should be able to comapre at the same index
			for(int i = 0; i < this.vars.size(); i++){
				if(!this.vars.get(i).equals(htvo.vars.get(i))){
					return false;
				}
			}
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			
			for(ClassVarPair cvp : this.vars){
				buf.append(cvp.varClass).append(" ").append(cvp.varName).append(" ");
			}
			
			return buf.toString();
		}
		
		
		
		
		static class ClassVarPair implements Comparable<ClassVarPair>{
			
			public String varClass;
			public String varName;
			
			public ClassVarPair(String c, String n){
				this.varClass = c;
				this.varName = n;
			}
			
			@Override
			public boolean equals(Object other){
				if(this == other){
					return true;
				}
				
				if(!(other instanceof ClassVarPair)){
					return false;
				}
				
				ClassVarPair o = (ClassVarPair)other;
				
				return this.varClass.equals(o.varClass) && this.varName.equals(o.varName);
				
			}

			@Override
			public int hashCode(){
				return (varClass + " " + varName).hashCode();
			}
			
			@Override
			public int compareTo(ClassVarPair o) {
				return this.varName.compareTo(o.varName);
			}
			
		}
		
		
		
	}
	
	
	
	
	
	
	
	public static class AbstractCondition{
		
		public PFClassTemplate	pfclass;
		public String []		params;
		
		public AbstractCondition(PFClassTemplate pfclass, String [] params){
			this.pfclass = pfclass;
			this.params = params;
		}
		
		
		@Override
		public boolean equals(Object other){
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof AbstractCondition)){
				return false;
			}
			
			AbstractCondition that = (AbstractCondition)other;
			
			for(int i = 0; i < params.length; i++){
				if(this.params[i] != that.params[i]){
					return false;
				}
			}
			
			
			return true;
			
		}
		
		@Override
		public String toString(){
			
			StringBuffer buf = new StringBuffer();
			buf.append(pfclass.className).append("(");
			for(int i = 0; i < this.params.length; i++){
				if(i > 0){
					buf.append(" ");
				}
				buf.append(this.params[i]);
			}
			buf.append(")");
			
			
			return buf.toString();
		}
		
		public List<ClassVarPair> getAllClassVarPairs(){
			List <ClassVarPair> allcvps = new ArrayList<TAModule.HollowTaskValue.ClassVarPair>();
			for(int i = 0; i < params.length; i++){
				allcvps.add(new ClassVarPair(pfclass.parameterClasses[i], params[i]));
			}
			return allcvps;
		}
		
	}
	
	
	public static class AbstractConditionsValue extends RVariableValue{
		
		public List<AbstractCondition> conditions;
		
		public AbstractConditionsValue(){
			conditions = new ArrayList<TAModule.AbstractCondition>();
		}
		
		public AbstractConditionsValue(RVariable varOwner){
			conditions = new ArrayList<TAModule.AbstractCondition>();
			this.setOwner(varOwner);
		}
		
		public void addCondition(AbstractCondition ac){
			this.conditions.add(ac);
		}
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof AbstractConditionsValue)){
				return false;
			}
			
			AbstractConditionsValue that = (AbstractConditionsValue)other;
			
			if(this.conditions.size() != that.conditions.size()){
				return false;
			}
			
			for(AbstractCondition ac : this.conditions){
				if(!that.conditions.contains(ac)){
					return false;
				}
			}
			
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			
			for(int i = 0; i < conditions.size(); i++){
				if(i > 0){
					buf.append(" ^ ");
				}
				buf.append(conditions.get(i).toString());
			}
			
			return buf.toString();
		}
		
		
		public List<ClassVarPair> getAllClassVarPairs(){
			List<ClassVarPair> allcvps = new ArrayList<TAModule.HollowTaskValue.ClassVarPair>();
			for(AbstractCondition ac : conditions){
				List <ClassVarPair> set = ac.getAllClassVarPairs();
				for(ClassVarPair cvp : set){
					if(!allcvps.contains(cvp)){
						allcvps.add(cvp);
					}
				}
			}
			
			
			return allcvps;
		}
		
		
	}
	
	
	
	public class TaskDescriptionValue extends RVariableValue{

		public List <GroundedProp> props;
		
		public TaskDescriptionValue(){
			this.props = new ArrayList<GroundedProp>();
		}
		
		public TaskDescriptionValue(RVariable owner){
			this.props = new ArrayList<GroundedProp>();
			this.setOwner(owner);
		}
		
		public void addGP(GroundedProp gp){
			this.props.add(gp);
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof TaskDescriptionValue)){
				return false;
			}
			
			TaskDescriptionValue that = (TaskDescriptionValue)other;
			
			for(GroundedProp gp : props){
				boolean foundMatch = false;
				for(GroundedProp tgp : that.props){
					if(gp.equals(tgp)){
						foundMatch = true;
					}
				}
				if(!foundMatch){
					return false;
				}
			}
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < this.props.size(); i++){
				if(i > 0){
					buf.append(" ");
				}
				GroundedProp gp = this.props.get(i);
				buf.append(gp.toString());
			}
			
			return buf.toString();
		}
		
		
		
	}
	
	
	
	public static class ConjunctiveGroundedPropRF implements RewardFunction{

		public List <GroundedProp>			gps;
		public double						goalR;
		public double						nonGoalR;
		
		public ConjunctiveGroundedPropRF(){
			this.gps = new ArrayList<GroundedProp>();
			this.goalR = 1;
			this.nonGoalR = 0;
		}
		
		public ConjunctiveGroundedPropRF(List <GroundedProp> gps){
			this.gps = gps;
			this.goalR = 1;
			this.nonGoalR = 0;
		}
		
		public void addGP(GroundedProp gp){
			this.gps.add(gp);
		}
		
		
		@Override
		public double reward(State s, GroundedAction ga, State sprime) {
		
			for(GroundedProp gp : gps){
				if(!gp.isTrue(sprime)){
					return nonGoalR;
				}
			}
			
			return goalR;
		}
		
		
	}
	
	
	
	public class RFConVariableValue extends RVariableValue{

		public ConjunctiveGroundedPropRF			rf;
		
		
		public RFConVariableValue(){
			rf = new ConjunctiveGroundedPropRF();
			this.setOwner(rewardRV);
		}
		
		public RFConVariableValue(ConjunctiveGroundedPropRF rf){
			this.rf = rf;
			this.setOwner(rewardRV);
		}
		
		public void addGoalGP(GroundedProp gp){
			this.rf.addGP(gp);
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof RFConVariableValue)){
				return false;
			}
			
			RFConVariableValue that = (RFConVariableValue)other;
			
			for(GroundedProp gp : this.rf.gps){
				boolean foundMatch = false;
				for(GroundedProp tgp : that.rf.gps){
					if(gp.equals(tgp)){
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
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			
			for(GroundedProp gp : this.rf.gps){
				buf.append(gp.toString() + " ");
			}
			
			return buf.toString();
		}
			
		
	}
	
	
	
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////////
	// Iterators
	////////////////////////////////////////////////////////////////////////////////
	
	
	
	
	
	
	
	public class HollowTaskVarIterator implements Iterator<RVariableValue>{

		Iterator<HollowTaskValue> iter;
		
		
		public HollowTaskVarIterator(){
			iter = hollowTasks.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public RVariableValue next() {
			if(!iter.hasNext()){
				throw new UnsupportedOperationException();
			}
			return iter.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
		
	}
	
	
	
	
	public class AbstractGoalConditionIterator implements Iterator<RVariableValue>{

		Iterator<AbstractConditionsValue> iter;
		
		public AbstractGoalConditionIterator(){
			iter = goalConditionValues.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public RVariableValue next() {
			return iter.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
		
	}
	
	
	/**
	 * This class will iterate through possible constraint sets given the list of constraint classes. However,
	 * this method is only defined when the constraint classes are all single parameter classes. It will not
	 * work if there are more than one.
	 * @author James MacGlashan
	 *
	 */
	public class AbstractConstraintSinglePFVarIterator implements Iterator<RVariableValue>{

		Iterator<HollowTaskValue>		htIterator;
		Iterator<RVariableValue>		condIter;

		AbstractConditionsValue			next;
		
		Set<RVariableValue>				previouslyComputedVals;
		
		
		public AbstractConstraintSinglePFVarIterator(){
			htIterator = hollowTasks.iterator();
			previouslyComputedVals = new HashSet<RVariableValue>();
			this.setupForNextHT();
		}
		
		
		@Override
		public boolean hasNext() {
			if(next != null){
				return true;
			}
			return false;
		}

		@Override
		public RVariableValue next() {
			
			RVariableValue toRet = next;
			
			if(toRet != null){
				if(!setupForNextCombination()){
					setupForNextHT();
				}
			}
			
			return toRet;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
			
		}
		
		protected void setupForNextHT(){
			
			if(!htIterator.hasNext()){
				next = null;
				return ;
			}
			
			HollowTaskValue ht = htIterator.next();
			condIter = new AbstractConstraintSinglePFVarConditionedIterator(ht);
			
			
			if(!this.setupForNextCombination()){
				this.setupForNextHT();
			}
			
		}
		
		protected boolean setupForNextCombination(){
			
			do{
				
				if(!condIter.hasNext()){
					return false;
				}
				
				next = (AbstractConditionsValue)condIter.next();
				
			}while(previouslyComputedVals.contains(next));
			
			previouslyComputedVals.add(next);
			
			return true;
			
		}
		
		
		
		
	}
	
	
	
	public class AbstractConstraintSinglePFVarConditionedIterator implements Iterator<RVariableValue>{

		
		List <AbstractCondition>		allPossibleConditionsForHT;
		int								nCombs;
		int								bitStringNumber;
		AbstractConditionsValue			next;
		
		
		public AbstractConstraintSinglePFVarConditionedIterator(HollowTaskValue ht){
			allPossibleConditionsForHT = new ArrayList<TAModule.AbstractCondition>();
			for(String pfcname : constraintPFClasses){
				PFClassTemplate pfclass = PFClassTemplate.getPFClassTemplateWithName(oomdpDomain, pfcname);
				String objectClassForParm = pfclass.parameterClasses[0];
				for(ClassVarPair cvp : ht.vars){
					if(cvp.varClass.equals(objectClassForParm)){
						AbstractCondition ac = new AbstractCondition(pfclass, new String[]{cvp.varName});
						allPossibleConditionsForHT.add(ac);
					}
				}
			}
			
			nCombs = (int)(Math.pow(2, allPossibleConditionsForHT.size()));
			
			//always skip zero (will be incremented in next method) 
			//because there is no null constraints... may want to change this later or add an additional null constraint 
			//at the end since there should only be one empty value, not one for each ht
			bitStringNumber = 0;
			
			this.setupNext();
		}
		
		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public RVariableValue next() {
			RVariableValue toRet = next;
			this.setupNext();
			return toRet;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		protected void setupNext(){
			
			bitStringNumber++;
			if(bitStringNumber >= nCombs){
				next = null;
				return ;
			}
			
			String bitString = get0PaddedBinary(bitStringNumber, allPossibleConditionsForHT.size());
			next = new AbstractConditionsValue(abstractConstraintRV);
			
			for(int i = 0; i < bitString.length(); i++){
				if(bitString.charAt(i) == '1'){
					next.addCondition(allPossibleConditionsForHT.get(i));
				}
			}
			
		}
		
		
		
		
	}
	
	
	
	public class TaskDescriptionIterator implements Iterator <RVariableValue>{

		RVariable										varToIterate;
		Iterator <RVariableValue>						parentIterator;
		Iterator<RVariableValue>						conditionedIterator;
		
		
		TaskDescriptionValue 							nextV;
		
		
		
		public TaskDescriptionIterator(RVariable varToIterate){
			this.varToIterate = varToIterate;
			if(varToIterate.equals(constraintRV)){
				parentIterator = new AbstractConstraintSinglePFVarIterator();
			}
			else if(varToIterate.equals(goalRV)){
				parentIterator = new AbstractGoalConditionIterator();
			}
			
			this.setupForNextParentValue();
		}
		
		
		
		@Override
		public boolean hasNext() {
			return nextV != null;
		}

		@Override
		public RVariableValue next() {
			TaskDescriptionValue toRet = nextV;
			
			if(!setupNext()){
				this.setupForNextParentValue();
			}
			
			
			return toRet;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
		protected void setupForNextParentValue(){
			
			if(!parentIterator.hasNext()){
				nextV = null;
				return ;
			}
			
			AbstractConditionsValue pval = (AbstractConditionsValue)parentIterator.next();
			conditionedIterator = new TaskDescriptionConditionedIterator(pval, varToIterate);
			
			
			this.setupNext();
			
		}
		
		protected boolean setupNext(){
			
			if(!conditionedIterator.hasNext()){
				return false;
			}
			
			nextV = (TaskDescriptionValue)conditionedIterator.next();
			
			return true;
		}
		
		
	}
	
	
	public class TaskDescriptionConditionedIterator implements Iterator<RVariableValue>{

		RVariable										varToIterate;
		
		List <List <GroundedProp>>						possiblePFForEachAbstract;
		List <Integer>									indexForDoubleNext;
		
		Map<String, Set<PropositionalFunction>> 		pfclassMap;
		
		TaskDescriptionValue 							nextV;
		
		
		public TaskDescriptionConditionedIterator(AbstractConditionsValue parentValue, RVariable varToIterate){
			
			this.varToIterate = varToIterate;
			
			pfclassMap = oomdpDomain.getPropositionlFunctionsMap();
			
			possiblePFForEachAbstract = new ArrayList<List<GroundedProp>>(parentValue.conditions.size());
			for(AbstractCondition ac : parentValue.conditions){
				Set<PropositionalFunction> pfsForClass = pfclassMap.get(ac.pfclass.className);
				List <GroundedProp> possibleProps = new ArrayList<GroundedProp>(pfsForClass.size());
				for(PropositionalFunction pf : pfsForClass){
					GroundedProp gp = new GroundedProp(pf, ac.params.clone());
					possibleProps.add(gp);
				}
				possiblePFForEachAbstract.add(possibleProps);
			}
			
			indexForDoubleNext = new ArrayList<Integer>();
			for(int i = 0; i < possiblePFForEachAbstract.size(); i++){
				indexForDoubleNext.add(0);
			}
			
			this.setupNext();
			
		}
		
		
		protected void setupNext(){
			
			if(indexForDoubleNext.get(0) >= possiblePFForEachAbstract.get(0).size()){
				nextV = null;
				return;
			}
			
			nextV = new TaskDescriptionValue(varToIterate);
			for(int i = 0; i < possiblePFForEachAbstract.size(); i++){
				int ind = indexForDoubleNext.get(i);
				nextV.addGP(possiblePFForEachAbstract.get(i).get(ind));
			}
			
			//increment counter
			for(int i = indexForDoubleNext.size() - 1; i >= 0; i--){
				int v = indexForDoubleNext.get(i);
				v++;
				if(v < possiblePFForEachAbstract.get(i).size() || i == 0){
					indexForDoubleNext.set(i, v);
					break; //finished
				}
				else{
					indexForDoubleNext.set(i, 0);
				}
			}
			
		}
		
		
		@Override
		public boolean hasNext() {
			
			return nextV != null;
		}

		@Override
		public RVariableValue next() {
			
			RVariableValue toRet = nextV;
			this.setupNext();
			return toRet;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
		
		
	}
	
	
	
	
	public class RFConditionedIterator implements Iterator <RVariableValue>{

		StateRVValue							srv;
		TaskDescriptionValue					constraints;
		TaskDescriptionValue					goals;
		List <Map <String, String>> 			varMappings;
		int										nextMapping;
		
		
		RVariableValue							next;
		
		
		public RFConditionedIterator(StateRVValue srv, TaskDescriptionValue constraints, TaskDescriptionValue goals){
			
			this.srv = srv;
			this.constraints = constraints;
			this.goals = goals;
			
			
			Set<ClassVarPair> uniqueVars = new HashSet<TAModule.HollowTaskValue.ClassVarPair>();
			
			//add from constraints
			for(GroundedProp gp : constraints.props){
				String [] paramClasses = gp.pf.getParameterClasses();
				for(int i = 0; i < gp.params.length; i++){
					ClassVarPair cvp = new ClassVarPair(paramClasses[i], gp.params[i]);
					uniqueVars.add(cvp);
				}
			}
			
			//add from goals
			for(GroundedProp gp : goals.props){
				String [] paramClasses = gp.pf.getParameterClasses();
				for(int i = 0; i < gp.params.length; i++){
					ClassVarPair cvp = new ClassVarPair(paramClasses[i], gp.params[i]);
					uniqueVars.add(cvp);
				}
			}
			
			List <ClassVarPair> vars = new ArrayList<TAModule.HollowTaskValue.ClassVarPair>(uniqueVars);
			String [] paramClasses = new String[vars.size()];
			String [] paramOrderGroup = new String[vars.size()];
			
			for(int i = 0; i < vars.size(); i++){
				paramClasses[i] = vars.get(i).varClass;
				paramOrderGroup[i] = vars.get(i).varName;
			}
			
			List <List <String>> possibleBindings = srv.s.getPossibleBindingsGivenParamOrderGroups(paramClasses, paramOrderGroup);
			varMappings = new ArrayList<Map<String,String>>();
			
			for(List <String> abinding : possibleBindings){
				Map <String, String> amapping = new HashMap<String, String>();
				varMappings.add(amapping);
				for(int i = 0; i < abinding.size(); i++){
					amapping.put(paramOrderGroup[i], abinding.get(i));
				}
			}
			
			nextMapping = 0;
			
			this.setupNext();
			
		}
		
		
		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public RVariableValue next() {
			RVariableValue toRet = next;
			this.setupNext();
			return toRet;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
		protected void setupNext(){
			
			if(nextMapping >= varMappings.size()){
				next = null;
				return;
			}
			
			boolean foundValid = false;
			do{
				
				Map <String, String> amapping = varMappings.get(nextMapping);
				if(this.satisfiesConstraints(amapping)){
					foundValid = true;
					List <GroundedProp> boundGoals = this.getMappedGPs(goals.props, amapping);
					ConjunctiveGroundedPropRF rf = new ConjunctiveGroundedPropRF(boundGoals);
					next = new RFConVariableValue(rf);
				}
				
				
				nextMapping++;
				
			}while(nextMapping < varMappings.size() && !foundValid);
			
			if(!foundValid){
				next = null;
			}
			
		}
		
		
		
		protected boolean satisfiesConstraints(Map <String, String> amapping){
			
			List <GroundedProp> bound = this.getMappedGPs(constraints.props, amapping);
			
			for(GroundedProp gp : bound){
				if(!gp.isTrue(srv.s)){
					return false;
				}
			}
			
			return true;
			
		}
		
		
		protected List <GroundedProp> getMappedGPs(List <GroundedProp> src, Map <String, String> amapping){
			
			List <GroundedProp> res = new ArrayList<GroundedProp>(src.size());
			for(GroundedProp sgp : src){
				String [] bparams = new String [sgp.params.length];
				for(int i = 0; i < sgp.params.length; i++){
					bparams[i] = amapping.get(sgp.params[i]);
				}
				GroundedProp gp = new GroundedProp(sgp.pf, bparams);
				res.add(gp);
			}
			
			return res;
			
		}
		
		
	}
	

}
