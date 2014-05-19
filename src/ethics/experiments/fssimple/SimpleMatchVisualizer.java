package ethics.experiments.fssimple;

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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.WorldGenerator;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleBTSJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import domain.stocasticgames.foragesteal.simple.FSSimplePOJR;
import ethics.experiments.fssimple.aux.ConsantPsudoTermWorldGenerator;
import ethics.experiments.fssimple.aux.FSRQInit;
import ethics.experiments.fssimple.aux.FSSimpleBTSG;
import ethics.experiments.fssimple.aux.FSSubjectiveRF;
import ethics.experiments.fssimple.aux.RNPseudoTerm;
import ethics.experiments.tbforagesteal.matchvisualizer.MatchAnalizer;
import ethics.experiments.tbforagesteal.matchvisualizer.QTableVisController;
import ethics.experiments.tbforagesteal.matchvisualizer.QTableVisController.StateActionSetTuple;

public class SimpleMatchVisualizer extends JFrame {

	private static final long serialVersionUID = 1L;
	
	QTableVisController							qTableController;
	
	JLabel										lrLabel;
	JTextField									lrField;
	
	JLabel										agent0Label;
	JLabel										agent1Label;
	JLabel										sLabel;
	JLabel										pLabel;

	JTextField									a0SField;
	JTextField									a0PField;
	
	JTextField									a1SField;
	JTextField									a1PField;
	
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
	double										discount = 0.95;
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
	
	
	int											numMatches = 1;
	

