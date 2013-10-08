package behavior.learning;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.actorcritic.Actor;
import burlap.behavior.singleagent.learning.actorcritic.ActorCritic;
import burlap.behavior.singleagent.learning.actorcritic.Critic;
import burlap.behavior.singleagent.learning.actorcritic.CritiqueResult;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;




public class MultiVFPassAC extends ActorCritic {

	protected int				nVFPasses;
	
	public MultiVFPassAC(Domain domain, RewardFunction rf, TerminalFunction tf,
			double gamma, Actor actor, Critic critic, int nVFPasses) {
		super(domain, rf, tf, gamma, actor, critic);
		this.nVFPasses = nVFPasses;
	}

	public MultiVFPassAC(Domain domain, RewardFunction rf, TerminalFunction tf,
			double gamma, Actor actor, Critic critic, int maxEpisodeSize, int nVFPasses) {
		super(domain, rf, tf, gamma, actor, critic, maxEpisodeSize);
		this.nVFPasses = nVFPasses;
	}
	
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		
		EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		
		State curState = initialState;
		
		this.critic.initializeEpisode(curState);
		
		int timeSteps = 0;
		while(!tf.isTerminal(curState) && timeSteps < this.maxEpisodeSize){
			
			GroundedAction ga = this.actor.getAction(curState);
			State nextState = ga.executeIn(curState);
			double r = this.rf.reward(curState, ga, nextState);
			
			ea.recordTransitionTo(nextState, ga, r);
			
			CritiqueResult critqiue = this.critic.critiqueAndUpdate(curState, ga, nextState);
			this.actor.updateFromCritqique(critqiue);
			
			curState = nextState;
			timeSteps++;
			
		}
		
		this.critic.endEpisode();
		
		this.runMultipleVFPasses(initialState, nVFPasses);
		
		if(episodeHistory.size() >= numEpisodesToStore){
			episodeHistory.poll();
		}
		episodeHistory.offer(ea);
		
		return ea;
	}
	
	public void runMultipleVFPasses(State initialState, int nPasses){
		for(int i = 0; i < nPasses; i++){
			this.runVFPass(initialState);
		}
	}
	
	public void runVFPass(State initialState){
		
		State curState = initialState;
		
		this.critic.initializeEpisode(curState);
		
		int timeSteps = 0;
		while(!tf.isTerminal(curState) && timeSteps < this.maxEpisodeSize){
			
			GroundedAction ga = this.actor.getAction(curState);
			State nextState = ga.executeIn(curState);
			
			this.critic.critiqueAndUpdate(curState, ga, nextState);
			
			curState = nextState;
			timeSteps++;
			
		}
		
		this.critic.endEpisode();
		
	}

}
