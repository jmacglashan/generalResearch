package behavior.training.taskinduction.commands;

import generativemodel.GMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import behavior.training.taskinduction.TaskInductionTraining;
import behavior.training.taskinduction.TaskProb;

import commands.model3.TaskModule.RFConVariableValue;

public class FeedbackGMMod extends GMModule {

	public static final String						FEEDBACKVARNAME = "feedback";
	
	protected RVariable								feedbackRV;
	
	protected RVariable								rewardRV;
	
	protected TaskInductionTraining 				feedbackAnalyzer;
	
	public FeedbackGMMod(String name, RVariable rewardRV, TaskInductionTraining feedbackAnalyzer) {
		super(name);
		this.feedbackRV = new RVariable(FEEDBACKVARNAME, this);
		this.rewardRV = rewardRV;
		this.feedbackAnalyzer = feedbackAnalyzer;
	}

	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		Set <RVariableValue> conditions = query.getConditionValues();
		RFConVariableValue rval = (RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);
		
		double p = 0.;
		boolean found = false;
		List<TaskProb> tps = this.feedbackAnalyzer.getPosteriors().getTaskProbs();
		for(TaskProb tp : tps){
			if(tp.getTask().rf.equals(rval.rf)){
				p = tp.getLikilhood();
				found = true;
				break;
			}
		}
		
		if(!found){
			throw new RuntimeException("No Matching probabiltiy associated with reward function: " + rval.rf.toString());
		}
		
		GMQueryResult result = new GMQueryResult(query, p);
		
		return result;
	}

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar, List<RVariableValue> conditions) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		throw new UnsupportedOperationException();
	}
	
	
	
	public static class StaticFeedbackVarVal extends RVariableValue{

		public int id = 0;
		
		public StaticFeedbackVarVal(RVariable owner){
			this.setOwner(owner);
		}
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			if(this == other){
				return true;
			}
			
			if(!(other instanceof StaticFeedbackVarVal)){
				return false;
			}
			
			return this.id == ((StaticFeedbackVarVal)other).id;

		}

		@Override
		public String stringRep() {
			return "" + id;
		}
		
		
		
	}

}
