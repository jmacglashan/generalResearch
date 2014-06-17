package ethics.experiments.fssimple.auxiliary;

import java.util.Map;

import domain.stocasticgames.foragesteal.simple.FSSimple;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.StateAbstraction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.WorldObserver;

public class PseudoGameCountWorld extends World {

	protected StateConditionTest	pseudoTerminal;
	
	public PseudoGameCountWorld(SGDomain domain, JointActionModel jam, JointReward jr, TerminalFunction tf, SGStateGenerator sg, StateConditionTest pt) {
		super(domain, jam, jr, tf, sg);
		this.pseudoTerminal = pt;
	}
	
	public PseudoGameCountWorld(SGDomain domain, JointActionModel jam, JointReward jr, TerminalFunction tf, SGStateGenerator sg, 
			StateAbstraction abstractionForAgents, StateConditionTest pt){
		super(domain, jam, jr, tf, sg, abstractionForAgents);
		this.pseudoTerminal = pt;
	}
	
	
	/**
	 * Runs a game until a terminal state is hit for maxStages have occurred
	 * @param maxStages the maximum number of stages to play in the game before its forced to end.
	 */
	public void runGame(int maxStages, int maxPsuedoGames){
		
		for(Agent a : agents){
			a.gameStarting();
		}
		
		currentState = initialStateGenerator.generateState(agents);
		int t = 0;
		int pg = 0;
		
		while(!tf.isTerminal(currentState) && t < maxStages && pg < maxPsuedoGames){
			this.runStage();
			t++;
			if(this.pseudoTerminal.satisfies(currentState)){
				pg++;
			}
		}
		
		for(Agent a : agents){
			a.gameTerminated();
		}
		
		DPrint.cl(debugId, currentState.getCompleteStateDescription());
		
	}
	

}
