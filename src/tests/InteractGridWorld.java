package tests;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class InteractGridWorld {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		GridWorldDomain dgen = new GridWorldDomain(11, 11);
		dgen.setMapToFourRooms();
		
		Domain d = dgen.generateDomain();
		
		Visualizer v = GridWorldVisualizer.getVisualizer(dgen.getMap());
		State s = GridWorldDomain.getOneAgentNoLocationState(d);
		
		VisualExplorer exp = new VisualExplorer(d, v, s);
		exp.addKeyAction("w", "north");
		exp.addKeyAction("s", "south");
		exp.addKeyAction("a", "west");
		exp.addKeyAction("d", "east");
		
		exp.initGUI();

	}

}
