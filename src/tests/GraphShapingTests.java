package tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import behavior.shaping.protoshaping.ProtoFunction;
import behavior.shaping.protoshaping.ProtoShapedRF;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;



public class GraphShapingTests {

	protected Domain d;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		GraphShapingTests t = new GraphShapingTests();
		
		Policy opt = t.optimalPolicy();
		//RewardFunction trueRF = t.getRF();
		RewardFunction trueRF = new NullRewardFunction();
		double discount = 0.99;
		int maxSteps = 100;
		
		RewardFunction shapedRF = t.getNegativeProtoRF(discount);
		
		/*
		for(int i = 0; i < 6; i++){
			t.printReturn(i, opt, trueRF, discount, maxSteps);
		}
		
		System.out.println("--");
		
		
		for(int i = 0; i < 6; i++){
			t.printReturn(i, opt, shapedRF, discount, maxSteps);
		}
		
		System.out.println("--");
		
		Policy cycle = t.cyclePolicy();
		t.printReturn(1, cycle, shapedRF, discount, maxSteps);
		t.printReturn(2, cycle, shapedRF, discount, maxSteps);
		*/
		
		t.planAndEvaluate(shapedRF, new NodeTF(3));

	}
	
	
	public GraphShapingTests(){
		
		GraphDefinedDomain gdd = new GraphDefinedDomain(6);
		gdd.setTransition(0, 0, 1, 1.);
		gdd.setTransition(1, 0, 2, 1.);
		gdd.setTransition(2, 0, 3, 1.);
		
		gdd.setTransition(1, 1, 4, 1.);
		gdd.setTransition(2, 1, 5, 1.);
		
		gdd.setTransition(4, 0, 0, 1.);
		gdd.setTransition(5, 0, 1, 1.);
		
		
		d = gdd.generateDomain();
		
	}
	
	
	public void printReturn(int sn, Policy p, RewardFunction rf, double discount, int maxSteps){
		State s = GraphDefinedDomain.getState(d, sn);
		TerminalFunction tf = new NodeTF(3);
		System.out.println(sn + ": " + p.evaluateBehavior(s, rf, tf, maxSteps).getDiscountedReturn(discount));
	}
	
	public void explore(){
		
		TerminalExplorer exp = new TerminalExplorer(d);
		exp.addActionShortHand("a", GraphDefinedDomain.BASEACTIONNAME+0);
		exp.addActionShortHand("s", GraphDefinedDomain.BASEACTIONNAME+1);
		
		State s = GraphDefinedDomain.getState(d, 0);
		
		exp.exploreFromState(s);
		
	}
	
	
	public void planAndEvaluate(RewardFunction rf, TerminalFunction tf){
		
		ValueIteration vi = new ValueIteration(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 0.001, 100);
		State initialState = GraphDefinedDomain.getState(d, 0);
		vi.planFromState(initialState);
		Policy p = new GreedyQPolicy(vi);
		
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rf, tf, 20);
		
		System.out.println(ea.getActionSequenceString());
		
	}
	
	
	public RewardFunction getRF(){
		return new GoalBasedRF(new NodeSC(3));
	}
	
	
	public RewardFunction getNegativeShapedProtoRF(double discount){
		RewardFunction baseRF = this.getRF();
		
		HardCodedProto hcp = new HardCodedProto();
		hcp.setPV(0, -3);
		hcp.setPV(1, -2);
		hcp.setPV(2, -1);
		hcp.setPV(3, 0);
		hcp.setPV(4, -4);
		hcp.setPV(5, -3);
		
		RewardFunction shaped = new ProtoShapedRF(baseRF, hcp, discount);
		
		return shaped;
		
		
	}
	
	
	public RewardFunction getNegativeProtoRF(double discount){
		RewardFunction baseRF = new NullRewardFunction();
		
		HardCodedProto hcp = new HardCodedProto();
		hcp.setPV(0, -3);
		hcp.setPV(1, -2);
		hcp.setPV(2, -1);
		hcp.setPV(3, 0);
		hcp.setPV(4, -4);
		hcp.setPV(5, -3);
		
		RewardFunction shaped = new ProtoShapedRF(baseRF, hcp, discount);
		
		return shaped;
		
		
	}
	
	public RewardFunction getPositiveShapedProtoRF(double discount){
		return null;
	}
	
	public Policy optimalPolicy(){
		HardCodedGraphPolicy p = new HardCodedGraphPolicy();
		
		p.setPolicy(0, 0);
		p.setPolicy(1, 0);
		p.setPolicy(2, 0);
		p.setPolicy(3, 0);
		p.setPolicy(4, 0);
		p.setPolicy(5, 0);
		
		return p;
	}
	
	public Policy cyclePolicy(){
		HardCodedGraphPolicy p = new HardCodedGraphPolicy();
		
		p.setPolicy(0, 0);
		p.setPolicy(1, 1);
		p.setPolicy(2, 1);
		p.setPolicy(3, 0);
		p.setPolicy(4, 0);
		p.setPolicy(5, 0);
		
		return p;
	}
	
	
	
	public static int stateNode(State s){
		ObjectInstance o = s.getObjectsOfClass(GraphDefinedDomain.CLASSAGENT).get(0);
		return o.getIntValForAttribute(GraphDefinedDomain.ATTNODE);
	}
	
	
	
	public static class NodeTF implements TerminalFunction{

		int gNode;
		
		public NodeTF(int gNode){
			this.gNode = gNode;
		}
		
		@Override
		public boolean isTerminal(State s) {
			return stateNode(s) == this.gNode;
		}
		

	}
	
	public static class NodeSC implements StateConditionTest{

		int gNode;
		
		public NodeSC(int gNode){
			this.gNode = gNode;
		}

		@Override
		public boolean satisfies(State s) {
			return stateNode(s) == this.gNode;
		}


	}
	
	
	public class HardCodedGraphPolicy extends Policy{

		Map <Integer, Integer> policy;
		
		
		public HardCodedGraphPolicy(){
			this.policy = new HashMap<Integer, Integer>();
		}
		
		public void setPolicy(int n, int a){
			this.policy.put(n, a);
		}
		
		
		@Override
		public GroundedAction getAction(State s) {
			int n = stateNode(s);
			int a = this.policy.get(n);
			GroundedAction ga = new GroundedAction(d.getAction(GraphDefinedDomain.BASEACTIONNAME+a), "");
			
			return ga;
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			return this.getDeterministicPolicy(s);
		}

		@Override
		public boolean isStochastic() {
			return false;
		}
		
		@Override
		public boolean isDefinedFor(State s) {
			return true;
		}
		

	}
	
	
	public class HardCodedProto implements ProtoFunction{

		Map <Integer, Double> pvs;
		
		public HardCodedProto(){
			this.pvs = new HashMap<Integer, Double>();
		}
		
		public void setPV(int n, double v){
			this.pvs.put(n, v);
		}
		
		@Override
		public double protoValue(State s) {
			int n = stateNode(s);
			return this.pvs.get(n);
		}
		
		
	}
	

}
