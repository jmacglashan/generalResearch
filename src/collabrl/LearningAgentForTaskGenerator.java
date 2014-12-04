package collabrl;

import burlap.behavior.singleagent.learning.LearningAgentFactory;

/**
 * @author James MacGlashan.
 */
public interface LearningAgentForTaskGenerator extends LearningAgentFactory{
	public void initializeParams(TaskAndTrain task);
}
