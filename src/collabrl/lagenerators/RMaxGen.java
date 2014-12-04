package collabrl.lagenerators;

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.behavior.statehashing.StateHashFactory;
import collabrl.LearningAgentForTaskGenerator;
import collabrl.TaskAndTrain;

/**
 * @author James MacGlashan.
 */
public class RMaxGen implements LearningAgentForTaskGenerator {

	protected int c;


	protected TaskAndTrain task;


	public RMaxGen(int c){
		this.c = c;
	}

	@Override
	public void initializeParams(TaskAndTrain task) {
		this.task = task;
	}

	@Override
	public String getAgentName() {
		return "RMAX_"+this.c;
	}

	@Override
	public LearningAgent generateAgent() {

		StateHashFactory hashingFactory = TabularHashFactoryGen.getHashingFactory(this.task);

		PotentialShapedRMax.RMaxPotential potential = new PotentialShapedRMax.RMaxPotential(task.vMax);
		PotentialShapedRMax rmax = new PotentialShapedRMax(task.domain, task.rf, task.tf, task.discount, hashingFactory, potential, c, 0.01, 500);

		return rmax;
	}
}
