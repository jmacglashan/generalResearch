package ethics.experiments.fssimple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.datastructures.HashedAggregator;
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
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleBTJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import ethics.experiments.fssimple.aux.FSSimpleSG;

public class StrategyOutcome {

	
	FSSimple gen = new FSSimple();;
	SGDomain domain;
	JointActionModel jam = new FSSimpleBTJAM(0.2);
	JointReward jr = new FSSimpleJR(1., -0.5, -2.5, 0.);
	AgentType at;
	SGStateGenerator sg;
	DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
	double discount = 0.95;
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		GraphDefinedDomain gdg = new GraphDefinedDomain(2);
		gdg.setTransition(0, 0, 1, 1.);
		gdg.setTransition(1, 0, 0, 1.);
		Domain d = gdg.generateDomain();
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				if(GraphDefinedDomain.getNodeId(sprime) == 1){
					return 1;
				}
				return 0;
			}
		};
		ValueIteration vi = new ValueIteration(d, rf, new NullTermination(), 0.95, new DiscreteStateHashFactory(), 0.000001, 1000);
		vi.planFromState(GraphDefinedDomain.getState(d, 0));
		System.out.println(vi.value(GraphDefinedDomain.getState(d, 0)));
		*/
		
		
		
		StrategyOutcome exp = new StrategyOutcome();
		exp.printRPlotCode(exp.objectiveZeroQDist(1000));
		//exp.printRPlotCode(exp.objectiveConstantQDist(1000));
		//exp.printRPlotCode(exp.objectiveRandomVariableQDist(1000));


	}
	
	
	
	public StrategyOutcome(){
		this.domain = (SGDomain)gen.generateDomain();
		this.sg = new FSSimpleSG(this.domain);
		this.at = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
	}
	
	public void printDist(Map<JointStrategy, Double> dist){
		for(Map.Entry<JointStrategy, Double> e : dist.entrySet()){
			System.out.println(e.getKey() + ": " + e.getValue());
		}
	}
	
	public void printRPlotCode(Map<JointStrategy, Double> dist){
		List<JointStrategy> keys = new ArrayList<StrategyOutcome.JointStrategy>(dist.keySet());
		List<String> kquoted = new ArrayList<String>(keys.size());
		System.out.print("barplot(");
		List<Double> vals = new ArrayList<Double>(dist.size());
		for(JointStrategy k : keys){
			vals.add(dist.get(k));
			kquoted.add("\"" + k + "\"");
		}
		System.out.print("c(" + strJoin(vals, ",") + "), names=c(" + strJoin(kquoted, ",") + "), cex.names=0.7)");
		
	}
	
	public Map<JointStrategy, Double> sumsToDist(HashedAggregator<JointStrategy> distSums, int n){
		Map<JointStrategy, Double> dist = new HashMap<StrategyOutcome.JointStrategy, Double>(distSums.size());
		for(Map.Entry<JointStrategy, Double> e : distSums.entrySet()){
			double p = e.getValue() / (double)n;
			dist.put(e.getKey(), p);
		}
		return dist;
	}
	
	public Map<JointStrategy, Double> objectiveZeroQDist(int n){
		
		HashedAggregator<JointStrategy> distSums = new HashedAggregator<StrategyOutcome.JointStrategy>();
		for(int i = 0; i < n; i++){
			if(i % 10 == 0){
				System.out.println("" + i);
			}
			distSums.add(this.objectiveZeroQ(), 1.);
		}
		
		return this.sumsToDist(distSums, n);
		
	}
	
	public Map<JointStrategy, Double> objectiveConstantQDist(int n){
		
		Random rand = RandomFactory.getMapped(0);
		
		double a0minq = 0.;
		double a0maxq = 1. / (1. - this.discount*this.discount);
		double a0rq = a0maxq - a0minq;
		
		double a1minq = (-this.discount) / (1. - this.discount*this.discount);
		double a1maxq = 0.;
		double a1rq = a1maxq - a1minq;
		
		HashedAggregator<JointStrategy> distSums = new HashedAggregator<StrategyOutcome.JointStrategy>();
		for(int i = 0; i < n; i++){
			if(i % 10 == 0){
				System.out.println("" + i);
			}
			
			double a0q = rand.nextDouble()*a0rq + a0minq;
			double a1q = rand.nextDouble()*a1rq + a1minq;
			
			distSums.add(this.objectiveConstantQ(a0q, a1q), 1.);
		}
		
		return this.sumsToDist(distSums, n);
		
	}
	
	
	public Map<JointStrategy, Double> objectiveRandomVariableQDist(int n){
		
		HashedAggregator<JointStrategy> distSums = new HashedAggregator<StrategyOutcome.JointStrategy>();
		for(int i = 0; i < n; i++){
			if(i % 10 == 0){
				System.out.println("" + i);
			}
			distSums.add(this.objectiveRandomVariableQ(), 1.);
		}
		
		return this.sumsToDist(distSums, n);
		
	}
	
	
	public JointStrategy objectiveZeroQ(){
		
		SGQLAgent a0 = new SGQLAgent(domain, this.discount, 0.0, 0., hashingFactory);
		a0.setLearningRate(new ExponentialDecayLR(0.1, 0.999, 0.01));
		
		//SGQLAgent a1 = new SGQLAgent(domain, this.discount, 0.0, 0., hashingFactory);
		SGQLAgent a1 = new SGQLAgent(domain, this.discount, 0.0, -6.5, hashingFactory);
		a1.setLearningRate(new ExponentialDecayLR(0.1, 0.999, 0.01));
		
		
		World w = new World(this.domain, this.jam, this.jr, new NullTermination(), this.sg);
		
		a0.joinWorld(w, this.at);
		a1.joinWorld(w, this.at);
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(1000);
		
		
		JointStrategy js = new JointStrategy(this.classifyThief(a0, a1), this.classifyPunisher(a0, a1));
		
		
		return js;
		
		
	}
	
	
	public JointStrategy objectiveConstantQ(double a0Q, double a1Q){
		
		SGQLAgent a0 = new SGQLAgent(domain, this.discount, 0.0, 0., hashingFactory);
		a0.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(a0Q));
		a0.setLearningRate(new ExponentialDecayLR(0.1, 0.999, 0.01));
		
		SGQLAgent a1 = new SGQLAgent(domain, this.discount, 0.0, 0., hashingFactory);
		a1.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(a1Q));
		a1.setLearningRate(new ExponentialDecayLR(0.1, 0.999, 0.01));
		
		
		World w = new World(this.domain, this.jam, this.jr, new NullTermination(), this.sg);
		
		a0.joinWorld(w, this.at);
		a1.joinWorld(w, this.at);
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(1000);
		
		
		JointStrategy js = new JointStrategy(this.classifyThief(a0, a1), this.classifyPunisher(a0, a1));
		
		
		return js;
		
	}
	
	
	public JointStrategy objectiveRandomVariableQ(){
		
		double a0minq = 0.;
		double a0maxq = 1. / (1. - this.discount*this.discount);
		
		double a1minq = (-this.discount) / (1. - this.discount*this.discount);
		double a1maxq = 0.;

		SGQLAgent a0 = new SGQLAgent(domain, this.discount, 0.0, 0., hashingFactory);
		//a0.setQValueInitializer(new RRangeQInit(a0minq, a0maxq));
		a0.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization());
		a0.setLearningRate(new ExponentialDecayLR(0.1, 0.999, 0.01));
		
		SGQLAgent a1 = new SGQLAgent(domain, this.discount, 0.0, 0., hashingFactory);
		a1.setQValueInitializer(new RRangeQInit(a1minq, a1maxq));
		a1.setLearningRate(new ExponentialDecayLR(0.1, 0.999, 0.01));
		
		
		World w = new World(this.domain, this.jam, this.jr, new NullTermination(), this.sg);
		
		a0.joinWorld(w, this.at);
		a1.joinWorld(w, this.at);
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(1000);
		
		
		JointStrategy js = new JointStrategy(this.classifyThief(a0, a1), this.classifyPunisher(a0, a1));
		
		
		return js;
	}
	
	
	protected State getState(String agent0Name, String agent1Name, int stateNode, int backTurned){
		State s = FSSimple.getInitialState(this.domain, agent0Name, agent1Name, backTurned, 0);
		FSSimple.setStateNode(s, stateNode);
		return s;
	}
	
	
	/**
	 * Index:
	 * 0: not punished, facing
	 * 1: not punished, back turned
	 * 2: punished, facing
	 * 3: punished, back turned
	 * @param a the agnet
	 * @return policy label where 1s indicate whether stealing is chosen
	 */
	protected int [] classifyThief(SGQLAgent a0, SGQLAgent a1){
		GreedyQPolicy p = new GreedyQPolicy(a0);
		int [] label = new int[4];
		
		label[0] = isSteal(p, this.getState(a0.getAgentName(), a1.getAgentName(), 0, 0));
		label[1] = isSteal(p, this.getState(a0.getAgentName(), a1.getAgentName(), 0, 1));
		label[2] = isSteal(p, this.getState(a0.getAgentName(), a1.getAgentName(), 1, 0));
		label[3] = isSteal(p, this.getState(a0.getAgentName(), a1.getAgentName(), 1, 1));
		
		
		return label;
	}
	
	protected int classifyPunisher(SGQLAgent a0, SGQLAgent a1){
		GreedyQPolicy p = new GreedyQPolicy(a1);
		return isPunish(p, this.getState(a0.getAgentName(), a1.getAgentName(), 2, 0));
	}
	
	
	protected int isSteal(Policy p, State s){
		GroundedSingleAction gsa = (GroundedSingleAction)p.getAction(s);
		if(gsa.actionName().equals(FSSimple.ACTIONSTEAL)){
			return 1;
		}
		return 0;
	}
	
	
	protected int isPunish(Policy p, State s){
		GroundedSingleAction gsa = (GroundedSingleAction)p.getAction(s);
		if(gsa.actionName().equals(FSSimple.ACTIONPUNISH)){
			return 1;
		}
		return 0;
	}
	
	
	public static String intArrayStr(int [] array){
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < array.length; i++){
			if(i > 0){
				buf.append(", ");
			}
			buf.append(array[i]);
		}
		
		return buf.toString();
	}
	
	
	
	
	
	
	public static class JointStrategy{
		int npf;
		int npbt;
		int pf;
		int pbt;
		int p;
		
		public JointStrategy(int [] thiefPolicy, int punisherPolicy){
			npf = thiefPolicy[0];
			npbt = thiefPolicy[1];
			pf = thiefPolicy[2];
			pbt = thiefPolicy[3];
			p = punisherPolicy;
		}
		
		
		@Override
		public String toString(){
			String join = "";
			return npf + join + npbt + join + pf + join + pbt + join + p;
		}
		
		@Override
		public int hashCode(){
			return this.toString().hashCode();
		}
		
		@Override
		public boolean equals(Object o){
			JointStrategy ojs = (JointStrategy)o;
			return this.npf == ojs.npf && this.npbt == ojs.npbt && this.pf == ojs.pf && this.pbt == ojs.pbt && this.p == ojs.p;
		}
		
	}
	
	
	public static String strJoin(List l, String token){
		
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < l.size(); i++){
			if(i > 0){
				buf.append(token);
			}
			buf.append(l.get(i).toString());
		}
		
		return buf.toString();
	}
	
	
	public static class RRangeQInit implements ValueFunctionInitialization{

		protected double lower;
		protected double range;
		protected Random rand = RandomFactory.getMapped(0);
		
		public RRangeQInit(double lower, double upper){
			this.lower = lower;
			this.range = upper-range;
		}
		
		@Override
		public double value(State s) {
			return rand.nextDouble()*this.range + this.lower;
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			return rand.nextDouble()*this.range + this.lower;
		}
		
	}

}
