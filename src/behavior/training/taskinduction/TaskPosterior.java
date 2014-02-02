package behavior.training.taskinduction;

import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class TaskPosterior {

	protected List<TaskProb>			tasks;
	
	
	public TaskPosterior(List<TaskProb> tasks) {
		this.tasks = tasks;
		//initialize to uniform
		double u = 1. / (tasks.size());
		for(TaskProb tp : tasks){
			tp.setProbAndLiklihood(u, u);
		}
	}
	
	public TaskPosterior(List<TaskProb> tasks, boolean initToUniform) {
		this.tasks = tasks;
		//initialize to uniform
		if(initToUniform){
			double u = 1. / (tasks.size());
			for(TaskProb tp : tasks){
				tp.setProbAndLiklihood(u, u);
			}
		}
	}
	
	public void resetTaskProbs(List<TaskProb> tasks){
		this.tasks = tasks;
	}
	
	public List <TaskProb> getTaskProbs(){
		return tasks;
	}
	
	public TaskProb getTaskProb(int i){
		return this.tasks.get(i);
	}
	
	public void setProbFor(int i, double p){
		this.tasks.get(i).setProbAndLiklihood(p, p);
	}
	
	public TaskProb getMostLikelyTask(){
		TaskProb mxTP = null;
		double mx = 0.;
		for(TaskProb tp : this.tasks){
			if(tp.prob > mx){
				mxTP = tp;
				mx = tp.prob;
			}
		}
		return mxTP;
	}
	
	
	public void updateWithSingleStateFeedback(State s, GroundedAction ga, double feedback){
		double [] liklihoodpriors = new double [tasks.size()];
		double marginal = 0.;
		for(int i = 0; i < tasks.size(); i++){
			TaskProb tp = tasks.get(i);
			double prior = tp.prob;
			double l = this.liklihood(s, ga, feedback, tp.policy);
			tp.setLiklihood(l*tp.getLikilhood());
			double lp = l*prior;
			liklihoodpriors[i] = lp;
			marginal += lp;
		}
		
		//now update poseterior
		for(int i = 0; i < tasks.size(); i++){
			TaskProb tp = tasks.get(i);
			tp.prob = liklihoodpriors[i] / marginal;
		}
	}
	
	
	
	protected double liklihood(State s, GroundedAction ga, double feedback, Policy p){
		
		List<ActionProb> pDist = p.getActionDistributionForState(s);
		double l = 0.;
		for(ActionProb ap : pDist){
			if(ap.ga.equals(ga)){
				l = ap.pSelection;
				break;
			}
		}
		
		if(feedback < 0.){
			return 1.-l;
		}
		else if(feedback > 0.){
			return l;
		}
		
		return 1.;
	}
	
	
	

}
