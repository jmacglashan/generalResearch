package behavior.training.taskinduction.strataware;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class FeedbackStrategy {
	
	public static double OPTIMALDIFFROMMAX = 0.01;
	
	protected double probOfStrategy = 0.;
	
	protected double muCorrect;		//assuming the trainer does not make a feedback error, the probability that they will *not* provide reward for a correct action
	protected double muIncorrect;	//assuming the trainer does not make a feedback error, the probability that they will *not* provide punishment for an incorrect action
	protected double epsilon;		//probability that trainer makes a feedback mistake


	protected String name = null;

	protected State cachedS = null;
	protected GroundedAction cachedGA = null;
	protected Policy cachedPolicy = null;
	protected double cachedLikelihood;

	public FeedbackStrategy(double muCorrect, double muIncorrect, double epsilon){
		this.muCorrect = muCorrect;
		this.muIncorrect = muIncorrect;
		this.epsilon = epsilon;
	}
	
	public void setProbOfStrategy(double probOfStrategy){
		this.probOfStrategy = probOfStrategy;
	}
	
	public double getProbOfStrategy(){
		return this.probOfStrategy;
	}

	public double getMuCorrect() {
		return muCorrect;
	}

	public void setMuCorrect(double muCorrect) {
		this.muCorrect = muCorrect;
	}

	public double getMuIncorrect() {
		return muIncorrect;
	}

	public void setMuIncorrect(double muIncorrect) {
		this.muIncorrect = muIncorrect;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double liklihood(State s, GroundedAction ga, double feedback, Policy p){


		if(s == cachedS && ga == cachedGA && p == cachedPolicy){
			return this.cachedLikelihood;
		}

		this.cachedS = s;
		this.cachedGA = ga;
		this.cachedPolicy = p;

		boolean isOptimal = this.isOptimal(s, ga, p);

		
		if(isOptimal){
			if(feedback > 0.){

				this.cachedLikelihood = (1.-this.epsilon) * (1. - this.muCorrect);
			}
			else if(feedback < 0.){
				this.cachedLikelihood =  this.epsilon * (1. - this.muIncorrect);
			}
			else{
				this.cachedLikelihood =  ((1. - this.epsilon) * this.muCorrect) + (this.epsilon * this.muIncorrect);
			}
		}
		else{
			
			if(feedback > 0.){
				this.cachedLikelihood =  this.epsilon * (1. - this.muCorrect);
			}
			else if(feedback < 0.){
				this.cachedLikelihood =  (1. - this.epsilon) * (1. - this.muIncorrect);
			}
			else{
				this.cachedLikelihood =  (this.epsilon * this.muCorrect) + ((1. - this.epsilon) * this.muIncorrect);
			}
			
		}

		return this.cachedLikelihood;
		

	}
	
	public double feedbackSample(State s, GroundedAction ga, Policy p){
		
		boolean isOptimal = this.isOptimal(s, ga, p);
		
		Random rand = RandomFactory.getMapped(0);
		double r = rand.nextDouble();
		
		if(isOptimal){
			
			double sumP = (1.-this.epsilon) * (1. - this.muCorrect);
			if(r < sumP){
				return 1.;
			}
			sumP += this.epsilon * (1. - this.muIncorrect);
			if(r < sumP){
				return -1.;
			}
			
			return 0.;
			
		}
		else{
			
			double sumP = this.epsilon * (1. - this.muCorrect);
			if(r < sumP){
				return 1.;
			}
			sumP += (1. - this.epsilon) * (1. - this.muIncorrect);
			if(r < sumP){
				return -1.;
			}
			
			return 0.;
			
		}
		
	}



	@Override
	public String toString(){
		String sName;
		if(this.name != null){
			sName = this.name;
		}
		else{
			sName = "(" + this.muCorrect + ", " + this.muIncorrect + ", " + this.epsilon + ")";
		}


		return this.probOfStrategy + " " + sName;
	}
	
	
	protected boolean isOptimal(State s, GroundedAction ga, Policy p){
		List<GroundedAction> gas = this.getOptimalActions(s, p);
		for(GroundedAction o : gas){
			if(o.equals(ga)){
				return true;
			}
		}
		
		return false;
	}
	
	protected List<GroundedAction> getOptimalActions(State s, Policy p){
		List<ActionProb> dist = p.getActionDistributionForState(s);
		List<GroundedAction> optimal = new ArrayList<GroundedAction>(dist.size());
		
		double maxProb = 0.;
		for(ActionProb ap : dist){
			maxProb = Math.max(maxProb, ap.pSelection);
		}
		
		double threshold = maxProb - OPTIMALDIFFROMMAX;
		for(ActionProb ap : dist){
			if(ap.pSelection >= threshold){
				optimal.add((GroundedAction)ap.ga);
			}
		}
		
		
		return optimal;
		
	}
	
	
}
