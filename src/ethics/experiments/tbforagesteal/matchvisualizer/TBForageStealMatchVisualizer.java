package ethics.experiments.tbforagesteal.matchvisualizer;

import java.awt.BorderLayout;
import java.awt.Color;
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
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.WorldGenerator;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import burlap.oomdp.stochasticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.TBFSAlternatingTurnSG;
import domain.stocasticgames.foragesteal.TBFSStandardMechanics;
import domain.stocasticgames.foragesteal.TBFSStandardReward;
import domain.stocasticgames.foragesteal.TBForageSteal;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import ethics.experiments.tbforagesteal.auxiliary.TBFSSubjectiveRF;
import ethics.experiments.tbforagesteal.matchvisualizer.ColoredTableRenderer.ColoredTableCell;
import ethics.experiments.tbforagesteal.matchvisualizer.ColoredTableRenderer.TableLabel;

public class TBForageStealMatchVisualizer extends JFrame {

private static final long serialVersionUID = 1L;

	
	//gui components
	ColoredTableRenderer						renderer;
	
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
	
	List <State> 								agent0QueryStates;
	List <State> 								agent1QueryStates;
	
	double []									oldA0Params;
	double []									oldA1Params;
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TBForageStealMatchVisualizer vis = new TBForageStealMatchVisualizer();
		vis.pack();
		vis.setVisible(true);

	}
	
	
	public TBForageStealMatchVisualizer(){
		stage = 0;
		maxStage = 1000;
		
		TBForageSteal gen = new TBForageSteal();
		domain = (SGDomain) gen.generateDomain();
		
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		objectiveRF = new TBFSStandardReward();
		
		worldGenerator = new ConstantWorldGenerator(domain, new TBFSStandardMechanics(), objectiveRF, new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG(domain));
		//worldGenerator = new ConstantWorldGenerator(domain, new TBFSWhoStartedMechanics(), objectiveRF, new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG());
		
		at = new AgentType("agent", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		
		
		
		this.initGUI();
	}
	
	
	public void initGUI(){
		
		renderer = new ColoredTableRenderer(3);
		renderer.setPreferredSize(new Dimension(1000, 650));
		
		Container viewingArea = new Container();
		viewingArea.setPreferredSize(new Dimension(1000, 770));
		viewingArea.setLayout(new BorderLayout());
		
		Container viewControlArea = new Container();
		viewControlArea.setPreferredSize(new Dimension(1000, 120));
		
		viewingArea.add(viewControlArea, BorderLayout.NORTH);
		viewingArea.add(renderer, BorderLayout.CENTER);
		
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
		
		/*
		a0CumulativeReturn = new JLabel("A0 CR: -----");
		c2.gridx = 0;
		c2.gridy = 2;
		c2.gridwidth = 1;
		viewControlArea.add(a0CumulativeReturn, c2);
		*/
		
		
		/*
		jointActionLabel = new JLabel("agent0::-----;agent1::-----");
		c2.gridx = 1;
		c2.gridy = 2;
		c2.gridwidth = 4;
		viewControlArea.add(jointActionLabel, c2);
		*/
		
		//c2.insets = new Insets(20, 20, 0, 0);
		
		/*
		a1CumulativeReturn = new JLabel("A1 CR: -----");
		c2.gridx = 5;
		c2.gridwidth = 1;
		viewControlArea.add(a1CumulativeReturn, c2);
		*/
		
		
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
		
		baseAgentFactory = new SGNaiveQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PSField.getText()), Double.parseDouble(a0PPField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PSField.getText()), Double.parseDouble(a1PPField.getText())};
		
		//TBFSSubjectiveRFWS
		
		JointReward a0SRF = new TBFSSubjectiveRF(objectiveRF, a0SRParams);
		JointReward a1SRF = new TBFSSubjectiveRF(objectiveRF, a1SRParams);
		
		//JointReward a0SRF = new TBFSSubjectiveRFWS(objectiveRF, a0SRParams);
		//JointReward a1SRF = new TBFSSubjectiveRFWS(objectiveRF, a1SRParams);
		
		System.out.println(a0SRF.toString());
		System.out.println(a1SRF.toString());
		
		AgentFactory a0Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a0SRF);
		AgentFactory a1Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a1SRF);
		
		SGNaiveQLAgent agent0 = (SGNaiveQLAgent)a0Factory.generateAgent();
		SGNaiveQLAgent agent1 = (SGNaiveQLAgent)a1Factory.generateAgent();
		
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
		
		if(firstTime){
			this.setupTable();
		}
		
		
		if(oldLearningRate != learningRate || !this.doubleArraysEqual(oldA0Params, a0SRParams) || !this.doubleArraysEqual(oldA1Params, a1SRParams)){
			this.a0AvgCumulativeReturn.setText("-----");
			this.a1AvgCumulativeReturn.setText("-----");
		}
		
		this.a0CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(0)));
		this.a1CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(1)));
		
		this.setupRewardFunctionValues(a0SRParams, a1SRParams);
		
		stage = 0;
		maxStage = this.ma.numQSpaceMeasure()-1;
		stageSlider.setMaximum(this.ma.numQSpaceMeasure()-1);
		stageSlider.setValue(0);
		
		
		this.oldA0Params = a0SRParams;
		this.oldA1Params = a1SRParams;
		
		this.updateStage();
		
		
	}
	
	protected MatchAnalizer getMatchAnalyzer(World world, SGNaiveQLAgent agent0, SGNaiveQLAgent agent1){
		return new MatchAnalyzerWInteraction(world, agent0, agent1, agent0QueryStates, agent1QueryStates);
	}
	
	protected void computeAverage(){
		
		System.out.println("compute averages");
		incStage.setEnabled(true);
		decStage.setEnabled(true);
		stageSlider.setEnabled(true);
		
		learningRate = Double.parseDouble(lrField.getText());
		
		baseAgentFactory = new SGNaiveQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PSField.getText()), Double.parseDouble(a0PPField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PSField.getText()), Double.parseDouble(a1PPField.getText())};
		
		JointReward a0SRF = new TBFSSubjectiveRF(objectiveRF, a0SRParams);
		JointReward a1SRF = new TBFSSubjectiveRF(objectiveRF, a1SRParams);
		
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
			
			SGNaiveQLAgent agent0 = (SGNaiveQLAgent)a0Factory.generateAgent();
			SGNaiveQLAgent agent1 = (SGNaiveQLAgent)a1Factory.generateAgent();
			
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
		
		
		
		if(firstTime){
			this.setupTable();
		}
		
		
		this.a0AvgCumulativeReturn.setText(String.format("%.1f", sum0/n));
		this.a1AvgCumulativeReturn.setText(String.format("%.1f", sum1/n));
		
		this.a0CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(0)));
		this.a1CumulativeReturn.setText(String.format("%.1f", this.ma.getObjectiveCumulativeReward(1)));
		
		this.setupRewardFunctionValues(a0SRParams, a1SRParams);
		
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
		
		
		this.updateQValues(0, agent0Name);
		this.updateQValues(1, agent1Name);
		
		this.renderer.repaint();
	}
	
	
	protected void updateQValues(int aid, String aName){
		
		String baseName = "a"+aid+"Q";
		
		List <State> queryStates = agent0QueryStates;
		if(aid == 1){
			queryStates = agent1QueryStates;
		}
		
		State s0 = queryStates.get(0);
		State s1 = queryStates.get(1);
		State s2 = queryStates.get(2);
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONNOP, this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONNOP), "")));
		renderer.setCellValue(baseName+"s1"+TBForageSteal.ACTIONNOP, this.ma.getQFor(aid, stage, s1, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONNOP), "")));
		renderer.setCellValue(baseName+"s2"+TBForageSteal.ACTIONNOP, this.ma.getQFor(aid, stage, s2, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONNOP), "")));
	
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"0", this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONFORAGE+"0"), "")));
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"1", this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONFORAGE+"1"), "")));
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"2", this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONFORAGE+"2"), "")));
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"3", this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONFORAGE+"3"), "")));
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"4", this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONFORAGE+"4"), "")));
		
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONSTEAL, this.ma.getQFor(aid, stage, s0, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONSTEAL), "")));
		
		
		renderer.setCellValue(baseName+"s1"+TBForageSteal.ACTIONPUNCH, this.ma.getQFor(aid, stage, s1, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONPUNCH), "")));
		renderer.setCellValue(baseName+"s2"+TBForageSteal.ACTIONPUNCH, this.ma.getQFor(aid, stage, s2, new GroundedSingleAction(aName, domain.getSingleAction(TBForageSteal.ACTIONPUNCH), "")));
		
		
	}

	
	protected void setupRewardFunctionValues(double [] a0Params, double [] a1Params){
		
		TBFSStandardReward obRF = (TBFSStandardReward)objectiveRF;
		
		this.setupObjectiveRewardFunctionValues(obRF, "a0OR");
		this.setupObjectiveRewardFunctionValues(obRF, "a1OR");
		
		this.setupSubjectiveRewardFunctionValues(obRF, "a0SR", a0Params);
		this.setupSubjectiveRewardFunctionValues(obRF, "a1SR", a1Params);
		
	}
	
	protected void setupObjectiveRewardFunctionValues(TBFSStandardReward obRF, String baseName){
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONNOP, 0.);
		renderer.setCellValue(baseName+"s1"+TBForageSteal.ACTIONNOP, 0.);
		renderer.setCellValue(baseName+"s2"+TBForageSteal.ACTIONNOP, 0.);
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"0", obRF.forageRewards[0]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"1", obRF.forageRewards[1]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"2", obRF.forageRewards[2]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"3", obRF.forageRewards[3]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"4", obRF.forageRewards[4]);
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONSTEAL, obRF.stealerReward);
		
		renderer.setCellValue(baseName+"s1"+TBForageSteal.ACTIONPUNCH, obRF.puncherReward);
		renderer.setCellValue(baseName+"s2"+TBForageSteal.ACTIONPUNCH, obRF.puncherReward);
	}
	
	
	protected void setupSubjectiveRewardFunctionValues(TBFSStandardReward obRF, String baseName, double [] params){
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONNOP, 0.);
		renderer.setCellValue(baseName+"s1"+TBForageSteal.ACTIONNOP, 0.);
		renderer.setCellValue(baseName+"s2"+TBForageSteal.ACTIONNOP, 0.);
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"0", obRF.forageRewards[0]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"1", obRF.forageRewards[1]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"2", obRF.forageRewards[2]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"3", obRF.forageRewards[3]);
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"4", obRF.forageRewards[4]);
		
		renderer.setCellValue(baseName+"s0"+TBForageSteal.ACTIONSTEAL, obRF.stealerReward+params[0]);
		
		renderer.setCellValue(baseName+"s1"+TBForageSteal.ACTIONPUNCH, obRF.puncherReward+params[1]);
		renderer.setCellValue(baseName+"s2"+TBForageSteal.ACTIONPUNCH, obRF.puncherReward+params[2]);
		
		
	}
	
	protected void setupTable(){
		
		
		//double discLimit = this.discount / (1 - this.discount);
		double minValue = -2.;
		double maxValue = 3.;
		
		LandmarkColorBlendInterpolation colorBlend = new LandmarkColorBlendInterpolation();
		colorBlend.addNextLandMark(minValue, Color.red);
		colorBlend.addNextLandMark(0., Color.white);
		colorBlend.addNextLandMark(maxValue, Color.blue);
		
		
		float lm = 30;
		float tm = 50;
		float tlm = 60;
		float llm = 100;
		float cellWidth = 60;
		int fontPointSize = 16;
		int labelPointSize = 14;
		float stateHGap = cellWidth;
		
		float startX = lm+llm;
		float startY = tm;
		
		
		TableLabel a0Label = new TableLabel("Agent 0", 24, 60, (int)startY);
		renderer.addLabel(a0Label);
		
		
		this.setupColumnLabels(startX, startY, cellWidth, labelPointSize, stateHGap);
		
		startY += tlm;
		
		String baseName = "a0Q";
		TableLabel a0QRowLabel = new TableLabel("Q-values", labelPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a0QRowLabel);
		this.setupTableRow(baseName, colorBlend, startX, startY, cellWidth, fontPointSize, stateHGap);
		
		startY += cellWidth;
		baseName = "a0SR";
		TableLabel a0SRRowLabel = new TableLabel("Subjective RF", labelPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a0SRRowLabel);
		this.setupTableRow(baseName, colorBlend, startX, startY, cellWidth, fontPointSize, stateHGap);
		
		startY += cellWidth;
		baseName = "a0OR";
		TableLabel a0ORRowLabel = new TableLabel("Objective RF", labelPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a0ORRowLabel);
		this.setupTableRow(baseName, colorBlend, startX, startY, cellWidth, fontPointSize, stateHGap);
		
		
		
		
		startY += cellWidth*2;
		
		TableLabel a1Label = new TableLabel("Agent 1", 24, 60, (int)startY);
		renderer.addLabel(a1Label);
		
		this.setupColumnLabels(startX, startY, cellWidth, labelPointSize, stateHGap);
		
		startY += tlm;
		
		baseName = "a1Q";
		TableLabel a1QRowLabel = new TableLabel("Q-values", labelPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a1QRowLabel);
		this.setupTableRow(baseName, colorBlend, startX, startY, cellWidth, fontPointSize, stateHGap);
		
		startY += cellWidth;
		baseName = "a1SR";
		TableLabel a1SRRowLabel = new TableLabel("Subjective RF", labelPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a1SRRowLabel);
		this.setupTableRow(baseName, colorBlend, startX, startY, cellWidth, fontPointSize, stateHGap);
		
		startY += cellWidth;
		baseName = "a1OR";
		TableLabel a1ORRowLabel = new TableLabel("Objective RF", labelPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a1ORRowLabel);
		this.setupTableRow(baseName, colorBlend, startX, startY, cellWidth, fontPointSize, stateHGap);
		
	}
	
	
	protected void setupColumnLabels(float startX, float startY, float cellWidth, int fontPointSize, float stateHGap){
		
		float ly = startY + (cellWidth / 2);
		
		TableLabel s0n = new TableLabel("S0-N", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0n);
		startX += cellWidth;
		
		
		
		TableLabel s0f0 = new TableLabel("S0-F0", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0f0);
		startX += cellWidth;
		
		TableLabel s0f1 = new TableLabel("S0-F1", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0f1);
		startX += cellWidth;
		
		TableLabel s0f2 = new TableLabel("S0-F2", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0f2);
		startX += cellWidth;
		
		TableLabel s0f3 = new TableLabel("S0-F3", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0f3);
		startX += cellWidth;
		
		TableLabel s0f4 = new TableLabel("S0-F4", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0f4);
		startX += cellWidth;
		
		
		
		TableLabel s0s = new TableLabel("S0-S", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s0s);
		startX += cellWidth;
		
		
		startX += stateHGap;
		
		
		TableLabel s1n = new TableLabel("S1-N", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s1n);
		startX += cellWidth;
		
		TableLabel s1p = new TableLabel("S1-PS", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s1p);
		startX += cellWidth;
		
		
		startX += stateHGap;
		
		
		TableLabel s2n = new TableLabel("S2-N", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s2n);
		startX += cellWidth;
		
		TableLabel s2p = new TableLabel("S2-PP", fontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
		renderer.addLabel(s2p);
		startX += cellWidth;
		
		
		
	}
	
	
	protected void setupTableRow(String baseName, ColorBlend colorBlend, float startX, float startY, float cellWidth, int fontPointSize, float stateHGap){
		
		ColoredTableCell s0n = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONNOP, s0n);
		startX += cellWidth;
		
		
		
		ColoredTableCell s0f0 = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"0", s0f0);
		startX += cellWidth;
		
		ColoredTableCell s0f1 = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"1", s0f1);
		startX += cellWidth;
		
		ColoredTableCell s0f2 = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"2", s0f2);
		startX += cellWidth;
		
		ColoredTableCell s0f3 = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"3", s0f3);
		startX += cellWidth;
		
		ColoredTableCell s0f4 = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONFORAGE+"4", s0f4);
		startX += cellWidth;
		
		
		ColoredTableCell s0s = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s0"+TBForageSteal.ACTIONSTEAL, s0s);
		startX += cellWidth;
		
		startX += stateHGap;
		
		
		ColoredTableCell s1n = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s1"+TBForageSteal.ACTIONNOP, s1n);
		startX += cellWidth;
		
		ColoredTableCell s1p = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s1"+TBForageSteal.ACTIONPUNCH, s1p);
		startX += cellWidth;
		
		
		startX += stateHGap;
		
		ColoredTableCell s2n = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s2"+TBForageSteal.ACTIONNOP, s2n);
		startX += cellWidth;
		
		ColoredTableCell s2p = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, fontPointSize);
		renderer.putColorCell(baseName+"s2"+TBForageSteal.ACTIONPUNCH, s2p);
		startX += cellWidth;
		
		
		
		
	}
	
	
	protected List <State> getQueryStates(String [] anames, int forAgent){
		
		List <State> res = new ArrayList<State>();
		
		int opponent = 1;
		if(forAgent == 1){
			opponent = 0;
		}
		
		int [] arrayFA = new int[TBForageSteal.NALTS];
		for(int i = 0; i < TBForageSteal.NALTS; i++){
			arrayFA[i] = i;
		}
		
		State s0 = TBForageSteal.getGameStartState(domain, arrayFA, forAgent);
		List <ObjectInstance> agentObs = s0.getObjectsOfClass(TBForageSteal.CLASSAGENT);
		s0.renameObject(agentObs.get(0), anames[0]);
		s0.renameObject(agentObs.get(1), anames[1]);
		
		res.add(s0);
		
		
		//now do when their opponent stole from them
		State s1 = s0.copy();
		ObjectInstance opponentOb = s1.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 2);
		
		res.add(s1);
		
		
		
		//now do when they are in punching match
		State s2 = s0.copy();
		ObjectInstance qAgent = s2.getObject(anames[forAgent]);
		qAgent.setValue(TBForageSteal.ATTPTA, 3);
		
		opponentOb = s2.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 3);
		
		res.add(s2);
		
		
		return res;
		
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
	
}
