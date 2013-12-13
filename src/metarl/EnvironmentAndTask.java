package metarl;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

public class EnvironmentAndTask {
	public Domain						domain;
	public RewardFunction				rf;
	public TerminalFunction				tf;
	public double						discount;
	public StateGenerator				initialStateGenerator;
	
	public EnvironmentAndTask(Domain domain, RewardFunction rf, TerminalFunction tf, double discount, StateGenerator initialStateGenerator){
		this.domain = domain;
		this.rf = rf;
		this.tf = tf;
		this.discount = discount;
		this.initialStateGenerator = initialStateGenerator;
	}
	
	
}
