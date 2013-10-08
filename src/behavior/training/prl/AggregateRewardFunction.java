package behavior.training.prl;

import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class AggregateRewardFunction implements RewardFunction {

	protected List<RewardFunction>			rfs;
	protected double						defaultReward = 0.;
	
	public AggregateRewardFunction() {
		rfs = new ArrayList<RewardFunction>();
	}
	
	public void addRF(RewardFunction rf){
		this.rfs.add(rf);
	}

	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		
		if(this.rfs.size() == 0){
			return defaultReward;
		}
		
		double sumR = 0.;
		for(RewardFunction rf : this.rfs){
			sumR += rf.reward(s, a, sprime);
		}
		
		return sumR;
	}

}
