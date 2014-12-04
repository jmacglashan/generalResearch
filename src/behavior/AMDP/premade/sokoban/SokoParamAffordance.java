package behavior.AMDP.premade.sokoban;

import behavior.AMDP.blacklistAffordance.AffordanceBlackList;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import commands.model3.TrajectoryModule;

import java.util.HashSet;
import java.util.Set;

/**
 * @author James MacGlashan.
 */
public abstract class SokoParamAffordance extends AffordanceBlackList {

	public Set<String> goalParameters = new HashSet<String>();
	public TrajectoryModule.ConjunctiveGroundedPropTF tf;


	public SokoParamAffordance(String associatedActionName) {
		super(associatedActionName);
	}

	@Override
	public void setCurrentGoal(StateConditionTest gc) {
		this.goalParameters.clear();
		TFGoalCondition tfgc = (TFGoalCondition)gc;
		this.tf = (TrajectoryModule.ConjunctiveGroundedPropTF)tfgc.getTf();
		for(GroundedProp gp : this.tf.gps){
			for(String p : gp.params){
				this.goalParameters.add(p);
			}
		}
	}


}
