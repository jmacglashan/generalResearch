package behavior.training.experiments.interactive.soko;

import java.util.ArrayList;
import java.util.List;

import behavior.planning.DeterministicGoalDirectedPartialVI;
import behavior.planning.DynamicDVIPolicy;
import behavior.training.taskinduction.MixtureModelPolicy;
import behavior.training.taskinduction.NoopOnTermPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.StratAwareTaskPosterior;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.DPrint;
import burlap.debugtools.MyTimer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class DynamicPlanISABL extends TaskInductionWithFeedbackStrategies {

	protected PolicyGenerator policyGenerator;
	
	public DynamicPlanISABL(Domain domain,
			RewardFunction rf, TerminalFunction tf,
			StateHashFactory hashingFactory, List<TaskDescription> tasks) {
		super(domain, rf, tf, hashingFactory, tasks);
		strategies = new ArrayList<FeedbackStrategy>();
		this.initPolicyGenerator();
		
	}
	
	public DynamicPlanISABL(Domain domain, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory, List <TaskDescription> tasks, MixtureModelPolicy policy){
		super(domain, rf, tf, hashingFactory, tasks, policy);
		strategies = new ArrayList<FeedbackStrategy>();
		this.initPolicyGenerator();
	}
	
	protected void initPolicyGenerator(){
		//this.policyGenerator = new DDVIPG();
		this.policyGenerator = new SokoAStarPlanner();
	}
	
	public void planPossibleTasksFromSeedState(State s){
		List <TaskProb> taskProbs = new ArrayList<TaskProb>(possibleTasks.size());
		
		//silence debug printing
		DPrint.toggleCode(10, false);
		DPrint.toggleCode(11, false);
		
		DPrint.cl(8473, "Starting Planning");
		
		MyTimer timer = new MyTimer();
		timer.start();
		for(int i = 0; i < this.possibleTasks.size(); i++){
			TaskDescription td = this.possibleTasks.get(i);
			double prior = this.priorsToUse.get(i);
			//ValueIteration planner = new ValueIteration(planningDomain, td.rf, td.tf, 0.99, hashingFactory, 0.001, 100);
			/*DeterministicGoalDirectedPartialVI planner = new DeterministicGoalDirectedPartialVI(planningDomain, td.rf, td.tf, 0.99, hashingFactory);
			planner.planFromState(s);
			Policy p = new NoopOnTermPolicy(noopAction, td.tf, new DynamicDVIPolicy(planner, 0.002));*/
			Policy p = new NoopOnTermPolicy(noopAction, td.tf, this.policyGenerator.getPolicy(planningDomain, s, td.rf, td.tf, hashingFactory));
			taskProbs.add(new TaskProb(td, p, prior));
			DPrint.cl(8473, "Planned for task: " + i + " {" + td.tf.toString() + "}");
		}
		timer.stop();
		DPrint.cl(8473, "Finished planning. Total initial planning time: " + timer.getTime());
		
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
	
}
