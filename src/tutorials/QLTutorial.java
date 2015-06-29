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
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
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
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;

public class QLTutorial extends OOMDPPlanner implements QFunction,
		LearningAgent {

	protected Map<StateHashTuple, List<QValue>> qValues;
	protected ValueFunctionInitialization qinit;
	protected double learningRate;
	protected Policy learningPolicy;
	
	protected LinkedList<EpisodeAnalysis> storedEpisodes = new LinkedList<EpisodeAnalysis>();
	protected int maxStoredEpisodes = 1;
	
	
	public QLTutorial(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, 
			ValueFunctionInitialization qinit, double learningRate, double epsilon){
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.qinit = qinit;
		this.learningRate = learningRate;
		this.qValues = new HashMap<StateHashTuple, List<QValue>>();
		this.learningPolicy = new EpsilonGreedy(this, epsilon);
	}
	
	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		return this.runLearningEpisodeFrom(initialState, -1);
	}

	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState,
			int maxSteps) {
		
		//initialize our episode analysis object with the given initial state
		EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		
		//behave until a terminal state or max steps is reached
		State curState = initialState;
		int steps = 0;
		while(!this.tf.isTerminal(curState) && (steps < maxSteps || maxSteps == -1)){
			
			//select an action
			AbstractGroundedAction a = this.learningPolicy.getAction(curState);
			
			//take the action and observe outcome
			State nextState = a.executeIn(curState);
			double r = this.rf.reward(curState, (GroundedAction)a, nextState);
			
			//record result
			ea.recordTransitionTo((GroundedAction)a, nextState, r);
			
			//update the old Q-value
			QValue oldQ = this.getQ(curState, a);
			oldQ.q = oldQ.q + this.learningRate * (r + (this.gamma * this.maxQ(nextState) - oldQ.q));
			
			
			//move on to next state
			curState = nextState;
			steps++;
			
		}
		
		while(this.storedEpisodes.size() >= this.maxStoredEpisodes){
			this.storedEpisodes.poll();
		}
		this.storedEpisodes.offer(ea);
		
		return ea;
	}

	@Override
	public EpisodeAnalysis getLastLearningEpisode() {
		return this.storedEpisodes.getLast();
	}

	@Override
	public void setNumEpisodesToStore(int numEps) {
		this.maxStoredEpisodes = numEps;
		while(this.storedEpisodes.size() > this.maxStoredEpisodes){
			this.storedEpisodes.poll();
		}
	}

	@Override
	public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
		return this.storedEpisodes;
	}

	@Override
	public List<QValue> getQs(State s) {
		
		//first get hashed state
		StateHashTuple sh = this.hashingFactory.hashState(s);
		
		//check if we already have stored values
		List<QValue> qs = this.qValues.get(sh);
		
		//create and add initialized Q-values if we don't have them stored for this state
		if(qs == null){
			List<GroundedAction> actions = this.getAllGroundedActions(s);
			qs = new ArrayList<QValue>(actions.size());
			//create a Q-value for each action
			for(GroundedAction ga : actions){
				//add q with initialized value
				qs.add(new QValue(s, ga, this.qinit.qValue(s, ga)));
			}
			//store this for later
			this.qValues.put(sh, qs);
		}
		
		return qs;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		
		//first get all Q-values
		List<QValue> qs = this.getQs(s);
		
		//translate action parameters to source state for Q-values if needed
		a = a.translateParameters(s, qs.get(0).s);
		
		//iterate through stored Q-values to find a match for the input action
		for(QValue q : qs){
			if(q.a.equals(a)){
				return q;
			}
		}
		
		throw new RuntimeException("Could not find matching Q-value.");
	}
	
	protected double maxQ(State s){
		if(this.tf.isTerminal(s)){
			return 0.;
		}
		List<QValue> qs = this.getQs(s);
		double max = Double.NEGATIVE_INFINITY;
		for(QValue q : qs){
			max = Math.max(q.q, max);
		}
		return max;
	}

	@Override
	public void planFromState(State initialState) {
		throw new UnsupportedOperationException("We are not supporting planning for this tutorial.");
	}

	@Override
	public void resetPlannerResults() {
		this.qValues.clear();
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
		
		//setup Q-learning with 0.99 discount factor, discrete state hashing factory, a value
		//function initialization that initializes all Q-values to value 0, a learning rate
		//of 0.1 and an epsilon value of 0.1.
		QLTutorial ql = new QLTutorial(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(1.), 
				0.1, 0.1);
		
		//run learning for 1000 episodes
		for(int i = 0; i < 1000; i++){
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s);
			System.out.println("Episode " + i + " took " + ea.numTimeSteps() + " steps.");
		}
		
		//get the greedy policy from it
		Policy p = new GreedyQPolicy(ql);
		
		//evaluate the policy with one roll out and print out the action sequence
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf);
		System.out.println(ea.getActionSequenceString("\n"));
		
	}
	

}
