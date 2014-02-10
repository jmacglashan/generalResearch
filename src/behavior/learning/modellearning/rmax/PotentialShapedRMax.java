package behavior.learning.modellearning.rmax;

import java.util.LinkedList;
import java.util.List;

import behavior.learning.modellearning.DomainMappedPolicy;
import behavior.learning.modellearning.Model;
import behavior.learning.modellearning.ModeledDoaminGenerator;
import behavior.learning.modellearning.modelplanners.ModelPlanner;
import behavior.learning.modellearning.modelplanners.VIModelPlanner;
import behavior.learning.modellearning.models.TabularModel;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.shaping.potential.PotentialFunction;
import burlap.behavior.singleagent.shaping.potential.PotentialShapedRF;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class PotentialShapedRMax extends OOMDPPlanner implements LearningAgent {

	protected Model								model;
	protected Domain							modeledDomain;
	protected RewardFunction					modeledRewardFunction;
	protected TerminalFunction					modeledTerminalFunction;
	
	protected ModelPlanner						modelPlanner;
	
	protected boolean							firstTimeRun = true;
	
	protected int								maxNumSteps = Integer.MAX_VALUE;
	
	
	/**
	 * the saved previous learning episodes
	 */
	protected LinkedList<EpisodeAnalysis>		episodeHistory = new LinkedList<EpisodeAnalysis>();
	
	/**
	 * The number of the most recent learning episodes to store.
	 */
	protected int								numEpisodesToStore = 1;
	
	
	public PotentialShapedRMax(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double maxReward, int nConfident,
			double maxVIDelta, int maxVIPasses){
		
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.model = new TabularModel(domain, hashingFactory, nConfident);
		
		ModeledDoaminGenerator mdg = new ModeledDoaminGenerator(domain, this.model, true);
		this.modeledDomain = mdg.generateDomain();
		
		this.modeledTerminalFunction = new PotentialShapedRMaxTerminal(this.model.getModelTF());
		this.modeledRewardFunction = new PotentialShapedRF(this.model.getModelRF(), new RMaxPotential(maxReward, gamma), gamma);
		
		this.modelPlanner = new VIModelPlanner(modeledDomain, modeledRewardFunction, modeledTerminalFunction, gamma, hashingFactory, maxVIDelta, maxVIPasses);
		
	}
	
	
	public PotentialShapedRMax(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, PotentialFunction potential, int nConfident,
			double maxVIDelta, int maxVIPasses){
		
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.model = new TabularModel(domain, hashingFactory, nConfident);
		
		ModeledDoaminGenerator mdg = new ModeledDoaminGenerator(domain, this.model, true);
		this.modeledDomain = mdg.generateDomain();
		
		this.modeledTerminalFunction = new PotentialShapedRMaxTerminal(this.model.getModelTF());
		this.modeledRewardFunction = new PotentialShapedRF(this.model.getModelRF(), potential, gamma);
		
		this.modelPlanner = new VIModelPlanner(modeledDomain, modeledRewardFunction, modeledTerminalFunction, gamma, hashingFactory, maxVIDelta, maxVIPasses);
		
	}
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState){
		return this.runLearningEpisodeFrom(initialState, maxNumSteps);
	}
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {
		
		if(this.firstTimeRun){
			this.modelPlanner.initializePlannerIn(initialState);
			this.firstTimeRun = false;
		}
		
		EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		
		DomainMappedPolicy policy = new DomainMappedPolicy(domain, this.modelPlanner.modelPlannedPolicy());
		
		State curState = initialState;
		int steps = 0;
		while(!this.tf.isTerminal(curState) && steps < maxSteps){
			
			GroundedAction ga = (GroundedAction)policy.getAction(curState);
			State nextState = ga.executeIn(curState);
			double r = this.rf.reward(curState, ga, nextState);
			
			ea.recordTransitionTo(nextState, ga, r);
			
			if(!this.model.transitionIsModeled(curState, ga)){
				this.model.updateModel(curState, ga, nextState, r, this.tf.isTerminal(nextState));
				if(this.model.transitionIsModeled(curState, ga)){
					this.modelPlanner.modelChanged(curState);
					policy = new DomainMappedPolicy(domain, this.modelPlanner.modelPlannedPolicy());
				}
			}
			
			
			curState = nextState;
			
			steps++;
		}
		
		if(episodeHistory.size() >= numEpisodesToStore){
			episodeHistory.poll();
		}
		episodeHistory.offer(ea);
		
		
		return ea;
	}

	@Override
	public EpisodeAnalysis getLastLearningEpisode() {
		return episodeHistory.getLast();
	}

	@Override
	public void setNumEpisodesToStore(int numEps) {
		if(numEps > 0){
			numEpisodesToStore = numEps;
		}
		else{
			numEpisodesToStore = 1;
		}
	}

	@Override
	public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
		return episodeHistory;
	}

	@Override
	public void planFromState(State initialState) {
		throw new RuntimeException("Model learning algorithms should not be used as planning algorithms.");
	}

	
	
	
	public class PotentialShapedRMaxTerminal implements TerminalFunction{

		TerminalFunction sourceModelTF;
		
		public PotentialShapedRMaxTerminal(TerminalFunction sourceModelTF){
			this.sourceModelTF = sourceModelTF;
		}
		
		@Override
		public boolean isTerminal(State s) {
			
			//RMaxStates are terminal states
			if(s.getObjectsOfTrueClass(ModeledDoaminGenerator.RMAXFICTIOUSSTATENAME).size() > 0){
				return true;
			}
			
			return this.sourceModelTF.isTerminal(s);
		}
			
		
	}
	
	
	public class RMaxPotential implements PotentialFunction{
		
		double vmax;
		
		public RMaxPotential(double rMax, double gamma){
			this.vmax = rMax / (1. - gamma);
		}

		@Override
		public double potentialValue(State s) {
			if(s.getObjectsOfTrueClass(ModeledDoaminGenerator.RMAXFICTIOUSSTATENAME).size() > 0){
				return this.vmax;
			}
			return 0;
		}
		
	}
	
	
}
