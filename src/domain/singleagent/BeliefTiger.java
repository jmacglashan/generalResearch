package domain.singleagent;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase;
import burlap.behavior.singleagent.vfa.cmac.CMACFeatureDatabase.TilingArrangement;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;

public class BeliefTiger implements DomainGenerator {

	public static final String				ATTLEFTTIGERBELIEF = "tigerLeft";

	public static final String				CLASSSTATE = "state";
	
	public static final String				ACTIONLEFT = "left";
	public static final String				ACTIONRIGHT = "right";
	public static final String				ACTIONLISTEN = "listen";
	public static final String				ACTIONDONOTHING = "nothing";
	
	@Override
	public Domain generateDomain() {
		
		SADomain domain = new SADomain();
		
		Attribute attleft = new Attribute(domain, ATTLEFTTIGERBELIEF, AttributeType.REAL);
		attleft.setLims(0., 1.);
		
		ObjectClass cstate = new ObjectClass(domain, CLASSSTATE);
		cstate.addAttribute(attleft);
		
		new LeftRightAction(ACTIONLEFT, domain);
		new LeftRightAction(ACTIONRIGHT, domain);
		new ListenAction(domain);
		new DoNothing(domain);
		
		return domain;
	}
	
	public static State getInitialState(Domain domain){
		State s = new State();
		ObjectInstance so = new ObjectInstance(domain.getObjectClass(CLASSSTATE), "state");
		so.setValue(ATTLEFTTIGERBELIEF, 0.5);
		s.addObject(so);
		return s;
	}
	
	
	protected class LeftRightAction extends Action{

		public LeftRightAction(String actionName, Domain domain){
			super(actionName, domain, "");
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance os = s.getFirstObjectOfClass(CLASSSTATE);
			//always reset
			os.setValue(ATTLEFTTIGERBELIEF, 0.5);
			
			return s;
		}
		
		
	}
	
	
	protected class ListenAction extends Action{

		public ListenAction(Domain domain){
			super(ACTIONLISTEN, domain, "");
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance os = s.getFirstObjectOfClass(CLASSSTATE);
			
			//current belief
			double bl = os.getRealValForAttribute(ATTLEFTTIGERBELIEF);
			
			
			
			//sample current state
			double r = RandomFactory.getMapped(0).nextDouble();
			int side = 0;
			if(r > bl){
				side = 1;
			}
			
			//next state is identical in tiger world from deterministic transition dynamics, how conveient!
			
			//sample an observation
			double r2 = RandomFactory.getMapped(0).nextDouble();
			int o;
			if(side == 0){ //tiger is left
				if(r2 < .85){
					o = 0;
				}
				else{
					o = 1;
				}
			}
			else{ //tiger is right
				if( r2 < 0.85){
					o = 1;
				}
				else{
					o = 0;
				}
			}
			
			//now that we have o, we can compute the belief update
			
			double prOLeft = 0.85;
			if(o == 1){
				prOLeft = 0.15;
			}
			
			double prORight = 0.85;
			if(o == 0){
				prORight = 0.15;
			}
			
			double leftNum = prOLeft*bl;
			double rightNum = prORight*(1-bl);
			
			double nBelief = leftNum / (leftNum + rightNum);
			
			os.setValue(ATTLEFTTIGERBELIEF, nBelief);
			
			return s;
		}
		
		
		
	}
	
	
	protected class DoNothing extends Action{

		public DoNothing(Domain domain){
			super(ACTIONDONOTHING, domain, "");
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			return s;
		}
		
		
		
		
	}
	
	public static class TigerExpectedReward implements RewardFunction{

		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			if(a.actionName().equals(ACTIONLISTEN)){
				return -1.;
			}
			else if(a.actionName().equals(ACTIONDONOTHING)){
				return 0.;
			}
			
			double bl = s.getFirstObjectOfClass(CLASSSTATE).getRealValForAttribute(ATTLEFTTIGERBELIEF);
			
			if(a.actionName().equals(ACTIONLEFT)){
				return -100*bl + 10*(1-bl);
			}
			else{
				return 10*bl + -100*(1-bl);
			}
			
		}
		

	}
	
	
	
	public static void main(String [] args){
		
		BeliefTiger bt = new BeliefTiger();
		Domain domain = bt.generateDomain();
		State initialState = BeliefTiger.getInitialState(domain);
		
		RewardFunction rf = new TigerExpectedReward();
		
		
		//TerminalExplorer exp = new TerminalExplorer(domain);
		//exp.exploreFromState(initialState);
		
		
		int nTilings = 1;
		CMACFeatureDatabase cmac = new CMACFeatureDatabase(nTilings, TilingArrangement.RANDOMJITTER);
		double resolution = 20.;
		cmac.addSpecificationForAllTilings(BeliefTiger.CLASSSTATE, domain.getAttribute(BeliefTiger.ATTLEFTTIGERBELIEF), 1. / resolution);
		
		double defaultQ = 10.;
		ValueFunctionApproximation vfa = cmac.generateVFA(defaultQ/nTilings);
		
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, new NullTermination(), 0.99, vfa, 0.1, 0.5);
		System.out.println("Beginning learning...");
		agent.runLearningEpisodeFrom(initialState, 10000);
		System.out.println("Finished learning.\n");
		
		Policy p = new GreedyQPolicy(agent);
		State curState = initialState;
		for(int i = 0; i < 50; i++){
			double bl = curState.getFirstObjectOfClass(BeliefTiger.CLASSSTATE).getRealValForAttribute(BeliefTiger.ATTLEFTTIGERBELIEF);
			
			GroundedAction ga = (GroundedAction)p.getAction(curState);
			State nextState = ga.executeIn(curState);
			
			System.out.println(ga.toString() + ": " + bl);
			if(!ga.actionName().equals(BeliefTiger.ACTIONLISTEN)){
				System.out.println("-------");
			}
			
			curState = nextState;
		}
		
	}

}
