package tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import behavior.vfa.heterogenousafd.HeterogenousActionFeatureDatabase;
import behavior.vfa.heterogenousafd.stateenumerators.EnumeratedFeatureDatabase;
import behavior.vfa.heterogenousafd.stateenumerators.HashingFactoryEnumerator;
import behavior.vfa.heterogenousafd.stateenumerators.StateConditionEnumerator;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.options.DeterminisitcTerminationOption;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.StateConditionTestIterable;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SingleGoalPFRF;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.visualizer.Visualizer;

public class OptionStateGeneralizationTest {

	
	GridWorldDomain 							gwdg;
	Domain										domain;
	StateParser 								sp;
	RewardFunction 								rf;
	TerminalFunction							tf;
	StateConditionTest							goalCondition;
	State 										initialState;
	DiscreteStateHashFactory					hashingFactory;
	EnumeratedFeatureDatabase 					hashEnumDB;
	HeterogenousActionFeatureDatabase			featureDatabase;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int domainType = 0;
		OptionStateGeneralizationTest example = new OptionStateGeneralizationTest(domainType);
		
		String outputPath = "output"; //directory to record results
		
		//example.runSarsa(outputPath);
		example.runGDSarsaLam(outputPath, 1, domainType);
		example.visualize(outputPath);

	}
	
	
	public OptionStateGeneralizationTest(int domainType) {
		
		int goalLocationX = 10;
		int goalLocationY = 10;
		if(domainType == 0){
			gwdg = new GridWorldDomain(11, 11);
			gwdg.setMapToFourRooms(); //will use the standard four rooms layout
		}
		else if(domainType == 1){
			gwdg = new GridWorldDomain(100, 100);
			gwdg.horizontalWall(0, 9, 50); //door at 10,50
			gwdg.horizontalWall(11, 55, 50);
			gwdg.horizontalWall(55, 70, 40); //door at 71,40
			gwdg.horizontalWall(72, 99, 40);
			
			gwdg.verticalWall(0, 9, 55); //door at 55,10
			gwdg.verticalWall(11, 70, 55); //door at 55,71
			gwdg.verticalWall(72, 99, 55);
			
			goalLocationX = 95;
			goalLocationY = 95;
			
		}
		
		
		
		domain = gwdg.generateDomain();
		sp = new GridWorldStateParser(domain); //for writing states to a file
		
		rf = new SingleGoalPFRF(domain.getPropFunction(GridWorldDomain.PFATLOCATION), 1.0, -1.0);
		tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION)); //ends when the agent reaches a location
		goalCondition = new TFGoalCondition(tf); //create a goal condition that is synonymous with the termination criteria; this is used with deterministic planners
		
		//set up the initial state
		initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
		GridWorldDomain.setAgent(initialState, 0, 0);
		GridWorldDomain.setLocation(initialState, 0, goalLocationX, goalLocationY);
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList); //optional code line; uses only the agent position to perform hash calculations instead of the agent and all locations
		
		featureDatabase = new HeterogenousActionFeatureDatabase();
		HashingFactoryEnumerator hashEnum = new HashingFactoryEnumerator(hashingFactory);
		hashEnumDB = new EnumeratedFeatureDatabase(hashEnum);
		
		for(Action a : domain.getActions()){
			featureDatabase.addFDForAction(a.getName(), hashEnumDB);
		}
		
		
	}
	
	
	public void visualize(String outputPath){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	
	
	public void runGDSarsaLam(String outputPath, int addOptions, int domainType){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		GradientDescentSarsaLam agent = new GradientDescentSarsaLam(domain, rf, tf, 0.99, featureDatabase.getLinearVFA(0.0), 0.5, 100000, 0.8);
		//agent.setUseReplaceTraces(true);
		
		if(addOptions > 0){
			
			List <DeterminisitcTerminationOption> sops = null;
			if(domainType == 0){
				sops = this.getStandard4RoomsOptions();
			}
			else if(domainType == 1){
				sops = this.getLarge4RoomsOptions();
			}
			
			if(addOptions == 1){
				this.addRoomsOptionsToPlannerWithAbstractOptionFeatures(agent, sops);
			}
			else if(addOptions == 2){
				this.addRoomsOptionsToPlannerWithFullState(agent, sops);
			}
			
			
		}
		
		int nEpisodes = 100;
		if(domainType == 1){
			nEpisodes = 1000;
		}
		
		System.out.println("Starting");
		
		//run learning for 100 episodes
		for(int i = 0; i < nEpisodes; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			//System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
			System.out.println(ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	public void runSarsa(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		LearningAgent agent = new SarsaLam(domain, rf, tf, 0.99, hashingFactory, 0.0, 0.5, 0.9);
		
		//run learning for 100 episodes
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			//System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
			System.out.println(ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	
	public void addRoomsOptionsToPlannerWithAbstractOptionFeatures(OOMDPPlanner planner, List<DeterminisitcTerminationOption> sops){
		
		
		for(DeterminisitcTerminationOption sop : sops){
			this.addSubgoalIniitationToActionFDAndPlanner(planner, sop);
		}
		
		
	}
	
	public void addRoomsOptionsToPlannerWithFullState(OOMDPPlanner planner, List<DeterminisitcTerminationOption> sops){
		
		for(DeterminisitcTerminationOption sop : sops){
			this.addSubgoalFullStateToActionFDAndPlanner(planner, sop);
		}
		
	}
	
	
	public List<DeterminisitcTerminationOption> getStandard4RoomsOptions(){
		
		List <DeterminisitcTerminationOption> sops = new ArrayList<DeterminisitcTerminationOption>(8);
		
		DeterminisitcTerminationOption blt = this.getRoomOption("blt", 0, 4, 0, 4, 1, 5);
		DeterminisitcTerminationOption blr = this.getRoomOption("blr", 0, 4, 0, 4, 5, 1);
		sops.add(blt);
		sops.add(blr);
		
		
		DeterminisitcTerminationOption tlr = this.getRoomOption("tlr", 0, 4, 6, 10, 5, 8);		
		DeterminisitcTerminationOption tlb = this.getRoomOption("tlb", 0, 4, 6, 10, 1, 5);
		sops.add(tlr);
		sops.add(tlb);
		
		
		DeterminisitcTerminationOption trb = this.getRoomOption("trb", 6, 10, 5, 10, 8, 4);
		DeterminisitcTerminationOption trl = this.getRoomOption("trl", 6, 10, 5, 10, 5, 8);
		sops.add(trb);
		sops.add(trl);
		
		
		
		DeterminisitcTerminationOption brt = this.getRoomOption("brt", 6, 10, 0, 3, 8, 4);		
		DeterminisitcTerminationOption brl = this.getRoomOption("brl", 6, 10, 6, 3, 5, 1);
		sops.add(brt);
		sops.add(brl);

		
		return sops;
	}
	
	public List<DeterminisitcTerminationOption> getLarge4RoomsOptions(){
		
		List <DeterminisitcTerminationOption> sops = new ArrayList<DeterminisitcTerminationOption>(8);
		
		DeterminisitcTerminationOption blt = this.getRoomOption("blt", 0, 54, 0, 49, 10, 50);
		DeterminisitcTerminationOption blr = this.getRoomOption("blr", 0, 54, 0, 49, 55, 10);
		sops.add(blt);
		sops.add(blr);
		
		
		DeterminisitcTerminationOption tlr = this.getRoomOption("tlr", 0, 54, 51, 99, 55, 71);		
		DeterminisitcTerminationOption tlb = this.getRoomOption("tlb", 0, 54, 51, 99, 10, 50);
		sops.add(tlr);
		sops.add(tlb);
		
		
		DeterminisitcTerminationOption trb = this.getRoomOption("trb", 56, 99, 51, 99, 71, 40);
		DeterminisitcTerminationOption trl = this.getRoomOption("trl", 56, 99, 51, 99, 55, 71);
		//sops.add(trb);
		//sops.add(trl);
		
		
		
		DeterminisitcTerminationOption brt = this.getRoomOption("brt", 56, 99, 0, 3, 71, 40);		
		DeterminisitcTerminationOption brl = this.getRoomOption("brl", 56, 99, 0, 3, 55, 10);
		sops.add(brt);
		sops.add(brl);

		
		return sops;
	}
	
	
	public DeterminisitcTerminationOption getRoomOption(String name, int leftBound, int rightBound, int bottomBound, int topBound, int hx, int hy){
		
		StateConditionTestIterable inRoom = new InRoomStateCheck(leftBound, rightBound, bottomBound, topBound);
		StateConditionTest atHallway = new AtPositionStateCheck(hx, hy);
		
		DiscreteMaskHashingFactory hashingFactory = new DiscreteMaskHashingFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);
		
		//OOMDPPlanner planner = new BFS(domain, atHallway, hashingFactory);
		//PlannerDerivedPolicy p = new SDPlannerPolicy();
		//OOMDPPlanner planner = new ValueIteration(domain, new LocalSubgoalRF(inRoom, atHallway, 0., 0., 1.), new LocalSubgoalTF(inRoom, atHallway), 0.99, hashingFactory, 0.001, 50);
		//OOMDPPlanner planner = new ValueIteration(domain, new LocalSubgoalRF(inRoom, atHallway), new LocalSubgoalTF(inRoom, atHallway), 0.99, hashingFactory, 0.001, 200);
		//PlannerDerivedPolicy p = new GreedyDeterministicQPolicy();
	
		//return new DeterminisitcTerminationOption(name, inRoom, atHallway, planner, p);
		
		return new DeterminisitcTerminationOption(name, new HardCodedLocationOptionPolicy(hx, hy), inRoom, atHallway);
		
	}
	
	protected void addSubgoalIniitationToActionFDAndPlanner(OOMDPPlanner planner, DeterminisitcTerminationOption sop){
		StateConditionEnumerator sce = new StateConditionEnumerator();
		sce.addStateCondition(sop.getInitiationTest());
		featureDatabase.addFDForAction(sop.getName(), new EnumeratedFeatureDatabase(sce));
		planner.addNonDomainReferencedAction(sop);
	}
	
	protected void addSubgoalFullStateToActionFDAndPlanner(OOMDPPlanner planner, DeterminisitcTerminationOption sop){
		featureDatabase.addFDForAction(sop.getName(), hashEnumDB);
		planner.addNonDomainReferencedAction(sop);
	}
	
	
	class InRoomStateCheck implements StateConditionTestIterable{

		int		leftBound;
		int		rightBound;
		int		bottomBound;
		int 	topBound;
		
		
		public InRoomStateCheck(int leftBound, int rightBound, int bottomBound, int topBound){
			this.leftBound = leftBound;
			this.rightBound = rightBound;
			this.bottomBound = bottomBound;
			this.topBound = topBound;
		}
		
		
		@Override
		public boolean satisfies(State s) {
			
			ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0); //get the agent object
			
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			if(ax >= this.leftBound && ax <= this.rightBound && ay >= this.bottomBound && ay <= this.topBound){
				return true;
			}
			
			return false;
		}

		@Override
		public Iterator<State> iterator() {

			return new Iterator<State>() {

				int ax=leftBound;
				int ay=bottomBound;
				
				@Override
				public boolean hasNext() {
					
					if(ay <= topBound){
						return true;
					}
					
					return false;
				}

				@Override
				public State next() {
					
					State s = GridWorldDomain.getOneAgentNLocationState(domain, 0);
					GridWorldDomain.setAgent(s, ax, ay);
					
					ax++;
					if(ax > rightBound){
						ax = leftBound;
						ay++;
					}
					
					return s;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
			
		}

		@Override
		public void setStateContext(State s) {
			//do not need to do anything here
		}
		
		
		
		
	}
	
	
	class AtPositionStateCheck implements StateConditionTest{

		int x;
		int y;
			
		public AtPositionStateCheck(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public boolean satisfies(State s) {

			ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0); //get the agent object
			
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			if(ax == this.x && ay == this.y){
				return true;
			}
			
			return false;
		}
			
	}

	
	class HardCodedLocationOptionPolicy extends Policy{

		int hx;
		int hy;
		int [][] map = gwdg.getMap();
		
		public HardCodedLocationOptionPolicy(int hx, int hy){
			this.hx = hx;
			this.hy = hy;
		}
		
		@Override
		public GroundedAction getAction(State s) {
			
			ObjectInstance agent = s.getObjectsOfClass(GridWorldDomain.CLASSAGENT).get(0);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);
			
			int xd = hx-ax;
			int yd = hy-ay;
			
			//try x
			if(xd > 0){
				int nx = ax+1;
				if(nx < map.length && map[nx][ay] == 0){
					return new GroundedAction(domain.getAction(GridWorldDomain.ACTIONEAST), "");
				}
			}
			if(xd < 0){
				int nx = ax-1;
				if(nx >= 0 && map[nx][ay] == 0){
					return new GroundedAction(domain.getAction(GridWorldDomain.ACTIONWEST), "");
				}
			}
			
			//try y
			if(yd > 0){
				int ny = ay+1;
				if(ny < map[0].length && map[ax][ny] == 0){
					return new GroundedAction(domain.getAction(GridWorldDomain.ACTIONNORTH), "");
				}
			}
			if(yd < 0){
				int ny = ay-1;
				if(ny >= 0 && map[ax][ny] == 0){
					return new GroundedAction(domain.getAction(GridWorldDomain.ACTIONSOUTH), "");
				}
			}
			
			
			
			return null;
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			return this.getDeterministicPolicy(s);
		}

		@Override
		public boolean isStochastic() {
			return false;
		}

		@Override
		public boolean isDefinedFor(State s) {
			return true;
		}
		
		
		
		
	}
	
}
