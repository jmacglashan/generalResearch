package tests.rmaxvtest;

import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.learning.modellearning.models.TabularModel;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;

import java.awt.*;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class TabModelVis extends TabularModel{

	protected List<State> allStates;
	protected int numUpdates = 0;
	protected Domain modeledDomain;

	public TabModelVis(Domain sourceDomain, StateHashFactory hashingFactory, int nConfident, List<State> allStates) {
		super(sourceDomain, hashingFactory, nConfident);
		this.allStates = allStates;
		ModeledDomainGenerator mdg = new ModeledDomainGenerator(sourceDomain, this, true);
		this.modeledDomain = mdg.generateDomain();
	}


	@Override
	public void updateModel(State s, GroundedAction ga, State sprime, double r, boolean sprimeIsTerminal) {
		super.updateModel(s, ga, sprime, r, sprimeIsTerminal);

		if(numUpdates % 20 == 0){


		}

		numUpdates++;

	}


	public void launchVisualizer(){
		ValueIteration planner = new ValueIteration(this.modeledDomain,modeledRF, modeledTF, 0.99, this.hashingFactory, 0.01, 100);
		planner.planFromState(this.allStates.get(0));

		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(allStates, planner, new GreedyQPolicy(planner));
		gui.initGUI();
	}
}
