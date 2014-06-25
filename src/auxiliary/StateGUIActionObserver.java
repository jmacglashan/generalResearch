package auxiliary;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;

public class StateGUIActionObserver implements ActionObserver {

	public StateVisualizingGUI gui;
	
	public StateGUIActionObserver(StateVisualizingGUI gui){
		this.gui = gui;
	}
	
	@Override
	public void actionEvent(State s, GroundedAction ga, State sp) {
		this.gui.setRenderState(sp);
	}

}