	public SimpleMatchVisualizer(){
		
		stage = 0;
		maxStage = 1000;
		
		double backTurnedProb = 0.2;
		
		FSSimple dgen = new FSSimple(3);
		this.domain = (SGDomain)dgen.generateDomain();
		//JointActionModel jam = new FSSimpleJAM();
		//JointActionModel jam = new FSSimpleBTJAM(backTurnedProb);
		JointActionModel jam = new FSSimpleBTSJAM(0.2, 0.2);
		
		//this.objectiveRF = new FSSimpleJR();
		//this.objectiveRF = new FSSimpleJR(1., -0.5, -2.5, 0.);
		this.objectiveRF = new FSSimplePOJR(1., -0.5, -2.5, 0.);
		
		this.hashingFactory = new DiscreteStateHashFactory();
		//SGStateGenerator sg = new FSSimpleSG(domain);
		SGStateGenerator sg = new FSSimpleBTSG(domain, backTurnedProb);
		
		this.worldGenerator = new ConsantPsudoTermWorldGenerator(domain, jam, this.objectiveRF, new NullTermination(), sg, new RNPseudoTerm());
		
		this.at = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
		
	}
	
	
	public void initGUI(){
		
		qTableController = new QTableVisController(800, 650, objectiveRF);
		
		Container viewingArea = new Container();
		viewingArea.setPreferredSize(new Dimension(800, 770));
		viewingArea.setLayout(new BorderLayout());
		
		Container viewControlArea = new Container();
		viewControlArea.setPreferredSize(new Dimension(800, 120));
		
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
		pLabel = new JLabel("Punish");
		c.gridx = 0;
		c.gridy = 3;
		paramContainer.add(pLabel, c);
		
		a0PField = new JTextField(fieldCols);
		a0PField.setText("0.0");
		c.gridx = 1;
		paramContainer.add(a0PField, c);
		
		a1PField = new JTextField(fieldCols);
		a1PField.setText("0.0");
		c.gridx = 2;
		paramContainer.add(a1PField, c);
		
		
		
		
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
		
		boolean firstTime = false;
		if(this.ma == null){
			firstTime = true;
		}
		
		double oldLearningRate = learningRate;
		
		
		learningRate = Double.parseDouble(lrField.getText());
		
		baseAgentFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PField.getText())};
		
		
		JointReward [] subjectiveRFStorage = new JointReward[2];
		
		this.performMatch(a0SRParams, a1SRParams, subjectiveRFStorage);
		
		
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
		
		
		this.qTableController.updateRFValues(subjectiveRFStorage[0], subjectiveRFStorage[1]);
		
		stage = 0;
		maxStage = this.ma.numQSpaceMeasure()-1;
		stageSlider.setMaximum(this.ma.numQSpaceMeasure()-1);
		stageSlider.setValue(0);
		
		
		this.oldA0Params = a0SRParams;
		this.oldA1Params = a1SRParams;
		
		this.updateStage();
		
	}
	
	
	protected void performMatch(double [] a0SRParams, double [] a1SRParams, JointReward[] subjectiveRFStorage){
		
		FSSubjectiveRF a0SRF = new FSSubjectiveRF(this.objectiveRF);
		a0SRF.setParameters(a0SRParams);
		FSRQInit v0QInit = new FSRQInit((FSSimpleJR)this.objectiveRF, a0SRF);
		
		FSSubjectiveRF a1SRF = new FSSubjectiveRF(this.objectiveRF);
		a1SRF.setParameters(a1SRParams);
		FSRQInit v1QInit = new FSRQInit((FSSimpleJR)this.objectiveRF, a1SRF);
		//FSRPunisherQInit v1QInit = new FSRPunisherQInit(a1SRF, (FSSimpleJR)this.objectiveRF);
		
		subjectiveRFStorage[0] = a0SRF;
		subjectiveRFStorage[1] = a1SRF;
		
		System.out.println(a0SRF.toString());
		System.out.println(a1SRF.toString());
		
		AgentFactory a0Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a0SRF);
		AgentFactory a1Factory = new AgentFactoryWithSubjectiveReward(baseAgentFactory, a1SRF);
		
		SGQLAgent agent0 = (SGQLAgent)a0Factory.generateAgent();
		//agent0.setQValueInitializer(v0QInit);
		agent0.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.));
		agent0.setLearningRate(new ExponentialDecayLR(learningRate, 0.999, 0.01));
		
		SGQLAgent agent1 = (SGQLAgent)a1Factory.generateAgent();
		agent1.setQValueInitializer(v1QInit);
		agent1.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(-6.5)); //caching uses this!
		//agent1.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(0));
		agent1.setLearningRate(new ExponentialDecayLR(learningRate, 0.999, 0.01));
		
		
		World world = worldGenerator.generateWorld();
		agent0.joinWorld(world, at);
		agent1.joinWorld(world, at);
		
		agent0Name = agent0.getAgentName();
		agent1Name = agent1.getAgentName();
		
		String [] anames = new String []{agent0.getAgentName(), agent1.getAgentName()};
		agent0QueryStates = getQueryStates(anames, 0);
		agent1QueryStates = getQueryStates(anames, 1);
		
		DPrint.toggleCode(world.getDebugId(), false);
		
		
		
		this.ma = this.getMatchAnalyzer(world, agent0, agent1);
		this.ma.runMatch(this.numMatches);
		
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
	
	protected MatchAnalizer getMatchAnalyzer(World w, SGQLAgent agent0, SGQLAgent agent1){
		
		MatchAnalizer ma = new MatchAnalyzerSimple(w, agent0, agent1, extractStatesFromQuery(agent0QueryStates), extractStatesFromQuery(agent1QueryStates));
		ma.setMaxStages(this.maxStage);
		return ma;
		
	}
	
	protected List <State> extractStatesFromQuery(List <StateActionSetTuple> queries){
		List <State> res = new ArrayList<State>(queries.size());
		for(StateActionSetTuple sas : queries){
			res.add(sas.s);
		}
		return res;
	}
	
	protected void computeAverage(){
		
		incStage.setEnabled(true);
		decStage.setEnabled(true);
		stageSlider.setEnabled(true);
		
		boolean firstTime = false;
		if(this.ma == null){
			firstTime = true;
		}
		
		double oldLearningRate = learningRate;
		
		
		learningRate = Double.parseDouble(lrField.getText());
		
		baseAgentFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		
		double [] a0SRParams = new double[]{Double.parseDouble(a0SField.getText()), Double.parseDouble(a0PField.getText())};
		double [] a1SRParams = new double[]{Double.parseDouble(a1SField.getText()), Double.parseDouble(a1PField.getText())};
		
		
		JointReward [] subjectiveRFStorage = new JointReward[2];
		
		double suma0 = 0.;
		double suma1 = 0.;
		double lasta0 = 0.;
		double lasta1 = 0.;
		int nTrials = 10;
		for(int i = 0; i < nTrials; i++){
			this.performMatch(a0SRParams, a1SRParams, subjectiveRFStorage);
			lasta0 = this.ma.getObjectiveCumulativeReward(0);
			lasta1 = this.ma.getObjectiveCumulativeReward(1);
			
			suma0 += lasta0;
			suma1 += lasta1;
		}
		
		
		
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
		
		this.a0AvgCumulativeReturn.setText(String.format("%.1f", suma0 / nTrials));
		this.a1AvgCumulativeReturn.setText(String.format("%.1f", suma1 / nTrials));
		
		
		this.qTableController.updateRFValues(subjectiveRFStorage[0], subjectiveRFStorage[1]);
		
		stage = 0;
		maxStage = this.ma.numQSpaceMeasure()-1;
		stageSlider.setMaximum(this.ma.numQSpaceMeasure()-1);
		stageSlider.setValue(0);
		
		
		this.oldA0Params = a0SRParams;
		this.oldA1Params = a1SRParams;
		
		this.updateStage();
		
		
	}
	
	protected void saveToFile(){
		
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
	
	protected void sliderChanged(){
		stage = stageSlider.getValue();
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
	
	
	
	protected List<StateActionSetTuple> getQueryStates(String [] agentNames, int playerId){
		
		List <StateActionSetTuple> res = new ArrayList<QTableVisController.StateActionSetTuple>();
		
		if(playerId == 0){
			
			State s0 = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 0, 0);
			StateActionSetTuple sas0 = new StateActionSetTuple(s0, "S0");
			sas0.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), ""), "F");
			sas0.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONSTEAL), ""), "S");
			res.add(sas0);
			
			State s0t = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 1, 0);
			StateActionSetTuple sas0t = new StateActionSetTuple(s0t, "S0T");
			sas0t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), ""), "F");
			sas0t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONSTEAL), ""), "S");
			res.add(sas0t);
			
			
			State s1 = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 0, 0);
			FSSimple.setStateNode(s1, 1);
			StateActionSetTuple sas1 = new StateActionSetTuple(s1, "S1");
			sas1.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), ""), "F");
			sas1.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONSTEAL), ""), "S");
			res.add(sas1);
			
			
			State s1t = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 1, 0);
			FSSimple.setStateNode(s1t, 1);
			StateActionSetTuple sas1t = new StateActionSetTuple(s1t, "S1T");
			sas1t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), ""), "F");
			sas1t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONSTEAL), ""), "S");
			res.add(sas1t);
			
			
			State s3 = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 0, 0);
			FSSimple.setStateNode(s3, 3);
			StateActionSetTuple sas3 = new StateActionSetTuple(s1, "S3");
			sas3.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), ""), "F");
			sas3.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONSTEAL), ""), "S");
			res.add(sas3);
			
			
			State s3t = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 1, 0);
			FSSimple.setStateNode(s3t, 3);
			StateActionSetTuple sas3t = new StateActionSetTuple(s3t, "S3T");
			sas3t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), ""), "F");
			sas3t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONSTEAL), ""), "S");
			res.add(sas3t);
			
			
			
		}
		else{
			
			State s2 = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 0, 0);
			FSSimple.setStateNode(s2, 2);
			StateActionSetTuple sas2 = new StateActionSetTuple(s2, "S2");
			sas2.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONPUNISH), ""), "P");
			sas2.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONDONOTHING), ""), "N");
			res.add(sas2);
			
			
			State s2t = FSSimple.getInitialState(this.domain, agentNames[0], agentNames[1], 0, 0);
			ObjectInstance p0 = s2t.getObject(agentNames[0]);
			p0.setValue(FSSimple.ATTBACKTURNED, 1);
			
			FSSimple.setStateNode(s2t, 2);
			StateActionSetTuple sas2t = new StateActionSetTuple(s2t, "S2T");
			sas2t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONPUNISH), ""), "P");
			sas2t.addAction(new GroundedSingleAction(agentNames[playerId], this.domain.getSingleAction(FSSimple.ACTIONDONOTHING), ""), "N");
			res.add(sas2t);
			
		}
		
		return res;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleMatchVisualizer smv = new SimpleMatchVisualizer();
		smv.initGUI();
		smv.pack();
		smv.setVisible(true);

	}

}
