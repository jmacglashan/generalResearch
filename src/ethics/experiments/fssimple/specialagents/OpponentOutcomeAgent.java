package ethics.experiments.fssimple.specialagents;

import java.util.Map;

import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleBTJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import ethics.experiments.fssimple.aux.FSSimpleSG;

public class OpponentOutcomeAgent extends Agent {

	
	/**
	 * state order: 
	 * 				0: (thief) punisher's back is turned
	 * 				1: (theif) punisher punished for theft
	 * 				2: (theif) punisher did not respond to theft
	 * 				3: (punisher) thief did not steal after punishment
	 * 				4: (punisher) theif did steal after punishment
	 * 
	 * value 0 corresponds to forage/do nothing
	 * value 1 correspodns to steal/punish
	 */
	protected int [] historyPolicy;
	
	protected double epsilon = 0.1;
	
	protected boolean thiefStoleAnyway = false;
	protected boolean iJustPunished = false;
	
	
	
	public OpponentOutcomeAgent(Domain domain, int [] historyPolicy){
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
					return this.nothingAction();
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
					if(this.thiefStoleAnyway){
						selection = this.punisherSelection(4);
					}
					else{
						selection = this.punisherSelection(3);
					}
				}
				else{
					selection = this.nothingAction();
				}
			}
			
		}
		
		
		return selection;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction, Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		int pNum = this.getMyPlayerNum(s);
		if(pNum == 1){
			//I am punisher
			
			//are we observing a subsequent action selection after my punishment?
			int sn = FSSimple.stateNode(s);
			if(sn != 2){
				
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
			
			
			
		}
		
	}

	@Override
	public void gameTerminated() {
		//do nothing
	}
	
	
	protected int getMyPlayerNum(State s){
		ObjectInstance me = s.getObject(this.worldAgentName);
		
		//which player am I
		int pNum = me.getDiscValForAttribute(FSSimple.ATTPN);
		
		return pNum;

	}
	
	
	protected boolean punisherBackIsTurned(State s){
		ObjectInstance punisher = null;
		for(ObjectInstance player : s.getObjectsOfTrueClass(FSSimple.CLASSPLAYER)){
			int pNum = player.getDiscValForAttribute(FSSimple.ATTPN);
			if(pNum == 1){
				punisher = player;
				break;
			}
		}
		
		return punisher.getDiscValForAttribute(FSSimple.ATTBACKTURNED) == 1;
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

	
	
	public static void main(String [] args){
		FSSimple gen = new FSSimple();
		SGDomain domain = (SGDomain)gen.generateDomain();
		JointActionModel jam = new FSSimpleBTJAM(0.0);
		JointReward r = new FSSimpleJR();
		AgentType at = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
		SGStateGenerator sg = new FSSimpleSG(domain);
		
		Agent a1 = new OpponentOutcomeAgent(domain, new int[]{1,0,1,1,0});
		Agent a2 = new OpponentOutcomeAgent(domain, new int[]{1,1,1,0,1});
		
		Agent human = new HumanAgent(domain);
		
		World w = new World(domain, jam, r, new NullTermination(), sg);
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		//a1.joinWorld(w, at);
		human.joinWorld(w, at);
		a2.joinWorld(w, at);
		
		w.runGame(50);
		
	}
	
}
