package behavior.training.prl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class PMDirectPolicy extends PreferenceModifiablePolicy {

	protected List<Action>							actions;
	protected StateHashFactory						hashingFactory;
	protected double								learningRate;
	
	protected Map<StateHashTuple, PolicyNode>		preferences;
	
	protected Random								rand;
	
	
	protected boolean								containsParameterizedActions = false;
	
	
	
	public PMDirectPolicy(Domain domain, StateHashFactory hashingFactory, double learningRate) {
		
		this.actions = new ArrayList<Action>(domain.getActions());
		this.hashingFactory = hashingFactory;
		this.learningRate = learningRate;
		
		this.preferences = new HashMap<StateHashTuple, PolicyNode>();
		
		this.rand = RandomFactory.getMapped(0);
		
		for(Action a : actions){
			if(a.getParameterClasses().length > 0){
				containsParameterizedActions = true;
				break;
			}
		}
		
	}
	
	public void addNonDomainReferencedAction(Action a) {
		
		if(!actions.contains(a)){
			this.actions.add(a);
			if(a.getParameterClasses().length > 0){
				containsParameterizedActions = true;
			}
		}
	}

	@Override
	public void updateStatePreferences(State s, List<ActionProb> preferences) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		PolicyNode pn = this.preferences.get(sh);
		if(pn == null){
			pn = new PolicyNode(sh);
			this.preferences.put(sh, pn);
		}
		
		for(ActionProb ap : preferences){
			ActionPreference app = this.getMatchingPreference(sh, ap.ga, pn);
			app.preference += this.learningRate * (ap.pSelection - app.preference);
		}
		

	}

	@Override
	public void updateSinglePreferenceInDirection(State s, GroundedAction ga, double preferenceChangeDirection) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		PolicyNode pn = this.preferences.get(sh);
		if(pn == null){
			pn = new PolicyNode(sh);
			this.preferences.put(sh, pn);
		}
		ActionPreference ap = this.getMatchingPreference(sh, ga, pn);
		
		if(preferenceChangeDirection < 0.){
			
			double target = 0.;
			double change = this.learningRate * (target - ap.preference);
			double remaining = 1. - ap.preference;
			//double alternateChange = -change / (pn.preferences.size() -1); //since change is negative other actions increased positively 
			
			if(change == 0.){
				return;
			}
			
			boolean normalizeRest = false;
			if(remaining == 0.){
				normalizeRest = true;
			}
			
			for(ActionPreference app : pn.preferences){
				if(app != ap){
					if(!normalizeRest){
						app.preference = app.preference + (app.preference/remaining)*Math.abs(change);
					}
					else{
						app.preference = 1. / ((double)(pn.preferences.size()-1));
					}
				}
				else{
					app.preference = app.preference + change;
				}
				if(Double.isNaN(app.preference)){
					System.out.println("error in pref");
				}
			}
			
			
		}
		
		
		//not supported for positive or neutral single changes
		
	}


	@Override
	public GroundedAction getAction(State s) {
		List <ActionProb> aprobs = this.getActionDistributionForState(s);
		double roll = this.rand.nextDouble();
		double sumP = 0.;
		for(ActionProb ap : aprobs){
			sumP += ap.pSelection;
			if(roll <= sumP){
				return ap.ga;
			}
		}
		System.out.println("error...");
		return null;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		List <GroundedAction> gas = s.getAllGroundedActionsFor(this.actions);
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		PolicyNode node = this.preferences.get(sh);
		if(node == null){
			node = new PolicyNode(sh);
			this.preferences.put(sh, node);
		}
		
		List <ActionProb> probs = new ArrayList<ActionProb>(gas.size());
		
		for(ActionPreference ap : node.preferences){
			probs.add(new ActionProb(ap.ga, ap.preference));
		}
		
		
		if(this.containsParameterizedActions){
			//then convert back to this query state's rep
			Map <String, String> matching = node.sh.s.getObjectMatchingTo(s, false);
			
			List <ActionProb> translated = new ArrayList<ActionProb>(probs.size());
			for(ActionProb ap : probs){
				if(ap.ga.params.length == 0){
					translated.add(ap);
				}
				else{
					ActionProb tap = new ActionProb(this.translateAction(ap.ga, matching), ap.pSelection);
					translated.add(tap);
				}
			}
			
			return translated;
			
		}
		
		
		return probs;
	}

	@Override
	public boolean isStochastic() {
		return true;
	}
	

	
	
	protected ActionPreference getMatchingPreference(StateHashTuple sh, GroundedAction ga, PolicyNode node){
		
		GroundedAction translatedAction = ga;
		if(ga.params.length > 0){
			Map <String, String> matching = sh.s.getObjectMatchingTo(node.sh.s, false);
			translatedAction = this.translateAction(ga, matching);
		}
		
		for(ActionPreference p : node.preferences){
			if(p.ga.equals(translatedAction)){
				return p;
			}
		}
		
		return null;
	}
	
	
	protected GroundedAction translateAction(GroundedAction a, Map <String,String> matching){
		String [] newParams = new String[a.params.length];
		for(int i = 0; i < a.params.length; i++){
			newParams[i] = matching.get(a.params[i]);
		}
		return new GroundedAction(a.action, newParams);
	}
	
	
	
	class PolicyNode{
		
		public StateHashTuple			sh;
		public List <ActionPreference>	preferences;
		
		
		public PolicyNode(StateHashTuple sh){
			this.sh = sh;
			this.preferences = new ArrayList<ActionPreference>();
			List <GroundedAction> gas = sh.s.getAllGroundedActionsFor(actions);
			double uni = 1./(double)gas.size();
			for(GroundedAction ga : gas){
				this.preferences.add(new ActionPreference(ga, uni));
			}
		}
		
		public void addPreference(ActionPreference pr){
			this.preferences.add(pr);
		}
		
		
	}
	
	
	class ActionPreference{
		
		public GroundedAction 	ga;
		public double			preference;
		
		public ActionPreference(GroundedAction ga, double preference){
			this.ga = ga;
			this.preference = preference;
		}
		
	}
	

}
