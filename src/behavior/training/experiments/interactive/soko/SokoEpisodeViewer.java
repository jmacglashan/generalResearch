package behavior.training.experiments.interactive.soko;

import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Parser;
import domain.singleagent.sokoban2.Sokoban2Visualizer;

/**
 * @author James MacGlashan.
 */
public class SokoEpisodeViewer {

	public static void main(String[] args) {
		//String path = "oomdpResearch/sokoUserTrain/15512393f71c6b_1";
		String path = "oomdpResearch/amt_soko_logs_2/1552ac1fd34fdb_1";
		Sokoban2Domain soko = new Sokoban2Domain();
		soko.includeDirectionAttribute(true);
		Domain d = soko.generateDomain();
		StateParser sp = new StateYAMLParser(d);
		Visualizer v = Sokoban2Visualizer.getVisualizer("oomdpResearch/robotImages");

		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, d, sp, path);
		evis.initGUI();


	}

}
