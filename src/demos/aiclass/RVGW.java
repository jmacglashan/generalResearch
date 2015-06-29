package demos.aiclass;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.planning.QFunction;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.SADomain;

import java.awt.*;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class RVGW {

	public static void main(String [] args){

		GridWorldDomain gwd = new GridWorldDomain(4, 3);
		gwd.setTransitionDynamics(new double[][]{{.8, 0.,.1,.1}, {0.,.8,.1,.1},{.1,.1,.8,0.},{.1,.1,0.,.8}});
		gwd.makeEmptyMap();
		gwd.setObstacleInCell(1,1);

		Domain domain = gwd.generateDomain();
		GridWorldRewardFunction rf = new GridWorldRewardFunction(domain, -0.04);
		rf.setReward(3,2,1.);
		rf.setReward(3,1,-1.);

		GridWorldTerminalFunction tf = new GridWorldTerminalFunction(3,2);
		tf.markAsTerminalPosition(3,1);

		State s = GridWorldDomain.getOneAgentNoLocationState(domain, 0, 0);

		ValueIteration vi = new ValueIteration(domain, rf, tf, 1.0, new DiscreteStateHashFactory(), 0.00001, 100);
		vi.planFromState(s);
		Policy p = new GreedyQPolicy(vi);

		valueFunctionVisualize(vi, p, domain, s);

	}


	public static void valueFunctionVisualize(QFunction planner, Policy p, Domain domain, State initialState){
		List<State> allStates = StateReachability.getReachableStates(initialState,
				(SADomain) domain, new DiscreteStateHashFactory());


		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, planner, new GreedyQPolicy(planner));
		StateValuePainter2D svp = (StateValuePainter2D)gui.getSvp();
		svp.setValueStringRenderingFormat(48, Color.BLACK, 3, 0f, 0.75f);
		gui.initGUI();

	}

}
