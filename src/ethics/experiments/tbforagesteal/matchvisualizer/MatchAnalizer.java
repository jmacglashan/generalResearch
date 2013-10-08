package ethics.experiments.tbforagesteal.matchvisualizer;

import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.behavior.stochasticgame.agents.naiveq.SGQValue;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.stocashticgames.AgentFactory;
import burlap.oomdp.stocashticgames.AgentType;
import burlap.oomdp.stocashticgames.GroundedSingleAction;
import burlap.oomdp.stocashticgames.JointAction;
import burlap.oomdp.stocashticgames.JointReward;
import burlap.oomdp.stocashticgames.SGDomain;
import burlap.oomdp.stocashticgames.World;
import burlap.oomdp.stocashticgames.WorldGenerator;
import burlap.oomdp.stocashticgames.common.AgentFactoryWithSubjectiveReward;
import burlap.oomdp.stocashticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.TBFSAlternatingTurnSG;
import domain.stocasticgames.foragesteal.TBFSStandardMechanics;
import domain.stocasticgames.foragesteal.TBFSStandardReward;
import domain.stocasticgames.foragesteal.TBForageSteal;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import ethics.experiments.tbforagesteal.aux.TBFSSubjectiveRF;

public class MatchAnalizer {

	protected SGQLAgent					agent0;
	protected SGQLAgent					agent1;
	protected World						world;
	
	protected List <State>				agent0QQueryStates;
	protected List <State>				agent1QQueryStates;
	
	protected List<JointAction>			jointActionSequence;

	protected List <QSpace>				agent0QSequence;
	protected List <QSpace>				agent1QSequence;
	
	
	
	
	public static void main(String [] args){
		
		
		SGDomain domain = (SGDomain) TBForageSteal.generateDomain();
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		double discount = 0.99;
		double learningRate = 0.1;
		
		AgentFactory af = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());

		
		JointReward rf = new TBFSStandardReward();
		JointReward punchFavored = new TBFSSubjectiveRF(rf, new double[]{0.0, 2.0, -2.0});
		
		WorldGenerator worldGenerator = new ConstantWorldGenerator(domain, new TBFSStandardMechanics(), rf, new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG());
		
		AgentType at = new AgentType("default", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		
		AgentFactory afpf = new AgentFactoryWithSubjectiveReward(af, punchFavored);
		
		SGQLAgent agent0 = (SGQLAgent)af.generateAgent();
		SGQLAgent agent1 = (SGQLAgent)afpf.generateAgent();
		
		World world = worldGenerator.generateWorld();
		agent0.joinWorld(world, at);
		agent1.joinWorld(world, at);
		
		
		String [] anames = new String []{agent0.getAgentName(), agent1.getAgentName()};
		List <State> agent0QueryStates = getQueryStates(anames, 0);
		List <State> agent1QueryStates = getQueryStates(anames, 1);
		
		
		
		DPrint.toggleCode(world.getDebugId(), false);
		
		MatchAnalizer ma = new MatchAnalizer(world, agent0, agent1, agent0QueryStates, agent1QueryStates);
		ma.runMatch(1000);
		
		System.out.println(ma.numQSpaceMeasure());
		System.out.println(ma.numJARecords());
		
		System.out.println(TBForageSteal.ACTIONSTEAL + ": " + ma.getQFor(0, 0, agent0QueryStates.get(0), 
				new GroundedSingleAction(agent0.getAgentName(), domain.getSingleAction(TBForageSteal.ACTIONSTEAL), "")));
		
	}
	
	
	public static List <State> getQueryStates(String [] anames, int forAgent){
		
		List <State> res = new ArrayList<State>();
		
		int opponent = 1;
		if(forAgent == 1){
			opponent = 0;
		}
		
		int [] arrayFA = new int[TBForageSteal.NALTS];
		for(int i = 0; i < TBForageSteal.NALTS; i++){
			arrayFA[i] = i;
		}
		
		State s0 = TBForageSteal.getGameStartState(arrayFA, forAgent);
		List <ObjectInstance> agentObs = s0.getObjectsOfTrueClass(TBForageSteal.CLASSAGENT);
		s0.renameObject(agentObs.get(0), anames[0]);
		s0.renameObject(agentObs.get(1), anames[1]);
		
		res.add(s0);
		
		
		//now do when their opponent stole from them
		State s1 = s0.copy();
		ObjectInstance opponentOb = s1.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 2);
		
		res.add(s1);
		
		
		
