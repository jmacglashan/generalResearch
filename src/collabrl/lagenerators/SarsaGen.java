package collabrl.lagenerators;

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.statehashing.StateHashFactory;
import collabrl.LearningAgentForTaskGenerator;
import collabrl.TaskAndTrain;

/**
 * @author James MacGlashan.
 */
public class SarsaGen implements LearningAgentForTaskGenerator {


	protected boolean pessimistic;
	protected double learningRate;
	protected double epsilon;
	protected double lambda;


	protected TaskAndTrain task;


	public SarsaGen(boolean pessimistic, double learningRate, double epsilon, double lambda){
		this.pessimistic = pessimistic;
		this.learningRate = learningRate;
		this.epsilon = epsilon;
		this.lambda = lambda;
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
		return "SARSA_"+qInit+"_"+learningRate+"_"+epsilon+"_"+lambda;
	}

	@Override
	public LearningAgent generateAgent() {

		StateHashFactory hashingFactory = TabularHashFactoryGen.getHashingFactory(this.task);

		double qinit = this.task.vMax;
		if(this.pessimistic){
			qinit = this.task.vMin;
		}

		SarsaLam sarsa = new SarsaLam(task.domain, task.rf, task.tf, task.discount, hashingFactory, qinit, learningRate, lambda);
		EpsilonGreedy p = new EpsilonGreedy(sarsa, epsilon);
		sarsa.setLearningPolicy(p);

		return sarsa;
	}
}
