package tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.blocksworld.BlocksWorld;
import burlap.domain.singleagent.blocksworld.BlocksWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.UniversalStateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;

public class RelationalPlanningTest {

	BlocksWorld							bwd;
	Domain								domain;
	StateParser 						sp;
	RewardFunction 						rf;
	TerminalFunction					tf;
	StateConditionTest					goalCondition;
	State 								initialState;
	NameDependentStateHashFactory		hashingFactory;
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RelationalPlanningTest rpt = new RelationalPlanningTest();
		String outputPath = "output"; //directory to record results
		
		//rpt.BFSExample(outputPath);
		//rpt.ValueIterationExample(outputPath);
		//rpt.QLearningExample(outputPath);
		//rpt.visualize(outputPath);
		rpt.efficiencyTest();

	}
	
	
	
	public RelationalPlanningTest(){
		bwd = new BlocksWorld();
		domain = bwd.generateDomain();
		sp = new UniversalStateParser(domain);
		rf = new UniformCostRF();
		tf = new StackTerminal();
		goalCondition = new TFGoalCondition(tf);
		
		initialState = BlocksWorld.getNewState(domain, 6);
		
		hashingFactory = new NameDependentStateHashFactory();
		
		
	}
	
	public void visualize(String outputPath){
		Visualizer v = BlocksWorldVisualizer.getVisualizer();
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	
	public void BFSExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//BFS ignores reward; it just searches for a goal condition satisfying state
		DeterministicPlanner planner = new BFS(domain, goalCondition, hashingFactory); 
		planner.planFromState(initialState);
		
		//capture the computed plan in a partial policy
		Policy p = new SDPlannerPolicy(planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	public void ValueIterationExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		
		//Value iteration computing for discount=0.99 with stopping criteria either being a maximum change in value less then 0.001 or 100 passes over the state space (which ever comes first)
		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.001, 100);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	public void QLearningExample(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		//creating the learning algorithm object; discount= 0.99; initialQ=0.0; learning rate=0.9
		LearningAgent agent = new QLearning(domain, rf, tf, 0.99, hashingFactory, 0., 0.9);
		
		//run learning for 100 episodes
		for(int i = 0; i < 100; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState); //run learning episode
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp); //record episode to a file
			System.out.println(i + ": " + ea.numTimeSteps()); //print the performance of this episode
		}
		
	}
	
	
	public class StackTerminal implements TerminalFunction{

		List<GroundedProp> gps;
		
		public StackTerminal(){
			gps = new ArrayList<GroundedProp>();
			gps.add(new GroundedProp(domain.getPropFunction(BlocksWorld.PFONBLOCK), new String[]{"block0", "block1"}));
			gps.add(new GroundedProp(domain.getPropFunction(BlocksWorld.PFONBLOCK), new String[]{"block2", "block0"}));
		}
		
		@Override
		public boolean isTerminal(State s) {
			for(GroundedProp gp : gps){
				if(!gp.isTrue(s)){
					return false;
				}
			}
			return true;
		}

		
	}
	
	
	public void efficiencyTest(){
		
		Set <StateHashTuple> reachability;
		MyTimer timer = new MyTimer();
		
		System.out.println("Starting");
		
		timer.start();
		reachability = StateReachability.getReachableHashedStates(initialState, (SADomain)domain, hashingFactory);
		timer.stop();
		
		System.out.println("Time: " +  timer.getTime() + "; n states: " + reachability.size());
		
		
	}
	
	
	public void efficiencyTest1(){
		
		int n = 4000;
		MyTimer timer = new MyTimer();
		
		
		List<StateHashTuple> shList = new ArrayList<StateHashTuple>(n);
		timer.start();
		for(int i = 0; i < n; i++){
			State s = initialState.copy();
			StateHashTuple sh = this.hashingFactory.hashState(s);
			sh.hashCode();
			shList.add(sh);
		}
		
		timer.stop();
		System.out.println("List time: " + timer.getTime());
		
		Set<StateHashTuple> shSet = new HashSet<StateHashTuple>(n);
		timer.start();
		for(int i = 0; i < n; i++){
			State s = initialState.copy();
			StateHashTuple sh = this.hashingFactory.hashState(s);
			shSet.add(sh);
		}
		
		timer.stop();
		System.out.println("Set time: " + timer.getTime());
		
	}
	

}
