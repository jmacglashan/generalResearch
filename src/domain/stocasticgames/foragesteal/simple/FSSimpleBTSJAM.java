package domain.stocasticgames.foragesteal.simple;

import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;

public class FSSimpleBTSJAM extends JointActionModel {

	protected double probBackTurned;
	protected double probSleeping;
	protected Random rand;
	
	public FSSimpleBTSJAM(double probBackTurned, double probSleeping){
		this.probBackTurned = probBackTurned;
		this.probSleeping = probSleeping;
		rand = RandomFactory.getMapped(0);
	}
	
	
	@Override
	public List<TransitionProbability> transitionProbsFor(State s,
			JointAction ja) {
		throw new RuntimeException("Transition probabilities currently not provided for back turned joint action model.");
	}

	@Override
	protected State actionHelper(State s, JointAction ja) {
		
		GroundedSingleAction player0Action = null;
		GroundedSingleAction player1Action = null;
		
		
		ObjectInstance player0 = null;
		ObjectInstance player1 = null;
		for(GroundedSingleAction gsa : ja){
			ObjectInstance player = s.getObject(gsa.actingAgent);
			if(player.getIntValForAttribute(FSSimple.ATTPN) == 0){
				player0Action = gsa;
				player0 = player;
			}
			else{
				player1Action = gsa;
				player1 = player;
			}
		}
		
		ObjectInstance sn = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE);
		
		if(FSSimple.isRootNode(s)){
			//only change state from root node if steal action is taken and if back is not turned
			int backTurned = player1.getIntValForAttribute(FSSimple.ATTBACKTURNED);
			if(player0Action.action.actionName.equals(FSSimple.ACTIONSTEAL) && backTurned == 0){
				sn.setValue(FSSimple.ATTSTATENODE, 2);
				player0.setValue(FSSimple.ATTBACKTURNED, this.sampleSleepingValue());
			}
			else{
				player1.setValue(FSSimple.ATTBACKTURNED, this.sampleBackTurnedValue());
			}
		}
		else{
			
			//get sleeping value before changing it
			int sleeping = player0.getIntValForAttribute(FSSimple.ATTBACKTURNED);
			
			player1.setValue(FSSimple.ATTBACKTURNED, this.sampleBackTurnedValue());
			player0.setValue(FSSimple.ATTBACKTURNED, 0);
			
			//if player0 is sleeping, then the action of player1 doesn't matter
			
			if(sleeping == 1){
				sn.setValue(FSSimple.ATTSTATENODE, 3);
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
		
		return s;
		
		
	}
	
	protected int sampleBackTurnedValue(){
		double roll = rand.nextDouble();
		if(roll <= this.probBackTurned){
			return 1;
		}
		return 0;
	}
	
	protected int sampleSleepingValue(){
		double roll = rand.nextDouble();
		if(roll <= this.probSleeping){
			return 1;
		}
		return 0;
	}

}
