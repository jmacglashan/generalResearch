package behavior.training.experiments.simulated.grid;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.training.DynamicFeedbackGUI;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.NoopOnTermPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class SimulatedHumanGrid {

	protected GridWorldDomain					gwg;
	protected Domain							domain;
	protected State								initialState;
	protected DiscreteMaskHashingFactory		hashingFactory;
	protected Visualizer						visualizer;
	protected int								maxGoalLocation;
	protected int[][]							specialMap;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		SimulatedHumanGrid shg = new SimulatedHumanGrid();
		shg.runVisualExplorer();
		//shg.runInteractiveTraining();
		
		//shg.experimentFixedCustomDriver("dataFiles/trainOutput/custom2/actual", 0.3, 0.7, 0.05);
		
		/*
		if(args.length != 6){
			System.out.println("Format:\n\toutputDir mu+ mu- epsilon stratAssumption stratToTrack\nstratAssumption 0: infer over all\nstratAssumption 1: r+p+\nstratAssumption 2: r+p-\nstratAssumption 3: r-p+");
			System.exit(0);
		}
		shg.experimentDriver(args[0], Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
		*/

	}
	
	
	public SimulatedHumanGrid(){
		
		this.gwg = new GridWorldDomain(15, 15);
		this.gwg.setNumberOfLocationTypes(1);
		
		
		
		this.domain = gwg.generateDomain();
		
		
		this.initialState = GridWorldDomain.getOneAgentNLocationState(this.domain, 0);
		GridWorldDomain.setAgent(this.initialState, 0, 0);
		
		IntMap smap = new IntMap(15, 15);
		smap.set(12, 0, 1000);
		smap.horizontal(0, 5, 2, 1000);
		smap.set(12, 2, 1000);
		smap.set(12, 3, 1000);
		smap.vertical(3, 6, 5, 1001);
		smap.horizontal(9, 14, 4, 1001);
		smap.horizontal(0, 1, 5, 1001);
		smap.horizontal(3, 4, 5, 1000);
		smap.vertical(5, 9, 9, 1000);
		smap.set(12, 5, 1000);
		smap.horizontal(10, 12, 7, 1001);
		smap.vertical(8, 10, 5, 1000);
		smap.horizontal(0, 1, 11, 1000);
		smap.horizontal(3, 9, 11, 1001);
		smap.horizontal(12, 14, 11, 1001);
		smap.set(7, 12, 1000);
		smap.vertical(12, 13, 12, 1001);
		smap.set(7, 14, 1000);
		smap.horizontal(7, 8, 4, 1000);
		
		int GCounter = 1;
		for(int i = 0; i < 15; i+=2){
			int offset = 1;
			if((i / 2) % 2 == 1){
				offset = 0;
			}
			for(int j = 0; j < 15; j++){
				if(j % 2 == offset){
					if(smap.map[j][i] == 0){
						smap.set(j, i, GCounter);
						GCounter++;
					}
				}
			}
		}
		
		maxGoalLocation = GCounter-1;
		this.specialMap = smap.map;
		System.out.println("Num goals: " + this.maxGoalLocation);
		
		StaticMultiValMapPainter staticpainter = new StaticMultiValMapPainter(smap.map);
		staticpainter.setSpecial(1000, Color.black);
		staticpainter.setSpecial(1001, Color.black.brighter().brighter().brighter().brighter().brighter().brighter().brighter());
		
		
		
		this.hashingFactory = new DiscreteMaskHashingFactory();
		this.hashingFactory.addAttributeForClass(GridWorldDomain.CLASSAGENT, this.domain.getAttribute(GridWorldDomain.ATTX));
		this.hashingFactory.addAttributeForClass(GridWorldDomain.CLASSAGENT, this.domain.getAttribute(GridWorldDomain.ATTY));
		
		this.visualizer = GridWorldVisualizer.getVisualizer(this.domain, this.gwg.getMap());
		this.visualizer.addStaticPainter(staticpainter);
		
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
		//TaskInductionTraining agent = new TaskInductionTraining(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, tasks, new MAPMixtureModelPolicy());
		TaskInductionWithFeedbackStrategies agent = new TaskInductionWithFeedbackStrategies(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, tasks, new MAPMixtureModelPolicy());
		agent.addFeedbackStrategy(new FeedbackStrategy(0.5, 0.5, 0.1));
		//agent.addFeedbackStrategy(new FeedbackStrategy(0.05, 0.9, 0.1));
		//agent.addFeedbackStrategy(new FeedbackStrategy(0.9, 0.05, 0.1));
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
	
	public void experimentDriver(String dir, double muPlus, double muMinus, double epsilon, int stratAssumption, int stratToTrack){
		
		FeedbackStrategy rPlusPPlus = new FeedbackStrategy(0.1, 0.1, 0.05);
		FeedbackStrategy rPlusPMinus = new FeedbackStrategy(0.1, 0.9, 0.05);
		FeedbackStrategy rMinusPPlus = new FeedbackStrategy(0.9, 0.1, 0.05);
		
		FeedbackStrategy trainerStrategy = new FeedbackStrategy(muPlus, muMinus, epsilon);
		
		if(!dir.endsWith("/")){
			dir = dir + "/";
		}
		
		TaskInductionWithFeedbackStrategies agent = null;
		
		if(stratAssumption == 0){
			agent = getSimulatedTraining(trainerStrategy, stratToTrack, rPlusPPlus, rPlusPMinus, rMinusPPlus);
		}
		else if(stratAssumption == 1){
			agent = getSimulatedTraining(trainerStrategy, 0, rPlusPPlus);
		}
		else if(stratAssumption == 2){
			agent = getSimulatedTraining(trainerStrategy, 0, rPlusPMinus);
		}
		else if(stratAssumption == 3){
			agent = getSimulatedTraining(trainerStrategy, 0, rMinusPPlus);
		}
		
		for(int i = 0; i < 50; i++){
			String path = dir + i + ".txt";
			this.runSimulatedTrainingUsingAgent(path, trainerStrategy, agent);
		}
		
	}
	
	public void experimentFixedCustomDriver(String dir, double muPlus, double muMinus, double epsilon){
		
		FeedbackStrategy trainerStrategy = new FeedbackStrategy(muPlus, muMinus, epsilon);
		
		if(!dir.endsWith("/")){
			dir = dir + "/";
		}
		
		TaskInductionWithFeedbackStrategies agent = getSimulatedTraining(trainerStrategy, 0, trainerStrategy);;
		
		for(int i = 0; i < 50; i++){
			String path = dir + i + ".txt";
			this.runSimulatedTrainingUsingAgent(path, trainerStrategy, agent);
		}
		
	}
	
	
	
	public TaskInductionWithFeedbackStrategies getSimulatedTraining(FeedbackStrategy trainerStrat, int stratToTrack, FeedbackStrategy...strategiesAware){
	
		Action noop = new NullAction("noop", domain, ""); //add noop, which is not attached to anything.
		List<TaskDescription> tasks = this.getTaskDescriptions();
		
		TaskInductionWithFeedbackStrategies agent = new TaskInductionWithFeedbackStrategies(domain, null, null, hashingFactory, tasks, new MAPMixtureModelPolicy());
		for(FeedbackStrategy fs : strategiesAware){
			agent.addFeedbackStrategy(fs);
		}
		agent.setNoopAction(noop);
		//set priors
		for(int i = 0; i < tasks.size(); i++){
			agent.setProbFor(i, 1./(double)tasks.size());
		}
		agent.useSeperatePlanningDomain(domain);
		agent.planPossibleTasksFromSeedState(initialState);
		agent.setStrategyToTrack(stratToTrack);
		
		return agent;
		
	}
	
	public void runSimulatedTrainingUsingAgent(String outputPath, FeedbackStrategy trainerStrat, TaskInductionWithFeedbackStrategies agent){
		
		List<TaskDescription> tasks = agent.getTasks();
		Random rand = RandomFactory.getMapped(20);
		int sel = rand.nextInt(tasks.size());
		TaskDescription selectedTask = tasks.get(sel);
		
		RewardFunction trainerRF = new FeedbackStrategyRF(trainerStrat, selectedTask, this.domain.getAction("noop"));
		TerminalFunction trainerTF = selectedTask.tf;
		
		agent.setRf(trainerRF);
		agent.setTf(trainerTF);
		
		agent.setTaskToTrack(sel);
		
		agent.setOutputPath(outputPath);
		
		StateGenerator sg = new RandomStateGenerator();
		
		
		int maxSteps = 300;
		while(maxSteps > 0){
			State s = sg.generateState();
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(s, Math.min(maxSteps, 100));
			maxSteps -= ea.numTimeSteps()-1;
		}
		
		/*
		for(int i = 0; i < 10; i++){
			State s = sg.generateState();
			agent.runLearningEpisodeFrom(s, 100);
			
		}
		*/
		
		agent.closeOutput();
		
		agent.resetStepCount();
		agent.setToUniform();
		
	}
	
	public void runSimulatedTraining(String outputPath, FeedbackStrategy trainerStrat, int stratToTrack, FeedbackStrategy...strategiesAware){
		
		Action noop = new NullAction("noop", domain, ""); //add noop, which is not attached to anything.
		List<TaskDescription> tasks = this.getTaskDescriptions();
		
		Random rand = RandomFactory.getMapped(20);
		int sel = rand.nextInt(tasks.size());
		TaskDescription selectedTask = tasks.get(sel);
		
		/*
		List<FeedbackStrategy> fss = new ArrayList<FeedbackStrategy>();
		fss.add(new FeedbackStrategy(0.5, 0.5, 0.1));
		fss.add(new FeedbackStrategy(0.05, 0.9, 0.1));
		fss.add(new FeedbackStrategy(0.9, 0.05, 0.1));
		*/
		
		RewardFunction trainerRF = new FeedbackStrategyRF(trainerStrat, selectedTask, noop);
		TerminalFunction trainerTF = selectedTask.tf;
	
		TaskInductionWithFeedbackStrategies agent = new TaskInductionWithFeedbackStrategies(domain, trainerRF, trainerTF, hashingFactory, tasks, new MAPMixtureModelPolicy());
		agent.setOutputPath(outputPath);
		for(FeedbackStrategy fs : strategiesAware){
			agent.addFeedbackStrategy(fs);
		}
		agent.setNoopAction(noop);
		//set priors
		for(int i = 0; i < tasks.size(); i++){
			agent.setProbFor(i, 1./(double)tasks.size());
		}
		agent.useSeperatePlanningDomain(domain);
		agent.planPossibleTasksFromSeedState(initialState);
		agent.setTaskToTrack(sel);
		agent.setStrategyToTrack(stratToTrack);
		
		StateGenerator sg = new RandomStateGenerator();
		
		for(int i = 0; i < 10; i++){
			State s = sg.generateState();
			agent.runLearningEpisodeFrom(s, 100);
			
		}
		agent.closeOutput();
		
		
	}
	
	
	public void runVisualExplorer(){
		
		VisualExplorer exp = new VisualExplorer(this.domain, this.visualizer, this.initialState);
		
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		
		exp.initGUI();
		
	}
	
	
	public List<TaskDescription> getTaskDescriptions(){
		List<StateConditionTest> goalChecks = this.getGoalChecks();
		List<StateConditionTest> avoidChecks = this.avoidChecks();
		
		List<TaskDescription> res = new ArrayList<TaskDescription>(goalChecks.size()*avoidChecks.size());
		for(StateConditionTest gc : goalChecks){
			for(StateConditionTest ac : avoidChecks){
				GoalLocWithAvoidRF rf = new GoalLocWithAvoidRF(gc, ac);
				TerminalFunction tf = new TFOnSC(gc);
				TaskDescription td = new TaskDescription(rf, tf);
				res.add(td);
			}
		}
		
		shuffle(res);
		
		return res;
	}
	
	
	public List<TaskDescription> getTaskDescriptionsOld(){
		
		List<GWLocationTypeSC> goalChecks = this.getGoalLocationChecksOld();
		List<GWLocationTypesSC> avoidChecks = this.avoidLocationChecksOld();
		
		List<TaskDescription> res = new ArrayList<TaskDescription>(goalChecks.size()*avoidChecks.size());
		
		for(GWLocationTypeSC gc : goalChecks){
			for(GWLocationTypesSC ac : avoidChecks){
				GoalLocWithAvoidRF rf = new GoalLocWithAvoidRF(gc, ac);
				TerminalFunction tf = new TFOnSC(gc);
				TaskDescription td = new TaskDescription(rf, tf);
				res.add(td);
			}
		}
		
		
		return res;
	}
	
	
	public List<StateConditionTest> getGoalChecks(){
		List<StateConditionTest> res = new ArrayList<StateConditionTest>(this.maxGoalLocation);
		for(int i = 1; i <= this.maxGoalLocation; i++){
			res.add(new GWSpecialLocTypeSC(this.specialMap, i));
		}
		return res;
	}
	
	public List<GWLocationTypeSC> getGoalLocationChecksOld(){
		int num = 8;
		List<GWLocationTypeSC> res = new ArrayList<SimulatedHumanGrid.GWLocationTypeSC>();
		for(int i = 0; i < num; i++){
			GWLocationTypeSC loc = new GWLocationTypeSC(this.domain, i);
			res.add(loc);
		}
		
		return res;
	}
	
	
	
	public List<StateConditionTest> avoidChecks(){
		
		List<StateConditionTest> res = new ArrayList<StateConditionTest>();
		res.add(new GWSpecialLocTypeSC(specialMap));
		res.add(new GWSpecialLocTypeSC(specialMap, 1000));
		res.add(new GWSpecialLocTypeSC(specialMap, 1001));
		res.add(new GWSpecialLocTypeSC(specialMap, 1001, 1000));
		
		return res;
		
	}
	
	public List<GWLocationTypesSC> avoidLocationChecksOld(){
		List<GWLocationTypesSC> res = new ArrayList<SimulatedHumanGrid.GWLocationTypesSC>();
		
		boolean type8 = false;
		for(int i = 0; i < 2; i++){
			boolean type9 = false;
			for(int j = 0; j < 2; j++){
				boolean type10 = false;
				for(int k = 0; k < 2; k++){
					GWLocationTypesSC check = new GWLocationTypesSC(this.domain);
					if(type8){
						check.addType(8);
					}
					if(type9){
						check.addType(9);
					}
					if(type10){
						check.addType(10);
					}
					
					res.add(check);
					
					type10 = true;
				}
				type9 = true;
			}
			type8 = true;
		}
		return res;
	}
	
	
	
	
	
	protected void oldInitialState(){
		
		this.gwg.setNumberOfLocationTypes(11);
		
		//set map
		this.gwg.setObstacleInCell(0, 5);
		this.gwg.horizontalWall(2, 3, 5);
		this.gwg.verticalWall(0, 1, 3);
		this.gwg.verticalWall(5, 8, 3);
		this.gwg.horizontalWall(3, 7, 8);
		this.gwg.verticalWall(0, 6, 6);
		this.gwg.setObstacleInCell(7, 6);
		this.gwg.verticalWall(12, 14, 7);
		this.gwg.verticalWall(0, 1, 11);
		this.gwg.horizontalWall(12, 14, 8);
		
		this.initialState = GridWorldDomain.getOneAgentNLocationState(this.domain, 34);
		GridWorldDomain.setAgent(this.initialState, 0, 0);
		
		//goal locations are first 8
		GridWorldDomain.setLocation(this.initialState, 0, 0, 14, 0);
		GridWorldDomain.setLocation(this.initialState, 1, 3, 13, 1);
		GridWorldDomain.setLocation(this.initialState, 2, 7, 11, 2);
		GridWorldDomain.setLocation(this.initialState, 3, 10, 14, 3);
		GridWorldDomain.setLocation(this.initialState, 4, 14, 14, 4);
		GridWorldDomain.setLocation(this.initialState, 5, 12, 4, 5);
		GridWorldDomain.setLocation(this.initialState, 6, 14, 0, 6);
		GridWorldDomain.setLocation(this.initialState, 7, 9, 0, 7);
		
		//now do all the potential avoidables
		GridWorldDomain.setLocation(this.initialState, 8, 2, 2, 9);
		GridWorldDomain.setLocation(this.initialState, 9, 8, 2, 10);
		GridWorldDomain.setLocation(this.initialState, 10, 9, 2, 11);
		GridWorldDomain.setLocation(this.initialState, 11, 12, 2, 8);
		
		GridWorldDomain.setLocation(this.initialState, 12, 11, 4, 9);
		
		GridWorldDomain.setLocation(this.initialState, 13, 1, 5, 8);
		GridWorldDomain.setLocation(this.initialState, 14, 9, 5, 10);
		GridWorldDomain.setLocation(this.initialState, 15, 10, 5, 9);
		
		GridWorldDomain.setLocation(this.initialState, 16, 5, 6, 10);
		
		GridWorldDomain.setLocation(this.initialState, 17, 10, 8, 8);
		GridWorldDomain.setLocation(this.initialState, 18, 11, 8, 9);
		
		GridWorldDomain.setLocation(this.initialState, 19, 6, 10, 8);
		GridWorldDomain.setLocation(this.initialState, 20, 7, 10, 9);
		GridWorldDomain.setLocation(this.initialState, 21, 8, 10, 10);
		
		GridWorldDomain.setLocation(this.initialState, 22, 5, 11, 8);
		
		GridWorldDomain.setLocation(this.initialState, 23, 3, 12, 9);
		GridWorldDomain.setLocation(this.initialState, 24, 5, 12, 10);
		
		GridWorldDomain.setLocation(this.initialState, 25, 0, 13, 9);
		GridWorldDomain.setLocation(this.initialState, 26, 1, 13, 8);
		GridWorldDomain.setLocation(this.initialState, 27, 5, 13, 8);
		GridWorldDomain.setLocation(this.initialState, 28, 9, 13, 10);
		GridWorldDomain.setLocation(this.initialState, 29, 10, 13, 10);
		GridWorldDomain.setLocation(this.initialState, 30, 11, 13, 9);
		GridWorldDomain.setLocation(this.initialState, 31, 13, 13, 8);
		GridWorldDomain.setLocation(this.initialState, 32, 14, 13, 8);
		
		GridWorldDomain.setLocation(this.initialState, 33, 9, 14, 9);
	}
	
	
	
	
	public static void shuffle(List<TaskDescription> objects){
		Random rand = RandomFactory.getMapped(0);
		for(int i = 0; i < objects.size(); i++){
			int npos = rand.nextInt(objects.size());
			TaskDescription old = objects.get(npos);
			objects.set(npos, objects.get(i));
			objects.set(i, old);
		}
	}
	
	
	public static class GoalLocWithAvoidRF implements RewardFunction{

		StateConditionTest goalLocTest;
		StateConditionTest toAvoid;
		
		protected double goalLoc = 1.;
		protected double avoidLoc = -100;
		
		
		public GoalLocWithAvoidRF(StateConditionTest goalLocTest, StateConditionTest toAvoid){
			this.goalLocTest = goalLocTest;
			this.toAvoid = toAvoid;
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
		
			if(this.toAvoid.satisfies(sprime)){
				return this.avoidLoc;
			}
			else if(this.goalLocTest.satisfies(sprime)){
				return this.goalLoc;
			}
			
			return 0;
		}
		
		
		
	}
	
	public static class TFOnSC implements TerminalFunction{

		StateConditionTest sc;
		
		public TFOnSC(StateConditionTest sc){
			this.sc = sc;
		}
		
		@Override
		public boolean isTerminal(State s) {
			return this.sc.satisfies(s);
		}
		
		
		
	}
	
	
	
	public static class GWSpecialLocTypeSC implements StateConditionTest{
		
		int [][] map;
		Set<Integer> types;
		
		public GWSpecialLocTypeSC(int [][] map, int...types){
			this.map = map;
			this.types = new HashSet<Integer>();
			for(int t : types){
				this.types.add(t);
			}
		}
		
		@Override
		public boolean satisfies(State s) {
			
			ObjectInstance a = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = a.getDiscValForAttribute(GridWorldDomain.ATTX);
			int ay = a.getDiscValForAttribute(GridWorldDomain.ATTY);
			
			if(this.types.contains(this.map[ax][ay])){
				return true;
			}
			
			return false;
		}
		
		
		
	}
	
	
	
	
	public class FeedbackStrategyRF implements RewardFunction{

		Policy p;
		FeedbackStrategy fs;
		
		public FeedbackStrategyRF(FeedbackStrategy fs, TaskDescription td, Action noopAction){
			System.out.println("Planning for simulated trainer");
			ValueIteration planner = new ValueIteration(domain, td.rf, td.tf, 0.99, hashingFactory, 0.001, 100);
			planner.planFromState(initialState);
			System.out.println("Finished planning for simulated trainer");
			this.p = new NoopOnTermPolicy(noopAction, td.tf, new BoltzmannQPolicy(planner, 0.002));
			this.fs = fs;
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			return this.fs.feedbackSample(s, a, this.p);
		}
		
		
		
	}
	
	
	public class RandomStateGenerator implements StateGenerator{
		
		@Override
		public State generateState() {
			
			Random rand = RandomFactory.getMapped(20);
			
			int x = rand.nextInt(specialMap.length);
			int y = rand.nextInt(specialMap[0].length);
			
			while(specialMap[x][y] != 0){
				x = rand.nextInt(specialMap.length);
				y = rand.nextInt(specialMap[0].length);
			}
			
			State s = GridWorldDomain.getOneAgentNLocationState(domain, 0);
			GridWorldDomain.setAgent(s, x, y);
			
			return s;
		}
		
		
		
	}
	
	public static class GWLocationTypeSC implements StateConditionTest{

		protected int locationType;
		protected PropositionalFunction atLocationPF;
		
		public GWLocationTypeSC(Domain domain, int locationType){
			this.locationType = locationType;
			this.atLocationPF = domain.getPropFunction(GridWorldDomain.PFATLOCATION);
		}
		
		@Override
		public boolean satisfies(State s) {
			
			List<ObjectInstance> locations = s.getObjectsOfTrueClass(GridWorldDomain.CLASSLOCATION);
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			for(ObjectInstance loc : locations){
				if(loc.getDiscValForAttribute(GridWorldDomain.ATTLOCTYPE) == this.locationType){
					if(this.atLocationPF.isTrue(s, new String[]{agent.getName(), loc.getName()})){
						return true;
					}
				}
			}
			
			return false;
		}
		
	}
	
	public static class GWLocationTypesSC implements StateConditionTest{

		protected Set<Integer> types;
		protected PropositionalFunction atLocationPF;
		
		public GWLocationTypesSC(Domain domain){
			this.types = new HashSet<Integer>();
			this.atLocationPF = domain.getPropFunction(GridWorldDomain.PFATLOCATION);
		}
		
		public void addType(int type){
			this.types.add(type);
		}
		
		@Override
		public boolean satisfies(State s) {
			
			if(this.types.size() == 0){
				return false;
			}
			
			List<ObjectInstance> locations = s.getObjectsOfTrueClass(GridWorldDomain.CLASSLOCATION);
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			for(ObjectInstance loc : locations){
				if(this.types.contains(loc.getDiscValForAttribute(GridWorldDomain.ATTLOCTYPE))){
					if(this.atLocationPF.isTrue(s, new String[]{agent.getName(), loc.getName()})){
						return true;
					}
				}
			}
			
			return false;
		}
		
	}

}
