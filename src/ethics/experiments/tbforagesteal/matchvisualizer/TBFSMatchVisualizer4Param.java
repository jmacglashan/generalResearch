package ethics.experiments.tbforagesteal.matchvisualizer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.debugtools.DPrint;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import burlap.oomdp.stochasticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.TBFSAlternatingTurnSG;
import domain.stocasticgames.foragesteal.TBFSStandardMechanics;
import domain.stocasticgames.foragesteal.TBFSStandardReward;
import domain.stocasticgames.foragesteal.TBFSWhoStartedMechanics;
import domain.stocasticgames.foragesteal.TBForageSteal;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import ethics.experiments.tbforagesteal.auxiliary.TBFSSRFWS4Param;

public class TBFSMatchVisualizer4Param extends TBFSMatchVisualizer3Param {

	JLabel										bLabel;
	
	JTextField									a0BField;
	JTextField									a1BField;
	

	private static final long serialVersionUID = 1L;

	public TBFSMatchVisualizer4Param() {
		this.gameVariant = 2;
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
	
	
	
	@Override
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
		
		//bull
		bLabel = new JLabel("Bully Punch");
		c.gridx = 0;
		c.gridy = 5;
		paramContainer.add(bLabel, c);
		
		a0BField = new JTextField(fieldCols);
		a0BField.setText("0.0");
		c.gridx = 1;
		paramContainer.add(a0BField, c);
		
		a1BField = new JTextField(fieldCols);
		a1BField.setText("0.0");
		c.gridx = 2;
		paramContainer.add(a1BField, c);
		
		
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
		
		baseAgentFactory = new SGNaiveQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PSField.getText()), Double.parseDouble(a0PPField.getText()), Double.parseDouble(a0BField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PSField.getText()), Double.parseDouble(a1PPField.getText()), Double.parseDouble(a1BField.getText())};
		
		
		JointReward a0SRF = new TBFSSRFWS4Param(objectiveRF, a0SRParams); 
		JointReward a1SRF = new TBFSSRFWS4Param(objectiveRF, a1SRParams);
		
		

		
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
	
	
	
	protected void computeAverage(){
		
		System.out.println("compute averages");
		incStage.setEnabled(true);
		decStage.setEnabled(true);
		stageSlider.setEnabled(true);
		
		learningRate = Double.parseDouble(lrField.getText());
		
		baseAgentFactory = new SGNaiveQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PSField.getText()), Double.parseDouble(a0PPField.getText()), Double.parseDouble(a0BField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PSField.getText()), Double.parseDouble(a1PPField.getText()), Double.parseDouble(a1BField.getText())};
		
		
		JointReward a0SRF = new TBFSSRFWS4Param(objectiveRF, a0SRParams); 
		JointReward a1SRF = new TBFSSRFWS4Param(objectiveRF, a1SRParams);
		
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
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TBFSMatchVisualizer4Param vis = new TBFSMatchVisualizer4Param();
		vis.pack();
		vis.setVisible(true);

	}

}
