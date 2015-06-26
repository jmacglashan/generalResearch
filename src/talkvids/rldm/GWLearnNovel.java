package talkvids.rldm;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class GWLearnNovel {

	public GridWorldDomain gwd;
	public Domain domain;
	public State s;
	public Visualizer v;
	public DiscreteMaskHashingFactory hashingFactory;

	public GWLearnNovel(){

		this.gwd = new GridWorldDomain(11,11);
		this.gwd.setMapToFourRooms();
		this.domain = gwd.generateDomain();
		//this.s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);
		this.v = GridWorldVisualizer.getVisualizer(gwd.getMap());

		this.s = GridWorldDomain.getOneAgentNLocationState(domain, 1);
		GridWorldDomain.setAgent(s, 0, 0);
		GridWorldDomain.setLocation(s, 0, 10, 10);

		hashingFactory = new DiscreteMaskHashingFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

	}

	public QLearning getQLAgent(){



		QLearning ql = new QLearning(domain, null, null, 0.99, hashingFactory, 0., 1.);
		return ql;
	}

	public void runLearning(QLearning agent, RewardFunction rf, TerminalFunction tf, int nsteps, int visualize){

		agent.setRf(rf);
		agent.setTf(tf);

		if(visualize == 2){
			VisualActionObserver ob = new VisualActionObserver(domain, v);
			ob.setFrameDelay((long)(1./24.*1000));
			((SADomain)domain).addActionObserverForAllAction(ob);
			ob.initGUI();
			v.updateState(s);
		}

		if(visualize > 0){
			try {
				Thread.sleep(25000);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}

		int c = 0;
		while(nsteps > 0){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(s, nsteps);
			System.out.println(c + ": " + ea.numTimeSteps());
			v.updateState(s);
			if(visualize > 0) {
				try {
					Thread.sleep((long) (1. / 24. * 1000));
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			c++;
			nsteps -= ea.maxTimeStep();
		}

	}


	public void runQFunctionRenderLearning(QLearning agent, RewardFunction rf, TerminalFunction tf){

		List <State> states = StateReachability.getReachableStates(this.s, (SADomain)domain, hashingFactory);

		agent.setRf(rf);
		agent.setTf(tf);

		CopiedQs cQs = new CopiedQs(states, agent, hashingFactory);
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(states, cQs, new GreedyQPolicy(cQs));
		((StateValuePainter2D)gui.getSvp()).toggleValueStringRendering(false);
		gui.initGUI();

		int nsteps = 100;
		State lastState = this.s;
		while(nsteps > 0){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(lastState, nsteps);
			if(tf.isTerminal(ea.getState(ea.maxTimeStep()))){
				lastState = this.s;
			}
			nsteps -= ea.maxTimeStep();
		}

		cQs = new CopiedQs(states, agent, hashingFactory);
		gui = GridWorldDomain.getGridWorldValueFunctionVisualization(states, cQs, new GreedyQPolicy(cQs));
		((StateValuePainter2D)gui.getSvp()).toggleValueStringRendering(false);
		gui.initGUI();



		nsteps = 400;
		while(nsteps > 0){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(lastState, nsteps);
			if(tf.isTerminal(ea.getState(ea.maxTimeStep()))){
				lastState = this.s;
			}
			nsteps -= ea.maxTimeStep();
		}

		cQs = new CopiedQs(states, agent, hashingFactory);
		gui = GridWorldDomain.getGridWorldValueFunctionVisualization(states, cQs, new GreedyQPolicy(cQs));
		((StateValuePainter2D)gui.getSvp()).toggleValueStringRendering(false);
		gui.initGUI();

		nsteps = 400;
		while(nsteps > 0){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(lastState, nsteps);
			if(tf.isTerminal(ea.getState(ea.maxTimeStep()))){
				lastState = this.s;
			}
			nsteps -= ea.maxTimeStep();
		}

		cQs = new CopiedQs(states, agent, hashingFactory);
		gui = GridWorldDomain.getGridWorldValueFunctionVisualization(states, cQs, new GreedyQPolicy(cQs));
		((StateValuePainter2D)gui.getSvp()).toggleValueStringRendering(false);
		gui.initGUI();

	}


	public void runAStarMultiGoal(){

		State curState = this.s;

		VisualActionObserver ob = new VisualActionObserver(domain, v);
		ob.setFrameDelay((long)(1./10.*1000));
		ob.initGUI();
		v.updateState(s);


		try {
			//Thread.sleep((long)(1./24.*1000));
			Thread.sleep(25000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		PolicyTF ptf = generateAStarPolicy(curState, 0, 4);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 4, 4);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 4, 0);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 6, 3);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 6, 0);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 10, 0);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 10, 3);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 10, 5);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 10, 10);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 6, 10);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 6, 5);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 4, 10);
		curState = animatePolicy(curState, ptf.p, ptf.tf, ob);

		ptf = generateAStarPolicy(curState, 0, 10);
		animatePolicy(curState, ptf.p, ptf.tf, ob);






	}

	public PolicyTF generateAStarPolicy(State curState, int gx, int gy){
		TerminalFunction tf = new GridWorldTerminalFunction(gx, gy);
		AStar g1 = new AStar(domain, new UniformCostRF(), new TFGoalCondition(tf), new DiscreteStateHashFactory(), new NullHeuristic());
		g1.planFromState(curState);
		return new PolicyTF(new DDPlannerPolicy(g1), tf);
	}

	public State animatePolicy(State s, Policy p, TerminalFunction tf, VisualActionObserver ob){

		((SADomain)domain).addActionObserverForAllAction(ob);

		EpisodeAnalysis ea = p.evaluateBehavior(s, new UniformCostRF(), tf);

		((SADomain)domain).clearAllActionObserversForAllActions();

		return ea.getState(ea.maxTimeStep());

	}

	public static class PolicyTF{
		public Policy p;
		public TerminalFunction tf;

		public PolicyTF(Policy p, TerminalFunction tf) {
			this.p = p;
			this.tf = tf;
		}
	}


	public static class CopiedQs implements QComputablePlanner{

		protected StateHashFactory hashingFactory;
		protected Map<StateHashTuple, List<QValue>> qfunction;

		public CopiedQs(List<State> states, QComputablePlanner qSource, StateHashFactory hashingFactory){

			this.hashingFactory = hashingFactory;

			this.qfunction = new HashMap<StateHashTuple, List<QValue>>(states.size());

			for(State s : states){
				StateHashTuple sh = this.hashingFactory.hashState(s);
				List<QValue> qs = qSource.getQs(s);
				List<QValue> cqs = new ArrayList<QValue>(qs.size());
				for(QValue q : qs){
					cqs.add(new QValue(s, q.a, q.q));
				}
				this.qfunction.put(sh, cqs);
			}

		}

		@Override
		public List<QValue> getQs(State s) {
			return this.qfunction.get(this.hashingFactory.hashState(s));
		}

		@Override
		public QValue getQ(State s, AbstractGroundedAction a) {
			List<QValue> qs = this.getQs(s);
			for(QValue q : qs){
				if(q.a.equals(a)){
					return q;
				}
			}
			return null;
		}
	}


	public static void main(String[] args) {

		GWLearnNovel gw = new GWLearnNovel();


		QLearning agent = gw.getQLAgent();
		gw.runLearning(agent, new UniformCostRF(), new GridWorldTerminalFunction(10,10), 10000, 0);

		System.out.println("--------------");

		GridWorldDomain.setLocation(gw.s, 0, 0, 10);
		//gw.runLearning(agent, new UniformCostRF(), new GridWorldTerminalFunction(0,10), 10000, 2);
		gw.runQFunctionRenderLearning(agent, new UniformCostRF(), new GridWorldTerminalFunction(0, 10));


		//gw.runAStarMultiGoal();

	}




}
