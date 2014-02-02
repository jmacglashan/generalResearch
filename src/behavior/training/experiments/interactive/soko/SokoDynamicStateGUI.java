package behavior.training.experiments.interactive.soko;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import auxiliary.DynamicVisualFeedbackEnvironment;
import auxiliary.StateVisualizingGUI;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.training.taskinduction.MAPMixtureModelPolicy;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.commands.CommandsToTrainingInterface;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import behavior.training.taskinduction.strataware.TaskInductionWithFeedbackStrategies;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.visualizer.MultiLayerRenderer;
import burlap.oomdp.visualizer.StateRenderLayer;

import commands.model3.GPConjunction;
import commands.model3.mt.Tokenizer;

import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Visualizer;

public class SokoDynamicStateGUI extends JFrame implements StateVisualizingGUI,MouseMotionListener,MouseListener,KeyListener{

	private static final long serialVersionUID = 1L;
	
	
	protected int						maxX = 20;
	protected int						maxY = 20;

	
	protected Domain					domain;
	protected Domain					planningDomain;
	protected Domain					domainEnvWrapper;
	
	protected DynamicVisualFeedbackEnvironment		env;
	
	
	protected DiscreteStateHashFactory	hashingFactory;
	protected Action					noopAction;
	
	protected State						curState;
	protected State						initialState;
	
	protected MultiLayerRenderer		canvas;
	protected StateRenderLayer			sLayer;
	protected CursorHighlightLayer		cLayer;
	protected HallucinateStateRenderLayer hLayer;
	
	protected JLabel					cellWidthLabel;
	protected JLabel					cellHeightLabel;
	protected JTextField				cellWidthField;
	protected JTextField				cellHeightField;
	
	protected JTextArea					commandArea;
	
	protected JButton					giveCommandButton;
	protected JButton					finishTrainingButton;
	protected JButton					rewardButton;
	protected JButton					punishButton;
	
	protected JCheckBox					hallucinateButton;
	
	protected int						lastCellX;
	protected int						lastCellY;
	
	protected int						mouseDownStartCellX;
	protected int						mouseDownStartCellY;
	
	protected ObjectInstance			currentlyCreatedRoom = null;
	
	protected ObjectInstance			selectedObject = null;
	protected boolean					freezeSelection = false;
	
	
	protected boolean					needsRepaint = false;
	
	
	protected TextArea					propViewer;
	
	protected CommandsToTrainingInterface			commandInterface;
	protected TaskInductionWithFeedbackStrategies	agent;
	
	protected Thread					agentThread;
	
