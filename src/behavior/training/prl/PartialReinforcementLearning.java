package behavior.training.prl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import behavior.training.MTPositiveSPNegativeSA;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class PartialReinforcementLearning extends OOMDPPlanner implements
		LearningAgent {

	
	protected PreferenceModifiablePolicy			policy;
	protected MTPositiveSPNegativeSA				trainerModel;
	
	protected Domain								planningDomain;
	
	protected LinkedList<EpisodeAnalysis>			episodeHistory;
	protected int									numEpisodesToStore;
	
	protected int									maxEpisodeSize = 2000;
	
	protected int									numPlanningPasses = 1;
	
	public PartialReinforcementLearning(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, 
			PreferenceModifiablePolicy prefPolicy) {
		
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		
		this.policy = prefPolicy;
		this.trainerModel = new MTPositiveSPNegativeSA(this.hashingFactory);
		this.planningDomain = domain;
		
		episodeHistory = new LinkedList<EpisodeAnalysis>();
		numEpisodesToStore = 1;
	}
	
	public void useSeperatePlanningDomain(Domain d){
		this.planningDomain = d;
	}
	
	public void setNumPlanningPasses(int numPlanningPasses){
		this.numPlanningPasses = numPlanningPasses;
	}

	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		return this.runLearningEpisodeFrom(initialState, maxEpisodeSize);
	}
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {

		EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		
		Set<StateHashTuple> filteredModledRewardStates = new HashSet<StateHashTuple>();
		
		State s0 = initialState;
		State curState = initialState;
		int timeSinceS0 = 0;

		
		int timeStep = 0;
		while(!this.tf.isTerminal(curState) && timeStep < maxSteps){
			
			GroundedAction ga = (GroundedAction)this.policy.getAction(curState);
			State nextState = ga.executeIn(curState);
			double trainerFeedback = this.rf.reward(curState, ga, nextState);
			ea.recordTransitionTo(nextState, ga, trainerFeedback);
			timeSinceS0++;
			
			StateHashTuple nextSH = this.hashingFactory.hashState(nextState);
			
			if(trainerFeedback < 0.){
				trainerModel.updateWithFeedback(0, trainerFeedback, curState, ga, nextState);
				this.policy.updateSinglePreferenceInDirection(curState, ga, trainerFeedback);
				s0 = nextState;
				timeSinceS0 = 0;
				
			}
			else if(trainerFeedback > 0.){
				trainerModel.updateWithFeedback(0, trainerFeedback, curState, ga, nextState);
				filteredModledRewardStates.add(nextSH);
			}
			else if(!filteredModledRewardStates.contains(nextSH)){
				//get modeled feedback instead and filter it from further use in this episode
				//trainerFeedback = this.trainerModel.reward(curState, ga, nextState);
				//filteredModledRewardStates.add(nextSH);
			}
			
			if(trainerFeedback > 0.){
				PlannerPolicy localPlannerAndPolicy = this.getPlannerForLocalGoalBasedMDP(nextState, timeSinceS0);
				localPlannerAndPolicy.planner.planFromState(s0);
				EpisodeAnalysis rollout = localPlannerAndPolicy.policy.evaluateBehavior(s0, localPlannerAndPolicy.planner.getRF(), localPlannerAndPolicy.planner.getTF());
				this.policy.updatePreferencesInEpisodeAccordingToExternalPolicy(rollout, localPlannerAndPolicy.policy);
				s0 = nextState;
				timeSinceS0 = 0;
			}
			
			curState = nextState;
			timeStep++;
			
		}
		
		if(episodeHistory.size() >= numEpisodesToStore){
			episodeHistory.poll();
		}
		episodeHistory.offer(ea);
		
		return ea;
	}
	
	
	protected PlannerPolicy getPlannerForLocalGoalBasedMDP(final State feedbackState, int exploreSize){
		
		AggregateRewardFunction localRF = new AggregateRewardFunction();
		localRF.addRF(trainerModel.getNegativeRewardFunction());
		localRF.addRF(new RewardFunction() {
			
			StateHashTuple goalSH = hashingFactory.hashState(feedbackState);
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				StateHashTuple sh = hashingFactory.hashState(sprime);
				if(goalSH.equals(sh)){
					return 2.; //using 2 for the goal will counter act a previously specified negative action leading to the goal state
				}
				return 0;
			}
		});
		
		TerminalFunction lcoalTF = new TerminalFunction() {
			
			StateHashTuple goalSH = hashingFactory.hashState(feedbackState);
			
			@Override
			public boolean isTerminal(State s) {
				StateHashTuple sh = hashingFactory.hashState(s);
				if(goalSH.equals(sh)){
					return true;
				}
				
				return false;
			}
		};
		
		
		SarsaLam planner = new SarsaLam(this.planningDomain, localRF, lcoalTF, 0.99, this.hashingFactory, 0.2, 0.1, 1000, 0.9);
		planner.setMaximumEpisodesForPlanning(200);
		planner.setMaxQChangeForPlanningTerminaiton(0.0001);
		Policy p = new BoltzmannQPolicy(planner, 0.002);
		//Policy p = new GreedyQPolicy(planner);
		
		return new PlannerPolicy(planner, p);
	}

	@Override
	public EpisodeAnalysis getLastLearningEpisode() {
		return episodeHistory.getLast();
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
		for(int i = 0; i < numPlanningPasses; i++){
			this.runLearningEpisodeFrom(initialState);
		}
	}
	
	
	
	class PlannerPolicy{
		
		public OOMDPPlanner planner;
		public Policy policy;
		
		public PlannerPolicy(OOMDPPlanner planner, Policy policy){
			this.planner = planner;
			this.policy = policy;
		}
		
	}



	@Override
	public void resetPlannerResults() {
		this.trainerModel = new MTPositiveSPNegativeSA(this.hashingFactory);
	}

}
