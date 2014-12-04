package tests;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import datastructures.HashedAggregator;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class ActionGroupTest {

	public static void main(String [] args){
		GridWorldDomain gwd = new GridWorldDomain(11,11);
		gwd.setMapToFourRooms();
		Domain domain = gwd.generateDomain();
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new GridWorldTerminalFunction(10,10);

		ValueIteration vi = new ValueIteration(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0.001, 500);
		State si = GridWorldDomain.getOneAgentNoLocationState(domain);
		GridWorldDomain.setAgent(si, 0, 0);

		vi.planFromState(si);
		GreedyQPolicy p = new GreedyQPolicy(vi);

		List<State> allStates = StateReachability.getReachableStates(si, (SADomain)domain, new DiscreteStateHashFactory());
		List<GroundedAction> allActions = Action.getAllApplicableGroundedActionsFromActionList(domain.getActions(), si);


		HashedAggregator<String> counts = new HashedAggregator<String>(0);
		System.out.println("All state size: " + allStates.size());
		for(State s : allStates){
			List<Policy.ActionProb> aps = p.getActionDistributionForState(s);
			StringBuilder sb = new StringBuilder();
			for(GroundedAction ga : allActions){
				if(inList(ga, aps)){
					if(sb.length() > 0){
						sb.append(",");
					}
					sb.append(ga.toString());
				}
			}
			String key = sb.toString();
			counts.add(key, 1.);
		}

		int total = 0;
		for(String key : counts.keySet()){
			int c = (int)counts.v(key);
			total += c;
			System.out.println(key + ": " + c);
		}
		System.out.println("total: " + total);

	}

	public static boolean inList(GroundedAction ga, List<Policy.ActionProb> aps){
		for(Policy.ActionProb ap : aps){
			if(ap.ga.equals(ga)){
				if(ap.pSelection > 0.) {
					return true;
				}
				else{
					return false;
				}
			}
		}
		return false;
	}


}
