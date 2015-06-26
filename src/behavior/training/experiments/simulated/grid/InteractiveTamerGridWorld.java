package behavior.training.experiments.simulated.grid;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.burlapirlext.*;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.training.DynamicFeedbackGUI;
import behavior.training.taskinduction.CSABL;
import behavior.training.taskinduction.FeedbackTuple;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.strataware.CSABLAgent;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateEnumerator;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateActionDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.*;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import cern.colt.Arrays;
import tests.irltests.StateRewardFunctionValue;

public class InteractiveTamerGridWorld {

	protected GridWorldDomain					gwg;
	protected Domain							domain;
	protected State								initialState;
	protected DiscreteMaskHashingFactory		hashingFactory;
	protected Visualizer						visualizer;
	
	
	public static void main(String [] args){
		InteractiveTamerGridWorld e = new InteractiveTamerGridWorld();
		//e.runInteractiveTraining();
		e.interactiveCSABL();
		//e.testCSABLPlanner();
		//e.testHardCodedCSABL();
		//e.testHardCodedCSABL2();
		//e.testHardCodedCSABL3();
	}
	
	
	public InteractiveTamerGridWorld(){
		
		this.gwg = new GridWorldDomain(6, 6);
		
		
		this.gwg.horizontal1DNorthWall(1, 4, 4);
		this.gwg.horizontal1DNorthWall(1, 4, 0);
		
		this.gwg.verticalWall(1, 4, 1);
		this.gwg.horizontalWall(3, 4, 3);
		this.gwg.set1DEastWall(4, 5);
		
		this.domain = this.gwg.generateDomain();
		
		this.initialState = GridWorldDomain.getOneAgentNLocationState(domain, 0);
		GridWorldDomain.setAgent(this.initialState, 4, 5);
		
		this.visualizer = GridWorldVisualizer.getVisualizer(domain, gwg.getMap());
		
		this.hashingFactory = new DiscreteMaskHashingFactory();
		this.hashingFactory.addAttributeForClass(GridWorldDomain.CLASSAGENT, this.domain.getAttribute(GridWorldDomain.ATTX));
		this.hashingFactory.addAttributeForClass(GridWorldDomain.CLASSAGENT, this.domain.getAttribute(GridWorldDomain.ATTY));
		
		
	}
	
	public void testCSABLPlanner(){
		
		StateToFeatureVectorGenerator rfFV = this.getRBFsForEachState(this.initialState, this.domain, 0.1);
		DifferentiableRF learningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		double [] rfParams = learningRF.getParameters();
		for(int i = 0; i < rfParams.length; i++){
			rfParams[i] = 1.;
		}
		
		double [] fvVals = rfFV.generateFeatureVectorFrom(initialState);
		System.out.println(Arrays.toString(fvVals));
		
		DifferentiableSparseSampling planner = new DifferentiableSparseSampling(this.domain, learningRF, new NullTermination(), 0.99, hashingFactory, 22, 1, 0.1);
		List<QValue> qs = planner.getQs(this.initialState);
		for(QValue q : qs){
			System.out.println(q.a.toString() + ": " + q.q);
		}
		
	}
	
	public void testHardCodedCSABL(){
		
		double boltzBeta = 0.5;
		
		//first get plan:
		StateToFeatureVectorGenerator rfFV = new StateIndicatorFV(initialState, domain);
		DifferentiableRF planningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		double [] rfParams = planningRF.getParameters();
		rfParams[25] = 1.;
		DifferentiableSparseSampling planner = new DifferentiableSparseSampling(this.domain, planningRF, new NullTermination(), 1, hashingFactory, 19, 1, boltzBeta);
		planner.toggleDebugPrinting(false);
		Policy p = new GreedyQPolicy(planner);
		
		//DifferentiableRF learningRF = new DifferentiableRF.LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length, true);
		DifferentiableRF learningRF = new LinearStateActionDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length,
				new GroundedAction(domain.getAction(GridWorldDomain.ACTIONNORTH), ""),
				new GroundedAction(domain.getAction(GridWorldDomain.ACTIONSOUTH), ""),
				new GroundedAction(domain.getAction(GridWorldDomain.ACTIONEAST), ""),
				new GroundedAction(domain.getAction(GridWorldDomain.ACTIONWEST), ""));
		double [] lparams = learningRF.getParameters();
		DifferentiableSparseSampling learningPlanner = new DifferentiableSparseSampling(this.domain, learningRF, new NullTermination(), 1, hashingFactory, 3, 1, boltzBeta);
		Policy lp = new GreedyQPolicy(learningPlanner);
		
