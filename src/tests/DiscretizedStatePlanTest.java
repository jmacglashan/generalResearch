package tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import behavior.statehashing.DiscretizingStateHashFactory;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.baxter.pickupplace.PickupAndPlaceDomain;
import domain.singleagent.baxter.pickupplace.PickupAndPlaceVisualizer;

public class DiscretizedStatePlanTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		 * Create domain
		 */
		PickupAndPlaceDomain dgen = new PickupAndPlaceDomain(true);
		SADomain domain = (SADomain)dgen.generateDomain();
		
		
		/*
		 * Create an initial state from which we will start planning
		 */
		State s = PickupAndPlaceDomain.getCleanState(domain, 3, 9);
		PickupAndPlaceDomain.tileRegions(s, 3, 3, 0, 100., 0, 100, 20);
		PickupAndPlaceDomain.setObject(s, 0, 20, 20, 20, "red");
		PickupAndPlaceDomain.setObject(s, 1, 80, 20, 20, "green");
		PickupAndPlaceDomain.setObject(s, 2, 20, 20, 80, "blue");
		
		
		/*
		 * Setup a goal condition that requires object 0 in region 4; object 1 in region 8 and object 2 in region 1.
		 */
		InRegionGoal gc = new InRegionGoal();
		gc.addGP(new GroundedProp(domain.getPropFunction(PickupAndPlaceDomain.PFINREGION), new String[]{"object0", "region4"}));
		gc.addGP(new GroundedProp(domain.getPropFunction(PickupAndPlaceDomain.PFINREGION), new String[]{"object1", "region8"}));
		gc.addGP(new GroundedProp(domain.getPropFunction(PickupAndPlaceDomain.PFINREGION), new String[]{"object2", "region1"}));
		
		//Create corresonding terminal function that ends in the goal condition and a uniform cost rf
		TerminalFunction tf = new GoalConditionTF(gc);
		RewardFunction rf = new UniformCostRF();
		
		
		/*
		 * Use a discretizing state hashing factory so we can do conventional discrete planning (like A*)
		 */
		DiscretizingStateHashFactory hashingFactory = new DiscretizingStateHashFactory(30.);
		
		/*
		 * Create our A* planner
		 */
		AStar planner = new AStar(domain, rf, gc, hashingFactory, new NullHeuristic());

		/*
		 * Run planning
		 */
		planner.planFromState(s);
		
		/*
		 * Extract a dynamic policy from it (will replan if quried for an unkown state along the A* plan)
		 */
		Policy p = new DDPlannerPolicy(planner);
		
		
		/*
		 * To get an action, we just query the policy. In the below, we get the action for the initial state and print it out
		 */
		AbstractGroundedAction a = p.getAction(s);
		System.out.println(a.toString());
		
		
		

	}
	
	
	
	public static class InRegionGoal implements StateConditionTest{

		List<GroundedProp> props = new ArrayList<GroundedProp>();
		
		public void addGP(GroundedProp gp){
			this.props.add(gp);
		}
		
		@Override
		public boolean satisfies(State s) {
			
			for(GroundedProp gp : this.props){
				if(!gp.isTrue(s)){
					return false;
				}
			}
			
			return true;
		}
		
		
		
	}

}
