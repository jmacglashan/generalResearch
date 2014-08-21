package behavior.planning;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling.HashedHeightState;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.datastructures.HashIndexedHeap;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class FSSS extends OOMDPPlanner implements QComputablePlanner {

	/**
	 * The height of the tree
	 */
	protected int h;
	
	/**
	 * The number of transition dynamics samples (for the root if depth-variable C is used)
	 */
	protected int c;
	
	/**
	 * Whether the number of transition dyanmic samples should scale with the depth of the node. Default is false.
	 */
	protected boolean useVariableC = false;
	
	/**
	 * Whether previous planning results should be forgetten or reused; default is reused (false).
	 */
	protected boolean forgetPreviousPlanResults = false;
	
	/**
	 * The state value used for leaf nodes; default is zero.
	 */
	protected ValueFunctionInitialization vinit;
	
	protected double vMin;
	
	
	/**
	 * The tree nodes indexed by state and height.
	 */
	protected Map<HashedHeightState, FSSSStateNode> nodesByHeight;
	
	/**
	 * The root state node Q-values that have been estimated by previous planning calls.
	 */
	protected Map<StateHashTuple, List<QValue>> rootLevelQValues;
	
	
	/**
	 * The total number of pseudo-Bellman updates
	 */
	protected int numUpdates = 0;
	
	/**
	 * The debug code used for printing planning information.
	 */
	protected int debugCode = 7369430;
	
	
	@Override
	public List<QValue> getQs(State s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void planFromState(State initialState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetPlannerResults() {
		// TODO Auto-generated method stub

	}
	
	
	
	/**
	 * Returns the value of C for a node at the given height (height from a leaf node).
	 * @param height the height from a leaf node.
	 * @return the value of C to use.
	 */
	protected int getCAtHeight(int height){
		if(!this.useVariableC){
			return c;
		}
		
		//convert height from bottom to depth from root
		int d = this.h = height;
		int vc = (int) (c * Math.pow(this.gamma, 2*d));
		if(vc == 0){
			vc = 1;
		}
		return vc;
	}
	
	/**
	 * Either returns, or creates, indexes, and returns, the state node for the given state at the given height in the tree
	 * @param sh the hased state
	 * @param height the height (distance from leaf node) of the node.
	 * @return the state node for the given state at the given height in the tree
	 */
	protected FSSSStateNode getStateNode(StateHashTuple sh, int height){
		HashedHeightState hhs = new HashedHeightState(sh, height);
		FSSSStateNode sn = this.nodesByHeight.get(hhs);
		if(sn == null){
			sn = new FSSSStateNode(sh, height);
			this.nodesByHeight.put(hhs, sn);
		}
		
		return sn;
	}
	
	
	protected class FSSSStateNode{
		
		StateHashTuple sh;
		double lowerBound;
		double upperBound;
		int height;
		
		HashIndexedHeap<FSSSActionNode> actionsByUpper;
		HashIndexedHeap<FSSSActionNode> actionsByLower;
		
		public FSSSStateNode(StateHashTuple sh, int height){
			this.sh = sh;
			this.height = height;
			if(height < 1){
				this.upperBound = this.lowerBound = 0.;
			}
			else{
				this.upperBound = FSSS.this.vinit.value(this.sh.s);
				this.lowerBound = FSSS.this.vMin;
			}
		}
		
		
		public boolean closed(){
			return this.lowerBound==this.upperBound;
		}
		
		public void rollout(){
			if(this.actionsByUpper == null){
				this.initActions();
			}
			
			if(this.closed()){
				return;
			}
			
			//select action with largest upper val
			
			//select next state node given action with largest margin
			
			//recurse
			
			//update upper and lower Q-value for selected action
			
			//update upper and lower for this state
		}
		
		public void initActions(){
			List<GroundedAction> gas = FSSS.this.getAllGroundedActions(this.sh.s);
			this.actionsByLower = new HashIndexedHeap<FSSS.FSSSActionNode>(new ActionLowerComparator(), gas.size());
			this.actionsByUpper = new HashIndexedHeap<FSSS.FSSSActionNode>(new ActionUpperComparator(), gas.size());
			for(GroundedAction ga : gas){
				FSSSActionNode node = new FSSSActionNode(this.sh.s, ga, this.height);
				this.actionsByLower.insert(node);
				this.actionsByUpper.insert(node);
			}
		}
		
	}
	
	
	protected class FSSSActionNode{
		
		GroundedAction a;
		double lowerBound;
		double upperBound;
		int height;
		
		HashIndexedHeap<FSSSTransition> samples;
		
		
		public FSSSActionNode(State sourceState, GroundedAction a, int height){
			this.a = a;
			this.height = height;
			this.upperBound = FSSS.this.vinit.value(sourceState);
			this.lowerBound = FSSS.this.vMin;
			
			int c = FSSS.this.getCAtHeight(height);
			Map<HashedHeightState, FSSSTransition> sampledTransitions = new HashMap<SparseSampling.HashedHeightState, FSSS.FSSSTransition>(c);
			for(int i = 0; i < c; i++){
				
				//sample a state
				State ns = a.executeIn(sourceState);
				double r = FSSS.this.rf.reward(sourceState, a, ns);
				int k = 1;
				
				if(a.action instanceof Option){
					k = ((Option)a.action).getLastNumSteps();
				}
				HashedHeightState hhs = new HashedHeightState(FSSS.this.hashingFactory.hashState(ns), this.height-k);
				FSSSTransition trans = sampledTransitions.get(hhs);
				if(trans == null){
					FSSSStateNode sn = FSSS.this.getStateNode(hhs.sh, hhs.height);
					trans = new FSSSTransition(sn);
					sampledTransitions.put(hhs, trans);
				}
				
				trans.addTransition(r);
				
			}
			
			this.samples = new HashIndexedHeap<FSSS.FSSSTransition>(new TransitionMarginComparator(), sampledTransitions.size());
			for(FSSSTransition trans : sampledTransitions.values()){
				this.samples.insert(trans);
			}
			
		}
		
		
		
	}
	
	protected class FSSSTransition{
		List<Double> reward;
		FSSSStateNode node;
		
		public FSSSTransition(FSSSStateNode stateNode){
			this.node = stateNode;
			this.reward = new LinkedList<Double>();
		}
		
		public void addTransition(double r){
			this.reward.add(r);
		}
		
		@Override
		public int hashCode(){
			return node.height*31 + node.sh.hashCode();
		}
		
		@Override
		public boolean equals(Object other){
			return this==other; //ptr comparison good enough
		}
	}
	
	protected static class TransitionMarginComparator implements Comparator<FSSSTransition>{

		@Override
		public int compare(FSSSTransition o1, FSSSTransition o2) {
			double d1 = o1.node.upperBound - o1.node.lowerBound;
			double d2 = o2.node.upperBound - o2.node.lowerBound;
			
			if(d1 > d2){
				return 1;
			}
			if(d1 < d2){
				return -1;
			}
			return 0;
		}
		
	}
	
	
	protected static class ActionUpperComparator implements Comparator<FSSSActionNode>{

		@Override
		public int compare(FSSSActionNode o1, FSSSActionNode o2) {
			if(o1.upperBound > o2.upperBound){
				return 1;
			}
			else if(o1.upperBound < o2.upperBound){
				return -1;
			}
			return 0;
		}
		
	}
	
	protected static class ActionLowerComparator implements Comparator<FSSSActionNode>{

		@Override
		public int compare(FSSSActionNode o1, FSSSActionNode o2) {
			if(o1.lowerBound > o2.lowerBound){
				return 1;
			}
			else if(o1.lowerBound < o2.lowerBound){
				return -1;
			}
			return 0;
		}
		
	}

}
