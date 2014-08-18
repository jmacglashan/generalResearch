package irl.mlirl;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class MLIRL {

	protected DifferentiableRF					curRF;
	protected QGradientPlanner					planner;
	protected Domain							domain;
	protected double							gamma;
	protected double							boltzBeta;
	protected StateHashFactory					hashingFactory;
	
	protected List<EpisodeAnalysis>				exampleTrajectories;
	
	
	
	
	public MLIRL(DifferentiableRF rf, List<EpisodeAnalysis> exampleTrajectories, Domain domain, double gamma, double boltzBeta, StateHashFactory hashingFactory){
		this.curRF = rf;
		this.exampleTrajectories = exampleTrajectories;
		this.gamma = gamma;
		this.boltzBeta = boltzBeta;
		this.hashingFactory = hashingFactory;
		
		this.planner = new DifferentiableVI(domain, rf, new NullTermination(), gamma, boltzBeta, hashingFactory, 0.01, 500);
		((OOMDPPlanner)this.planner).toggleDebugPrinting(false);
	}
	
	public void setPlanner(QGradientPlanner planner){
		this.planner = planner;
		((OOMDPPlanner)this.planner).toggleDebugPrinting(false);
	}
	
	public void runPlanner(){
		((OOMDPPlanner)this.planner).planFromState(this.exampleTrajectories.get(0).getState(0));
	}
	
	public double logLikelihoodOfTrajectory(EpisodeAnalysis ea){
		double logLike = 0.;
		Policy p = new BoltzmannQPolicy(this.planner, 1./this.boltzBeta);
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			((OOMDPPlanner)this.planner).planFromState(ea.getState(i));
			double actProb = p.getProbOfAction(ea.getState(i), ea.getAction(i));
			logLike += Math.log(actProb);
		}
		return logLike;
	}
	
	public double logLikelihood(){
		double sum = 0.;
		for(EpisodeAnalysis ea : this.exampleTrajectories){
			sum += this.logLikelihoodOfTrajectory(ea);
		}
		return sum;
	}
	
	public void runGradientAscent(double alpha, int iterations){
		for(int i = 0; i < iterations; i++){
			double [] params = this.curRF.getParameters();
			((OOMDPPlanner)this.planner).resetPlannerResults();
			System.out.println("RF: " + this.curRF.toString());
			
			
			double [] gradient = this.logLikelihoodGradient();
			System.out.println("Log likelihood: " + this.logLikelihood());
			
			for(int f = 0; f < params.length; f++){
				params[f] += alpha*gradient[f];
			}
			
		}
		
		((OOMDPPlanner)this.planner).resetPlannerResults();
		System.out.println("RF: " + this.curRF.toString());
		System.out.println("Log likelihood: " + this.logLikelihood());
	}
	
	
	
	public double [] logLikelihoodGradient(){
		double [] gradient = new double[this.curRF.dim];
		this.runPlanner();
		for(EpisodeAnalysis ea : this.exampleTrajectories){
			for(int t = 0; t < ea.numTimeSteps()-1; t++){
				((OOMDPPlanner)this.planner).planFromState(ea.getState(t));
				this.addToVector(gradient,this.logPolicyGrad(ea.getState(t), ea.getAction(t)));
			}
		}
		
		return gradient;
	}
	
	public double [] logPolicyGrad(State s, GroundedAction ga){
		
		Policy p = new BoltzmannQPolicy(this.planner, 1./this.boltzBeta);
		double invActProb = 1./p.getProbOfAction(s, ga);
		double [] gradient = BoltzmannPolicyGradient.computeBoltzmannPolicyGradient(s, ga, this.planner, this.boltzBeta);
		for(int f = 0; f < gradient.length; f++){
			gradient[f] *= invActProb;
		}
		return gradient;
		
	}
	
	
	protected double logSumExp(double [] eng, double m){
		
		double sum = 0.;
		for(int i = 0; i < eng.length; i++){
			sum += Math.exp(eng[i] - m);
		}
		double v = m + Math.log(sum);
		
		return v;
	}
	
	protected double logSumExp(double [] eng, double [] coeff, double m){
		
		double sum = 0.;
		for(int i = 0; i < eng.length; i++){
			sum += coeff[i] * Math.exp(eng[i] - m);
		}
		double v = m + Math.log(sum);
		
		return v;
	}
	
	protected double shiftedExponentialSum(double [] eng, double [] coeff, double m){
		double sum = 0.;
		for(int i = 0; i < eng.length; i++){
			sum += coeff[i] * Math.exp(eng[i] - m);
		}
		return sum;
	}
	
	
	protected void addToVector(double [] sumVector, double [] deltaVector){
		for(int i = 0; i < sumVector.length; i++){
			sumVector[i] += deltaVector[i];
		}
	}
	
	
	

	
	
}
