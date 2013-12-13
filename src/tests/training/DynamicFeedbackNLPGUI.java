package tests.training;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import commands.mttest.IBM2EM;
import commands.mttest.MTDataInstance;
import commands.mttest.Tokenizer;
import commands.mttest.IBM2EM.DecodeResult;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.training.DynamicFeedbackGUI;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskInductionTraining;
import behavior.training.taskinduction.TaskProb;
import burlap.oomdp.visualizer.Visualizer;

public class DynamicFeedbackNLPGUI extends DynamicFeedbackGUI {

	private static final long serialVersionUID = 1L;

	protected TaskInductionTraining trainingAlg;
	protected List<MTDataInstance> dataset = new ArrayList<MTDataInstance>();
	
	protected JTextField commandField;
	protected Tokenizer tokenizer = new Tokenizer(true);
	
	protected boolean trainedOnACommand = false;
	
	protected String lastCommand = "";
	
	public DynamicFeedbackNLPGUI(Visualizer v, DynamicVisualFeedbackEnvironment env) {
		super(v, env);
		
	}
	
	public DynamicFeedbackNLPGUI(Visualizer v, DynamicVisualFeedbackEnvironment env, TaskInductionTraining trainingAlg) {
		super(v, env);
		this.trainingAlg = trainingAlg;
		
		//this.dataset.add(new MTDataInstance(tokenizer, "wallToEast agent0 wallToNorth agent0 inArea agent0 area0", "go to the top right corner of room zero"));
		//this.dataset.add(new MTDataInstance(tokenizer, "wallToWest agent0 wallToSouth agent0 inArea agent0 area2", "go to the bottom left corner of room two"));
	}
	
	public void setTaskInductionTrainer(TaskInductionTraining trainingAlg){
		this.trainingAlg = trainingAlg;
	}

	
	