		//get trajectory
		//EpisodeAnalysis ea = p.evaluateBehavior(initialState, planningRF, 20);
		//System.out.println(ea.getActionSequenceString("\n"));
		//System.out.println(ea.getState(ea.numTimeSteps()-1).toString());
	
		
		
		
		
		CSABL csabl = new CSABL(learningRF, domain, 1., boltzBeta, hashingFactory, 0.1, 0.1);
		csabl.setPlanner(learningPlanner);
		
		double alpha = 0.1;
		int numMovements = 1;
		
		State curState = this.initialState;
		int wrong = 0;
		int totalWrong = 0;
		List<FeedbackTuple> feedbacks = new ArrayList<FeedbackTuple>();
		csabl.setFeedbacks(feedbacks);
		while(rfFV.generateFeatureVectorFrom(curState)[25] != 1){
			GroundedAction selected = (GroundedAction)lp.getAction(curState);
			GroundedAction optimal = (GroundedAction)p.getAction(curState);
			FeedbackTuple ft;
			if(!selected.equals(optimal)){
				ft = new FeedbackTuple(curState, selected, -1);
				System.out.println("Incorrect: " + selected.toString());
				wrong++;
				totalWrong++;
			}
			else{
				ft = new FeedbackTuple(curState, selected, 1.);
				System.out.println("Correct after (" +wrong + ") times; " + selected.toString());
				wrong = 0;
			}
			
			feedbacks.add(ft);
			this.resetParams(learningRF.getParameters());
			csabl.runGradientAscent(alpha, 10);
			System.out.println(curState.toString());
			System.out.println("State id: " + ((StateIndicatorFV)rfFV).enumerator.getEnumeratedID(curState));
			for(int i = 0; i < lparams.length; i++){
				//System.out.println(i + ": " + lparams[i]);
			}
			
			/*
			for(int i = 0; i < numMovements; i++){
				csabl.stochasticGradientAscentOnInstance(ft, alpha);
			}
			*/
			
			curState = selected.executeIn(curState);
		}
		
