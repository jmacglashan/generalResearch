package commands.interactive;

import behavior.learning.Environment;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class InteractiveReplanAgent {

	protected Domain					planningDomain;
	protected Environment				actingEnvironment;
	protected StateHashFactory			hashingFactory;
	
	protected Policy					currentPolicy;
	
	
	public InteractiveReplanAgent(Domain planningDomain, Environment actingEnvironment, Policy defaultPolicy, StateHashFactory hashingFactory){
		
		this.planningDomain = planningDomain;
		this.actingEnvironment = actingEnvironment;
		
		this.currentPolicy = defaultPolicy;
		
		this.hashingFactory = hashingFactory;
		
	}
	
	
	
	
	public void changeTask(State initialPlanState, RewardFunction rf, TerminalFunction tf){
		
		ValueIteration vi = new ValueIteration(planningDomain, rf, tf, 0.99, hashingFactory, 0.001, 1000);
		vi.planFromState(initialPlanState);
		Policy nextPolicy = new GreedyQPolicy(vi);
		this.currentPolicy = nextPolicy;
		
	}
	
	
	
	
	public void actLoop(){
		
		while(true){
			State s = actingEnvironment.getCurState();
			AbstractGroundedAction ga = this.currentPolicy.getAction(s);
			this.actingEnvironment.executeAction(ga.actionName(), ga.params);
		}
	}
	
	
}
