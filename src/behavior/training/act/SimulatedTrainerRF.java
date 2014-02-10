package behavior.training.act;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class SimulatedTrainerRF implements RewardFunction {

	public enum FeedbackType{
		
		POSITIVE(0),
		NEGATIVE(1),
		NEUTRAL(2);
		
		
		private final int intVal;
		FeedbackType(int i){
			this.intVal = i;
		}
		
		public int toInt(){
			return this.intVal;
		}
		
	}
	
	
	
	
	
	Policy										objectivePolicy;
	double [][]									feedbackProb;
	
	protected Random							rand;
	
	protected List <StateConditionTest>			feedbackStates;
	
	
	public SimulatedTrainerRF(Policy objectivePolicy) {
		this.objectivePolicy = objectivePolicy;
		this.feedbackProb = new double [3][2];
	
		this.setProbability(FeedbackType.POSITIVE, FeedbackType.POSITIVE, 1.);
		this.setProbability(FeedbackType.NEUTRAL, FeedbackType.POSITIVE, 0.);
		this.setProbability(FeedbackType.NEGATIVE, FeedbackType.POSITIVE, 0.);
		
		this.setProbability(FeedbackType.POSITIVE, FeedbackType.NEGATIVE, 0.);
		this.setProbability(FeedbackType.NEUTRAL, FeedbackType.NEGATIVE, 0.);
		this.setProbability(FeedbackType.NEGATIVE, FeedbackType.NEGATIVE, 1.);
		
		
		feedbackStates = new ArrayList<StateConditionTest>();
		
		rand = RandomFactory.getMapped(0);
	}
	
	public void setProbability(FeedbackType retFeedback, FeedbackType evaluation, double p){
		this.feedbackProb[retFeedback.toInt()][evaluation.toInt()] = p;
	}
	
	public void addConditionTest(StateConditionTest test){
		this.feedbackStates.add(test);
	}

	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		
		if(!this.isFeedbackState(sprime)){
			return 0.;
		}
		
		
		List <GroundedAction> objectiveSelections = this.getAllOptimalActions(s);
		FeedbackType objectiveAssessment = FeedbackType.NEGATIVE;
		if(objectiveSelections.contains(a)){
			objectiveAssessment = FeedbackType.POSITIVE;
		}
		
		
		double feedback = 0.;
		
		double pPostive = this.feedbackProb[FeedbackType.POSITIVE.toInt()][objectiveAssessment.toInt()];
		double pNegative = this.feedbackProb[FeedbackType.NEGATIVE.toInt()][objectiveAssessment.toInt()];
		
		double roll = rand.nextDouble();
		if(roll <= pPostive){
			feedback = 1.;
		}
		else if(roll <= pPostive+pNegative){
			feedback = -1.;
		}
		
		
		return feedback;
	}
	
	
	protected List<GroundedAction> getAllOptimalActions(State s){
		List<ActionProb> policyDist = this.objectivePolicy.getActionDistributionForState(s);
		
		List <GroundedAction> selections = new ArrayList<GroundedAction>();
		double maxProb = 0.0;
		for(ActionProb ap : policyDist){
			if(ap.pSelection > maxProb){
				maxProb = ap.pSelection;
				selections.clear();
				selections.add((GroundedAction)ap.ga);
			}
			else if(ap.pSelection == maxProb){
				selections.add((GroundedAction)ap.ga);
			}
		}
		
		return selections;
		
	}
	
	
	protected boolean isFeedbackState(State s){
		
		if(this.feedbackStates.size() == 0){
			return true;
		}
		
		for(StateConditionTest test : feedbackStates){
			if(test.satisfies(s)){
				return true;
			}
		}
		
		return false;
	}

}
