package behavior.training.pit;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class TDPolicyIteration extends OOMDPPlanner implements QFunction{

	protected QTDLam						qSource;
	protected QModifiablePolicy				policy;
	
	
	public TDPolicyIteration() {
		
		
		
	}

	@Override
	public void planFromState(State initialState) {
		// TODO Auto-generated method stub
		
	}

	
	public void policyEvaluation(State initialState, int nPasses, int horizon){
		
		List <EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>(nPasses);
		for(int i = 0; i < nPasses; i++){
			EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
			
			qSource.initializeEpisodeInState(initialState);
			
			State curState = initialState;
			int timeStep = 0;
			
			GroundedAction curAction = (GroundedAction)this.policy.getAction(curState);
			
			
			while(!this.tf.isTerminal(curState) && timeStep < horizon){
				
				State nextState = curAction.executeIn(curState);
				double r = this.rf.reward(curState, curAction, nextState);
				ea.recordTransitionTo(nextState, curAction, r);
				
				GroundedAction nextAction = null;
				if(!tf.isTerminal(nextState)){
					nextAction = (GroundedAction)this.policy.getAction(nextState);
				}
				
				this.qSource.updateQValues(curState, curAction, nextState, nextAction);
				
			}
			
			episodes.add(ea);
			
			
			qSource.endEpisode();
		}
		
	}

	@Override
	public List<QValue> getQs(State s) {
		return this.qSource.getQs(s);
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		return this.qSource.getQ(s, (GroundedAction)a);
	}

	@Override
	public void resetPlannerResults() {
		this.qSource.reset();
	}

}
