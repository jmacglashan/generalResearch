package commands.interactive;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import commands.experiments.DogTrainingTest;
import commands.model2.CommandsLearningDriver;
import commands.model2.CommandsLearningDriver.RFTFPair;

import auxiliary.DynamicVisualFeedbackEnvironment;
import auxiliary.StateVisualizingGUI;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.dogtraining.DTVisualizer;
import domain.singleagent.dogtraining.DogTraining;

public class InteractiveCommandsGUI extends JFrame implements StateVisualizingGUI{

	private static final long serialVersionUID = 1L;
	
	protected Visualizer									painter;
	protected DynamicVisualFeedbackEnvironment				env;
	protected CommandsLearningDriver						commandsDriver;
	protected PrepStateForCommand							commandPrep;
	protected InteractiveReplanAgent						agent;
	
	protected JTextField									commandField;
	
	int														cWidth = 800;
	int														cHeight = 800;
	
	
	public InteractiveCommandsGUI(Visualizer v, DynamicVisualFeedbackEnvironment env, CommandsLearningDriver driver, PrepStateForCommand commandPrep, InteractiveReplanAgent agent) {
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

	
	@Override
	public void setRenderState(State s) {
		this.painter.updateState(s);
		this.painter.repaint();
	}
	
	
	protected void runCommand(){
		
		String command = commandField.getText().toLowerCase();
		
		System.out.println("Run Command: " + command);
		State commandPreppedState = commandPrep.prepStateForCommand(env.getCurState());
		
		//tell agent to replan and set policy
		RFTFPair task = this.commandsDriver.getMaximiumLikelihoodTaskForCommand(command, commandPreppedState);
		this.agent.changeTask(commandPreppedState, task.rf, task.tf);
		
		
		//update environment to that state
		env.setCurStateTo(commandPreppedState);
		
	}
	
	
	
	public static void main(String [] args){
		
		DogTrainingTest dtcommands = new DogTrainingTest("commands/dogTrainingData2");
		dtcommands.driver.initializeGMandEM();
		dtcommands.driver.runEM(5);
		
		
		DogTraining dt = new DogTraining(5, 5, true);
		final Domain d = dt.generateDomain();
		
		Visualizer v = DTVisualizer.getVisualizer(d, dt.getMap());
		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(d);
		
		Policy defaultPolicy = new Policy() {
			
			Domain md = d;
			
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
				burlap.oomdp.singleagent.Action waitA = md.getAction(DogTraining.ACTIONWAIT);
				GroundedAction ga = new GroundedAction(waitA, "");
				return ga;
			}
		};
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, d.getAttribute(DogTraining.ATTX));
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, d.getAttribute(DogTraining.ATTY));
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, d.getAttribute(DogTraining.ATTHOLDING));
		hashingFactory.addAttributeForClass(DogTraining.CLASSDOG, d.getAttribute(DogTraining.ATTWAITING));
		
		hashingFactory.addAttributeForClass(DogTraining.CLASSTOY, d.getAttribute(DogTraining.ATTX));
		hashingFactory.addAttributeForClass(DogTraining.CLASSTOY, d.getAttribute(DogTraining.ATTY));
		
		
		InteractiveReplanAgent agent = new InteractiveReplanAgent(d, env, defaultPolicy, hashingFactory);
		
		
		
		State initialState = DogTraining.getOneDogNLocationNToyState(d, 4, 1);
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
		
		
		
		
		InteractiveCommandsGUI gui = new InteractiveCommandsGUI(v, env, dtcommands.driver, prep, agent);
		env.setGUI(gui);
		gui.initGUI();
		env.setCurStateTo(initialState);
		
		agent.actLoop();
	}


	
	
	
}
