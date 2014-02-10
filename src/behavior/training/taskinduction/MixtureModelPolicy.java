package behavior.training.taskinduction;

import java.util.List;
import java.util.Random;

import javax.management.RuntimeErrorException;

import burlap.behavior.singleagent.Policy;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

public class MixtureModelPolicy extends Policy {

	protected TaskPosterior		posteriors;
	protected Random			rand;
	
	
	public MixtureModelPolicy(){
		this.rand = RandomFactory.getMapped(0);
	}
	
	public MixtureModelPolicy(TaskPosterior posteriors) {
		this.posteriors = posteriors;
		rand = RandomFactory.getMapped(0);
	}
	
	
	public void setPosteriors(TaskPosterior posteriors){
		this.posteriors = posteriors;
	}

	@Override
	public AbstractGroundedAction getAction(State s) {
		
		double sump = 0.;
		double roll = rand.nextDouble();
		List <TaskProb> taskProbs = posteriors.getTaskProbs();
		for(TaskProb tp : taskProbs){
			sump += tp.prob;
			if(roll < sump){
				//then sample this policy
				return tp.policy.getAction(s);
			}
		}
		
		throw new RuntimeErrorException(new Error("Policy mixture model probability did not sum to 1"));
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isStochastic() {
		return true;
	}
	
	@Override
	public boolean isDefinedFor(State s) {
		return true;
	}

}
