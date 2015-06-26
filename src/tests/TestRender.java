package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import tools.EpisodeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class TestRender {

	public static void main(String[] args) {
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		QLearning ql = new QLearning(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0., 1.);

		List<EpisodeAnalysis> eas = new ArrayList<EpisodeAnalysis>();
		for(int i = 0; i < 30; i++){
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s);
			eas.add(ea);
		}

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		EpisodeRenderer er = new EpisodeRenderer(v, domain, eas);
		er.setFrameDelayMS(33);
		er.initGUI();

	}

}
