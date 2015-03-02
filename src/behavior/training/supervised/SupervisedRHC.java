package behavior.training.supervised;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SupervisedRHC extends Policy{


	protected StateToFeatureVectorGenerator fvgen;
	protected Classifier classifier;
	protected Domain domain;
	protected RewardFunction rf;
	protected TerminalFunction tf;
	protected SparseSampling ss;
	protected Policy p;

	protected int debugCode = 2324;


	public SupervisedRHC(Domain domain, RewardFunction rf, TerminalFunction tf, double discount, int h, int c, StateToFeatureVectorGenerator fvgen, Classifier classifier){
		this.fvgen = fvgen;
		this.domain = domain;
		this.rf = rf;
		this.tf = tf;
		this.classifier = classifier;

		this.ss = new SparseSampling(domain, rf, tf, discount, new NameDependentStateHashFactory(), h, c);
		this.ss.setValueForLeafNodes(new WekaVFInit());
		this.p = new GreedyQPolicy(this.ss);

	}

	public SupervisedRHC(Domain domain, RewardFunction rf, TerminalFunction tf, double discount, int h, int c, StateToFeatureVectorGenerator fvgen, Classifier classifier, List <EpisodeAnalysis> trainingEpisodes){
		this.fvgen = fvgen;
		this.domain = domain;
		this.rf = rf;
		this.tf = tf;
		this.classifier = classifier;

		Instances dataset = ToWekaRegessionInstance.getTrainingInstances(trainingEpisodes, fvgen, rf, discount);
		try {
			classifier.buildClassifier(dataset);
		} catch (Exception e) {
			throw new RuntimeException("Could not build weka classifier; weka error: " + e.getMessage());
		}

		try {
			Evaluation eval = new Evaluation(dataset);
			eval.evaluateModel(classifier, dataset);
			DPrint.cl(this.debugCode, "Finished building classifier with training data evaluation:\n" + eval.toSummaryString());
		} catch (Exception e) {
			System.out.println("Could not evaluate trained weka classifier, but it built it. Evaluation error: " + e.getMessage());
		}

		System.out.println("coefficients:\n" + Arrays.toString(((LinearRegression)classifier).coefficients()));
		System.out.println(((LinearRegression) classifier).toString());

		this.ss = new SparseSampling(domain, rf, tf, discount, new NameDependentStateHashFactory(), h, c);
		this.ss.setValueForLeafNodes(new WekaVFInit());
		this.p = new GreedyQPolicy(this.ss);

		DPrint.toggleCode(this.ss.getDebugCode(), false);
	}


	public StateToFeatureVectorGenerator getFvgen() {
		return fvgen;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	public Domain getDomain() {
		return domain;
	}

	public RewardFunction getRf() {
		return rf;
	}

	public TerminalFunction getTf() {
		return tf;
	}

	public SparseSampling getSs() {
		return ss;
	}

	public int getDebugCode() {
		return debugCode;
	}

	@Override
	public AbstractGroundedAction getAction(State s) {
		return this.p.getAction(s);
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		return this.p.getActionDistributionForState(s);
	}

	@Override
	public boolean isStochastic() {
		return this.p.isStochastic();
	}

	@Override
	public boolean isDefinedFor(State s) {
		return true;
	}



	public class WekaVFInit implements ValueFunctionInitialization{




		@Override
		public double value(State s) {

			Instance inst = ToWekaRegessionInstance.getInstance(s, fvgen, 0., null);
			double val = 0.;
			try {
				val = classifier.classifyInstance(inst);
			} catch (Exception e) {
				throw new RuntimeException("Could not evaluate weka classifier on state; weka error: " + e.getMessage());
			}

			return val;
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return this.value(s);
		}
	}
}
