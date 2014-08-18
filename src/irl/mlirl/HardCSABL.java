package irl.mlirl;

import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;

public class HardCSABL {

	
	protected DifferentiableRF					curRF;
	protected QGradientPlanner					planner;
	protected Domain							domain;
	protected double							gamma;
	protected double							boltzBeta;
	protected StateHashFactory					hashingFactory;
	
	protected double							muPlus;
	protected double							muMinus;
	
	List<FeedbackTuple>							feedbacks;
	
	
	public HardCSABL(DifferentiableRF rf, List<FeedbackTuple> feedbackTuples, Domain domain, double gamma, double boltzBeta, StateHashFactory hashingFactory,
			double muPlus, double muMinus){
		this.curRF = rf;
		this.gamma = gamma;
		this.boltzBeta = boltzBeta;
		this.hashingFactory = hashingFactory;
		this.feedbacks = feedbackTuples;
		
		this.muPlus = muPlus;
		this.muMinus = muMinus;
		
		this.planner = new DifferentiableVI(domain, rf, new NullTermination(), gamma, boltzBeta, hashingFactory, 0.01, 500);
	}
	
	public void runPlanner(){
		((OOMDPPlanner)this.planner).planFromState(this.feedbacks.get(0).s);
	}
	
	public void runGradientAscent(double alpha, int iterations){
		for(int i = 0; i < iterations; i++){
			double [] params = this.curRF.getParameters();
			((OOMDPPlanner)this.planner).resetPlannerResults();
			this.runPlanner();
			System.out.println("RF: " + this.curRF.toString());
			System.out.println("Log likelihood: " + this.logLikelihood());
			
			double [] gradient = this.logLikelihoodGradient();
			
			for(int f = 0; f < params.length; f++){
				params[f] += alpha*gradient[f];
			}
			
		}
		
		((OOMDPPlanner)this.planner).resetPlannerResults();
		this.runPlanner();
		System.out.println("RF: " + this.curRF.toString());
		System.out.println("Log likelihood: " + this.logLikelihood());
	}
	
	public void runStochasticGradientAscent(double alpha, int iterations){
		
		for(int i = 0; i < iterations; i++){
			double [] params = this.curRF.getParameters();
			((OOMDPPlanner)this.planner).resetPlannerResults();
			this.runPlanner();
			System.out.println("RF: " + this.curRF.toString());
			System.out.println("Log likelihood: " + this.logLikelihood());
			
			FeedbackTuple randFeedback = this.feedbacks.get(RandomFactory.getMapped(0).nextInt(this.feedbacks.size()));
			double [] gradient = this.logLikelihoodFeedbackGradient(randFeedback);
			
			for(int f = 0; f < params.length; f++){
				params[f] += alpha*gradient[f];
			}
			
		}
		
	}
	
	
	public double logLikelihood(){
		double sum = 0.;
		for(FeedbackTuple ft : this.feedbacks){
			sum += this.logLikelihoodFeedback(ft);
		}
		return sum;
	}
	
	public double logLikelihoodFeedback(FeedbackTuple ft){
		double p = this.likelihoodFeedback(ft);
		return Math.log(p);
	}
	
	public double likelihoodFeedback(FeedbackTuple ft){
		Policy policy = new BoltzmannQPolicy(this.planner, 1./this.boltzBeta);
		double actionProb = policy.getProbOfAction(ft.s, ft.a);
		double p;
		if(ft.feedback > 0.){
			p = (1. - this.muPlus) * actionProb;
		}
		else if(ft.feedback < 0.){
			p = (1. - this.muMinus) * (1. - actionProb);
		}
		else{
			p = (this.muPlus * actionProb) + (this.muMinus * (1. - actionProb));
		}
		
		return p;
	}
	
	public double [] logLikelihoodFeedbackGradient(FeedbackTuple ft){
		
		double invftp = 1./this.likelihoodFeedback(ft);
		double [] pg = BoltzmannPolicyGradient.computeBoltzmannPolicyGradient(ft.s, ft.a, this.planner, this.boltzBeta);
		double c;
		if(ft.feedback > 0.){
			c = 1. - this.muPlus;
		}
		else if(ft.feedback < 0.){
			c = this.muMinus - 1.;
		}
		else{
			c = this.muPlus - this.muMinus;
		}
		c *= invftp;
		for(int f = 0; f < pg.length; f++){
			pg[f] *= c;
		}
		
		return pg;
	}
	
	public double [] logLikelihoodGradient(){
		
		double [] gradient = new double[this.curRF.dim];
		for(FeedbackTuple ft : this.feedbacks){
			this.addToVector(gradient, this.logLikelihoodFeedbackGradient(ft));
		}
		
		return gradient;
		
	}
	
	protected void addToVector(double [] sumVector, double [] deltaVector){
		for(int i = 0; i < sumVector.length; i++){
			sumVector[i] += deltaVector[i];
		}
	}

}
