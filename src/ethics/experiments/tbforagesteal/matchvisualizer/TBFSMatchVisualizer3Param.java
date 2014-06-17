package ethics.experiments.tbforagesteal.matchvisualizer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.WorldGenerator;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import burlap.oomdp.stochasticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.TBFSAlternatingTurnSG;
import domain.stocasticgames.foragesteal.TBFSStandardMechanics;
import domain.stocasticgames.foragesteal.TBFSStandardReward;
import domain.stocasticgames.foragesteal.TBFSWhoStartedMechanics;
import domain.stocasticgames.foragesteal.TBForageSteal;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import ethics.experiments.tbforagesteal.auxiliary.TBFSSubjectiveRF;
import ethics.experiments.tbforagesteal.auxiliary.TBFSSubjectiveRFWS;
import ethics.experiments.tbforagesteal.matchvisualizer.QTableVisController.StateActionSetTuple;

public class TBFSMatchVisualizer3Param extends JFrame {


	QTableVisController							qTableController;
	
	JLabel										lrLabel;
	JTextField									lrField;
	
	JLabel										agent0Label;
	JLabel										agent1Label;
	JLabel										sLabel;
	JLabel										psLabel;
	JLabel										ppLabel;
	
	JTextField									a0SField;
	JTextField									a0PSField;
	JTextField									a0PPField;
	
	JTextField									a1SField;
	JTextField									a1PSField;
	JTextField									a1PPField;
	
	JTextField									csvFileField;
	JButton										saveToFileButton;
	
	JButton										playMatchButton;
	JButton										computeAverageButton;
	
	JSlider										stageSlider;
	JLabel										stageLabel;
	JButton										incStage;
	JButton										decStage;
	
	JLabel										a0ActionLabel;
	JLabel										a1ActionLabel;
	
	JLabel										performLabel;
	JLabel										a0CumulativeReturn;
	JLabel										a1CumulativeReturn;
	
	JLabel										avgCumLabel;
	JLabel										a0AvgCumulativeReturn;
	JLabel										a1AvgCumulativeReturn;
	
	int											stage;
	int											maxStage;
	
	//data related components
	AgentFactory								baseAgentFactory;
	SGDomain									domain;
	DiscreteStateHashFactory					hashingFactory;
	double										discount = 0.99;
	double										learningRate;
	JointReward									objectiveRF;
	WorldGenerator								worldGenerator;
	AgentType									at;
	MatchAnalizer								ma = null;
	String										agent0Name;
	String										agent1Name;
	
	List <StateActionSetTuple> 					agent0QueryStates;
	List <StateActionSetTuple> 					agent1QueryStates;
	
	double []									oldA0Params;
	double []									oldA1Params;
	
	
	int											gameVariant = 2;
	
	
	private static final long serialVersionUID = 1L;

