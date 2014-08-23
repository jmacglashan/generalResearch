package behavior.planning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling.HashedHeightState;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.datastructures.HashIndexedHeap;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;


//shouldn't vmin and vmax be based on height in the tree?

public class FSSS extends OOMDPPlanner {

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
	
	protected double rMin;
	protected double rMax;
	
	
	/**
	 * The tree nodes indexed by state and height.
	 */
	protected Map<HashedHeightState, FSSSStateNode> nodesByHeight;
	
	
	/**
	 * The total number of pseudo-Bellman updates
	 */
	protected int numUpdates = 0;
	
	/**
	 * The debug code used for printing planning information.
	 */
	protected int debugCode = 7369430;
	
	
	protected int numClosed = 0;
	
	public FSSS(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, int h, int c, double rMax, double rMin){
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.h = h;
		this.c = c;
		this.nodesByHeight = new HashMap<SparseSampling.HashedHeightState, FSSS.FSSSStateNode>();
		
		this.rMin = rMin;
		this.rMax = rMax;
	}
	
	
	public GroundedAction getAction(State s){
		StateHashTuple sh = this.hashingFactory.hashState(s);
		HashedHeightState hhs = new HashedHeightState(sh, this.h);
		FSSSStateNode sn = this.nodesByHeight.get(hhs);
		if(sn == null){
			this.planFromState(s);
			sn = this.nodesByHeight.get(hhs);
		}
		return sn.maxActionDominates();
	}
	
	
	
	public State getStoredStateRepresentation(State s){
		return this.mapToStateIndex.get(this.hashingFactory.hashState(s)).s;
	}
	
	public int getNumberOfStateNodesCreated(){
		return this.nodesByHeight.size();
	}

