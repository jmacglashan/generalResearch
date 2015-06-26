package demos.gt;

import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;

public class DiscountFactorQuiz {

	DomainGenerator				qdg;
	Domain						domain;
	RewardFunction				rf;
	TerminalFunction			tf;
	State						initialState;
	DiscreteStateHashFactory	hashingFactory;

	public static final String STATEINIT = "Initial State";
	public static final String STATEA = "State A";
	public static final String STATEB1 = "State B1";
	public static final String STATEB2 = "State B2";
	public static final String STATEC1 = "State C1";
	public static final String STATEC2 = "State C2";

	public static final String ACTION1 = "Action a";
	public static final String ACTION2 = "Action b";
	public static final String ACTION3 = "Action c";

	public static final String ATTSTATE = "State";

	public static final String CLASSAGENT = "Agent";
	public static final String NAMEAGENT = "Agent 0";

	public DiscountFactorQuiz(double p1, double p2, double p3, double p4) {
		qdg = new QuizDomainGenerator();
		rf = new FourParamRF(p1,p2,p3,p4);
		tf = new NeverEndingTF();
		domain = qdg.generateDomain();
		initialState = getInitialState();
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(DiscountFactorQuiz.CLASSAGENT,
				domain.getObjectClass(DiscountFactorQuiz.CLASSAGENT).attributeList);
	}

	protected class QuizDomainGenerator implements DomainGenerator {
		@Override
		public Domain generateDomain() {

			SADomain domain = new SADomain();

			Attribute attState = new Attribute(domain, ATTSTATE, AttributeType.STRING);

			ObjectClass agentClass = new ObjectClass(domain, CLASSAGENT);
			agentClass.addAttribute(attState);

			new Movement(ACTION1, domain);
			new Movement(ACTION2, domain);
			new Movement(ACTION3, domain);

			return domain;
		}
	}

	public State getInitialState(){
		State s = new MutableState();
		ObjectInstance agent = new MutableObjectInstance(domain.getObjectClass(CLASSAGENT), NAMEAGENT);
		agent.setValue(ATTSTATE, STATEINIT);

		s.addObject(agent);

		return s;
	}

	protected class Movement extends Action{

		public Movement(String actionName, Domain domain) {
			super(actionName, domain, "");
		}

		@Override
		protected State performActionHelper(State s, String[] params) {
			//get agent and current position
			ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
			String curState = agent.getStringValForAttribute(ATTSTATE);

			//get resulting position
			String nextState = this.moveResult(curState, this.getName());

			//set the new position
			agent.setValue(ATTSTATE, nextState);

			//return the state we just modified
			return s;
		}

		protected String moveResult(String curState, String actionName){
			String nextState = null;

			if( curState.equals(STATEINIT) ) {
				if( actionName.equals(ACTION1) ) {
					nextState = STATEA;
				}
				else if( actionName.equals(ACTION2) ) {
					nextState = STATEB1;
				}
				else if( actionName.equals(ACTION3) ){
					nextState = STATEC1;
				}
			}
			else if( curState.equals(STATEA) ) {
				nextState = STATEA;
			}
			else if( curState.equals(STATEB1) ) {
				nextState = STATEB2;
			}
			else if( curState.equals(STATEB2) ) {
				nextState = STATEB1;
			}
			else if( curState.equals(STATEC1) ) {
				nextState = STATEC2;
			}
			else if( curState.equals(STATEC2) ) {
				nextState = STATEC2;
			}

			return nextState;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return deterministicTransition(s, params);
		}

		@Override
		public boolean applicableInState(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
			String curState = agent.getStringValForAttribute(ATTSTATE);

			if( this.getName().equals(ACTION1) ) { //action1 is always valid
				return true;
			}
			else {
				return curState.equals(STATEINIT); //other actions are only valid in the initial state
			}
		}

	}

	public static class FourParamRF implements RewardFunction{

		double p1;
		double p2;
		double p3;
		double p4;

		public FourParamRF(double p1, double p2, double p3, double p4){
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			this.p4 = p4;
		}

		@Override
		public double reward(State s, GroundedAction a, State t) {

			String curState = s.getFirstObjectOfClass(CLASSAGENT).getStringValForAttribute(ATTSTATE);

			if( curState.equals(STATEINIT) || curState.equals(STATEC1) ){
				return 0.;
			}
			else if( curState.equals(STATEA) ){
				return p1;
			}
			else if( curState.equals(STATEB1) ) {
				return p2;
			}
			else if( curState.equals(STATEB2) ) {
				return p3;
			}
			else if( curState.equals(STATEC2) ) {
				return p4;
			}

			return Double.NEGATIVE_INFINITY;
		}

	}

	public static class NeverEndingTF implements TerminalFunction {
		@Override
		public boolean isTerminal(State s) {
			return false;
		}
	}

	public String bestFirstAction(double discountFactor) {
		double delta = 0.00001;
		int maxIters = 1000;
		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, discountFactor, hashingFactory, delta, maxIters);
		planner.planFromState(initialState);
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		return p.getAction(initialState).actionName();
	}

	public static void main(String[] args){
		double p1 = 5.;
		double p2 = 6.;
		double p3 = 3.;
		double p4 = 7.;

		double discountFactor = 0.6;

		DiscountFactorQuiz dfq = new DiscountFactorQuiz(p1,p2,p3,p4);
		System.out.println("Best initial action: " + dfq.bestFirstAction(discountFactor));
	}

}
