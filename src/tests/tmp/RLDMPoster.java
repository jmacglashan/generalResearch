package tests.tmp;

import burlap.domain.singleagent.cartpole.InvertedPendulum;
import burlap.domain.singleagent.cartpole.InvertedPendulumVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import org.eclipse.jetty.deploy.graph.Graph;

/**
 * @author James MacGlashan.
 */
public class RLDMPoster {

	public static void main(String[] args) {

		InvertedPendulum ip = new InvertedPendulum();
		Domain domain = ip.generateDomain();
		State s = InvertedPendulum.getInitialState(domain);
		Visualizer v = InvertedPendulumVisualizer.getInvertedPendulumVisualizer();
		VisualExplorer exp = new VisualExplorer(domain, v, s);

		exp.addKeyAction("a", InvertedPendulum.ACTIONLEFT);
		exp.addKeyAction("d", InvertedPendulum.ACTIONRIGHT);
		exp.addKeyAction("s", InvertedPendulum.ACTIONNOFORCE);

		exp.initGUI();

	}


	public static void gwd(){

		GridWorldDomain gwd = new GridWorldDomain(11 ,11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		VisualExplorer exp = new VisualExplorer(domain, v, s);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.initGUI();
	}

}
