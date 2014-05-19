package ethics.experiments.fssimple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.SetStrategyAgent;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.AbstractGroundedAction;
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
		
		//saTest(false);
		//saTest2(false);
		saTest3(true);
		//maTest();
		//maPTTest();
		//testQLPolicyDist(1000);

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
		
		List <QValue> qs = a1.getQs(s);
		for(QValue qv : qs){
			System.out.println(qv.q + "\t" + qv.a.actionName());
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
		
		List <QValue> qs = a1.getQs(s);
		for(QValue qv : qs){
			System.out.println(qv.q + "\t" + qv.a.actionName());
		}
		
	}
	
	
	/**
	 * Use this method for testing a punish learner
	 * @param useVI
	 */
	public static void saTest(boolean useVI){
		
		double opponetError = 0.1;
		
		Domain domain = getDomainForSAPunisherPlayingAgainstAContingentStealer(opponetError);
		//Domain domain = getDomainForSAPunisherPlayingAgainstAConstantStealer(opponetError);
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				
				int spn = GraphDefinedDomain.getNodeId(sprime);
				if(spn == 2){
					return -1; //steal cost
				}
				
				int sn = GraphDefinedDomain.getNodeId(s);
				if(sn == 2 && spn == 1){
					return -2.; //punish cost incurred when transitioning from my decision state to the punish state
				}
				
				return 0;
			}
		};
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		double discount = 0.95;
		//double initQ = -4.8;
		double initQ = -4.9;
		
		
		ValueIteration vi = new ValueIteration(domain, rf, new NullTermination(), discount, hashingFactory, 0.0001, 1000);
		QLearning ql = new QLearning(domain, rf, new NullTermination(), discount, hashingFactory, initQ, 0.01);
		ql.setLearningRateFunction(new ExponentialDecayLR(0.1, 0.99, 0.001));
		//ql.setQInitFunction(new OptPunisherQOnPessimisticOpponent());
		ql.setQInitFunction(new PunisherQForContingent());
		
		
		State s = GraphDefinedDomain.getState(domain, 0);
		
		QComputablePlanner qSource = vi;
		if(useVI){
			vi.planFromState(s);
		}
		else{
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s, 1000);
			/*
			for(int i = 0; i < ea.numTimeSteps()-1; i++){
				System.out.println("R: " + ea.getReward(i));
			}*/
			
			qSource = ql;
		}
		
		

		State ps = GraphDefinedDomain.getState(domain, 2);
		
		List<QValue> qs = qSource.getQs(ps);
		
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("action0", "Do nothing");
		aMap.put("action1", "Punish");
		
		for(QValue q : qs){
			System.out.println(q.q + "\t" + aMap.get(q.a.actionName()));
		}
		
		System.out.println("----------------------");
		
		ps = GraphDefinedDomain.getState(domain, 0);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + "S0");
		}
		
		System.out.println("----------------------");
		
		ps = GraphDefinedDomain.getState(domain, 1);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + "S1");
		}
		
		
	}
	
	
	public static Domain getDomainForSAPunisherPlayingAgainstAContingentStealer(double opponentError){
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 2, 1.-opponentError);
		gen.setTransition(0, 0, 0, opponentError);
		
		gen.setTransition(2, 0, 0, 1.); //action 0 is do nothing
		gen.setTransition(2, 1, 1, 1.); //action 1 is punish
		
		gen.setTransition(1, 0, 1, 1.-opponentError);
		gen.setTransition(1, 0, 2, opponentError);
		
		Domain domain = gen.generateDomain();
		
		return domain;
	}
	
	public static Domain getDomainForSAPunisherPlayingAgainstAConstantStealer(double opponentError){
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 2, 1.-opponentError);
		gen.setTransition(0, 0, 0, opponentError);
		
		gen.setTransition(2, 0, 0, 1.); //action 0 is do nothing
		gen.setTransition(2, 1, 1, 1.); //action 1 is punish
		
		gen.setTransition(1, 0, 2, 1.-opponentError);
		gen.setTransition(1, 0, 1, opponentError);
		
		Domain domain = gen.generateDomain();
		
		return domain;
	}
	
	
	
	
	
	
	
	/**
	 * Use this method for testing a forage learner
	 * @param useVI
	 */
	public static void saTest2(boolean useVI){
		
		double opponentError = 0.1;
		
		//Domain domain = getDomainForSAStealerPlayingAgainstPunisher(opponentError);
		Domain domain = getDomainForSAStealerPlayingAgainstPassive(opponentError);
		
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
		
		double discount = 0.92;
		double qInit = 0.0;
		
		ValueIteration vi = new ValueIteration(domain, rf, new NullTermination(), discount, hashingFactory, 0.0001, 1000);
		QLearning ql = new QLearning(domain, rf, new NullTermination(), discount, hashingFactory, qInit, 0.1);
		ql.setLearningRateFunction(new ExponentialDecayLR(0.2, 0.99, 0.001));
		
		State s = GraphDefinedDomain.getState(domain, 0);
		
		QComputablePlanner qSource = vi;
		if(useVI){
			vi.planFromState(s);
		}
		else{
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s, 50);
			for(int i = 0; i < ea.numTimeSteps()-1; i++){
				//System.out.println("R: " + ea.getReward(i));
			}
			
			qSource = ql;
		}
		
		
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("action0", "forage");
		aMap.put("action1", "steal");
		

		State ps = GraphDefinedDomain.getState(domain, 0);
		List<QValue> qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + aMap.get(q.a.actionName()));
		}
		
		
		System.out.println("----------------");
		ps = GraphDefinedDomain.getState(domain, 1);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + aMap.get(q.a.actionName()));
		}
		
		
	}
	
	
	
	
	public static Domain getDomainForSAStealerPlayingAgainstPunisher(double opponetError){
		
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 0, 1.); //action 0 is forage
		gen.setTransition(0, 1, 2, 1.); //action 1 is steal
		
		gen.setTransition(2, 0, 0, opponetError); 
		gen.setTransition(2, 0, 1, 1.-opponetError);
		
		
		gen.setTransition(1, 0, 1, 1.); //action 0 is forage
		gen.setTransition(1, 1, 2, 1.); //action 1 is steal
		
		return gen.generateDomain();
		
	}
	
	public static Domain getDomainForSAStealerPlayingAgainstPassive(double opponetError){
		
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 0, 1.); //action 0 is forage
		gen.setTransition(0, 1, 2, 1.); //action 1 is steal
		
		gen.setTransition(2, 0, 1, opponetError); 
		gen.setTransition(2, 0, 0, 1.-opponetError);
		
		
		gen.setTransition(1, 0, 1, 1.); //action 0 is forage
		gen.setTransition(1, 1, 2, 1.); //action 1 is steal
		
		return gen.generateDomain();
		
	}
	
	
	
	public static void testQLPolicyDist(int n){
		double sumPunish = 0.;
		double sumDoNothing = 0.;
		
		for(int i = 0; i < n; i++){
			if(saTest3(false) == 1){
				sumPunish += 1.;
			}
			else{
				sumDoNothing += 1.;
			}
		}
		
		double pPunish = sumPunish / (double)n;
		double pDoNothing = sumDoNothing / (double)n;
		
		System.out.println("barplot(c(" + pPunish + ", " + pDoNothing + "), names=c(\"Punish\", \"Do Nothing\"))");
	}
	
	
	/**
	 * Use this method for testing a punish learner
	 * @param useVI
	 */
	public static int saTest3(boolean useVI){
		
		final double opponetError = 0.1;
		
		Domain domain = getDomainForSAPunisherPlayingAgainstAContingentStealerWithBackTurning(opponetError, 0.2);
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				
				int spn = GraphDefinedDomain.getNodeId(sprime);
				int sn = GraphDefinedDomain.getNodeId(s);
				
				//punisher turn
				if(sn == 4){
					if(spn == 2 || spn == 3){
						//punished
						return -.5;
					}
					else{
						return 0.; //punisher did nothing
					}
				}
				
				if(sn == 0 || sn == 2){
					if(spn == 4){
						return -1.; //stealer stole
					}
					else{
						return 0.; //stealer did not steal
					}
				}
				else{
					//expected value of action in back turned
					return (1. - opponetError) * -1;
				}
				
				
			}
		};
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		double discount = 0.95;
		//double initQ = -4.8;
		double initQ = -6.5;
		
		
		ValueIteration vi = new ValueIteration(domain, rf, new NullTermination(), discount, hashingFactory, 0.000001, 1000);
		QLearning ql = new QLearning(domain, rf, new NullTermination(), discount, hashingFactory, initQ, 0.01);
		ql.setLearningRateFunction(new ExponentialDecayLR(0.1, 0.999, 0.01));
		//ql.setQInitFunction(new OptPunisherQOnPessimisticOpponent());
		//ql.setQInitFunction(new PunisherQForContingent());
		
		
		State s = GraphDefinedDomain.getState(domain, 0);
		
		QComputablePlanner qSource = vi;
		if(useVI){
			vi.planFromState(s);
		}
		else{
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s, 1000);
			/*
			for(int i = 0; i < ea.numTimeSteps()-1; i++){
				System.out.println("R: " + ea.getReward(i));
			}*/
			
			qSource = ql;
		}
		
		

		State ps = GraphDefinedDomain.getState(domain, 4);
		
		
		/*
		Policy gp = new GreedyQPolicy(qSource);
		AbstractGroundedAction aselection = gp.getAction(ps);
		//System.out.println("Chosen = " + aselection.actionName());
		if(aselection.actionName().equals("action1")){
			//System.out.println("Returning 1");
			return 1;
		}
		else{
			return 0;
		}
		*/
		
		
		List<QValue> qs = qSource.getQs(ps);
		
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("action0", "Do nothing");
		aMap.put("action1", "Punish");
		
		for(QValue q : qs){
			System.out.println(q.q + "\t" + aMap.get(q.a.actionName()));
		}
		
		System.out.println("----------------------");
		
		ps = GraphDefinedDomain.getState(domain, 0);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + "S0");
		}
		
		System.out.println("----------------------");
		
		ps = GraphDefinedDomain.getState(domain, 1);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + "S1");
		}
		
		System.out.println("----------------------");
		
		ps = GraphDefinedDomain.getState(domain, 2);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + "S2");
		}
		
		System.out.println("----------------------");
		
		ps = GraphDefinedDomain.getState(domain, 3);
		qs = qSource.getQs(ps);
		for(QValue q : qs){
			System.out.println(q.q + "\t" + "S3");
		}
		
		
		return 0;
		
	}
	
	
	
	
	public static Domain getDomainForSAPunisherPlayingAgainstAContingentStealerWithBackTurning(double opponentError, double probBT){
		
		double opponentIE = 1. - opponentError;
		double pIBT = 1. - probBT;
		
		GraphDefinedDomain gen = new GraphDefinedDomain(5);
		
		//stealer no response with punisher facing
		gen.setTransition(0, 0, 4, opponentIE);
		gen.setTransition(0, 0, 0, opponentError * pIBT);
		gen.setTransition(0, 0, 1, opponentError * probBT);
		
		//stealer no response with punisher turned
		gen.setTransition(1, 0, 1, probBT);
		gen.setTransition(1, 0, 0, pIBT);
		
		
		
		//stealer punished with punisher facing
		gen.setTransition(2, 0, 4, opponentError);
		gen.setTransition(2, 0, 2, opponentIE * pIBT);
		gen.setTransition(2, 0, 3, opponentIE * probBT);
		
		//stealer no response with punisher turned
		gen.setTransition(3, 0, 3, probBT);
		gen.setTransition(3, 0, 2, pIBT);
		
		
		
		//punisher does nothing
		gen.setTransition(4, 0, 0, pIBT);
		gen.setTransition(4, 0, 1, probBT);
		
		//punisher punishers
		gen.setTransition(4, 1, 2, pIBT);
		gen.setTransition(4, 1, 3, probBT);

		
		Domain domain = gen.generateDomain();
		
		return domain;
		
	}
	
	
	
	
	
	
	
	
	
	public static class OptPunisherQOnPessimisticOpponent implements ValueFunctionInitialization {

		QComputablePlanner planner;
		
		public OptPunisherQOnPessimisticOpponent(){
			
			Domain domain = getDomainForSAPunisherPlayingAgainstAConstantStealer(0.1);
			
			RewardFunction rf = new RewardFunction() {
				
				@Override
				public double reward(State s, GroundedAction a, State sprime) {
					
					int spn = GraphDefinedDomain.getNodeId(sprime);
					if(spn == 2){
						return -1; //steal cost
					}
					
					int sn = GraphDefinedDomain.getNodeId(s);
					if(sn == 2 && spn == 1){
						return -2.; //punish cost incurred when transitioning from my decision state to the punish state
					}
					
					return 0;
				}
			};
			
			DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
			double discount = 0.92;
			planner = new ValueIteration(domain, rf, new NullTermination(), discount, hashingFactory, 0.0001, 1000);
			State s = GraphDefinedDomain.getState(domain, 0);
			
			((OOMDPPlanner)planner).planFromState(s);
			
			
		}
		
		@Override
		public double value(State s) {
			double maxQ = Double.NEGATIVE_INFINITY;
			List<QValue> qs = planner.getQs(s);
			for(QValue q : qs){
				if(q.q > maxQ){
					maxQ = q.q;
				}
			}
			return maxQ;
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return this.value(s);
		}
		
		
		
	}
	
	
	public static class PunisherQForContingent implements ValueFunctionInitialization {

		QComputablePlanner planner;
		
		public PunisherQForContingent(){
			
			Domain domain = getDomainForSAPunisherPlayingAgainstAContingentStealer(0.1);
			
			RewardFunction rf = new RewardFunction() {
				
				@Override
				public double reward(State s, GroundedAction a, State sprime) {
					
					int spn = GraphDefinedDomain.getNodeId(sprime);
					if(spn == 2){
						return -1; //steal cost
					}
					
					int sn = GraphDefinedDomain.getNodeId(s);
					if(sn == 2 && spn == 1){
						return -2.; //punish cost incurred when transitioning from my decision state to the punish state
					}
					
					return 0;
				}
			};
			
			DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
			double discount = 0.95;
			planner = new ValueIteration(domain, rf, new NullTermination(), discount, hashingFactory, 0.0001, 1000);
			State s = GraphDefinedDomain.getState(domain, 0);
			
			((OOMDPPlanner)planner).planFromState(s);
			
			
		}
		
		@Override
		public double value(State s) {
			return ((ValueFunctionPlanner)planner).value(s);
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return this.planner.getQ(s, a).q;
		}
		
		
		
	}
	
	
	
	
	public static class ConditionalStealStrategy extends Policy{

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
		public AbstractGroundedAction getAction(State s) {
			
			
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
		public List<ActionProb> getActionDistributionForState(State s) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isStochastic() {
			return true;
		}

		@Override
		public boolean isDefinedFor(State s) {
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
