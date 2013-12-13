package domain.stocasticgames.foragesteal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

public class TBFSAlternatingTurnSG extends SGStateGenerator {

	protected int agentStartTurn;
	protected double [] forageProbabilities;
	protected Random rand;
	protected Domain domain;
	
	public TBFSAlternatingTurnSG(Domain domain){
		agentStartTurn = 0;
		forageProbabilities = new double[TBForageSteal.NALTS];
		for(int i = 0; i < forageProbabilities.length; i++){
			forageProbabilities[i] = 0.5;
		}
		rand = RandomFactory.getMapped(0);
		this.domain = domain;
	}
	
	public TBFSAlternatingTurnSG(Domain domain, double [] forageProbs){
		agentStartTurn = 0;
		forageProbabilities = forageProbs;
		rand = RandomFactory.getMapped(0);
		this.domain = domain;
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
		
		//this will construct a state with 2 players in it, but the object names of the players are arbitrary and do not necessarily match the
		//agent names in the world
		State s = TBForageSteal.getGameStartState(domain, arrayFA, agentStartTurn);
		
		
		//rename the create agent object instances to the names of the agents in the world
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

	
	
	public static void main(String [] args){
		TBForageSteal gen = new TBForageSteal();
		gen.setNoopInFirstState(false);
		SGDomain d = (SGDomain)gen.generateDomain();
		
		TBFSAlternatingTurnSG sg = new TBFSAlternatingTurnSG(d, new double[]{1., 0.5, 0.5, 0., 0.});
		
		List <Agent> agents = new ArrayList<Agent>();
		Agent a1 = new Agent() {
			
			@Override
			public void observeOutcome(State s, JointAction jointAction,
					Map<String, Double> jointReward, State sprime, boolean isTerminal) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public GroundedSingleAction getAction(State s) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void gameTerminated() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void gameStarting() {
				// TODO Auto-generated method stub
				
			}
		};
		
		a1.joinWorld(new World(d, null, null, null, sg), new AgentType("player", d.getObjectClass(TBForageSteal.CLASSAGENT), null));
		
		
		Agent a2 = new Agent() {
			
			@Override
			public void observeOutcome(State s, JointAction jointAction,
					Map<String, Double> jointReward, State sprime, boolean isTerminal) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public GroundedSingleAction getAction(State s) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void gameTerminated() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void gameStarting() {
				// TODO Auto-generated method stub
				
			}
		};
		
		a2.joinWorld(new World(d, null, null, null, sg), new AgentType("player", d.getObjectClass(TBForageSteal.CLASSAGENT), null));
		
		
		agents.add(a1);
		agents.add(a2);
		
		
		for(int i = 0; i < 20; i++){
			System.out.println(sg.generateState(agents).getCompleteStateDescription());
			System.out.println("-------------------------------------------------------------");
		}
		
	}
	
	
}
