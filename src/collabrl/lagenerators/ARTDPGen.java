package collabrl.lagenerators;

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.artdp.ARTDP;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import collabrl.LearningAgentForTaskGenerator;
import collabrl.TaskAndTrain;

/**
 * @author James MacGlashan.
 */
public class ARTDPGen implements LearningAgentForTaskGenerator{

	protected double temperature;

	protected TaskAndTrain task;


	public ARTDPGen(double temperature){
		this.temperature = temperature;
	}

	@Override
	public void initializeParams(TaskAndTrain task) {
		this.task = task;
	}

	@Override
	public String getAgentName() {
		return "ARTDP_" + this.temperature;
	}

	@Override
	public LearningAgent generateAgent() {

		StateHashFactory hashingFactory = TabularHashFactoryGen.getHashingFactory(this.task);

		ARTDP artdp = new ARTDP(task.domain, task.rf, task.tf, task.discount, hashingFactory, task.vMax);
		BoltzmannQPolicy p = new BoltzmannQPolicy(artdp, this.temperature);
		artdp.setPolicy(p);

		return artdp;
	}
}
