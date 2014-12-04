package collabrl.algevals;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.oomdp.core.State;
import collabrl.AlgEvaluator;
import collabrl.TaskAndTrain;

/**
 * @author James MacGlashan.
 */
public class MultiStepReturnEval extends AlgEvaluator{

	protected int numIntervals;

	public MultiStepReturnEval(int nTrials, int numIntervals) {
		super(nTrials);
		this.numIntervals = numIntervals;
	}


	@Override
	public double[] eval(TaskAndTrain task, LearningAgent agent) {

		int [] timeSteps = this.timeSteps(task);
		double [] perStepReturns = new double[timeSteps.length];
		double totalReturn = 0.;
		int numStepsLeft = task.numTrainingSteps;
		State initialState = task.sg.generateState();
		int c = 0;
		int nextTimeStep = 0;
		while(numStepsLeft > 0) {
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, numStepsLeft);
			numStepsLeft -= (ea.numTimeSteps() - 1); //-1 to remove last step counted in which no action is taken

			for(int j = 1; j < ea.numTimeSteps(); j++){
				totalReturn += ea.getReward(j);
				if(c + j == timeSteps[nextTimeStep]){
					perStepReturns[nextTimeStep] = totalReturn;
					nextTimeStep++;
				}
			}


			initialState = task.sg.generateState();
			c += ea.numTimeSteps()-1;

		}


		return perStepReturns;
	}

	@Override
	public int[] timeSteps(TaskAndTrain task) {

		int [] steps = new int[this.numIntervals];
		double slope = (double)task.numTrainingSteps / (double)this.numIntervals;
		for(int i = 1; i <= this.numIntervals; i++){
			if(i < this.numIntervals) {
				steps[i - 1] = (int) (i * slope);
			}
			else{
				steps[i-1] = task.numTrainingSteps;
			}
		}

		return steps;
	}
}
