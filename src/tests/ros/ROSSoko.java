package tests.ros;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.modellearning.DomainMappedPolicy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.singleagent.environment.DomainEnvironmentWrapper;
import burlap.oomdp.singleagent.explorer.SpecialExplorerAction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import burlap.ros.AsynchronousRosEnvironment;
import domain.singleagent.sokoban2.SokoTurtleBot;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Visualizer;
import sun.management.resources.agent;

import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class ROSSoko {

	public static void main(String[] args) {

		//define ros soko
		SokoTurtleBot sokoTurtle = new SokoTurtleBot();
		Domain domain = sokoTurtle.generateDomain();

		//setup ROS information
		String uri = "ws://chelone:9090";
		String stateTopic = "/burlap_state";
		String actionTopic = "/burlap_action";

		//create environment with 2000ms (2s) action execution time
		//final AsynchronousRosEnvironment env = new SokoEnv(domain, uri, stateTopic, actionTopic, 4000, 66, 1);
		final AsynchronousRosEnvironment env = new SokoEnv(domain, uri, stateTopic, actionTopic, 4000, 16, 1);
		env.blockUntilStateReceived();

		//optionally, uncomment the below so that you can see the received state printed to the terminal
		//env.setPrintStateAsReceived(true);

		//create a domain wrapper of the environment so that wrapped domain's actions go to
		//to the environment, rather than the normal GridWorld action simulator code.
		DomainEnvironmentWrapper envDomainWrapper = new DomainEnvironmentWrapper(domain, env);
		final Domain envDomain = envDomainWrapper.generateDomain();

		Visualizer v = Sokoban2Visualizer.getVisualizer("oomdpResearch/robotImages");


		/*v.updateState(env.getCurState());
		VisualActionObserver ob = new VisualActionObserver(envDomain, v);
		ob.initGUI();

		((SADomain)envDomain).addActionObserverForAllAction(ob);
		*/

		/*
		((SADomain)envDomain).addActionObserverForAllAction(new ActionObserver() {
			@Override
			public void actionEvent(State s, GroundedAction ga, State sp) {
				System.out.println(s.getCompleteStateDescriptionWithUnsetAttributesAsNull());
				System.out.println(ga.toString());
				System.out.println("--------------------");
			}
		});
		*/



		//Visualizer v = Sokoban2Visualizer.getVisualizer("oomdpResearch/robotImages");
		VisualExplorer exp = new VisualExplorer(envDomain, v, env.getCurState());
		exp.addKeyAction("w", SokoTurtleBot.ACTIONFORWARD);
		exp.addKeyAction("d", SokoTurtleBot.ACTIONROTATE);

		exp.addSpecialAction("u", new SpecialExplorerAction() {
			@Override
			public State applySpecialAction(State curState) {
				System.out.println("Getting new state...");
				return env.getCurState();
			}
		});

		exp.initGUI();



		/*
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new LocTF(7, 7);

		//BFS planner = new BFS(domain, new TFGoalCondition(tf), new DiscreteStateHashFactory());
		AStar planner = new AStar(domain, new UniformCostRF(), new TFGoalCondition(tf), new DiscreteStateHashFactory(), new NullHeuristic());
		planner.planFromState(env.getCurState());
		DDPlannerPolicy policy = new DDPlannerPolicy(planner);


		System.out.println("initial plan:");
		System.out.println(policy.evaluateBehavior(env.getCurState(),rf, tf, 100).getActionSequenceString("\n"));

		System.out.println("-----\nBeginning actual control");
		DomainMappedPolicy envPolicy = new DomainMappedPolicy(envDomain, policy);

		envPolicy.evaluateBehavior(env.getCurState(), rf, tf, 100);

		System.out.println("Finished control");
		*/


	}



	public static class LocRF implements RewardFunction{

		int gx;
		int gy;

		public LocRF(int x, int y){
			this.gx = x;
			this.gy = y;
		}

		@Override
		public double reward(State s, GroundedAction a, State sprime) {

			ObjectInstance agent = sprime.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int x = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int y = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);

			if(gx == x && gy == y){
				return 1.;
			}

			return 0;
		}
	}

	public static class LocTF implements TerminalFunction{


		int gx;
		int gy;

		public LocTF(int x, int y){
			this.gx = x;
			this.gy = y;
		}

		@Override
		public boolean isTerminal(State s) {

			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int x = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int y = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);

			if(gx == x && gy == y){
				return true;
			}

			return false;
		}
	}


	public static class SokoEnv extends AsynchronousRosEnvironment{


		protected int curSeq;

		public SokoEnv(Domain domain, String rosBridgeURI, String rosStateTopic, String rosActionTopic, int actionSleepMS) {
			super(domain, rosBridgeURI, rosStateTopic, rosActionTopic, actionSleepMS);
		}

		public SokoEnv(Domain domain, String rosBridgeURI, String rosStateTopic, String rosActionTopic, int actionSleepMS, int rosBridgeThrottleRate, int rosBridgeQueueLength) {
			super(domain, rosBridgeURI, rosStateTopic, rosActionTopic, actionSleepMS, rosBridgeThrottleRate, rosBridgeQueueLength);
		}

		@Override
		public void receive(Map<String, Object> data, String stringRep) {

			//System.out.println("In receive...");

			Map<String, Object> msg = (Map<String,Object>)data.get("msg");
			Map<String, Object> header = (Map<String, Object>)msg.get("header");
			this.curSeq = (Integer)header.get("seq");
			//System.out.println(seq);
			super.receive(data, stringRep);
		}



		@Override
		protected State onStateReceive(State s) {

			ObjectInstance room = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSROOM), "macroRoom");
			room.setValue(Sokoban2Domain.ATTTOP, 11);
			room.setValue(Sokoban2Domain.ATTLEFT, 0);
			room.setValue(Sokoban2Domain.ATTBOTTOM, 0);
			room.setValue(Sokoban2Domain.ATTRIGHT, 11);
			room.setValue(Sokoban2Domain.ATTCOLOR, Sokoban2Domain.COLORS[0]);

			s.addObject(room);

			System.out.println("Received: " + this.curSeq + this.agentString(s));

			return s;

		}

		@Override
		public State executeAction(String aname, String[] params) {

			System.out.println("Enter action " + this.agentString(this.curState) + ": " + aname);

			State end = super.executeAction(aname, params);

			System.out.println("return state: " + this.agentString(end));
			System.out.println("--");

			return end;
		}


		protected String agentString(State s){
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);
			String dir = agent.getStringValForAttribute(Sokoban2Domain.ATTDIR).substring(0, 1);
			return "(" + ax + "," + ay + "," + dir + ")";
		}
	}

}
