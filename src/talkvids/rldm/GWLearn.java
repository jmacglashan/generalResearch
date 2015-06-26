package talkvids.rldm;

import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
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

/**
 * @author James MacGlashan.
 */
public class GWLearn {

	public static void main(String[] args) {


		originalTest();


	}




	public static void originalTest(){

		GridWorldDomain gwd = new GridWorldDomain(11,11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10,10);
		//State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);
		State s = GridWorldDomain.getOneAgentNLocationState(domain, 1);
		GridWorldDomain.setAgent(s, 0, 0);
		GridWorldDomain.setLocation(s, 0, 10, 10);

		//QLearning ql = new QLearning(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0., 1.);
		//ql.setLearningPolicy(new GreedyQPolicy(ql));
		PotentialShapedRMax rmax = new PotentialShapedRMax(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0, 1, 0.01, 20);

		Visualizer v = GridWorldVisualizer.getVisualizer(gwd.getMap());
		VisualActionObserver ob = new VisualActionObserver(domain, v);
		ob.setFrameDelay((long)(1./24.*1000));
		((SADomain)domain).addActionObserverForAllAction(ob);
		ob.initGUI();
		v.updateState(s);

		try {
			Thread.sleep(25000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		while(true){
			System.out.println(rmax.runLearningEpisodeFrom(s).numTimeSteps());
			v.updateState(s);
			try {
				Thread.sleep((long)(1./24.*1000));
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
