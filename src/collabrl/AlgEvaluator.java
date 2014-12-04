package collabrl;

import burlap.behavior.singleagent.learning.LearningAgent;

/**
 * @author James MacGlashan.
 */
public abstract class AlgEvaluator {

	protected int numTrials = 1;

	public AlgEvaluator(){

	}

	public AlgEvaluator(int nTrials){
		this.numTrials = nTrials;
	}

	public double [] eval(TaskAndTrain task, LearningAgentForTaskGenerator agentGen){
		agentGen.initializeParams(task);
		double [] sum = null;
		for(int i = 0; i < this.numTrials; i++){
			LearningAgent agent = agentGen.generateAgent();
			double [] p = this.eval(task, agent);
			if(sum == null){
				sum = new double[p.length];
			}
			for(int j = 0; j < p.length; j++){
				sum[j] += p[j];
			}
		}
		for(int j = 0; j < sum.length; j++){
			sum[j] /= this.numTrials;
		}
		return sum;
	}


	public abstract double [] eval(TaskAndTrain task, LearningAgent agent);

	public abstract int [] timeSteps(TaskAndTrain task);

}
