package tests.stochasticgames;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;


/**
 * Returns a reward of 1 to any agent that reaches a goal location; 0 otherwise
 * @author James MacGlashan
 *
 */
public class GGJointRewardFunction implements JointReward {

	PropositionalFunction agentInPersonalGoal;
	PropositionalFunction agentInUniversalGoal;
	
	double defaultReward = 0.;
	double goalReward = 1.;
	
	public GGJointRewardFunction(Domain ggDomain){
		agentInPersonalGoal = ggDomain.getPropFunction(GridGame.PFINPGOAL);
		agentInUniversalGoal = ggDomain.getPropFunction(GridGame.PFINUGOAL);
	}
	
	@Override
	public Map<String, Double> reward(State s, JointAction ja, State sp) {
		
		Map <String, Double> rewards = new HashMap<String, Double>();
		
		//get all agents and initialize reward to default
		List <ObjectInstance> obs = sp.getObjectsOfClass(GridGame.CLASSAGENT);
		for(ObjectInstance o : obs){
			rewards.put(o.getName(), defaultReward);
		}
		
		
		//check for any agents that reached a personal goal location and give them a goal reward if they did
		List<GroundedProp> ipgps = agentInPersonalGoal.getAllGroundedPropsForState(sp);
		for(GroundedProp gp : ipgps){
			String agentName = gp.params[0];
			if(gp.isTrue(sp)){
				rewards.put(agentName, goalReward);
			}
		}
		
		
		//check for any agents that reached a universal goal location and give them a goal reward if they did
		List<GroundedProp> upgps =agentInUniversalGoal.getAllGroundedPropsForState(sp);
		for(GroundedProp gp : upgps){
			String agentName = gp.params[0];
			if(gp.isTrue(sp)){
				rewards.put(agentName, goalReward);
			}
		}
		
		return rewards;
		
	}

}
