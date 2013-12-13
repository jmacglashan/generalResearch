package behavior.training.experiments.interactive.soko;

import java.util.List;

import behavior.training.taskinduction.MixtureModelPolicy;
import behavior.training.taskinduction.TaskPosterior;
import behavior.training.taskinduction.TaskProb;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class MAPNOCompleteMMPolicy extends MixtureModelPolicy {

	Action noopAction;
	
	public MAPNOCompleteMMPolicy(Action noopAction) {
		super();
		this.noopAction = noopAction;
	}
	
	
	public MAPNOCompleteMMPolicy(TaskPosterior posteriors) {
		super(posteriors);
	}
	
	@Override
	public GroundedAction getAction(State s) {
		
		List <TaskProb> taskProbs = posteriors.getTaskProbs();
		
		double maxP = 0.;
		TaskProb maxTP = null;

		for(TaskProb tp : taskProbs){
			if(tp.getProb() > maxP){
				maxTP = tp;
				maxP = tp.getProb();
			}
		}
		
		if(maxTP.getTf().isTerminal(s)){
			return new GroundedAction(noopAction, "");
		}
		
		return maxTP.getPolicy().getAction(s);
		
	
	}
	
	
	
}
