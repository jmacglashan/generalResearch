package ethics.experiments.fssimple;

import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.Strategy;
import burlap.behavior.stochasticgame.agents.SetStrategyAgent;
import burlap.behavior.stochasticgame.agents.naiveq.SGEQGreedy;
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
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;

public class SimpleLearningExp {

	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		saTest();
		//maTest();

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
		
		SGQLAgent a1 = new SGQLAgent(domain, .99, 0.1, -25., hashingFactory);
		//a1.setStrategy(new SGEQGreedy(a1, 0.));
		
		World w = new World(domain, jam, r, new NullTermination(), sg);
		
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		cs.myAgentName = a0.getAgentName();
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(5000);
		
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
	
	
	public static void saTest(){
		
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 2, 1.);
		
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
		
		State s = GraphDefinedDomain.getState(domain, 0);
		
		//vi.planFromState(s);
		
		QLearning ql = new QLearning(domain, rf, new NullTermination(), 0.99, hashingFactory, -25, 0.1);
		EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s, 5000);
		
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			System.out.println("R: " + ea.getReward(i));
		}
		
		State ps = GraphDefinedDomain.getState(domain, 2);
		
		//List<QValue> qs = vi.getQs(ps);
		List<QValue> qs = ql.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + q.a.actionName());
		}
		
		
	}
	
	
	
	
	
	
	
	public static class FSSimpleSG extends SGStateGenerator{

		SGDomain domain;
		
		public FSSimpleSG(SGDomain domain){
			this.domain = domain;
		}
		
		@Override
		public State generateState(List<Agent> agents) {
			
			String p0Name = agents.get(0).getAgentName();
			String p1Name = agents.get(1).getAgentName();
			
			State s = FSSimple.getInitialState(this.domain, "player0", "player1", 0);
			
			return s;
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
				if(sn == 1){
					n1andsteal++;
					n1++;
				}
				return new GroundedSingleAction(this.myAgentName, this.domain.getSingleAction(FSSimple.ACTIONSTEAL), "");
			}
			else if(sn == 1){
				n1++;
				new GroundedSingleAction(this.myAgentName, this.domain.getSingleAction(FSSimple.ACTIONFORAGEBASE+0), "");
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

}
