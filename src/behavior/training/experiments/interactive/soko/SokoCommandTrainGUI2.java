package behavior.training.experiments.interactive.soko;

import auxiliary.StateGUIActionObserver;
import auxiliary.StateVisualizingGUI;
import behavior.training.taskinduction.TaskDescription;
import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.commands.CommandsTrainingInterface;
import behavior.training.taskinduction.commands.version2.CommandsTrainingInterface2;
import behavior.training.taskinduction.strataware.FeedbackStrategy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.oomdp.core.*;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.visualizer.MultiLayerRenderer;
import burlap.oomdp.visualizer.StateRenderLayer;
import commands.model3.GPConjunction;
import commands.model3.mt.Tokenizer;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Visualizer;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.util.*;

/**
 * @author James MacGlashan.
 */
public class SokoCommandTrainGUI2 extends JFrame implements StateVisualizingGUI,MouseMotionListener,MouseListener,KeyListener {

	private static final long serialVersionUID = 1L;

	protected CommandsTrainingInterface2 cti;

	protected Domain domain;
	protected DiscreteMaskHashingFactory hashingFactory;

	protected int									maxX = 20;
	protected int									maxY = 20;

	protected State curState;
	protected State									initialState;

	protected MultiLayerRenderer canvas;
	protected StateRenderLayer sLayer;
	protected CursorHighlightLayer					cLayer;
	protected HallucinateStateRenderLayer			hLayer;

	protected JLabel								cellWidthLabel;
	protected JLabel								cellHeightLabel;
	protected JTextField							cellWidthField;
	protected JTextField							cellHeightField;

	protected JTextArea								commandArea;

	protected JButton								giveCommandButton;
	protected JButton								finishTrainingButton;
	protected JButton								finishWithoutLearningButton;
	protected JButton								rewardButton;
	protected JButton								punishButton;

	protected JCheckBox								hallucinateButton;

	protected int									lastCellX;
	protected int									lastCellY;

	protected int									mouseDownStartCellX;
	protected int									mouseDownStartCellY;

	protected ObjectInstance currentlyCreatedRoom = null;

	protected ObjectInstance						selectedObject = null;
	protected boolean								freezeSelection = false;


	protected boolean								needsRepaint = false;


	protected TextArea propViewer;


	protected TaskDescription lastMostLikelyTask = null;


