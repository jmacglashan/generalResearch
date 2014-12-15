package behavior.training.taskinduction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TaskInductionTraining extends OOMDPPlanner implements
		LearningAgent {

	protected List <TaskDescription>				possibleTasks;
	protected TaskPosterior							posteriors = null;
	protected MixtureModelPolicy					policy;
	
	protected Domain								planningDomain;
	
	protected LinkedList<EpisodeAnalysis>			episodeHistory;
	protected int									numEpisodesToStore;
	
	protected int									maxEpisodeSize = 2000;
	
	protected List <Double>							priorsToUse;
	protected boolean								initializedPriors = false;
	
	protected Action								noopAction = null;
	
	public TaskInductionTraining(Domain domain, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory, List <TaskDescription> tasks) {
		
		this.plannerInit(domain, rf, tf, 1., hashingFactory);
		this.possibleTasks = tasks;
		
		this.priorsToUse = new ArrayList<Double>(tasks.size());
		double u = 1. / (double)tasks.size();
		for(int i = 0; i < tasks.size(); i++){
			priorsToUse.add(u);
		}
		
		episodeHistory = new LinkedList<EpisodeAnalysis>();
		this.policy = new MixtureModelPolicy();
		
	}
	
	public TaskInductionTraining(Domain domain, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory, List <TaskDescription> tasks, MixtureModelPolicy policy) {
		
		this.plannerInit(domain, rf, tf, 1., hashingFactory);
		this.possibleTasks = tasks;
		
		this.priorsToUse = new ArrayList<Double>(tasks.size());
		double u = 1. / (double)tasks.size();
		for(int i = 0; i < tasks.size(); i++){
			priorsToUse.add(u);
		}
		
		episodeHistory = new LinkedList<EpisodeAnalysis>();
		this.policy = policy;
		
	}
	
	public void useSeperatePlanningDomain(Domain d){
		this.planningDomain = d;
	}
	
	public void setNoopAction(Action noop){
		this.noopAction = noop;
	}

	
	public void setProbFor(int taskId, double p){
		if(!this.initializedPriors){
			this.priorsToUse.set(taskId, p);
		}
		else{
			this.posteriors.setProbFor(taskId, p);
		}
	}
	
	public void resetTasks(List<TaskDescription> tasks, List<Double> priors){
		this.possibleTasks = tasks;
		this.priorsToUse = priors;
	}
	
	public void setToUniform(){
		double uni = 1. / this.possibleTasks.size();
		for(int i = 0; i < this.possibleTasks.size(); i++){
			this.posteriors.setProbFor(i, uni);
		}
	}
	
	public List <TaskDescription> getTasks(){
		return new ArrayList<TaskDescription>(this.possibleTasks);
	}
	
	public TaskPosterior getPosteriors(){
		return this.posteriors;
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
		
			posteriors = new TaskPosterior(taskProbs, false);
			policy.setPosteriors(posteriors);
			
		}
		
		this.initializedPriors = true;
	}

	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		return this.runLearningEpisodeFrom(initialState, maxEpisodeSize);
	}
	
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {
		
		//EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		BeliefExtendedEA ea = new BeliefExtendedEA(initialState, new TaskBelief(this.getPosteriors()));
		
		State curState = initialState;
		int timeStep = 0;
		this.bookKeeping(curState, null, 0.);
		while(!this.tf.isTerminal(curState) && timeStep < maxSteps){
		//while(timeStep < this.maxEpisodeSize){
			GroundedAction ga = this.worldAction(curState, (GroundedAction)this.policy.getAction(curState));
			State nextState = ga.executeIn(curState);
			double trainerFeedback = this.rf.reward(curState, ga, nextState);
			//ea.recordTransitionTo(nextState, ga, trainerFeedback);
			
			this.posteriors.updateWithSingleStateFeedback(curState, ga, trainerFeedback);
			ea.recordTransitionTo(ga, nextState, trainerFeedback, new TaskBelief(this.getPosteriors()));

			
			this.bookKeeping(curState, ga, trainerFeedback);
			
			curState = nextState;
			timeStep++;
			
		}
		
		if(episodeHistory.size() >= numEpisodesToStore){
			episodeHistory.poll();
		}
		episodeHistory.offer(ea);
		
		return ea;
	}
	
	protected void bookKeeping(State s, GroundedAction a, double feedback){
		//do nothing
	}
	
	protected GroundedAction worldAction(State s, GroundedAction ga){
		List <GroundedAction> wgas = s.getAllGroundedActionsFor(this.actions);
		for(GroundedAction wga : wgas){
			if(wga.equals(ga)){
				return wga;
			}
		}
		return null;
	}

	@Override
	public EpisodeAnalysis getLastLearningEpisode() {
		return this.episodeHistory.getLast();
	}

	@Override
	public void setNumEpisodesToStore(int numEps) {
		this.numEpisodesToStore = numEps;
	}

	@Override
	public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
		return episodeHistory;
	}

	@Override
	public void planFromState(State initialState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetPlannerResults() {
		//nothing to do... to what priors should things be set?
	}
	
	
	
	
	
	
	

}
