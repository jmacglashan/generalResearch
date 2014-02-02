package behavior.training.taskinduction.strataware;

import java.util.ArrayList;
import java.util.List;

import behavior.training.taskinduction.TaskPosterior;
import behavior.training.taskinduction.TaskProb;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class StratAwareTaskPosterior extends TaskPosterior {
	
	protected List<FeedbackStrategy>	strategies;
	
	public StratAwareTaskPosterior(List<TaskProb> tasks) {
		super(tasks);
		this.strategies = new ArrayList<FeedbackStrategy>();
	}
	
	public StratAwareTaskPosterior(List<TaskProb> tasks, boolean initToUniform) {
		super(tasks, initToUniform);
		this.strategies = new ArrayList<FeedbackStrategy>();
	}
	
	public void addFeedbackStrateg(FeedbackStrategy strat){
		this.strategies.add(strat);
	}
	
	public void setFeedbackStrategyProb(int i, double prob){
		this.strategies.get(i).setProbOfStrategy(prob);
	}
	
	public void setFeedbackStrategyProbsToUniform(){
		double uni = 1./this.strategies.size();
		for(FeedbackStrategy strat : this.strategies){
			strat.setProbOfStrategy(uni);
		}
	}
	
	public FeedbackStrategy getFeedbackStrategy(int i){
		return this.strategies.get(i);
	}
	
	public List<FeedbackStrategy> getFeedbackStrategies(){
		return this.strategies;
	}
	
	
	@Override
	public void updateWithSingleStateFeedback(State s, GroundedAction ga, double feedback){
		
		double normalizing = 0.;
		double [] tTerms = new double[this.tasks.size()];
		double [] tlTerms = new double[this.tasks.size()];
		double [] sTerms = new double[this.strategies.size()];
		
		for(int i = 0; i < tTerms.length; i++){
			tTerms[i] = 0.;
			tlTerms[i] = 0.;
		}
		for(int i = 0; i < sTerms.length; i++){
			sTerms[i] = 0.;
		}
		
		//compute
		for(int i = 0; i < this.strategies.size(); i++){
			FeedbackStrategy strat = this.strategies.get(i);
			double sp = strat.getProbOfStrategy();
			for(int j = 0; j < this.tasks.size(); j++){
				TaskProb tp = this.tasks.get(j);
				double l = strat.liklihood(s, ga, feedback, tp.getPolicy());
				double term = sp * tp.getProb() * l;
				normalizing += term;
				sTerms[i] += term;
				tTerms[j] += term;
				tlTerms[j] += l * sp;
			}
		}
		
		
		//update posterior
		for(int i = 0; i < sTerms.length; i++){
			double p = sTerms[i] / normalizing;
			this.strategies.get(i).setProbOfStrategy(p);
		}
		
		for(int i = 0; i < tTerms.length; i++){
			TaskProb tp = this.tasks.get(i);
			double p = tTerms[i] / normalizing;
			tp.setProbAndLiklihood(p, tlTerms[i]*tp.getLikilhood());
		}
		
	}
	

}