	public SokoCommandTrainGUI2(){
		canvas = new MultiLayerRenderer();
		sLayer = Sokoban2Visualizer.getStateRenderLayer(maxX, maxY, "oomdpResearch/robotImages");
		canvas.addRenderLayer(sLayer);

		cLayer = new CursorHighlightLayer(maxX, maxY);
		canvas.addRenderLayer(cLayer);

		Sokoban2Domain dgen = new Sokoban2Domain();
		dgen.includeDirectionAttribute(true);
		dgen.includePullAction(true);

		StateRenderLayer abSL = new StateRenderLayer();
		abSL.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new Sokoban2Visualizer.AgentPainterWithImages("oomdpResearch/robotImages", maxX, maxY));
		abSL.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new Sokoban2Visualizer.BlockPainter(maxX, maxY, "oomdpResearch/robotImages"));
		hLayer = new HallucinateStateRenderLayer(abSL);
		hLayer.setOpacity(0.5f);
		canvas.addRenderLayer(hLayer);

		this.cti = new CommandsTrainingInterface2(dgen);

		this.domain = this.cti.getOperatingDomain();
		this.cti.addActionObserverToOperatingDomain(new StateGUIActionObserver(this));

		this.hashingFactory = new DiscreteMaskHashingFactory();
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSAGENT, this.domain.getAttribute(Sokoban2Domain.ATTX));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSAGENT, this.domain.getAttribute(Sokoban2Domain.ATTY));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTX));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTY));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTCOLOR));
		this.hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, this.domain.getAttribute(Sokoban2Domain.ATTSHAPE));

		java.util.List<FeedbackStrategy> feedbackStrategies = new ArrayList<FeedbackStrategy>();


		/*FeedbackStrategy balanced = new FeedbackStrategy(0.5, 0.5, 0.1);
		FeedbackStrategy RPlusPMinus = new FeedbackStrategy(0.1, 0.6, 0.1);
		FeedbackStrategy RMinusPPlus = new FeedbackStrategy(0.6, 0.1, 0.1);*/


		FeedbackStrategy balanced = new FeedbackStrategy(0.75, 0.75, 0.1);
		FeedbackStrategy RPlusPMinus = new FeedbackStrategy(0.75, 0.85, 0.1);
		FeedbackStrategy RMinusPPlus = new FeedbackStrategy(0.85, 0.75, 0.1);


		/*
		FeedbackStrategy balanced = new FeedbackStrategy(0.75, 0.75, 0.1);
		FeedbackStrategy RPlusPMinus = new FeedbackStrategy(0.6, 0.95, 0.1);
		FeedbackStrategy RMinusPPlus = new FeedbackStrategy(0.6, 0.95, 0.1);
		*/

		balanced.setProbOfStrategy(0.32);
		RPlusPMinus.setProbOfStrategy(0.36);
		RMinusPPlus.setProbOfStrategy(0.32);


		/*
		balanced.setProbOfStrategy(1./3.);
		RPlusPMinus.setProbOfStrategy(1./3.);
		RMinusPPlus.setProbOfStrategy(1./3.);
		*/

		balanced.setName("balanced");
		RPlusPMinus.setName("R+/P-");
		RMinusPPlus.setName("R-/P+");


		feedbackStrategies.add(balanced);
		//feedbackStrategies.add(RPlusPMinus);
		//feedbackStrategies.add(RMinusPPlus);

		//feedbackStrategies.add(new FeedbackStrategy(0.5, 0.5, 0.1));
		//feedbackStrategies.add(new FeedbackStrategy(0.1, 0.6, 0.1)); //R+/P-
		//feedbackStrategies.add(new FeedbackStrategy(0.6, 0.1, 0.1));

		//feedbackStrategies.add(new FeedbackStrategy(0.7, 0.05, 0.1));


		this.cti.intantiateDefaultAgent(hashingFactory, feedbackStrategies);



		this.cti.setAlwaysResetPriorsWithCommand(false);
		this.cti.setRemoveRPPMWhenTrueSatisfied(true);
		this.cti.addTrueGoal("go to the green room", "agentInRoom agent0 room1");
		this.cti.addTrueGoal("go to the blue room", "agentInRoom agent0 room2");

		java.util.List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);

		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);

		Tokenizer tokenizer = new Tokenizer(true, true);

		this.cti.instatiateCommandsLearning(hashingFactory, tokenizer, liftedTaskDescriptions, 22);

		this.curState = Sokoban2Domain.getClassicState(this.domain);
		sLayer.updateState(this.curState);
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
		c.gridy = 7;
		controlContainer.add(finishTrainingButton,c);


		finishWithoutLearningButton = new JButton("Finish Without Learning");
		finishWithoutLearningButton.setEnabled(false);
		finishWithoutLearningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				finishWithoutLearning();
			}
		});
		c.gridy = 6;
		controlContainer.add(finishWithoutLearningButton,c);



		rewardButton = new JButton("Reward");
		rewardButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				reward();
			}
		});
		rewardButton.setEnabled(false);
		c.gridx = 0;
		c.gridy = 8;
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



		JButton recordDataButton = new JButton("Record Data");
		recordDataButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SokoCommandTrainGUI2.this.cti.writeAllEpisodesToFiles("oomdpResearch/sokoTrainData");
			}
		});

		c.gridx = 0;
		c.gridy = 9;
		c.insets = new Insets(20, 0, 0, 0);
		controlContainer.add(recordDataButton, c);


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
		c.gridy = 10;
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
		c.gridy = 11;
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

		java.util.List<PropositionalFunction> props = domain.getPropFunctions();
		for(PropositionalFunction pf : props){
			java.util.List<GroundedProp> gps = pf.getAllGroundedPropsForState(s);
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
			this.curState = new MutableState();
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
		this.finishWithoutLearningButton.setEnabled(true);
		this.rewardButton.setEnabled(true);
		this.punishButton.setEnabled(true);

		this.cLayer.setHidden(true);

		this.initialState = curState;

		this.cti.giveCommandInInitialState(this.initialState, this.commandArea.getText());

	}

	protected void finishTraining(){

		this.giveCommandButton.setEnabled(true);
		this.finishTrainingButton.setEnabled(false);
		this.finishWithoutLearningButton.setEnabled(false);
		this.rewardButton.setEnabled(false);
		this.punishButton.setEnabled(false);

		this.cLayer.setHidden(false);

		this.curState = initialState;
		this.sLayer.updateState(curState);

		this.cti.giveTerminateAndLearnSignal();

		this.hLayer.updateState(null);
		lastMostLikelyTask = null;

		this.needsRepaint = true;
		this.updateGUI();

	}

	protected void finishWithoutLearning(){

		this.giveCommandButton.setEnabled(true);
		this.finishTrainingButton.setEnabled(false);
		this.finishWithoutLearningButton.setEnabled(false);
		this.rewardButton.setEnabled(false);
		this.punishButton.setEnabled(false);

		this.cLayer.setHidden(false);

		this.curState = initialState;
		this.sLayer.updateState(curState);

		this.cti.giveTerminateSignal();

		this.hLayer.updateState(null);
		lastMostLikelyTask = null;

		this.needsRepaint = true;
		this.updateGUI();

	}

	protected void reward(){
		this.cti.giveReward();
	}

	protected void punish(){
		this.cti.givePunishment();
	}

	protected void roomDrag(MouseEvent e){

		int draggedCellX = this.getCellXForMouse(e.getX());
		int draggedCellY = this.getCellYForMouse(e.getY());

		if(draggedCellX != this.lastCellX || draggedCellY != this.lastCellY){

			if(this.currentlyCreatedRoom == null){
				int nroomNum = this.nextRoomObNameNumber(curState);
				this.currentlyCreatedRoom = new MutableObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSROOM), Sokoban2Domain.CLASSROOM+nroomNum);
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
		int nBlock = this.nextObNameNumber(this.curState.getObjectsOfClass(Sokoban2Domain.CLASSBLOCK));
		ObjectInstance b = new MutableObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSBLOCK), Sokoban2Domain.CLASSBLOCK+nBlock);
		Sokoban2Domain.setBlock(b, this.lastCellX, this.lastCellY, Sokoban2Domain.SHAPES[0], Sokoban2Domain.COLORS[0]);
		this.curState.addObject(b);
		this.selectedObject = b;
		this.needsRepaint = true;
	}

	protected void addOrSetAgent(){

		java.util.List<ObjectInstance> agents = this.curState.getObjectsOfClass(Sokoban2Domain.CLASSAGENT);
		ObjectInstance a = null;
		if(agents.size() == 0){
			a = new MutableObjectInstance(domain.getObjectClass(Sokoban2Domain.CLASSAGENT), Sokoban2Domain.CLASSAGENT + 0);
			this.curState.addObject(a);
			Sokoban2Domain.setAgent(this.curState, this.lastCellX, this.lastCellY, 1);
		}
		else{
			Sokoban2Domain.setAgent(this.curState, this.lastCellX, this.lastCellY);
		}


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
		int top = this.selectedObject.getIntValForAttribute(Sokoban2Domain.ATTTOP);
		int left = this.selectedObject.getIntValForAttribute(Sokoban2Domain.ATTLEFT);
		int bottom = this.selectedObject.getIntValForAttribute(Sokoban2Domain.ATTBOTTOM);
		int right = this.selectedObject.getIntValForAttribute(Sokoban2Domain.ATTRIGHT);

		if(this.lastCellX == left || this.lastCellX == right || this.lastCellY == top || this.lastCellY == bottom){

			//are we adjacent to existing doors?
			java.util.List<ObjectInstance> adjacentDoors = this.getAdjacentDoorsToCell(this.lastCellX, this.lastCellY);
			if(adjacentDoors.size() > 0){

				int minX = this.lastCellX;
				int maxX = minX;
				int minY = this.lastCellY;
				int maxY = minY;

				for(ObjectInstance d : adjacentDoors){
					int dtop = d.getIntValForAttribute(Sokoban2Domain.ATTTOP);
					int dleft = d.getIntValForAttribute(Sokoban2Domain.ATTLEFT);
					int dbottom = d.getIntValForAttribute(Sokoban2Domain.ATTBOTTOM);
					int dright = d.getIntValForAttribute(Sokoban2Domain.ATTRIGHT);

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
				int ndoor = this.nextObNameNumber(this.curState.getObjectsOfClass(Sokoban2Domain.CLASSDOOR));
				ObjectInstance d = new MutableObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSDOOR), Sokoban2Domain.CLASSDOOR+ndoor);
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

	protected java.util.List<ObjectInstance> getAdjacentDoorsToCell(int x, int y){
		java.util.List<ObjectInstance> doors = this.curState.getObjectsOfClass(Sokoban2Domain.CLASSDOOR);
		java.util.List<ObjectInstance> res = new ArrayList<ObjectInstance>(doors.size());

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
		java.util.List<ObjectInstance> agents = this.curState.getObjectsOfClass(Sokoban2Domain.CLASSAGENT);
		for(ObjectInstance o : agents){
			if(o.getIntValForAttribute(Sokoban2Domain.ATTX) == cellX && o.getIntValForAttribute(Sokoban2Domain.ATTY) == cellY){
				this.selectedObject = o;
				return;
			}
		}

		//then check blocks
		java.util.List<ObjectInstance> blocks = this.curState.getObjectsOfClass(Sokoban2Domain.CLASSBLOCK);
		for(ObjectInstance o : blocks){
			if(o.getIntValForAttribute(Sokoban2Domain.ATTX) == cellX && o.getIntValForAttribute(Sokoban2Domain.ATTY) == cellY){
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
		return this.nextObNameNumber(s.getObjectsOfClass(Sokoban2Domain.CLASSROOM));
	}

	protected int nextObNameNumber(java.util.List<ObjectInstance> obs){

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

		TaskProb tp = this.cti.getMostLikelyTask();
		if(tp.getTask() != this.lastMostLikelyTask && this.hallucinateButton.isSelected()){
			EpisodeAnalysis ea = tp.getPolicy().evaluateBehavior(s, tp.getRf(), tp.getTf());
			State endState = ea.getState(ea.numTimeSteps()-1);
			this.hLayer.updateState(endState);
			this.lastMostLikelyTask = tp.getTask();
		}

		this.updateGUI();
	}


	public static void main(String [] args){
		SokoCommandTrainGUI2 gui = new SokoCommandTrainGUI2();
		gui.initGUI();
		gui.launch();
	}


}
