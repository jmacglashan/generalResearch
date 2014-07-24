package irl.mlirl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.datastructures.BoltzmannDistribution;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;

public abstract class DifferentiableVFPlanner extends ValueFunctionPlanner {

	protected Map <StateHashTuple, double[]>				valueGradient = new HashMap<StateHashTuple, double[]>();
	protected double										boltzBeta;
	
	
	
	protected double performBellmanUpdateOn(StateHashTuple sh){
	    
		if(this.tf.isTerminal(sh.s)){
			this.valueFunction.put(sh, 0.);
			return 0.;
		}
		
		List<QValue> qs = this.getQs(sh.s);
		double [] dqs = new double[qs.size()];
		for(int i = 0; i < qs.size(); i++){
			dqs[i] = qs.get(i).q;
		}
		BoltzmannDistribution bd = new BoltzmannDistribution(dqs, 1./this.boltzBeta);
		double [] dist = bd.getProbabilities();
		
		double sum = 0.;
		for(int i = 0; i < dqs.length; i++){
			sum += dqs[i] * dist[i];
		}
		
		this.valueFunction.put(sh, sum);
		
		return sum;
	}
	
	protected double [] performDPValueGradientUpdateOn(StateHashTuple sh){
    	//updates graient of value function for the given state using bellman-like method
		
		//prepare value function gradient double array
		int d = ((DifferentiableRF)this.rf).getParameterDimension();
		double gv [] = new double[d];
		for(int i = 0; i < d; i++){
			gv[i] = 0.;
		}
		
		//get q objects
		List<QValue> Qs = this.getQs(sh.s);
		double [] qs = new double[Qs.size()];
		for(int i = 0; i < Qs.size(); i++){
			qs[i] = Qs.get(i).q;
		}
		
		
		//get all q gradients
		double [][] gqs = new double[qs.length][d];
		for(int i = 0; i < qs.length; i++){
			double [] gq = this.getQGradient(sh.s, (GroundedAction)Qs.get(i).a).gradient;
			for(int j = 0; j < d; j++){
				gqs[i][j] = gq[j];
			}
		}
		
		double maxBetaScaled = this.maxBetaScaled(qs);
		double logSum = this.logSum(qs, maxBetaScaled);
		
		for(int i = 0; i < qs.length; i++){
			
			double probA = Math.exp(this.boltzBeta * qs[i] - logSum);
			double [] policyGradient = this.policyGradient(qs, maxBetaScaled, logSum, gqs, i);
			
			for(int j = 0; j < d; j++){
				gv[j] += (probA * gqs[i][j]) + qs[i] * policyGradient[j];
			}
			
		}
		
		this.valueGradient.put(sh, gv);
		
		return gv;
	}
	
	protected double [] policyGradient(double [] qs, double maxBetaScaled, double logSum, double [][] gqs, int aInd){
		
		int d = ((DifferentiableRF)this.rf).getParameterDimension();
		double [] pg = new double[d];
		
		double constantPart = this.boltzBeta * Math.exp(this.boltzBeta*qs[aInd] + maxBetaScaled - logSum - logSum);
		
		for(int i = 0; i < qs.length; i++){
			for(int j = 0; j < d; j++){
				pg[j] += (gqs[aInd][j] - gqs[i][j]) * Math.exp(this.boltzBeta * qs[i] - maxBetaScaled);
			}
		}
		
		for(int j = 0; j < d; j++){
			pg[j] *= constantPart;
		}
		
		/* this is wrong and the approach wont work anyway
		double [] upperExpSum = new double[pg.length];
		for(int i = 0; i < qs.length; i++){
			
			for(int j = 0; j < d; j++){
				upperExpSum[j] += this.boltzBeta * (gqs[aInd][j] - gqs[i][j]) * Math.exp(this.boltzBeta * qs[i] - maxBetaScaled);
				
			}
		}
		 

		for(int i = 0; i < d; i++){
			double upperLogSum = maxBetaScaled + Math.log(upperExpSum[i]);
			double totalLogged = (this.boltzBeta * qs[aInd]) + upperLogSum - (2*logSum);
			pg[i] = Math.exp(totalLogged);
			if(Double.isNaN(pg[i])){
				System.out.println("Error in policy gradient compute");
			}
			
		}
		*/
		
		return pg;
	}
	
	protected double maxBetaScaled(double [] qs){
		double max = Double.NEGATIVE_INFINITY;
		for(double q : qs){
			if(q > max){
				max = q;
			}
		}
		return this.boltzBeta*max;
	}
	
	protected double logSum(double [] qs, double maxBetaScaled){
		
		double expSum = 0.;
		for(int i = 0; i < qs.length; i++){
			expSum += Math.exp(this.boltzBeta * qs[i] - maxBetaScaled);
		}
		double v = maxBetaScaled + Math.log(expSum);
		return v;
		
	}
	
	protected double [] shiftedAndBetaScaledElements(double [] qs, double maxBetaScaled){
		
		double [] res = new double[qs.length];
		for(int i = 0; i < qs.length; i++){
			res[i] = this.boltzBeta * qs[i] - maxBetaScaled;
		}
		
		return res;
		
	}
	
	
	
	public double [] getValueGradient(State s){
        //returns deriviate value
		StateHashTuple sh = this.hashingFactory.hashState(s);
		double [] grad = this.valueGradient.get(sh);
		if(grad == null){
			grad = new double[((DifferentiableRF)this.rf).getParameterDimension()];
		}
		return grad;
	}
	
	public List<QGradientTuple> getAllQGradients(State s){
		List<GroundedAction> gas = this.getAllGroundedActions(s);
		List<QGradientTuple> res = new ArrayList<QGradientTuple>(gas.size());
		for(GroundedAction ga : gas){
			res.add(this.getQGradient(s, ga));
		}
		return res;
	}
	
	public QGradientTuple getQGradient(State s, GroundedAction a){

		double [] gradient = this.computeQGradient(s, a);
		QGradientTuple tuple = new QGradientTuple(s, a, gradient);
		return tuple;
	}
	
	
	
	
	protected double [] computeQGradient(State s, GroundedAction ga){
		
		int d = ((DifferentiableRF)this.rf).getParameterDimension();
		double [] gradient = new double[d];
		for(int i = 0; i < gradient.length; i++){
			gradient[i] = 0.;
		}
		
		List<TransitionProbability> tps = ga.action.getTransitions(s, ga.params);
		for(TransitionProbability tp : tps){
			double [] valueGradient = this.getValueGradient(tp.s);
			double [] rewardGradient = ((DifferentiableRF)this.rf).getGradient(s, ga, tp.s);
			for(int i = 0; i < gradient.length; i++){
				gradient[i] += tp.p * (rewardGradient[i] + this.gamma * valueGradient[i]);
			}
		}
		
		
		return gradient;
		
	}

}
