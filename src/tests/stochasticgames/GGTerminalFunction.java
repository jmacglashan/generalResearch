package tests.stochasticgames;

import java.util.List;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;

/**
 * Causes termination when any agent reaches a personal or universal goal location.
 * @author James MacGlashan
 *
 */
public class GGTerminalFunction implements TerminalFunction {

	PropositionalFunction agentInPersonalGoal;
	PropositionalFunction agentInUniversalGoal;
	
	public GGTerminalFunction(Domain ggDomain){
		agentInPersonalGoal = ggDomain.getPropFunction(GridGame.PFINPGOAL);
		agentInUniversalGoal = ggDomain.getPropFunction(GridGame.PFINUGOAL);
	}
	
	
	@Override
	public boolean isTerminal(State s) {
		
		//check personal goals; if anyone reached their personal goal, it's game over
		List<GroundedProp> ipgps = agentInPersonalGoal.getAllGroundedPropsForState(s);
		for(GroundedProp gp : ipgps){
			if(gp.isTrue(s)){
				return true;
			}
		}
		
		
		//check universal goals; if anyone reached a universal goal, it's game over
		List<GroundedProp> upgps = agentInUniversalGoal.getAllGroundedPropsForState(s);
		for(GroundedProp gp : upgps){
			if(gp.isTrue(s)){
				return true;
			}
		}
		
		return false;
	}

}
