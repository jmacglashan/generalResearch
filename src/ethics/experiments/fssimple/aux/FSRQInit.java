package ethics.experiments.fssimple.aux;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimple.ForageAction;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;

public class FSRQInit implements ValueFunctionInitialization {

	protected FSSubjectiveRF		subjectiveRF;
	protected FSSimpleJR			objectiveRF;
	
	public FSRQInit(FSSimpleJR objectiveRF, FSSubjectiveRF subjectiveRF){
		this.objectiveRF = objectiveRF;
		this.subjectiveRF = subjectiveRF;
	}
	
	
	protected int forageAltForAction(GroundedSingleAction gsa){
		ForageAction sa = (ForageAction)gsa.action;
		return sa.falt;
	}

	@Override
	public double value(State s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double qValue(State s, AbstractGroundedAction a) {
		
		GroundedSingleAction gsa = (GroundedSingleAction)a;
		
		double [] params = this.subjectiveRF.getParameters();
		
		if(gsa.action.actionName.startsWith(FSSimple.ACTIONFORAGEBASE)){
			int fa = this.forageAltForAction(gsa);
			double [] fvals = this.objectiveRF.getForageRewards();
			return fvals[fa];
		}
		else if(gsa.action.actionName.equals(FSSimple.ACTIONSTEAL)){
			return params[0] + this.objectiveRF.getStealerReward();
		}
		else if(gsa.action.actionName.equals(FSSimple.ACTIONPUNISH)){
			return params[1] + this.objectiveRF.getPuncherReward();
		}
		
		return 0; //return 0 for all do nothing actions
	}
	
}
