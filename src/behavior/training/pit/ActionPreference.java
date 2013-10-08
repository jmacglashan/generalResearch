package behavior.training.pit;

import burlap.oomdp.singleagent.GroundedAction;

public class ActionPreference{
	
	public GroundedAction 	ga;
	public double			preference;
	
	public ActionPreference(GroundedAction ga, double preference){
		this.ga = ga;
		this.preference = preference;
	}
	
}
