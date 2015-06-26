package ethics.experiments.fssimple.specialagents;

import java.util.Map;

import domain.stocasticgames.foragesteal.simple.FSSimple;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;

public class OpponentOutcomeDBLStealthAgent extends Agent {

	/**
	 * state order: 
	 * 				0: (thief) punisher's back is turned
	 * 				1: (theif) punisher punished for theft
	 * 				2: (theif) punisher did not respond to theft
	 * 				3: (thief) I slept through last interaction
	 * 				4: (punisher) thief did not steal after punishment
	 * 				5: (punisher) thief did steal after punishment
	 * 				6: (punisher) thief is asleep
	 * 
	 * value 0 corresponds to forage/do nothing
	 * value 1 correspodns to steal/punish
	 */
	protected int [] historyPolicy;
	
	protected double epsilon = 0.1;
	
	protected boolean thiefStoleAnyway = false;
	protected boolean iJustPunished = false;
	
	
	public OpponentOutcomeDBLStealthAgent(Domain domain, int [] historyPolicy){
		if(historyPolicy.length != 7){
			throw new RuntimeException("History policy must be of length 7; input is length: " + historyPolicy.length);
		}
		this.domain = (SGDomain)domain;
		this.historyPolicy = historyPolicy.clone();
	}
	
	
	@Override
	public void gameStarting() {
		thiefStoleAnyway = false;
		iJustPunished = false;
	}

	@Override
	public GroundedSingleAction getAction(State s) {
		
		
		int pNum = this.getMyPlayerNum(s);
		
		int sn = FSSimple.stateNode(s);
		
		GroundedSingleAction selection = null;
		
		boolean punisherBackIsTurned = this.punisherBackIsTurned(s);

		
		double roll = RandomFactory.getMapped(0).nextDouble();
		
		if(pNum == 0){
			
			//then I am thief
			
			if(roll < this.epsilon){
				if(sn != 2){
					if(RandomFactory.getMapped(0).nextDouble() < 0.5){
						selection = this.stealAction();
					}
					else{
						selection = this.forageAction();
					}
				}
				else{
					selection = this.nothingAction();
				}
			}
			else{
			
				if(punisherBackIsTurned){
					selection = this.thiefSelection(0);
				}
				else if(sn == 0){
					selection = this.thiefSelection(2);
				}
				else if(sn == 1){
					selection = this.thiefSelection(1);
				}
				else if(sn == 3){
					selection = this.thiefSelection(3);
				}
				else{
					selection = this.nothingAction();
				}
				
			}
			
		}
		else{
			//I am a punisher
			
			if(roll < 0){
				if(sn == 2){
					if(RandomFactory.getMapped(0).nextDouble() < 0.5){
						selection = this.forageAction();
					}
					else{
						selection = this.nothingAction();
					}
				}
				else{
					selection = this.nothingAction();
				}
			}
			else{
				if(sn == 2){
					if(this.thiefIsSleeping(s)){
						selection = this.punisherSelection(6);
					}
					else if(this.thiefStoleAnyway){
						selection = this.punisherSelection(5);
					}
					else{
						selection = this.punisherSelection(4);
					}
				}
				else{
					selection = this.nothingAction();
				}
			}
			
		}
		
		if(!selection.action.isApplicableInState(s, worldAgentName, selection.params)){
			throw new RuntimeException("Error in action selection return.");
		}
		
		
		return selection;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		
		int pNum = this.getMyPlayerNum(s);
		if(pNum == 1){
			//I am punisher
			
			//are we observing a subsequent action selection after my punishment and from which the agent did not sleep?
			int sn = FSSimple.stateNode(s);
			if(sn != 2 && sn != 3){
				
				//do I need to record the immediate result of my punishment?
				if(this.iJustPunished){
					
					//is it something that I can observe?
					boolean punisherBackIsTurned = this.punisherBackIsTurned(s);
					if(!punisherBackIsTurned){
						
						int spn = FSSimple.stateNode(sprime);
						if(spn == 2){
							this.thiefStoleAnyway = true;
						}
						else{
							this.thiefStoleAnyway = false;
						}
						
						this.iJustPunished = false;
					}
					
				}
				
				
			}
			else if(sn == 3){
				this.iJustPunished = false;
			}
			
			
			
		}
		

	}

	@Override
	public void gameTerminated() {
		//do nothing
	}
	
	
	protected int getMyPlayerNum(State s){
		ObjectInstance me = s.getObject(this.worldAgentName);
		
		//which player am I
		int pNum = me.getIntValForAttribute(FSSimple.ATTPN);
		
		return pNum;

	}
	
	
	protected boolean punisherBackIsTurned(State s){
		ObjectInstance punisher = null;
		for(ObjectInstance player : s.getObjectsOfClass(FSSimple.CLASSPLAYER)){
			int pNum = player.getIntValForAttribute(FSSimple.ATTPN);
			if(pNum == 1){
				punisher = player;
				break;
			}
		}
		
		return punisher.getIntValForAttribute(FSSimple.ATTBACKTURNED) == 1;
	}
	
	protected boolean thiefIsSleeping(State s){
		ObjectInstance thief = null;
		for(ObjectInstance player : s.getObjectsOfClass(FSSimple.CLASSPLAYER)){
			int pNum = player.getIntValForAttribute(FSSimple.ATTPN);
			if(pNum == 0){
				thief = player;
				break;
			}
		}
		
		return thief.getIntValForAttribute(FSSimple.ATTBACKTURNED) == 1;
	}
	
	
	protected GroundedSingleAction thiefSelection(int policyStateIndex){
		if(this.historyPolicy[policyStateIndex] == 0){
			return this.forageAction();
		}
		else{
			return this.stealAction();
		}
	}
	
	protected GroundedSingleAction punisherSelection(int policyStateIndex){
		if(this.historyPolicy[policyStateIndex] == 0){
			return this.nothingAction();
		}
		else{
			return this.punishAction();
		}
	}
	
	protected GroundedSingleAction forageAction(){
		return new GroundedSingleAction(this.worldAgentName, this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), "");
	}
	
	protected GroundedSingleAction stealAction(){
		return new GroundedSingleAction(this.worldAgentName, this.domain.getSingleAction(FSSimple.ACTIONSTEAL), "");
	}
	
	protected GroundedSingleAction nothingAction(){
		return new GroundedSingleAction(this.worldAgentName, this.domain.getSingleAction(FSSimple.ACTIONDONOTHING), "");
	}
	
	protected GroundedSingleAction punishAction(){
		this.iJustPunished = true;
		return new GroundedSingleAction(this.worldAgentName, this.domain.getSingleAction(FSSimple.ACTIONPUNISH), "");
	}

}
