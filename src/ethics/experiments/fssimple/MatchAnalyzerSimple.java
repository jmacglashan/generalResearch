package ethics.experiments.fssimple;

import java.util.List;

import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.World;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import ethics.experiments.tbforagesteal.matchvisualizer.MatchAnalyzerWInteraction;

public class MatchAnalyzerSimple extends MatchAnalyzerWInteraction {

	public MatchAnalyzerSimple(World world, SGNaiveQLAgent agent0, SGNaiveQLAgent agent1,
			List<State> agent0qQueryStates, List<State> agent1qQueryStates) {
		super(world, agent0, agent1, agent0qQueryStates, agent1qQueryStates);
	}

	protected int whichAgentToScoreForStartState(State s){
		
		ObjectInstance a0Ob = s.getObject(agent0.getAgentName());
		int pn = a0Ob.getIntValForAttribute(FSSimple.ATTPN);
		if(pn == 0){
			if(FSSimple.isRootNode(s)){
				return 0;
			}
			else{
				return 1;
			}
		}
		else{
			if(FSSimple.isRootNode(s)){
				return 1;
			}
			else{
				return 0;
			}
		}
		
	}

}
