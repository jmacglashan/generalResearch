package behavior.planning.vfa;


import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class LookaheadVFATree implements QFunction{

	protected SparseSampling tree;

	public LookaheadVFATree(Domain domain, RewardFunction rf, TerminalFunction tf, double discount, ValueFunctionApproximation vfa, int depth, int numSamples){
		this(domain, rf, tf, discount, new NameDependentStateHashFactory(), vfa, depth, numSamples);
	}

	public LookaheadVFATree(Domain domain, RewardFunction rf, TerminalFunction tf, double discount, StateHashFactory hashingFactory, ValueFunctionApproximation vfa, int depth, int numSamples){
		ValueFunctionInitialization vinit = new VFAValueInit(vfa);
		this.tree = new SparseSampling(domain, rf, tf, discount, hashingFactory, depth, numSamples);
		this.tree.setForgetPreviousPlanResults(true);
		this.tree.setValueForLeafNodes(vinit);
		DPrint.toggleCode(this.tree.getDebugCode(), false);

	}

	@Override
	public List<QValue> getQs(State s) {
		return this.tree.getQs(s);
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		return this.tree.getQ(s, a);
	}


	public void resetPlannerResults(){
		this.tree.resetPlannerResults();
	}


}
