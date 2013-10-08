package domain.stocasticgames.foragesteal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stocashticgames.Agent;
import burlap.oomdp.stocashticgames.SGStateGenerator;

public class TBFSAlternatingTurnSG extends SGStateGenerator {

	protected int agentStartTurn;
	protected double [] forageProbabilities;
	protected Random rand;
	
	public TBFSAlternatingTurnSG(){
		agentStartTurn = 0;
		forageProbabilities = new double[TBForageSteal.NALTS];
		for(int i = 0; i < forageProbabilities.length; i++){
			forageProbabilities[i] = 0.5;
		}
		rand = RandomFactory.getMapped(0);
	}
	
	
	@Override
	public State generateState(List<Agent> agents) {
		
		//choose forage options
		List <Integer> falts = new ArrayList<Integer>();
		for(int i = 0; i < forageProbabilities.length; i++){
			double roll = rand.nextDouble();
			if(roll < forageProbabilities[i]){
				falts.add(i);
			}
		}
		
		int [] arrayFA = new int[falts.size()];
		for(int i = 0; i < arrayFA.length; i++){
			arrayFA[i] = falts.get(i);
		}
		
		State s = TBForageSteal.getGameStartState(arrayFA, agentStartTurn);
		
		List <ObjectInstance> agentObs = s.getObjectsOfTrueClass(TBForageSteal.CLASSAGENT);
		s.renameObject(agentObs.get(0), agents.get(0).getAgentName());
		s.renameObject(agentObs.get(1), agents.get(1).getAgentName());
		
		if(agentStartTurn == 0){
			agentStartTurn = 1;
		}
		else{
			agentStartTurn = 0;
		}
		
		return s;
	}

}
