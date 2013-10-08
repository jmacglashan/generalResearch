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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.core.GroundedProp;

import commands.model2.gm.TAModule.TaskDescriptionValue;

public class MMNLPModule extends MNPEMModule {

	public static final String						PNAME = "prop";
	public static final String						WNAME = "word";
	public static final String						CNAME = "command";
	
	
	protected RVariable								commandLenRV;
	protected RVariable								constraintRV;
	protected RVariable								goalRV;

	protected RVariable								propRV;
	protected RVariable								wordRV;
	protected RVariable								commandRV;
	
	
	protected List <String>							wordDeliminators;
	
	protected List <String>							propFunctionNames;
	protected List <String> 						vocab;
	protected Set <String>							vocabSet;
	protected Set <String>							filterSet;
	
	
	protected boolean								useConstantFeature = true;
	protected String								constantFeatureName = "########";
	
	
	
	public MMNLPModule(String name, RVariable commandLenRV, RVariable constraintRV, RVariable goalRV, List <String> propFunctionNames, List <String> trainingCommands) {
		super(name);
		
		this.commandLenRV = commandLenRV;
		this.constraintRV = constraintRV;
		this.goalRV = goalRV;
		
		this.externalDependencyList.add(commandLenRV);
		this.externalDependencyList.add(constraintRV);
		this.externalDependencyList.add(goalRV);
		
		this.propRV = new RVariable(PNAME, this);
		this.propRV.addDependency(constraintRV);
		this.propRV.addDependency(goalRV);
		
		this.wordRV = new RVariable(WNAME, this);
		this.wordRV.addDependency(this.propRV);
		
		this.commandRV = new RVariable(CNAME, this);
		this.commandRV.addDependency(commandLenRV);
		this.commandRV.addDependency(constraintRV);
		this.commandRV.addDependency(goalRV);
		
		
		this.propFunctionNames = new ArrayList<String>(propFunctionNames);
		if(this.useConstantFeature){
			this.propFunctionNames.add(constantFeatureName);
		}
		
		
		wordDeliminators = new ArrayList<String>();
		wordDeliminators.add(" ");
		wordDeliminators.add("-");
		
		filterSet = new HashSet<String>();
		filterSet.add("!");
		filterSet.add(",");
		filterSet.add("(");
		filterSet.add(")");
		filterSet.add("/");
		filterSet.add("....");
		filterSet.add("a");
		filterSet.add("n");
		filterSet.add("to");
		filterSet.add("so");
		filterSet.add("and");
		filterSet.add("that");
		filterSet.add("re");
		filterSet.add("at");
		filterSet.add("as");
		filterSet.add("of");
		filterSet.add("or");
		filterSet.add(".....");
		filterSet.add(").");
		filterSet.add("if");
		
		
		//set up vocabulary
		vocabSet = new HashSet<String>();
		for(String c : trainingCommands){
			c = c.trim();
			String [] words = this.wordsInCommand(c);
			for(int i = 0; i < words.length; i++){
				if(!filterSet.contains(words[i])){
					vocabSet.add(words[i]);
				}
			}
		}
		vocab = new ArrayList<String>(vocabSet);
		
		double uniVocab = 1./(double)vocab.size();
		System.out.println("Uniform: " + uniVocab);
		
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

	
	public int vocabSize(){
		return this.vocab.size();
	}
	
	public String constantFeatureName(){
		return constantFeatureName;
	}
	
	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		double p = 0.;
		RVariableValue qv = query.getSingleQueryVar();
		
		if(qv.isValueFor(propRV)){
			p = this.computeProbForProp(query);
		}
		if(qv.isValueFor(wordRV)){
			p = this.computeMNParamProb(qv, query.getConditionValues());
		}
		if(qv.isValueFor(commandRV)){
			p = this.computeProbForCommand(query);
		}
		
		return new GMQueryResult(query, p);
	}

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar, List<RVariableValue> conditions) {
		
		if(queryVar.equals(propRV)){
			RVariableValue constraints = this.extractValueForVariableFromConditions(constraintRV, conditions);
			RVariableValue goals = this.extractValueForVariableFromConditions(goalRV, conditions);
			Iterator<RVariableValue> condIter = new ConditionedPFVarIter((TaskDescriptionValue)constraints, (TaskDescriptionValue)goals);
			
			return new P0RejectQRIterator(condIter, conditions, this.owner);
		}
		
		if(queryVar.equals(wordRV)){
			Iterator<RVariableValue> wIter = new StringVarIter(vocab, wordRV);
			return new P0RejectQRIterator(wIter, conditions, this.owner);
		}
		
		
		return null;
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		
		if(queryVar.equals(this.propRV)){
			return new StringVarIter(this.propFunctionNames, this.propRV);
		}
		if(queryVar.equals(this.wordRV)){
			return new StringVarIter(vocab, this.wordRV);
		}
		
		return null;
	}

	
	
	
	
	private String [] wordsInCommand(String command){
		List <String> tokens = this.tokenizeString(command, wordDeliminators, 0);
		return tokens.toArray(new String[tokens.size()]);
	}
	
	private List <String> tokenizeString(String s, List <String> delimList, int dIndex){
		
		List <String> res = new ArrayList<String>();
		
		String delim = delimList.get(dIndex);
		
		String [] tokens = s.split(delim);
		for(int i = 0; i < tokens.length; i++){
			if(dIndex < delimList.size()-1){
				res.addAll(this.tokenizeString(tokens[i], delimList, dIndex+1));
			}
			else{
				res.add(tokens[i]);
			}
		}
		
		return res;
	}
	
	
	
	
	
	public double computeProbForProp(GMQuery query){
		
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
			return 0.;
		}
		
		double n = pfNames.size();
		if(useConstantFeature){
			n += 1.;
		}
		
		double p = 1./n;
		
		return p;
	}
	

	
	
	public double computeProbForCommand(GMQuery query){
		
		StringValue commandValue = (StringValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		TaskDescriptionValue constraints = (TaskDescriptionValue)this.extractValueForVariableFromConditions(constraintRV, conditions);
		TaskDescriptionValue goals = (TaskDescriptionValue)this.extractValueForVariableFromConditions(goalRV, conditions);
		
		Map<StringValue, Integer> counts = getWordCounts(commandValue.s);
		Set <String> pfNames = this.getUniquePFNames(constraints, goals);
		
		double p = 1.;
		for(StringValue wVal : counts.keySet()){
			p *= Math.pow(this.probWGivenProps(pfNames, wVal), counts.get(wVal));
		}
		
		if(p == 0){
			p = 1.;
			for(StringValue wVal : counts.keySet()){
				p *= Math.pow(this.probWGivenProps(pfNames, wVal), counts.get(wVal));
			}
		}
		
		
		return p;
	}
	
	
	public Set<String> getUniquePFNames(TaskDescriptionValue constraints, TaskDescriptionValue goals){
		
		Set<String> pfNames = new HashSet<String>();
		
		for(GroundedProp gp : constraints.props){
			String pfName = gp.pf.getName();
			pfNames.add(pfName);
		}
		for(GroundedProp gp : goals.props){
			String pfName = gp.pf.getName();
			pfNames.add(pfName);
		}
		
		if(this.useConstantFeature){
			pfNames.add(this.constantFeatureName);
		}
		
		return pfNames;
	}
	
	public Map<StringValue, Integer> getWordCounts(String command){
		
		Map <StringValue, Integer> counts = new HashMap<MMNLPModule.StringValue, Integer>();
		command = command.trim();
		String [] words = this.wordsInCommand(command);
		for(int i = 0; i < words.length; i++){
			String w = words[i];
			if(!vocabSet.contains(w)){
				continue;
			}
			StringValue wv = new StringValue(w, wordRV);
			Integer curV = counts.get(wv);
			int nv = curV == null ? 1 : curV + 1;
			counts.put(wv, nv);
			
		}
		
		return counts;
	}
	
	public double probWGivenProps(Set <String> pfNames, StringValue wordValue){
		
		double p = 0.;

		
		for(String pfName : pfNames){
			StringValue pfVal = new StringValue(pfName, propRV);
			MultiNomialRVPI index = new MultiNomialRVPI(wordValue);
			index.addConditional(pfVal);
			double param = wordRV.getParameter(index);
			p += param;
		}
		
		p /= (double)pfNames.size();
		
		return p;
		
	}
	
	
	public static class StringValue extends RVariableValue{

		public String			s;
		
		
		public StringValue(String s, RVariable owner){
			this.s = s;
			//this.s = s.toLowerCase();
			this.setOwner(owner);
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof StringValue)){
				return false;
			}
			
			if(!this.owner.equals(other.getOwner())){
				return false;
			}
			
			StringValue that = (StringValue)other;

			return this.s.equals(that.s);
		}

		@Override
		public String stringRep() {
			return s;
		}
		
		
	}
	
	
	
	class StringVarIter implements Iterator<RVariableValue>{

		Iterator<String>	sIter;
		RVariable			owner;
		
		
		public StringVarIter(List <String> values, RVariable owner){		
			this.sIter = values.iterator();
			this.owner = owner;
		}
		
		@Override
		public boolean hasNext() {
			return sIter.hasNext();
		}

		@Override
		public RVariableValue next() {
			
			String s = sIter.next();
			StringValue sv = new StringValue(s, owner);
			return sv;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}
	
	
	class ConditionedPFVarIter implements Iterator<RVariableValue>{

		Iterator<String> sIter;
		
		
		public ConditionedPFVarIter(TaskDescriptionValue constraints, TaskDescriptionValue goals){
			
			Set <String> pfNames = new HashSet<String>();
			for(GroundedProp gp : constraints.props){
				pfNames.add(gp.pf.getName());
			}
			for(GroundedProp gp : goals.props){
				pfNames.add(gp.pf.getName());
			}
			
			this.sIter = pfNames.iterator();
			
		}
		
		
		@Override
		public boolean hasNext() {
			return sIter.hasNext();
		}

		@Override
		public RVariableValue next() {
			
			String s = sIter.next();
			StringValue sv = new StringValue(s, propRV);
			return sv;
					
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		
	}
	
	
	
	
}
