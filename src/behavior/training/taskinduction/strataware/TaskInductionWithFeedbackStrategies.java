package behavior.training.taskinduction.strataware;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import behavior.training.taskinduction.MixtureModelPolicy;
import behavior.training.taskinduction.NoopOnTermPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskInductionTraining;
import behavior.training.taskinduction.TaskProb;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TaskInductionWithFeedbackStrategies extends TaskInductionTraining {

	protected List<FeedbackStrategy>			strategies;
	
	protected int								totalNumberOfSteps = 0;
	
	protected int								taskToTrack = -1;
	protected int								strategyToTrack = -1;
	
	protected BufferedWriter					output = null;
	
	public TaskInductionWithFeedbackStrategies(Domain domain,
			RewardFunction rf, TerminalFunction tf,
			StateHashFactory hashingFactory, List<TaskDescription> tasks) {
		super(domain, rf, tf, hashingFactory, tasks);
		strategies = new ArrayList<FeedbackStrategy>();
		
	}
	
	public TaskInductionWithFeedbackStrategies(Domain domain, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory, List <TaskDescription> tasks, MixtureModelPolicy policy){
		super(domain, rf, tf, hashingFactory, tasks, policy);
		strategies = new ArrayList<FeedbackStrategy>();
	}

	public void addFeedbackStrategy(FeedbackStrategy strat){
		this.strategies.add(strat);
	}
	
	public void setTaskToTrack(int i){
		this.taskToTrack = i;
	}
	
	public void setStrategyToTrack(int i){
		this.strategyToTrack = i;
	}
	
	public void setOutputPath(String path){
		try {
			this.output = new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setToUniform(){
		super.setToUniform();
		StratAwareTaskPosterior spost = (StratAwareTaskPosterior)this.posteriors;
		spost.setFeedbackStrategyProbsToUniform();
		
	}
	
	public void resetStepCount(){
		this.totalNumberOfSteps = 0;
	}
	
	public void closeOutput(){
		try {
			this.output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void planPossibleTasksFromSeedState(State s){
		List <TaskProb> taskProbs = new ArrayList<TaskProb>(possibleTasks.size());
		
		//silence debug printing
		DPrint.toggleCode(10, false);
		DPrint.toggleCode(11, false);
		
		DPrint.cl(8473, "Starting Planning");
		
		for(int i = 0; i < this.possibleTasks.size(); i++){
			TaskDescription td = this.possibleTasks.get(i);
			double prior = this.priorsToUse.get(i);
			ValueIteration planner = new ValueIteration(planningDomain, td.rf, td.tf, 0.99, hashingFactory, 0.001, 100);
			planner.planFromState(s);
			Policy p = new NoopOnTermPolicy(noopAction, td.tf, new BoltzmannQPolicy(planner, 0.002));
			taskProbs.add(new TaskProb(td, p, prior));
			DPrint.cl(8473, "Planned for task: " + i);
		}
		
		DPrint.cl(8473, "Finished planning");
		
		if(this.posteriors != null){
			this.posteriors.resetTaskProbs(taskProbs);
		}
		else{
			StratAwareTaskPosterior saposteriors = new StratAwareTaskPosterior(taskProbs, false);
			for(FeedbackStrategy strat : this.strategies){
				saposteriors.addFeedbackStrateg(strat);
			}
			saposteriors.setFeedbackStrategyProbsToUniform();
			
			posteriors = saposteriors;
			policy.setPosteriors(posteriors);
			
		}
			
		this.initializedPriors = true;
	}
	
	
	@Override
	protected void bookKeeping(State s, GroundedAction a, double feedback){
		
		if(this.taskToTrack == -1 && this.strategyToTrack == -1){
			return ; //nothing to do
		}
		
		if(a != null || totalNumberOfSteps == 0){
			//then it's not an episode restart star of an initial state
			
			String baseReport = this.totalNumberOfSteps + " " + feedback;
			String taskReport = "";
			String stratReport = "";
			
			if(this.taskToTrack != -1){
				
				TaskProb tp = this.posteriors.getTaskProb(this.taskToTrack);
				boolean uniqueMax = this.uniqueMaxTaskProb(tp, this.posteriors.getTaskProbs());
				taskReport = " " + tp.getProb() + " " + uniqueMax;
				
			}
			if(this.strategyToTrack != -1){
				StratAwareTaskPosterior sposteriors = (StratAwareTaskPosterior)this.posteriors;
				FeedbackStrategy fs = sposteriors.getFeedbackStrategy(this.strategyToTrack);
				boolean uniqueMax = this.uniqueMaxStratProb(fs, sposteriors.getFeedbackStrategies());
				stratReport = " " + fs.getProbOfStrategy() + " " + uniqueMax;
			}
			
			System.out.println(baseReport + taskReport + stratReport);
			if(this.output != null){
				try {
					output.write(baseReport + taskReport + stratReport + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			totalNumberOfSteps++;
		}
		
		
	}
	
	protected boolean uniqueMaxTaskProb(TaskProb tp, List<TaskProb> allTPs){
		double p = tp.getProb();
		for(TaskProb otp : allTPs){
			if(otp.getProb() > p || (otp.getProb() == p && otp != tp)){
				return false;
			}
		}
		return true;
	}
	
	protected boolean uniqueMaxStratProb(FeedbackStrategy fs, List<FeedbackStrategy> allFSs){
		double p = fs.getProbOfStrategy();
		for(FeedbackStrategy ofs : allFSs){
			if(ofs.getProbOfStrategy() > p || (ofs.getProbOfStrategy() == p && ofs != fs)){
				return false;
			}
		}
		return true;
	}
	
	
}
