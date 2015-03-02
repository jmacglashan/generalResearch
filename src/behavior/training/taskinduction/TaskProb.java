package behavior.training.taskinduction;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class TaskProb {

	TaskDescription			task;
	Policy					policy;
	double 					prob;
	double					likelihood = 1.;
	
	
	public TaskProb(TaskDescription task, Policy policy){
		this.task = task;
		this.policy = policy;
		this.prob = 0.;
	}
	
	public TaskProb(TaskDescription task, Policy policy, double prob){
		this.task = task;
		this.policy = policy;
		this.prob = prob;
	}
	
	
	public TaskProb(TerminalFunction tf, RewardFunction rf, Policy policy){
		this.task = new TaskDescription(rf, tf);
		this.policy = policy;
		this.prob = 0.;
	}
	
	public TaskProb(TerminalFunction tf, RewardFunction rf, Policy policy, double prob){
		this.task = new TaskDescription(rf, tf);
		this.policy = policy;
		this.prob = prob;
	}
	

	public TerminalFunction getTf() {
		return task.tf;
	}

	public void setTf(TerminalFunction tf) {
		this.task.tf = tf;
	}

	public RewardFunction getRf() {
		return task.rf;
	}

	public void setRf(RewardFunction rf) {
		this.task.rf = rf;
	}

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	public double getProb() {
		return prob;
	}
	
	public double getLikilhood(){
		return this.likelihood;
	}

	public void setProb(double prob) {
		this.prob = prob;
	}
	
	public void setLiklihood(double l){
		this.likelihood = l;
	}
	
	public void setProbAndLiklihood(double prob, double l){
		this.prob = prob;
		this.likelihood = l;
	}

	public TaskDescription getTask(){
		return this.task;
	}


	@Override
	public String toString(){
		return this.prob + " " + this.task.toString();
	}

	
}
