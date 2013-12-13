package domain.stocasticgames.foragesteal;

import java.util.HashMap;
import java.util.Map;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;

public class TBFSStandardReward implements JointReward {

	public double				stealerReward;
	public double				stealeeReward;
	
	public double				puncherReward;
	public double				puncheeReward;
	
	public double []			forageRewards;
	
	
	public TBFSStandardReward(){
		this(1, -1, -.1, -2, new double[]{-1, -.5, .5, 1., 1.5});
	}
	
	public TBFSStandardReward(double stealerReward, double stealeeReward, double puncherReward, double puncheeReward, double [] forageReward){
		this.stealerReward = stealerReward;
		this.stealeeReward = stealeeReward;
		
		this.puncherReward = puncherReward;
		this.puncheeReward = puncheeReward;
		
		this.forageRewards = forageReward;
	}
	
	
	@Override
	public Map<String, Double> reward(State s, JointAction ja, State sp) {
		
		
		Map<String, Double> rewards = new HashMap<String, Double>();
		
		GroundedSingleAction agentForTurnAction = null;
		GroundedSingleAction nonActingAgentAction = null;
		for(GroundedSingleAction gsa : ja){
			ObjectInstance ao = s.getObject(gsa.actingAgent);
			
			int turn = ao.getDiscValForAttribute(TBForageSteal.ATTISTURN);
			if(turn == 1){
				agentForTurnAction = gsa;
			}
			else{
				nonActingAgentAction = gsa;
			}
		}
		
		
		if(agentForTurnAction == null){
			for(GroundedSingleAction gsa : ja){
				rewards.put(gsa.actingAgent, 0.);
			}
		}
		else{
			
			double actingReward = 0.;
			double nonActingReward = 0;
			
			String actionName = agentForTurnAction.action.actionName;
			
			if(actionName.equals(TBForageSteal.ACTIONSTEAL)){
				actingReward += stealerReward;
				nonActingReward += stealeeReward;
			}
			else if(actionName.equals(TBForageSteal.ACTIONPUNCH)){
				actingReward += puncherReward;
				nonActingReward += puncheeReward;
			}
			else if(actionName.startsWith(TBForageSteal.ACTIONFORAGE)){
				//which forage is it?
				String endActionName = actionName.substring(TBForageSteal.ACTIONFORAGE.length());
				int falt = Integer.parseInt(endActionName);
				actingReward += forageRewards[falt];
			}
			
			
			rewards.put(agentForTurnAction.actingAgent, actingReward);
			if(nonActingAgentAction != null){
				rewards.put(nonActingAgentAction.actingAgent, nonActingReward);
			}
			
		}
		
		
		return rewards;
	}

}
