package behavior.training.pit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.learning.tdmethods.QLearningStateNode;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class QTDLam {

	protected RewardFunction										rf;
	protected TerminalFunction										tf;
	protected double												gamma;
	protected StateHashFactory										hashingFactory;
	protected double												learningRate;
	protected double												qInit;
	protected double												lambda;
	protected Map<StateHashTuple, QLearningStateNode>				qIndex;
	protected List<Action>											actions;
	LinkedList<EligibilityTrace> 									traces;
	
	
	
	public QTDLam(RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double learningRate, double qinit, double lambda) {
		
		this.rf = rf;
		this.tf = tf;
		this.gamma = gamma;
		this.hashingFactory = hashingFactory;
		
		this.learningRate = learningRate;
		this.qInit = qinit;
		this.lambda = lambda;
		
		this.qIndex = new HashMap<StateHashTuple, QLearningStateNode>();
		
		this.actions = new ArrayList<Action>();
		
		
	}
	
	public QTDLam(Domain d, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double learningRate, double qinit, double lambda) {
		
		this.rf = rf;
		this.tf = tf;
		this.gamma = gamma;
		this.hashingFactory = hashingFactory;
		
		this.learningRate = learningRate;
		this.qInit = qinit;
		this.lambda = lambda;
		
		this.qIndex = new HashMap<StateHashTuple, QLearningStateNode>();
		
		this.actions = new ArrayList<Action>();
		this.addActions(d.getActions());
		
		
	}
	
	public void addActions(List <Action> actions){
		for(Action a : actions){
			this.actions.add(a);
		}
	}
	
	
	public void initializeEpisodeInState(State initialState){
		traces = new LinkedList<QTDLam.EligibilityTrace>();
	}
	
	public void endEpisode(){
		
	}
	
	public void updateQValues(State s, GroundedAction ga, State sprime, GroundedAction nextAction){
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		QValue curQ = this.getQ(s, ga);
		
		
		double nextQ = 0.;
		if(!this.tf.isTerminal(sprime)){
			nextQ = this.getQ(sprime, nextAction).q;
		}
		
		double r = 0.;
		
		double discount = this.gamma;
		if(ga.action.isPrimitive()){
			r = rf.reward(s, ga, sprime);
		}
		else{
			Option o = (Option)ga.action;
			r = o.getLastCumulativeReward();
			int n = o.getLastNumSteps();
			discount = Math.pow(this.gamma, n);
		}
		
		
		
		//delta
		double delta = r + (discount * nextQ) - curQ.q;
		
		//update all
		boolean foundCurrentQTrace = false;
		for(EligibilityTrace et : traces){
			
			if(et.sh.equals(sh)){
				if(et.q.a.equals(ga)){
					foundCurrentQTrace = true;
					et.eligibility = 1.; //replacing traces
				}
				else{
					et.eligibility = 0.; //replacing traces
				}
			}
			
			et.q.q = et.q.q + (learningRate * et.eligibility * delta);
			et.eligibility = et.eligibility * lambda * discount;
			
			
			
		}
		
		if(!foundCurrentQTrace){
			//then update and add it
			curQ.q = curQ.q + (learningRate * delta);
			EligibilityTrace et = new EligibilityTrace(this.hashingFactory.hashState(s), curQ, lambda*discount);
			
			traces.add(et);

		}
		
	}
	
	
	public List<QValue> getQs(State s) {
		return this.getQs(this.hashingFactory.hashState(s));
	}
	
	protected List<QValue> getQs(StateHashTuple s) {
		QLearningStateNode node = this.getStateNode(s);
		return node.qEntry;
	}
	
	protected QValue getQ(State s, GroundedAction a) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		return this.getQ(sh, a);
		
	}
	
	protected QValue getQ(StateHashTuple s, GroundedAction a) {
		QLearningStateNode node = this.getStateNode(s);
		
		if(a.params.length > 0){
			Map<String, String> matching = s.s.getObjectMatchingTo(node.s.s, false);
			a = this.translateAction(a, matching);
		}
		
		for(QValue qv : node.qEntry){
			if(qv.a.equals(a)){
				return qv;
			}
		}
		
		return null; //no action for this state indexed / raise problem
	}
	
	protected QLearningStateNode getStateNode(StateHashTuple s){
		
		QLearningStateNode node = qIndex.get(s);
		
		if(node == null){
			node = new QLearningStateNode(s);
			List<GroundedAction> gas = s.s.getAllGroundedActionsFor(this.actions);
			for(GroundedAction ga : gas){
				node.addQValue(ga, qInit);
			}
			
			qIndex.put(s, node);
		}
		
		return node;
		
	}
	
	
	protected GroundedAction translateAction(GroundedAction a, Map <String,String> matching){
		String [] newParams = new String[a.params.length];
		for(int i = 0; i < a.params.length; i++){
			newParams[i] = matching.get(a.params[i]);
		}
		return new GroundedAction(a.action, newParams);
	}
	
	
	public static class EligibilityTrace{
		
		public double					eligibility;
		public StateHashTuple			sh;
		public QValue					q;
		public double					initialQ;
		
		public EligibilityTrace(StateHashTuple sh, QValue q, double elgigbility){
			this.sh = sh;
			this.q = q;
			this.eligibility = elgigbility;
			this.initialQ = q.q;
		}
		
		
	}

}
