package ethics.experiments.fssimple;

import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.Strategy;
import burlap.behavior.stochasticgame.agents.SetStrategyAgent;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.behavior.stochasticgame.agents.naiveq.SGQValue;
import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.WorldObserver;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import ethics.experiments.fssimple.aux.FSSimpleSG;
import ethics.experiments.fssimple.aux.PseudoGameCountWorld;
import ethics.experiments.fssimple.aux.RNPseudoTerm;

public class SimpleLearningExp {

	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//saTest(true);
		//saTest2(false);
		//maTest();
		maPTTest();

	}
	
	
	public static void maTest(){
		
		FSSimple gen = new FSSimple();
		SGDomain domain = (SGDomain)gen.generateDomain();
		JointActionModel jam = new FSSimpleJAM();
		JointReward r = new FSSimpleJR();
		AgentType at = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
		SGStateGenerator sg = new FSSimpleSG(domain);
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		//SGQLAgent a0 = new SGQLAgent(domain, .99, 0.1, 10., hashingFactory);
		
		ConditionalStealStrategy cs = new ConditionalStealStrategy("player0", domain, 0.1);
		SetStrategyAgent a0 = new SetStrategyAgent(domain, cs);
		
		SGQLAgent a1 = new SGQLAgent(domain, .99, 0.1, -27., hashingFactory);
		//a1.setStrategy(new SGEQGreedy(a1, 0.));
		
		World w = new World(domain, jam, r, new NullTermination(), sg);
		//w.addWorldObserver(new SimpleObsever());
		
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		cs.myAgentName = a0.getAgentName();
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(500);
		
		System.out.println("Starting test.");
		DPrint.toggleCode(w.getDebugId(), true);
		//w.runGame(100);
		
		//System.out.println(((double)cs.n1andsteal / (double)cs.n1));
		
		State s = FSSimple.getInitialState(domain, "player0", "player1", 0);
		s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).setValue(FSSimple.ATTSTATENODE, 2);
		
		List <SGQValue> qs = a1.getAllQsFor(s);
		for(SGQValue qv : qs){
			System.out.println(qv.q + "\t" + qv.gsa.action.actionName);
		}
		
	}
	
	
	public static void maPTTest(){
		
		FSSimple gen = new FSSimple();
		SGDomain domain = (SGDomain)gen.generateDomain();
		JointActionModel jam = new FSSimpleJAM();
		JointReward r = new FSSimpleJR();
		AgentType at = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
		SGStateGenerator sg = new FSSimpleSG(domain);
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		//SGQLAgent a0 = new SGQLAgent(domain, .99, 0.1, 10., hashingFactory);
		
		ConditionalStealStrategy cs = new ConditionalStealStrategy("player0", domain, 0.1);
		SetStrategyAgent a0 = new SetStrategyAgent(domain, cs);
		
		SGQLAgent a1 = new SGQLAgent(domain, .99, 0.1, -27., hashingFactory);
		//a1.setStrategy(new SGEQGreedy(a1, 0.));
		
		PseudoGameCountWorld w = new PseudoGameCountWorld(domain, jam, r, new NullTermination(), sg, new RNPseudoTerm());
		w.addWorldObserver(new SimpleObsever());
		
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		cs.myAgentName = a0.getAgentName();
		
		//DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(Integer.MAX_VALUE, 2);
		
		System.out.println("Starting PT test.");
		DPrint.toggleCode(w.getDebugId(), true);
		//w.runGame(100);
		
		//System.out.println(((double)cs.n1andsteal / (double)cs.n1));
		
		State s = FSSimple.getInitialState(domain, "player0", "player1", 0);
		s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).setValue(FSSimple.ATTSTATENODE, 2);
		
		List <SGQValue> qs = a1.getAllQsFor(s);
		for(SGQValue qv : qs){
			System.out.println(qv.q + "\t" + qv.gsa.action.actionName);
		}
		
	}
	
	
	public static void saTest(boolean useVI){
		
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		//gen.setTransition(0, 0, 2, 1.); //use this for deterministic strategy
		gen.setTransition(0, 0, 2, 0.9);
		gen.setTransition(0, 0, 0, 0.1);
		
		gen.setTransition(2, 0, 0, 1.); //action 0 is do nothing
		gen.setTransition(2, 1, 1, 1.); //action 1 is punish
		
		gen.setTransition(1, 0, 1, 0.9);
		gen.setTransition(1, 0, 2, .1);
		
		Domain domain = gen.generateDomain();
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				
				int spn = GraphDefinedDomain.getNodeId(sprime);
				if(spn == 2){
					return -1; //steal cost
				}
				
				int sn = GraphDefinedDomain.getNodeId(s);
				if(sn == 2 && spn == 1){
					return -2.;
				}
				
				return 0;
			}
		};
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		
		
		ValueIteration vi = new ValueIteration(domain, rf, new NullTermination(), 0.99, hashingFactory, 0.0001, 1000);
		QLearning ql = new QLearning(domain, rf, new NullTermination(), 0.99, hashingFactory, -25, 0.1);
		
		State s = GraphDefinedDomain.getState(domain, 0);
		
		QComputablePlanner qSource = vi;
		if(useVI){
			vi.planFromState(s);
		}
		else{
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s, 5000);
			for(int i = 0; i < ea.numTimeSteps()-1; i++){
				System.out.println("R: " + ea.getReward(i));
			}
			
			qSource = ql;
		}
		
		

		State ps = GraphDefinedDomain.getState(domain, 2);
		
		List<QValue> qs = qSource.getQs(ps);
		
		for(QValue q : qs){
			System.out.println(q.q + "\t" + q.a.actionName());
		}
		
		
	}
	
	public static void saTest2(boolean useVI){
		
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 0, 1.); //action 0 is forage
		gen.setTransition(0, 1, 2, 1.); //action 1 is steal
		
		gen.setTransition(2, 0, 0, 0.1); //0.1 response of nothing
		gen.setTransition(2, 0, 1, 0.9); //0.9 response of punish
		
		
		gen.setTransition(1, 0, 1, 1.); //action 0 is forage
		gen.setTransition(1, 1, 2, 1.); //action 1 is steal
		
		Domain domain = gen.generateDomain();
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				
				int sn = GraphDefinedDomain.getNodeId(s);
				int spn = GraphDefinedDomain.getNodeId(sprime);
				if(sn == 2){
					
					if(spn == 1){
						return -2; //punish cost
					}
					else{
						return 0.;
					}
				}
				else if(spn == 2){
					return 1.;
				}
				
				return 0;
			}
		};
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		
		
		ValueIteration vi = new ValueIteration(domain, rf, new NullTermination(), 0.99, hashingFactory, 0.0001, 1000);
		QLearning ql = new QLearning(domain, rf, new NullTermination(), 0.99, hashingFactory, 1, 0.1);
		
		State s = GraphDefinedDomain.getState(domain, 0);
		
		QComputablePlanner qSource = vi;
		if(useVI){
			vi.planFromState(s);
		}
		else{
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s, 5000);
			for(int i = 0; i < ea.numTimeSteps()-1; i++){
				//System.out.println("R: " + ea.getReward(i));
			}
			
			qSource = ql;
		}
		
		

		State ps = GraphDefinedDomain.getState(domain, 0);
		List<QValue> qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + q.a.actionName());
		}
		
		
		System.out.println("----------------");
		ps = GraphDefinedDomain.getState(domain, 1);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + q.a.actionName());
		}
		
		
	}
	
	
	
	
	
	
	
	
	
	public static class ConditionalStealStrategy extends Strategy{

		public String myAgentName;
		public SGDomain domain;
		double randProb;
		Random rand;
		

		int n1 = 0;
		int n1andsteal = 0;
		
		public ConditionalStealStrategy(String agentName, SGDomain domain, double randProb){
			this.myAgentName = agentName;
			this.domain = domain;
			this.randProb = randProb;
			this.rand = RandomFactory.getMapped(0);
		}
		
		@Override
		public GroundedSingleAction getAction(State s) {
			
			
			int sn = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).getDiscValForAttribute(FSSimple.ATTSTATENODE);
			
			double r = this.rand.nextDouble();
			
			if(sn == 0 || (sn == 1 && r < this.randProb)){
				return new GroundedSingleAction(this.myAgentName, this.domain.getSingleAction(FSSimple.ACTIONSTEAL), "");
			}
			else if(sn == 1){
				return new GroundedSingleAction(this.myAgentName, this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), "");
			}
			
			
			return new GroundedSingleAction(this.myAgentName, this.domain.getSingleAction(FSSimple.ACTIONDONOTHING), "");
		}

		@Override
		public List<SingleActionProb> getActionDistributionForState(State s) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isStochastic() {
			return true;
		}
		
	}
	
	
	
	public static class SimpleObsever implements WorldObserver{

		
		@Override
		public void observe(State s, JointAction ja,
				Map<String, Double> reward, State sp) {
			
			int sn0 = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).getDiscValForAttribute(FSSimple.ATTSTATENODE);
			int sn1 = s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).getDiscValForAttribute(FSSimple.ATTSTATENODE);
			
			System.out.println(reward.get("player1") + "\t" + ja.action("player0").action.actionName + "\t" + sn0 + " " + sn1);
			
		}
		
		
		
	}

}
