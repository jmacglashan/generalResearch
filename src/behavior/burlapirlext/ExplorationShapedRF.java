package behavior.burlapirlext;

import behavior.shaping.ShapedRewardFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

/**
 * @author James MacGlashan.
 */
public class ExplorationShapedRF extends ShapedRewardFunction{

	protected VisitationCount visitations;
	protected VisitationRewardBonus bonus;

	public ExplorationShapedRF(RewardFunction baseRF, VisitationCount visitations, VisitationRewardBonus bonus) {
		super(baseRF);
		this.visitations = visitations;
		this.bonus = bonus;
	}

	public VisitationCount getVisitations() {
		return visitations;
	}

	public void setVisitations(VisitationCount visitations) {
		this.visitations = visitations;
	}

	public VisitationRewardBonus getBonus() {
		return bonus;
	}

	public void setBonus(VisitationRewardBonus bonus) {
		this.bonus = bonus;
	}

	@Override
	public double additiveReward(State s, GroundedAction a, State sprime) {
		return this.bonus.bonus(this.visitations.visitation(sprime));
	}
}
