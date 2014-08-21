package irl.mlirl;

import java.util.ArrayList;
import java.util.List;

import cern.colt.Arrays;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;

public class CSABL {

	
	protected DifferentiableRF					curRF;
	protected QGradientPlanner					planner;
	protected Domain							domain;
	protected double							gamma;
	protected double							boltzBeta;
	protected StateHashFactory					hashingFactory;
	
	protected double							muPlus;
	protected double							muMinus;
	
	List<FeedbackTuple>							feedbacks;
	
	
	
	public CSABL(DifferentiableRF rf, List<FeedbackTuple> feedbackTuples, Domain domain, double gamma, double boltzBeta, StateHashFactory hashingFactory,
			double muPlus, double muMinus){
		this.curRF = rf;
		this.gamma = gamma;
		this.boltzBeta = boltzBeta;
		this.hashingFactory = hashingFactory;
		this.feedbacks = feedbackTuples;
		
		this.muPlus = muPlus;
		this.muMinus = muMinus;
		
		this.planner = new DifferentiableVI(domain, rf, new NullTermination(), gamma, boltzBeta, hashingFactory, 0.01, 500);
		((OOMDPPlanner)this.planner).toggleDebugPrinting(false);
	}
	
	public CSABL(DifferentiableRF rf, Domain domain, double gamma, double boltzBeta, StateHashFactory hashingFactory,
			double muPlus, double muMinus){
		this.curRF = rf;
		this.gamma = gamma;
		this.boltzBeta = boltzBeta;
		this.hashingFactory = hashingFactory;
		this.feedbacks = new ArrayList<FeedbackTuple>();
		
		this.muPlus = muPlus;
		this.muMinus = muMinus;
		
		this.planner = new DifferentiableVI(domain, rf, new NullTermination(), gamma, boltzBeta, hashingFactory, 0.01, 500);
		((OOMDPPlanner)this.planner).toggleDebugPrinting(false);
	}
	
	public void setPlanner(QGradientPlanner planner){
		this.planner = planner;
		((OOMDPPlanner)this.planner).toggleDebugPrinting(false);
	}
	
	public QGradientPlanner getPlanner(){
		return this.planner;
	}
	
	public void setFeedbacks(List<FeedbackTuple> feedbacks){
		this.feedbacks = feedbacks;
	}
	
	public void runPlanner(){
		((OOMDPPlanner)this.planner).planFromState(this.feedbacks.get(0).s);
	}
	
	public void runGradientAscent(double alpha, int iterations){
		for(int i = 0; i < iterations; i++){
			double [] params = this.curRF.getParameters();
			((OOMDPPlanner)this.planner).resetPlannerResults();

			//System.out.println("RF: " + this.curRF.toString());
			
			
			double [] gradient = this.logLikelihoodGradient();
			//System.out.println("Log likelihood: " + this.logLikelihood());
			
			for(int f = 0; f < params.length; f++){
				params[f] += alpha*gradient[f];
			}
			
		}
		
		((OOMDPPlanner)this.planner).resetPlannerResults();
		//System.out.println("RF: " + this.curRF.toString());
		//System.out.println("Log likelihood: " + this.logLikelihood());
	}
	
	public void runStochasticGradientAscent(double alpha, int iterations){
		
		for(int i = 0; i < iterations; i++){
			double [] params = this.curRF.getParameters();
			((OOMDPPlanner)this.planner).resetPlannerResults();

			System.out.println("RF: " + this.curRF.toString());
			
			
			FeedbackTuple randFeedback = this.feedbacks.get(RandomFactory.getMapped(0).nextInt(this.feedbacks.size()));
			double [] gradient = this.logLikelihoodFeedbackGradient(randFeedback);
			System.out.println("Log likelihood: " + this.logLikelihood());
			
			for(int f = 0; f < params.length; f++){
				params[f] += alpha*gradient[f];
			}
			
		}
		
		((OOMDPPlanner)this.planner).resetPlannerResults();
		System.out.println("RF: " + this.curRF.toString());
		System.out.println("Log likelihood: " + this.logLikelihood());
		
	}
	
	public void stochasticGradientAscentOnInstance(FeedbackTuple ft, double alpha){
		
		if(ft.feedback == 0.){
			return;
		}
		
		
		
		double [] gradient = this.logLikelihoodFeedbackGradient(ft);
		
		//System.out.println("RF is now: ");
		double [] params = this.curRF.getParameters();
		for(int f = 0; f < params.length; f++){
			params[f] += alpha*gradient[f];
			//System.out.println(f + ": " + params[f]);
		}
		
		((OOMDPPlanner)this.planner).resetPlannerResults();
		
		
		
	}
	
	
	public double logLikelihood(){
		double sum = 0.;
		for(FeedbackTuple ft : this.feedbacks){
			((OOMDPPlanner)this.planner).planFromState(ft.s);
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
			((OOMDPPlanner)this.planner).planFromState(ft.s);
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
