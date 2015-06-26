package tests.irltests;

import behavior.burlapirlext.DifferentiableSparseSampling;
import behavior.burlapirlext.DifferentiableZeroStepPlanner;
import behavior.burlapirlext.diffvinit.DiffVFRF;
import behavior.burlapirlext.diffvinit.LinearDiffRFVInit;
import behavior.burlapirlext.diffvinit.LinearStateDiffVF;
import behavior.training.supervised.WekaPolicy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.StateValuePainter;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateActionDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;
import demos.aiclass.IRLExample;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class GeneralPuddlesIRL {

	protected GridWorldDomain gwd;
	protected Domain domain;
	protected Visualizer v;
	protected StateParser sp;

	protected State initialState;
	protected int [][] puddleMap;

	protected String expertDir = "oomdpResearch/irlGP";
	protected String trainedDir = "oomdpResearch/irlGPTrained";


	public GeneralPuddlesIRL(){

		this.gwd = new GridWorldDomain(30, 30);
		this.gwd.setNumberOfLocationTypes(5);
		this.domain = gwd.generateDomain();

		this.puddleMap = this.generateCellMap(0, 0, 20, 20, 0, 56, 0.25);
		this.initialState = GridWorldDomain.getOneAgentNoLocationState(this.domain);
		GridWorldDomain.setAgent(this.initialState, 0, 0);

		//this.initialState = this.generateStateWithLocations(0, 0, 20, 20, 0, 56, 0.25);
		this.v = GridWorldVisualizer.getVisualizer(this.gwd.getMap());
		this.v.addStaticPainter(new PMapPainter(this.puddleMap));

		this.sp = new GridWorldStateParser(this.domain);

	}


	public void launchExplorer(){

		VisualExplorer exp = new VisualExplorer(domain, v, initialState);

		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);

		StateParser sp = new GridWorldStateParser(this.domain);

		exp.enableEpisodeRecording("r", "f", new NullRewardFunction(), expertDir, sp);


		//final PuddleMapFVComponent fvg = new PuddleMapFVComponent(this.puddleMap, 5, 20, 20);
		final PuddleMapFV fvg = new PuddleMapFV(this.puddleMap, 5, 20, 20);
		ActionObserver obs = new ActionObserver() {
			@Override
			public void actionEvent(State s, GroundedAction ga, State sp) {
				double [] vec = fvg.generateFeatureVectorFrom(sp);
				System.out.println(Arrays.toString(vec));
			}
		};
		((SADomain)this.domain).addActionObserverForAllAction(obs);

		exp.initGUI();

	}


	public void launchSavedEpisodeViewer(){
		new EpisodeSequenceVisualizer(this.v, this.domain, this.sp, this.expertDir);
	}


	public void generateExpertTrajectories(){

		PuddleMapFV fvgen = new PuddleMapFV(this.puddleMap, 5, 20, 20);
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		rf.setParameters(new double[]{1., -10, -10, 0, -10, 0, 0, 0, 0, 0});
		TerminalFunction tf = new GridWorldTerminalFunction(20, 20);

		ValueIteration vi = new ValueIteration(this.domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0.01, 500);
		vi.planFromState(this.initialState);

		System.out.println("Rolling out episodes");
		Policy p = new GreedyQPolicy(vi);

		EpisodeAnalysis e1 = p.evaluateBehavior(this.initialState, rf, tf, 100);

		State s2 = this.initialState.copy();
		GridWorldDomain.setAgent(s2, 3, 14);
		EpisodeAnalysis e2 = p.evaluateBehavior(s2, rf, tf, 100);

		State s3 = this.initialState.copy();
		GridWorldDomain.setAgent(s3, 11, 27);
		EpisodeAnalysis e3 = p.evaluateBehavior(s3, rf, tf, 100);

		State s4 = this.initialState.copy();
		GridWorldDomain.setAgent(s4, 28, 6);
		EpisodeAnalysis e4 = p.evaluateBehavior(s4, rf, tf, 100);


		e1.writeToFile(this.expertDir + "/ex1", this.sp);
		e2.writeToFile(this.expertDir + "/ex2", this.sp);
		e3.writeToFile(this.expertDir + "/ex3", this.sp);
		e4.writeToFile(this.expertDir + "/ex4", this.sp);



	}

	public void runIRL(){

		MyTimer timer = new MyTimer();
		timer.start();

		PuddleMapFV ofvgen = new PuddleMapFV(this.puddleMap, 5, 20, 20);

		//PuddleMapFV fvgen = new PuddleMapFV(this.puddleMap, 5, 20, 20);
		PuddleMapFVComponent fvgen = new PuddleMapFVComponent(this.puddleMap, 5, 20, 20);
		//StateToFeatureVectorGenerator fvgen = new ConcatenatedObjectFeatureVectorGenerator(false, GridWorldDomain.CLASSAGENT);
		//PuddleMapExactFV fvgen = new PuddleMapExactFV(this.puddleMap, 5);
		//LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		LinearStateActionDifferentiableRF rf = new LinearStateActionDifferentiableRF(fvgen, fvgen.getDim(),
				new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONNORTH), ""),
				new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONSOUTH), ""),
				new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONEAST), ""),
				new GroundedAction(this.domain.getAction(GridWorldDomain.ACTIONWEST), ""));

		LinearStateDifferentiableRF objectiveRF = new LinearStateDifferentiableRF(ofvgen, ofvgen.getDim());
		objectiveRF.setParameters(new double[]{1., -10, -10, 0, -10, 0, 0, 0, 0, 0});
		//objectiveRF.setParameters(new double[]{1., -10, -10, 0, -10});

		java.util.List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, this.sp);


		int depth = 6;
		double beta = 10;
		//DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, new NullTermination(), 0.99, new NameDependentStateHashFactory(), depth, -1, beta);
		DifferentiableZeroStepPlanner dss = new DifferentiableZeroStepPlanner(domain, rf);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, eas, rf);
		request.setBoltzmannBeta(beta);

		//MLIRL irl = new MLIRL(request, 0.001, 0.01, 10); //use this for only the given features
		//MLIRL irl = new MLIRL(request, 0.00001, 0.01, 10);
		MLIRL irl = new MLIRL(request, 0.0001, 0.01, 10);

		irl.performIRL();

		//System.out.println(this.getFVAndShapeString(rf.getParameters()));

		timer.stop();
		System.out.println("Training time: " + timer.getTime());

		/*//uncomment to run test examples

		String baseName = "RH45";

		Policy p = new GreedyQPolicy(dss);
		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(20, 20);

		State simple = this.initialState.copy();
		GridWorldDomain.setAgent(simple, 18, 0);
		EpisodeAnalysis trainedEp1 = p.evaluateBehavior(simple, objectiveRF, tf, 200);
		trainedEp1.writeToFile(trainedDir+"/IRL" + baseName + "EpSimple", this.sp);



		State hardAgent = this.initialState.copy();
		GridWorldDomain.setAgent(hardAgent, 0, 9);
		EpisodeAnalysis trainedEp2 = p.evaluateBehavior(hardAgent, objectiveRF, tf, 200);
		trainedEp2.writeToFile(trainedDir+"/IRL" + baseName + "EpHardAgent", this.sp);



		dss.resetPlannerResults();

		int ngx = 12;
		int ngy = 14;

		tf = new GridWorldTerminalFunction(ngx, ngy);
		this.puddleMap[ngx][ngy] = 1;
		this.puddleMap[20][20] = 0;
		//fvgen.setGoal(ngx, ngy);
		State hardGoal = this.initialState.copy();
		GridWorldDomain.setAgent(hardGoal, 0, 0);



		EpisodeAnalysis trainedEp3 = p.evaluateBehavior(hardGoal, objectiveRF, tf, 200);
		trainedEp3.writeToFile(trainedDir+"/IRL" + baseName + "EpHardGoal", this.sp);



		new EpisodeSequenceVisualizer(this.v, this.domain, this.sp, this.trainedDir);
		*/

	}




	public void runVFIRL(){


		PuddleMapExactFV fvgen = new PuddleMapExactFV(this.puddleMap, 5);
		PuddleMapDistOnlyFV vfFvGen = new PuddleMapDistOnlyFV(this.puddleMap, 5, 20, 20);
		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(20, 20);
		LinearStateDifferentiableRF objectiveRF = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		objectiveRF.setParameters(new double[]{1., -10, -10, 0, -10});
		LinearStateDiffVF vinit = new LinearStateDiffVF(vfFvGen, 5);

		DiffVFRF rf = new DiffVFRF(objectiveRF, vinit);

		java.util.List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, this.sp);


		int depth = 6;
		double beta = 10;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, tf, 0.99, new NameDependentStateHashFactory(), depth, -1, beta);
		dss.setValueForLeafNodes(vinit);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, eas, rf);
		request.setBoltzmannBeta(beta);

		MLIRL irl = new MLIRL(request, 0.001, 0.01, 10); //use this for only the given features
		//MLIRL irl = new MLIRL(request, 0.00001, 0.01, 10);
		//MLIRL irl = new MLIRL(request, 0.0001, 0.01, 10);

		irl.performIRL();

		//System.out.println(this.getFVAndShapeString(rf.getParameters()));

		String baseName = "VD5";

		Policy p = new GreedyQPolicy(dss);


		State simple = this.initialState.copy();
		GridWorldDomain.setAgent(simple, 18, 0);
		EpisodeAnalysis trainedEp1 = p.evaluateBehavior(simple, objectiveRF, tf, 200);
		trainedEp1.writeToFile(trainedDir+"/IRL" + baseName + "EpSimple", this.sp);



		State hardAgent = this.initialState.copy();
		GridWorldDomain.setAgent(hardAgent, 0, 9);
		EpisodeAnalysis trainedEp2 = p.evaluateBehavior(hardAgent, objectiveRF, tf, 200);
		trainedEp2.writeToFile(trainedDir+"/IRL" + baseName + "EpHardAgent", this.sp);



		dss.resetPlannerResults();

		int ngx = 12;
		int ngy = 14;

		tf = new GridWorldTerminalFunction(ngx, ngy);
		dss.setTf(tf);
		this.puddleMap[ngx][ngy] = 1;
		this.puddleMap[20][20] = 0;
		vfFvGen.setGoal(ngx, ngy);
		State hardGoal = this.initialState.copy();
		GridWorldDomain.setAgent(hardGoal, 0, 0);



		EpisodeAnalysis trainedEp3 = p.evaluateBehavior(hardGoal, objectiveRF, tf, 200);
		trainedEp3.writeToFile(trainedDir+"/IRL" + baseName + "EpHardGoal", this.sp);



		new EpisodeSequenceVisualizer(this.v, this.domain, this.sp, this.trainedDir);


	}





	public void runVFRFIRL(){


		PuddleMapExactFV fvgen = new PuddleMapExactFV(this.puddleMap, 5);
		PuddleMapDistOnlyFV vfFvGen = new PuddleMapDistOnlyFV(this.puddleMap, 5, 20, 20);
		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(20, 20);
		LinearStateDifferentiableRF objectiveRF = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		objectiveRF.setParameters(new double[]{1., -10, -10, 0, -10});
		//LinearStateDiffVF vinit = new LinearStateDiffVF(vfFvGen, 5);

		//DiffVFRF rf = new DiffVFRF(objectiveRF, vinit);
		LinearDiffRFVInit rfvf = new LinearDiffRFVInit(fvgen, vfFvGen, 5, 5);

		java.util.List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, this.sp);


		int depth = 4;
		double beta = 10;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rfvf, new NullTermination(), 0.99, new NameDependentStateHashFactory(), depth, -1, beta);
		dss.setValueForLeafNodes(rfvf);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, eas, rfvf);
		request.setBoltzmannBeta(beta);

		MLIRL irl = new MLIRL(request, 0.001, 0.01, 10); //use this for only the given features
		//MLIRL irl = new MLIRL(request, 0.00001, 0.01, 10);
		//MLIRL irl = new MLIRL(request, 0.0001, 0.01, 10);

		irl.performIRL();

		//System.out.println(this.getFVAndShapeString(rf.getParameters()));

		String baseName = "SSRFVFD3";

		SparseSampling ss = new SparseSampling(domain, rfvf, new NullTermination(), 0.99, new NameDependentStateHashFactory(), depth, -1);
		ss.toggleDebugPrinting(false);
		ss.setValueForLeafNodes(rfvf);


		//Policy p = new GreedyQPolicy(dss);
		Policy p = new GreedyQPolicy(ss);


		State simple = this.initialState.copy();
		GridWorldDomain.setAgent(simple, 18, 0);
		EpisodeAnalysis trainedEp1 = p.evaluateBehavior(simple, objectiveRF, tf, 200);
		trainedEp1.writeToFile(trainedDir+"/IRL" + baseName + "EpSimple", this.sp);



		State hardAgent = this.initialState.copy();
		GridWorldDomain.setAgent(hardAgent, 0, 9);
		EpisodeAnalysis trainedEp2 = p.evaluateBehavior(hardAgent, objectiveRF, tf, 200);
		trainedEp2.writeToFile(trainedDir+"/IRL" + baseName + "EpHardAgent", this.sp);



		dss.resetPlannerResults();
		ss.resetPlannerResults();

		int ngx = 12;
		int ngy = 14;

		tf = new GridWorldTerminalFunction(ngx, ngy);
		//dss.setTf(tf);
		this.puddleMap[ngx][ngy] = 1;
		this.puddleMap[20][20] = 0;
		vfFvGen.setGoal(ngx, ngy);
		State hardGoal = this.initialState.copy();
		GridWorldDomain.setAgent(hardGoal, 0, 0);



		EpisodeAnalysis trainedEp3 = p.evaluateBehavior(hardGoal, objectiveRF, tf, 200);
		trainedEp3.writeToFile(trainedDir+"/IRL" + baseName + "EpHardGoal", this.sp);



		new EpisodeSequenceVisualizer(this.v, this.domain, this.sp, this.trainedDir);


	}













	public void runSupervised(){

		MyTimer timer = new MyTimer();
		timer.start();

		PuddleMapFV agentfv = new PuddleMapFV(this.puddleMap, 5, 20, 20);
		PuddleMapFVComponent agentCompFV = new PuddleMapFVComponent(this.puddleMap, 5, 20, 20);
		StateToFeatureVectorGenerator svar = new ConcatenatedObjectFeatureVectorGenerator(false, GridWorldDomain.CLASSAGENT);
		java.util.List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, this.sp);
		LinearStateDifferentiableRF objectiveRF = new LinearStateDifferentiableRF(agentfv, agentfv.getDim());
		objectiveRF.setParameters(new double[]{1., -10, -10, 0, -10, 0, 0, 0, 0, 0});

		WekaPolicy p = new WekaPolicy(agentfv, new J48(), this.domain.getActions(), eas);
		//WekaPolicy p = new WekaPolicy(svar, new Logistic(), this.domain.getActions(), eas);

		timer.stop();

		System.out.println("Training Time: " + timer.getTime());

		String baseName = "Svar";

		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(20, 20);

		State simple = this.initialState.copy();
		GridWorldDomain.setAgent(simple, 18, 0);
		EpisodeAnalysis trainedEp1 = p.evaluateBehavior(simple, objectiveRF, tf, 200);
		trainedEp1.writeToFile(trainedDir+"/j48" + baseName + "EpSimple", this.sp);


		State hardAgent = this.initialState.copy();
		GridWorldDomain.setAgent(hardAgent, 0, 9);
		EpisodeAnalysis trainedEp2 = p.evaluateBehavior(hardAgent, objectiveRF, tf, 200);
		trainedEp2.writeToFile(trainedDir+"/j48" + baseName + "EpHardAgent", this.sp);


		int ngx = 12;
		int ngy = 14;
		tf = new GridWorldTerminalFunction(ngx, ngy);
		this.puddleMap[ngx][ngy] = 1;
		this.puddleMap[20][20] = 0;
		agentfv.setGoal(ngx, ngy);
		agentCompFV.setGoal(ngx, ngy);
		State hardGoal = this.initialState.copy();
		GridWorldDomain.setAgent(hardGoal, 0, 0);


		EpisodeAnalysis trainedEp3 = p.evaluateBehavior(hardGoal, objectiveRF, tf, 200);
		trainedEp3.writeToFile(trainedDir+"/j48" + baseName + "EpHardGoal", this.sp);


		new EpisodeSequenceVisualizer(this.v, this.domain, this.sp, this.trainedDir);

	}

	public void visualizeFunctions(){

		PuddleMapFV fvgen = new PuddleMapFV(this.puddleMap, 5, 20, 20);
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		LinearStateDifferentiableRF objectiveRF = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		objectiveRF.setParameters(new double[]{1., -10, -10, 0, -10, 0, 0, 0, 0, 0});

		java.util.List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, this.sp);

		int depth = 6;
		double beta = 10;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, new NullTermination(), 0.99, new NameDependentStateHashFactory(), depth, -1, beta);
		//DifferentiableZeroStepPlanner dss = new DifferentiableZeroStepPlanner(domain, rf);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, eas, rf);
		request.setBoltzmannBeta(beta);

		//MLIRL irl = new MLIRL(request, 0.001, 0.01, 10); //use this for only the given features
		MLIRL irl = new MLIRL(request, 0.00001, 0.01, 10);
		//MLIRL irl = new MLIRL(request, 0.0001, 0.01, 10);

		irl.performIRL();

		TerminalFunction tf = new GridWorldTerminalFunction(20, 20);

		ValueIteration vi = new ValueIteration(this.domain, objectiveRF, new NullTermination(), 0.99, new DiscreteStateHashFactory(), 0.01, 200);
		//vi.planFromState(this.initialState);

		SparseSampling ssLearned = new SparseSampling(this.domain, request.getRf(), new NullTermination(), 0.99, new DiscreteStateHashFactory(), depth, -1);
		SparseSampling ssObjective = new SparseSampling(this.domain, objectiveRF, new NullTermination(), 0.99, new DiscreteStateHashFactory(), depth, -1);

		StateRewardFunctionValue objectiveRFVis = new StateRewardFunctionValue(this.domain, objectiveRF);
		StateRewardFunctionValue learnedRFVis = new StateRewardFunctionValue(this.domain, rf);

		List<State> allStates = StateReachability.getReachableStates(this.initialState, (SADomain)this.domain, new DiscreteStateHashFactory());

		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, ssObjective, null);
		StateValuePainter2D vpainter = (StateValuePainter2D)gui.getSvp();
		vpainter.toggleValueStringRendering(false);
		LandmarkColorBlendInterpolation colorBlend = new LandmarkColorBlendInterpolation();
		colorBlend.addNextLandMark(0., Color.BLACK);
		colorBlend.addNextLandMark(1., Color.WHITE);
		vpainter.setColorBlend(colorBlend);
		gui.initGUI();




	}


	public void launchTrainedViewer(){

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(this.trainedDir, this.domain, this.sp);
		for(int i = 0; i < episodes.size(); i++){
			System.out.println(i + " " + (episodes.get(i).numTimeSteps()-1));
		}

		new EpisodeSequenceVisualizer(this.v, this.domain, this.sp, this.trainedDir);

	}

	public void launchTrajectoryRenderer(){

		Visualizer tv = this.getTrajectoryRenderLayerBase();
		List <EpisodeAnalysis> trajectories = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, this.domain, this.sp);
		TrajectoryRenderer tr = new TrajectoryRenderer(trajectories, GridWorldDomain.CLASSAGENT, GridWorldDomain.ATTX, GridWorldDomain.ATTY, new double[]{0, 30, .5}, new double[]{0, 30, .5}, 3.f, 10.f);
		tv.addRenderLayer(tr);

		tv.updateState(trajectories.get(0).getState(0));

		JFrame frame = new JFrame();
		frame.setPreferredSize(new Dimension(800, 800));
		frame.getContentPane().add(tv);
		frame.pack();
		frame.setVisible(true);

	}

	protected Visualizer getTrajectoryRenderLayerBase(){
		StateRenderLayer srl = new StateRenderLayer();
		srl.addStaticPainter(new PMapPainter(this.puddleMap));
		Visualizer v = new Visualizer(srl);
		return v;
	}


	protected String getFVAndShapeString(double [] vec){
		StringBuilder s = new StringBuilder();
		int w = 15;
		s.append(String.format("%-"+w+"s: %4f\n", "in blue", vec[0]));
		s.append(String.format("%-"+w+"s: %4f\n", "in red", vec[1]));
		s.append(String.format("%-"+w+"s: %4f\n", "in green", vec[2]));
		s.append(String.format("%-"+w+"s: %4f\n", "in yellow", vec[3]));
		s.append(String.format("%-"+w+"s: %4f\n", "in purple", vec[4]));

		s.append(String.format("%-"+w+"s: %4f\n", "dist blue", vec[5]));
		s.append(String.format("%-"+w+"s: %4f\n", "dist red", vec[6]));
		s.append(String.format("%-"+w+"s: %4f\n", "dist green", vec[7]));
		s.append(String.format("%-"+w+"s: %4f\n", "dist yellow", vec[8]));
		s.append(String.format("%-"+w+"s: %4f\n", "dist purple", vec[9]));



		return s.toString();
	}

	protected int [][] generateCellMap(int ax, int ay, int gx, int gy, int gt, int seed, double p){

		int w = 30;
		int h = 30;
		int [][] map = new int[w][h];

		map[gx][gy] = gt+1;

		Random rand = new Random(seed);

		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				if((x == ax && y == ay) || (x == gx && y == gy)){
					continue;
				}
				double roll = rand.nextDouble();
				if(roll < p){
					map[x][y] = this.chooseType(rand, gt);
				}

			}
		}


		return map;

	}

	protected State generateStateWithLocations(int ax, int ay, int gx, int gy, int gt, int seed, double p){

		int w = 30;
		int h = 30;
		int [][] map = new int[w][h];

		map[gx][gy] = gt;

		Random rand = new Random(seed);

		int numLocations = 1;
		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				if((x == ax && y == ay) || (x == gx && y == gy)){
					continue;
				}
				double roll = rand.nextDouble();
				if(roll < p){
					map[x][y] = this.chooseType(rand, gt);
					numLocations++;
				}

			}
		}


		State s = GridWorldDomain.getOneAgentNLocationState(domain, numLocations);
		GridWorldDomain.setAgent(s, ax, ay);
		GridWorldDomain.setLocation(s, 0, gx, gy, gt);

		int locInd = 1;
		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				if(map[x][y] != 0){
					GridWorldDomain.setLocation(s, locInd, x, y, map[x][y]-1);
					locInd++;
				}

			}
		}



		return s;

	}

	protected int chooseType(Random rand, int gt){
		int type = gt;
		while(type == gt){
			type = rand.nextInt(5);
		}
		return type+1;
	}






	public static class PMapPainter implements StaticPainter {

		protected int 				dwidth;
		protected int 				dheight;
		protected int [][] 			map;
		protected Color[]			cols = new Color[]{Color.blue, Color.red, Color.green, Color.yellow, Color.magenta, Color.orange};


		/**
		 * Initializes for the domain and wall map
		 * @param map the wall map matrix where 1s indicate a wall in that cell and 0s indicate it is clear of walls
		 */
		public PMapPainter(int [][] map) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
		}

		@Override
		public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {




			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;

			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;

			//pass through each cell of the map and if it is a wall, draw it
			for(int i = 0; i < this.dwidth; i++){
				for(int j = 0; j < this.dheight; j++){


					if(this.map[i][j] > 0){

						float rx = i*width;
						float ry = cHeight - height - j*height;

						//draw the walls; make them black
						g2.setColor(this.cols[this.map[i][j]-1]);

						g2.fill(new Rectangle2D.Float(rx, ry, width, height));

					}


				}
			}

		}




	}


	public static class PuddleMapFVComponent implements StateToFeatureVectorGenerator{


		int numCells;
		int [][] map;
		int gx;
		int gy;


		public PuddleMapFVComponent(int[][] map, int numCells, int gx, int gy){
			this.map = map;
			this.numCells = numCells;
			this.gx = gx;
			this.gy = gy;
		}


		public void setMap(int [][] map){
			this.map = map;
		}

		public void setGoal(int gx, int gy){
			this.gx = gx;
			this.gy = gy;
		}

		public int getDim(){
			return numCells * 3;
		}


		@Override
		public double[] generateFeatureVectorFrom(State s) {

			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);

			double [] vec = new double[this.getDim()];

			if(this.map[ax][ay] > 0){
				vec[map[ax][ay]-1] = 1.;
			}

			//now do distances
			//first seed to max val
			for(int i = this.numCells; i < vec.length; i++){
				vec[i] = 31.;
			}

			//set goal (type 0) to its goal position assuming only 1 instance of it, so we don't scan large distances for it
			if(this.gx != -1) {
				vec[this.numCells] = this.gx - ax;
				vec[this.numCells * 2] = this.gy - ay;
			}

			//now do scan
			for(int r = 0; r < 16; r++){

				int x;

				//scan top
				int y = ay + r;
				if(y < 30){
					for(x = Math.max(ax-r, 0); x <= Math.min(ax+r, 29); x++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan bottom
				y = ay - r;
				if(y > -1){
					for(x = Math.max(ax-r, 0); x <= Math.min(ax+r, 29); x++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan left
				x = ax - r;
				if(x > -1){
					for(y = Math.max(ay-r, 0); y <= Math.min(ay+r, 29); y++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan right
				x = ax + r;
				if(x < 30){
					for(y = Math.max(ay-r, 0); y <= Math.min(ay+r, 29); y++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}


				if(this.foundNearestForAll(vec)){
					break;
				}
			}




			return vec;
		}

		protected void updateNearest(double [] vec, int ax, int ay, int x, int y){
			int type = this.map[x][y]-1;
			if(type > -1){

				int dx = x - ax;
				int dy = y - ay;

				int ix = this.numCells + type;
				int iy = this.numCells*2 + type;

				if(Math.abs(dx) + Math.abs(dy) < (int)(Math.abs(vec[ix]) + Math.abs(vec[iy]))){
					vec[ix] = dx;
					vec[iy] = dy;
				}

			}
		}

		protected boolean foundNearestForAll(double [] vec){
			for(int i = this.numCells; i < this.numCells*2; i++){
				if(vec[i] > 30){
					return false;
				}
			}
			return true;
		}
	}






	public static class PuddleMapFV implements StateToFeatureVectorGenerator{


		int numCells;
		int [][] map;
		int gx;
		int gy;


		public PuddleMapFV(int[][] map, int numCells, int gx, int gy){
			this.map = map;
			this.numCells = numCells;
			this.gx = gx;
			this.gy = gy;
		}


		public void setMap(int [][] map){
			this.map = map;
		}

		public void setGoal(int gx, int gy){
			this.gx = gx;
			this.gy = gy;
		}

		public int getDim(){
			return numCells * 2;
		}


		@Override
		public double[] generateFeatureVectorFrom(State s) {

			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);

			double [] vec = new double[this.getDim()];

			if(this.map[ax][ay] > 0){
				vec[map[ax][ay]-1] = 1.;
			}

			//now do distances
			//first seed to max val
			for(int i = this.numCells; i < vec.length; i++){
				vec[i] = 61.;
			}

			//set goal (type 0) to its goal position assuming only 1 instance of it, so we don't scan large distances for it
			if(this.gx != -1) {
				vec[this.numCells] = Math.abs(this.gx - ax) + Math.abs(this.gy - ay);
			}

			//now do scan
			for(int r = 0; r < 16; r++){

				int x;

				//scan top
				int y = ay + r;
				if(y < 30){
					for(x = Math.max(ax-r, 0); x <= Math.min(ax+r, 29); x++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan bottom
				y = ay - r;
				if(y > -1){
					for(x = Math.max(ax-r, 0); x <= Math.min(ax+r, 29); x++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan left
				x = ax - r;
				if(x > -1){
					for(y = Math.max(ay-r, 0); y <= Math.min(ay+r, 29); y++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan right
				x = ax + r;
				if(x < 30){
					for(y = Math.max(ay-r, 0); y <= Math.min(ay+r, 29); y++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}


				if(this.foundNearestForAll(vec)){
					break;
				}
			}




			return vec;
		}

		protected void updateNearest(double [] vec, int ax, int ay, int x, int y){
			int type = this.map[x][y]-1;
			if(type > -1){

				int dx = x - ax;
				int dy = y - ay;
				int d = Math.abs(dx) + Math.abs(dy);

				int ind = this.numCells + type;

				if(d < vec[ind]){
					vec[ind] = d;
				}

			}
		}

		protected boolean foundNearestForAll(double [] vec){
			for(int i = this.numCells; i < this.numCells*2; i++){
				if(vec[i] > 30){
					return false;
				}
			}
			return true;
		}
	}



	public static class PuddleMapDistOnlyFV implements StateToFeatureVectorGenerator{


		int numCells;
		int [][] map;
		int gx;
		int gy;


		public PuddleMapDistOnlyFV(int[][] map, int numCells, int gx, int gy){
			this.map = map;
			this.numCells = numCells;
			this.gx = gx;
			this.gy = gy;
		}


		public void setMap(int [][] map){
			this.map = map;
		}

		public void setGoal(int gx, int gy){
			this.gx = gx;
			this.gy = gy;
		}

		public int getDim(){
			return numCells;
		}


		@Override
		public double[] generateFeatureVectorFrom(State s) {

			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);

			double [] vec = new double[this.getDim()];


			//now do distances
			//first seed to max val
			for(int i = 0; i < vec.length; i++){
				vec[i] = 61.;
			}

			//set goal (type 0) to its goal position assuming only 1 instance of it, so we don't scan large distances for it
			if(this.gx != -1) {
				vec[0] = Math.abs(this.gx - ax) + Math.abs(this.gy - ay);
			}

			//now do scan
			for(int r = 0; r < 16; r++){

				int x;

				//scan top
				int y = ay + r;
				if(y < 30){
					for(x = Math.max(ax-r, 0); x <= Math.min(ax+r, 29); x++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan bottom
				y = ay - r;
				if(y > -1){
					for(x = Math.max(ax-r, 0); x <= Math.min(ax+r, 29); x++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan left
				x = ax - r;
				if(x > -1){
					for(y = Math.max(ay-r, 0); y <= Math.min(ay+r, 29); y++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}

				//scan right
				x = ax + r;
				if(x < 30){
					for(y = Math.max(ay-r, 0); y <= Math.min(ay+r, 29); y++){
						this.updateNearest(vec, ax, ay, x, y);
					}
				}


				if(this.foundNearestForAll(vec)){
					break;
				}
			}




			return vec;
		}

		protected void updateNearest(double [] vec, int ax, int ay, int x, int y){
			int type = this.map[x][y]-1;
			if(type > -1){

				int dx = x - ax;
				int dy = y - ay;
				int d = Math.abs(dx) + Math.abs(dy);

				int ind = type;

				if(d < vec[ind]){
					vec[ind] = d;
				}

			}
		}

		protected boolean foundNearestForAll(double [] vec){
			for(int i = 0; i < this.numCells; i++){
				if(vec[i] > 30){
					return false;
				}
			}
			return true;
		}
	}



	public static class PuddleMapExactFV implements StateToFeatureVectorGenerator{


		int numCells;
		int [][] map;


		public PuddleMapExactFV(int[][] map, int numCells){
			this.map = map;
			this.numCells = numCells;
		}


		public void setMap(int [][] map){
			this.map = map;
		}


		public int getDim(){
			return numCells;
		}


		@Override
		public double[] generateFeatureVectorFrom(State s) {

			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getIntValForAttribute(GridWorldDomain.ATTY);

			double [] vec = new double[this.getDim()];

			if(this.map[ax][ay] > 0){
				vec[map[ax][ay]-1] = 1.;
			}

			return vec;
		}

	}

















	public static void main(String[] args) {
		GeneralPuddlesIRL exp = new GeneralPuddlesIRL();

		//exp.launchExplorer();
		//exp.generateExpertTrajectories();
		//exp.launchSavedEpisodeViewer();
		//exp.runIRL();
		//exp.runSupervised();
		//exp.runVFIRL();
		//exp.runVFRFIRL();

		//exp.launchTrajectoryRenderer();

		//exp.launchTrainedViewer();
		exp.visualizeFunctions();
	}

}
