package behavior.learning.modellearning.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import behavior.learning.modellearning.Model;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TabularModel extends Model {

	protected Domain							sourceDomain;
	protected StateHashFactory					hashingFactory;
	protected Map<StateHashTuple, StateNode> 	stateNodes;
	protected Set<StateHashTuple> 				terminalStates;
	protected int								nConfident;
	
	
	protected TerminalFunction					modeledTF;
	protected RewardFunction					modeledRF;
	
	
	public TabularModel(Domain sourceDomain, StateHashFactory hashingFactory, int nConfident){
		this.sourceDomain = sourceDomain;
		this.hashingFactory = hashingFactory;
		this.stateNodes = new HashMap<StateHashTuple, TabularModel.StateNode>();
		this.terminalStates = new HashSet<StateHashTuple>();
		this.nConfident = nConfident;
		
		this.modeledTF = new TerminalFunction() {
			
			@Override
			public boolean isTerminal(State s) {
				return terminalStates.contains(TabularModel.this.hashingFactory.hashState(s));
			}
		};
		
		
		this.modeledRF = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				StateActionNode san = TabularModel.this.getStateActionNode(TabularModel.this.hashingFactory.hashState(s), a);
				if(san == null){
					return 0;
				}
				if(san.nTries == 0){
					return 0.;
				}
				return san.sumR / (double)san.nTries;
			}
		};
	}
	
	@Override
	public RewardFunction getModelRF() {
		return this.modeledRF;
	}

	@Override
	public TerminalFunction getModelTF() {
		return this.modeledTF;
	}

	@Override
	public boolean transitionIsModeled(State s, GroundedAction ga) {
		
		StateActionNode san = this.getStateActionNode(this.hashingFactory.hashState(s), ga);
		if(san == null){
			return false;
		}
		if(san.nTries < this.nConfident){
			return false;
		}
		
		return true;
	}

	@Override
	public State sampleModelHelper(State s, GroundedAction ga) {
		return this.sampleTransitionFromTransitionProbabilities(s, ga);
	}

	@Override
	public List<TransitionProbability> getTransitionProbabilities(State s, GroundedAction ga) {
		
		List<TransitionProbability> transitions = new ArrayList<TransitionProbability>();
		
		StateActionNode san = this.getStateActionNode(this.hashingFactory.hashState(s), ga);
		if(san == null){
			//assume transition to self if we haven't modeled this at all
			TransitionProbability tp = new TransitionProbability(s, 1.);
			transitions.add(tp);
		}
		else{
			for(OutcomeState os : san.outcomes.values()){
				State sp = os.osh.s;
				double p = (double)os.nTimes / (double)san.nTries;
				TransitionProbability tp = new TransitionProbability(sp, p);
				transitions.add(tp);
			}
		}
		
		return transitions;
	}

	@Override
	public void updateModel(State s, GroundedAction ga, State sprime, double r, boolean sprimeIsTerminal) {
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		StateHashTuple shp = this.hashingFactory.hashState(sprime);
		
		if(sprimeIsTerminal){
			this.terminalStates.add(shp);
		}
		
		StateActionNode san = this.getOrCreateActionNode(sh, ga);
		san.update(r, shp);

	}
	
	
	protected StateActionNode getStateActionNode(StateHashTuple sh, GroundedAction ga){

		StateNode sn = this.stateNodes.get(sh);
		if(sn == null){
			return null;
		}
		return sn.actionNode(ga.translateParameters(sh.s, sn.sh.s));
	}
	
	
	protected StateActionNode getOrCreateActionNode(StateHashTuple sh, GroundedAction ga){

		StateNode sn = this.stateNodes.get(sh);
		StateActionNode toReturn = null;
		if(sn == null){
			sn = new StateNode(sh);
			this.stateNodes.put(sh, sn);
			
			List <GroundedAction> allActions = sh.s.getAllGroundedActionsFor(this.sourceDomain.getActions());
			for(GroundedAction tga : allActions){
				StateActionNode san = sn.addActionNode(tga);
				if(tga.equals(ga)){
					toReturn = san;
				}
			}
			
		}
		else{
			toReturn = sn.actionNode(ga.translateParameters(sh.s, sn.sh.s));
		}
		
		if(toReturn == null){
			throw new RuntimeException("Could not finding matching grounded action in model for action: " + ga.toString());
		}
		
		
		return toReturn;
	}
	
	
	
	class StateNode{
		StateHashTuple sh;
		Map <GroundedAction, StateActionNode> actionNodes;
		
		public StateNode(StateHashTuple sh){
			this.sh = sh;
			this.actionNodes = new HashMap<GroundedAction, TabularModel.StateActionNode>();
		}
		
		public StateActionNode actionNode(GroundedAction ga){
			return actionNodes.get(ga);
		}
		
		public StateActionNode addActionNode(GroundedAction ga){
			StateActionNode san = new StateActionNode(ga);
			this.actionNodes.put(ga, san);
			return san;
		}
		
		
	}
	
	class StateActionNode{
		
		GroundedAction ga;
		int nTries;
		double sumR;
		Map<StateHashTuple, OutcomeState> outcomes;
		
		
		public StateActionNode(GroundedAction ga){
			this.ga = ga;
			this.sumR = 0.;
			this.nTries = 0;
			
			this.outcomes = new HashMap<StateHashTuple, TabularModel.OutcomeState>();
		}
		
		public StateActionNode(GroundedAction ga, double r, StateHashTuple sprime){
			this.ga = ga;
			this.sumR = r;
			this.nTries = 1;
			
			this.outcomes = new HashMap<StateHashTuple, TabularModel.OutcomeState>();
			this.outcomes.put(sprime, new OutcomeState(sprime));
		}
		
		
		public void update(double r, StateHashTuple sprime){
			this.nTries++;
			this.sumR += r;
			OutcomeState stored = this.outcomes.get(sprime);
			if(stored != null){
				stored.nTimes++;
			}
			else{
				this.outcomes.put(sprime, new OutcomeState(sprime));
			}
		}
		
	}
	
	class OutcomeState{
		StateHashTuple osh;
		int nTimes;
		
		public OutcomeState(StateHashTuple osh){
			this.osh = osh;
			nTimes = 1;
		}
		
		@Override
		public int hashCode(){
			return osh.hashCode();
		}
		
		@Override
		public boolean equals(Object o){
			if(!(o instanceof OutcomeState)){
				return false;
			}
			
			OutcomeState oos = (OutcomeState)o;
			return this.osh.equals(oos.osh);
		}
		
		
	}
	
	
	

}
