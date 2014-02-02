package behavior.training.taskinduction.commands;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskInductionTraining;
import behavior.training.taskinduction.commands.FeedbackGMMod.StaticFeedbackVarVal;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

import commands.model3.GPConjunction;
import commands.model3.StringValue;
import commands.model3.TaskModule;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.RFConVariableValue;
import commands.model3.TaskModule.StateRVValue;
import commands.model3.TrajectoryModule.ConjunctiveGroundedPropTF;
import commands.model3.mt.MTModule;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;
import commands.model3.mt.MTModule.WordParam;
import commands.model3.mt.em.MTEMModule;
import commands.model3.mt.em.WeightedMTInstance;

import datastructures.HashedAggregator;

public class CommandsToTrainingInterface {

	public final static String			TASKMODNAME = "taskMod";
	public final static String			FEEDBACKMODNAME = "feedbackMod";
	public final static String			LANGMODNAME = "langMod";
	
	
	protected GenerativeModel			gm;
	protected TaskModule				taskMod;
	protected FeedbackGMMod				feedMod;
	protected MTModule					mtMod;
	
	protected Domain					domain;
	protected StateHashFactory			hashingFactory;
	protected Tokenizer					tokenizer;
	
	protected TaskInductionTraining		feedbackAnalyzer;
	
	protected RVariable					naturalCommandVariable;
	
	protected List<WeightedMTInstance> 	mtDataset;
	
	
	public CommandsToTrainingInterface(Domain domain, List<GPConjunction> taskGoals, StateHashFactory hashingFactory, TaskInductionTraining feedbackAnalyzer, 
			Tokenizer tokenizer, int maxSemanticCommandLength){
		
		this.domain = domain;
		this.hashingFactory = hashingFactory;
		this.tokenizer = tokenizer;
		
		this.feedbackAnalyzer = feedbackAnalyzer;
		
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
		
		
		this.feedMod = new FeedbackGMMod(FEEDBACKMODNAME, this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), feedbackAnalyzer);
		this.gm.addGMModule(feedMod);
		
		this.mtDataset = new ArrayList<WeightedMTInstance>();
		Set<String> semanticWords = this.getSemanticWords(true);
		
