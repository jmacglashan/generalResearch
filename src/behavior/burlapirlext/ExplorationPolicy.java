package behavior.burlapirlext;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class ExplorationPolicy extends Policy{

	protected Policy sourcePolicy;
	protected DiffExpSS planner;

	public ExplorationPolicy(Policy sourcePolicy, DiffExpSS planner) {
		this.sourcePolicy = sourcePolicy;
		this.planner = planner;
	}

	@Override
	public AbstractGroundedAction getAction(State s) {
		boolean oldSetting = this.planner.isReturnUnshapedQs();
		this.planner.setReturnUnshapedQs(false);
		AbstractGroundedAction action = this.sourcePolicy.getAction(s);
		this.planner.setReturnUnshapedQs(oldSetting);
		return action;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		boolean oldSetting = this.planner.isReturnUnshapedQs();
		this.planner.setReturnUnshapedQs(false);
		List<ActionProb> probs = this.sourcePolicy.getActionDistributionForState(s);
		this.planner.setReturnUnshapedQs(oldSetting);
		return probs;
	}

	@Override
	public boolean isStochastic() {
		return this.sourcePolicy.isStochastic();
	}

	@Override
	public boolean isDefinedFor(State s) {
		return this.sourcePolicy.isDefinedFor(s);
	}
}
