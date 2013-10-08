package behavior.training.pit;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;

public abstract class QModifiablePolicy extends Policy {

	
	public abstract void updatePolicyFromEpisodes(List <EpisodeAnalysis> episodes);

}
