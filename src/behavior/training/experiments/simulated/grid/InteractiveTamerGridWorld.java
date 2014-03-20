package behavior.training.experiments.simulated.grid;

import java.util.ArrayList;
import java.util.List;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.training.DynamicFeedbackGUI;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.visualizer.Visualizer;

public class InteractiveTamerGridWorld {

	protected GridWorldDomain					gwg;
	protected Domain							domain;
	protected State								initialState;
	protected DiscreteMaskHashingFactory		hashingFactory;
	protected Visualizer						visualizer;
	
	
	public static void main(String [] args){
		InteractiveTamerGridWorld e = new InteractiveTamerGridWorld();
		e.runInteractiveTraining();
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
	
	
	public class GWAtLocTest implements StateConditionTest{

		int x,y;
		
		public GWAtLocTest(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public boolean satisfies(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int x = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int y = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
			
			return x == this.x && y == this.y;
		}
		
		
		
	}
	
}
