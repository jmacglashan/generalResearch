package behavior.training.act;

import behavior.training.TrainerModel;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.actorcritic.Actor;
import burlap.behavior.singleagent.learning.actorcritic.ActorCritic;
import burlap.behavior.singleagent.learning.actorcritic.CritiqueResult;
import burlap.behavior.singleagent.learning.actorcritic.critics.TimeIndexedTDLambda;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class ActorCriticTrainer extends ActorCritic {

	protected TrainerModel			trainerModel;
	protected TimeIndexedRF			modeledRF;
	
	public ActorCriticTrainer(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, Actor actor, TimeIndexedTDLambda critic, TrainerModel trainerModel) {
		super(domain, rf, tf, gamma, actor, critic);
		this.trainerModel = trainerModel;
		this.modeledRF = (TimeIndexedRF)this.trainerModel.getTrainerRewardFunction();
		critic.setRewardFunction(this.modeledRF);
	}

	public ActorCriticTrainer(Domain domain, RewardFunction rf,
			TerminalFunction tf, double gamma, Actor actor, TimeIndexedTDLambda critic, TrainerModel trainerModel,
			int maxEpisodeSize) {
		super(domain, rf, tf, gamma, actor, critic, maxEpisodeSize);
		
		this.trainerModel = trainerModel;
		this.modeledRF = (TimeIndexedRF)this.trainerModel.getTrainerRewardFunction();
		critic.setRewardFunction(this.modeledRF);
		
	}
	
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		
		EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		
		State curState = initialState;
		
		this.critic.initializeEpisode(curState);
		
		int timeSteps = 0;
		while(!tf.isTerminal(curState) && timeSteps < this.maxEpisodeSize){
			
			this.modeledRF.setCurTime(timeSteps);
			
			GroundedAction ga = this.actor.getAction(curState);
			State nextState = ga.executeIn(curState);
			double trainerActualReward = this.rf.reward(curState, ga, nextState);
			
			this.trainerModel.updateWithFeedback(timeSteps, trainerActualReward, curState, ga, nextState);
			
			ea.recordTransitionTo(nextState, ga, trainerActualReward);
			
			CritiqueResult critqiue = this.critic.critiqueAndUpdate(curState, ga, nextState);
			this.actor.updateFromCritqique(critqiue);
			
			curState = nextState;
			timeSteps++;
			
		}
		
		this.critic.endEpisode();
		
		this.updateVFWithStableRewardModel(initialState, 10);
		
		if(episodeHistory.size() >= numEpisodesToStore){
			episodeHistory.poll();
		}
		episodeHistory.offer(ea);
		
		return ea;
	}
	
	
	
	public void updateVFWithStableRewardModel(State initialState, int nPasses){
		
		for(int i = 0; i < nPasses; i++){
			int us = this.runVFPass(initialState);
			if(us == 1){
				i = -1; //restart passes
			}
		}
		
	}
	
	
	public int runVFPass(State initialState){
		
		int modelUpdateState = 0;
		
		State curState = initialState;
		
		this.critic.initializeEpisode(curState);
		
		int timeSteps = 0;
		while(!tf.isTerminal(curState) && timeSteps < this.maxEpisodeSize){
			
			this.modeledRF.setCurTime(timeSteps);
			
			GroundedAction ga = this.actor.getAction(curState);
			State nextState = ga.executeIn(curState);
			
			int mu = this.trainerModel.updateWithoutFeedback(timeSteps, curState, ga, nextState);
			if(mu != 0){
				modelUpdateState = mu;
			}
			
			this.critic.critiqueAndUpdate(curState, ga, nextState);
			
			curState = nextState;
			timeSteps++;
			
		}
		
		this.critic.endEpisode();
		
		return modelUpdateState;
		
	}

}
