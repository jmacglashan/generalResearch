package behavior.training.pit;

import java.util.List;

import burlap.behavior.singleagent.learning.actorcritic.Actor;
import burlap.oomdp.core.State;

public abstract class ActionPreferenceActor extends Actor {

	public abstract void updateWithPreferences(State s, List<ActionPreference> actionPreferences);

}
