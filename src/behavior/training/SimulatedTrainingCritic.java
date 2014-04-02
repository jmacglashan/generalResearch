package behavior.training;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.learning.actorcritic.Critic;
import burlap.behavior.singleagent.learning.actorcritic.CritiqueResult;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class SimulatedTrainingCritic implements Critic {

	
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
	
	
	
	
	
	Policy					objectivePolicy;
	double [][]				feedbackProb;
	
	protected Random		rand;

	
	public SimulatedTrainingCritic(Policy objectivePolicy) {
		this.objectivePolicy = objectivePolicy;
		this.feedbackProb = new double [3][2];
	
		this.setProbability(FeedbackType.POSITIVE, FeedbackType.POSITIVE, 1.);
		this.setProbability(FeedbackType.NEUTRAL, FeedbackType.POSITIVE, 0.);
		this.setProbability(FeedbackType.NEGATIVE, FeedbackType.POSITIVE, 0.);
		
		this.setProbability(FeedbackType.POSITIVE, FeedbackType.NEGATIVE, 0.);
		this.setProbability(FeedbackType.NEUTRAL, FeedbackType.NEGATIVE, 0.);
		this.setProbability(FeedbackType.NEGATIVE, FeedbackType.NEGATIVE, 1.);
		
		
		rand = RandomFactory.getMapped(0);
		
	}
	
	public void setProbability(FeedbackType retFeedback, FeedbackType evaluation, double p){
		this.feedbackProb[retFeedback.toInt()][evaluation.toInt()] = p;
	}

	@Override
	public void addNonDomainReferencedAction(Action a) {
		//nothing to do
	}

	@Override
	public void initializeEpisode(State s) {
		//nothing to do
	}

	@Override
	public void endEpisode() {
		//nothing to do
	}

	@Override
	public CritiqueResult critiqueAndUpdate(State s, GroundedAction ga, State sprime) {
		
		List <GroundedAction> objectiveSelections = this.getAllOptimalActions(s);
		FeedbackType objectiveAssessment = FeedbackType.NEGATIVE;
		if(objectiveSelections.contains(ga)){
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
		
		
		CritiqueResult critique = new CritiqueResult(s, ga, sprime, feedback);
		
		
		return critique;
		
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

	@Override
	public void resetData() {
		//nothing to do
	}

}
