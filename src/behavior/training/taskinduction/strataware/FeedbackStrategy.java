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
	
	
	
	public double liklihood(State s, GroundedAction ga, double feedback, Policy p){
		
		boolean isOptimal = this.isOptimal(s, ga, p);
		
		if(isOptimal){
			if(feedback > 0.){
				return (1.-this.epsilon) * (1. - this.muCorrect);
			}
			else if(feedback < 0.){
				return this.epsilon * (1. - this.muIncorrect);
			}
			else{
				return ((1. - this.epsilon) * this.muCorrect) + (this.epsilon * this.muIncorrect);
			}
		}
		else{
			
			if(feedback > 0.){
				return this.epsilon * (1. - this.muCorrect);
			}
			else if(feedback < 0.){
				return (1. - this.epsilon) * (1. - this.muIncorrect);
			}
			else{
				return (this.epsilon * this.muCorrect) + ((1. - this.epsilon) * this.muIncorrect);
			}
			
		}
		

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
