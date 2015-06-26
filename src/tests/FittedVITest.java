package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyDeterministicQPolicy;
import burlap.behavior.singleagent.planning.vfa.fittedvi.FittedVI;
import burlap.behavior.singleagent.planning.vfa.fittedvi.WekaVFATrainer;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class FittedVITest {

	public static void main(String[] args) {

		MountainCar mc = new MountainCar();
		Domain domain = mc.generateDomain();
		TerminalFunction tf = new MountainCar.ClassicMCTF();
		RewardFunction rf = new GoalBasedRF(tf, 100., -1.);

		State valleyState  = mc.getCleanState(domain);
		StateGridder sg = new StateGridder();
		int samplesPerDimension = 50;
		sg.gridEntireDomainSpace(domain, samplesPerDimension);
		List<State> samples = sg.gridInputState(valleyState);

		//make sure we have goal
		int terminalStates = 0;
		for(State s : samples){
			if(tf.isTerminal(s)){
				terminalStates++;
			}
		}

		if(terminalStates == 0){
			throw new RuntimeException("Did not find termainal state in gridding");
		}
		else{
			System.out.println("found " + terminalStates + " terminal states");
		}


		FittedVI fvi = new FittedVI(domain, rf, tf, 0.99,
				WekaVFATrainer.getKNNTrainer(new ConcatenatedObjectFeatureVectorGenerator(false, MountainCar.CLASSAGENT), 4),
				samples, -1, 0.01, 150);

		/*FittedVI fvi = new FittedVI(domain, rf, tf, 0.99,
				WekaVFATrainer.getKNNTrainer(new ConcatenatedObjectFeatureVectorGenerator(false, MountainCar.CLASSAGENT), 4),
				samples, -1, 0.01, 150);*/
		//fvi.setPlanningDepth(4);


		fvi.setPlanningAndControlDepth(1);
		fvi.runVI();


		System.out.println("Starting policy eval");
		//fvi.setControlDepth(4);
		Policy p = new GreedyDeterministicQPolicy(fvi);
		//MountainCar.setAgent(valleyState, -1.1, 0.);
		EpisodeAnalysis ea = p.evaluateBehavior(valleyState, rf, tf, 500);
		List<EpisodeAnalysis> eas = new ArrayList<EpisodeAnalysis>();
		eas.add(ea);

		System.out.println("Episode size: " + ea.maxTimeStep());

		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(MountainCarVisualizer.getVisualizer(mc), domain, eas);
		evis.initGUI();

		System.out.println("Starting value funciton vis");

		//fvi.setControlDepth(1);

		ValueFunctionVisualizerGUI gui = ValueFunctionVisualizerGUI.createGridWorldBasedValueFunctionVisualizerGUI(samples, fvi, null, MountainCar.CLASSAGENT, MountainCar.ATTX, MountainCar.ATTV,
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
		sv2.setNumXCells(samplesPerDimension-1);
		sv2.setNumYCells(samplesPerDimension-1);

		gui.initGUI();










	}

}
