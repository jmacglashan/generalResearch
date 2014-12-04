package tests;

import behavior.planning.vfa.td.GradientDescentTDLambdaLookahead;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.auxiliary.StateGridder;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.common.ConcatenatedObjectFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBFFeatureDatabase;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.behavior.singleagent.vfa.rbf.metrics.EuclideanDistance;
import burlap.domain.singleagent.mountaincar.MountainCar;
import burlap.domain.singleagent.mountaincar.MountainCarVisualizer;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class TDVFATest {

	public static void main(String [] args){

		MountainCar mcGen = new MountainCar();
		Domain domain = mcGen.generateDomain();
		TerminalFunction tf = mcGen.new ClassicMCTF();
		RewardFunction rf = new GoalBasedRF(tf, 100);

		//get a state definition earlier, we'll use it soon.
		State s = mcGen.getCleanState(domain);

		//set up RBF feature database
		RBFFeatureDatabase rbf = new RBFFeatureDatabase(true);
		StateGridder gridder = new StateGridder();
		gridder.gridEntireDomainSpace(domain, 5);
		List<State> griddedStates = gridder.gridInputState(s);

		DistanceMetric metric = new EuclideanDistance(new ConcatenatedObjectFeatureVectorGenerator(true, MountainCar.CLASSAGENT));
		for(State g : griddedStates){
			rbf.addRBF(new GaussianRBF(g, metric, .2));
		}

		ValueFunctionApproximation vfa = rbf.generateVFA(1.);



		/*
		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);
		VisualActionObserver vexp = new VisualActionObserver(domain, v);
		vexp.initGUI();
		((SADomain)domain).addActionObserverForAllAction(vexp);
		*/

		GradientDescentTDLambdaLookahead planner = new GradientDescentTDLambdaLookahead(domain, rf, tf, 0.99, vfa, 0.01, 0.5, 100, 5000, 5, -1);
		planner.planFromState(s);

		GreedyQPolicy p = new GreedyQPolicy(planner);
		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf);
		System.out.println("Greedy size: " + (ea.numTimeSteps()-1));
		ea.writeToFile("tdTst/rolloutGreedy", new StateYAMLParser(domain));

		Visualizer v = MountainCarVisualizer.getVisualizer(mcGen);

		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, new StateYAMLParser(domain), "tdTst");
		evis.initGUI();

		/*for(int i = 0; i < 5; i++){
			p.evaluateBehavior(s, rf, tf);
		}*/

		System.out.println("Finished.");


	}


}
