package irl.mlirl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public abstract class DifferentiableRF implements RewardFunction {
	
protected double []							parameters;
protected int									dim;

public abstract double [] getGradient(State s, GroundedAction ga, State sp);

	public void setParameters(double [] parameters){
		this.parameters = parameters;
	}

	public void setParameter(int i, double w){
		this.parameters[i] = w;
	}

	public int getParameterDimension(){
		return this.dim;
	}

	public double [] getParameters(){
		return this.parameters;
	}

	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < this.parameters.length; i++){
			if(i > 0){
				buf.append(", ");
			}
			buf.append(this.parameters[i]);
		}
		
		return buf.toString();
	}



	public static class LinearStateDifferentiableRF extends DifferentiableRF{
	
			protected boolean 							featuresAreForNextState = true;
			protected StateToFeatureVectorGenerator		fvGen;
			
		public LinearStateDifferentiableRF(StateToFeatureVectorGenerator fvGen, int dim){
			this.dim = dim;
			this.parameters = new double[dim];
			this.fvGen = fvGen;
		}
		
		public LinearStateDifferentiableRF(StateToFeatureVectorGenerator fvGen, int dim, boolean featuresAreForNextState){
			this.featuresAreForNextState = featuresAreForNextState;
			this.dim = dim;
			this.parameters = new double[dim];
			this.fvGen = fvGen;
		}
		
		public double [] getGradient(State s, GroundedAction ga, State sp){
			if(featuresAreForNextState){
				return fvGen.generateFeatureVectorFrom(sp);
			}
			else{
				return fvGen.generateFeatureVectorFrom(s);
			}
		}
	
		@Override
		public double reward(State s, GroundedAction a, State sprime){
			double [] features;
			if(this.featuresAreForNextState){
				 features = fvGen.generateFeatureVectorFrom(sprime);
			}
			else{
				features = fvGen.generateFeatureVectorFrom(s);
			}
			double sum = 0.;
			for(int i = 0; i < features.length; i++){
				sum += features[i] * this.parameters[i];
			}
			return sum;
		}
	
	}
	
	
	public static class LinearStateActionDifferentiableRF extends DifferentiableRF{
	
		Map<GroundedAction, Integer> 				actionMap;
		protected StateToFeatureVectorGenerator		fvGen;
		protected int 								numStateFeatures;
		int 										numActions = 0;
		
		
		public LinearStateActionDifferentiableRF(StateToFeatureVectorGenerator stateFeatures, int numStateFeatures, GroundedAction...allPossibleActions){
			this.fvGen = stateFeatures;
			this.numStateFeatures = numStateFeatures;
			this.actionMap = new HashMap<GroundedAction, Integer>(allPossibleActions.length);
			for(int i = 0; i < allPossibleActions.length; i++){
				this.actionMap.put(allPossibleActions[i], i);
			}
			this.numActions = allPossibleActions.length;
			this.parameters = new double[numActions*this.numStateFeatures];
			this.dim = this.numActions*this.numStateFeatures;
		}
		
		
		public void addAction(GroundedAction ga){
			this.actionMap.put(ga, this.numActions);
			this.numActions++;
			this.parameters = new double[numActions*this.numStateFeatures];
			this.dim = this.numActions*this.numStateFeatures;
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			double [] sFeatures = this.fvGen.generateFeatureVectorFrom(s);
			int sIndex = this.actionMap.get(a) * this.numStateFeatures;
			double sum = 0.;
			for(int i = sIndex; i < sIndex + this.numStateFeatures; i++){
				sum += this.parameters[i]*sFeatures[i-sIndex];
			}
			return sum;
		}
		@Override
		public double[] getGradient(State s, GroundedAction ga, State sp) {
			double [] sFeatures = this.fvGen.generateFeatureVectorFrom(s);
			int sIndex = this.actionMap.get(ga) * this.numStateFeatures;
			double [] gradient = new double[this.numStateFeatures*this.numActions];
			this.copyInto(sFeatures, gradient, sIndex);
			
			return gradient;
		}
		
		
		protected void copyInto(double [] source, double [] target, int index){
			for(int i = index; i < index + source.length; i++){
				target[i] = source[i-index];
			}
		}
		
		
	}

}
