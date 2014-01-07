package commands.model3;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import behavior.irl.DGDIRLFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

import commands.data.TrainingElement;
import commands.data.TrainingElementParser;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.StateRVValue;
import commands.model3.mt.MTModule;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;

import datastructures.HashedAggregator;

public class Model3Controller {

	public final static String			TASKMODNAME = "taskMod";
	public final static String			TRAJECMODNAME = "trajectoryMod";
	public final static String			LANGMODNAME = "langMod";
	
	
	protected GenerativeModel			gm;
	protected TaskModule				taskMod;
	protected TrajectoryModule			trajectMod;
	
	
	protected Domain					domain;
	protected StateHashFactory			hashingFactory;
	
	protected RVariable					naturalCommandVariable;
	
	public Model3Controller(Domain domain, List<GPConjunction> taskGoals, StateHashFactory hashingFactory, boolean addTermainateActionForIRL){
		
		this.domain = domain;
		this.hashingFactory = hashingFactory;
		
		this.gm = new GenerativeModel();
		
		this.taskMod = new TaskModule(TASKMODNAME, this.domain);
		this.gm.addGMModule(this.taskMod);
		
		RVariable liftedVar = gm.getRVarWithName(TaskModule.LIFTEDRFNAME);
		for(GPConjunction conj : taskGoals){
			LiftedVarValue lrf = new LiftedVarValue(liftedVar);
			for(GroundedProp gp : conj){
				lrf.addProp(gp);
			}
			this.taskMod.addLiftedVarValue(lrf);
		}
		
		DGDIRLFactory plannerFactory = new DGDIRLFactory(this.domain, 0.99, this.hashingFactory);
		this.trajectMod = new TrajectoryModule(TRAJECMODNAME, this.gm.getRVarWithName(TaskModule.STATENAME), this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), this.domain, plannerFactory, addTermainateActionForIRL, true);
		this.gm.addGMModule(trajectMod);
		
	}
	
	public GenerativeModel getGM(){
		return this.gm;
	}
	
	public void setToMTLanguageModel(Set<String> semanticWords, Set<String> naturalWords, int maxSemanticCommandLength, int maxNaturalCommandLenth, Tokenizer tokenizer){
		
		MTModule langMod = new MTModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME), 
				semanticWords, naturalWords, maxSemanticCommandLength, maxNaturalCommandLenth, tokenizer);
		
		this.gm.addGMModule(langMod);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(MTModule.NNAME);
		
	}
	
	
	public void setToMTLanguageModel(List<TrainingElement> trainingData, int maxSemanticCommandLength, Tokenizer tokenizer){
		
		Set<String> semanticWords = this.getSemanticWords();
		Set<String> naturalWords = this.getNaturalWords(trainingData, tokenizer);
		
		int maxNaturalCommandLenth = 0;
		for(TrainingElement te : trainingData){
			maxNaturalCommandLenth = Math.max(maxNaturalCommandLenth, tokenizer.tokenize(te.command).size());
		}
		
		MTModule langMod = new MTModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME), 
				semanticWords, naturalWords, maxSemanticCommandLength, maxNaturalCommandLenth, tokenizer);
		
		this.gm.addGMModule(langMod);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(MTModule.NNAME);
		
	}
	
	
	public List<GMQueryResult> getLiftedRFAndBindingDistribution(State initialState, String naturalCommand){
		
		StateRVValue sval = new StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		StringValue ncommandVal = new StringValue(naturalCommand, naturalCommandVariable);
		
		HashedAggregator<GMQuery> jointP = new HashedAggregator<GMQuery>();
		double totalProb = 0.;
		
		List<RVariableValue> sconds = new ArrayList<RVariableValue>(1);
		sconds.add(sval);
		Iterator<GMQueryResult> lrIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), sconds, true);
		while(lrIter.hasNext()){
			GMQueryResult lrRes = lrIter.next();
			
			List<RVariableValue> lrConds = new ArrayList<RVariableValue>(2);
			lrConds.add(sval);
			lrConds.add(lrRes.getSingleQueryVar());
			Iterator<GMQueryResult> grIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), lrConds, true);
			while(grIter.hasNext()){
				GMQueryResult grRes = grIter.next();
				double stackLRGR = lrRes.probability*grRes.probability;
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					double stackLRGRB = stackLRGR * bRes.probability;
					
					GMQuery nCommandQuery = new GMQuery();
					nCommandQuery.addQuery(ncommandVal);
					nCommandQuery.addCondition(lrRes.getSingleQueryVar());
					nCommandQuery.addCondition(bRes.getSingleQueryVar());
					
					GMQueryResult langQR = this.gm.getProb(nCommandQuery, true);
					double p = langQR.probability * stackLRGRB;
					
					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(lrRes.getSingleQueryVar());
					distroWrapper.addQuery(bRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);
					distroWrapper.addCondition(ncommandVal);
					
					jointP.add(distroWrapper, p);
					totalProb += p;
					
				}
				
			}
			
		}
		
		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}
		
		
		return distro;
	}
	
	public List<GMQueryResult> getRFDistribution(State initialState, String naturalCommand){
		
		StateRVValue sval = new StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		StringValue ncommandVal = new StringValue(naturalCommand, naturalCommandVariable);
		
		HashedAggregator<GMQuery> jointP = new HashedAggregator<GMQuery>();
		double totalProb = 0.;
		
		List<RVariableValue> sconds = new ArrayList<RVariableValue>(1);
		sconds.add(sval);
		Iterator<GMQueryResult> lrIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), sconds, true);
		while(lrIter.hasNext()){
			GMQueryResult lrRes = lrIter.next();
			
			List<RVariableValue> lrConds = new ArrayList<RVariableValue>(2);
			lrConds.add(sval);
			lrConds.add(lrRes.getSingleQueryVar());
			Iterator<GMQueryResult> grIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), lrConds, true);
			while(grIter.hasNext()){
				GMQueryResult grRes = grIter.next();
				double stackLRGR = lrRes.probability*grRes.probability;
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					double stackLRGRB = stackLRGR * bRes.probability;
					
					GMQuery nCommandQuery = new GMQuery();
					nCommandQuery.addQuery(ncommandVal);
					nCommandQuery.addCondition(lrRes.getSingleQueryVar());
					nCommandQuery.addCondition(bRes.getSingleQueryVar());
					
					GMQueryResult langQR = this.gm.getProb(nCommandQuery, true);
					double p = langQR.probability * stackLRGRB;
					
					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(grRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);
					distroWrapper.addCondition(ncommandVal);
					
					jointP.add(distroWrapper, p);
					totalProb += p;
					
				}
				
			}
			
		}
		
		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}
		
		
		return distro;
	}
	
	
	
	public Set<String> getSemanticWords(){
		Set<String> semanticWords = new HashSet<String>();
		semanticWords.add(TokenedString.NULLTOKEN);
		for(PropositionalFunction pf : this.domain.getPropFunctions()){
			semanticWords.add(pf.getName());
			for(String p : pf.getParameterClasses()){
				semanticWords.add(p);
			}
		}
		return semanticWords;
	}
	
	public Set<String> getNaturalWords(List<TrainingElement> dataset, Tokenizer tokenizer){
		Set<String> naturalWords = new HashSet<String>();
		for(TrainingElement te : dataset){
			TokenedString t = tokenizer.tokenize(te.command);
			for(int i = 1; i <= t.size(); i++){
				String nw = t.t(i);
				naturalWords.add(nw);
			}
		}
		return naturalWords;
	}
	
	
	public static List <TrainingElement> getCommandsDataset(Domain domain, String path, StateParser sp){
		
		TrainingElementParser teparser = new TrainingElementParser(domain, sp);
		
		//get dataset
		List<TrainingElement> dataset = teparser.getTrainingElementDataset(path, ".txt");
		
		return dataset;
	}
	
	
}
