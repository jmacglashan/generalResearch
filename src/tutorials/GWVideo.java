package tutorials;


import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

public class GWVideo {

	public static void main(String[] args) {

		GridWorldDomain gwd = new GridWorldDomain(11,11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10,10);
		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		QLearning ql = new QLearning(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0., 1.);
		ql.setLearningPolicy(new GreedyQPolicy(ql));

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		VisualActionObserver ob = new VisualActionObserver(domain, v);
		((SADomain)domain).addActionObserverForAllAction(ob);
		ob.initGUI();

		while(true){
			ql.runLearningEpisodeFrom(s);
		}

	}

}
