package collabrl.algevals;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.oomdp.core.State;
import collabrl.AlgEvaluator;
import collabrl.TaskAndTrain;
import sun.management.resources.agent;

/**
 * @author James MacGlashan.
 */
public class ReturnEval extends AlgEvaluator{

	public ReturnEval(){
		super();
	}

	public ReturnEval(int nTrials){
		super(nTrials);
	}

	@Override
	public double [] eval(TaskAndTrain task, LearningAgent agent) {

		int numStepsLeft = task.numTrainingSteps;
		State initialState = task.sg.generateState();
		double totalReturn = 0.;
		int c = 0;
		while(numStepsLeft > 0){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, numStepsLeft);
			//System.out.println("(" + c + ") Steps: " + ea.numTimeSteps());
			totalReturn += ea.getDiscountedReturn(1.);
			numStepsLeft -= (ea.numTimeSteps() - 1); //-1 to remove last step counted in which no action is taken
			c++;

			initialState = task.sg.generateState();
		}


		return new double[]{totalReturn};
	}

	@Override
	public int[] timeSteps(TaskAndTrain task) {
		return new int[]{task.numTrainingSteps};
	}
}
