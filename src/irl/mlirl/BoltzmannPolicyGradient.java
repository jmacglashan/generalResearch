package irl.mlirl;

import java.util.List;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class BoltzmannPolicyGradient {

	public static double [] computeBoltzmannPolicyGradient(State s, GroundedAction a, QGradientPlanner planner, double beta){
		
		DifferentiableRF rf = (DifferentiableRF)((OOMDPPlanner)planner).getRF();
		int d = rf.getParameterDimension();
		
		double gv [] = new double[d];
		for(int i = 0; i < d; i++){
			gv[i] = 0.;
		}
		
		//get q objects
		List<QValue> Qs = planner.getQs(s);
		double [] qs = new double[Qs.size()];
		for(int i = 0; i < Qs.size(); i++){
			qs[i] = Qs.get(i).q;
		}
		
		//find matching action index
		int aind = -1;
		for(int i = 0; i < Qs.size(); i++){
			if(Qs.get(i).a.equals(a)){
				aind = i;
				break;
			}
		}
		
		if(aind == -1){
			throw new RuntimeException("Error: Could not find query action in Q-value list.");
		}
		
		//get all q gradients
		double [][] gqs = new double[qs.length][d];
		for(int i = 0; i < qs.length; i++){
			double [] gq = planner.getQGradient(s, (GroundedAction)Qs.get(i).a).gradient;
			for(int j = 0; j < d; j++){
				gqs[i][j] = gq[j];
			}
		}
		
		double maxBetaScaled = maxBetaScaled(qs, beta);
		double logSum = logSum(qs, maxBetaScaled, beta);
		
		double [] policyGradient = computePolicyGradient(rf, beta, qs, maxBetaScaled, logSum, gqs, aind);
		
		return policyGradient;
		
	}
	
	public static double [] computePolicyGradient(DifferentiableRF rf, double beta, double [] qs, double maxBetaScaled, double logSum, double [][] gqs, int aInd){
		
		int d = rf.getParameterDimension();
		double [] pg = new double[d];
		
		double constantPart = beta * Math.exp(beta*qs[aInd] + maxBetaScaled - logSum - logSum);
		
		for(int i = 0; i < qs.length; i++){
			for(int j = 0; j < d; j++){
				pg[j] += (gqs[aInd][j] - gqs[i][j]) * Math.exp(beta * qs[i] - maxBetaScaled);
			}
		}
		
		for(int j = 0; j < d; j++){
			pg[j] *= constantPart;
		}
		
		
		return pg;
	}
	
	public static double maxBetaScaled(double [] qs, double beta){
		double max = Double.NEGATIVE_INFINITY;
		for(double q : qs){
			if(q > max){
				max = q;
			}
		}
		return beta*max;
	}
	
	
	
	
	public static double logSum(double [] qs, double maxBetaScaled, double beta){
		
		double expSum = 0.;
		for(int i = 0; i < qs.length; i++){
			expSum += Math.exp(beta * qs[i] - maxBetaScaled);
		}
		double v = maxBetaScaled + Math.log(expSum);
		return v;
		
	}
	
}
