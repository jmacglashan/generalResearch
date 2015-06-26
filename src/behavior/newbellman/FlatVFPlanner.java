package behavior.newbellman;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.planning.ActionTransitions;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class FlatVFPlanner extends OOMDPPlanner implements QComputablePlanner {


	protected Map<StateHashTuple, VFNode> valueFunction;

	/**
	 * The value function initialization to use; defaulted to an initialization of 0 everywhere.
	 */
	protected ValueFunctionInitialization valueInitializer = new ValueFunctionInitialization.ConstantValueFunctionInitialization();

	/**
	 * Common init method for ValueFunction Planners. This will automatically call the OOMDPPLanner init method.
	 * @param domain the domain in which to plan
	 * @param rf the reward function
	 * @param tf the terminal state function
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factory
	 */
	public void VFPInit(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory){

		this.plannerInit(domain, rf, tf, gamma, hashingFactory);


		//this.valueFunction = new HashMap<StateHashTuple, Double>();



	}

	@Override
	public void planFromState(State initialState) {

	}

	@Override
	public void resetPlannerResults() {

	}

	@Override
	public List<QValue> getQs(State s) {
		return null;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		return null;
	}

	protected VFNode getVFNode(State s){
		StateHashTuple sh = this.hashingFactory.hashState(s);
		VFNode node = this.valueFunction.get(sh);
		if(node == null){
			node = new VFNode(this.valueInitializer.value(s), this.tf.isTerminal(s));
			this.valueFunction.put(sh, node);
		}
		return node;
	}

	public class VFNode{

		double v;
		boolean isTerminal;
		NodeActionTransitions [] actionTransitions;

		public VFNode(double initValue, boolean isTerminal){
			this.v = initValue;
			this.isTerminal = isTerminal;
		}

		public void initializeTransitions(State sourceState){

			List <GroundedAction> gas = FlatVFPlanner.this.getAllGroundedActions(sourceState);
			this.actionTransitions = new NodeActionTransitions[gas.size()];
			int ac = 0;
			for(GroundedAction ga : gas){
				boolean isOption = ga.action instanceof Option;
				double expectedR = 0.;
				if(isOption){
					expectedR = ((Option)ga.action).getExpectedRewards(sourceState, ga.params);
				}
				List<TransitionProbability> tps  = ga.action.getTransitions(sourceState, ga.params);
				NodeTransition [] transitions = new NodeTransition[tps.size()];
				int i = 0;
				for(TransitionProbability tp : tps){
					if(!isOption) {
						double r = FlatVFPlanner.this.rf.reward(sourceState, ga, tp.s);
						expectedR += r*tp.p;
					}
					transitions[i] = new NodeTransition(tp.p, FlatVFPlanner.this.getVFNode(tp.s));
					i++;
				}

				this.actionTransitions[ac] = new NodeActionTransitions(isOption, expectedR, transitions);

				ac++;
			}

		}

		public void bellmanBackup(){
			if(this.isTerminal){
				this.v = 0.;
				return;
			}
			if(this.actionTransitions == null){
				throw new RuntimeException("Need to initialize the transitions for this VFNode before performing a bellman backup");
			}
			double maxQ = Double.NEGATIVE_INFINITY;
			for(NodeActionTransitions nat : this.actionTransitions){
				maxQ = Math.max(maxQ, nat.computeQ());
			}
			this.v = maxQ;


		}

	}

	public class NodeActionTransitions {
		public boolean isOption;
		public double expectedReward;
		public NodeTransition[] transitions;

		public NodeActionTransitions(boolean isOption, double expectedReward, NodeTransition[] transitions) {
			this.isOption = isOption;
			this.expectedReward = expectedReward;
			this.transitions = transitions;
		}

		public double computeQ(){
			double q = expectedReward;
			double discount = isOption ? 1. : FlatVFPlanner.this.gamma;
			for(NodeTransition nt : this.transitions){
				q += discount*nt.prob*nt.nextStateNode.v;
			}
			return q;
		}
	}

	public static class NodeTransition{

		public double prob;
		public VFNode nextStateNode;


		public NodeTransition(double p, VFNode nextStateNode){

			this.prob = p;
			this.nextStateNode = nextStateNode;
		}

	}

}
