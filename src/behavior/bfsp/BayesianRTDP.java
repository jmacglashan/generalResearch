package behavior.bfsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.rtdp.BoundedRTDP;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;

public class BayesianRTDP extends BoundedRTDP {

	
	protected Policy			policyPrior;
	
	public BayesianRTDP(Domain domain, RewardFunction rf, TerminalFunction tf,
			double gamma, StateHashFactory hashingFactory,
			ValueFunctionInitialization lowerVInit,
			ValueFunctionInitialization upperVInit,
			Policy policyPrior, int maxRollouts) {
		super(domain, rf, tf, gamma, hashingFactory, lowerVInit, upperVInit, 0,
				maxRollouts);
		this.policyPrior = policyPrior;
	}
	
	
	
	
	
	/**
	 * Runs a planning rollout from the provided state.
	 * @param s the initial state from which a planning rollout should be performed.
	 * @return the margin between the lower bound and upper bound value function for the initial state.
	 */
	public double runRollout(State s){
		LinkedList<StateHashTuple> trajectory = new LinkedList<StateHashTuple>();
		
		StateHashTuple csh = this.hashingFactory.hashState(s);
		
		while(!this.tf.isTerminal(csh.s) && (trajectory.size() < this.maxDepth+1 || this.maxDepth == -1)){
			
			if(this.runRolloutsInReverse){
				trajectory.offerFirst(csh);
			}
			
			this.setValueFunctionToLowerBound();
			List<QValue> lowerQs = this.getQs(csh.s);
			QValue mxL = this.maxQ(lowerQs);
			this.lowerBoundV.put(csh, mxL.q);
			
			this.setValueFunctionToUpperBound();
			List<QValue> upperQs = this.getQs(csh.s);
			QValue mxU = this.maxQ(upperQs);
			this.upperBoundV.put(csh, mxU.q);
			
			numBellmanUpdates += 2;
			this.numSteps++;
			
			GroundedAction a = this.selectAction(csh.s, lowerQs, upperQs);
			
			StateSelectionAndExpectedGap select = this.getNextState(csh.s, a);
			csh = select.sh;
			
			if(select.expectedGap < this.maxDiff){
				break;
			}
			
			
		}
		
		if(this.tf.isTerminal(csh.s)){
			this.lowerBoundV.put(csh, 0.);
			this.upperBoundV.put(csh, 0.);
		}
		
		
		double lastGap = 0.;
		
		//run in reverse
		if(this.runRolloutsInReverse){
			while(trajectory.size() > 0){
				StateHashTuple sh = trajectory.pop();
				this.setValueFunctionToLowerBound();
				QValue mxL = this.maxQ(sh.s);
				this.lowerBoundV.put(sh, mxL.q);
				
				this.setValueFunctionToUpperBound();
				QValue mxU = this.maxQ(sh.s);
				this.upperBoundV.put(sh, mxU.q);
				
				numBellmanUpdates += 2;
				lastGap = mxU.q - mxL.q;
				
			}
		}
		else{
			lastGap = this.getGap(this.hashingFactory.hashState(s));
		}
		
		
		if(this.defaultToLowerValueAfterPlanning){
			this.setValueFunctionToLowerBound();
		}
		else{
			this.setValueFunctionToUpperBound();
		}
		
		return lastGap;
		
	}
	
	
	
	protected GroundedAction selectAction(State s, List<QValue> lowerQs, List<QValue> upperQs){
		
		return this.selectActionBySampling(s, lowerQs, upperQs);
		//return this.selectActionByMAP(s, lowerQs, upperQs);
		
	}
	
	protected GroundedAction selectActionByMAP(State s, List<QValue> lowerQs, List<QValue> upperQs){
		
		double maxNum = Double.NEGATIVE_INFINITY;
		List<Integer> maxOptions = new ArrayList<Integer>(lowerQs.size());
		for(int i = 0; i < lowerQs.size(); i++){
			double prior = this.policyPrior.getProbOfAction(s, (GroundedAction)lowerQs.get(i).a);
			double posterior = prior * this.probGreater(i, lowerQs, upperQs);
			if(posterior == maxNum){
				maxOptions.add(i);
			}
			else if(posterior > maxNum){
				maxNum = posterior;
				maxOptions.clear();
				maxOptions.add(i);
			}
		}
		
		
		if(maxOptions.size() == 0){
			for(int i = 0; i < lowerQs.size(); i++){
				double prior = this.policyPrior.getProbOfAction(s, (GroundedAction)lowerQs.get(i).a);
				double posterior = prior * this.probGreater(i, lowerQs, upperQs);
				if(posterior == maxNum){
					maxOptions.add(i);
				}
				else if(posterior > maxNum){
					maxNum = posterior;
					maxOptions.clear();
					maxOptions.add(i);
				}
			}
		}
		
		int rint = RandomFactory.getMapped(0).nextInt(maxOptions.size());
		return (GroundedAction)lowerQs.get(maxOptions.get(rint)).a;
		
		
	}
	
