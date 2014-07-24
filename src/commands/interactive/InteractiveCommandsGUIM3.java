package commands.interactive;

import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import auxiliary.DynamicVisualFeedbackEnvironment;
import auxiliary.StateVisualizingGUI;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.visualizer.Visualizer;

import commands.data.TrainingElement;
import commands.experiments.DogTrainingTest.ConstantPF;
import commands.model3.GPConjunction;
import commands.model3.Model3Controller;
import commands.model3.TaskModule;
import commands.model3.TaskModule.ConjunctiveGroundedPropRF;
import commands.model3.TaskModule.RFConVariableValue;
import commands.model3.TrajectoryModule.ConjunctiveGroundedPropTF;
import commands.model3.mt.Tokenizer;
import commands.model3.mt.em.MTEMModule;
import commands.model3.mt.em.WeightedMTInstance;

import domain.singleagent.dogtraining.DTStateParser;
import domain.singleagent.dogtraining.DTVisualizer;
import domain.singleagent.dogtraining.DogTraining;

public class InteractiveCommandsGUIM3 extends JFrame implements StateVisualizingGUI{

	private static final long serialVersionUID = 1L;
	
	
	
	public static String 									DATASETTESTPATH = "dataFiles/commands/dogTrainingData2";
	
	
	
	protected Visualizer									painter;
	protected DynamicVisualFeedbackEnvironment				env;
	protected Model3Controller								commandsDriver;
	
	protected PrepStateForCommand							commandPrep;
	protected InteractiveReplanAgent						agent;
	
	protected JTextField									commandField;
	
	int														cWidth = 800;
	int														cHeight = 800;
	
	
	
	
	public InteractiveCommandsGUIM3(Visualizer v, DynamicVisualFeedbackEnvironment env, Model3Controller driver, PrepStateForCommand commandPrep, InteractiveReplanAgent agent) {
		this.painter = v;
		this.env = env;
		this.commandsDriver = driver;
		this.commandPrep = commandPrep;
		this.agent = agent;
		
	}
	
