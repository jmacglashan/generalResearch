package behavior.training.supervised;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class WekaPolicy extends Policy{

	protected StateToFeatureVectorGenerator fvgen;
	protected Classifier classifier;
	protected List <Action> actions;
	protected boolean isStochastic = false;

	protected int debugCode = 523648;

	public WekaPolicy(StateToFeatureVectorGenerator fvgen, Classifier classifier, Domain domain){
		this.fvgen = fvgen;
		this.classifier = classifier;
		this.actions = domain.getActions();
	}

	public WekaPolicy(StateToFeatureVectorGenerator fvgen, Classifier classifier, List<Action> actions){
		this.fvgen = fvgen;
		this.classifier = classifier;
		this.actions = actions;
	}

	public WekaPolicy(StateToFeatureVectorGenerator fvgen, Classifier classifier, List<Action> actions, List <EpisodeAnalysis> trainingEpisode){

		this.fvgen = fvgen;
		this.classifier = classifier;
		this.actions = actions;

		Instances trainingDataset = ToWekaInstance.getTrainingDataset(trainingEpisode, fvgen, actions);
		DPrint.cl(this.debugCode, "Building weka classifier...");
		try {
			classifier.buildClassifier(trainingDataset);
		} catch (Exception e) {
			throw new RuntimeException("Could not build weka classifier; weka error: " + e.getMessage());
		}

		try {
			Evaluation eval = new Evaluation(trainingDataset);
			eval.evaluateModel(classifier, trainingDataset);
			DPrint.cl(this.debugCode, "Finished building classifier with training data evaluation:\n" + eval.toSummaryString());
		} catch (Exception e) {
			System.out.println("Could not evaluate trained weka classifier, but it built it. Evaluation error: " + e.getMessage());
		}


	}


	public void setStochastic(boolean isStochastic) {
		this.isStochastic = isStochastic;
	}

	public StateToFeatureVectorGenerator getFvgen() {
		return fvgen;
	}

	public void setFvgen(StateToFeatureVectorGenerator fvgen) {
		this.fvgen = fvgen;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	public List<Action> getActions() {
		return actions;
	}

	public void setActions(List<Action> actions) {
		this.actions = actions;
	}


	public int getDebugCode() {
		return debugCode;
	}

	public void setDebugCode(int debugCode) {
		this.debugCode = debugCode;
	}

	@Override
	public AbstractGroundedAction getAction(State s) {

		Instance testInst = ToWekaInstance.genInstance(s, this.fvgen, this.actions);

		if(!this.isStochastic) {

			try {
				int aInd = (int)this.classifier.classifyInstance(testInst);
				return new GroundedAction(this.actions.get(aInd), "");
			} catch (Exception e) {
				throw new RuntimeException("Could not predict action for state; Weka error: " + e.getMessage());
			}

		}

		else{
			return this.sampleFromActionDistribution(s);
		}

	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {

		Instance testInst = ToWekaInstance.genInstance(s, this.fvgen, this.actions);

		if(!this.isStochastic){

			try {
				int aInd = (int)this.classifier.classifyInstance(testInst);
				List <ActionProb> aps = new ArrayList<ActionProb>(1);
				aps.add(new ActionProb(new GroundedAction(this.actions.get(aInd), ""), 1.));
				return aps;
			} catch (Exception e) {
				throw new RuntimeException("Could not predict action distribution for state; Weka error: " + e.getMessage());
			}

		}
		else{

			try {
				double [] predictDist = this.classifier.distributionForInstance(testInst);
				List<ActionProb> aps = new ArrayList<ActionProb>(predictDist.length);
				for(int i = 0 ; i < predictDist.length; i++){
					aps.add(new ActionProb(new GroundedAction(this.actions.get(i), ""), predictDist[i]));
				}
				return aps;
			} catch (Exception e) {
				throw new RuntimeException("Could not predict action distribution for state; Weka error: " + e.getMessage());
			}

		}

	}

	@Override
	public boolean isStochastic() {
		return this.isStochastic;
	}

	@Override
	public boolean isDefinedFor(State s) {
		return true;
	}
}