		//now do when they are in punching match
		State s2 = s0.copy();
		ObjectInstance qAgent = s2.getObject(anames[forAgent]);
		qAgent.setValue(TBForageSteal.ATTPTA, 3);
		
		opponentOb = s2.getObject(anames[opponent]);
		opponentOb.setValue(TBForageSteal.ATTPTA, 3);
		
		res.add(s2);
		
		
		return res;
		
	}
	
	
	
	
	public MatchAnalizer(World world, SGQLAgent agent0, SGQLAgent agent1, List <State> agent0QQueryStates, List <State> agent1QQueryStates) {
		
		this.world = world;
		
		this.agent0 = agent0;
		this.agent1 = agent1;
		
		this.agent0QQueryStates = agent0QQueryStates;
		this.agent1QQueryStates = agent1QQueryStates;
		
		this.jointActionSequence = new ArrayList<JointAction>();
		
		this.agent0QSequence = new ArrayList<MatchAnalizer.QSpace>();
		this.agent1QSequence = new ArrayList<MatchAnalizer.QSpace>();
		
		
		//this.runMatch(maxGames);
		
		
	}
	
	
	public int numQSpaceMeasure(){
		return this.agent0QSequence.size(); //same number as agent 2
	}
	
	public int numJARecords(){
		return this.jointActionSequence.size();
	}
	
	public double getObjectiveCumulativeReward(int aid){
		if(aid == 0){
			return this.world.getCumulativeRewardForAgent(agent0.getAgentName());
		}
		else{
			return this.world.getCumulativeRewardForAgent(agent1.getAgentName());
		}
	}
	
	public QSpace getQSpace(int agentInd, int timeIndex){
		if(agentInd == 0){
			return this.agent0QSequence.get(timeIndex);
		}
		else{
			return this.agent1QSequence.get(timeIndex);
		}
	}
	
	public double getQFor(int agentInd, int timeIndex, State s, GroundedSingleAction gsa){
		return this.getQSpace(agentInd, timeIndex).getQFor(s, gsa);
	}
	
	public JointAction getJointActionAtTime(int timeIndex){
		return this.jointActionSequence.get(timeIndex);
	}
	
	
	protected void runMatch(int maxGames){
		
		for(int i = 0; i < maxGames; i++){
			this.runGame();
		}
		this.recordQStatus();
		
	}
	
	protected void runGame(){
		
		TerminalFunction tf = this.world.getTF();
		
		agent0.gameStarting();
		agent1.gameStarting();
		
		this.world.generateNewCurrentState();
		

		int t = 0;
		while(!tf.isTerminal(this.world.getCurrentWorldState()) && t < 100){
			this.recordQStatus();
			this.world.runStage();
			this.jointActionSequence.add(this.world.getLastJointAction());
			t++;
		}
		
		
		agent0.gameTerminated();
		agent1.gameTerminated();
		
		
	}
	
	
	protected void recordQStatus(){
		
		QSpace a0Space = new QSpace();
		for(State s : agent0QQueryStates){
			QResult res = new QResult(s, agent0.getAllQsFor(s));
			a0Space.addQResult(res);
		}
		agent0QSequence.add(a0Space);
		
		QSpace a1Space = new QSpace();
		for(State s : agent1QQueryStates){
			QResult res = new QResult(s, agent1.getAllQsFor(s));
			a1Space.addQResult(res);
		}
		agent1QSequence.add(a1Space);
		
	}
	
	
	
	
	public class QSpace{
		
		public List<QResult> qResults;
		
		public QSpace(List <QResult> qResults){
			this.qResults = qResults;
		}
		
		public QSpace(){
			this.qResults = new ArrayList<MatchAnalizer.QResult>();
		}
		
		public void addQResult(QResult qr){
			this.qResults.add(qr);
		}
		
		public double getQFor(State s, GroundedSingleAction gsa){
			
			for(QResult qr : qResults){
				if(qr.s.equals(s)){
					for(SGQValue qe : qr.qEntries){
						if(qe.gsa.equals(gsa)){
							return qe.q;
						}
					}
				}
			}
			
				
			throw new RuntimeErrorException(new Error("No Q index for the queried state"));
			
		}
		
	}
	
	public class QResult{
		
		public State s;
		public List <SGQValue> qEntries;
		
		
		public QResult(State s, List<SGQValue> qEntries){
			this.s = s;
			this.qEntries = new ArrayList<SGQValue>(qEntries.size());
			for(SGQValue qe : qEntries){
				this.qEntries.add(new SGQValue(qe));
			}
		}
		
	}
	
}
