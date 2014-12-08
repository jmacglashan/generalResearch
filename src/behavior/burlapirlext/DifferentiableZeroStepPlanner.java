package behavior.burlapirlext;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.QGradientPlanner;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.QGradientTuple;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class DifferentiableZeroStepPlanner extends OOMDPPlanner implements QGradientPlanner {


	public DifferentiableZeroStepPlanner(Domain domain, DifferentiableRF rf){
		this.domain = domain;
		this.rf = rf;
	}


	@Override
	public List<QGradientTuple> getAllQGradients(State s) {
		List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.domain.getActions(), s);
		List<QGradientTuple> qs = new ArrayList<QGradientTuple>(gas.size());
		for(GroundedAction ga : gas){
			qs.add(this.getQGradient(s, ga));
		}
		return qs;
	}

	@Override
	public QGradientTuple getQGradient(State s, GroundedAction a) {
		return new QGradientTuple(s, a, ((DifferentiableRF)this.rf).getGradient(s, a, null));
	}

	@Override
	public void setBoltzmannBetaParameter(double beta) {
		//no need to do anything since value function is never explicitly computed
	}

	@Override
	public List<QValue> getQs(State s) {
		List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.domain.getActions(), s);
		List<QValue> qs = new ArrayList<QValue>(gas.size());
		for(GroundedAction ga : gas){
			qs.add(this.getQ(s, ga));
		}
		return qs;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		return new QValue(s, a, this.rf.reward(s, (GroundedAction)a, null));
	}

	@Override
	public void planFromState(State initialState) {
		//do nothing
	}

	@Override
	public void resetPlannerResults() {
		//do nothing
	}
}
