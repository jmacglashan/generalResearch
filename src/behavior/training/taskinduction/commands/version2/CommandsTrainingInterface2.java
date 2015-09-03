package behavior.training.taskinduction.commands.version2;

import auxiliary.DynamicFeedbackEnvironment;
import behavior.training.experiments.interactive.soko.DynamicPlanISABL;
import behavior.training.experiments.interactive.soko.SokoAStarPlanner;
import behavior.training.experiments.interactive.soko.sokoamdp.SokoAMDPPlannerPolicyGen;
import behavior.training.taskinduction.*;
import behavior.training.taskinduction.commands.CommandsToTrainingInterface;
import behavior.training.taskinduction.commands.CommandsTrainingInterface;
import behavior.training.taskinduction.sabl.SABLAgent;
import behavior.training.taskinduction.sabl.extendedEpisode.SABLExtendedEA;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class CommandsTrainingInterface2 {

	protected Domain domain;
	protected Domain								domainEnvWrapper;
	protected Domain								planningDomain;
	protected DynamicFeedbackEnvironment env;
	protected CommandsToTrainingInterface2 commandInterface;
	protected SABLAgent agent;
	protected StateHashFactory hashingFactory;

	protected RewardFunction envRF;
	protected TerminalFunction envTF;

	protected Action noopAction;

	protected Thread								agentThread;
	protected boolean								agentIsRunning = false;

	protected State initialState = null;
	protected String								lastCommand;

	protected TaskDescription lastMostLikelyTask = null;

	protected boolean								alwaysResetPriorsWithCommand = true;


	protected double								commandProbLearningThreshold = 5e-5;


	protected List <List<List<Double>>>				allEpisodeRewardPerStepSequneces = new ArrayList<List<List<Double>>>();
	protected List <String>							commandHistory = new ArrayList<String>();


	protected boolean								removeRPPMWhenTrueSatisfied = false;
	protected Map<String, String>					trueGoals = new HashMap<String, String>();
	protected List<FeedbackStrategy>				lastFullStrategyDistribution;
	protected boolean 								agentUsingRPPP = true;


	public CommandsTrainingInterface2(DomainGenerator dgen){

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

	public void setActionDelay(int delay){
		this.env.setActionUpdateDelay((long)delay);
	}

	/**
	 * Sets the speed mode.
	 * @param mode 0 = linear; 1 = sigmoid; 2 = threshold.
	 */
	public void setSpeedMode(int mode){
		this.agent.setSpeedMode(mode);
	}

	public void setRemoveRPPMWhenTrueSatisfied(boolean removeRPPMWhenTrueSatisfied){
		this.removeRPPMWhenTrueSatisfied = removeRPPMWhenTrueSatisfied;
	}

	public void addTrueGoal(String command, String goalRep){
		this.trueGoals.put(command, goalRep);
	}

	public void intantiateDefaultAgent(StateHashFactory hashingFactory, List<FeedbackStrategy> feedbackStrategies){

		/*this.agent = new DynamicPlanISABL(domainEnvWrapper, this.envRF, this.envTF, hashingFactory, new ArrayList<TaskDescription>(),
				new MAPMixtureModelPolicy());*/

		this.agent = new SABLAgent(domainEnvWrapper, this.planningDomain, this.envRF, this.envTF, hashingFactory, new SokoAMDPPlannerPolicyGen());

		this.agent.setNoopAction(noopAction);
		this.agent.setFeedbackStrategies(feedbackStrategies);
		this.hashingFactory = hashingFactory;
		this.agent.setNumEpisodesToStore(100);
		this.agent.setEnv(this.env);


	}

	public void setAgent(SABLAgent agent){
		this.agent = agent;
		this.agent.setEnv(env);
	}

	public void instatiateCommandsLearning(StateHashFactory hashingFactory, Tokenizer tokenizer, List<GPConjunction> liftedTasks, int maxBindingConstraints){
		this.commandInterface = new CommandsToTrainingInterface2(this.domain, liftedTasks, hashingFactory, this.agent, tokenizer, maxBindingConstraints);
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

	/**
	 * If set to true, then giving a command will always set the task beliefs to those dictated by the command prior.
	 * If set to false, then if the command and state is the same, then the same task beliefs are retained.
	 * @param alwaysReset a boolean
	 */
	public void setAlwaysResetPriorsWithCommand(boolean alwaysReset){
		this.alwaysResetPriorsWithCommand = alwaysReset;
	}

	public State getEndStateOfMostLikelyTask(){
		TaskProb tp = this.agent.getMostLikelyTask();
		EpisodeAnalysis ea = tp.getPolicy().evaluateBehavior(this.env.getCurState(), tp.getRf(), tp.getTf());
		State endState = ea.getState(ea.numTimeSteps()-1);
		return endState;
	}

	public TaskProb getMostLikelyTask(){
		return this.agent.getMostLikelyTask();
	}

	public void giveCommandInInitialState(final State s, String command){

		System.out.println("Received command: " + command);


		boolean isSameAsLast = false;

		if(this.initialState != null){
			//isSameAsLast = s == this.initialState && command.equals(this.lastCommand);
			StateHashTuple hs = this.hashingFactory.hashState(s);
			StateHashTuple hi = this.hashingFactory.hashState(this.initialState);
			isSameAsLast = hs.equals(hi) && command.equals(this.lastCommand);
		}

		if(isSameAsLast && this.removeRPPMWhenTrueSatisfied && this.agentUsingRPPP){

			String trueGoal = this.trueGoals.get(this.lastCommand);
			if(trueGoal != null){
				//check last goal
				GroundedProp trueGoalGP = this.parseStringIntoGP(trueGoal);
				TaskDescription td = this.agent.getMostLikelyTask().getTask();
				EpisodeAnalysis lastEpisode = this.agent.getLastLearningEpisode();
				State lastEpisodeLastState = lastEpisode.getState(lastEpisode.maxTimeStep());
				if(trueGoalGP.isTrue(lastEpisodeLastState) && td.toString().equals(trueGoalGP.toString())){
					System.out.println("Should Remove R+/P-");
					this.lastFullStrategyDistribution = this.agent.getStrategyProbabiltyDistribution();
					List <FeedbackStrategy> removedRPPM = new ArrayList<FeedbackStrategy>(this.lastFullStrategyDistribution);
					FeedbackStrategy rppm = this.findRPPM(removedRPPM);
					if(rppm != null){
						System.out.println("Removing: " + rppm.toString());
						removedRPPM.remove(rppm);
						List <FeedbackStrategy> normed = this.renormalized(removedRPPM);
						this.agent.updateTaskPriorsToPosteriors();
						this.agent.setFeedbackStrategies(normed);
						this.agent.initializeJointProbabilities();
						this.agentUsingRPPP = false;
					}

				}
				//make sure last state is goal satisfying state and that most likely task is true task.
			}

		}
		else if(!isSameAsLast && !this.agentUsingRPPP){
			System.out.println("Should add back R+/P-");
			this.agent.setFeedbackStrategies(this.lastFullStrategyDistribution);
			this.agent.initializeJointProbabilities();
			this.agentUsingRPPP = true;
		}

		//remember this state and command for learning completion
		this.initialState = s.copy();
		this.lastCommand = command;

		//first set our environment to this state
		this.env.setCurStateTo(s);
		this.env.receiveIsTerminalSignal(false);

		if(this.alwaysResetPriorsWithCommand || !isSameAsLast) {
			//then let agent reason about the command and setup its task distribution
			System.out.println("Resetting beliefs!");
			this.commandInterface.setRFDistribution(s, command);
		}
		else{
			System.out.println("Keeping same beliefs!");
		}

		this.commandHistory.add(command);

		//then let learning start in a separate thread so that the user can interact with it
		this.agentThread = new Thread(new Runnable() {

			@Override
			public void run() {
				agent.runLearningEpisodeFrom(CommandsTrainingInterface2.this.initialState);
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

		if(this.agentIsRunning) {

			this.env.receiveIsTerminalSignal(true);


			try {
				this.agentThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.agentIsRunning = false;

			this.allEpisodeRewardPerStepSequneces.add(this.env.getAndResetrewardSequences());

			/*
			if(this.agent.getAllStoredLearningEpisodes().size() == 2){
				this.writeAllEpisodesToFiles("testBeliefDir");
			}
			*/

		}
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



	public void writeAllEpisodesToFiles(String directoryName){
		this.writeAllEpisodesToFiles(directoryName, new StateYAMLParser(this.domain));
	}



	public void writeAllEpisodesToFiles(String directoryName, StateParser sp){

		if(!directoryName.endsWith("/")){
			directoryName = directoryName + "/";
		}

		List<EpisodeAnalysis> eas = this.agent.getAllStoredLearningEpisodes();
		for(int i = 0; i < eas.size(); i++){
			SABLExtendedEA ea = (SABLExtendedEA)eas.get(i);

			String epFName = String.format(directoryName+"episode%03d", i);
			ea.writeToFile(epFName, sp);

			String bFName = String.format(directoryName+"beliefs%03d", i);
			ea.writeBeliefsToFile(bFName);

			String cFName = String.format(directoryName+"commands%03d.commands", i);
			try{

				String str = this.commandHistory.get(i);
				BufferedWriter out = new BufferedWriter(new FileWriter(cFName));
				out.write(str);
				out.close();


			}catch(Exception e){
				System.out.println(e);
			}

			String rfSeqName = String.format(directoryName+"allFeedback%03d.feedback", i);
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(rfSeqName));
				List<List<Double>> perStepFeedbacks = this.allEpisodeRewardPerStepSequneces.get(i);

				for(List<Double> stepFeedback : perStepFeedbacks){
					boolean first = true;
					for(Double d : stepFeedback){
						if(!first){
							out.write(" ");
						}
						out.write(""+d);
						first = false;
					}
					out.write("\n");

				}

				out.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}


	protected GroundedProp parseStringIntoGP(String s){
		String [] comps = s.split(" ");
		String [] args = new String[comps.length-1];
		for(int i = 1; i < comps.length; i++){
			args[i-1] = comps[i];
		}
		GroundedProp gp = new GroundedProp(this.domain.getPropFunction(comps[0]), args);
		return gp;
	}

	protected FeedbackStrategy findRPPM(List<FeedbackStrategy> strategies){
		double maxGap = 0.;
		FeedbackStrategy rppm = null;
		for(FeedbackStrategy fs : strategies){
			double gap = fs.getMuIncorrect() - fs.getMuCorrect();
			if(gap > maxGap){
				maxGap = gap;
				rppm = fs;
			}
		}
		return rppm;
	}

	protected List <FeedbackStrategy> renormalized(List <FeedbackStrategy> strats){
		double sum = 0.;
		for(FeedbackStrategy fs : strats){
			sum += fs.getProbOfStrategy();
		}
		List<FeedbackStrategy> normed = new ArrayList<FeedbackStrategy>(strats.size());
		for(FeedbackStrategy fs : strats){
			FeedbackStrategy nfs = new FeedbackStrategy(fs.getMuCorrect(), fs.getMuIncorrect(), fs.getEpsilon());
			nfs.setProbOfStrategy(fs.getProbOfStrategy() / sum);
			nfs.setName(fs.getName());
			normed.add(nfs);
		}

		return normed;
	}


}
