package behavior.training.taskinduction;

import java.util.List;

import javax.management.RuntimeErrorException;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class MAPMixtureModelPolicy extends MixtureModelPolicy {

	public MAPMixtureModelPolicy() {
		super();
	}
	
	
	public MAPMixtureModelPolicy(TaskPosterior posteriors) {
		super(posteriors);
	}

	
	@Override
	public GroundedAction getAction(State s) {
		
		List <TaskProb> taskProbs = posteriors.getTaskProbs();
		
		double maxP = 0.;
		TaskProb maxTP = null;

		for(TaskProb tp : taskProbs){
			if(tp.prob > maxP){
				maxTP = tp;
				maxP = tp.prob;
			}
		}
		
		if(maxTP == null){
			System.out.println("Max p: " + maxP);
		}
		
		return maxTP.policy.getAction(s);
		
	}
	

}
