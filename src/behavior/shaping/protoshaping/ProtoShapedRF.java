package behavior.shaping.protoshaping;


import behavior.shaping.ShapedRewardFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class ProtoShapedRF extends ShapedRewardFunction {

	protected ProtoFunction protoFunction;
	protected double discount;
	
	public ProtoShapedRF(RewardFunction baseRF, ProtoFunction protoFunction, double discount) {
		super(baseRF);
		this.protoFunction = protoFunction;
		this.discount = discount;
	}

	@Override
	public double additiveReward(State s, GroundedAction a, State sprime) {
		return (this.discount * this.protoFunction.protoValue(sprime)) - this.protoFunction.protoValue(s);
	}

}
