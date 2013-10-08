package tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import behavior.training.SimulatedTrainingCritic;
import behavior.training.SimulatedTrainingCritic.FeedbackType;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.learning.actorcritic.Actor;
import burlap.behavior.singleagent.learning.actorcritic.ActorCritic;
import burlap.behavior.singleagent.learning.actorcritic.Critic;
import burlap.behavior.singleagent.learning.actorcritic.actor.BoltzmannActor;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.NullRewardFunction;

public class MultiStateBanditTrainingTest {

	protected GraphDefinedDomain			gdd;
	protected Domain						domain;
	protected int							nDecisionNodes = 12;
	protected int							nActions = 4;
	protected ObjectiveGraphPolicy			objectivePolicy;
	protected TerminalFunction				tf;
	protected DiscreteStateHashFactory		hashingFactory;
	protected Random						rand;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		MultiStateBanditTrainingTest example = new MultiStateBanditTrainingTest();
		//example.trainAndAssess();
		
		List <Critic> critics = new ArrayList<Critic>();
		critics.add(example.perfectFullFeedbackCritic());
		critics.add(example.dogTrainerCritic());
		critics.add(example.imperfectFullFeedbackCritic());
		critics.add(example.imperfectDogTrainerCritic());
		
		example.trainAndAssessMultipleCritics(critics);

	}
	
	
	
	public MultiStateBanditTrainingTest() {
		
		
		gdd = new GraphDefinedDomain(nDecisionNodes+1); //additional node for termination node; always at zero
		for(int i = 1; i < nDecisionNodes+1; i++){
			for(int j = 0; j < nActions; j++){
				gdd.setTransition(i, j, 0, 1.);
			}
		}
		
		domain = gdd.generateDomain();
		objectivePolicy = new ObjectiveGraphPolicy();
		tf = new GraphTermination();
		
		
		hashingFactory = new DiscreteStateHashFactory();
		
		rand = RandomFactory.getMapped(0);
		
	}

	
	public Critic perfectFullFeedbackCritic(){
		return new SimulatedTrainingCritic(objectivePolicy);
	}
	
	
	public Critic imperfectFullFeedbackCritic(){
		SimulatedTrainingCritic critic = new SimulatedTrainingCritic(objectivePolicy);
		
		critic.setProbability(FeedbackType.POSITIVE, FeedbackType.POSITIVE, 0.8);
		critic.setProbability(FeedbackType.NEUTRAL, FeedbackType.POSITIVE, 0.15);
		critic.setProbability(FeedbackType.NEGATIVE, FeedbackType.POSITIVE, 0.05);
		
		critic.setProbability(FeedbackType.POSITIVE, FeedbackType.NEGATIVE, 0.05);
		critic.setProbability(FeedbackType.NEUTRAL, FeedbackType.NEGATIVE, 0.15);
		critic.setProbability(FeedbackType.NEGATIVE, FeedbackType.NEGATIVE, 0.8);
		
		return critic;
	}
	
	public Critic dogTrainerCritic(){
		SimulatedTrainingCritic critic = new SimulatedTrainingCritic(objectivePolicy);
		critic.setProbability(FeedbackType.NEUTRAL, FeedbackType.NEGATIVE, 1.0);
		critic.setProbability(FeedbackType.NEGATIVE, FeedbackType.NEGATIVE, 0.0);
		
		return critic;
	}
	
	
	public Critic imperfectDogTrainerCritic(){
		SimulatedTrainingCritic critic = new SimulatedTrainingCritic(objectivePolicy);
		
		critic.setProbability(FeedbackType.POSITIVE, FeedbackType.POSITIVE, 0.8);
		critic.setProbability(FeedbackType.NEUTRAL, FeedbackType.POSITIVE, 0.15);
		critic.setProbability(FeedbackType.NEGATIVE, FeedbackType.POSITIVE, 0.05);
		
		critic.setProbability(FeedbackType.POSITIVE, FeedbackType.NEGATIVE, 0.05);
		critic.setProbability(FeedbackType.NEUTRAL, FeedbackType.NEGATIVE, 0.9);
		critic.setProbability(FeedbackType.NEGATIVE, FeedbackType.NEGATIVE, 0.05);
		
		return critic;
	}
	
	
	
	public void trainAndAssessMultipleCritics(List <Critic> critics){
		
		List <ActorCritic> acs = new ArrayList<ActorCritic>(critics.size());
		for(Critic c : critics){
			Actor actor = new BoltzmannActor(domain, hashingFactory, 10.0);
			ActorCritic ac = new ActorCritic(domain, new NullRewardFunction(), tf, 1., actor, c);
			acs.add(ac);
		}
		
		int n = 200;
		for(int i = 0; i < n; i++){
			
			State s = this.randomState();
			
			System.out.print("" + i);
			for(ActorCritic ac : acs){
				System.out.print("\t" + this.assessStochasticPolicy(ac.getPolicy()));
				ac.runLearningEpisodeFrom(s);
			}
			System.out.println("");

		}
		
		System.out.print("" + n);
		for(ActorCritic ac : acs){
			System.out.print("\t" + this.assessStochasticPolicy(ac.getPolicy()));
			
		}
		
	}
	
	public void trainAndAssess(){
		
		SimulatedTrainingCritic critic = new SimulatedTrainingCritic(objectivePolicy);
		Actor actor = new BoltzmannActor(domain, hashingFactory, 10.0);
		ActorCritic ac = new ActorCritic(domain, new NullRewardFunction(), tf, 1., actor, critic);
		
		int n = 100;
		for(int i = 0; i < n; i++){
			System.out.println("" + i + ": " + this.assessStochasticPolicy(actor) + ";\t" + this.assessDeterminisiticPolicy(actor));
			State s = this.randomState();
			ac.runLearningEpisodeFrom(s);
		}
		System.out.println("" + n + ": " + this.assessStochasticPolicy(actor) + ";\t" + this.assessDeterminisiticPolicy(actor));
		
	}
	
	
	
	public double assessStochasticPolicy(Policy p){
		
		double sumProb = 0.;
		for(int i = 1; i < nDecisionNodes+1; i++){
			State s = GraphDefinedDomain.getState(domain, i);
			GroundedAction oAction = objectivePolicy.getAction(s);
			ActionProb ap = this.actionProbFor(p, s, oAction);
			sumProb += ap.pSelection;
			
		}
		
		
		return sumProb / nDecisionNodes;
		
	}
	
	public double assessDeterminisiticPolicy(Policy p){
		
		double sumCorrect = 0;
		
		for(int i = 1; i < nDecisionNodes+1; i++){
			State s = GraphDefinedDomain.getState(domain, i);
			GroundedAction oAction = objectivePolicy.getAction(s);
			sumCorrect += this.isSoleMaximum(p, s, oAction);
			
		}
		
		return sumCorrect / nDecisionNodes;
		
	}
	
	
	protected ActionProb actionProbFor(Policy p, State s, GroundedAction ga){
		List <ActionProb> aps = p.getActionDistributionForState(s);
		for(ActionProb ap : aps){
			if(ap.ga.equals(ga)){
				return ap;
			}
		}
		
		//didn't find it so must be 0 probability
		return new ActionProb(ga, 0.);
	}
	
	
	protected int isSoleMaximum(Policy p, State s, GroundedAction ga){
		
		List <GroundedAction> optimal = this.getAllOptimalActions(p, s);
		if(optimal.size() != 1){
			return 0;
		}
		
		if(ga.equals(optimal.get(0))){
			return 1;
		}
		
		return 0;
	}
	
	protected List<GroundedAction> getAllOptimalActions(Policy p, State s){
		List<ActionProb> policyDist = p.getActionDistributionForState(s);
		
		List <GroundedAction> selections = new ArrayList<GroundedAction>();
		double maxProb = 0.0;
		for(ActionProb ap : policyDist){
			if(ap.pSelection > maxProb){
				maxProb = ap.pSelection;
				selections.clear();
				selections.add(ap.ga);
			}
			else if(ap.pSelection == maxProb){
				selections.add(ap.ga);
			}
		}
		
		return selections;
		
	}
	
	protected State randomState(){
		int sid = rand.nextInt(nDecisionNodes)+1;
		return GraphDefinedDomain.getState(domain, sid);
	}
	
	class ObjectiveGraphPolicy extends Policy{

		@Override
		public GroundedAction getAction(State s) {
			
			int gNode = s.getObjectsOfTrueClass(GraphDefinedDomain.CLASSAGENT).get(0).getDiscValForAttribute(GraphDefinedDomain.ATTNODE);
			
			int aId = (gNode - 1) % nActions;
			
			Action a = domain.getAction(GraphDefinedDomain.BASEACTIONNAME+aId);
			GroundedAction ga = new GroundedAction(a, "");
			
			return ga;
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			return this.getDeterministicPolicy(s);
		}

		@Override
		public boolean isStochastic() {
			return false;
		}
		
	
	}
	
	
	class GraphTermination implements TerminalFunction{

		protected int termNode = 0;
		
		public GraphTermination(){
			
		}
		
		public GraphTermination(int termNode){
			this.termNode = termNode;
		}
		
		@Override
		public boolean isTerminal(State s) {
			int gNode = s.getObjectsOfTrueClass(GraphDefinedDomain.CLASSAGENT).get(0).getDiscValForAttribute(GraphDefinedDomain.ATTNODE);
			return gNode == termNode;
		}
		
		
		
	}
	

}