	@Override
	public void initGUI(){
		
		
		painter.setPreferredSize(new Dimension(cWidth, cHeight));
		getContentPane().add(painter, BorderLayout.CENTER);
		
		Container controlContainer = new Container();
		//controlContainer.setPreferredSize(new Dimension(cWidth, controlHeight));
		getContentPane().add(controlContainer, BorderLayout.SOUTH);
		GridBagLayout layout = new GridBagLayout();
		controlContainer.setLayout(layout);
		
		GridBagConstraints c = new GridBagConstraints();
		
		JButton punishB = new JButton("Punish");
		Action punishAct = new AbstractAction("punish") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("punish");
				env.setReward(-1.);
			}
		};
		punishB.addActionListener(punishAct);
		punishB.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(this.punishKey), "punish");
		punishB.getActionMap().put("punish", punishAct);

		c.gridx = 1;
		c.gridy = 0;
		c.insets = new Insets(20, 0, 0, 0);
		controlContainer.add(punishB, c);
		
		
		JButton rewardB = new JButton("Reward");
		Action rewardAct = new AbstractAction("reward") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("reward");
				env.setReward(1.);
			}
		};
		rewardB.addActionListener(rewardAct);
		rewardB.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(this.rewardKey), "reward");
		rewardB.getActionMap().put("reward", rewardAct);
		c.gridx = 3;
		c.gridy = 0;
		controlContainer.add(rewardB, c);
		
		
		
		JButton terminateB = new JButton("Terminate");
		Action terminateAct = new AbstractAction("terminate") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("terminate");
				env.setTerminal();
			}
		};
		terminateB.addActionListener(terminateAct);
		terminateB.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(this.temrinateKey), "terminate");
		terminateB.getActionMap().put("terminate", terminateAct);
		c.gridx = 2;
		c.gridy = 1;
		c.insets = new Insets(0, 0, 0, 0);
		controlContainer.add(terminateB, c);
		
		
		
		this.commandField = new JTextField(24);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		c.insets = new Insets(20, 0, 0, 0);
		controlContainer.add(this.commandField, c);
		
		JButton newCommandButton = new JButton("Execute Command");
		newCommandButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				startNewTraining();
			}
		});
		c.gridx = 3;
		c.gridwidth = 1;
		controlContainer.add(newCommandButton, c);
		

		
	}
	
	
	
	protected void startNewTraining(){
		System.out.println("Starting new Training");
		
		
		if(trainedOnACommand){
			//extract most likely for a label for our last command to add to our data set
			SemanticTaskDescription mapTask = this.mostLikelyTask();
			MTDataInstance nd = new MTDataInstance(this.tokenizer, mapTask.semeanticString, this.lastCommand);
			System.out.println("Adding training instance\n" + mapTask.semeanticString + " -> " + this.lastCommand);
			dataset.add(nd);
			
			//train our new dataset
			List <TaskDescription> tasksInTraining = this.trainingAlg.getTasks();
			IBM2EM m2 = new IBM2EM(dataset);
			this.setAllGenerating(m2, tasksInTraining);
			//this.tmp(m2);
			m2.runEM(10);
		
			//decode the new command into priors
			List<DecodeResult> probs = m2.probDist(this.tokenizer.tokenize(this.commandField.getText()));
			DecodeResult ddr = m2.decode(this.tokenizer.tokenize(this.commandField.getText()));
			System.out.println("Hard Decode: " + ddr.input.toString() + " -> " + ddr.decoded.toString());
			
			//terminate old now
			env.setTerminal();
			
			//set the probabilties
			for(DecodeResult dr : probs){
				int ind = this.matchingTaskId(dr, tasksInTraining);
				this.trainingAlg.setProbFor(ind, dr.prob);
			}
		
			
			
		}
		
		lastCommand = this.commandField.getText();
		//lastCommand = "go to the top right corner of room two";
		
		trainedOnACommand = true;
		
	}
	
	protected SemanticTaskDescription mostLikelyTask(){
		
		List<TaskProb> probs = this.trainingAlg.getPosteriors().getTaskProbs();
		double max = 0.;
		SemanticTaskDescription maxTask = null;
		for(TaskProb tp : probs){
			if(tp.getProb() > max){
				max = tp.getProb();
				maxTask = (SemanticTaskDescription)tp.getTask();
			}
		}
		
		
		return maxTask;
		
	}
	
	protected int matchingTaskId(DecodeResult dr, List <TaskDescription> tasks){
		
		String sString = dr.decoded.toString();
		for(int i = 0; i < tasks.size(); i++){
			SemanticTaskDescription std = (SemanticTaskDescription)tasks.get(i);
			if(std.semeanticString.equals(sString)){
				return i;
			}
		}
		
		System.out.println("error");
		for(int i = 0; i < tasks.size(); i++){
			SemanticTaskDescription std = (SemanticTaskDescription)tasks.get(i);
			System.out.println(std.semeanticString);
		}
		
		throw new RuntimeException("Could not finding matching task for " + sString);
		
	}
	
	protected void setAllGenerating(IBM2EM m2, List<TaskDescription> tasks){
		for(TaskDescription td : tasks){
			SemanticTaskDescription std = (SemanticTaskDescription)td;
			m2.addGeneratingText(this.tokenizer.tokenize(std.semeanticString));
		}
	}
	
	protected void tmp(IBM2EM m2){
		for(int i = 0; i < 4; i++){
			
			m2.addGeneratingText(tokenizer.tokenize("wallWest agent0 wallSouth agent0 inArea agent0 area"+i));
			m2.addGeneratingText(tokenizer.tokenize("wallWest agent0 wallNorth agent0 inArea agent0 area"+i));
			m2.addGeneratingText(tokenizer.tokenize("wallEast agent0 wallNorth agent0 inArea agent0 area"+i));
			m2.addGeneratingText(tokenizer.tokenize("wallEast agent0 wallSouth agent0 inArea agent0 area"+i));
			
			
		}
	}
	

}
