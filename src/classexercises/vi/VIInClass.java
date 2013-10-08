package classexercises.vi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class VIInClass extends OOMDPPlanner implements QComputablePlanner {

	protected Map<StateHashTuple, Double>				v; //a mapping (e.g., dictionary) from states to their value
	protected double									minDelta; //a minimum threshold until we're bored of VI iterations
	
	public VIInClass(Domain domain, RewardFunction rf, 
			TerminalFunction tf, double gamma, 
			StateHashFactory hashingFactory, double minDelta) {
		
		//calls the abstract super classes init method so that this object will know about the domain, reward function, terminal function, and hashing function
		this.PlannerInit(domain, rf, tf, gamma, hashingFactory);
		
		//initialize our values
		this.v = new HashMap<StateHashTuple, Double>();
		this.minDelta = minDelta;
		
	}

	

	@Override
	public List<QValue> getQs(State s) {
		
		//this method is required to be implemented by the QComputablePlanner interface
		//it asks us to return a Q-value for each action this planner can take in the provided state
		
		//first get a list of all the grounded actions that can be applied in this state
		List <GroundedAction> gas = s.getAllGroundedActionsFor(this.actions);
		
		//for each grounded action, get its q-value and add it to the list of q-values
		List <QValue> qs = new ArrayList<QValue>(gas.size());
		for(GroundedAction ga : gas){
			QValue q = this.getQ(s, ga);
			qs.add(q);
		}
		
		//return the list of q-values
		return qs;
	}

	@Override
	public QValue getQ(State s, GroundedAction a) {
		
		//this method is required to be implemented by the QComputablePlanner interface
		//it asks us to return the Q-value for the specified action
		
		//Q-values are computed using the Bellman operator
		//Q(s,a) = sum_s' T(s, a, s') * [R(s, a, s') + gamma * V(s')]
		
		
		
		double sum = 0.;
		
		//get the list of states to which the action can transition with non-zero probability
		//this list can be retrieved from the action object, which is a data member of our grounded action
		//the getTransition method requires the parameters passes to the action, which are specified
		//in a data member of the grounded action (params)
		List <TransitionProbability> transitions = a.action.getTransitions(s, a.params);
		
		//iterate through each possible transition and sum up the contribution
		for(TransitionProbability tp : transitions){
			
			//TransitionProbability objects are a tuple with two data members, a state (to which the action may transition)
			//and the probability of that state transition
			
			StateHashTuple sh = this.hashingFactory.hashState(tp.s);
			sum += tp.p * (this.rf.reward(s, a, tp.s) + this.gamma * this.v.get(sh)); //bellman operate in BURLAP form
			
		}
		
		//create a Q-value object holding our result (contains a reference to the state, the action, and the Q(s,a) value)
		QValue q = new QValue(s, a, sum);
		
		return q;
	}

	@Override
	public void planFromState(State initialState) {
		
		//first we need to find the set of reachable states from initial state if we haven't done so already
		StateHashTuple sh = this.hashingFactory.hashState(initialState);
		if(!this.v.containsKey(sh)){ //if our value function does not have an entry for our state, then we need to expand the state space
			this.expandStateSpace(initialState);
		}
		
		//run vi
		this.vi();
		

	}
	
	protected void vi(){
		
		double maxDelta;
		
		do{
			
			//Initialize a variable to keep track of the maximum change in the value function
			//across an entire sweep (or "iteration") of the state space.
			maxDelta = 0.;
			
			//iterate over the state-value pairs in the state space
			for(Map.Entry<StateHashTuple, Double> vpair : this.v.entrySet()){
				
				//get a state
				StateHashTuple sh = vpair.getKey();
				
				//if this state is a terminal state, then its value function should be zero.
				//since the value function is initialized to zero, we can just skip terminal states
				if(this.tf.isTerminal(sh.s)){
					continue ;
				}
				
				double curV = vpair.getValue(); //the old V-value
				double nextV = this.maxQ(sh.s); //the new V-value is just the maximum Q-value for the state
				double delta = Math.abs(curV - nextV); //what's the change in value?
				this.v.put(sh, nextV); //update our value function
				
				maxDelta = Math.max(delta, maxDelta); //update our maxchange if need be
				
			}
			
			
		}while(maxDelta > this.minDelta); //when the biggest change is smaller than a threshold we'll stop do VI
		
	}
	
	
	/**
	 * Will return the maximum Q-value in state s
	 * @param s the state to query
	 * @return the maximum Q-value
	 */
	protected double maxQ(State s){
		
		List <QValue> qs = this.getQs(s);
		double max = Double.NEGATIVE_INFINITY;
		for(QValue q : qs){
			max = Math.max(max, q.q);
		}
		
		return max;
		
	}
	
	/**
	 * Will find all states that are reachable from s and initialize their value function to 0.
	 * This method will do a breadth-first search like approach. The difference is that
	 * in node expansion, we follow all possible transitions from an action in a given state
	 * unlike typical BFS which is based on deterministic transition dynamics
	 * @param s the seed state from which to find all reachable state
	 */
	protected void expandStateSpace(State s){
		
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		
		//create a FIFO queue for our BFS-like search
		LinkedList<StateHashTuple> openQueue = new LinkedList<StateHashTuple>();
		
		//offer means "enqueue" in java; so enqueue our seed state
		openQueue.offer(sh);
		
		//keep searching until there is nothing left to search!
		while(openQueue.size() > 0){
			
			
			//poll means "dequeue" in java; it will remove the first element in the list
			StateHashTuple cur = openQueue.poll();
			
			//if we've already seen this state before, then we don't need to expand it.
			//continue will take us back to the start of the loop
			if(this.v.containsKey(cur)){
				continue ;
			}
			
			
			//if we get here then this is a new state; initialize the value function to zero
			this.v.put(cur, 0.);
			
			
			//get all the possible actions that can be applied in this state and iterate over them
			List <GroundedAction> gas = cur.s.getAllGroundedActionsFor(this.actions);
			for(GroundedAction ga : gas){
				
				//find all the possible transitions from applying this action in this state
				List <TransitionProbability> transitions = ga.action.getTransitions(cur.s, ga.params);
				for(TransitionProbability tp : transitions){
					
					StateHashTuple next = hashingFactory.hashState(tp.s);
					if(!this.v.containsKey(next)){
						openQueue.offer(next); //if this is an unseen state, add it to our open queue
					}
				}
			}
			
		}
		
		
	}

}
