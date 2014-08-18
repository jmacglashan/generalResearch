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

public abstract class DifferentiableVFPlanner extends ValueFunctionPlanner implements QGradientPlanner {

	protected Map <StateHashTuple, double[]>				valueGradient = new HashMap<StateHashTuple, double[]>();
	protected double										boltzBeta;
	
	
	@Override
	public void resetPlannerResults(){
		super.resetPlannerResults();
		this.valueGradient.clear();
	}
	
	
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
    	//updates gradient of value function for the given state using bellman-like method
		
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
		
		double maxBetaScaled = BoltzmannPolicyGradient.maxBetaScaled(qs, this.boltzBeta);
		double logSum = BoltzmannPolicyGradient.logSum(qs, maxBetaScaled, this.boltzBeta);
		
		for(int i = 0; i < qs.length; i++){
			
			double probA = Math.exp(this.boltzBeta * qs[i] - logSum);
			double [] policyGradient = BoltzmannPolicyGradient.computePolicyGradient((DifferentiableRF)this.rf, this.boltzBeta, qs, maxBetaScaled, logSum, gqs, i);
			
			for(int j = 0; j < d; j++){
				gv[j] += (probA * gqs[i][j]) + qs[i] * policyGradient[j];
			}
			
		}
		
		this.valueGradient.put(sh, gv);
		
		return gv;
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
	
	@Override
	public List<QGradientTuple> getAllQGradients(State s){
		List<GroundedAction> gas = this.getAllGroundedActions(s);
		List<QGradientTuple> res = new ArrayList<QGradientTuple>(gas.size());
		for(GroundedAction ga : gas){
			res.add(this.getQGradient(s, ga));
		}
		return res;
	}
	
	@Override
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
