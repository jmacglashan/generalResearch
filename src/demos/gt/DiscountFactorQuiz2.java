package demos.gt;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;


public class DiscountFactorQuiz2 {

	GraphDefinedDomain 			gdd;
	Domain 						domain;
	RewardFunction				rf;
	TerminalFunction			tf;
	State						initialState;
	DiscreteStateHashFactory	hashingFactory;



	public DiscountFactorQuiz2(double p1, double p2, double p3, double p4){

		this.gdd = new GraphDefinedDomain(6);

		/*
		a = 1
		b1 = 2
		b2 = 4
		c1 = 3
		c2 = 5
		 */

		//actions from initial state 0
		this.gdd.setTransition(0, 0, 1, 1.);
		this.gdd.setTransition(0, 1, 2, 1.);
		this.gdd.setTransition(0, 2, 3, 1.);

		//transitions from action "a" outcome state
		this.gdd.setTransition(1, 0, 1, 1.);

		//transitions from action "b" outcome state
		this.gdd.setTransition(2, 0, 4, 1.);
		this.gdd.setTransition(4, 0, 2, 1.);

		//transitions from action "c" outcome state
		this.gdd.setTransition(3, 0, 5, 1.);
		this.gdd.setTransition(5, 0, 5, 1.);

		this.domain = this.gdd.generateDomain();
		this.rf = new FourParamRF(p1, p2, p3, p4);
		this.tf = new NullTermination();
		this.initialState = GraphDefinedDomain.getState(this.domain, 0);
		this.hashingFactory = new DiscreteStateHashFactory();

	}


	public String bestFirstAction(double discountFactor){
		double delta = 0.00001;
		int maxIters = 1000;
		ValueIteration planner = new ValueIteration(domain, rf, tf, discountFactor, hashingFactory, delta, maxIters);
		planner.planFromState(initialState);
		Policy p = new GreedyQPolicy(planner);
		String actionName = p.getAction(this.initialState).actionName();

		System.out.println("v(i) = " + planner.value(GraphDefinedDomain.getState(domain, 0)));
		System.out.println("v(a) = " + planner.value(GraphDefinedDomain.getState(domain, 1)));
		System.out.println("v(b) = " + planner.value(GraphDefinedDomain.getState(domain, 2)));
		System.out.println("v(c) = " + planner.value(GraphDefinedDomain.getState(domain, 3)));

		actionName = actionName.replaceAll("0", " a");
		actionName = actionName.replaceAll("1", " b");
		actionName = actionName.replaceAll("2", " c");
		return actionName;

	}

	public static class FourParamRF implements RewardFunction {

		double p1;
		double p2;
		double p3;
		double p4;

		public FourParamRF(double p1, double p2, double p3, double p4) {
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			this.p4 = p4;
		}

		@Override
		public double reward(State s, GroundedAction a, State t) {

			int sid = GraphDefinedDomain.getNodeId(s);

			if(sid == 0 || sid == 3){
				return 0;
			}
			else if(sid == 1){
				return p1;
			}
			else if(sid == 2){
				return p2;
			}
			else if(sid == 4){
				return p3;
			}
			else if(sid == 5){
				return p4;
			}

			throw new RuntimeException("Unknown state: " + sid);
		}


	}



	public static void main(String[] args){
		double p1 = 5.;
		double p2 = 6.;
		double p3 = 3.;
		double p4 = 7.;

		double discountFactor = 0.6;

		DiscountFactorQuiz2 dfq = new DiscountFactorQuiz2(p1,p2,p3,p4);
		System.out.println("Best initial action: " + dfq.bestFirstAction(discountFactor));
	}


}
