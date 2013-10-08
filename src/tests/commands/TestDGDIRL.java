package tests.commands;

import java.util.ArrayList;
import java.util.List;

import behavior.irl.DGDIRLFactory;
import behavior.irl.TabularIRL;
import behavior.irl.TabularIRL.TaskCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import commands.data.TrainingElement;
import commands.data.TrainingElementParser;
import commands.data.Trajectory;

import domain.singleagent.sokoban.SokobanDomain;
import domain.singleagent.sokoban.SokobanParser;

public class TestDGDIRL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//create domain
		Domain d = (new SokobanDomain()).generateDomain();
		
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.addAttributeForClass(SokobanDomain.AGENTCLASS, d.getAttribute(SokobanDomain.XATTNAME));
		hashingFactory.addAttributeForClass(SokobanDomain.AGENTCLASS, d.getAttribute(SokobanDomain.YATTNAME));
		hashingFactory.addAttributeForClass(SokobanDomain.BLOCKCLASS, d.getAttribute(SokobanDomain.XATTNAME));
		hashingFactory.addAttributeForClass(SokobanDomain.BLOCKCLASS, d.getAttribute(SokobanDomain.YATTNAME));
		
		
		//create data parsers
		StateParser sp = new SokobanParser();
		TrainingElementParser teparser = new TrainingElementParser(d, sp);
		
		//get dataset
		String path = "commands/sokoDataForEM";
		//String path = "code/turkTrajectories";
		List<TrainingElement> dataset = teparser.getTrainingElementDataset(path, ".txt");
		
		System.out.println("Dataset size: " + dataset.size());
		
		DGDIRLFactory plannerFactory = new DGDIRLFactory(d, 0.99, hashingFactory);
		TabularIRL irl = new TabularIRL(d, plannerFactory, true);
		
		
		for(int i = 0; i < dataset.size(); i++){
			System.out.println(dataset.get(i).command);
			Trajectory t = dataset.get(i).trajectory;
			List <TaskCondition> conds = getPossibleGoalTerminationsFor(t.getState(0));
			double [] probs = irl.getBehaviorProbabilities(t.convertToZeroRewardEpisodeAnalysis(), conds);
			for(int j = 0; j < probs.length; j++){
				System.out.println("" + probs[j] + "\t" + conds.get(j).rf.toString());
			}
			System.out.println("---------------------");
			
		}
		

	}
	
	
	protected static List<TaskCondition> getPossibleGoalTerminationsFor(State s){
		
		//create domain
		Domain domain = (new SokobanDomain()).generateDomain();
		
		List <TaskCondition> taskConditions = new ArrayList<TabularIRL.TaskCondition>();
		
		PropositionalFunction aInRoom = domain.getPropFunction(SokobanDomain.PFAGENTINROOM);
		addTCsForProp(s, aInRoom, taskConditions);
		
		PropositionalFunction bInRoom = domain.getPropFunction(SokobanDomain.PFBLOCKINROOM);
		addTCsForProp(s, bInRoom, taskConditions);
		
		return taskConditions;
		
	}
	
	protected static void addTCsForProp(State s, PropositionalFunction pf, List <TaskCondition> tcs){
		List <GroundedProp> gps = s.getAllGroundedPropsFor(pf);
		for(GroundedProp gp : gps){
			TerminalFunction tf = new MPOneGPTerminalFunction(gp);
			RewardFunction rf = new MPOneGPRewardFunction(gp);
			tcs.add(new TaskCondition(rf, tf));
		}
	}
	
	
	static class MPOneGPTerminalFunction implements TerminalFunction{

		GroundedProp		gp;
		
		public MPOneGPTerminalFunction(GroundedProp gp){
			this.gp = gp;
		}
		
		@Override
		public boolean isTerminal(State s) {
			return gp.isTrue(s);
		}
			
	}
	
	
	static class MPOneGPRewardFunction implements RewardFunction{

		GroundedProp		gp;
		
		public MPOneGPRewardFunction(GroundedProp gp){
			this.gp = gp;
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			if(this.gp.isTrue(sprime)){
				return 1.;
			}
			
			return 0;
		}
		
		
		@Override
		public String toString(){
			return gp.toString();
		}
		
	}
	
}
