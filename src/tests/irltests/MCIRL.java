package tests.irltests;

import behavior.burlapirlext.DifferentiableSparseSampling;
import behavior.burlapirlext.DifferentiableZeroStepPlanner;
import behavior.burlapirlext.diffvinit.DiffVFRF;
import behavior.burlapirlext.diffvinit.LinearDiffRFVInit;
import behavior.burlapirlext.diffvinit.LinearStateDiffVF;
import behavior.training.supervised.SupervisedRHC;
import behavior.training.supervised.WekaPolicy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateActionDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.lunarlander.LLStateParser;
import burlap.domain.singleagent.lunarlander.LunarLanderTF;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarStateParser;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.Visualizer;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class MCIRL {

	MountainCar mcg;
	Domain domain;
	Visualizer v;
	State initialState;
	StateParser sp;

	protected String expertDir = "oomdpResearch/irlMC";
	protected String trainedDir = "oomdpResearch/irlMCTrained";


	public MCIRL(){
		this.mcg = new MountainCar();
		this.domain = mcg.generateDomain();

		this.v = MountainCarVisualizer.getVisualizer(this.mcg);

		this.initialState = this.mcg.getCleanState(domain);

		this.sp = new MountainCarStateParser(domain);
	}


	public void launchExplorer(){
		VisualExplorer exp = new VisualExplorer(domain, v, initialState);

		exp.addKeyAction("d", MountainCar.ACTIONFORWARD);
		exp.addKeyAction("a", MountainCar.ACTIONBACKWARDS);
		exp.addKeyAction("s", MountainCar.ACTIONCOAST);

		exp.enableEpisodeRecording("r", "f", new NullRewardFunction(), expertDir, sp);

		exp.initGUI();

	}

	public void launchSavedEpisodeViewer(){
		new EpisodeSequenceVisualizer(v, domain, sp, expertDir);
	}


	public void runIRL(){

		MyTimer timer = new MyTimer();
		timer.start();

		MCFVGen fvgen = new MCFVGen();

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(expertDir, domain, sp);

		System.out.println("expert size: " + episodes.get(0).numTimeSteps());
		System.out.println(episodes.get(0).getActionSequenceString(" "));

		/*
		EpisodeAnalysis ea = episodes.get(0);
		for(State s : ea.stateSequence){
			System.out.println(Arrays.toString(fvgen.generateFeatureVectorFrom(s)));
		}
		*/

		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.dim());
		/*LinearStateActionDifferentiableRF rf = new LinearStateActionDifferentiableRF(fvgen, fvgen.dim(),
				new GroundedAction(domain.getAction(MountainCar.ACTIONFORWARD), ""),
				new GroundedAction(domain.getAction(MountainCar.ACTIONBACKWARDS), ""),
				new GroundedAction(domain.getAction(MountainCar.ACTIONCOAST), ""));*/


		int depth = 4;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, new NullTermination(), 1., new NameDependentStateHashFactory(), depth, -1, 20);
		dss.toggleDebugPrinting(false);

		DifferentiableZeroStepPlanner zsp = new DifferentiableZeroStepPlanner(domain, rf);


		MLIRLRequest request = new MLIRLRequest(domain, dss, episodes, rf);
		//MLIRLRequest request = new MLIRLRequest(domain, zsp, episodes, rf);
		request.setBoltzmannBeta(5.);

		MLIRL irl = new MLIRL(request, 0.1, 0.01, 15);



		irl.performIRL();

		timer.stop();
		System.out.println("Trainign time: " + timer.getTime());


		/* //uncomment to run tests
		Policy p = new GreedyQPolicy(dss);
		//Policy p = new GreedyQPolicy(zsp);

		State ns = this.initialState.copy();
		//MountainCar.setAgent(ns, ns.getFirstObjectOfClass(MountainCar.CLASSAGENT).getRealValForAttribute(MountainCar.ATTX)+0.2, 0.);

		System.out.println("starting episode...");
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(ns, new NullRewardFunction(), this.mcg.new ClassicMCTF(), 500);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		trainedEpisode.writeToFile(trainedDir+"/easyRH4", sp);


		new EpisodeSequenceVisualizer(v, domain, sp, trainedDir);
		*/

	}


	public void runVFIRL(){
		MCFVGen fvgen = new MCFVGen();

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(expertDir, domain, sp);

		System.out.println("expert size: " + episodes.get(0).numTimeSteps());
		System.out.println(episodes.get(0).getActionSequenceString(" "));

		/*
		EpisodeAnalysis ea = episodes.get(0);
		for(State s : ea.stateSequence){
			System.out.println(Arrays.toString(fvgen.generateFeatureVectorFrom(s)));
		}
		*/

		TerminalFunction tf = new MountainCar.ClassicMCTF();
		LinearStateDiffVF vf = new LinearStateDiffVF(fvgen, fvgen.dim());
		RewardFunction objectiveRF = new UniformCostRF();
		DiffVFRF rf = new DiffVFRF(objectiveRF, vf);

		//LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.dim());
		/*LinearStateActionDifferentiableRF rf = new LinearStateActionDifferentiableRF(fvgen, fvgen.dim(),
				new GroundedAction(domain.getAction(MountainCar.ACTIONFORWARD), ""),
				new GroundedAction(domain.getAction(MountainCar.ACTIONBACKWARDS), ""),
				new GroundedAction(domain.getAction(MountainCar.ACTIONCOAST), ""));*/


		int depth = 4;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, tf, 1., new NameDependentStateHashFactory(), depth, -1, 20);
		dss.toggleDebugPrinting(false);
		dss.setValueForLeafNodes(vf);

		DifferentiableZeroStepPlanner zsp = new DifferentiableZeroStepPlanner(domain, rf);


		MLIRLRequest request = new MLIRLRequest(domain, dss, episodes, rf);
		//MLIRLRequest request = new MLIRLRequest(domain, zsp, episodes, rf);
		request.setBoltzmannBeta(5.);

		MLIRL irl = new MLIRL(request, 0.1, 0.01, 15);



		irl.performIRL();

		Policy p = new GreedyQPolicy(dss);
		//Policy p = new GreedyQPolicy(zsp);

		State ns = this.initialState.copy();
		//MountainCar.setAgent(ns, ns.getFirstObjectOfClass(MountainCar.CLASSAGENT).getRealValForAttribute(MountainCar.ATTX)+0.2, 0.);



		System.out.println("starting episode...");
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(ns, new NullRewardFunction(), tf, 500);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		trainedEpisode.writeToFile(trainedDir+"/hardIRLVFD4", sp);


		new EpisodeSequenceVisualizer(v, domain, sp, trainedDir);

	}






	public void visLearnedRF(){

		MCFVGen fvgen = new MCFVGen();
		LinearStateDifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.dim());
		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(expertDir, domain, sp);

		int depth = 1;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, new NullTermination(), 1., new NameDependentStateHashFactory(), depth, -1, 20);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, episodes, rf);
		request.setBoltzmannBeta(5.);

		MLIRL irl = new MLIRL(request, 0.1, 0.01, 15);
		irl.performIRL();

		StateRewardFunctionValue learnedRFVis = new StateRewardFunctionValue(this.domain, rf);

		StateGridder gridder = new StateGridder();
		gridder.gridEntireObjectClass(this.domain.getObjectClass(MountainCar.CLASSAGENT), 100);
		List <State> griddedStates = gridder.gridInputState(this.initialState);
		System.out.println("num gridded states = " + griddedStates.size());

		ValueFunctionVisualizerGUI gui = ValueFunctionVisualizerGUI.createGridWorldBasedValueFunctionVisualizerGUI(griddedStates, learnedRFVis, null, MountainCar.CLASSAGENT, MountainCar.ATTX, MountainCar.ATTV,
				MountainCar.ACTIONFORWARD,
				MountainCar.ACTIONBACKWARDS,
				MountainCar.ACTIONCOAST,
				MountainCar.ACTIONBACKWARDS);


		StateValuePainter2D sv2 = (StateValuePainter2D)gui.getSvp();
		sv2.toggleValueStringRendering(false);
		LandmarkColorBlendInterpolation colorBlend = new LandmarkColorBlendInterpolation();
		colorBlend.addNextLandMark(0., Color.BLACK);
		colorBlend.addNextLandMark(1., Color.WHITE);
		sv2.setColorBlend(colorBlend);
		sv2.setNumXCells(99);
		sv2.setNumYCells(99);


		List <Color> colors = new ArrayList<Color>();
		colors.add(Color.red);
		colors.add(Color.blue);
		List<EpisodeAnalysis> trainedEp = EpisodeAnalysis.parseFilesIntoEAList("oomdpResearch/irlMCH1Trained", this.domain, this.sp);
		TrajectoryRenderer tr = new TrajectoryRenderer(trainedEp, MountainCar.CLASSAGENT, MountainCar.ATTX, MountainCar.ATTV, new double[]{this.mcg.physParams.xmin, this.mcg.physParams.xmax, 0.}, new double[]{this.mcg.physParams.vmin, this.mcg.physParams.vmax, 0.}, 5.f, 5.f);
		tr.setColors(colors);
		gui.getMultiLayerRenderer().addRenderLayer(tr);


		gui.initGUI();

	}


	public void runSupervised(){

		MyTimer timer = new MyTimer();
		timer.start();

		StateToFeatureVectorGenerator svarGen  = new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT);
		MCFVGen fvgen = new MCFVGen();


		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(expertDir, domain, sp);

		System.out.println("expert size: " + episodes.get(0).numTimeSteps());
		System.out.println(episodes.get(0).getActionSequenceString(" "));

		WekaPolicy p = new WekaPolicy(fvgen, new J48(), this.domain.getActions(), episodes);
		//WekaPolicy p = new WekaPolicy(fvgen, new Logistic(), this.domain.getActions(), episodes);

		timer.stop();
		System.out.println("Training time: " + timer.getTime());


		/*
		State ns = this.initialState.copy();

		System.out.println("starting episode...");
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(ns, new NullRewardFunction(), this.mcg.new ClassicMCTF(), 500);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		trainedEpisode.writeToFile(trainedDir+"/easyLogisticAgent", sp);


		new EpisodeSequenceVisualizer(v, domain, sp, trainedDir);
		*/


	}

	public void runSupervisedRHC(){

		MCFVGen fvgen = new MCFVGen();
		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(expertDir, domain, sp);

		System.out.println("expert size: " + episodes.get(0).numTimeSteps());
		System.out.println(episodes.get(0).getActionSequenceString(" "));

		SupervisedRHC p = new SupervisedRHC(this.domain, new UniformCostRF(), new MountainCar.ClassicMCTF(), 0.99, 1, -1, fvgen, new LinearRegression(), episodes);

		State ns = this.initialState.copy();

		System.out.println("starting episode...");
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(ns, new NullRewardFunction(), new MountainCar.ClassicMCTF(), 500);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		trainedEpisode.writeToFile(trainedDir+"/superRHCTrained", sp);

		int nGrids = 10;

		StateGridder gridder = new StateGridder();
		gridder.gridEntireObjectClass(this.domain.getObjectClass(MountainCar.CLASSAGENT), nGrids);
		List <State> griddedStates = gridder.gridInputState(this.initialState);
		System.out.println("num gridded states = " + griddedStates.size());

		ValueFunctionVisualizerGUI gui = ValueFunctionVisualizerGUI.createGridWorldBasedValueFunctionVisualizerGUI(griddedStates, p.getSs(), null, MountainCar.CLASSAGENT, MountainCar.ATTX, MountainCar.ATTV,
				MountainCar.ACTIONFORWARD,
				MountainCar.ACTIONBACKWARDS,
				MountainCar.ACTIONCOAST,
				MountainCar.ACTIONBACKWARDS);


		StateValuePainter2D sv2 = (StateValuePainter2D)gui.getSvp();
		sv2.toggleValueStringRendering(true);
		sv2.setNumXCells(nGrids-1);
		sv2.setNumYCells(nGrids-1);
		LandmarkColorBlendInterpolation colorBlend = new LandmarkColorBlendInterpolation(0.2);
		colorBlend.addNextLandMark(0., Color.BLACK);
		colorBlend.addNextLandMark(1., Color.WHITE);
		//sv2.setColorBlend(colorBlend);

		ns = ns.copy();
		MountainCar.setAgent(ns, ns.getFirstObjectOfClass(MountainCar.CLASSAGENT).getRealValForAttribute(MountainCar.ATTX)+0.2, 0.);

		State penUlt = episodes.get(0).getState(episodes.get(0).numTimeSteps()-2);

		System.out.println("Value of init state: " + getV(p.getSs().getQs(ns)));
		System.out.println("Value of pen ultimate: " + getV(p.getSs().getQs(penUlt)));

		System.out.println("Pen ultmate state: \n" + penUlt.toString());

		gui.initGUI();

		//new EpisodeSequenceVisualizer(v, domain, sp, trainedDir);

	}

	public void launchTrainedViewer(){
		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(this.trainedDir, this.domain, this.sp);
		for(int i = 0; i < episodes.size(); i++){
			System.out.println(i + " " + (episodes.get(i).numTimeSteps()-1));
		}


		new EpisodeSequenceVisualizer(v, domain, sp, trainedDir);
	}



	public static double getV(List<QValue> qs){
		double max = Double.NEGATIVE_INFINITY;
		for(QValue q : qs){
			max = Math.max(max, q.q);
		}
		return max;
	}


	public class MCFVGen implements StateToFeatureVectorGenerator{

		RBFFeatureDatabase rbf;


		public MCFVGen(){

			//set up RBF feature database
			rbf = new RBFFeatureDatabase(true);
			StateGridder gridder = new StateGridder();
			gridder.gridEntireDomainSpace(domain, 5);
			List<State> griddedStates = gridder.gridInputState(initialState);

			DistanceMetric metric = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT));
			for(State g : griddedStates){
				rbf.addRBF(new GaussianRBF(g, metric, .2));
			}

		}


		public int dim(){
			return rbf.numberOfFeatures();
		}

		@Override
		public double[] generateFeatureVectorFrom(State s) {

			double [] fv = new double[this.rbf.numberOfFeatures()];
			List<StateFeature> sfs = this.rbf.getStateFeatures(s);
			for(StateFeature sf : sfs){
				fv[sf.id] = sf.value;
			}


			return fv;
		}
	}


	public static void main(String[] args) {
		MCIRL mcirl = new MCIRL();
		//mcirl.launchExplorer();
		//mcirl.launchSavedEpisodeViewer();
		mcirl.runIRL();
		//mcirl.runSupervised();

		//mcirl.runVFIRL();

		//mcirl.launchTrainedViewer();
		mcirl.visLearnedRF();
		//mcirl.runSupervisedRHC();
	}

}