	public void initGUI(){
		
		painter.setPreferredSize(new Dimension(cWidth, cHeight));
		getContentPane().add(painter, BorderLayout.CENTER);
		
		Container controlContainer = new Container();
		//controlContainer.setPreferredSize(new Dimension(cWidth, controlHeight));
		getContentPane().add(controlContainer, BorderLayout.SOUTH);
		GridBagLayout layout = new GridBagLayout();
		controlContainer.setLayout(layout);
		
		GridBagConstraints c = new GridBagConstraints();
		
		this.commandField = new JTextField(20);
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(20, 0, 0, 0);
		controlContainer.add(commandField, c);
		
		JButton commandButton = new JButton("Give Command");
		Action commandAct = new AbstractAction("command") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				runCommand();
			}
		};
		commandButton.addActionListener(commandAct);
		//commandButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('a'), "punish");
		//commandButton.getActionMap().put("punish", punishAct);
		
		c.gridx=1;
		controlContainer.add(commandButton, c);
		
		
		
		
		pack();
		setVisible(true);
		
	}
	
	
	protected void runCommand(){
		
		String command = commandField.getText().toLowerCase();
		
		System.out.println("Run Command: " + command);
		State commandPreppedState = commandPrep.prepStateForCommand(env.getCurState());
		
		
		//tell agent to replan and set policy
		GMQueryResult predicted = GMQueryResult.maxProb(commandsDriver.getRFDistribution(commandPreppedState, command));
		RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(this.commandsDriver.getGM().getRVarWithName(TaskModule.GROUNDEDRFNAME));
		ConjunctiveGroundedPropRF rf = gr.rf;
		ConjunctiveGroundedPropTF tf = new ConjunctiveGroundedPropTF(rf.gps);
		System.out.println(predicted.probability + ": " + tf.toString());
		
		//RFTFPair task = this.commandsDriver.getMaximiumLikelihoodTaskForCommand(command, commandPreppedState);
		//this.agent.changeTask(commandPreppedState, task.rf, task.tf);
		this.agent.changeTask(commandPreppedState, rf, tf);
		
		//update environment to that state
		env.setCurStateTo(commandPreppedState);
		
	}
	

	@Override
	public void setRenderState(State s) {
		this.painter.updateState(s);
		this.painter.repaint();
	}
	
	
	
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 2){
			System.out.println("Requires path to training dataset directory and dog images directory.");
			System.exit(0);
		}
		
		String trainingPath = args[0];
		
		DogTraining dt = new DogTraining(5, 5, true);
		final Domain oomdpDomain = dt.generateDomain();
		PropositionalFunction constant = new ConstantPF("constantPF", oomdpDomain, new String[]{DogTraining.CLASSDOG}, "constantClass");
		
		StateParser sp = new DTStateParser(oomdpDomain);
		
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);
		String [] wallDirs = new String[]{DogTraining.PFWALLNORTH, DogTraining.PFWALLSOUTH, DogTraining.PFWALLEAST, DogTraining.PFWALLEAST};
		
		GPConjunction atr = null;
		
		/*
		for(String wallDir : wallDirs){
			atr = new GPConjunction();
			atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(wallDir), new String[]{"d"}));
			atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFWAIT), new String[]{"d"}));
			liftedTaskDescriptions.add(atr);
		}
		*/
		
		atr = new GPConjunction();
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFHASTOY), new String[]{"d", "t"}));
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFWAIT), new String[]{"d"}));
		liftedTaskDescriptions.add(atr);
		
		atr = new GPConjunction();
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFNOTOY), new String[]{"d"}));
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFWAIT), new String[]{"d"}));
		liftedTaskDescriptions.add(atr);
		
		atr = new GPConjunction();
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFDOGAT), new String[]{"d", "l"}));
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFWAIT), new String[]{"d"}));
		liftedTaskDescriptions.add(atr);
		
		atr = new GPConjunction();
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFTOYAT), new String[]{"t", "l"}));
		atr.addGP(new GroundedProp(oomdpDomain.getPropFunction(DogTraining.PFWAIT), new String[]{"d"}));
		liftedTaskDescriptions.add(atr);
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, oomdpDomain.getAttribute(DogTraining.ATTX));
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, oomdpDomain.getAttribute(DogTraining.ATTY));
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, oomdpDomain.getAttribute(DogTraining.ATTHOLDING));
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, oomdpDomain.getAttribute(DogTraining.ATTWAITING));
		
		hashingFactory.addAttributeForClass(DogTraining.CLASSTOY, oomdpDomain.getAttribute(DogTraining.ATTX));
		hashingFactory.addAttributeForClass(DogTraining.CLASSTOY, oomdpDomain.getAttribute(DogTraining.ATTY));
		
		Model3Controller controller = new Model3Controller(oomdpDomain, liftedTaskDescriptions, hashingFactory, false);
		GenerativeModel gm = controller.getGM();
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(oomdpDomain, trainingPath, sp);
		List<WeightedMTInstance> mtDataset = controller.getWeightedMTDatasetFromTrajectoryDataset(trainingDataset, tokenizer, 1.e-20);
		
		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, false);
		
		//now do learning
		System.out.println("Starting training.");
		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(10);
		System.out.println("Finished training; beginning testing.");
		
		
		Visualizer v = DTVisualizer.getVisualizer(oomdpDomain, dt.getMap(), args[1]);
		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(oomdpDomain);
		
		Policy defaultPolicy = new Policy() {
			
			
			@Override
			public boolean isStochastic() {
				return false;
			}
			
			@Override
			public List<ActionProb> getActionDistributionForState(State s) {
				return this.getDeterministicPolicy(s);
			}
			
			@Override
			public GroundedAction getAction(State s) {
				burlap.oomdp.singleagent.Action waitA = oomdpDomain.getAction(DogTraining.ACTIONWAIT);
				GroundedAction ga = new GroundedAction(waitA, "");
				return ga;
			}
			
			@Override
			public boolean isDefinedFor(State s) {
				return true;
			}
		};
		
		InteractiveReplanAgent agent = new InteractiveReplanAgent(oomdpDomain, env, defaultPolicy, hashingFactory);
		
		State initialState = DogTraining.getOneDogNLocationNToyState(oomdpDomain, 4, 1);
		DogTraining.setDog(initialState, 2, 1, 4, 0);
		
		DogTraining.setLocation(initialState, 0, 2, 0, DogTraining.LIDHOME);
		DogTraining.setLocation(initialState, 1, 0, 4, DogTraining.LIDRED);
		DogTraining.setLocation(initialState, 2, 2, 4, DogTraining.LIDGREEN);
		DogTraining.setLocation(initialState, 3, 4, 4, DogTraining.LIDBLUE);
		
		DogTraining.setToy(initialState, 0, 1, 4);
		
		
		
		
		
		PrepStateForCommand prep = new PrepStateForCommand() {
			
			@Override
			public State prepStateForCommand(State s) {
				State ns = s.copy();
				DogTraining.setDogWaiting(ns, 0);
				return ns;
			}
		};
		
		InteractiveCommandsGUIM3 gui = new InteractiveCommandsGUIM3(v, env, controller, prep, agent);
		
		env.setGUI(gui);
		gui.initGUI();
		env.setCurStateTo(initialState);
		
		agent.actLoop();

	}

}
