package tests.irltests;

import behavior.burlapirlext.DifferentiableSparseSampling;
import behavior.burlapirlext.DifferentiableZeroStepPlanner;
import behavior.burlapirlext.diffvinit.DiffVFRF;
import behavior.burlapirlext.diffvinit.LinearStateDiffVF;
import behavior.training.supervised.SupervisedRHC;
import behavior.training.supervised.WekaPolicy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateActionDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.lunarlander.*;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.Visualizer;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class LunarLanderIRL {

	protected LunarLanderDomain ldg;
	protected Domain domain;
	protected Visualizer v;

	protected State initialState;

	protected String expertDir = "oomdpResearch/irlLL";
	protected String trainedDir = "oomdpResearch/irlLLTrained";


	public LunarLanderIRL(){

		this.ldg = new LunarLanderDomain();
		this.ldg.setToStandardLunarLander();
		this.ldg.setAngmax(Math.PI/6.);
		this.ldg.setAnginc(Math.PI/6.);
		this.ldg.setVmax(2.5);
		this.domain = ldg.generateDomain();

		new TouchEdgePF(this.ldg, this.domain);

		initialState = LunarLanderDomain.getCleanState(domain, 0);
		LunarLanderDomain.setAgent(initialState, 0., 5, 0.);
		LunarLanderDomain.setPad(initialState, 80., 95., 0., 10.);

		this.v = LLVisualizer.getVisualizer(this.ldg);

	}



	public void launchExplorer(){

		VisualExplorer exp = new VisualExplorer(domain, v, initialState);

		exp.addKeyAction("w", LunarLanderDomain.ACTIONTHRUST+0);
		exp.addKeyAction("s", LunarLanderDomain.ACTIONTHRUST+1);
		exp.addKeyAction("a", LunarLanderDomain.ACTIONTURNL);
		exp.addKeyAction("d", LunarLanderDomain.ACTIONTURNR);
		exp.addKeyAction("x", LunarLanderDomain.ACTIONIDLE);

		StateParser sp = new LLStateParser(domain);
		exp.enableEpisodeRecording("r", "f", new NullRewardFunction(), expertDir, sp);

		exp.initGUI();


	}


	public void launchSavedEpisodeViewer(){
		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), expertDir);
	}


	public void runIRL(){
		LLDRFFV fvgen = new LLDRFFV();
		DifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());
		/*LinearStateActionDifferentiableRF rf = new LinearStateActionDifferentiableRF(fvgen, fvgen.getDim(),
				new GroundedAction(this.domain.getAction(LunarLanderDomain.ACTIONTHRUST+0), ""),
				new GroundedAction(this.domain.getAction(LunarLanderDomain.ACTIONTHRUST+1), ""),
				new GroundedAction(this.domain.getAction(LunarLanderDomain.ACTIONIDLE), ""),
				new GroundedAction(this.domain.getAction(LunarLanderDomain.ACTIONTURNL), ""),
				new GroundedAction(this.domain.getAction(LunarLanderDomain.ACTIONTURNR), ""));*/


		List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, new LLStateParser(domain));
		System.out.println("Training for " + eas.size() + " episodes");

		/*
		EpisodeAnalysis ea = eas.get(0);
		for(State s : ea.stateSequence){
			double [] fv = fvgen.generateFeatureVectorFrom(s);
			System.out.println(Arrays.toString(fv));
		}
		*/

		int depth = 3;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, new NullTermination(), 1., new NameDependentStateHashFactory(), depth, -1, 5);
		//DifferentiableZeroStepPlanner dss = new DifferentiableZeroStepPlanner(domain, rf);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, eas, rf);
		request.setBoltzmannBeta(5.);

		MLIRL irl = new MLIRL(request, 0.1, 0.01, 10);

		irl.performIRL();

		System.out.println(this.getStringRepOfWeights(rf.getParameters()));

		Policy p = new GreedyQPolicy(dss);

		System.out.println("starting episode...");
		State s = this.initialState.copy();
		//LunarLanderDomain.setAgent(s, 0, 50, 0.);
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(s, new NullRewardFunction(), new LunarLanderTF(domain), 500);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());
		//trainedEpisode.writeToFile(trainedDir+"/irlTestH3", new LLStateParser(domain));

		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), trainedDir);

		/*
		State ls = trainedEpisode.getState(trainedEpisode.numTimeSteps()-1);
		double [] fv = fvgen.generateFeatureVectorFrom(ls);
		System.out.println("Last state feature:");
		System.out.println(Arrays.toString(fv));
		*/


	}


	public void runVFIRL(){
		LLDVFFV fvgen = new LLDVFFV();
		LinearStateDiffVF vf = new LinearStateDiffVF(fvgen, 11);
		RewardFunction objectiveRF = new LunarLanderRF(this.domain);
		DifferentiableRF rf = new DiffVFRF(objectiveRF, vf);
		TerminalFunction tf = new LunarLanderTF(this.domain);


		List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, new LLStateParser(domain));

		/*
		EpisodeAnalysis ea = eas.get(0);
		for(State s : ea.stateSequence){
			double [] fv = fvgen.generateFeatureVectorFrom(s);
			System.out.println(Arrays.toString(fv));
		}
		*/

		int depth = 4;
		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, tf, 0.99, new NameDependentStateHashFactory(), depth, -1, 5);
		dss.setValueForLeafNodes(vf);
		dss.toggleDebugPrinting(false);

		MLIRLRequest request = new MLIRLRequest(domain, dss, eas, rf);
		request.setBoltzmannBeta(5.);

		MLIRL irl = new MLIRL(request, 0.1, 0.01, 10);

		irl.performIRL();

		System.out.println(this.getStringRepOfVFWeights(rf.getParameters()));

		Policy p = new GreedyQPolicy(dss);

		System.out.println("starting episode...");
		State s = this.initialState.copy();
		//LunarLanderDomain.setAgent(s, 0, 50, 0.);
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(s, objectiveRF, tf, 500);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());
		trainedEpisode.writeToFile(trainedDir+"/vf45Hard", new LLStateParser(domain));

		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), trainedDir);

		/*
		State ls = trainedEpisode.getState(trainedEpisode.numTimeSteps()-1);
		double [] fv = fvgen.generateFeatureVectorFrom(ls);
		System.out.println("Last state feature:");
		System.out.println(Arrays.toString(fv));
		*/


	}


	public void runSupervised(){

		LLDRFFV fvgen = new LLDRFFV();
		StateToFeatureVectorGenerator svarGen = new ConcatenatedObjectFeatureVectorGenerator(true, LunarLanderDomain.AGENTCLASS);

		List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, new LLStateParser(domain));

		//WekaPolicy p = new WekaPolicy(fvgen, new J48(), this.domain.getActions(), eas);
		WekaPolicy p = new WekaPolicy(fvgen, new Logistic(), this.domain.getActions(), eas);

		System.out.println("starting episode...");
		State s = this.initialState.copy();
		LunarLanderDomain.setAgent(s, 0, 50, 0.);
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(s, new NullRewardFunction(), new LunarLanderTF(domain), 200);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		trainedEpisode.writeToFile(trainedDir+"/logisticAgent	Easy", new LLStateParser(domain));

		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), trainedDir);

	}

	public void runSupervisedRHC(){

		LLDRFFV fvgen = new LLDRFFV();
		//StateToFeatureVectorGenerator svarGen = new ConcatenatedObjectFeatureVectorGenerator(true, LunarLanderDomain.AGENTCLASS);

		List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, new LLStateParser(domain));

		SupervisedRHC p = new SupervisedRHC(this.domain, new LunarLanderRF(this.domain), new LunarLanderTF(this.domain), 0.99, 4, -1, fvgen, new LinearRegression(), eas);
		//WekaPolicy p = new WekaPolicy(svarGen, new Logistic(), this.domain.getActions(), eas);

		System.out.println("starting episode...");
		State s = this.initialState.copy();
		//LunarLanderDomain.setAgent(s, 0, 50, 0.);
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(s, new NullRewardFunction(), new LunarLanderTF(domain), 200);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		trainedEpisode.writeToFile(trainedDir+"/supervisedRHCHard", new LLStateParser(domain));

		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), trainedDir);

	}


	public void launchTrainedViewer(){

		List<EpisodeAnalysis> episodes = EpisodeAnalysis.parseFilesIntoEAList(this.trainedDir, this.domain, new LLStateParser(this.domain));
		for(int i = 0; i < episodes.size(); i++){
			System.out.println(i + " " + (episodes.get(i).numTimeSteps()-1));
		}

		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), trainedDir);
	}

	public void launchTrajectoryRenderer(){

		Visualizer tv = this.getTrajectoryRenderLayerBase();
		//List <EpisodeAnalysis> trajectories = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, this.domain, new LLStateParser(this.domain));
		List <EpisodeAnalysis> trajectories = EpisodeAnalysis.parseFilesIntoEAList("oomdpResearch/irlLLTrainedForTrajectories", this.domain, new LLStateParser(this.domain));

		List <Color> colors = new ArrayList<Color>();
		colors.add(Color.black);
		colors.add(Color.black);
		colors.add(Color.red);
		colors.add(Color.MAGENTA);
		colors.add(Color.blue);

		colors.add(Color.MAGENTA);
		colors.add(Color.MAGENTA);
		colors.add(Color.MAGENTA);


		TrajectoryRenderer tr = new TrajectoryRenderer(trajectories, LunarLanderDomain.AGENTCLASS, LunarLanderDomain.XATTNAME, LunarLanderDomain.YATTNAME, new double[]{0, this.ldg.getXmax(), 0.}, new double[]{0, this.ldg.getYmax(), 0.}, 3.f, 10.f);
		tr.setColors(colors);
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
		srl.addObjectClassPainter(LunarLanderDomain.PADCLASS, new LLVisualizer.PadPainter(this.ldg));
		Visualizer v = new Visualizer(srl);
		return v;
	}

	public void testRFs(){

		LLDRFFV fvgen = new LLDRFFV();
		DifferentiableRF rf = new LinearStateDifferentiableRF(fvgen, fvgen.getDim());

		/*rf.setParameters(new double[]{
			0.,
			3.176,
			-14.226,
			-1.966,
			1.245,
			0.028,
			-2.246,
			1.714,
			-1.123,
			-3.176,
			-3.419,
			-1.343

		});*/
		rf.setParameters(new double[]{
				0.,
				1000.,
				-1000,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				5,
				0,
				100

		});


		List<EpisodeAnalysis> eas = EpisodeAnalysis.parseFilesIntoEAList(this.expertDir, domain, new LLStateParser(domain));

		DifferentiableSparseSampling dss = new DifferentiableSparseSampling(domain, rf, new NullTermination(), 1., new NameDependentStateHashFactory(), 6, -1, 20);
		//dss.toggleDebugPrinting(false);
		dss.setForgetPreviousPlanResults(true);



		State testState = initialState.copy();
		//testState.getObject("agent0").setValue(LunarLanderDomain.XATTNAME, 70);


		Policy p = new GreedyQPolicy(dss);

		System.out.println("starting episode...");
		EpisodeAnalysis trainedEpisode = p.evaluateBehavior(testState, new NullRewardFunction(), new LunarLanderTF(domain), 80);

		System.out.println("num steps in trained policy: " + trainedEpisode.numTimeSteps());

		State ls = trainedEpisode.getState(trainedEpisode.numTimeSteps()-1);
		double [] fv = fvgen.generateFeatureVectorFrom(ls);
		System.out.println("Last state feature:");
		System.out.println(Arrays.toString(fv));

		trainedEpisode.writeToFile(trainedDir+"/trainedEp", new LLStateParser(domain));

		new EpisodeSequenceVisualizer(v, domain, new LLStateParser(domain), trainedDir);



	}



	public class LLDRFFV implements StateToFeatureVectorGenerator{


		//constant + on pad + obstacle + 9 spatial + pad spatial
		protected int nDim = 13;

		protected PropositionalFunction 		onGround;
		protected PropositionalFunction			touchingSurface;
		protected PropositionalFunction			touchingPad;
		protected PropositionalFunction			onPad;

		protected double [][]					rbfCenters;

		protected double 						bandwidth = 0.2;
		protected double						padBandWdith = 0.6;

		public LLDRFFV(){

			//this.onGround = domain.getPropFunction(LunarLanderDomain.PFONGROUND);
			this.onGround = domain.getPropFunction(TouchEdgePF.EDGEPFNAME);
			this.touchingSurface = domain.getPropFunction(LunarLanderDomain.PFTOUCHSURFACE);
			this.touchingPad = domain.getPropFunction(LunarLanderDomain.PFTPAD);
			this.onPad = domain.getPropFunction(LunarLanderDomain.PFONPAD);

			StateGridder gridder = new StateGridder();
			gridder.setObjectClassAttributesToTile(LunarLanderDomain.AGENTCLASS,
					new StateGridder.AttributeSpecification(domain.getAttribute(LunarLanderDomain.XATTNAME), 3),
					new StateGridder.AttributeSpecification(domain.getAttribute(LunarLanderDomain.YATTNAME), 3));


			List<State> griddedStates = gridder.gridInputState(initialState);

			rbfCenters = new double[griddedStates.size()][2];

			for(int i = 0; i < griddedStates.size(); i++){
				double [] nxy = this.getNormalizedXY(griddedStates.get(i));
				//System.out.println(i + " " + nxy[0] + " " + nxy[1]);
				rbfCenters[i][0] = nxy[0];
				rbfCenters[i][1] = nxy[1];
			}

		}

		protected int getDim(){
			return this.nDim;
		}

		@Override
		public double[] generateFeatureVectorFrom(State s) {

			double [] nxy = this.getNormalizedXY(s);

			double [] fv = new double[this.nDim];
			fv[0] = 1.; //constant
			double [] pfs = this.evaluatePFFeatures(s, nxy[0], nxy[1]);
			fv[1] = pfs[0];
			fv[2] = pfs[1];



			for(int i = 0; i < this.rbfCenters.length; i++){
				double sig = this.getRBFSignal(nxy[0], nxy[1], this.rbfCenters[i][0], this.rbfCenters[i][1], this.bandwidth);
				fv[i+3] = sig;
			}

			double [] padXY = this.getNormalizedPadXY(s);
			fv[12] = this.getRBFSignal(nxy[0], nxy[1], padXY[0], padXY[1], this.padBandWdith);

			return fv;
		}


		protected double [] evaluatePFFeatures(State s, double nx, double ny){

			if(this.onPad.somePFGroundingIsTrue(s)){
				return new double []{1., 0.};
			}
			else if(touchingSurface.somePFGroundingIsTrue(s) || touchingPad.somePFGroundingIsTrue(s) || onGround.somePFGroundingIsTrue(s)
					|| nx <= 0.01 || nx >= 0.99 || ny == 0. || ny == 1.){
				return new double[]{0., 1.};
			}

			return new double[]{0., 0.};

		}

		protected double [] getNormalizedXY(State s){
			ObjectInstance a = s.getFirstObjectOfClass(LunarLanderDomain.AGENTCLASS);
			double x = a.getRealValForAttribute(LunarLanderDomain.XATTNAME);
			double y = a.getRealValForAttribute(LunarLanderDomain.YATTNAME);

			Attribute xatt = domain.getAttribute(LunarLanderDomain.XATTNAME);
			Attribute yatt = domain.getAttribute(LunarLanderDomain.YATTNAME);

			double nx = (x - xatt.lowerLim) / (xatt.upperLim - xatt.lowerLim);
			double ny = (y - yatt.lowerLim) / (yatt.upperLim - yatt.lowerLim);

			return new double[]{nx, ny};
		}


		protected double [] getNormalizedPadXY(State s){
			ObjectInstance a = s.getFirstObjectOfClass(LunarLanderDomain.PADCLASS);
			double x = (a.getRealValForAttribute(LunarLanderDomain.LATTNAME) + a.getRealValForAttribute(LunarLanderDomain.RATTNAME))/2.;
			double y = a.getRealValForAttribute(LunarLanderDomain.TATTNAME);

			Attribute xatt = domain.getAttribute(LunarLanderDomain.XATTNAME);
			Attribute yatt = domain.getAttribute(LunarLanderDomain.YATTNAME);

			double nx = (x - xatt.lowerLim) / (xatt.upperLim - xatt.lowerLim);
			double ny = (y - yatt.lowerLim) / (yatt.upperLim - yatt.lowerLim);

			return new double[]{nx, ny};
		}


		protected double getRBFSignal(double x, double y, double rx, double ry, double bandwidth){
			double diffx = x - rx;
			double diffy = y - ry;

			double dist = Math.sqrt(diffx*diffx + diffy*diffy);

			double sig = Math.exp(-dist / bandwidth);

			return sig;

		}

	}






	public class LLDVFFV implements StateToFeatureVectorGenerator{


		//constant + 9 spatial + pad spatial
		protected int nDim = 11;

		protected double [][]					rbfCenters;

		protected double 						bandwidth = 0.2;
		protected double						padBandWdith = 0.6;

		public LLDVFFV(){


			StateGridder gridder = new StateGridder();
			gridder.setObjectClassAttributesToTile(LunarLanderDomain.AGENTCLASS,
					new StateGridder.AttributeSpecification(domain.getAttribute(LunarLanderDomain.XATTNAME), 3),
					new StateGridder.AttributeSpecification(domain.getAttribute(LunarLanderDomain.YATTNAME), 3));


			List<State> griddedStates = gridder.gridInputState(initialState);

			rbfCenters = new double[griddedStates.size()][2];

			for(int i = 0; i < griddedStates.size(); i++){
				double [] nxy = this.getNormalizedXY(griddedStates.get(i));
				//System.out.println(i + " " + nxy[0] + " " + nxy[1]);
				rbfCenters[i][0] = nxy[0];
				rbfCenters[i][1] = nxy[1];
			}

		}

		protected int getDim(){
			return this.nDim;
		}

		@Override
		public double[] generateFeatureVectorFrom(State s) {

			double [] nxy = this.getNormalizedXY(s);

			double [] fv = new double[this.nDim];
			fv[0] = 1.; //constant


			//double [] pfs = this.evaluatePFFeatures(s, nxy[0], nxy[1]);
			//fv[1] = pfs[0];
			//fv[2] = pfs[1];



			for(int i = 0; i < this.rbfCenters.length; i++){
				double sig = this.getRBFSignal(nxy[0], nxy[1], this.rbfCenters[i][0], this.rbfCenters[i][1], this.bandwidth);
				fv[i+1] = sig;
			}

			double [] padXY = this.getNormalizedPadXY(s);
			fv[10] = this.getRBFSignal(nxy[0], nxy[1], padXY[0], padXY[1], this.padBandWdith);

			return fv;
		}

		protected double [] getNormalizedXY(State s){
			ObjectInstance a = s.getFirstObjectOfClass(LunarLanderDomain.AGENTCLASS);
			double x = a.getRealValForAttribute(LunarLanderDomain.XATTNAME);
			double y = a.getRealValForAttribute(LunarLanderDomain.YATTNAME);

			Attribute xatt = domain.getAttribute(LunarLanderDomain.XATTNAME);
			Attribute yatt = domain.getAttribute(LunarLanderDomain.YATTNAME);

			double nx = (x - xatt.lowerLim) / (xatt.upperLim - xatt.lowerLim);
			double ny = (y - yatt.lowerLim) / (yatt.upperLim - yatt.lowerLim);

			return new double[]{nx, ny};
		}


		protected double [] getNormalizedPadXY(State s){
			ObjectInstance a = s.getFirstObjectOfClass(LunarLanderDomain.PADCLASS);
			double x = (a.getRealValForAttribute(LunarLanderDomain.LATTNAME) + a.getRealValForAttribute(LunarLanderDomain.RATTNAME))/2.;
			double y = a.getRealValForAttribute(LunarLanderDomain.TATTNAME);

			Attribute xatt = domain.getAttribute(LunarLanderDomain.XATTNAME);
			Attribute yatt = domain.getAttribute(LunarLanderDomain.YATTNAME);

			double nx = (x - xatt.lowerLim) / (xatt.upperLim - xatt.lowerLim);
			double ny = (y - yatt.lowerLim) / (yatt.upperLim - yatt.lowerLim);

			return new double[]{nx, ny};
		}


		protected double getRBFSignal(double x, double y, double rx, double ry, double bandwidth){
			double diffx = x - rx;
			double diffy = y - ry;

			double dist = Math.sqrt(diffx*diffx + diffy*diffy);

			double sig = Math.exp(-dist / bandwidth);

			return sig;

		}

	}










	public String getStringRepOfWeights(double [] weights){

		/*
		0 0.0 0.0
		1 0.0 0.5
		2 0.0 1.0
		3 0.5 0.0
		4 0.5 0.5
		5 0.5 1.0
		6 1.0 0.0
		7 1.0 0.5
		8 1.0 1.0
		 */

		StringBuilder sb = new StringBuilder();

		int fieldWidth = 10;

		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "constant", weights[0]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "on pad", weights[1]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "edge", weights[2]));

		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "0 0", weights[3]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "0 .5", weights[4]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "0 1", weights[5]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", ".5 0", weights[6]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", ".5 .5", weights[7]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", ".5 1", weights[8]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "1 0", weights[9]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "1 .5", weights[10]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "1 1", weights[11]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "pad loc", weights[11]));

		return sb.toString();
	}


	public String getStringRepOfVFWeights(double [] weights){

		/*
		0 0.0 0.0
		1 0.0 0.5
		2 0.0 1.0
		3 0.5 0.0
		4 0.5 0.5
		5 0.5 1.0
		6 1.0 0.0
		7 1.0 0.5
		8 1.0 1.0
		 */

		StringBuilder sb = new StringBuilder();

		int fieldWidth = 10;

		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "constant", weights[0]));


		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "0 0", weights[1]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "0 .5", weights[2]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "0 1", weights[3]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", ".5 0", weights[4]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", ".5 .5", weights[5]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", ".5 1", weights[6]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "1 0", weights[7]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "1 .5", weights[8]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "1 1", weights[9]));
		sb.append(String.format("%-"+fieldWidth+"s: %.3f\n", "pad loc", weights[10]));

		return sb.toString();
	}


	public static class TouchEdgePF extends PropositionalFunction{

		public static final String EDGEPFNAME = "touchingEdge";

		public LunarLanderDomain dgen;

		public TouchEdgePF(LunarLanderDomain dgen, Domain domain){
			super(EDGEPFNAME,domain, LunarLanderDomain.AGENTCLASS);
			this.dgen = dgen;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance agent = s.getObject(params[0]);
			double x = agent.getRealValForAttribute(LunarLanderDomain.XATTNAME);
			double y = agent.getRealValForAttribute(LunarLanderDomain.YATTNAME);

			if(y == dgen.getYmin() || y == dgen.getYmax() || x == dgen.getXmin() || x == dgen.getXmax()){
				return true;
			}


			return false;
		}
	}


	public static void main(String[] args) {
		LunarLanderIRL irl = new LunarLanderIRL();


		//irl.launchExplorer();
		//irl.launchSavedEpisodeViewer();
		irl.runIRL();
		//irl.runVFIRL();
		//irl.testRFs();
		//irl.runSupervised();
		//irl.launchTrainedViewer();
		//irl.launchTrajectoryRenderer();
		//irl.runSupervisedRHC();
	}



}
