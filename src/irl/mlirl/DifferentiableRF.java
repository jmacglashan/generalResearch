package irl.mlirl;

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
        	  return fvGen.generateFeatureVectorFrom(s);
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

}