	protected GroundedAction selectActionBySampling(State s, List<QValue> lowerQs, List<QValue> upperQs){
		double [] numerators = new double[lowerQs.size()];
		double sum = 0.;
		for(int i = 0; i < lowerQs.size(); i++){
			double prior = this.policyPrior.getProbOfAction(s, (GroundedAction)lowerQs.get(i).a);
			numerators[i] = prior * this.probGreater(i, lowerQs, upperQs);
			sum += numerators[i];
		}
		
		double cumSum = 0.;
		//sample action
		double roll = RandomFactory.getMapped(0).nextDouble();
		for(int i = 0; i < numerators.length; i++){
			cumSum += (numerators[i] / sum);
			if(roll < cumSum){
				return (GroundedAction)lowerQs.get(i).a;
			}
		}
		
		
		//failed, try again
		numerators = new double[lowerQs.size()];
		sum = 0.;
		for(int i = 0; i < lowerQs.size(); i++){
			double prior = this.policyPrior.getProbOfAction(s, (GroundedAction)lowerQs.get(i).a);
			numerators[i] = prior * this.probGreater(i, lowerQs, upperQs);
			sum += numerators[i];
		}
		
		throw new RuntimeException("Error! probablities for actions did not sum to 1.");
	}
	
	protected double probGreater(int qIndex, List<QValue> lowerQs, List<QValue> upperQs){
		
		QValue queryLower = lowerQs.get(qIndex);
		QValue queryUpper = upperQs.get(qIndex);
		
		double product = 1.;
		for(int i = 0; i < lowerQs.size(); i++){
			if(i == qIndex){
				continue;
			}
			QValue qLower = lowerQs.get(i);
			QValue qUpper = upperQs.get(i);
			double pGreater = UniGreaterProb.probXGreaterOrEqualThanY(queryLower.q, queryUpper.q, qLower.q, qUpper.q);
			product *= pGreater;
		}
		
		return product;
	}

	
	/**
	 * Returns the maximum Q-value entry in the given list with ties broken randomly. 
	 * @param s the query state for the Q-value
	 * @return the maximum Q-value entry for the given state with ties broken randomly. 
	 */
	protected QValue maxQ(List<QValue> qs){
		
		double max = Double.NEGATIVE_INFINITY;
		List<QValue> maxQs = new ArrayList<QValue>(qs.size());
		
		for(QValue q : qs){
			if(q.q == max){
				maxQs.add(q);
			}
			else if(q.q > max){
				max = q.q;
				maxQs.clear();
				maxQs.add(q);
			}
		}
		
		//return random max
		int rint = RandomFactory.getMapped(0).nextInt(maxQs.size());
		
		return maxQs.get(rint);
	}
	
	
	
	
	
	
	
	protected static class UniformPolicyPrior extends Policy{

		protected Domain domain;
		
		public UniformPolicyPrior(Domain domain){
			this.domain = domain;
		}
		
		@Override
		public AbstractGroundedAction getAction(State s) {
			List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(domain.getActions(), s);
			int rint = RandomFactory.getMapped(0).nextInt(gas.size());
			return gas.get(rint);
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(domain.getActions(), s);
			List<ActionProb> probs = new ArrayList<Policy.ActionProb>(gas.size());
			double u = 1./(double)gas.size();
			for(GroundedAction ga : gas){
				probs.add(new ActionProb(ga, u));
			}
			return probs;
		}

		@Override
		public boolean isStochastic() {
			return true;
		}

		@Override
		public boolean isDefinedFor(State s) {
			return true;
		}
		
		
	}
	
	
	protected static class StateFreeBiasedPolicy extends Policy{

		protected Domain domain;
		protected double pNorth = 68./168.;
		protected double pSouth = 16./168.;
		protected double pEast = 70./168;
		protected double pWest = 14./168;
		
		public StateFreeBiasedPolicy(Domain domain){
			this.domain = domain;
		}
		
		@Override
		public AbstractGroundedAction getAction(State s) {
			return this.sampleFromActionDistribution(s);
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			List<ActionProb> probs = new ArrayList<Policy.ActionProb>(4);
			probs.add(new ActionProb(new GroundedAction(domain.getAction(GridWorldDomain.ACTIONNORTH), ""), pNorth));
			probs.add(new ActionProb(new GroundedAction(domain.getAction(GridWorldDomain.ACTIONSOUTH), ""), pSouth));
			probs.add(new ActionProb(new GroundedAction(domain.getAction(GridWorldDomain.ACTIONEAST), ""), pEast));
			probs.add(new ActionProb(new GroundedAction(domain.getAction(GridWorldDomain.ACTIONWEST), ""), pWest));
			
			return probs;
		}

		@Override
		public boolean isStochastic() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isDefinedFor(State s) {
			// TODO Auto-generated method stub
			return false;
		}
		
		
		
	}
	
	
	public static class SoftenedSampledPolicy extends Policy{

