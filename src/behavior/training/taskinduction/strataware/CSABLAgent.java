package behavior.training.taskinduction.strataware;

import behavior.burlapirlext.DifferentiableSparseSampling;
import behavior.burlapirlext.ExplorationShapedRF;
import behavior.training.taskinduction.CSABL;
import behavior.training.taskinduction.FeedbackTuple;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.DomainMappedPolicy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;


import java.util.LinkedList;
import java.util.List;

public class CSABLAgent extends OOMDPPlanner implements LearningAgent {

	protected CSABL 		csabl;
	protected double 		learningRate;
	protected double		boltzBeta;
	protected Policy		controlPolicy;

	protected ExplorationShapedRF expRF = null;
	
	protected LinkedList<EpisodeAnalysis> allEpisodes;
	protected int maxNumEpisodesRemembered = 1;
	
	public CSABLAgent(Domain domain, Domain planningDomain, double gamma, double boltzBeta, DifferentiableRF learningRF, RewardFunction trainerRF, TerminalFunction trainerTF, StateHashFactory hashingFactory,
			double muPlus, double muMinus, double learningRate, int h, int c){
		
		this.plannerInit(domain, trainerRF, trainerTF, gamma, hashingFactory);
		this.csabl = new CSABL(learningRF, planningDomain, gamma, boltzBeta, hashingFactory, muPlus, muMinus);
		this.csabl.setPlanner(new DifferentiableSparseSampling(planningDomain, learningRF, new NullTermination(), gamma, hashingFactory, h, c, boltzBeta));
		this.learningRate = learningRate;
		this.boltzBeta = boltzBeta;
		
		this.allEpisodes = new LinkedList<EpisodeAnalysis>();

		this.controlPolicy = new DomainMappedPolicy(this.domain, new GreedyQPolicy(this.csabl.getPlanner()));
		
	}

	public void setExpRF(ExplorationShapedRF expRF){
		this.expRF = expRF;
	}

	public CSABL getCSABL(){
		return this.csabl;
	}

	/**
	 * Sets the policy in the planning domain space. The agent will wrap the provided policy in a mapper to the environment
	 * actions.
	 * @param p the source policy that defines control.
	 */
	public void setSourceDomainControlPolicy(Policy p){
		this.controlPolicy = new DomainMappedPolicy(this.domain, p);
	}
	
	public void giveAgentPlannerTerminalFunction(TerminalFunction tf){
		((OOMDPPlanner)this.csabl.getPlanner()).setTf(tf);
	}
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		return this.runLearningEpisodeFrom(initialState, Integer.MAX_VALUE);
	}

	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState,
			int maxSteps) {
		
		State curState = initialState;
		
		EpisodeAnalysis ea = new EpisodeAnalysis(curState);
		for(int i = 0; i < maxSteps && !this.tf.isTerminal(curState); i++){
			//Policy p = new DomainMappedPolicy(this.domain, new GreedyQPolicy(this.csabl.getPlanner()));
			//Policy p = new DomainMappedPolicy(this.domain, new BoltzmannQPolicy(this.csabl.getPlanner(), 1./this.boltzBeta));
			//System.out.println("About to choose action...");
			GroundedAction ga = (GroundedAction)this.controlPolicy.getAction(curState);
			System.out.println("Chose: " + ga.toString());
			State nextState = ga.executeIn(curState);
			double r = this.rf.reward(curState, ga, nextState);
			ea.recordTransitionTo(ga, nextState, r);
			FeedbackTuple feedback = new FeedbackTuple(curState, ga, r);
			this.csabl.stochasticGradientAscentOnInstance(feedback, this.learningRate);
			((OOMDPPlanner)this.csabl.getPlanner()).resetPlannerResults();
			curState = nextState;
			if(this.expRF != null){
				this.expRF.getVisitations().addVisits(nextState, 1.);
			}
		}
		
		if(this.allEpisodes.size() > this.maxNumEpisodesRemembered){
			this.allEpisodes.poll();
		}
		this.allEpisodes.offer(ea);
		
		return ea;
	}

	@Override
	public EpisodeAnalysis getLastLearningEpisode() {
		return this.allEpisodes.peekLast();
	}

	@Override
	public void setNumEpisodesToStore(int numEps) {
		this.maxNumEpisodesRemembered = numEps;
	}

	@Override
	public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
		return this.allEpisodes;
	}

	@Override
	public void planFromState(State initialState) {
		throw new RuntimeException("CSABLAgent is a strict learning agent and cannot plan from taks.");
	}

	@Override
	public void resetPlannerResults() {
		double [] rfParams = ((DifferentiableRF)this.rf).getParameters();
		for(int i = 0; i < rfParams.length; i++){
			rfParams[i] = 0.;
		}
		
	}

}