	@Override
	public void planFromState(State initialState) {
		StateHashTuple sh = this.hashingFactory.hashState(initialState);
		HashedHeightState hhs = new HashedHeightState(sh, this.h);
		if(nodesByHeight.containsKey(hhs)){
			return; //no planning needed
		}
		
		DPrint.cl(this.debugCode, "Beginning Planning.");
		int oldUpdates = this.numUpdates;
		FSSSStateNode sn = this.getStateNode(sh, this.h);
		int nr = 0;
		while(sn.maxActionDominates() == null){
			int priorClosed = this.numClosed;
			//System.out.println("++++++++++++++++++++++++\nStarting rollout (" + nr + ")\n++++++++++++++++++++++++");
			sn.rollout();
			//System.out.println("Rollout " + nr + " closed " + (this.numClosed - priorClosed) + " for a total of " + this.numClosed + " closed.");
			nr++;
		}
		DPrint.cl(this.debugCode, "Finished Planning with " + (this.numUpdates - oldUpdates) + " value esitmates; for a cumulative total of: " + this.numUpdates);
		sn.maxActionDominates();
		
		//this.nodesByHeight.clear();
		//this.nodesByHeight.put(new HashedHeightState(sn.sh, sn.height), sn);
		
		this.mapToStateIndex.put(sh, sh);
		

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
		
		boolean visited = false;
		
		HashIndexedHeap<FSSSActionNode> actionsByUpper;
		HashIndexedHeap<FSSSActionNode> actionsByLower;
		
		public FSSSStateNode(StateHashTuple sh, int height){
			this.sh = sh;
			this.height = height;
			if(height < 1 || FSSS.this.tf.isTerminal(sh.s)){
				this.upperBound = this.lowerBound = 0.;
			}
			else{
				double gamma = FSSS.this.gamma;
				double gammaK = Math.pow(gamma, this.height);
				double discountedFuture = (1. - gammaK) / (1. - gamma);
				this.upperBound = FSSS.this.rMax * discountedFuture;
				this.lowerBound = FSSS.this.rMin * discountedFuture;
			}
		}
		
		
		public boolean closed(){
			return this.lowerBound==this.upperBound;
		}
		
		public GroundedAction maxActionDominates(){
			
			//first find the action nodes with the max upper bound
			//of those, find the action with the max lower bound
			//if the lower bound is the upper bound, done (closed case)
			//if the lower bound is greater than all other actions upper bound, done
			//planner should store this action; there are not meaningful Q-values to return so disable QComputeable planner implementation
			
			if(this.actionsByUpper == null){
				return null;
			}
			
			FSSSActionNode maxLowerOfUpperNode = null;
			double maxVal = Double.NEGATIVE_INFINITY;
			for(FSSSActionNode cand : this.actionsByUpper){
				if(cand.upperBound > maxVal){
					maxVal = cand.upperBound;
					maxLowerOfUpperNode = cand;
				}
				else if(cand.upperBound == maxVal){
					if(cand.lowerBound > maxLowerOfUpperNode.lowerBound){
						maxLowerOfUpperNode = cand;
					}
				}
			}
			
			if(maxLowerOfUpperNode.lowerBound == maxLowerOfUpperNode.upperBound){
				return maxLowerOfUpperNode.a;
			}
			
			
			
			for(FSSSActionNode o : this.actionsByUpper){
				if(maxLowerOfUpperNode == o){
					continue;
				}
				if(maxLowerOfUpperNode.lowerBound < o.upperBound){
					return null;
				}
			}
			
			return maxLowerOfUpperNode.a;
			
			
		}
		
		public void rollout(){
			
			if(this.closed()){
				if(this.height < 1 && !this.visited){
					this.visited = true;
					FSSS.this.numClosed++;
				}
				return;
			}
			
			if(this.actionsByUpper == null){
				this.initActions();
			}
			
			if(this.height == FSSS.this.h){
				//System.out.println("Entering:");
				//System.out.println(this.toString());
			}
			
		
			
			//select action with largest upper val
			FSSSActionNode a = this.actionsByUpper.peek();
			
			
			//select next state node given action with largest margin
			FSSSTransition stn = a.getMaxTransition();
			FSSSStateNode s = stn.node;
			
			//System.out.println("Selecting: " + a.a.toString());
			
			//recurse
			s.rollout();
			

			
			
			
			//update upper and lower Q-value for selected action
			double sumLowerA = 0.;
			double sumUpperA = 0.;
			int c = 0;
			for(FSSSTransition trans : a.samples){
				double discount = Math.pow(FSSS.this.gamma, this.height - trans.node.height);
				sumLowerA += trans.sumReward + (trans.numTransitions * discount*trans.node.lowerBound);
				sumUpperA += trans.sumReward + (trans.numTransitions * discount*trans.node.upperBound);
				c += trans.numTransitions;
			}
			a.lowerBound = sumLowerA / (double)c;
			a.upperBound = sumUpperA / (double)c;
			
			//refresh action position in heap
			this.actionsByLower.refreshPriority(a);
			this.actionsByUpper.refreshPriority(a);
			
			//update upper and lower for this state
			this.lowerBound = this.actionsByLower.peek().lowerBound;
			this.upperBound = this.actionsByUpper.peek().upperBound;
			
			
			FSSS.this.numUpdates++;
			
			if(this.height == FSSS.this.h){
				//System.out.println("Unrolling:");
				//System.out.println(this.toString());
			}
			
			if(this.closed()){
				FSSS.this.numClosed++;
			}
			
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
		
		
		@Override
		public String toString(){
			StringBuffer buf = new StringBuffer();
			buf.append("=================================\n");
			buf.append("Height: " + this.height + "\n");
			buf.append(this.sh.s.toString() + "\n");
			buf.append("Lower: " + this.lowerBound + "\n");
			buf.append("Upper: " + this.upperBound + "\n");
			for(FSSSActionNode a : this.actionsByUpper){
				buf.append(a.toString() + "\n");
			}
			buf.append("=================================\n");
			
			return buf.toString();
		}
		
		
		
	}
	
	
	protected class FSSSActionNode{
		
		GroundedAction a;
		double lowerBound;
		double upperBound;
		int height;
		
		List<FSSSTransition> samples;
		
		
		public FSSSActionNode(State sourceState, GroundedAction a, int height){
			this.a = a;
			this.height = height;
			double gamma = FSSS.this.gamma;
			double gammaK = Math.pow(gamma, this.height);
			double discountedFuture = (1. - gammaK) / (1. - gamma);
			this.upperBound = FSSS.this.rMax * discountedFuture;
			this.lowerBound = FSSS.this.rMin * discountedFuture;
			
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
			
			this.samples = new ArrayList<FSSS.FSSSTransition>(sampledTransitions.size());
			for(FSSSTransition trans : sampledTransitions.values()){
				this.samples.add(trans);
			}
			
			
			
		}
		
		@Override
		public String toString(){
			return "[" + this.a.toString() + ": " + this.lowerBound + ", " + this.upperBound + "]";
		}
		
		
		protected FSSSTransition getMaxTransition(){
			FSSSTransition maxTrans = null;
			double maxMargin = Double.NEGATIVE_INFINITY;
			for(FSSSTransition ft : this.samples){
				double margin = ft.node.upperBound - ft.node.lowerBound;
				if(margin > maxMargin){
					maxMargin = margin;
					maxTrans = ft;
				}
			}
			return maxTrans;
		}
		
		protected String heapStructure(){
			StringBuffer buf = new StringBuffer();
			for(FSSSTransition t : this.samples){
				double margin = t.node.upperBound-t.node.lowerBound;
				buf.append(margin).append(" ");
			}
			
			return buf.toString();
		}
		
		
	}
	
	protected class FSSSTransition{
		double sumReward = 0.;
		int numTransitions = 0;
		FSSSStateNode node;
		
		public FSSSTransition(FSSSStateNode stateNode){
			this.node = stateNode;
		}
		
		public void addTransition(double r){
			sumReward += r;
			numTransitions++;
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
