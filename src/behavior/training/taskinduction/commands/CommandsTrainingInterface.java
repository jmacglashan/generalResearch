package behavior.training.taskinduction.commands;

import java.util.ArrayList;
import java.util.List;

import auxiliary.DynamicFeedbackEnvironment;
import behavior.training.experiments.interactive.soko.DynamicPlanISABL;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskInductionTraining;
import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.singleagent.environment.DomainEnvironmentWrapper;

import commands.model3.GPConjunction;
import commands.model3.mt.Tokenizer;

public class CommandsTrainingInterface {

	protected Domain								domain;
	protected Domain								domainEnvWrapper;
	protected Domain								planningDomain;
	protected DynamicFeedbackEnvironment			env;
	protected CommandsToTrainingInterface			commandInterface;
	protected TaskInductionTraining					agent;
	
	protected RewardFunction						envRF;
	protected TerminalFunction						envTF;
	
	protected Action								noopAction;
	
	protected Thread								agentThread;
	protected boolean								agentIsRunning = false;
	
	protected State									initialState;
	protected String								lastCommand;
	
	protected TaskDescription						lastMostLikelyTask = null;
	
	
	protected double								commandProbLearningThreshold = 5e-5;
	
	public CommandsTrainingInterface(DomainGenerator dgen){
		
		this.domain = dgen.generateDomain();
		this.noopAction = new NullAction("noop", domain, ""); //add noop to the operating domain
		
		this.env = new DynamicFeedbackEnvironment(this.domain);
		
		DomainEnvironmentWrapper dEnvWrapper = new DomainEnvironmentWrapper(this.domain, this.env);
		this.domainEnvWrapper = dEnvWrapper.generateDomain();
		
		//generate a domain without the noop for planning
		this.planningDomain = dgen.generateDomain();
		
		this.envRF = this.env.getEnvironmentRewardRFWrapper();
		this.envTF = this.env.getEnvironmentTerminalStateTFWrapper();
		
		
	}
	
	
	public void intantiateDefaultAgent(StateHashFactory hashingFactory, List<FeedbackStrategy> feedbackStrategies){
		
		this.agent = new DynamicPlanISABL(domainEnvWrapper, this.envRF, this.envTF, hashingFactory, new ArrayList<TaskDescription>(), 
				new MAPMixtureModelPolicy());
		this.agent.setNoopAction(noopAction);
		this.agent.useSeperatePlanningDomain(this.planningDomain);
		for(FeedbackStrategy fs : feedbackStrategies){
			((TaskInductionWithFeedbackStrategies)this.agent).addFeedbackStrategy(fs);
		}
		
	}
	
	public void setAgent(TaskInductionTraining agent){
		this.agent = agent;
	}
	
	public void instatiateCommandsLearning(StateHashFactory hashingFactory, Tokenizer tokenizer, List<GPConjunction> liftedTasks, int maxBindingConstraints){
		this.commandInterface = new CommandsToTrainingInterface(this.domain, liftedTasks, hashingFactory, this.agent, tokenizer, maxBindingConstraints);
	}
	
	public Domain getOperatingDomain(){
		return this.domain;
	}
	
	
	/**
	 * Adding an action observer to the operating domain allows the observer to receive the actions the agent has taken in the environment before the agent
	 * observes the result in the environment. This is useful for allowing a human to observe the result of an aciton and given them time to provide feedback
	 * for it.
	 * @param observer the action observer to add to the operating domain
	 */
	public void addActionObserverToOperatingDomain(ActionObserver observer){
		((SADomain)this.domain).addActionObserverForAllAction(observer);
	}
	
	public State getEndStateOfMostLikelyTask(){
		TaskProb tp = this.agent.getPosteriors().getMostLikelyTask();
		EpisodeAnalysis ea = tp.getPolicy().evaluateBehavior(this.env.getCurState(), tp.getRf(), tp.getTf());
		State endState = ea.getState(ea.numTimeSteps()-1);
		return endState;
	}
	
	public TaskProb getMostLikelyTask(){
		return this.agent.getPosteriors().getMostLikelyTask();
	}
	
	public void giveCommandInInitialState(final State s, String command){
		
		//remember this state and command for learning completion
		this.initialState = s;
		this.lastCommand = command;
		
		//first set our environment to this state
		this.env.setCurStateTo(s);
		this.env.receiveIsTerminalSignal(false);
		
		//then let agent reason about the command and setup its task distribution
		this.commandInterface.setRFDistribution(s, command);
		
		//then let learning start in a separate thread so that the user can interact with it
		this.agentThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				agent.runLearningEpisodeFrom(s);
			}
		});
		
		this.agentIsRunning = true;
		this.agentThread.start();
		
	}
	
	
	public void giveTerminateAndLearnSignal(){
		
		this.giveTerminateSignal();
		
		this.commandInterface.addLastTrainingResultToDatasetAndRetrain(this.initialState, lastCommand, commandProbLearningThreshold);
	}
	
	public void giveTerminateSignal(){
		
		this.env.receiveIsTerminalSignal(true);
		
		
		try {
			this.agentThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.agentIsRunning = false;
	}
	
	public boolean agentIsRunning(){
		return this.agentIsRunning;
	}
	
	public void giveReward(){
		this.env.receiveHumanFeedback(1.);
	}
	
	public void givePunishment(){
		this.env.receiveHumanFeedback(-1.);
	}
	
}
