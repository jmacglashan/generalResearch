package tests;

import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

/**
 * @author James MacGlashan.
 */
public class TestExplorer {

	public static void main(String[] args) {
		GridWorldDomain gwd = new GridWorldDomain(5, 5);
		gwd.makeEmptyMap();
		Domain domain = gwd.generateDomain();

		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(4, 4);
		GoalBasedRF rf = new GoalBasedRF(tf, 10, -1);

		/*
		TerminalExplorer texp = new TerminalExplorer(domain);
		texp.setTerminalFunctionf(tf);
		texp.setRewardFunction(rf);

		texp.exploreFromState(GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0));
		*/

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		VisualExplorer exp = new VisualExplorer(domain, v, GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0));
		exp.setTerminalFunction(tf);
		exp.setTrackingRewardFunction(rf);

		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);

		exp.initGUI();


	}

}
