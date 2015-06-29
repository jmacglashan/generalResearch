package tests.irltests;

/**
 * @author James MacGlashan.
 */

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * A "planning" algorithm that sets the value of the state to the reward function value. This is useeful
 * for visualizing the learned reward function weights from IRL.
 */
public class StateRewardFunctionValue implements QFunction {

	protected RewardFunction rf;
	protected Domain domain;

	public StateRewardFunctionValue(Domain domain, RewardFunction rf){
		this.rf = rf;
		this.domain = domain;
	}

	@Override
	public List<QValue> getQs(State s) {

		List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.domain.getActions(), s);
		List<QValue> qs = new ArrayList<QValue>(gas.size());
		for(GroundedAction ga : gas){
			double r = this.rf.reward(s, ga, s);
			qs.add(new QValue(s, ga, r));
		}

		return qs;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {

		double r = this.rf.reward(s, (GroundedAction)a, s);
		QValue q = new QValue(s, a, r);

		return q;
	}
}
