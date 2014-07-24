package irl.mlirl;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D.PolicyGlyphRenderStyle;
import burlap.behavior.singleagent.planning.ActionTransitions;
import burlap.behavior.singleagent.planning.HashedTransitionProbability;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.DPrint;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.visualizer.Visualizer;

public class DifferentiableVI extends DifferentiableVFPlanner {

	/**
	 * When the maximum change in the value function is smaller than this value, VI will terminate.
	 */
	protected double												maxDelta;
	
	/**
	 * When the number of VI iterations exceeds this value, VI will terminate.
	 */
	protected int													maxIterations;
	
	
	/**
	 * Indicates whether the reachable states has been computed yet.
	 */
	protected boolean												foundReachableStates = false;
	
	
	/**
	 * When the reachability analysis to find the state space is performed, a breadth first search-like pass
	 * (spreading over all stochastic transitions) is performed. It can optionally be set so that the
	 * search is pruned at terminal states by setting this value to true. By default, it is false and the full
	 * reachable state space is found
	 */
	protected boolean												stopReachabilityFromTerminalStates = false;
	
	
	protected boolean												hasRunVI = false;
	
	
	
	/**
	 * Initializers the planner.
	 * @param domain the domain in which to plan
	 * @param rf the differentiable reward function that will be used
	 * @param tf the terminal state function
	 * @param gamma the discount factor
	 * @param boltzBeta the scaling factor in the boltzmann distirbution used for the state value function. The larger the value, the more deterministic.
	 * @param hashingFactory the state hashing factor to use
	 * @param maxDelta when the maximum change in the value function is smaller than this value, VI will terminate.
	 * @param maxIterations when the number of VI iterations exceeds this value, VI will terminate.
	 */
	public DifferentiableVI(Domain domain, DifferentiableRF rf, TerminalFunction tf, double gamma, double boltzBeta, StateHashFactory hashingFactory, double maxDelta, int maxIterations){
		
		this.VFPInit(domain, rf, tf, gamma, hashingFactory);
		
		this.maxDelta = maxDelta;
		this.maxIterations = maxIterations;
		this.boltzBeta = boltzBeta;
		
	}
	
	
	/**
	 * Calling this method will force the planner to recompute the reachable states when the {@link #planFromState(State)} method is called next.
	 * This may be useful if the transition dynamics from the last planning call have changed and if planning needs to be restarted as a result.
	 */
	public void recomputeReachableStates(){
		this.foundReachableStates = false;
		this.transitionDynamics = new HashMap<StateHashTuple, List<ActionTransitions>>();
	}
	
	
	/**
	 * Sets whether the state reachability search to generate the state space will be prune the search from terminal states.
	 * The default is not to prune.
	 * @param toggle true if the search should prune the search at terminal states; false if the search should find all reachable states regardless of terminal states.
	 */
	public void toggleReachabiltiyTerminalStatePruning(boolean toggle){
		this.stopReachabilityFromTerminalStates = toggle;
	}
	
	
	@Override
	public void planFromState(State initialState){
		this.initializeOptionsForExpectationComputations();
		if(this.performReachabilityFrom(initialState) || !this.hasRunVI){
			this.runVI();
		}
			
	}
	
	@Override
	public void resetPlannerResults(){
		super.resetPlannerResults();
		this.foundReachableStates = false;
		this.hasRunVI = false;
	}
	
	/**
	 * Runs VI until the specified termination conditions are met. In general, this method should only be called indirectly through the {@link #planFromState(State)} method.
	 * The {@link #performReachabilityFrom(State)} must have been performed at least once
	 * in the past or a runtime exception will be thrown. The {@link #planFromState(State)} method will automatically call the {@link #performReachabilityFrom(State)} 
	 * method first and then this if it hasn't been run.
	 */
	public void runVI(){
		
		if(!this.foundReachableStates){
			throw new RuntimeException("Cannot run VI until the reachable states have been found. Use the planFromState, performReachabilityFrom, addStateToStateSpace or addStatesToStateSpace methods at least once before calling runVI.");
		}
		
		Set <StateHashTuple> states = mapToStateIndex.keySet();
		
		int i = 0;
		for(i = 0; i < this.maxIterations; i++){
			
			double delta = 0.;
			for(StateHashTuple sh : states){
				
				double v = this.value(sh);
				double newV = this.performBellmanUpdateOn(sh);
				this.performDPValueGradientUpdateOn(sh);
				delta = Math.max(Math.abs(newV - v), delta);
				
			}
			
			if(delta < this.maxDelta){
				break; //approximated well enough; stop iterating
			}
			
		}
		
		DPrint.cl(this.debugCode, "Passes: " + i);
		
		this.hasRunVI = true;
		
	}
	
	
	public void addStateToStateSpace(State s){
		StateHashTuple sh = this.hashingFactory.hashState(s);
		this.mapToStateIndex.put(sh, sh);
		this.foundReachableStates = true;
	}
	
	public void addStatesToStateSpace(Collection<State> states){
		for(State s : states){
			this.addStateToStateSpace(s);
		}
	}
	
