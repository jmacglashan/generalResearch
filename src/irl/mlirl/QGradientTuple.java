package irl.mlirl;

import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

public class QGradientTuple {

	public State s;
	public AbstractGroundedAction a;
	public double [] gradient;
	
	public QGradientTuple(State s, AbstractGroundedAction a, double [] gradient){
		this.s = s;
		this.a = a;
		this.gradient = gradient;
	}
	
}


