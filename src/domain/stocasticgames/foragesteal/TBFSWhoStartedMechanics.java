package domain.stocasticgames.foragesteal;

import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.stocashticgames.GroundedSingleAction;
import burlap.oomdp.stocashticgames.JointAction;
import burlap.oomdp.stocashticgames.JointActionModel;

public class TBFSWhoStartedMechanics extends JointActionModel {

	
	protected double minTerminateProb;
	protected Random rand;
	
	public TBFSWhoStartedMechanics(){
		minTerminateProb = 0.01;
		rand = RandomFactory.getMapped(0);
	}
	
	
	@Override
	public List<TransitionProbability> transitionProbsFor(State s,
			JointAction ja) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void actionHelper(State s, JointAction ja) {
		
		
		GroundedSingleAction agentForTurnAction = null;
		ObjectInstance agentForTurnObject = null;
		ObjectInstance nonActingAgent = null;
		for(GroundedSingleAction gsa : ja){
			ObjectInstance ao = s.getObject(gsa.actingAgent);
			
			int turn = ao.getDiscValForAttribute(TBForageSteal.ATTISTURN);
			if(turn == 1){
				agentForTurnAction = gsa;
				agentForTurnObject = ao;
			}
			else{
				nonActingAgent = ao;
			}
		}
		
		boolean shouldSwapTurns = true;
		
		
		
		if(agentForTurnAction != null){
		
			//now that the acting agent is found perform the action
			
			if(agentForTurnAction.action.actionName.equals(TBForageSteal.ACTIONNOP) || agentForTurnAction.action.actionName.startsWith(TBForageSteal.ACTIONFORAGE)){
				shouldSwapTurns = false;
				agentForTurnObject.setValue(TBForageSteal.ATTPTA, 1);
			}
			else if(agentForTurnAction.action.actionName.equals(TBForageSteal.ACTIONSTEAL)){
				agentForTurnObject.setValue(TBForageSteal.ATTPTA, 2);
			}
			else if(agentForTurnAction.action.actionName.equals(TBForageSteal.ACTIONPUNCH)){
				//to mimic graph we should set both now into the punch state
				int punchState = this.punchState(agentForTurnObject, nonActingAgent);
				agentForTurnObject.setValue(TBForageSteal.ATTPTA, punchState);
				nonActingAgent.setValue(TBForageSteal.ATTPTA, punchState);
			}
			
			agentForTurnObject.setValue(TBForageSteal.ATTISTURN, 0);
			
		}
		else{
			shouldSwapTurns = false;
		}
		
		
		double roll = rand.nextDouble();
		if(roll < minTerminateProb){
			shouldSwapTurns = false;
		}
		
		if(shouldSwapTurns){
			nonActingAgent.setValue(TBForageSteal.ATTISTURN, 1);
		}

	}
	
	
	protected int punchState(ObjectInstance agentForTurn, ObjectInstance nonActingAgent){
		
		int punchState = 3;
		
		int aftPN = agentForTurn.getDiscValForAttribute(TBForageSteal.ATTPN);
		int naaPA = nonActingAgent.getDiscValForAttribute(TBForageSteal.ATTPTA);
		
		if((aftPN == 0 && naaPA == 2) || naaPA == 4){
			punchState = 4;
		}
		
		return punchState;
		
	}

	
}
