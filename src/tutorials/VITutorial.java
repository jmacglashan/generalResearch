package tutorials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;

public class VITutorial extends OOMDPPlanner implements QFunction {

	protected Map<StateHashTuple, Double> valueFunction;
	protected ValueFunctionInitialization vinit;
	protected int numIterations;

	
	public VITutorial(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, 
			StateHashFactory hashingFactory, ValueFunctionInitialization vinit, int numIterations){
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.vinit = vinit;
		this.numIterations = numIterations;
		this.valueFunction = new HashMap<StateHashTuple, Double>();
	}
	
	@Override
	public List<QValue> getQs(State s) {
		List<GroundedAction> applicableActions = this.getAllGroundedActions(s);
		List<QValue> qs = new ArrayList<QValue>(applicableActions.size());
		for(GroundedAction ga : applicableActions){
			qs.add(this.getQ(s, ga));
		}
		return qs;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		
		//type cast to the type we're using
		GroundedAction ga = (GroundedAction)a;
		
		//what are the possible outcomes?
		List<TransitionProbability> tps = ga.action.getTransitions(s, ga.params);
		
		//aggregate over each possible outcome
		double q = 0.;
		for(TransitionProbability tp : tps){
			//what is reward for this transition?
			double r = this.rf.reward(s, ga, tp.s);
			
			//what is the value for the next state?
			double vp = this.valueFunction.get(this.hashingFactory.hashState(tp.s));
			
			//add contribution weighted by transition probabiltiy and 
			//discounting the next state
			q += tp.p * (r + this.gamma * vp);
		}
		
		//create Q-value wrapper
		QValue qValue = new QValue(s, ga, q);
		
		return qValue;
	}
	
	protected double bellmanEquation(State s){
		
		if(this.tf.isTerminal(s)){
			return 0.;
		}
		
		List<QValue> qs = this.getQs(s);
		double maxQ = Double.NEGATIVE_INFINITY;
		for(QValue q : qs){
			maxQ = Math.max(maxQ, q.q);
		}
		return maxQ;
	}

	@Override
	public void planFromState(State initialState) {
		
		StateHashTuple hashedInitialState = this.hashingFactory.hashState(initialState);
		if(this.valueFunction.containsKey(hashedInitialState)){
			return; //already performed planning here!
		}
		
		//if the state is new, then find all reachable states from it first
		this.performReachabilityFrom(initialState);
		
		//now perform multiple iterations over the whole state space
		for(int i = 0; i < this.numIterations; i++){
			//iterate over each state
			for(StateHashTuple sh : this.valueFunction.keySet()){
				//update its value using the bellman equation
				this.valueFunction.put(sh, this.bellmanEquation(sh.s));
			}
		}

	}

	@Override
	public void resetPlannerResults() {
		this.valueFunction.clear();
	}
	
	
	public void performReachabilityFrom(State seedState){
		
		StateHashTuple hashedSeed = this.hashingFactory.hashState(seedState);
		
		//mark our seed state as seen and set its initial value function value
		this.valueFunction.put(hashedSeed, this.vinit.value(hashedSeed.s));
		
		LinkedList<StateHashTuple> open = new LinkedList<StateHashTuple>();
		open.offer(hashedSeed);
		
		while(open.size() > 0){
			
			//pop off a state and expand it
			StateHashTuple sh = open.poll();
			
			//which actions can be applied on this state?
			List<GroundedAction> appliactionActions = this.getAllGroundedActions(sh.s);
			
			//for each action...
			for(GroundedAction ga : appliactionActions){
				
				//what are the possible outcomes?
				List<TransitionProbability> tps = ga.action.getTransitions(sh.s, ga.params);
				
				//for each possible outcome...
				for(TransitionProbability tp : tps){
					
					//add previously unseed states to our open queue and 
					//set their initial value function
					StateHashTuple shp = this.hashingFactory.hashState(tp.s);
					if(!this.valueFunction.containsKey(shp)){
						this.valueFunction.put(shp, this.vinit.value(shp.s));
						open.offer(shp);
					}
					
				}
				
			}
			
			
		}
		
	}
	
	
	public static void main(String [] args){
		
		GridWorldDomain gwd = new GridWorldDomain(3, 3);
		gwd.setMapToFourRooms();
		
		//only go in intended directon 80% of the time
		gwd.setProbSucceedTransitionDynamics(0.8);
		
		Domain domain = gwd.generateDomain();
		
		//get initial state with agent in 0,0
		State s = GridWorldDomain.getOneAgentNoLocationState(domain);
		GridWorldDomain.setAgent(s, 0, 0);
		
		//all transitions return -1
		RewardFunction rf = new UniformCostRF();
		
		//terminate in top right corner
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		
		//setup vi with 0.99 discount factor, discrete state hashing factory, a value
		//function initialization that initializes all states to value 0, and which will
		//run for 30 iterations over the state space
		VITutorial vi = new VITutorial(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.0), 30);
		
		//run planning from our initial state
		vi.planFromState(s);
		
		
		//get the greedy policy from it
		Policy p = new GreedyQPolicy(vi);
		
		//evaluate the policy with one roll out and print out the action sequence
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf);
		System.out.println(ea.getActionSequenceString("\n"));
			
	}

}