	public TBFSMatchVisualizer3Param()  {
		stage = 0;
		maxStage = 1000;
		
		TBForageSteal gen = new TBForageSteal();
		domain = (SGDomain) gen.generateDomain();
		
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		objectiveRF = new TBFSStandardReward();
		
		JointActionModel jam;
		if(this.gameVariant == 1){
			jam = new TBFSStandardMechanics();
		}
		else{
			jam = new TBFSWhoStartedMechanics();
		}
		
		worldGenerator = new ConstantWorldGenerator(domain, jam, objectiveRF, new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG(domain));
		
		
		
		at = new AgentType("agent", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		

		this.initGUI();
		
		
		
	}
	
	
	public void initGUI(){
		
		qTableController = new QTableVisController(1300, 650, objectiveRF);
		
		Container viewingArea = new Container();
		viewingArea.setPreferredSize(new Dimension(1300, 770));
		viewingArea.setLayout(new BorderLayout());
		
		Container viewControlArea = new Container();
		viewControlArea.setPreferredSize(new Dimension(1300, 120));
		
		viewingArea.add(viewControlArea, BorderLayout.NORTH);
		viewingArea.add(qTableController.getRenderer(), BorderLayout.CENTER);
		
		getContentPane().add(viewingArea, BorderLayout.CENTER);
		
		Container paramContainer = new Container();
		paramContainer.setPreferredSize(new Dimension(300, 770));
		getContentPane().add(paramContainer, BorderLayout.WEST);
		
		paramContainer.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		//c.ipadx = 20;
		//c.ipady = 10;
		
		int fieldCols = 5;
		
		c.insets = new Insets(0, 0, 20, 0);
		
		lrLabel = new JLabel("Learning Rate");
		c.gridx = 0;
		c.gridy = 0;
		paramContainer.add(lrLabel, c);
		
		
		//learning rate
		lrField = new JTextField(fieldCols);
		lrField.setText("0.5");
		c.gridx = 1;
		paramContainer.add(lrField, c);
		
		
		c.insets = new Insets(0, 0, 5, 0);
		
		agent0Label = new JLabel("Agent 0");
		c.gridx = 1;
		c. gridy = 1;
		paramContainer.add(agent0Label, c);
		
		agent1Label = new JLabel("Agent 1");
		c.gridx = 2;
		paramContainer.add(agent1Label, c);
		
		c.insets = new Insets(0, 0, 0, 0);
		
		//steal
		sLabel = new JLabel("Steal");
		c.gridx = 0;
		c.gridy = 2;
		paramContainer.add(sLabel, c);
		
		a0SField = new JTextField(fieldCols);
		a0SField.setText("0.0");
		c.gridx = 1;
		paramContainer.add(a0SField, c);
		
		a1SField = new JTextField(fieldCols);
		a1SField.setText("0.0");
		c.gridx = 2;
		paramContainer.add(a1SField, c);
		
		
		//punch for steal
		psLabel = new JLabel("Punch for Steal");
		c.gridx = 0;
		c.gridy = 3;
		paramContainer.add(psLabel, c);
		
		a0PSField = new JTextField(fieldCols);
		a0PSField.setText("0.0");
		c.gridx = 1;
		paramContainer.add(a0PSField, c);
		
		a1PSField = new JTextField(fieldCols);
		a1PSField.setText("0.0");
		c.gridx = 2;
		paramContainer.add(a1PSField, c);
		
		
		//punch for punch
		ppLabel = new JLabel("Punch for Punch");
		c.gridx = 0;
		c.gridy = 4;
		paramContainer.add(ppLabel, c);
		
		a0PPField = new JTextField(fieldCols);
		a0PPField.setText("0.0");
		c.gridx = 1;
		paramContainer.add(a0PPField, c);
		
		a1PPField = new JTextField(fieldCols);
		a1PPField.setText("0.0");
		c.gridx = 2;
		paramContainer.add(a1PPField, c);
		
		
		c.insets = new Insets(30, 0, 0, 0);
		
		
		
		
		playMatchButton = new JButton("Play Match");
		playMatchButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				playMatch();
			}
		});
		
		c.gridy = 6;
		c.gridx = 0;
		c.gridwidth = 3;
		paramContainer.add(playMatchButton, c);
		
		
		a0CumulativeReturn = new JLabel("----");
		c.gridx = 1;
		c.gridy = 7;
		c.gridwidth = 1;
		c.insets = new Insets(10, 0, 0, 0);
		paramContainer.add(a0CumulativeReturn, c);
		
		a1CumulativeReturn = new JLabel("----");
		c.gridx = 2;
		paramContainer.add(a1CumulativeReturn, c);
		
		performLabel = new JLabel("Performance");
		c.gridx = 0;
		paramContainer.add(performLabel, c);
		
		
		
		computeAverageButton = new JButton("Compute Averages");
		computeAverageButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				computeAverage();
			}
		});
		c.gridx = 0;
		c.gridy = 8;
		c.gridwidth = 3;
		c.insets = new Insets(30, 0, 0, 0);
		paramContainer.add(computeAverageButton, c);
		
		
		a0AvgCumulativeReturn = new JLabel("----");
		c.insets = new Insets(10, 0, 0, 0);
		c.gridy = 9;
		c.gridx = 1;
		c.gridwidth = 1;
		paramContainer.add(a0AvgCumulativeReturn, c);
		
		a1AvgCumulativeReturn = new JLabel("----");
		c.gridx = 2;
		paramContainer.add(a1AvgCumulativeReturn, c);
		
		avgCumLabel = new JLabel("Mean Performance");
		c.gridx = 0;
		paramContainer.add(avgCumLabel, c);
		
		csvFileField = new JTextField(12);
		c.insets = new Insets(50, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 10;
		c.gridwidth = 3;
		paramContainer.add(csvFileField, c);
		
		saveToFileButton = new JButton("Save Results");
		saveToFileButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveToFile();
			}
		});
		
		c.gridy = 11;
		c.insets = new Insets(0, 0, 0, 0);
		paramContainer.add(saveToFileButton, c);
		
		
		
		
		
		//now do top control
		viewControlArea.setLayout(new GridBagLayout());
		GridBagConstraints c2 = new GridBagConstraints();
		
		
		incStage = new JButton(">");
		incStage.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				incStage();
			}
		});
		incStage.setEnabled(false);
		c2.gridx = 3;
		c2.gridy = 0;
		viewControlArea.add(incStage, c2);
		
		decStage = new JButton("<");
		decStage.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				decStage();
			}
		});
		decStage.setEnabled(false);
		c2.gridx = 1;
		viewControlArea.add(decStage, c2);
		
		
		stageSlider = new JSlider(0, 1000, 0);
		stageSlider.setPaintLabels(true);
		stageSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent arg0) {
				sliderChanged();
			}
		});
		stageSlider.setEnabled(false);
		
		c2.gridx = 1;
		c2.gridwidth=3;
		c2.gridy = 1;
		viewControlArea.add(stageSlider, c2);
		
		stageLabel = new JLabel("0");
		Dimension sld = stageLabel.getPreferredSize();
		stageLabel.setPreferredSize(new Dimension(sld.width+50, sld.height));
		c2.gridx = 4;
		c2.gridwidth=1;
		viewControlArea.add(stageLabel, c2);
		
		
		
		c2.insets = new Insets(20, 0, 0, 0);
		
		
		
		a0ActionLabel = new JLabel("--------");
		c2.gridx = 1;
		c2.gridy = 2;
		c2.gridwidth = 1;
		viewControlArea.add(a0ActionLabel, c2);
		
		a1ActionLabel = new JLabel("--------");
		c2.gridx = 3;
		viewControlArea.add(a1ActionLabel, c2);
		
	
	}
	
	protected void playMatch(){
		System.out.println("play match");
		incStage.setEnabled(true);
		decStage.setEnabled(true);
		stageSlider.setEnabled(true);
		
		double oldLearningRate = learningRate;
		learningRate = Double.parseDouble(lrField.getText());
		
		baseAgentFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PSField.getText()), Double.parseDouble(a0PPField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PSField.getText()), Double.parseDouble(a1PPField.getText())};
		
		//TBFSSubjectiveRFWS
		
		JointReward a0SRF; 
		JointReward a1SRF;
		
		if(this.gameVariant == 1){
			a0SRF = new TBFSSubjectiveRF(objectiveRF, a0SRParams);
			a1SRF = new TBFSSubjectiveRF(objectiveRF, a1SRParams);
		}
		else{
			a0SRF = new TBFSSubjectiveRFWS(objectiveRF, a0SRParams);
			a1SRF = new TBFSSubjectiveRFWS(objectiveRF, a1SRParams);
		}
			
		
		
		
		System.out.println(a0SRF.toString());
		System.out.println(a1SRF.toString());
		
		AgentFactory a0Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a0SRF);
		AgentFactory a1Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a1SRF);
		
		SGQLAgent agent0 = (SGQLAgent)a0Factory.generateAgent();
		SGQLAgent agent1 = (SGQLAgent)a1Factory.generateAgent();
		
		World world = worldGenerator.generateWorld();
		agent0.joinWorld(world, at);
		agent1.joinWorld(world, at);
		
		agent0Name = agent0.getAgentName();
		agent1Name = agent1.getAgentName();
		
		String [] anames = new String []{agent0.getAgentName(), agent1.getAgentName()};
		agent0QueryStates = getQueryStates(anames, 0);
		agent1QueryStates = getQueryStates(anames, 1);
		
		
		DPrint.toggleCode(world.getDebugId(), false);
		
		boolean firstTime = false;
		if(this.ma == null){
			firstTime = true;
		}
		
		this.ma = this.getMatchAnalyzer(world, agent0, agent1);
		this.ma.runMatch(1000);
		//this.ma = new MatchAnalizer(world, agent0, agent1, agent0QueryStates, agent1QueryStates, 1000);
		
		this.qTableController.setMatchAnalyzer(this.ma);
		this.qTableController.setAgentNames(agent0Name, agent1Name);
		this.qTableController.setQueryState(agent0QueryStates, agent1QueryStates);
		
		if(firstTime){
			this.qTableController.setupTable();
		}
		
		
		if(oldLearningRate != learningRate || !this.doubleArraysEqual(oldA0Params, a0SRParams) || !this.doubleArraysEqual(oldA1Params, a1SRParams)){
			this.a0AvgCumulativeReturn.setText("-----");
			this.a1AvgCumulativeReturn.setText("-----");
		}
		
		this.a0CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(0)));
		this.a1CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(1)));
		
		
		this.qTableController.updateRFValues(a0SRF, a1SRF);
		
		stage = 0;
		maxStage = this.ma.numQSpaceMeasure()-1;
		stageSlider.setMaximum(this.ma.numQSpaceMeasure()-1);
		stageSlider.setValue(0);
		
		
		this.oldA0Params = a0SRParams;
		this.oldA1Params = a1SRParams;
		
		this.updateStage();
		
		
	}
	
	protected MatchAnalizer getMatchAnalyzer(World world, SGQLAgent agent0, SGQLAgent agent1){
		return new MatchAnalyzerWInteraction(world, agent0, agent1, extractStatesFromQuery(agent0QueryStates), extractStatesFromQuery(agent1QueryStates));
	}
	
	protected List <State> extractStatesFromQuery(List <StateActionSetTuple> queries){
		List <State> res = new ArrayList<State>(queries.size());
		for(StateActionSetTuple sas : queries){
			res.add(sas.s);
		}
		return res;
	}
	
	protected void computeAverage(){
		
		System.out.println("compute averages");
		incStage.setEnabled(true);
		decStage.setEnabled(true);
		stageSlider.setEnabled(true);
		
		learningRate = Double.parseDouble(lrField.getText());
		
		baseAgentFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PSField.getText()), Double.parseDouble(a0PPField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PSField.getText()), Double.parseDouble(a1PPField.getText())};
		
		JointReward a0SRF; 
		JointReward a1SRF;
		
		if(this.gameVariant == 1){
			a0SRF = new TBFSSubjectiveRF(objectiveRF, a0SRParams);
			a1SRF = new TBFSSubjectiveRF(objectiveRF, a1SRParams);
		}
		else{
			a0SRF = new TBFSSubjectiveRFWS(objectiveRF, a0SRParams);
			a1SRF = new TBFSSubjectiveRFWS(objectiveRF, a1SRParams);
		}
		
		System.out.println(a0SRF.toString());
		System.out.println(a1SRF.toString());
		
		AgentFactory a0Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a0SRF);
		AgentFactory a1Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a1SRF);
		
		boolean firstTime = false;
		if(this.ma == null){
			firstTime = true;
		}
		
		int n = 25;
		double sum0 = 0.;
		double sum1 = 0.;
		
		for(int i = 0; i < n; i++){
			
			SGQLAgent agent0 = (SGQLAgent)a0Factory.generateAgent();
			SGQLAgent agent1 = (SGQLAgent)a1Factory.generateAgent();
			
			World world = worldGenerator.generateWorld();
			agent0.joinWorld(world, at);
			agent1.joinWorld(world, at);
			
			DPrint.toggleCode(world.getDebugId(), false);
			
			agent0Name = agent0.getAgentName();
			agent1Name = agent1.getAgentName();
			
			String [] anames = new String []{agent0.getAgentName(), agent1.getAgentName()};
			if(agent0QueryStates == null){
				agent0QueryStates = getQueryStates(anames, 0);
				agent1QueryStates = getQueryStates(anames, 1);
			}
			
			this.ma = this.getMatchAnalyzer(world, agent0, agent1);
			this.ma.runMatch(1000);
			
			sum0 += ma.getObjectiveCumulativeReward(0);
			sum1 += ma.getObjectiveCumulativeReward(1);
			
		}
		
		this.qTableController.setMatchAnalyzer(this.ma);
		this.qTableController.setAgentNames(agent0Name, agent1Name);
		this.qTableController.setQueryState(agent0QueryStates, agent1QueryStates);
		
		if(firstTime){
			this.qTableController.setupTable();
		}
		
		
		this.a0AvgCumulativeReturn.setText(String.format("%.1f", sum0/n));
		this.a1AvgCumulativeReturn.setText(String.format("%.1f", sum1/n));
		
		this.a0CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(0)));
		this.a1CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(1)));
		
		this.qTableController.updateRFValues(a0SRF, a1SRF);
		
		stage = 0;
		maxStage = this.ma.numQSpaceMeasure()-1;
		stageSlider.setMaximum(this.ma.numQSpaceMeasure()-1);
		stageSlider.setValue(0);
		
		
		this.oldA0Params = a0SRParams;
		this.oldA1Params = a1SRParams;
		
		this.updateStage();
		
		
	}

	
	protected void saveToFile(){
		String path = csvFileField.getText();
		
		if(path.length() == 0){
			return;
		}
		
		if(this.ma == null){
			return ;
		}
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			MatchAnalyzerWInteraction mai = (MatchAnalyzerWInteraction)this.ma;
			String csv = mai.getCSVStringUsingSubRF();
			out.write(csv);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	protected void sliderChanged(){
		stage = stageSlider.getValue();
		this.updateStage();
	}
	
	protected void incStage(){
		stage++;
		stage = Math.min(stage, maxStage);
		stageSlider.setValue(stage);
		this.updateStage();
	}
	
	protected void decStage(){
		stage--;
		stage = Math.max(stage, 0);
		stageSlider.setValue(stage);
		this.updateStage();
	}
	
	protected void updateStage(){
		
		stageLabel.setText(""+stage);
		if(stage < this.ma.numJARecords()){
			JointAction ja = ma.getJointActionAtTime(stage);

			this.a0ActionLabel.setText(ja.action(agent0Name).justActionString());
			this.a1ActionLabel.setText(ja.action(agent1Name).justActionString());
		}
		else{
			this.a0ActionLabel.setText("--------");
			this.a1ActionLabel.setText("--------");
		}
		
		
		this.qTableController.changeDisplayedQValues(stage);
		this.qTableController.getRenderer().repaint();
	}
	
	
	protected boolean doubleArraysEqual(double [] oldArray, double [] newArray){
		if(oldArray == null){
			return false;
		}
		
		for(int i = 0; i < oldArray.length; i++){
			if(oldArray[i] != newArray[i]){
				return false;
			}
		}
		
		return true;
	}
	
	
	protected List <StateActionSetTuple> getQueryStates(String [] anames, int forAgent){
		
		if(this.gameVariant == 1){
			return this.getQueryStatesVariant1(anames, forAgent);
		}
		else{
			return this.getQueryStatesVariant2(anames, forAgent);
		}
		
	}
	
	
	protected List <StateActionSetTuple> getQueryStatesVariant1(String [] anames, int forAgent){
		
		List <StateActionSetTuple> res = new ArrayList<QTableVisController.StateActionSetTuple>();
	
		
		int opponent = 1;
		if(forAgent == 1){
			opponent = 0;
		}
		
		int [] arrayFA = new int[TBForageSteal.NALTS];
		for(int i = 0; i < TBForageSteal.NALTS; i++){
			arrayFA[i] = i;
		}
		
		State s0 = TBForageSteal.getGameStartState(domain, arrayFA, forAgent);
		List <ObjectInstance> agentObs = s0.getObjectsOfTrueClass(TBForageSteal.CLASSAGENT);
		s0.renameObject(agentObs.get(0), anames[0]);
		s0.renameObject(agentObs.get(1), anames[1]);
		
		StateActionSetTuple sas0 = new StateActionSetTuple(s0, "R");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "0"), ""), "F0");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "1"), ""), "F1");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "2"), ""), "F2");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "3"), ""), "F3");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "4"), ""), "F4");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONSTEAL), ""), "S");
		
		
		res.add(sas0);
		
		
		//now do when their opponent stole from them
		State s1 = s0.copy();
		ObjectInstance opponentOb = s1.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 2);
		
		StateActionSetTuple sas1 = new StateActionSetTuple(s1, "PS");
		sas1.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas1.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONPUNCH), ""), "P");
		
		res.add(sas1);
		
		
		
		//now do when they are in punching match
		State s2 = s0.copy();
		ObjectInstance qAgent = s2.getObject(anames[forAgent]);
		qAgent.setValue(TBForageSteal.ATTPTA, 3);
		
		opponentOb = s2.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 3);
		
		StateActionSetTuple sas2 = new StateActionSetTuple(s2, "PP");
		sas2.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas2.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONPUNCH), ""), "P");
		
		res.add(sas2);
		
		
		return res;
		
	}
	
	
	protected List <StateActionSetTuple> getQueryStatesVariant2(String [] anames, int forAgent){
		
		List <StateActionSetTuple> res = new ArrayList<QTableVisController.StateActionSetTuple>();
	
		
		int opponent = 1;
		if(forAgent == 1){
			opponent = 0;
		}
		
		int [] arrayFA = new int[TBForageSteal.NALTS];
		for(int i = 0; i < TBForageSteal.NALTS; i++){
			arrayFA[i] = i;
		}
		
		State s0 = TBForageSteal.getGameStartState(domain, arrayFA, forAgent);
		List <ObjectInstance> agentObs = s0.getObjectsOfTrueClass(TBForageSteal.CLASSAGENT);
		s0.renameObject(agentObs.get(0), anames[0]);
		s0.renameObject(agentObs.get(1), anames[1]);
		
		StateActionSetTuple sas0 = new StateActionSetTuple(s0, "R");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "0"), ""), "F0");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "1"), ""), "F1");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "2"), ""), "F2");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "3"), ""), "F3");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONFORAGE + "4"), ""), "F4");
		sas0.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONSTEAL), ""), "S");
		
		
		res.add(sas0);
		
		
		//now do when their opponent stole from them
		State s1 = s0.copy();
		ObjectInstance opponentOb = s1.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 2);
		
		StateActionSetTuple sas1 = new StateActionSetTuple(s1, "PS");
		sas1.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas1.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONPUNCH), ""), "P");
		
		res.add(sas1);
		
		
		
		//now do when their opponent stole and punched them
		State s2 = s0.copy();
		ObjectInstance qAgent = s2.getObject(anames[forAgent]);
		int pn = qAgent.getDiscValForAttribute(TBForageSteal.ATTPN);
		int sa = 4;
		if(pn == 1){
			sa = 3;
		}
		
		qAgent.setValue(TBForageSteal.ATTPTA, sa);
		
		opponentOb = s2.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, sa);
		
		StateActionSetTuple sas2 = new StateActionSetTuple(s2, "PP");
		sas2.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas2.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONPUNCH), ""), "P");
		
		res.add(sas2);
		
		
		//now do when they stole and their opponent punched them for punishment
		State s3 = s0.copy();
		qAgent = s3.getObject(anames[forAgent]);
		pn = qAgent.getDiscValForAttribute(TBForageSteal.ATTPN);
		sa = 3;
		if(pn == 1){
			sa = 4;
		}
		
		qAgent.setValue(TBForageSteal.ATTPTA, sa);
		
		opponentOb = s3.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, sa);
		
		StateActionSetTuple sas3 = new StateActionSetTuple(s3, "B");
		sas3.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONNOP), ""), "N");
		sas3.addAction(new GroundedSingleAction(anames[forAgent], domain.getSingleAction(TBForageSteal.ACTIONPUNCH), ""), "P");
		
		res.add(sas3);
		
		
		return res;
		
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TBFSMatchVisualizer3Param vis = new TBFSMatchVisualizer3Param();
		vis.pack();
		vis.setVisible(true);

	}

}
