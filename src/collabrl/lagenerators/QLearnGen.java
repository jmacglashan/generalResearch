package collabrl.lagenerators;

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.statehashing.StateHashFactory;
import collabrl.LearningAgentForTaskGenerator;
import collabrl.TaskAndTrain;

/**
 * @author James MacGlashan.
 */
public class QLearnGen implements LearningAgentForTaskGenerator{

	protected boolean pessimistic;
	protected double learningRate;
	protected double epsilon;


	protected TaskAndTrain task;


	public QLearnGen(boolean pessimistic, double learningRate, double epsilon){
		this.pessimistic = pessimistic;
		this.learningRate = learningRate;
		this.epsilon = epsilon;
	}

	@Override
	public void initializeParams(TaskAndTrain task) {
		this.task = task;
	}

	@Override
	public String getAgentName() {
		String qInit = "pessimisstic";
		if(!this.pessimistic){
			qInit = "optimistic";
		}
		return "QL_"+qInit+"_"+learningRate+"_"+epsilon;
	}

	@Override
	public LearningAgent generateAgent() {

		StateHashFactory hashingFactory = TabularHashFactoryGen.getHashingFactory(this.task);

		double qinit = this.task.vMax;
		if(this.pessimistic){
			qinit = this.task.vMin;
		}


		QLearning ql = new QLearning(task.domain,task.rf,task.tf,task.discount,hashingFactory,qinit,this.learningRate);
		EpsilonGreedy p = new EpsilonGreedy(ql, this.epsilon);
		ql.setLearningPolicy(p);

		return ql;
	}
}
