package classexercises.die4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;



public class Die4Live {

	public static final String					ATTSCORE = "score";
	public static final String					ATTGOV = "gameover";
	public static final String					ACTIONROLL = "roll";
	public static final String					ACTIONQUIT = "quit";
	
	public static final String					CLASSAGENT = "agent";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		Domain domain = new SADomain();
		
		Attribute attscore = new Attribute(domain, ATTSCORE, Attribute.AttributeType.DISC);
		attscore.setDiscValuesForRange(0, 300, 1);
		
		Attribute attgov = new Attribute(domain, ATTGOV, Attribute.AttributeType.DISC);
		attgov.setDiscValuesForRange(0, 1, 1);
		
		ObjectClass agent = new ObjectClass(domain, CLASSAGENT);
		agent.addAttribute(attscore);
		agent.addAttribute(attgov);
		
		Action roll = new Roll(ACTIONROLL, domain);
		Action quit = new Quit(ACTIONQUIT, domain);
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				ObjectInstance agent = sprime.getObjectsOfTrueClass(CLASSAGENT).get(0);
				int gov = agent.getDiscValForAttribute(ATTGOV);
				if(gov == 1){
					return agent.getDiscValForAttribute(ATTSCORE);
				}
				return 0;
			}
		};
		
		TerminalFunction tf = new TerminalFunction() {
			
			@Override
			public boolean isTerminal(State s) {
				ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
				int gov = agent.getDiscValForAttribute(ATTGOV);
				if(gov==1){
					return true;
				}
				int score = agent.getDiscValForAttribute(ATTSCORE);
				if(score >= 300){
					return true;
				}
				return false;
			}
		};
		
		
		StateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		ValueIteration vi = new ValueIteration(domain, rf, tf, 1.0, hashingFactory, 0.001, 10000);
		
		State initialState = new State();
		ObjectInstance agentob = new ObjectInstance(agent, "agent");
		agentob.setValue(ATTSCORE, 0);
		agentob.setValue(ATTGOV, 0);
		initialState.addObject(agentob);
		
		vi.planFromState(initialState);
		
		System.out.println("Value: " + vi.value(initialState));
		
		Policy greedyQ = new GreedyQPolicy(vi);
		
		for(int i = 0; i < 20; i++){
			EpisodeAnalysis ea = greedyQ.evaluateBehavior(initialState, rf, tf, 1000);
			System.out.println(ea.getActionSequenceString());
			System.out.println(ea.getDiscountedReturn(1.));
		}
		
		

	}
	
	
	
	
	
	public static class Roll extends Action{
		
		Random rand = new Random();
		
		public Roll(String name, Domain d){
			super(name, d, "");
		}

		@Override
		protected State performActionHelper(State st, String[] params) {
			
			ObjectInstance agent = st.getObjectsOfTrueClass(CLASSAGENT).get(0);
			
			int r = rand.nextInt(6) + 1;
			
			if(r == 4){
				agent.setValue(ATTSCORE, 0);
				agent.setValue(ATTGOV, 1);
			}
			else{
				int oldScore = agent.getDiscValForAttribute(ATTSCORE);
				agent.setValue(ATTSCORE, oldScore+r);
			}
			
			
			return st;
		}
		
		
		public List<TransitionProbability> getTransitions(State st, String [] params){
			
			List <TransitionProbability> tps = new ArrayList<TransitionProbability>();
			
			ObjectInstance agent = st.getObjectsOfTrueClass(CLASSAGENT).get(0);
			int oldscore = agent.getDiscValForAttribute(ATTSCORE);
			
			for(int i = 1; i <= 6; i++){
				
				if(i == 4){
					
					State ns = st.copy();
					ObjectInstance newagent = ns.getObjectsOfTrueClass(CLASSAGENT).get(0);
					newagent.setValue(ATTSCORE, 0);
					newagent.setValue(ATTGOV, 1);
					
					TransitionProbability tp = new TransitionProbability(ns, 1./6.);
					tps.add(tp);
					
				}
				else{
					
					State ns = st.copy();
					ObjectInstance newagent = ns.getObjectsOfTrueClass(CLASSAGENT).get(0);
					newagent.setValue(ATTSCORE, oldscore+i);
					
					TransitionProbability tp = new TransitionProbability(ns, 1./6.);
					tps.add(tp);
					
					
				}
				
			}
			
			
			return tps;
		}
		
		
	}
	
	
	public static class Quit extends Action{

		public Quit(String name, Domain d){
			super(name, d, "");
		}
		
		@Override
		protected State performActionHelper(State st, String[] params) {
			ObjectInstance agent = st.getObjectsOfTrueClass(CLASSAGENT).get(0);
			agent.setValue(ATTGOV, 1);
			
			return st;
		}
		
		
		
		
		
	}


}
