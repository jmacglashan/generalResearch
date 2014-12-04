package behavior.AMDP.blacklistAffordance;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class AffordanceBlackListWrapper implements DomainGenerator{

	Domain srcDomain;

	public AffordanceBlackListWrapper(Domain srcDomain){
		this.srcDomain = srcDomain;
	}

	@Override
	public Domain generateDomain() {

		Domain domain = this.srcDomain.getNewDomainWithCopiedObjectClasses();
		List<PropositionalFunction> pfs = this.srcDomain.getPropFunctions();
		for(PropositionalFunction pf : pfs){
			domain.addPropositionalFunction(pf);
		}
		for(Action a : this.srcDomain.getActions()){
			new AffordanceBlackListActionWrapper(domain, a);
		}


		return domain;
	}

	public static void setGoal(Domain domain, StateConditionTest gc){
		for(Action a : domain.getActions()){
			AffordanceBlackListActionWrapper wa = (AffordanceBlackListActionWrapper)a;
			wa.getBlackList().setCurrentGoal(gc);
		}
	}

	public static void setBlackList(Domain domain, AffordanceBlackList blackList){
		AffordanceBlackListActionWrapper wa = (AffordanceBlackListActionWrapper)domain.getAction(blackList.associatedActionName);
		wa.setBlackList(blackList);
	}


	public static class AffordanceBlackListActionWrapper extends Action {

		protected Action srcAction;
		protected AffordanceBlackList blackList;

		public AffordanceBlackListActionWrapper(Domain domain, Action srcAction){
			super(srcAction.getName(), domain, srcAction.getParameterClasses(), srcAction.getParameterOrderGroups());
			this.srcAction = srcAction;
			this.clearBlackList();
		}

		public AffordanceBlackList getBlackList(){
			return this.blackList;
		}

		public void setBlackList(AffordanceBlackList blackList){
			this.blackList = blackList;
		}

		public void clearBlackList(){
			this.blackList = new AffordanceBlackList.NullAffordanceBlackList(this.name);
		}

		@Override
		public boolean applicableInState(State s, String[] params) {
			if(!this.srcAction.applicableInState(s, params)){
				return false;
			}

			GroundedAction ga = new GroundedAction(this.srcAction, params);
			if(this.blackList.filter(s, ga)){
				return false;
			}

			return true;

		}

		@Override
		public State performAction(State s, String[] params) {
			return this.srcAction.performAction(s, params);
		}

		@Override
		protected State performActionHelper(State s, String[] params) {
			return this.srcAction.performAction(s, params);
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.srcAction.getTransitions(s, params);
		}
	}


}
