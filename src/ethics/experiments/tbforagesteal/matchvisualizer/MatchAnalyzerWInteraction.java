package ethics.experiments.tbforagesteal.matchvisualizer;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import domain.stocasticgames.foragesteal.TBForageSteal;
import ethics.ParameterizedRF;

public class MatchAnalyzerWInteraction extends MatchAnalizer {

	protected List <Integer>			agent0Scores;
	protected List <Integer>			agent1Scores;
	
	

	public MatchAnalyzerWInteraction(World world, SGNaiveQLAgent agent0, SGNaiveQLAgent agent1,
			List<State> agent0qQueryStates, List<State> agent1qQueryStates) {
		
		super(world, agent0, agent1, agent0qQueryStates, agent1qQueryStates);
		
		this.agent0Scores = new ArrayList<Integer>();
		this.agent1Scores = new ArrayList<Integer>();
		
		
	}

	
	
	
	
	public String getCSVStringUsingSubRF(){
		

		TBForageSteal gen = new TBForageSteal();
		SGDomain d = (SGDomain) gen.generateDomain();
		
		ParameterizedRF a0rf = (ParameterizedRF)agent0.getInternalRewardFunction();
		ParameterizedRF a1rf = (ParameterizedRF)agent1.getInternalRewardFunction();
		
		double [] a0p = a0rf.getParameters();
		double [] a1p = a1rf.getParameters();
		
		String a0pRep = "R0_" + this.paramRep(a0p);
		String a1pRep = "R1_" + this.paramRep(a1p);
		
		
		StringBuffer buf = new StringBuffer();
		//header
		buf.append(a0pRep).append(",").append(a1pRep).append(",").append(a0pRep + "_S").append(",").append(a1pRep + "_S").append(",");	
		buf.append(a0pRep + "_PS").append(",").append(a1pRep + "_PS").append(",").append(a0pRep + "_PP").append(",").append(a1pRep + "_PP").append("\n");
		
		
		for(int i = 0; i < agent0QSequence.size(); i++){
			if(i < agent0Scores.size()){
				buf.append(agent0Scores.get(i)).append(",").append(agent1Scores.get(i)).append(",");
			}
			else{
				buf.append(",,");
			}
			QSpace a0q = agent0QSequence.get(i);
			QSpace a1q = agent1QSequence.get(i);
			
			buf.append(a0q.getQFor(agent0QQueryStates.get(0), new GroundedSingleAction(agent0.getAgentName(), d.getSingleAction(TBForageSteal.ACTIONSTEAL), ""))).append(",");
			buf.append(a1q.getQFor(agent1QQueryStates.get(0), new GroundedSingleAction(agent1.getAgentName(), d.getSingleAction(TBForageSteal.ACTIONSTEAL), ""))).append(",");
			
			buf.append(a0q.getQFor(agent0QQueryStates.get(1), new GroundedSingleAction(agent0.getAgentName(), d.getSingleAction(TBForageSteal.ACTIONPUNCH), ""))).append(",");
			buf.append(a1q.getQFor(agent1QQueryStates.get(1), new GroundedSingleAction(agent1.getAgentName(), d.getSingleAction(TBForageSteal.ACTIONPUNCH), ""))).append(",");
			
			buf.append(a0q.getQFor(agent0QQueryStates.get(2), new GroundedSingleAction(agent0.getAgentName(), d.getSingleAction(TBForageSteal.ACTIONPUNCH), ""))).append(",");
			buf.append(a1q.getQFor(agent1QQueryStates.get(2), new GroundedSingleAction(agent1.getAgentName(), d.getSingleAction(TBForageSteal.ACTIONPUNCH), ""))).append("\n");
			
			
			
		}
		
		return buf.toString();
		
	}
	
	
	public void runGame(){
		
		TerminalFunction tf = this.world.getTF();
		
		agent0.gameStarting();
		agent1.gameStarting();
		
		this.world.generateNewCurrentState();
		int agentToScore = this.whichAgentToScoreForStartState(this.world.getCurrentWorldState());
		String agentToScoreName = agent0.getAgentName();
		if(agentToScore == 1){
			agentToScoreName = agent1.getAgentName();
		}

		int score = 0;
		

		int t = 0;
		while(!tf.isTerminal(this.world.getCurrentWorldState()) && t < this.maxStages){
			this.recordQStatus();
			State curState = this.world.getCurrentWorldState();
			this.world.runStage();
			this.jointActionSequence.add(this.world.getLastJointAction());
			JointAction ja = this.world.getLastJointAction();
			score += this.actionScore(curState, agentToScoreName, ja, t);
			t++;
		}
		
		
		agent0.gameTerminated();
		agent1.gameTerminated();
		
		if(agentToScore == 0){
			this.agent0Scores.add(score);
		}
		else{
			this.agent1Scores.add(score);
		}
		
		
	}
	
	

	
	protected int actionScore(State s, String agentToScore, JointAction ja, int timeStep){
		
		if(timeStep > 2){
			return 0;
		}
		
		for(GroundedSingleAction gsa : ja){
			String aname = gsa.action.actionName;
			if(aname.equals(TBForageSteal.ACTIONSTEAL) || aname.equals(TBForageSteal.ACTIONPUNCH)){
				return 1;
			}
		}
		
		return 0;
	}
	
	protected int whichAgentToScoreForStartState(State s){
		
		ObjectInstance a0Ob = s.getObject(agent0.getAgentName());
		if(a0Ob.getDiscValForAttribute(TBForageSteal.ATTISTURN) == 1){
			return 0;
		}
		
		return 1;
	}
	
	
	protected String paramRep(double [] params){
		
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < params.length; i++){
			if(i > 0){
				buf.append("_");
			}
			buf.append(params[i]);
		}
		
		
		return buf.toString();
		
	}
	
}
