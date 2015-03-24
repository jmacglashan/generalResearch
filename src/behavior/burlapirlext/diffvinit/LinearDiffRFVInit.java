package behavior.burlapirlext.diffvinit;

import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

/**
 * @author James MacGlashan.
 */
public class LinearDiffRFVInit extends DifferentiableRF implements DifferentiableVInit{

	/**
	 * Whether features are based on the next state or previous state. Default is for the next state (true).
	 */
	protected boolean 								rfFeaturesAreForNextState = true;

	/**
	 * The state feature vector generator.
	 */
	protected StateToFeatureVectorGenerator 		rfFvGen;


	/**
	 * The state feature vector generator.
	 */
	protected StateToFeatureVectorGenerator 		vinitFvGen;


	protected int 									rfDim;

	protected int									vinitDim;


	public LinearDiffRFVInit(StateToFeatureVectorGenerator rfFvGen, StateToFeatureVectorGenerator vinitFvGen, int rfDim, int vinitDim) {
		this.rfFvGen = rfFvGen;
		this.vinitFvGen = vinitFvGen;
		this.rfDim = rfDim;
		this.vinitDim = vinitDim;

		this.dim = rfDim + vinitDim;
		this.parameters = new double[this.dim];

	}


	public LinearDiffRFVInit(StateToFeatureVectorGenerator rfFvGen, StateToFeatureVectorGenerator vinitFvGen, int rfDim, int vinitDim, boolean rfFeaturesAreForNextState) {
		this.rfFvGen = rfFvGen;
		this.vinitFvGen = vinitFvGen;
		this.rfDim = rfDim;
		this.vinitDim = vinitDim;
		this.rfFeaturesAreForNextState = rfFeaturesAreForNextState;

		this.dim = rfDim + vinitDim;
		this.parameters = new double[this.dim];

	}

	public boolean isRfFeaturesAreForNextState() {
		return rfFeaturesAreForNextState;
	}

	public void setRfFeaturesAreForNextState(boolean rfFeaturesAreForNextState) {
		this.rfFeaturesAreForNextState = rfFeaturesAreForNextState;
	}

	public StateToFeatureVectorGenerator getRfFvGen() {
		return rfFvGen;
	}

	public void setRfFvGen(StateToFeatureVectorGenerator rfFvGen) {
		this.rfFvGen = rfFvGen;
	}

	public StateToFeatureVectorGenerator getVinitFvGen() {
		return vinitFvGen;
	}

	public void setVinitFvGen(StateToFeatureVectorGenerator vinitFvGen) {
		this.vinitFvGen = vinitFvGen;
	}

	public int getRfDim() {
		return rfDim;
	}

	public void setRfDim(int rfDim) {
		this.rfDim = rfDim;
	}

	public int getVinitDim() {
		return vinitDim;
	}

	public void setVinitDim(int vinitDim) {
		this.vinitDim = vinitDim;
	}

	@Override
	public double[] getGradient(State s, GroundedAction ga, State sp) {

		double sfeatures [];

		if(rfFeaturesAreForNextState){
			sfeatures = rfFvGen.generateFeatureVectorFrom(sp);
		}
		else{
			sfeatures = rfFvGen.generateFeatureVectorFrom(s);
		}

		double [] allFeatures = new double [this.dim];
		for(int i = 0; i < this.rfDim; i++){
			allFeatures[i] = sfeatures[i];
		}


		return allFeatures;
	}

	@Override
	protected DifferentiableRF copyHelper() {
		return null;
	}

	@Override
	public double reward(State s, GroundedAction a, State sprime) {

		double [] features;
		if(this.rfFeaturesAreForNextState){
			features = this.rfFvGen.generateFeatureVectorFrom(sprime);
		}
		else{
			features = this.rfFvGen.generateFeatureVectorFrom(s);
		}
		double sum = 0.;
		for(int i = 0; i < features.length; i++){
			sum += features[i] * this.parameters[i];
		}
		return sum;

	}


	@Override
	public double[] getVGradient(State s) {

		double [] vFeatures = this.vinitFvGen.generateFeatureVectorFrom(s);

		double [] allFeatures = new double[this.dim];
		for(int i = 0; i < vFeatures.length; i++){
			allFeatures[i+this.rfDim] = vFeatures[i];
		}

		return allFeatures;
	}

	@Override
	public double[] getQGradient(State s, AbstractGroundedAction ga) {
		return this.getVGradient(s);
	}

	@Override
	public double value(State s) {
		double [] features = this.vinitFvGen.generateFeatureVectorFrom(s);

		double sum = 0.;
		for(int i = 0; i < features.length; i++){
			sum += features[i] * this.parameters[i+this.rfDim];
		}
		return sum;
	}

	@Override
	public double qValue(State s, AbstractGroundedAction a) {
		return this.value(s);
	}
}
