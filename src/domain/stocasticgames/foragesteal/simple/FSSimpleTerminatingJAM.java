package domain.stocasticgames.foragesteal.simple;

import java.util.List;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.explorers.SGTerminalExplorer;

public class FSSimpleTerminatingJAM extends JointActionModel {

	@Override
	public List<TransitionProbability> transitionProbsFor(State s,
			JointAction ja) {
		return this.deterministicTransitionProbsFor(s, ja);
	}

	@Override
	protected State actionHelper(State s, JointAction ja) {
		
		GroundedSingleAction player0Action = null;
		
		for(GroundedSingleAction gsa : ja){
			ObjectInstance player = s.getObject(gsa.actingAgent);
			if(player.getIntValForAttribute(FSSimple.ATTPN) == 0){
				player0Action = gsa;
			}
		}
		
		ObjectInstance sn = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE);
		
		if(FSSimple.isRootNode(s)){
			//only change state from root node if steal action is taken
			if(player0Action.action.actionName.equals(FSSimple.ACTIONSTEAL)){
				sn.setValue(FSSimple.ATTSTATENODE, 2);
			}
			else{ //otherwise terminate
				sn.setValue(FSSimple.ATTSTATENODE, 3);
			}
		}
		else{
			
			//always terminate after choice of punishment
			sn.setValue(FSSimple.ATTSTATENODE, 3);
			
		}
		
		return s;

	}
	
	
	
	public static class FSSimpleTerminatingTF implements TerminalFunction{

		@Override
		public boolean isTerminal(State s) {
			int sn = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).getIntValForAttribute(FSSimple.ATTSTATENODE);
			return sn == 3;
		}
		
		
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		FSSimple gen = new FSSimple(3);
		SGDomain domain = (SGDomain)gen.generateDomain();
		JointActionModel jam = new FSSimpleTerminatingJAM();
		JointReward r = new FSSimpleJR();
		
		State s = FSSimple.getInitialState(domain, "player0", "player1", 0);
		
		SGTerminalExplorer exp = new SGTerminalExplorer(domain, jam);
		exp.setRewardFunction(r);
		
		for(int i = 0; i < gen.nfalts; i++){
			exp.addActionShortHand("f"+i, FSSimple.ACTIONFORAGEBASE+i);
		}
		exp.addActionShortHand("s", FSSimple.ACTIONSTEAL);
		exp.addActionShortHand("p", FSSimple.ACTIONPUNISH);
		exp.addActionShortHand("n", FSSimple.ACTIONDONOTHING);
		
		exp.exploreFromState(s);

	}

}
