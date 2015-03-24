package tests.debug;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

/**
 * @author James MacGlashan.
 */
public class WallsTest {

	public static void main(String[] args) {
		GridWorldDomain gwd = new GridWorldDomain(11,11);
		gwd.makeEmptyMap();
		gwd.setNumberOfLocationTypes(5);
		Domain domain = gwd.generateDomain();

		State s = GridWorldDomain.getOneAgentNLocationState(domain, 3);
		GridWorldDomain.setAgent(s, 0, 0);
		GridWorldDomain.setLocation(s, 0, 3, 3, 0);
		GridWorldDomain.setLocation(s, 1, 5, 5, 1);
		GridWorldDomain.setLocation(s, 2, 7, 7, 2);

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		VisualExplorer exp = new VisualExplorer(domain, v, s);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);

		exp.initGUI();

	}


}
