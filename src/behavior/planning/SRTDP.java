package behavior.planning;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.stochastic.rtdp.BoundedRTDP;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class SRTDP extends BoundedRTDP{


	/**
	 * The shaped value function
	 */
	protected Map<StateHashTuple, Double> shapedV = new HashMap<StateHashTuple, Double>();


	/**
	 * The shaped value function initialization
	 */
	protected ValueFunctionInitialization		shapedVInit;


	protected double							maxShapedDiff;

	protected int								curValueFunction = 0;

	protected boolean							gapUsesShaped = true;



	public SRTDP(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory,
				 ValueFunctionInitialization lowerVInit, ValueFunctionInitialization shapedVInit, ValueFunctionInitialization upperVInit,
				 double maxShapedDiff, double maxDiff, int maxRollouts) {
		super(domain, rf, tf, gamma, hashingFactory, lowerVInit, upperVInit, maxDiff, maxRollouts);
		this.shapedVInit = shapedVInit;
		this.maxShapedDiff = maxShapedDiff;
	}


	/**
	 * Sets the value function to use to be the lower bound.
	 */
	public void setValueFunctionToShaped(){
		this.valueFunction = this.shapedV;
		this.valueInitializer = this.shapedVInit;
		this.curValueFunction = 1;

	}

	/**
	 * Sets the value function to use to be the upper bound.
	 */
	@Override
	public void setValueFunctionToUpperBound(){
		this.valueFunction = this.upperBoundV;
		this.valueInitializer = this.upperVInit;
		this.curValueFunction = 2;
	}


	/**
	 * Sets the value function to use to be the lower bound.
	 */
	@Override
	public void setValueFunctionToLowerBound(){
		this.valueFunction = this.lowerBoundV;
		this.valueInitializer = this.lowerVInit;
		this.curValueFunction = 0;
	}




	/**
	 * Runs a planning rollout from the provided state.
	 * @param s the initial state from which a planning rollout should be performed.
	 * @return the margin between the lower bound and upper bound value function for the initial state.
	 */
	public double runRollout(State s){
		LinkedList<StateHashTuple> trajectory = new LinkedList<StateHashTuple>();

		StateHashTuple csh = this.hashingFactory.hashState(s);

		while(!this.tf.isTerminal(csh.s) && (trajectory.size() < this.maxDepth+1 || this.maxDepth == -1)){

			if(this.runRolloutsInReverse){
				trajectory.offerFirst(csh);
			}

			this.gapUsesShaped = true;
			double sgap = this.getGap(csh);
			boolean useShaped = sgap > this.maxShapedDiff;

			this.setValueFunctionToLowerBound();
			QValue mxL = this.maxQ(csh.s);
			this.lowerBoundV.put(csh, mxL.q);

			if(useShaped){
				this.setValueFunctionToShaped();
			}
			else {
				this.gapUsesShaped = false;
				this.setValueFunctionToUpperBound();
			}
			QValue mxU = this.maxQ(csh.s);
			this.valueFunction.put(csh, mxU.q);

			numBellmanUpdates += 2;
			this.numSteps++;

			StateSelectionAndExpectedGap select = this.getNextState(csh.s, (GroundedAction)mxU.a);
			csh = select.sh;

			if(!useShaped && select.expectedGap < this.maxDiff){
				break;
			}


		}

		if(this.tf.isTerminal(csh.s)){
			this.lowerBoundV.put(csh, 0.);
			this.upperBoundV.put(csh, 0.);
			this.shapedV.put(csh, 0.);
		}


		double lastGap = 0.;

		//run in reverse
		if(this.runRolloutsInReverse){
			while(trajectory.size() > 0){
				StateHashTuple sh = trajectory.pop();
				this.setValueFunctionToLowerBound();
				QValue mxL = this.maxQ(sh.s);
				this.lowerBoundV.put(sh, mxL.q);

				this.setValueFunctionToShaped();
				QValue mxS = this.maxQ(sh.s);
				this.shapedV.put(sh, mxS.q);

				this.setValueFunctionToUpperBound();
				QValue mxU = this.maxQ(sh.s);
				this.upperBoundV.put(sh, mxU.q);

				numBellmanUpdates += 3;
				lastGap = mxU.q - mxL.q;

			}
		}
		else{
			lastGap = this.getGap(this.hashingFactory.hashState(s));
		}


		if(this.defaultToLowerValueAfterPlanning){
			this.setValueFunctionToLowerBound();
		}
		else{
			this.setValueFunctionToUpperBound();
		}

		return lastGap;

	}



	/**
	 * Returns the lower bound and upper bound value function margin/gap for the given state
	 * @param sh the state whose margin should be returned.
	 * @return the lower bound and upper bound value function margin/gap for the given state
	 */
	@Override
	protected double getGap(StateHashTuple sh){
		this.setValueFunctionToLowerBound();
		double l = this.value(sh);
		if(this.gapUsesShaped){
			this.setValueFunctionToShaped();
		}
		else{
			this.setValueFunctionToUpperBound();
		}
		double u = this.value(sh);
		double gap = u-l;
		return gap;
	}





}
