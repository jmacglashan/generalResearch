package irl.mlirl;

import java.util.List;

import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public interface QGradientPlanner extends QComputablePlanner{

	public List<QGradientTuple> getAllQGradients(State s);
	public QGradientTuple getQGradient(State s, GroundedAction a);
	
	
}
