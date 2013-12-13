package domain.stocasticgames.foragesteal.simple;

import java.util.List;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;

public class FSSimpleJAM extends JointActionModel {

	@Override
	public List<TransitionProbability> transitionProbsFor(State s,
			JointAction ja) {
		return this.deterministicTransitionProbsFor(s, ja);
	}

	@Override
	protected void actionHelper(State s, JointAction ja) {
		
		GroundedSingleAction player0Action = null;
		GroundedSingleAction player1Action = null;
		
		for(GroundedSingleAction gsa : ja){
			ObjectInstance player = s.getObject(gsa.actingAgent);
			if(player.getDiscValForAttribute(FSSimple.ATTPN) == 0){
				player0Action = gsa;
			}
			else{
				player1Action = gsa;
			}
		}
		
		ObjectInstance sn = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE);
		
		if(FSSimple.isRootNode(s)){
			//only change state from root node if steal action is taken
			if(player0Action.action.actionName.equals(FSSimple.ACTIONSTEAL)){
				sn.setValue(FSSimple.ATTSTATENODE, 2);
			}
		}
		else{
			
			//otherwise next state depends on player1 action
			if(player1Action.action.actionName.equals(FSSimple.ACTIONDONOTHING)){
				sn.setValue(FSSimple.ATTSTATENODE, 0);
			}
			else{
				sn.setValue(FSSimple.ATTSTATENODE, 1);
			}
			
		}

	}
	
	

}
