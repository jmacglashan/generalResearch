package behavior.planning.vfa.td;

import behavior.planning.vfa.LookaheadVFATree;
import burlap.behavior.learningrate.ConstantLR;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.singleagent.vfa.ApproximationResult;
import burlap.behavior.singleagent.vfa.FunctionWeight;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.WeightGradient;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class GradientDescentTDLambdaLookahead extends OOMDPPlanner implements QComputablePlanner{


	protected ValueFunctionApproximation vfa;
	protected LookaheadVFATree lookaheadTree;
	protected LearningRate learningRate;
	protected double lambda;
	protected int numRollouts;
	protected int maxRolloutSize;
	protected Policy learningPolicy;
	protected int numPlanningSteps = 0;

	public GradientDescentTDLambdaLookahead(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, ValueFunctionApproximation vfa,
											double learningRate, double lambda, int numRollouts, int maxRolloutSize){

		this(domain, rf, tf, gamma, vfa, learningRate, lambda, numRollouts, maxRolloutSize, 1, -1);

	}

	public GradientDescentTDLambdaLookahead(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, ValueFunctionApproximation vfa,
											double learningRate, double lambda, int numRollouts, int maxRolloutSize, int treeDepth, int numSamples){

		this.plannerInit(domain, rf, tf, gamma, null);
		this.vfa = vfa;
		this.lookaheadTree = new LookaheadVFATree(domain, rf, tf, gamma, vfa, treeDepth, numSamples);
		this.lambda = lambda;
		this.learningRate = new ConstantLR(learningRate);
		this.learningPolicy = new EpsilonGreedy(this, 0.1);
		this.numRollouts = numRollouts;
		this.maxRolloutSize = maxRolloutSize;

	}


	@Override
	public void planFromState(State initialState) {


		int lastTraceSize = 100;
		int totalSteps = 0;
		for(int i = 0; i < this.numRollouts; i++){

			Map<Integer, EligibilityTraceVector> traces = new HashMap<Integer, EligibilityTraceVector>(lastTraceSize);

			State curState = initialState;
			int numSteps = 0;
			while(!this.tf.isTerminal(curState) && (numSteps < maxRolloutSize || maxRolloutSize == -1)){

				ApproximationResult curApprox = this.vfa.getStateValue(curState);
				WeightGradient gradient = this.vfa.getWeightGradient(curApprox);

				GroundedAction ga = (GroundedAction)this.learningPolicy.getAction(curState);
				State ns = ga.executeIn(curState);
				double r = this.rf.reward(curState, ga, ns);
				double nextV = 0.;
				if(!this.tf.isTerminal(ns)){
					nextV = this.vfa.getStateValue(ns).predictedValue;
				}

				double delta = r + this.gamma*nextV - curApprox.predictedValue;

				//first check for new traces
				for(FunctionWeight fw : curApprox.functionWeights){
					if(!traces.containsKey(fw.weightId())){
						traces.put(fw.weightId(), new EligibilityTraceVector(fw, 0.));
					}
				}

				//update eligibility traces and weight vector
				for(EligibilityTraceVector t : traces.values()){
					t.eligibilityValue = this.lambda*this.gamma*t.eligibilityValue + gradient.getPartialDerivative(t.weight.weightId());
					double lr = this.learningRate.pollLearningRate(totalSteps, t.weight.weightId());
					double newWeight = t.weight.weightValue() + lr*delta*t.eligibilityValue;
					t.weight.setWeight(newWeight);
				}


				totalSteps++;
				numSteps++;
				curState = ns;

			}

			DPrint.cl(76437, "Rollout " + i + ": " + numSteps);

			lastTraceSize = traces.size();

		}

		this.numPlanningSteps += totalSteps;
		DPrint.cl(76437, "total steps: " + this.numPlanningSteps);


	}

	@Override
	public void resetPlannerResults() {
		this.vfa.resetWeights();
	}

	@Override
	public List<QValue> getQs(State s) {
		return this.lookaheadTree.getQs(s);
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		return this.lookaheadTree.getQ(s, a);
	}


	/**
	 * An object for keeping track of the eligibility traces within an episode for each VFA weight
	 * @author James MacGlashan
	 *
	 */
	public static class EligibilityTraceVector{

		/**
		 * The VFA weight being traced
		 */
		public FunctionWeight weight;

		/**
		 * The eligibility value
		 */
		public double				eligibilityValue;

		/**
		 * The value of the weight when the trace started
		 */
		public double				initialWeightValue;


		/**
		 * Creates a trace for the given weight with the given eligibility value
		 * @param weight the VFA weight
		 * @param eligibilityValue the eligibility to assign to it.
		 */
		public EligibilityTraceVector(FunctionWeight weight, double eligibilityValue){
			this.weight = weight;
			this.eligibilityValue = eligibilityValue;
			this.initialWeightValue = weight.weightValue();
		}

	}
}