		Map<StateHashTuple, List<ActionProb>> softendPolicy;
		protected StateHashFactory hashingFactory;
		protected List<ActionProb> unknownPrior;
		
		
		public SoftenedSampledPolicy(Policy source, Domain domain, State sourceState, int numStates, double bias, StateHashFactory hashingFactory){
			this.hashingFactory = hashingFactory;
			Set<StateHashTuple> reachableStatesSet = StateReachability.getReachableHashedStates(sourceState, (SADomain)domain, hashingFactory);
			List<StateHashTuple> reachableStates = new ArrayList<StateHashTuple>(reachableStatesSet);
			this.softendPolicy = new HashMap<StateHashTuple, List<ActionProb>>(numStates);
			Random rand = RandomFactory.getMapped(0);
			List<GroundedAction> actions = Action.getAllApplicableGroundedActionsFromActionList(domain.getActions(), sourceState);
			double negBias = (1. - bias) / (double)(actions.size()-1);
			this.unknownPrior = new ArrayList<Policy.ActionProb>(actions.size());
			for(GroundedAction ga : actions){
				this.unknownPrior.add(new ActionProb(ga, 1./(double)actions.size()));
			}
			while(softendPolicy.size() < numStates){
				StateHashTuple sh = reachableStates.get(rand.nextInt(reachableStates.size()));
				if(!this.softendPolicy.containsKey(sh)){
					
					GroundedAction sel = (GroundedAction)source.getAction(sh.s);
					List<ActionProb> aps = new ArrayList<Policy.ActionProb>(actions.size());
					for(GroundedAction ga : actions){
						if(!ga.equals(sel)){
							aps.add(new ActionProb(ga, negBias));
						}
						else{
							aps.add(new ActionProb(ga, bias));
						}
					}
					
					this.softendPolicy.put(sh, aps);
					
				}
			}
		}
		
		@Override
		public AbstractGroundedAction getAction(State s) {
			return this.sampleFromActionDistribution(s);
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			StateHashTuple sh = this.hashingFactory.hashState(s);
			List<ActionProb> aps = this.softendPolicy.get(sh);
			if(aps != null){
				return aps;
			}
			return unknownPrior;
		}

		@Override
		public boolean isStochastic() {
			return true;
		}

		@Override
		public boolean isDefinedFor(State s) {
			return true;
		}
		
		
		
		
	}
	
	public static void main(String [] args){
		
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		//gwd.setProbSucceedTransitionDynamics(0.8);
		Domain domain = gwd.generateDomain();
		State s = GridWorldDomain.getOneAgentNoLocationState(domain);
		GridWorldDomain.setAgent(s, 0, 0);
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		
		
		//do exact planning for softend policy prior
		/*
		ValueIteration vi = new ValueIteration(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0.01, 100);
		vi.planFromState(s);
		Policy viPolicy = new GreedyQPolicy(vi);
		SoftenedSampledPolicy ssp = new SoftenedSampledPolicy(viPolicy, domain, s, 60, 0.8, new DiscreteStateHashFactory());
		*/
		
		BayesianRTDP bayes = new BayesianRTDP(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-100), 
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.), 
				//new UniformPolicyPrior(domain),
				new StateFreeBiasedPolicy(domain),
				//ssp,
				10);
		
		
		BoundedRTDP brtdp = new BoundedRTDP(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(-60), 
				new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.), 
				.5, 10);
		
		Policy bayesLower = new GreedyQPolicy(bayes);
		Policy bp = new GreedyQPolicy(brtdp);
		
		int maxSteps = 1000;
		int numTrials = 10;
		for(int i = 0; i < 1; i++){
			bayes.runRollout(s);
			//rtdp.normalRTDP(s);
			brtdp.runRollout(s);
			
			double bperf = evalPolicy(bp, s, rf, tf, maxSteps, numTrials);
			double bayesperf = evalPolicy(bayesLower, s, rf, tf, maxSteps, numTrials);
			//double rperf = evalPolicy(rp, s, rf, tf, maxSteps, numTrials);
			
			//System.out.println(brtdp.getNumberOfSteps() + ", " + bperf + ", " + rtdp.getNumberOfBellmanUpdates() + ", " + rperf);
			//System.out.println(brtdp.getNumberOfBellmanUpdates() + ", " + bperf + ", " + rtdp.getNumberOfBellmanUpdates() + ", " + rperf);
			System.out.println(bayes.getNumberOfSteps() + ", " + bayesperf + ", " + brtdp.getNumberOfSteps() + ", " + bperf);
		}
		
	}
	
	
	
	protected static double evalPolicy(Policy p, State s, RewardFunction rf, TerminalFunction tf, int maxSteps, int numTrials){
		
		double sum = 0.;
		for(int i = 0; i < numTrials; i++){
			EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, maxSteps);
			sum += ea.getDiscountedReturn(1.);
		}
		
		return sum/numTrials;
		
	}
	
}
