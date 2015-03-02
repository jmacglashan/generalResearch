package behavior.training.taskinduction.commands.version2;

import behavior.training.taskinduction.TaskInductionTraining;
import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.sabl.SABLAgent;
import commands.model3.TaskModule;
import generativemodel.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * NOTE: Does not return likelihood; returns full probability.
 *
 * @author James MacGlashan.
 */
public class FeedbackGMMod2 extends GMModule{

	public static final String						FEEDBACKVARNAME = "feedback";

	protected RVariable feedbackRV;

	protected RVariable								rewardRV;

	protected SABLAgent 							feedbackAnalyzer;

	public FeedbackGMMod2(String name, RVariable rewardRV, SABLAgent feedbackAnalyzer) {
		super(name);
		this.feedbackRV = new RVariable(FEEDBACKVARNAME, this);
		this.rewardRV = rewardRV;
		this.feedbackAnalyzer = feedbackAnalyzer;
	}

	@Override
	public GMQueryResult computeProb(GMQuery query) {

		Set<RVariableValue> conditions = query.getConditionValues();
		TaskModule.RFConVariableValue rval = (TaskModule.RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);

		double p = 0.;
		boolean found = false;
		List<TaskProb> tps = this.feedbackAnalyzer.getTaskProbabilityDistribution();
		for(TaskProb tp : tps){
			if(tp.getTask().rf.equals(rval.rf)){
				p = tp.getProb();
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
