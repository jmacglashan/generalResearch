package behavior.training.taskinduction.sabl;

import auxiliary.DynamicFeedbackEnvironment;
import behavior.training.experiments.interactive.soko.PolicyGenerator;
import behavior.training.taskinduction.NoopOnTermPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.sabl.extendedEpisode.SABLBelief;
import behavior.training.taskinduction.sabl.extendedEpisode.SABLExtendedEA;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import datastructures.HashedAggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class SABLAgent extends OOMDPPlanner implements LearningAgent {

	protected Domain planningDomain;

	protected List <TaskPolicyProbTuple> tasks;
	protected List <FeedbackStrategyProbPair> strategies;

	protected List<JointPolicyStrategyProb> jointProbabilities;

	protected PolicyGenerator policyGenerator;

	protected Action noopAction = null;

	protected LearningAgentBookKeeping bookKeeping = new LearningAgentBookKeeping();

	protected DynamicFeedbackEnvironment env = null;

	protected int speedMode = -1;


	public SABLAgent(Domain domain, Domain planningDomain, RewardFunction rf, TerminalFunction tf,
					 StateHashFactory hashingFactory, PolicyGenerator policyGenerator){
		this.plannerInit(domain, rf, tf, 1.0, hashingFactory);

		this.planningDomain = planningDomain;

		this.policyGenerator = policyGenerator;

		this.tasks = new ArrayList<TaskPolicyProbTuple>();
		this.strategies = new ArrayList<FeedbackStrategyProbPair>();

	}

	public void setTasks(List <TaskDescription> tasks, List <Double> priors){
		//reset our joints
		this.jointProbabilities = null;

		this.tasks.clear();
		for(int i = 0; i < tasks.size(); i++){
			TaskDescription td = tasks.get(i);
			double prior = priors.get(i);
			this.tasks.add(new TaskPolicyProbTuple(td, prior));
		}
	}

	public void updateTaskPriorsToPosteriors(){

		if(this.jointProbabilities == null){
			return;
		}

		List <TaskProb> posteriors = this.getTaskProbabilityDistribution();
		for(int i = 0; i < posteriors.size(); i++){
			this.tasks.get(i).setProb(posteriors.get(i).getProb());
		}

	}

	public void updateStrategyPriorsToPosteriors(){

		if(this.jointProbabilities == null){
			return;
		}

		List <FeedbackStrategy> newPriors = this.getStrategyProbabiltyDistribution();

		this.strategies = new ArrayList<FeedbackStrategyProbPair>();
		for(FeedbackStrategy fs : newPriors){
			this.strategies.add(new FeedbackStrategyProbPair(fs, fs.getProbOfStrategy()));
		}
	}

	public void setFeedbackStrategies(List <FeedbackStrategy> strategies){

		//reset our joints
		this.jointProbabilities = null;

		this.strategies.clear();
		for(FeedbackStrategy fs : strategies){
			this.strategies.add(new FeedbackStrategyProbPair(fs, fs.getProbOfStrategy()));
		}

	}

	public void setNoopAction(Action noopAction){
		this.noopAction = noopAction;
	}

	public void initializePlanningOnInputState(State s){
		for(TaskPolicyProbTuple tpp : tasks){
			tpp.policy = new NoopOnTermPolicy(this.noopAction, tpp.task.tf, this.policyGenerator.getPolicy(this.planningDomain,
					s, tpp.task.rf, tpp.task.tf, this.hashingFactory));
		}
	}

	public void initializeJointProbabilities(){
		this.jointProbabilities = new ArrayList<JointPolicyStrategyProb>();
		for(TaskPolicyProbTuple tpp : this.tasks){
			for(FeedbackStrategyProbPair fspp : this.strategies){
				this.jointProbabilities.add(new JointPolicyStrategyProb(tpp, fspp));
			}
		}
	}

	public void setEnv(DynamicFeedbackEnvironment env) {
		this.env = env;
	}

	public void setSpeedMode(int speedMode) {
		this.speedMode = speedMode;
	}

	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
		return this.runLearningEpisodeFrom(initialState, this.bookKeeping.maxEpisodeSize);
	}

	@Override
	public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {

		//EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
		SABLBelief sb = new SABLBelief(this.getTaskProbabilityDistribution(), this.getStrategyProbabiltyDistribution());
		SABLExtendedEA ea = new SABLExtendedEA(initialState, sb);

		System.out.println("\n\nStarting\n\n"+sb.toString()+"\n\n");

		State curState = initialState;
		SABLBelief curBelief = sb;
		int numSteps = 0;
		while(!this.tf.isTerminal(curState) && numSteps < maxSteps){

			GroundedAction ga = this.worldAction(curState, (GroundedAction)this.mostLikelyTaskTuple().getPolicy().getAction(curState));
			if(this.env != null && this.speedMode != -1){
				long time = this.getSpeed(curBelief, curState);
				System.out.println("delay: " + time);
				this.env.setActionUpdateDelay(time);
			}
			State nextState = ga.executeIn(curState);
			double trainerFeedback = this.rf.reward(curState, ga, nextState);

			this.updateJointDistribution(curState, ga, trainerFeedback);

			SABLBelief nextBelief = new SABLBelief(this.getTaskProbabilityDistribution(), this.getStrategyProbabiltyDistribution());
			ea.recordTransitionTo(ga, nextState, trainerFeedback, nextBelief);

			curState = nextState;
			numSteps++;
			curBelief = nextBelief;

			System.out.println(nextBelief.toString()+"\n\n");

		}

		this.bookKeeping.offerEpisodeToHistory(ea);

		return ea;
	}

	protected long getSpeed(SABLBelief belief, State s){
		if(this.speedMode == 0) {
			return getLinearSpeed(belief, s);
		}
		else if(this.speedMode == 1){
			return getSigmoidSpeed(belief, s);
		}
		else if(this.speedMode == 2){
			return getThresholdSpeed(belief, s);
		}
		throw new RuntimeException("Cannot select speed mode for mode " + this.speedMode);
	}

	protected long getLinearSpeed(SABLBelief belief, State s){
		//System.out.println("Linear");
		double h = this.getPolicyEntropy(belief, s);
		double speed = 500 + 1500*h;
		return (int)speed;
	}

	protected long getSigmoidSpeed(SABLBelief belief, State s){
		//System.out.println("Sigmoidal");
		double h = this.getPolicyEntropy(belief, s);
		double scale = 1. / (1 + Math.exp(-10. * (h-0.5)));
		double speed = 500 + 1500*scale;
		return (int)speed;
	}

	protected long getThresholdSpeed(SABLBelief belief, State s){
		//System.out.println("Threshold");
		double h = this.getPolicyEntropy(belief, s);
		if(h > 0.1){
			return 2000;
		}
		return 500;
	}


	public double getPolicyEntropy(SABLBelief belief, State s){

		HashedAggregator<GroundedAction> policyDist = new HashedAggregator<GroundedAction>();
		for(TaskProb tp : belief.taskProbs){
			List<Policy.ActionProb> aps = tp.getPolicy().getActionDistributionForState(s);
			for(Policy.ActionProb ap : aps){
				double p = ap.pSelection*tp.getProb();
				policyDist.add((GroundedAction)ap.ga, p);
			}
		}

		int nActions = 0;
//		for(Double d : policyDist.valueSet()){
//			if(d > 0){
//				nActions++;
//			}
//		}

		List<GroundedAction> actions = this.getAllGroundedActions(s);
		nActions = actions.size();

		if(nActions == 1){
			return 0.;
		}

		double h = 0.;
		for(Double d : policyDist.valueSet()){
			double el = d*this.log(nActions, d);
			h += el;
		}
		h*=-1;
		return h;

	}

	protected double log(double base, double x){
		return Math.log(x) / Math.log(base);
	}


	public List<TaskProb> getTaskProbabilityDistribution(){

		List <TaskProb> taskProbs = new ArrayList<TaskProb>(this.tasks.size());
		for(int i = 0; i < this.tasks.size(); i++){
			TaskPolicyProbTuple tpp = this.tasks.get(i);
			double p = JointPolicyStrategyProb.marginalizeForTask(this.jointProbabilities, tpp.task);
			TaskProb tp = new TaskProb(tpp.task, tpp.policy, p);
			taskProbs.add(tp);
		}
		return taskProbs;

	}


	public List <FeedbackStrategy> getStrategyProbabiltyDistribution(){

		List <FeedbackStrategy> strategyDist = new ArrayList<FeedbackStrategy>(this.strategies.size());

		for(FeedbackStrategyProbPair fs : this.strategies){
			double p = JointPolicyStrategyProb.marginalizeForStrategy(this.jointProbabilities, fs.getStrategy());
			FeedbackStrategy nfs = new FeedbackStrategy(fs.getStrategy().getMuCorrect(), fs.getStrategy().getMuIncorrect(), fs.getStrategy().getEpsilon());
			nfs.setProbOfStrategy(p);
			nfs.setName(fs.getStrategy().getName());
			strategyDist.add(nfs);
		}

		return strategyDist;
	}

	public TaskProb getMostLikelyTask(){

		double maxProb = Double.NEGATIVE_INFINITY;
		TaskProb tp = null;
		for(TaskPolicyProbTuple t : this.tasks){
			double marginal = JointPolicyStrategyProb.marginalizeForTask(this.jointProbabilities, t.task);
			if(marginal > maxProb){
				maxProb = marginal;
				tp = new TaskProb(t.task, t.policy, marginal);
			}
		}

		return tp;
	}


	@Override
	public EpisodeAnalysis getLastLearningEpisode() {
		return this.bookKeeping.getLastLearningEpisode();
	}

	@Override
	public void setNumEpisodesToStore(int numEps) {
		this.bookKeeping.setNumEpisodesToStore(numEps);
	}

	@Override
	public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
		return this.bookKeeping.episodeHistory;
	}

	@Override
	public void planFromState(State initialState) {
		throw new UnsupportedOperationException("SABL agent cannot plan.");
	}

	@Override
	public void resetPlannerResults() {
		//do nothing
	}


	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();

		List <TaskProb> taskD = this.getTaskProbabilityDistribution();
		List <FeedbackStrategy> fsD = this.getStrategyProbabiltyDistribution();

		boolean first = true;
		for(TaskProb tp : taskD){
			if(!first){
				sb.append("\n");
			}
			sb.append(tp.toString());
			first = false;
		}
		sb.append("\n---");
		for(FeedbackStrategy fs : fsD){
			sb.append("\n").append(fs.toString());
		}



		return sb.toString();
	}

	protected TaskPolicyProbTuple mostLikelyTaskTuple(){
		double maxProb = Double.NEGATIVE_INFINITY;
		TaskPolicyProbTuple task = null;
		for(TaskPolicyProbTuple t : this.tasks){
			double marginal = JointPolicyStrategyProb.marginalizeForTask(this.jointProbabilities, t.task);
			if(marginal > maxProb){
				maxProb = marginal;
				task = t;
			}
		}
		return task;
	}

	protected void updateJointDistribution(State s, GroundedAction ga, double f){
		double [] propPosts = new double[this.jointProbabilities.size()];
		double sum = 0.;
		for(int i = 0; i < this.jointProbabilities.size(); i++){
			double p = this.jointProbabilities.get(i).proportionalPosterior(s, ga, f);
			sum += p;
			propPosts[i] = p;
		}

		for(int i = 0; i < this.jointProbabilities.size(); i++){
			double normed = propPosts[i] / sum;
			this.jointProbabilities.get(i).prob = normed;
		}
	}


	protected GroundedAction worldAction(State s, GroundedAction ga){
		List <GroundedAction> wgas = s.getAllGroundedActionsFor(this.actions);
		for(GroundedAction wga : wgas){
			if(wga.equals(ga)){
				return wga;
			}
		}
		return null;
	}



}