	protected TaskDescription			lastMostLikelyTask = null;
	
	
	public SokoDynamicStateGUI(){
		canvas = new MultiLayerRenderer();
		sLayer = Sokoban2Visualizer.getStateRenderLayer(maxX, maxY, "robotImages");
		canvas.addRenderLayer(sLayer);
		
		cLayer = new CursorHighlightLayer(maxX, maxY);
		canvas.addRenderLayer(cLayer);
		
		Sokoban2Domain dgen = new Sokoban2Domain();
		dgen.includeDirectionAttribute(true);
		this.domain = dgen.generateDomain();
		noopAction = new NullAction("noop", domain, ""); //add noop to the domain
		
		this.env = new DynamicVisualFeedbackEnvironment(this.domain);
		this.env.setGUI(this);
		domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();
		
		this.planningDomain = dgen.generateDomain();
		
		this.hashingFactory = new DiscreteMaskHashingFactory();
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSAGENT, this.domain.getAttribute(Sokoban2Domain.ATTX));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSAGENT, this.domain.getAttribute(Sokoban2Domain.ATTY));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTX));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTY));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTCOLOR));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTSHAPE));
		
		this.curState = Sokoban2Domain.getClassicState(this.domain);
		
		sLayer.updateState(this.curState);
		
		StateRenderLayer abSL = new StateRenderLayer();
		abSL.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new Sokoban2Visualizer.AgentPainterWithImages("robotImages", maxX, maxY));
		abSL.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new Sokoban2Visualizer.BlockPainter(maxX, maxY, "robotImages"));
		hLayer = new HallucinateStateRenderLayer(abSL);
		hLayer.setOpacity(0.5f);
		canvas.addRenderLayer(hLayer);
		
		
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		//this.agent = new TaskInductionWithFeedbackStrategies(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, new ArrayList<TaskDescription>(), 
				//new MAPMixtureModelPolicy());
		
		this.agent = new DynamicPlanISABL(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, new ArrayList<TaskDescription>(), 
			new MAPMixtureModelPolicy());
		this.agent.setNoopAction(noopAction);
		this.agent.useSeperatePlanningDomain(domain);
		this.agent.addFeedbackStrategy(new FeedbackStrategy(0.7, 0.7, 0.1)); 
		//this.agent.addFeedbackStrategy(new FeedbackStrategy(0.05, 0.7, 0.1));
		//this.agent.addFeedbackStrategy(new FeedbackStrategy(0.7, 0.05, 0.1));
		
		
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);
		
		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);
		
		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		this.commandInterface = new CommandsToTrainingInterface(domain, liftedTaskDescriptions, hashingFactory, this.agent, tokenizer, 22);
		
	}
	
	
	
	public void initGUI(){
		
		this.propViewer = new TextArea();
		this.propViewer.setEditable(false);
		
		this.canvas.setPreferredSize(new Dimension(800, 800));
		
		propViewer.setPreferredSize(new Dimension(800, 100));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container bottomContainer = new Container();
		bottomContainer.setLayout(new BorderLayout());
		bottomContainer.add(propViewer, BorderLayout.NORTH);
		
		
		Container viewerContainer = new Container();
		viewerContainer.setLayout(new BorderLayout());
		
		viewerContainer.add(bottomContainer, BorderLayout.SOUTH);
		viewerContainer.add(canvas, BorderLayout.CENTER);
		
		getContentPane().add(viewerContainer, BorderLayout.CENTER);
		
		
		Container controlContainer = new Container();
		controlContainer.setPreferredSize(new Dimension(400, 800));
		controlContainer.setLayout(new GridBagLayout());
		getContentPane().add(controlContainer, BorderLayout.WEST);
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		cellWidthLabel = new JLabel("Cavas Cell Width");
		controlContainer.add(cellWidthLabel, c);
		
		c.gridx =1;
		this.cellWidthField = new JTextField(4);
		this.cellWidthField.setText("" + maxX);
		controlContainer.add(this.cellWidthField, c);
		
		c.gridx = 0;
		c.gridy = 1;
		cellHeightLabel = new JLabel("Cavas Cell Height");
		controlContainer.add(cellHeightLabel, c);
		
		c.gridx =1;
		this.cellHeightField = new JTextField(4);
		this.cellHeightField.setText("" + maxY);
		controlContainer.add(this.cellHeightField, c);
		
		JButton setDimButton = new JButton("Set");
		setDimButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				resetVisualizerCellDim();
			}
		});
		c.gridx = 1;
		c.gridy = 2;
		controlContainer.add(setDimButton, c);
		
		
		JButton newStateButton = new JButton("New State");
		newStateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				resetState(true);
			}
		});
		
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.insets = new Insets(10, 0, 0, 0);
		controlContainer.add(newStateButton, c);
		
		JButton classicStateButton = new JButton("Classic State");
		classicStateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				resetState(false);
			}
		});
		c.gridy = 4;
		c.insets = new Insets(0, 0, 0, 0);
		controlContainer.add(classicStateButton, c);
		
		
		this.commandArea = new JTextArea(4, 25);
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 3;
		c.insets = new Insets(100, 0, 0, 0);
		controlContainer.add(this.commandArea, c);
		
		giveCommandButton = new JButton("Give Command");
		giveCommandButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				giveCommand();
			}
		});
		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 1;
		c.insets = new Insets(0, 0, 0, 0);
		controlContainer.add(giveCommandButton, c);
		
		
		finishTrainingButton = new JButton("Finish Training");
		finishTrainingButton.setEnabled(false);
		finishTrainingButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				finishTraining();
			}
		});
		c.gridx = 1;
		controlContainer.add(finishTrainingButton,c);
		
		
		rewardButton = new JButton("Reward");
		rewardButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				reward();
			}
		});
		rewardButton.setEnabled(false);
		c.gridx = 0;
		c.gridy = 7;
		c.insets = new Insets(20, 0, 0, 0);
		controlContainer.add(rewardButton,c);
		
		
		punishButton = new JButton("Punish");
		punishButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				punish();
			}
		});
		punishButton.setEnabled(false);
		c.gridx = 1;
		controlContainer.add(punishButton,c);
		
		
		this.hallucinateButton = new JCheckBox("Hallucinate");
		this.hallucinateButton.setSelected(true);
		this.hallucinateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!hallucinateButton.isSelected()){
					hLayer.updateState(null);
					lastMostLikelyTask = null;
					updateGUI();
				}
				
			}
		});
		c.gridx = 0;
		c.gridy = 8;
		c.insets = new Insets(50, 0, 0, 0);
		controlContainer.add(this.hallucinateButton, c);
		
		
		
		JLabel cheatSheet = new JLabel("<html>" +
									   "<b>Cheat Sheet</b><br/>" +
									   "c: color<br/>" +
									   "s: shape<br/>" +
									   "b: add block<br/>" +
									   "d: add door<br/>" +
									   "a: place agent<br/>" +
									   "x: delete" +
									   "</html>");
		c.gridx = 0;
		c.gridy = 9;
		c.gridwidth = 2;
		c.insets = new Insets(100, 0, 0, 0);
		controlContainer.add(cheatSheet, c);
		
		
		this.canvas.addMouseMotionListener(this);
		this.canvas.addMouseListener(this);
		
		this.addKeyListener(this);
		this.canvas.addKeyListener(this);
		
		this.updatePropTextArea(curState);
		
		
	}
	
	
	private void updatePropTextArea(State s){
		
		StringBuffer buf = new StringBuffer();
		
		List <PropositionalFunction> props = domain.getPropFunctions();
		for(PropositionalFunction pf : props){
			List<GroundedProp> gps = s.getAllGroundedPropsFor(pf);
			for(GroundedProp gp : gps){
				if(gp.isTrue(s)){
					buf.append(gp.toString()).append("\n");
				}
			}
		}
		propViewer.setText(buf.toString());
		
		
	}
	
	
	public void launch(){
		this.pack();
		this.setVisible(true);
	}
	
	public void updateGUI(){
		this.canvas.repaint();
		this.needsRepaint = false;
		this.updatePropTextArea(this.curState);
	}
	
	protected void resetVisualizerCellDim(){
		System.out.println("tmp");
		
		this.maxX = Integer.parseInt(this.cellWidthField.getText());
		this.maxY = Integer.parseInt(this.cellHeightField.getText());
		
		this.canvas.removeRenderLayer(0);
		sLayer = Sokoban2Visualizer.getStateRenderLayer(maxX, maxY, "robotImages");
		canvas.insertRenderLayerTo(0, sLayer);
		sLayer.updateState(curState);
		
		this.cLayer.setDim(maxX, maxY);
		
		StateRenderLayer abSL = new StateRenderLayer();
		abSL.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new Sokoban2Visualizer.AgentPainterWithImages("robotImages", maxX, maxY));
		abSL.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new Sokoban2Visualizer.BlockPainter(maxX, maxY, "robotImages"));
		this.hLayer.setSrcStateRenderLayer(abSL);
		
		this.needsRepaint = true;
		this.updateGUI();
	}
	
	protected void resetState(boolean emptyState){
		if(emptyState){
			this.curState = new State();
		}
		else{
			this.curState = Sokoban2Domain.getClassicState(this.domain);
		}
		
		this.sLayer.updateState(curState);
		this.needsRepaint = true;
		this.updateGUI();
	}
	
	
	protected void giveCommand(){
		
		this.giveCommandButton.setEnabled(false);
		this.finishTrainingButton.setEnabled(true);
		this.rewardButton.setEnabled(true);
		this.punishButton.setEnabled(true);
		
		this.cLayer.setHidden(true);
		
		this.initialState = curState;
		
		this.commandInterface.setRFDistribution(initialState, this.commandArea.getText());
		this.env.setCurStateTo(curState);
		
		this.agentThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				agent.runLearningEpisodeFrom(curState);
			}
		});
		
		this.agentThread.start();
		
		
		/*
		List<TaskDescription> tasks = new ArrayList<TaskDescription>();
		tasks.add(new SemanticTaskDescription(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"agent0", "room0"})));
		tasks.add(new SemanticTaskDescription(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"agent0", "room1"})));
		tasks.add(new SemanticTaskDescription(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"agent0", "room2"})));
		
		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();
		
		//this.agent = new TaskInductionTraining(domainEnvWrapper, trainerRF, trainerTF, hashingFactory, 
			//	tasks, new MAPMixtureModelPolicy());
		this.agent.setNoopAction(noopAction);
		for(int i = 0; i < tasks.size(); i++){
			this.agent.setProbFor(i, 1./(double)tasks.size());
		}
		
		this.agent.useSeperatePlanningDomain(domain);
		this.agent.planPossibleTasksFromSeedState(initialState);
		
		this.env.setCurStateTo(curState);
		
		this.agentThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				agent.runLearningEpisodeFrom(curState);
			}
		});
		
		this.agentThread.start();
		*/
		
	}
	
	protected void finishTraining(){
		
		this.giveCommandButton.setEnabled(true);
		this.finishTrainingButton.setEnabled(false);
		this.rewardButton.setEnabled(false);
		this.punishButton.setEnabled(false);
		
		this.cLayer.setHidden(false);
		
		this.curState = initialState;
		this.sLayer.updateState(curState);
		
		this.env.setTerminal();
		
		System.out.println("Waiting...");
		try {
			this.agentThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Finished waiting.");
		
		this.hLayer.updateState(null);
		
		this.lastMostLikelyTask = null;
		
		this.commandInterface.addLastTrainingResultToDatasetAndRetrain(this.initialState, this.commandArea.getText(), 5e-5);
		
		this.needsRepaint = true;
		this.updateGUI();
		
	}
	
	protected void reward(){
		this.env.setReward(1.);
	}
	
	protected void punish(){
		this.env.setReward(-1.);
	}
	
	
	protected void roomDrag(MouseEvent e){
		
		int draggedCellX = this.getCellXForMouse(e.getX());
		int draggedCellY = this.getCellYForMouse(e.getY());
		
		if(draggedCellX != this.lastCellX || draggedCellY != this.lastCellY){
			
			if(this.currentlyCreatedRoom == null){
				int nroomNum = this.nextRoomObNameNumber(curState);
				this.currentlyCreatedRoom = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSROOM), Sokoban2Domain.CLASSROOM+nroomNum);
				this.curState.addObject(currentlyCreatedRoom);
				this.freezeSelection = true;
			}
			int minx = Math.min(draggedCellX, this.mouseDownStartCellX);
			int maxx = Math.max(draggedCellX, this.mouseDownStartCellX);
			int miny = Math.min(draggedCellY, this.mouseDownStartCellY);
			int maxy = Math.max(draggedCellY, this.mouseDownStartCellY);
			
			Sokoban2Domain.setRoom(currentlyCreatedRoom, maxy, minx, miny, maxx, "red");
			this.needsRepaint = true;

			this.updateCursor(e.getX(), e.getY());
			
		}
		
	}
	
	protected void objectDrag(MouseEvent e){
		
		if(this.selectedObject.getTrueClassName().equals(Sokoban2Domain.CLASSAGENT) || this.selectedObject.getTrueClassName().equals(Sokoban2Domain.CLASSBLOCK)){
			
			int draggedCellX = this.getCellXForMouse(e.getX());
			int draggedCellY = this.getCellYForMouse(e.getY());
			
			if(draggedCellX != this.lastCellX || draggedCellY != this.lastCellY){
			
				this.selectedObject.setValue(Sokoban2Domain.ATTX, draggedCellX);
				this.selectedObject.setValue(Sokoban2Domain.ATTY, draggedCellY);
			
				this.updateCursor(e.getX(), e.getY());
				
				this.needsRepaint = true;
				
			}
			
			
			
		}
		
		
		
	}
	
	
	protected void addBlock(){
		int nBlock = this.nextObNameNumber(this.curState.getObjectsOfTrueClass(Sokoban2Domain.CLASSBLOCK));
		ObjectInstance b = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSBLOCK), Sokoban2Domain.CLASSBLOCK+nBlock);
		Sokoban2Domain.setBlock(b, this.lastCellX, this.lastCellY, Sokoban2Domain.SHAPES[0], Sokoban2Domain.COLORS[0]);
		this.curState.addObject(b);
		this.selectedObject = b;
		this.needsRepaint = true;
	}
	
	protected void addOrSetAgent(){
		
		List<ObjectInstance> agents = this.curState.getObjectsOfTrueClass(Sokoban2Domain.CLASSAGENT);
		ObjectInstance a = null;
		if(agents.size() == 0){
			a = new ObjectInstance(domain.getObjectClass(Sokoban2Domain.CLASSAGENT), Sokoban2Domain.CLASSAGENT + 0);
			this.curState.addObject(a);
		}
		
		Sokoban2Domain.setAgent(this.curState, this.lastCellX, this.lastCellY);
		this.needsRepaint = true;
		
	}
	
	protected void addDoor(){
		
		//first we need to make sure that what we're selected on is a room
		if(this.selectedObject == null){
			return ;
		}
		if(!this.selectedObject.getTrueClassName().equals(Sokoban2Domain.CLASSROOM)){
			return ;
		}
		
		//make sure we're on the wall of a room
		int top = this.selectedObject.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
		int left = this.selectedObject.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
		int bottom = this.selectedObject.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
		int right = this.selectedObject.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);
		
		if(this.lastCellX == left || this.lastCellX == right || this.lastCellY == top || this.lastCellY == bottom){
			
			//are we adjacent to existing doors?
			List <ObjectInstance> adjacentDoors = this.getAdjacentDoorsToCell(this.lastCellX, this.lastCellY);
			if(adjacentDoors.size() > 0){
				
				int minX = this.lastCellX;
				int maxX = minX;
				int minY = this.lastCellY;
				int maxY = minY;
				
				for(ObjectInstance d : adjacentDoors){
					int dtop = d.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
					int dleft = d.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
					int dbottom = d.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
					int dright = d.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);
					
					minX = Math.min(minX, dleft);
					maxX = Math.max(maxX, dright);
					minY = Math.min(minY, dbottom);
					maxY = Math.max(maxY, dtop);
					
				}
				
				ObjectInstance d = adjacentDoors.get(0);
				Sokoban2Domain.setRegion(d, maxY, minX, minY, maxX);
				for(int i = 1; i < adjacentDoors.size(); i++){
					//remove rest
					this.curState.removeObject(adjacentDoors.get(i));
				}
				
			}
			else{
				
				//otherwise make a new door object
				int ndoor = this.nextObNameNumber(this.curState.getObjectsOfTrueClass(Sokoban2Domain.CLASSDOOR));
				ObjectInstance d = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSDOOR), Sokoban2Domain.CLASSDOOR+ndoor);
				Sokoban2Domain.setRegion(d, this.lastCellY, this.lastCellX, this.lastCellY, this.lastCellX);
				this.curState.addObject(d);
				
			}
			
			this.needsRepaint = true;
			
		}
		
	}
	
	protected void changeColor(){
		
		if(this.selectedObject == null){
			return;
		}
		
		if(!this.selectedObject.getTrueClassName().equals(Sokoban2Domain.CLASSROOM) && !this.selectedObject.getTrueClassName().equals(Sokoban2Domain.CLASSBLOCK)){
			return;
		}
		
		String curCol = this.selectedObject.getStringValForAttribute(Sokoban2Domain.ATTCOLOR);
		int curIndex;
		for(curIndex = 0; curIndex < Sokoban2Domain.COLORS.length; curIndex++){
			if(Sokoban2Domain.COLORS[curIndex].equals(curCol)){
				break;
			}
		}
		
		int nIndex = (curIndex+1) % Sokoban2Domain.COLORS.length;
		String nCol = Sokoban2Domain.COLORS[nIndex];
		
		this.selectedObject.setValue(Sokoban2Domain.ATTCOLOR, nCol);
		
		this.needsRepaint = true;
		
	}
	
	protected void changeShape(){
		
		if(this.selectedObject == null){
			return;
		}
		
		if(!this.selectedObject.getTrueClassName().equals(Sokoban2Domain.CLASSBLOCK)){
			return;
		}
		
		String curShape = this.selectedObject.getStringValForAttribute(Sokoban2Domain.ATTSHAPE);
		int curIndex;
		for(curIndex = 0; curIndex < Sokoban2Domain.SHAPES.length; curIndex++){
			if(Sokoban2Domain.SHAPES[curIndex].equals(curShape)){
				break;
			}
		}
		
		int nIndex = (curIndex+1) % Sokoban2Domain.SHAPES.length;
		String nShape = Sokoban2Domain.SHAPES[nIndex];
		
		this.selectedObject.setValue(Sokoban2Domain.ATTSHAPE, nShape);
		
		this.needsRepaint = true;
		
	}
	
	protected void delete(){
		
		if(this.selectedObject == null){
			return;
		}
		
		this.curState.removeObject(this.selectedObject);
		this.selectedObject = null;
		this.needsRepaint = true;
		
	}
	
	protected List<ObjectInstance> getAdjacentDoorsToCell(int x, int y){
		List <ObjectInstance> doors = this.curState.getObjectsOfTrueClass(Sokoban2Domain.CLASSDOOR);
		List <ObjectInstance> res = new ArrayList<ObjectInstance>(doors.size());
		
		for(ObjectInstance d : doors){
			if(Sokoban2Domain.regionContainsPoint(d, x-1, y, true) || Sokoban2Domain.regionContainsPoint(d, x+1, y, true) || 
					Sokoban2Domain.regionContainsPoint(d, x, y-1, true) || Sokoban2Domain.regionContainsPoint(d, x, y+1, true)){
				
				res.add(d);
				
			}
		}
		
		return res;
	}
	
	
	protected void setSelectedObject(int cellX, int cellY){
		
		if(freezeSelection){
			return;
		}
		
		//first check agent
		List<ObjectInstance> agents = this.curState.getObjectsOfTrueClass(Sokoban2Domain.CLASSAGENT);
		for(ObjectInstance o : agents){
			if(o.getDiscValForAttribute(Sokoban2Domain.ATTX) == cellX && o.getDiscValForAttribute(Sokoban2Domain.ATTY) == cellY){
				this.selectedObject = o;
				return;
			}
		}
		
		//then check blocks
		List <ObjectInstance> blocks = this.curState.getObjectsOfTrueClass(Sokoban2Domain.CLASSBLOCK);
		for(ObjectInstance o : blocks){
			if(o.getDiscValForAttribute(Sokoban2Domain.ATTX) == cellX && o.getDiscValForAttribute(Sokoban2Domain.ATTY) == cellY){
				this.selectedObject = o;
				return;
			}
		}
		
		
		//then check doors
		this.selectedObject = Sokoban2Domain.doorContainingPoint(this.curState, cellX, cellY);
		if(this.selectedObject != null){
			return;
		}
		
		//then check doors
		this.selectedObject = Sokoban2Domain.roomContainingPointIncludingBorder(this.curState, cellX, cellY);
		
	}
	
	
	
	protected void updateCursor(int mx, int my){
		int mcx = this.getCellXForMouse(mx);
		int mcy = this.getCellYForMouse(my);
		
		int oldCX = this.cLayer.getCellX();
		int oldCY = this.cLayer.getCellY();
		
		if(mcx != oldCX || mcy != oldCY){
			this.cLayer.updateCell(mcx, mcy);
			this.lastCellX = mcx;
			this.lastCellY = mcy;
			this.setSelectedObject(mcx, mcy);
			this.needsRepaint = true;
		}
		
	}
	
	protected int getCellXForMouse(int mx){
		float cellWidth = this.canvas.getWidth() / maxX;
		int mcx = (int)(mx / cellWidth);
		return mcx;
	}
	
	protected int getCellYForMouse(int my){
		float cellHeight = this.canvas.getHeight() / maxY;
		int mcy = (int)((this.canvas.getHeight() - my) / cellHeight);
		return mcy;
	}
	
	
	protected int nextRoomObNameNumber(State s){
		return this.nextObNameNumber(s.getObjectsOfTrueClass(Sokoban2Domain.CLASSROOM));
	}
	
	protected int nextObNameNumber(List<ObjectInstance> obs){
		
		int mx = -1;
		if(obs.size() == 0){
			return 0;
		}
		
		String cname = obs.get(0).getObjectClass().name;
		for(ObjectInstance o : obs){
			int num = Integer.parseInt(o.getName().substring(cname.length()));
			if(num > mx){
				mx = num;
			}
		}
		
		return mx+1;
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		
		if(this.selectedObject == null){
			this.roomDrag(e);
		}
		else{
			this.objectDrag(e);
		}
		
		
		if(this.needsRepaint){
			this.updateGUI();
		}
		
	}



	@Override
	public void mouseMoved(MouseEvent e) {

		int mx = e.getX();
		int my = e.getY();
		this.updateCursor(mx, my);
		
		if(this.needsRepaint){
			this.updateGUI();
		}
		
	}
	

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void mousePressed(MouseEvent e) {
		
		this.mouseDownStartCellX = this.getCellXForMouse(e.getX());
		this.mouseDownStartCellY = this.getCellYForMouse(e.getY());
		
		currentlyCreatedRoom = null;
		
	}



	@Override
	public void mouseReleased(MouseEvent e) {
		this.freezeSelection = false;
	}



	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void keyReleased(KeyEvent e) {
		
		char key = e.getKeyChar();
		
		if(key == 'b'){
			this.addBlock();
		}
		else if(key == 'a'){
			this.addOrSetAgent();
		}
		else if(key == 'd'){
			this.addDoor();
		}
		else if(key == 'c'){
			this.changeColor();
		}
		else if(key == 's'){
			this.changeShape();
		}
		else if(key == 'x'){
			this.delete();
		}
		
		if(this.needsRepaint){
			this.updateGUI();
		}
		
	}
	
	
	@Override
	public void setRenderState(State s) {
		this.curState = s;
		this.sLayer.updateState(curState);
		
		TaskProb tp = this.agent.getPosteriors().getMostLikelyTask();
		if(tp.getTask() != this.lastMostLikelyTask && this.hallucinateButton.isSelected()){
			EpisodeAnalysis ea = tp.getPolicy().evaluateBehavior(s, tp.getRf(), tp.getTf());
			State endState = ea.getState(ea.numTimeSteps()-1);
			this.hLayer.updateState(endState);
			this.lastMostLikelyTask = tp.getTask();
		}
		
		this.updateGUI();
	}
	
	public static void main(String [] args){
		SokoDynamicStateGUI gui = new SokoDynamicStateGUI();
		
		gui.initGUI();
		gui.launch();
	}



	
	
}
