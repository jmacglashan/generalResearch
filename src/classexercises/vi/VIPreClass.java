package classexercises.vi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.*;


public class VIPreClass extends OOMDPPlanner implements QFunction {

	protected Map<StateHashTuple, Double>				v;
	protected double									minDelta;
	
	public VIPreClass(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double minDelta) {
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.v = new HashMap<StateHashTuple, Double>();
		this.minDelta = minDelta;
	}

	@Override
	public List<QValue> getQs(State s) {
		
		List <GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.actions, s);
		List <QValue> qs = new ArrayList<QValue>(gas.size());
		for(GroundedAction ga : gas){
			QValue q = this.getQ(s, ga);
			qs.add(q);
		}
		
		return qs;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		
		double sum = 0.;
		List<TransitionProbability> tps = ((GroundedAction)a).action.getTransitions(s, a.params);
		for(TransitionProbability tp : tps){
			StateHashTuple sh = this.hashingFactory.hashState(tp.s);
			double v = this.v.get(sh);
			double r = this.rf.reward(s, (GroundedAction)a, sh.s);
			sum += tp.p * (r + this.gamma*v);
			
		}
		
		QValue q = new QValue(s, a, sum);
		
		return q;
	}

	@Override
	public void planFromState(State initialState) {
		
		StateHashTuple sh = hashingFactory.hashState(initialState);
		
		if(!this.v.containsKey(sh)){
			this.expandStateSpace(sh);
		}
		
		//run VI
		this.vi();

	}
	
	
	protected void vi(){
		
		double maxDelta;
		do{
			
			maxDelta = 0.;
			
			for(Map.Entry<StateHashTuple, Double> vpair : this.v.entrySet()){
				
				StateHashTuple sh = vpair.getKey();
				
				if(this.tf.isTerminal(sh.s)){
					continue; //terminal states always have value of 0
				}
				
				
				double curv = vpair.getValue();
				double maxq = this.maxQ(sh.s);
				this.v.put(sh, maxq);
				
				
				double delta = Math.abs(maxq - curv);
				maxDelta = Math.max(delta, maxDelta);
				
			}

			
		}while(maxDelta > this.minDelta);
		
	}
	
	
	protected double maxQ(State s){
		List<QValue> qs = this.getQs(s);
		double maxq = Double.NEGATIVE_INFINITY;
		for(QValue q : qs){
			if(q.q > maxq){
				maxq = q.q;
			}
		}
		return maxq;
	}
	
	protected void expandStateSpace(StateHashTuple sh){
		
		
		LinkedList<StateHashTuple> openList = new LinkedList<StateHashTuple>();
		openList.offer(sh);
		
		
		while(openList.size() > 0){
			
			StateHashTuple cur = openList.poll();
			if(this.v.containsKey(cur)){
				continue;
			}
			
			this.v.put(cur, 0.);
			
			//expand; get all grounded actions in this state
			List <GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.actions, cur.s);
			for(GroundedAction ga : gas){
				//all possible outcomes
				List<TransitionProbability> tps = ga.action.getTransitions(cur.s, ga.params);
				for(TransitionProbability tp : tps){
					StateHashTuple next = this.hashingFactory.hashState(tp.s);
					if(!this.v.containsKey(next)){
						openList.offer(next);
					}
				}
			}
			
		}
		
		
		
		
	}

	@Override
	public void resetPlannerResults() {
		this.v.clear();
	}

}
