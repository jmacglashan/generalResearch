package tests.training;

import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

/**
 * @author James MacGlashan.
 */
public class Tmp {

	public static void main(String[]args){


		GridWorldDomain dg = new GridWorldDomain(11, 11);
		dg.setMapToFourRooms();
		Domain domain = dg.generateDomain();


		Visualizer v = GridWorldVisualizer.getVisualizer(dg.getMap());

		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, new StateJSONParser(domain), "testJSON");

	}

	public static void record(){
		GridWorldDomain dg = new GridWorldDomain(11, 11);
		dg.setMapToFourRooms();
		Domain domain = dg.generateDomain();

		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		Visualizer v = GridWorldVisualizer.getVisualizer(dg.getMap());

		VisualExplorer exp = new VisualExplorer(domain, v, s);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		exp.enableEpisodeRecording("r", "f", new NullRewardFunction(), "testJSON", new StateJSONParser(domain));

		exp.initGUI();
	}

}
