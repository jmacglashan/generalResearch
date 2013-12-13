package tests.training;

import java.util.ArrayList;
import java.util.List;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskInductionTraining;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.visualizer.Visualizer;

public class MGWDriver {

	MacroGridWorldGenerator 		dg;
	Domain							domain;
	State 							initialState;
	DiscreteStateHashFactory		hashingFactory;
	List<SemanticTaskDescription>	tasks;
	
	
	public static void main(String[] args) {
		
		MGWDriver driver = new MGWDriver();
		driver.runTraining();
		

	}
	
	
	public MGWDriver(){
		
		dg = new MacroGridWorldGenerator(11, 11);
		dg.setMapToFourRooms();
		domain = dg.generateDomain();
		Action noop = new NullAction("noop", domain, ""); //add noop to the domain
		
		initialState = MacroGridWorldGenerator.getOneAgentNLocationMAreaState(domain, 0, 4);
		MacroGridWorldGenerator.setAgent(initialState, 2, 2);
		MacroGridWorldGenerator.setArea(initialState, 0, 4, 0, 0, 4);
		MacroGridWorldGenerator.setArea(initialState, 1, 10, 0, 6, 4);
		MacroGridWorldGenerator.setArea(initialState, 2, 10, 6, 5, 10);
		MacroGridWorldGenerator.setArea(initialState, 3, 3, 6, 0, 10);
		
		tasks = new ArrayList<SemanticTaskDescription>();
		
		for(int i = 0; i < 4; i++){
			tasks.add(new SemanticTaskDescription(this.getPropsForTaskDescrip(GridWorldDomain.PFWALLWEST, GridWorldDomain.PFWALLSOUTH, i)));
			tasks.add(new SemanticTaskDescription(this.getPropsForTaskDescrip(GridWorldDomain.PFWALLWEST, GridWorldDomain.PFWALLNORTH, i)));
			tasks.add(new SemanticTaskDescription(this.getPropsForTaskDescrip(GridWorldDomain.PFWALLEAST, GridWorldDomain.PFWALLNORTH, i)));
			tasks.add(new SemanticTaskDescription(this.getPropsForTaskDescrip(GridWorldDomain.PFWALLEAST, GridWorldDomain.PFWALLSOUTH, i)));
		}
		
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList); //optional code line; uses only the agent position to perform hash calculations instead of the agent and all locations
		
		
	}
	
	
	public void runTraining(){
		
		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(domain);
		Domain domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		
		
		List <TaskDescription> javasDumbTypeWraper = new ArrayList<TaskDescription>();
		for(SemanticTaskDescription td : this.tasks){
			javasDumbTypeWraper.add(td);
		}
		
		TaskInductionTraining agent = new TaskInductionTraining(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, javasDumbTypeWraper, new MAPMixtureModelPolicy());
		for(int i = 0; i < tasks.size(); i++){
			agent.setProbFor(i, 1./(double)tasks.size());
		}
		
		agent.useSeperatePlanningDomain(domain);
		agent.planPossibleTasksFromSeedState(initialState);
		
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, dg.getMap());
		DynamicFeedbackNLPGUI gui = new DynamicFeedbackNLPGUI(v, env, agent);
		env.setGUI(gui);
		
		
		boolean hasInitedGUI = false;
		
		for(int i = 0; i < 20; i++){
			env.setCurStateTo(initialState);
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
			agent.runLearningEpisodeFrom(initialState);
		}
		
		System.out.println("finished training");
		
	}
	
	
	protected List<GroundedProp> getPropsForTaskDescrip(String wall1, String wall2, int area){
		List <GroundedProp> gps = new ArrayList<GroundedProp>();
		
		gps.add(new GroundedProp(domain.getPropFunction(wall1), new String[]{"agent0"}));
		gps.add(new GroundedProp(domain.getPropFunction(wall2), new String[]{"agent0"}));
		gps.add(new GroundedProp(domain.getPropFunction(MacroGridWorldGenerator.PFINAREA), new String[]{"agent0","area"+area}));
		
		
		return gps;
	}

}
