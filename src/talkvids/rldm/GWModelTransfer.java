package talkvids.rldm;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.modelplanners.VIModelPlanner;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author James MacGlashan.
 */
public class GWModelTransfer {

	GridWorldDomain gwd;
	Domain domain;
	State s;
	RewardFunction rf;
	StateHashFactory hashingFactory;

	public GWModelTransfer(){
		this.gwd = new GridWorldDomain(11, 11);
		this.gwd.setMapToFourRooms();
		this.domain = this.gwd.generateDomain();
		this.rf = new UniformCostRF();
		//this.s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		this.s = GridWorldDomain.getOneAgentNLocationState(domain, 1);
		GridWorldDomain.setAgent(s, 0, 0);
		GridWorldDomain.setLocation(s, 0, 10 ,10);

		RandomFactory.seedMapped(0, 123);

		//this.hashingFactory = new DiscreteStateHashFactory();
		this.hashingFactory = new DiscreteMaskHashingFactory();
		((DiscreteMaskHashingFactory)hashingFactory).setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);
	}

	public PotentialShapedRMax getInitialProblemAgent(){

		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		PotentialShapedRMax rmax = new PotentialShapedRMax(domain, rf, tf, 0.99, this.hashingFactory, 0, 1, 0.01, 20);


		for(int i = 0; i < 25; i++){
			EpisodeAnalysis ea = rmax.runLearningEpisodeFrom(s);
			System.out.println(i + ": " + ea.maxTimeStep());
		}


		return rmax;

	}

	public void explorer(){

		State ms = GridWorldDomain.getOneAgentNLocationState(domain, 16);
		ms.removeObject("agent0");

		GridWorldDomain.setLocation(ms, 0, 0, 0);
		GridWorldDomain.setLocation(ms, 1, 0, 4);
		GridWorldDomain.setLocation(ms, 2, 4, 0);
		GridWorldDomain.setLocation(ms, 3, 4, 4);

		GridWorldDomain.setLocation(ms, 4, 6, 0);
		GridWorldDomain.setLocation(ms, 5, 6, 3);
		GridWorldDomain.setLocation(ms, 6, 10, 3);
		GridWorldDomain.setLocation(ms, 7, 10, 0);

		GridWorldDomain.setLocation(ms, 8, 6, 5);
		GridWorldDomain.setLocation(ms, 9, 6, 10);
		GridWorldDomain.setLocation(ms, 10, 10, 10);
		GridWorldDomain.setLocation(ms, 11, 10, 5);

		GridWorldDomain.setLocation(ms, 12, 0, 6);
		GridWorldDomain.setLocation(ms, 13, 0, 10);
		GridWorldDomain.setLocation(ms, 14, 4, 10);
		GridWorldDomain.setLocation(ms, 15, 4, 6);

		Visualizer v = GridWorldVisualizer.getVisualizer(this.gwd.getMap());

		VisualExplorer exp = new VisualExplorer(domain, v, ms);

		exp.initGUI();

	}

	public void transferAgent(PotentialShapedRMax sourceAgent){

		Model sourceModel = sourceAgent.getModel();


		//update source model for previous terminal state as if it had seen it
		GroundedAction tga = null;
		State oldT = GridWorldDomain.getOneAgentNoLocationState(this.domain, 10, 10);

		tga = new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONNORTH), "");
		sourceModel.updateModel(oldT, tga, tga.executeIn(oldT), -1, false);

		tga = new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONSOUTH), "");
		sourceModel.updateModel(oldT, tga, tga.executeIn(oldT), -1, false);

		tga = new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONEAST), "");
		sourceModel.updateModel(oldT, tga, tga.executeIn(oldT), -1, false);

		tga = new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONWEST), "");
		sourceModel.updateModel(oldT, tga, tga.executeIn(oldT), -1, false);


		GridWorldDomain.setLocation(this.s, 0, 0, 10);

		TerminalFunction tf = new GridWorldTerminalFunction(0, 10);
		PartialModel targetModel = new PartialModel(this.domain, this.hashingFactory, sourceModel);

		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 0, 4));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 4, 4));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 4, 0));

		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 6, 0));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 6, 3));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 10, 3));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 10, 0));

		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 6, 5));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 6, 10));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 10, 10));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 10, 5));

		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 0, 6));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 0, 10));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 4, 10));
		targetModel.addTerminalStateCandidate(GridWorldDomain.getOneAgentNoLocationState(domain, 4, 6));


		PotentialShapedRMax rmax = new PotentialShapedRMax(domain, rf, tf, 0.99,
				this.hashingFactory, new PotentialShapedRMax.RMaxPotential(0),targetModel,
				new VIModelPlanner.VIModelPlannerGenerator(hashingFactory, 0.01, 100));



		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		VisualActionObserver ob = new VisualActionObserver(domain, v);
		ob.setFrameDelay((long)(1./10.*1000));
		((SADomain)domain).addActionObserverForAllAction(ob);
		ob.initGUI();
		v.updateState(s);

		try {
			Thread.sleep(25000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}


		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		for(int i = 0; i < 1; i++){
			EpisodeAnalysis ea = rmax.runLearningEpisodeFrom(s);
			episodes.add(ea);
			System.out.println(i + ": " + ea.maxTimeStep());

			try {
				Thread.sleep((long) (1. / 10. * 1000));
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}




		/*
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		State curState = s;
		for(int i = 0; i < 81; i++){
			System.out.println("step : " + i);
			EpisodeAnalysis ea = rmax.runLearningEpisodeFrom(curState, 1);
			State lState = ea.getState(ea.maxTimeStep());
			if(!tf.isTerminal(lState)){
				curState = lState;
			}
			else{
				curState = lState;
			}
			episodes.add(ea);
		}
		*/

		//Visualizer v = GridWorldVisualizer.getVisualizer(this.gwd.getMap());
		//EpisodeSequenceVisualizer exp = new EpisodeSequenceVisualizer(v, domain, episodes);


	}

	public void transferLearning(){
		PotentialShapedRMax agent = this.getInitialProblemAgent();
		System.out.println("==========================");
		this.transferAgent(agent);
	}



	public static class PartialModel extends Model{

		protected Domain domain;
		protected StateHashFactory hashingFactory;
		protected Model sourceModel;
		protected StateHashTuple terminalState;
		protected Set<StateHashTuple> candidateTerminalStates = new HashSet<StateHashTuple>();
		protected TerminalFunction tf = new PMTerminalFunction();

		protected int falseHits = 0;

		public PartialModel(Domain domain, StateHashFactory hashingFactory, Model sourceModel){
			this.domain = domain;
			this.hashingFactory = hashingFactory;
			this.sourceModel = sourceModel;
		}


		public void addTerminalStateCandidate(State s){
			this.candidateTerminalStates.add(this.hashingFactory.hashState(s));
		}

		@Override
		public RewardFunction getModelRF() {
			return this.sourceModel.getModelRF();
		}

		@Override
		public TerminalFunction getModelTF() {
			return tf;
		}

		@Override
		public boolean transitionIsModeled(State s, GroundedAction ga) {
			if(this.terminalModeled(s)){
				return this.sourceModel.transitionIsModeled(s, ga);
			}

			return false;

		}

		@Override
		public boolean stateTransitionsAreModeled(State s) {
			if(this.terminalModeled(s)) {
				return this.sourceModel.stateTransitionsAreModeled(s);
			}
			return false;
		}

		@Override
		public List<AbstractGroundedAction> getUnmodeledActionsForState(State s) {
			if(this.terminalModeled(s)) {
				return this.sourceModel.getUnmodeledActionsForState(s);
			}
			return new ArrayList<AbstractGroundedAction>(Action.getAllApplicableGroundedActionsFromActionList(this.domain.getActions(), s));
		}

		@Override
		public State sampleModelHelper(State s, GroundedAction ga) {
			return this.sourceModel.sampleModelHelper(s, ga);
		}

		@Override
		public List<TransitionProbability> getTransitionProbabilities(State s, GroundedAction ga) {
			return this.sourceModel.getTransitionProbabilities(s, ga);
		}

		@Override
		public void updateModel(State s, GroundedAction ga, State sprime, double r, boolean sprimeIsTerminal) {
			//System.out.println("In update model");
			int oldSize = this.candidateTerminalStates.size();
			this.sourceModel.updateModel(s, ga, sprime, r, sprimeIsTerminal);
			StateHashTuple sh = this.hashingFactory.hashState(s);
			this.candidateTerminalStates.remove(sh);
			if(sprimeIsTerminal){
				this.terminalState = this.hashingFactory.hashState(sprime);
			}
			else{
				StateHashTuple sph = this.hashingFactory.hashState(sprime);
				this.candidateTerminalStates.remove(sph);
			}

			int newSize = this.candidateTerminalStates.size();
			if(oldSize != newSize){
				//System.out.println("t size: " + oldSize + " -> " + newSize);
			}
		}

		@Override
		public void resetModel() {
			this.sourceModel.resetModel();
		}

		public boolean terminalModeled(State s){
			if(this.terminalState != null){
				return true;
			}

			StateHashTuple sh = this.hashingFactory.hashState(s);
			if(this.candidateTerminalStates.contains(sh)){
				return false;
			}
			return true;
		}


		public class PMTerminalFunction implements TerminalFunction{

			@Override
			public boolean isTerminal(State s) {
				if(terminalState == null){
					return false;
				}
				StateHashTuple sh = hashingFactory.hashState(s);
				return sh.equals(terminalState);
			}
		}
	}



	public static void main(String[] args) {
		GWModelTransfer exp = new GWModelTransfer();
		//exp.getInitialProblemAgent();
		exp.transferLearning();
		//exp.explorer();
	}

}
