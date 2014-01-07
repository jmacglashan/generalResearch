package ethics.experiments.fssimple.aux;

import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import domain.stocasticgames.foragesteal.simple.FSSimple.ForageAction;
import burlap.behavior.stochasticgame.agents.naiveq.SGNQValueInitialization;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;

public class FSRQInit implements SGNQValueInitialization {

	protected FSSubjectiveRF		subjectiveRF;
	protected FSSimpleJR			objectiveRF;
	
	public FSRQInit(FSSimpleJR objectiveRF, FSSubjectiveRF subjectiveRF){
		this.objectiveRF = objectiveRF;
		this.subjectiveRF = subjectiveRF;
	}
	
	@Override
	public double qInit(State s, GroundedSingleAction gsa) {
		
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

	
	protected int forageAltForAction(GroundedSingleAction gsa){
		ForageAction sa = (ForageAction)gsa.action;
		return sa.falt;
	}
	
}
