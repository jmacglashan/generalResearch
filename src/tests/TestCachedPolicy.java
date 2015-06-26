package tests;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.planning.commonpolicies.CachedPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class TestCachedPolicy {

	public static void main(String[] args) {

		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10, 10);
		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);
		StateHashFactory hashingFactory = new DiscreteStateHashFactory();

		List<State> allStates = StateReachability.getReachableStates(s, (SADomain)domain, hashingFactory);

		ValueIteration vi = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.01, 100);
		vi.planFromState(s);

		Policy gp = new GreedyQPolicy(vi);
		CachedPolicy cp = new CachedPolicy(hashingFactory, gp);
		for(State ss : allStates){
			cp.getAction(ss);
		}

		MyTimer timer = new MyTimer();
		timer.start();
		for(int i = 0; i < 100000; i++){
			cp.evaluateBehavior(s, rf, tf);
		}
		timer.stop();
		System.out.println("cached time: " + timer.getTime());

		timer.start();
		for(int i = 0; i < 100000; i++){
			gp.evaluateBehavior(s, rf, tf);
		}
		timer.stop();
		System.out.println("greedy time: " + timer.getTime());

		System.out.println("cached episode:\n" + cp.evaluateBehavior(s, rf, tf).getActionSequenceString(" "));


	}

}
