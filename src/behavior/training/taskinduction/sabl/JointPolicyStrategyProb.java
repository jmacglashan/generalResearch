package behavior.training.taskinduction.sabl;

import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class JointPolicyStrategyProb {

	public TaskPolicyProbTuple taskPrior;
	public FeedbackStrategyProbPair feedbackPrior;
	public double prob;

	public JointPolicyStrategyProb(TaskPolicyProbTuple taskPrior, FeedbackStrategyProbPair feedbackPrior) {
		this.taskPrior = taskPrior;
		this.feedbackPrior = feedbackPrior;
		this.prob = taskPrior.getProb() * feedbackPrior.getPrior();
	}



	public double proportionalPosterior(State s, GroundedAction ga, double f){
		double l = this.prob*this.feedbackPrior.getStrategy().liklihood(s, ga, f, this.taskPrior.getPolicy());
		return l;
	}


	/**
	 * Computes marginal for the given task. A task in the joint distribution is considered equal to the query task
	 * if its pointer address is the same.
	 * @param jointDistribution
	 * @param task
	 * @return
	 */
	public static double marginalizeForTask(List<JointPolicyStrategyProb> jointDistribution, TaskDescription task){

		double sum = 0.;
		for(JointPolicyStrategyProb jp : jointDistribution){
			if(jp.taskPrior.getTask() == task){
				sum += jp.prob;
			}
		}
		return sum;

	}

	/**
	 * Computes marginal for teh given feedback strategy. A feedback strategy in the joint distribution is considered equal
	 * to the query strategy if tis point address is the same
	 * @param jointDistribution
	 * @param fs
	 * @return
	 */
	public static double marginalizeForStrategy(List<JointPolicyStrategyProb> jointDistribution, FeedbackStrategy fs){
		double sum = 0.;
		for(JointPolicyStrategyProb jp : jointDistribution){
			if(jp.feedbackPrior.getStrategy() == fs){
				sum += jp.prob;
			}
		}
		return sum;
	}
}