		System.out.println("Total wrong = " + totalWrong);
		
		
		
	}
	
	public void testHardCodedCSABL2(){
		
		double boltzBeta = 10;
		
		//first get plan:
		StateToFeatureVectorGenerator rfFV = new StateIndicatorFV(initialState, domain);
		DifferentiableRF planningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		double [] rfParams = planningRF.getParameters();
		rfParams[25] = 1.;
		DifferentiableSparseSampling planner = new DifferentiableSparseSampling(this.domain, planningRF, new NullTermination(), 1, hashingFactory, 19, 1, boltzBeta);
		planner.toggleDebugPrinting(false);
		Policy p = new GreedyQPolicy(planner);
		
		DifferentiableRF learningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		double [] lparams = learningRF.getParameters();
		DifferentiableSparseSampling learningPlanner = new DifferentiableSparseSampling(this.domain, learningRF, new NullTermination(), 1, hashingFactory, 22, 1, boltzBeta);
		Policy lp = new GreedyQPolicy(learningPlanner);
		
		//get trajectory
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, planningRF, 20);
		
		CSABL csabl = new CSABL(learningRF, domain, 1., boltzBeta, hashingFactory, 0.0, 0.0);
		csabl.setPlanner(learningPlanner);
		
		double alpha = 0.1;
		
		State curState = this.initialState;
		
		List<FeedbackTuple> feedbacks = new ArrayList<FeedbackTuple>();
		for(int i = 0; i < 6; i++){
			FeedbackTuple ft = new FeedbackTuple(ea.getState(i), ea.getAction(i), 1.);
			feedbacks.add(ft);
			System.out.println(ft.s.toString() + "\n" + ft.a.toString() + "\n-------------------");
		}
		
		//now add punish feedback
		FeedbackTuple punishFB = new FeedbackTuple(ea.getState(6), new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONWEST), ""), -1);
		FeedbackTuple rewardFB = new FeedbackTuple(ea.getState(6), new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONSOUTH), ""), 1.);
		feedbacks.add(punishFB);
		//feedbacks.add(rewardFB);
		System.out.println(punishFB.s.toString() + "\n" + punishFB.a.toString() + "\n-------------------");
		
		
		csabl.setFeedbacks(feedbacks);
		
		csabl.runGradientAscent(alpha, 100);
		System.out.println(curState.toString());
		for(int i = 0; i < lparams.length; i++){
			System.out.println(i + ": " + lparams[i]);
		}
		learningPlanner.resetPlannerResults();
		List<QValue> qs = learningPlanner.getQs(ea.getState(6));
		for(QValue q : qs){
			System.out.println(q.q + ": " + q.a.toString());
		}
		
		System.out.println(csabl.logLikelihood());
		
		lparams[29] += 1.3;
		learningPlanner.resetPlannerResults();
		qs = learningPlanner.getQs(ea.getState(6));
		for(QValue q : qs){
			System.out.println(q.q + ": " + q.a.toString());
		}
		System.out.println(csabl.logLikelihood());
		
		learningPlanner.resetPlannerResults();
		csabl.runGradientAscent(alpha, 100);
		for(int i = 0; i < lparams.length; i++){
			System.out.println(i + ": " + lparams[i]);
		}
		qs = learningPlanner.getQs(ea.getState(6));
		for(QValue q : qs){
			System.out.println(q.q + ": " + q.a.toString());
		}
		System.out.println(csabl.logLikelihood());
		
	}
	
	
	public void testHardCodedCSABL3(){
		
		
		double boltzBeta = 10;
		
		//first get plan:
		StateToFeatureVectorGenerator rfFV = new StateIndicatorFV(initialState, domain);
		DifferentiableRF planningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		double [] rfParams = planningRF.getParameters();
		rfParams[25] = 1.;
		DifferentiableSparseSampling planner = new DifferentiableSparseSampling(this.domain, planningRF, new NullTermination(), 1, hashingFactory, 19, 1, boltzBeta);
		planner.toggleDebugPrinting(false);
		Policy p = new GreedyQPolicy(planner);
		
		DifferentiableRF learningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		double [] lparams = learningRF.getParameters();
		DifferentiableSparseSampling learningPlanner = new DifferentiableSparseSampling(this.domain, learningRF, new NullTermination(), 1, hashingFactory, 22, 1, boltzBeta);
		Policy lp = new GreedyQPolicy(learningPlanner);
		
		//get trajectory
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, planningRF, 20);
		
		CSABL csabl = new CSABL(learningRF, domain, 1., boltzBeta, hashingFactory, 0.0, 0.0);
		csabl.setPlanner(learningPlanner);
		
		double alpha = 0.1;
		
		State curState = this.initialState;
		
		List<FeedbackTuple> feedbacks = new ArrayList<FeedbackTuple>();
		
		//now single goal feedback
		FeedbackTuple rewardFB = new FeedbackTuple(ea.getState(ea.numTimeSteps()-2), ea.getAction(ea.numTimeSteps()-2), 1.);
		feedbacks.add(rewardFB);
		System.out.println(rewardFB.s.toString() + "\n" + rewardFB.a.toString() + "\n-------------------");
		
		
		csabl.setFeedbacks(feedbacks);
		
		csabl.runGradientAscent(alpha, 100);
		//System.out.println(curState.toString());
		for(int i = 0; i < lparams.length; i++){
			System.out.println(i + ": " + lparams[i]);
		}
		learningPlanner.resetPlannerResults();
		EpisodeAnalysis lea = lp.evaluateBehavior(this.initialState, learningRF, new GridWorldTerminalFunction(5, 5), 22);
		System.out.println("Result:");
		System.out.println(lea.getActionSequenceString("\n"));
		
	}
	
	protected void resetParams(double [] params){
		for(int i = 0; i < params.length; i++){
			params[i] = 0.;
		}
	}
	
	
	public void interactiveCSABL(){

		//get true policy
		TerminalFunction otf = new GridWorldTerminalFunction(5, 5);
		RewardFunction orf = new UniformCostRF();
		ValueIteration vi = new ValueIteration(this.domain, orf, otf, 0.99, new DiscreteStateHashFactory(), 0.01, 100);
		vi.planFromState(this.initialState);
		Policy noisyObjectivePolicy = new EpsilonGreedy(vi, 0.25);


		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(domain);
		Domain domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();
		
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		DynamicFeedbackGUI gui = new DynamicFeedbackGUI(this.visualizer, env);
		env.setGUI(gui);
		
		//StateToFeatureVectorGenerator rfFV = this.getRBFsForEachState(this.initialState, this.domain, 0.1);
		StateToFeatureVectorGenerator rfFV = new StateIndicatorFV(initialState, domain);
		//StateToFeatureVectorGenerator rfFV = new InitialAndGoalFV();
		DifferentiableRF learningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);
		//learningRF.randomizeParameters(-1., 1., new Random(7372));

		ExplorationShapedRF expRF = new ExplorationShapedRF(learningRF, new VisitationCount.TabularVisitationCount(new DiscreteStateHashFactory()), new VisitationRewardBonus.InverseRatioThresholdBondus(1.0, 1));

		
		double lr = 0.001;
		double beta = 1.;
		double gamma = 1.;
		CSABLAgent agent = new CSABLAgent(domainEnvWrapper, this.domain, gamma, beta, learningRF, trainerRF, trainerTF, new DiscreteStateHashFactory(), 0.0, 0.0, lr, 22, -1);
		//agent.setSourceDomainControlPolicy(noisyObjectivePolicy);
		agent.getCSABL().setPlanner(new DiffExpSS(this.domain, learningRF, expRF, new NullTermination(), gamma, new DiscreteStateHashFactory(),22, -1, beta));
		agent.setSourceDomainControlPolicy(new ExplorationPolicy(new GreedyQPolicy(agent.getCSABL().getPlanner()), (DiffExpSS)agent.getCSABL().getPlanner()));
		agent.setExpRF(expRF);

		StateRewardFunctionValue rfplan = new StateRewardFunctionValue(this.domain, learningRF);
		//StateRewardFunctionValue rfplan = new StateRewardFunctionValue(this.domain, expRF);
		List<State> allStates = StateReachability.getReachableStates(this.initialState, (SADomain)this.domain, new DiscreteStateHashFactory());
		final ValueFunctionVisualizerGUI rfgui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, rfplan, new GreedyQPolicy(agent.getCSABL().getPlanner()));
		rfgui.initGUI();

		ActionObserver rfUpdater = new ActionObserver() {
			@Override
			public void actionEvent(State s, GroundedAction ga, State sp) {
				rfgui.getMultiLayerRenderer().repaint();
			}
		};
		((SADomain)domainEnvWrapper).addActionObserverForAllAction(rfUpdater);



		boolean hasInitedGUI = false;
		for(int i = 0; i < 20; i++){
			env.setCurStateTo(this.initialState);
			if(!hasInitedGUI){
				hasInitedGUI = true;
				gui.initGUI();
				gui.launch();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("Starting episode");
			//now start learning episode
			agent.runLearningEpisodeFrom(this.initialState);
		}
		
		System.out.println("finished training");
		
	}

	public void projectRF(){



	}
	
	public void runInteractiveTraining(){
		
		Action noop = new NullAction("noop", domain, ""); //add noop, which is not attached to anything.
		
		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(domain);
		Domain domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();
		
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		DynamicFeedbackGUI gui = new DynamicFeedbackGUI(this.visualizer, env);
		env.setGUI(gui);
		
		List<TaskDescription> tasks = this.getTaskDescriptions();
		TaskInductionWithFeedbackStrategies agent = new TaskInductionWithFeedbackStrategies(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, tasks, new MAPMixtureModelPolicy());
		agent.addFeedbackStrategy(new FeedbackStrategy(0.5, 0.5, 0.1));
		agent.addFeedbackStrategy(new FeedbackStrategy(0.05, 0.9, 0.1));
		agent.addFeedbackStrategy(new FeedbackStrategy(0.9, 0.05, 0.1));
		agent.setNoopAction(noop);
		
		
		//set priors
		for(int i = 0; i < tasks.size(); i++){
			agent.setProbFor(i, 1./(double)tasks.size());
		}
		
		agent.useSeperatePlanningDomain(domain);
		agent.planPossibleTasksFromSeedState(initialState);
		
		boolean hasInitedGUI = false;
		for(int i = 0; i < 20; i++){
			env.setCurStateTo(this.initialState);
			if(!hasInitedGUI){
				hasInitedGUI = true;
				gui.initGUI();
				gui.launch();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("Starting episode");
			//now start learning episode
			agent.runLearningEpisodeFrom(this.initialState);
		}
		
		System.out.println("finished training");
		
	}
	
	
	public List<TaskDescription> getTaskDescriptions(){
		
		List<TaskDescription> tasks = new ArrayList<TaskDescription>();
		
		int [][]map = gwg.getMap();
		
		for(int x = 0; x < map.length; x++){
			for(int y = 0; y < map[0].length; y++){
				if(map[x][y] != 1){
					GWAtLocTest loc = new GWAtLocTest(x, y);
					RewardFunction rf = new GoalBasedRF(loc);
					TerminalFunction tf = new GoalConditionTF(loc);
					TaskDescription td = new TaskDescription(rf, tf);
					tasks.add(td);
				}
			}
		}
		
		return tasks;
		
	}
	
	public StateToFeatureVectorGenerator getRBFsForEachState(State seedState, Domain domain, double epsilon){
		
		EuclideanDistance metric = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, GridWorldDomain.CLASSAGENT));
		RBFFV fv = new RBFFV(metric);
		
		List<State> allStates = StateReachability.getReachableStates(seedState, (SADomain)domain, new DiscreteStateHashFactory());
		for(State s : allStates){
			fv.addRBF(s, epsilon);
		}
		
		return fv;
	}
	
	public class GWAtLocTest implements StateConditionTest{

		int x,y;
		
		public GWAtLocTest(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public boolean satisfies(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int x = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int y = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			return x == this.x && y == this.y;
		}
		
		
		
	}
	
	
	
	public static class StateIndicatorFV implements StateToFeatureVectorGenerator{

		StateEnumerator enumerator;
		
		public StateIndicatorFV(State seedState, Domain domain){
			this.enumerator = new StateEnumerator(domain, new DiscreteStateHashFactory());
			this.enumerator.findReachableStatesAndEnumerate(seedState);
			for(int i = 0; i < this.enumerator.numStatesEnumerated(); i++){
				//System.out.println(i + ":\n" + this.enumerator.getStateForEnumertionId(i).toString());
			}
		}
		
		@Override
		public double[] generateFeatureVectorFrom(State s) {
			
			double [] fv = new double[this.enumerator.numStatesEnumerated()];
			fv[this.enumerator.getEnumeratedID(s)] = 1.;
			
			return fv;
		}
		
		
	}
	
	public static class InitialAndGoalFV implements StateToFeatureVectorGenerator{

		@Override
		public double[] generateFeatureVectorFrom(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			double [] fv = new double[2];
			
			if(ax == 4 && ay == 5){
				fv[0] = 1.;
			}
			if(ax == 5 && ay == 5){
				fv[1] = 1.;
			}
			
			return fv;
		}
		
	}
	
}
