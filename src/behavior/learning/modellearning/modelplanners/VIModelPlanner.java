package behavior.learning.modellearning.modelplanners;

import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class VIModelPlanner implements ModelPlanner {

	protected ValueIteration		vi;
	protected Policy				modelPolicy;
	
	protected State					initialState;
	
	
	protected Domain				domain;
	protected RewardFunction		rf;
	protected TerminalFunction		tf;
	protected double				gamma;
	protected StateHashFactory		hashingFactory;
	protected double				maxDelta;
	protected int					maxIterations;
	
	
	public VIModelPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double maxDelta, int maxIterations){
		
		this.domain = domain;
		this.rf = rf;
		this.tf = tf;
		this.gamma = gamma;
		this.hashingFactory = hashingFactory;
		this.maxDelta = maxDelta;
		this.maxIterations = maxIterations;
		
		vi = new ValueIteration(domain, rf, tf, gamma, hashingFactory, maxDelta, maxIterations);
		DPrint.toggleCode(vi.getDebugCode(), false);
		this.modelPolicy = new ReplanIfUnseenPolicy(new GreedyQPolicy(vi));
	}
	
	
	@Override
	public void initializePlannerIn(State s) {
		this.initialState = s;
	}

	@Override
	public void modelChanged(State changedState) {
		//this.vi.recomputeReachableStates();
		vi = new ValueIteration(domain, rf, tf, gamma, hashingFactory, maxDelta, maxIterations);
		this.modelPolicy = new ReplanIfUnseenPolicy(new GreedyQPolicy(vi));
		
		this.vi.planFromState(initialState);
		this.vi.planFromState(changedState);
	}

	@Override
	public Policy modelPlannedPolicy() {
		return modelPolicy;
	}
	
	class ReplanIfUnseenPolicy extends Policy{

		
		Policy p;
		
		public ReplanIfUnseenPolicy(Policy p){
			this.p = p;
		}
		
		@Override
		public AbstractGroundedAction getAction(State s) {
			if(!VIModelPlanner.this.vi.hasComputedValueFor(s)){
				VIModelPlanner.this.vi.planFromState(s);
			}
			return p.getAction(s);
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			
			if(!VIModelPlanner.this.vi.hasComputedValueFor(s)){
				VIModelPlanner.this.vi.planFromState(s);
			}
			return p.getActionDistributionForState(s);
		}

		@Override
		public boolean isStochastic() {
			return p.isStochastic();
		}

		@Override
		public boolean isDefinedFor(State s) {
			return p.isDefinedFor(s);
		}
		
		
		
		
	}
	

}
