package behavior.training.prl;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public abstract class PreferenceModifiablePolicy extends Policy {

	public abstract void updateStatePreferences(State s, List<ActionProb> preferences);
	public abstract void updateSinglePreferenceInDirection(State s, GroundedAction ga, double preferenceChangeDirection);
	
	public void updatePreferencesInEpisodeAccordingToExternalPolicy(EpisodeAnalysis ea, Policy p){
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			State s = ea.getState(i);
			this.updateStatePreferences(s, p.getActionDistributionForState(s));
		}
	}
	
}