		this.mtMod = new MTModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME), semanticWords, 
				new HashSet<String>(), maxSemanticCommandLength, 1, tokenizer);
		
		this.gm.addGMModule(mtMod);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(MTModule.NNAME);
		
	}
	
	
	public void setRFDistribution(State initialState, String naturalCommand){
		
		//System.out.println(naturalCommand);
		
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
					
					System.out.println(p + ": " + grRes.getSingleQueryVar().toString() + " " + bRes.getSingleQueryVar().toString());
					
					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(grRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);
					distroWrapper.addCondition(ncommandVal);
					
					jointP.add(distroWrapper, p);
					totalProb += p;
					
				}
				
			}
			
		}
		
		
		//List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		List<TaskDescription> taskForTraining = new ArrayList<TaskDescription>();
		List<Double> priors = new ArrayList<Double>();
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			
			RFConVariableValue rfVar = (RFConVariableValue)e.getKey().getSingleQueryVar();
			TaskDescription td = new TaskDescription(rfVar.rf, new ConjunctiveGroundedPropTF(rfVar.rf.gps));
			taskForTraining.add(td);
			priors.add(prob);
		}
		
		this.feedbackAnalyzer.resetTasks(taskForTraining, priors);
		this.feedbackAnalyzer.planPossibleTasksFromSeedState(initialState);
		
		
		
	}
	
	
	public void addLastTrainingResultToDatasetAndRetrain(State initialState, String command, double threshold){
		
		Map<String, Double> semanticProbs = this.getSemanticSentenceDistributionWithFeedback(initialState);
		
		System.out.println("Trained semantic entries for: \"" + command + "\"");
		
		WeightedMTInstance instance = new WeightedMTInstance(this.tokenizer.tokenize(command));
		for(Map.Entry<String, Double> e : semanticProbs.entrySet()){
			double p = e.getValue();
			if(p > threshold){
				TokenedString tokenedSem = tokenizer.tokenize(e.getKey());
				instance.addWeightedSemanticCommand(tokenedSem, p);
				System.out.println(p + ": " + tokenedSem.toString());
			}
		}
		this.mtDataset.add(instance);
		
		Set<String> naturalWords = new HashSet<String>();
		int maxNaturalCommandLength = getNaturalWordsFromMTDataset(mtDataset, naturalWords);
		
		this.mtMod.resetParametersToUniforForNewDictionary(naturalWords, maxNaturalCommandLength);
		
		
		System.out.println("Beginning Lanugage Learning");
		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(10);
		System.out.println("Finished Lanugage Learning");
		this.gm.emptyCache();
		printWordParams(this.mtMod);
		//this.mtMod.printDistortionParams();
		
		
		
	}
	
	
	
	protected Map<String, Double> getSemanticSentenceDistributionWithFeedback(State initialState){
		
		StateRVValue sval = new StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		HashedAggregator<String> semSentenceProbs = new HashedAggregator<String>();
		HashedAggregator<String> rewardProbs = new HashedAggregator<String>();
		double sumFeedbackProb = 0.;
		
		StaticFeedbackVarVal feedVarVal = new StaticFeedbackVarVal(this.gm.getRVarWithName(FeedbackGMMod.FEEDBACKVARNAME));
		
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
				//double stackLRGR = lrRes.probability*grRes.probability;
				double stackLRGR = lrRes.probability; //don't include grounded rf prob because it will implicitly be provided from the feedback return
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				
				//compute probability of trajectory
				GMQuery feedQuery = new GMQuery();
				feedQuery.addQuery(feedVarVal);
				feedQuery.addCondition(sval);
				feedQuery.addCondition(grRes.getSingleQueryVar());
				GMQueryResult feedRes = this.gm.getProb(feedQuery, true);
				
				double stackedFeedbackProb = stackLRGR * feedRes.probability;
				rewardProbs.add(grRes.getSingleQueryVar().toString(), stackedFeedbackProb);
				sumFeedbackProb += stackedFeedbackProb;
				
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					
					String sementicString = getSemanticString((LiftedVarValue)lrRes.getSingleQueryVar(), (LiftedVarValue)bRes.getSingleQueryVar());
					double jointP = stackedFeedbackProb * bRes.probability;
					semSentenceProbs.add(sementicString, jointP);
					
				}
				
				
			}
			
			
		}
		
		
		Map<String, Double> semanticStringProbs = new HashMap<String, Double>(semSentenceProbs.size());
		for(Map.Entry<String, Double> e : semSentenceProbs.entrySet()){
			double normP = e.getValue() / sumFeedbackProb;
			semanticStringProbs.put(e.getKey(), normP);
		}
		
		for(Map.Entry<String, Double> e : rewardProbs.entrySet()){
			double normP = e.getValue() / sumFeedbackProb;
			System.out.println(normP + ": " + e.getKey());
		}
		
		return semanticStringProbs;
	}
	
	
	
	
	
	public Set<String> getSemanticWords(boolean includeParameterObjectClass){
		Set<String> semanticWords = new HashSet<String>();
		semanticWords.add(TokenedString.NULLTOKEN);
		for(PropositionalFunction pf : this.domain.getPropFunctions()){
			semanticWords.add(pf.getName());
			if(includeParameterObjectClass){
				for(String p : pf.getParameterClasses()){
					semanticWords.add(p);
				}
			}
		}
		return semanticWords;
	}
	
	
	
	
	
	
	
	public static String getSemanticString(LiftedVarValue liftedRF, LiftedVarValue bindingConstraints){

		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for(GroundedProp gp : liftedRF.conditions){
			if(!first){
				buf.append(" ");
			}
			buf.append(gp.pf.getName());
			for(String p : gp.pf.getParameterClasses()){
				buf.append(" ").append(p);
			}
			first = false;
		}
		for(GroundedProp gp : bindingConstraints.conditions){
			buf.append(" ").append(gp.pf.getName());
			for(String p : gp.pf.getParameterClasses()){
				buf.append(" ").append(p);
			}
		}
		
		return buf.toString();
	}
	
	
	
	/**
	 * Fills in up a provided set with the natural words and returns the maximum semantic command length
	 * @param dataset the input MT dataset
	 * @param natWords the set to fill with the natural words
	 * @return the maximum natural command length
	 */
	public static int getNaturalWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> natWords){
		
		int maxLength = 0;
		for(WeightedMTInstance wi : dataset){
			maxLength = Math.max(maxLength, wi.naturalCommand.size());
			for(int i = 1; i <= wi.naturalCommand.size(); i++){
				natWords.add(wi.naturalCommand.t(i));
			}
		}
		
		return maxLength;
		
	}
	
	
	public static void printWordParams(MTModule mtmod){
		
		Set<String> semWords = mtmod.getSemanticWords();
		Set<String> natWords = mtmod.getNaturalWords();
		WordParam wp = mtmod.getWp();
		
		for(String s : semWords){
			System.out.println(s+"\n--------------------------------------");
			for(String n : natWords){
				double param = wp.prob(n, s);
				if(param > -1.){
					System.out.println(param + " " + n);
				}
			}
		}
		
	}
	
}