	/**
	 * This method will find all reachable states that will be used by the {@link #runVI()} method and will cache all the transition dynamics.
	 * This method will not do anything if all reachable states from the input state have been discovered from previous calls to this method.
	 * @param si the source state from which all reachable states will be found
	 * @return true if a reachability analysis had never been performed from this state; false otherwise.
	 */
	public boolean performReachabilityFrom(State si){
		
		
		
		StateHashTuple sih = this.stateHash(si);
		//if this is not a new state and we are not required to perform a new reachability analysis, then this method does not need to do anything.
		if(mapToStateIndex.containsKey(sih) && this.foundReachableStates){
			return false; //no need for additional reachability testing
		}
		
		DPrint.cl(this.debugCode, "Starting reachability analysis");
		
		//add to the open list
		LinkedList <StateHashTuple> openList = new LinkedList<StateHashTuple>();
		Set <StateHashTuple> openedSet = new HashSet<StateHashTuple>();
		openList.offer(sih);
		openedSet.add(sih);
		
		
		while(openList.size() > 0){
			StateHashTuple sh = openList.poll();
			
			//skip this if it's already been expanded
			if(mapToStateIndex.containsKey(sh)){
				continue;
			}
			
			mapToStateIndex.put(sh, sh);
			
			//do not need to expand from terminal states if set to prune
			if(this.tf.isTerminal(sh.s) && stopReachabilityFromTerminalStates){
				continue;
			}
			
			
			//get the transition dynamics for each action and queue up new states
			List <ActionTransitions> transitions = this.getActionsTransitions(sh);
			for(ActionTransitions at : transitions){
				for(HashedTransitionProbability tp : at.transitions){
					StateHashTuple tsh = tp.sh;
					if(!openedSet.contains(tsh) && !transitionDynamics.containsKey(tsh)){
						openedSet.add(tsh);
						openList.offer(tsh);
					}
				}
				
			}
			
			
		}
		
		DPrint.cl(this.debugCode, "Finished reachability analysis; # states: " + mapToStateIndex.size());
		
		this.foundReachableStates = true;
		this.hasRunVI = false;
		
		return true;
		
	}
	
	
	
	
	
	
	public static void main(String [] args){
		final GridWorldDomain gw = new GridWorldDomain(11, 11);
		gw.setMapToFourRooms();
		final Domain domain = gw.generateDomain();
		final TerminalFunction tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
		
		final StateToFeatureVectorGenerator fv = new StateToFeatureVectorGenerator() {
			
			@Override
			public double[] generateFeatureVectorFrom(State s) {
				double [] fv = new double[1];
				fv[0] = 0.;
				if(tf.isTerminal(s)){
					fv[0] = 1.;
				}
				return fv;
			}
		};
		
		final DifferentiableRF.LinearStateDifferentiableRF rf = new DifferentiableRF.LinearStateDifferentiableRF(fv, 1);
		rf.setParameter(0, 1.);
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		DifferentiableVI vi = new DifferentiableVI(domain, rf, tf, 0.99, 10, hashingFactory, 0.001, 500);
		
		State initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
		GridWorldDomain.setAgent(initialState, 0, 0);
		GridWorldDomain.setLocation(initialState, 0, 10, 10);
		
		
		State nearGoal = initialState.copy();
		GridWorldDomain.setAgent(nearGoal, 10, 9);
		State nearishGoal = initialState.copy();
		GridWorldDomain.setAgent(nearishGoal, 10, 8);
		
		State goalState = initialState.copy();
		GridWorldDomain.setAgent(goalState, 10, 10);
		
		//vi.addStateToStateSpace(nearGoal);
		//vi.runVI();
		
		
		vi.planFromState(initialState);
		
		
		Policy p = new GreedyQPolicy(vi);
		
		/*
		EpisodeAnalysis ea = p.evaluateBehavior(nearGoal, rf, tf, 40);
		
		StateParser sp = new GridWorldStateParser(domain);
		ea.writeToFile("DiffVI/plan", sp);
		
		Visualizer v = GridWorldVisualizer.getVisualizer(gw.getMap());
		new EpisodeSequenceVisualizer(v, domain, sp, "DiffVI");
		*/
		
		double [] gradFar = vi.getValueGradient(initialState);
		double [] gradNear = vi.getValueGradient(nearGoal);
		double [] gradNearish = vi.getValueGradient(nearishGoal);
		
		System.out.println(gradFar[0]);
		System.out.println(gradNear[0]);
		System.out.println(gradNearish[0]);
		
		System.out.println("------");
		
		List<QValue> qs = vi.getQs(goalState);
		for(QValue q : qs){
			System.out.println(q.a.toString() + ": " + q.q);
		}
		
		
		List <State> allStates = StateReachability.getReachableStates(initialState, (SADomain)domain, hashingFactory);
		LandmarkColorBlendInterpolation rb = new LandmarkColorBlendInterpolation();
		rb.addNextLandMark(0., Color.RED);
		rb.addNextLandMark(1., Color.BLUE);
		
		StateValuePainter2D svp = new StateValuePainter2D(rb);
		svp.setXYAttByObjectClass(GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTX, GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTY);
		
		PolicyGlyphPainter2D spp = new PolicyGlyphPainter2D();
		spp.setXYAttByObjectClass(GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTX, GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTY);
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONNORTH, new ArrowActionGlyph(0));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONSOUTH, new ArrowActionGlyph(1));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONEAST, new ArrowActionGlyph(2));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONWEST, new ArrowActionGlyph(3));
		spp.setRenderStyle(PolicyGlyphRenderStyle.MAXACTIONSOFTTIE);
		//spp.setSoftTieRenderStyleDelta(0.1);
		
		
		ValueFunctionVisualizerGUI gui = new ValueFunctionVisualizerGUI(allStates, svp, vi);
		gui.setSpp(spp);
		gui.setPolicy(p);
		gui.setBgColor(Color.GRAY);
		gui.initGUI();
		
		
		
	}

}
