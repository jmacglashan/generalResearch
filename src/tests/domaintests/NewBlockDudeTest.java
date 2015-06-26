package tests.domaintests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.domain.singleagent.blockdude.BlockDude;
import burlap.domain.singleagent.blockdude.BlockDudeLevelConstructor;
import burlap.domain.singleagent.blockdude.BlockDudeTF;
import burlap.domain.singleagent.blockdude.BlockDudeVisualizer;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author James MacGlashan.
 */
public class NewBlockDudeTest {

	public static void main(String[] args) {

		BlockDude bd = new BlockDude();
		Domain domain = bd.generateDomain();
		State s = BlockDudeLevelConstructor.getLevel3(domain);

		Visualizer v = BlockDudeVisualizer.getVisualizer(bd.getMaxx(), bd.getMaxy());


		/*
		VisualExplorer exp = new VisualExplorer(domain, v, s);

		exp.addKeyAction("w", ACTIONUP);
		exp.addKeyAction("d", ACTIONEAST);
		exp.addKeyAction("a", ACTIONWEST);
		exp.addKeyAction("s", ACTIONPICKUP);
		exp.addKeyAction("x", ACTIONPUTDOWN);

		exp.initGUI();
		*/

		TerminalFunction tf = new BlockDudeTF();
		RewardFunction rf = new UniformCostRF();
		StateConditionTest gc = new TFGoalCondition(tf);

		HashMap<String, List<Attribute>> atts = new HashMap<String, List<Attribute>>();
		atts.put(BlockDude.CLASSAGENT, domain.getObjectClass(BlockDude.CLASSAGENT).attributeList);
		atts.put(BlockDude.CLASSBLOCK, domain.getObjectClass(BlockDude.CLASSBLOCK).attributeList);
		AStar planner = new AStar(domain, rf, gc, new DiscreteMaskHashingFactory(atts), new NullHeuristic());

		planner.planFromState(s);
		Policy p = new SDPlannerPolicy(planner);

		EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 100);
		List <EpisodeAnalysis> eas = new ArrayList<EpisodeAnalysis>();
		eas.add(ea);

		new EpisodeSequenceVisualizer(v, domain, eas);


	}



}
