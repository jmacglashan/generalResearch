package behavior.AMDP.premade.sokoban;

import behavior.AMDP.blacklistAffordance.AffordanceBlackList;
import behavior.AMDP.blacklistAffordance.AffordanceBlackListWrapper;
import behavior.training.experiments.interactive.soko.SokoAStarPlanner;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.MyTimer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Visualizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoAMDPExp {

	Domain domain;
	TerminalFunction goalTF;
	State initialState;

	public SokoAMDPExp(){

		Sokoban2Domain soko = new Sokoban2Domain();
		soko.includePullAction(true);
		soko.inlcludeTouchingBlockPF(true);
		this.domain = soko.generateDomain();

	}

	public void setSimpleTask(){

		this.initialState = Sokoban2Domain.getClassicState(domain);

		List<GroundedProp> goalgp = new ArrayList<GroundedProp>(1);
		goalgp.add(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"block0", "room1"}));
		this.goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(goalgp);

	}


	public void setHardTask(){

		this.initialState = Sokoban2Domain.getCleanState(this.domain, 3, 2, 4);

		Sokoban2Domain.setRoom(this.initialState, 0, 18, 0, 0, 7, "red");
		Sokoban2Domain.setRoom(this.initialState, 1, 18, 7, 0, 13, "blue");
		Sokoban2Domain.setRoom(this.initialState, 2, 18, 13, 0, 19, "yellow");

		Sokoban2Domain.setDoor(this.initialState, 0, 17, 7, 17, 7);
		//Sokoban2Domain.setDoor(this.initialState, 1, 1, 13, 1, 13);
		Sokoban2Domain.setDoor(this.initialState, 1, 14, 13, 14, 13);

		Sokoban2Domain.setBlock(this.initialState, 0, 5, 2, "chair", "green");
		Sokoban2Domain.setBlock(this.initialState, 1, 5, 4, "chair", "red");
		Sokoban2Domain.setBlock(this.initialState, 2, 5, 6, "chair", "yellow");
		Sokoban2Domain.setBlock(this.initialState, 3, 5, 8, "chair", "blue");


		Sokoban2Domain.setAgent(this.initialState, 3, 6);




		List<GroundedProp> goalgp = new ArrayList<GroundedProp>(1);
		goalgp.add(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"block3", "room2"}));
		this.goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(goalgp);


	}

	public void setHardTask2Blocks(){

		this.initialState = Sokoban2Domain.getCleanState(this.domain, 3, 2, 4);

		Sokoban2Domain.setRoom(this.initialState, 0, 18, 0, 0, 7, "red");
		Sokoban2Domain.setRoom(this.initialState, 1, 18, 7, 0, 13, "blue");
		Sokoban2Domain.setRoom(this.initialState, 2, 18, 13, 0, 19, "yellow");

		Sokoban2Domain.setDoor(this.initialState, 0, 17, 7, 17, 7);
		//Sokoban2Domain.setDoor(this.initialState, 1, 1, 13, 1, 13);
		Sokoban2Domain.setDoor(this.initialState, 1, 14, 13, 14, 13);

		Sokoban2Domain.setBlock(this.initialState, 0, 5, 2, "chair", "green");
		Sokoban2Domain.setBlock(this.initialState, 1, 5, 4, "chair", "red");
		Sokoban2Domain.setBlock(this.initialState, 2, 5, 6, "chair", "yellow");
		Sokoban2Domain.setBlock(this.initialState, 3, 5, 8, "chair", "blue");


		Sokoban2Domain.setAgent(this.initialState, 3, 6);




		List<GroundedProp> goalgp = new ArrayList<GroundedProp>(1);
		goalgp.add(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"block3", "room2"}));
		goalgp.add(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"block2", "room1"}));
		this.goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(goalgp);


	}
	
	public void viewTask(){
		Visualizer v = Sokoban2Visualizer.getVisualizer(20, 20, "oomdpResearch/robotImages");
		VisualExplorer exp = new VisualExplorer(domain, v, this.initialState);

		exp.addKeyAction("w", Sokoban2Domain.ACTIONNORTH);
		exp.addKeyAction("s", Sokoban2Domain.ACTIONSOUTH);
		exp.addKeyAction("d", Sokoban2Domain.ACTIONEAST);
		exp.addKeyAction("a", Sokoban2Domain.ACTIONWEST);
		exp.addKeyAction("r", Sokoban2Domain.ACTIONPULL);

		exp.initGUI();
	}


	public void testPlanners(){
		this.testAStar();
		this.testAMDP1();
	}

	public void testAStar(){

		SokoAStarPlanner astar = new SokoAStarPlanner();

		MyTimer timer = new MyTimer();
		timer.start();
		Policy p = astar.getPolicy(this.domain, this.initialState, new UniformCostRF(), this.goalTF, new DiscreteStateHashFactory());
		timer.stop();

		EpisodeAnalysis ea = p.evaluateBehavior(this.initialState, new UniformCostRF(), this.goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));

		System.out.println("A* plan time: " + timer.getTime());

	}

	public void testAMDP1(){

		SokoAMDP1 asoko = new SokoAMDP1();
		Domain adomain = asoko.generateDomain();

		Soko1To0Planner planner = new Soko1To0Planner(domain, adomain, goalTF, new DiscreteStateHashFactory());
		MyTimer timer = new MyTimer();
		timer.start();
		planner.planFromState(this.initialState);
		timer.stop();
		Policy p = new DDPlannerPolicy(planner);

		EpisodeAnalysis ea = p.evaluateBehavior(this.initialState, new UniformCostRF(), goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));

		System.out.println("AMDP1 plan time: " + timer.getTime());

	}

	public void testAMDP1WithAffordances(){

		SokoAMDP1 asoko = new SokoAMDP1();
		Domain adomain = asoko.generateDomain();

		AffordanceBlackListWrapper wrap = new AffordanceBlackListWrapper(adomain);
		Domain aadomain = wrap.generateDomain();

		SokoParamAffordance atdAff = new SokoAMDP1.AgentMoveBlackList(SokoAMDP1.ACTIONTODOOR);
		SokoParamAffordance atrAff = new SokoAMDP1.AgentMoveBlackList(SokoAMDP1.ACTIONTOROOM);

		SokoParamAffordance btdAff = new SokoAMDP1.BlockMoveBlackList(SokoAMDP1.ACTIONBLOCKTODOOR);
		SokoParamAffordance btrAff = new SokoAMDP1.BlockMoveBlackList(SokoAMDP1.ACTIONBLOCKTOROOM);

		SokoParamAffordance atbAff = new SokoAMDP1.ToBlockBlackList(SokoAMDP1.ACTIONTOBLOCK);

		AffordanceBlackListWrapper.setBlackList(aadomain, atdAff);
		AffordanceBlackListWrapper.setBlackList(aadomain, atrAff);
		AffordanceBlackListWrapper.setBlackList(aadomain, btdAff);
		AffordanceBlackListWrapper.setBlackList(aadomain, btrAff);
		AffordanceBlackListWrapper.setBlackList(aadomain, atbAff);

		AffordanceBlackListWrapper.setGoal(aadomain, new TFGoalCondition(this.goalTF));

		Soko1To0Planner planner = new Soko1To0Planner(domain, aadomain, goalTF, new DiscreteStateHashFactory());
		MyTimer timer = new MyTimer();
		timer.start();
		planner.planFromState(this.initialState);
		timer.stop();
		Policy p = new DDPlannerPolicy(planner);

		EpisodeAnalysis ea = p.evaluateBehavior(this.initialState, new UniformCostRF(), goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));

		System.out.println("AMDP1 plan time: " + timer.getTime());

	}



	public void testAMDP2(){

		SokoAMDP1 a1soko = new SokoAMDP1();
		Domain a1domain = a1soko.generateDomain();

		SokoAMDP2 a2soko = new SokoAMDP2();
		Domain a2domain = a2soko.generateDomain();

		Soko2To0Planner planner = new Soko2To0Planner(this.domain, a1domain, a2domain, this.goalTF, new DiscreteStateHashFactory());
		planner.setA1Affordances(false);
		MyTimer timer = new MyTimer();
		timer.start();
		planner.planFromState(this.initialState);
		timer.stop();
		Policy p = new DDPlannerPolicy(planner);

		EpisodeAnalysis ea = p.evaluateBehavior(this.initialState, new UniformCostRF(), goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));

		System.out.println("AMDP2 plan time: " + timer.getTime());

	}


	public void testAMDP2WithA1Affordances(){

		SokoAMDP1 a1soko = new SokoAMDP1();
		Domain a1domain = a1soko.generateDomain();

		AffordanceBlackListWrapper wrap = new AffordanceBlackListWrapper(a1domain);
		Domain aa1domain = wrap.generateDomain();

		SokoParamAffordance atdAff = new SokoAMDP1.AgentMoveBlackList(SokoAMDP1.ACTIONTODOOR);
		SokoParamAffordance atrAff = new SokoAMDP1.AgentMoveBlackList(SokoAMDP1.ACTIONTOROOM);

		SokoParamAffordance btdAff = new SokoAMDP1.BlockMoveBlackList(SokoAMDP1.ACTIONBLOCKTODOOR);
		SokoParamAffordance btrAff = new SokoAMDP1.BlockMoveBlackList(SokoAMDP1.ACTIONBLOCKTOROOM);

		SokoParamAffordance atbAff = new SokoAMDP1.ToBlockBlackList(SokoAMDP1.ACTIONTOBLOCK);

		AffordanceBlackListWrapper.setBlackList(aa1domain, atdAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, atrAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, btdAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, btrAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, atbAff);

		AffordanceBlackListWrapper.setGoal(aa1domain, new TFGoalCondition(this.goalTF));

		SokoAMDP2 a2soko = new SokoAMDP2();
		Domain a2domain = a2soko.generateDomain();

		Soko2To0Planner planner = new Soko2To0Planner(this.domain, aa1domain, a2domain, this.goalTF, new DiscreteStateHashFactory());
		MyTimer timer = new MyTimer();
		timer.start();
		planner.planFromState(this.initialState);
		timer.stop();
		Policy p = new DDPlannerPolicy(planner);

		EpisodeAnalysis ea = p.evaluateBehavior(this.initialState, new UniformCostRF(), goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));

		System.out.println("AMDP2 plan time: " + timer.getTime());

	}



	public void testAMDP2WithAffordances(){

		SokoAMDP1 a1soko = new SokoAMDP1();
		Domain a1domain = a1soko.generateDomain();

		AffordanceBlackListWrapper wrap = new AffordanceBlackListWrapper(a1domain);
		Domain aa1domain = wrap.generateDomain();

		SokoParamAffordance atdAff = new SokoAMDP1.AgentMoveBlackList(SokoAMDP1.ACTIONTODOOR);
		SokoParamAffordance atrAff = new SokoAMDP1.AgentMoveBlackList(SokoAMDP1.ACTIONTOROOM);

		SokoParamAffordance btdAff = new SokoAMDP1.BlockMoveBlackList(SokoAMDP1.ACTIONBLOCKTODOOR);
		SokoParamAffordance btrAff = new SokoAMDP1.BlockMoveBlackList(SokoAMDP1.ACTIONBLOCKTOROOM);

		SokoParamAffordance atbAff = new SokoAMDP1.ToBlockBlackList(SokoAMDP1.ACTIONTOBLOCK);

		AffordanceBlackListWrapper.setBlackList(aa1domain, atdAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, atrAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, btdAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, btrAff);
		AffordanceBlackListWrapper.setBlackList(aa1domain, atbAff);

		AffordanceBlackListWrapper.setGoal(aa1domain, new TFGoalCondition(this.goalTF));

		SokoAMDP2 a2soko = new SokoAMDP2();
		Domain a2domain = a2soko.generateDomain();

		AffordanceBlackListWrapper wrap2 = new AffordanceBlackListWrapper(a2domain);
		Domain aa2domain = wrap2.generateDomain();

		SokoAMDP2.AgentAffordance a2AgentAffordance = new SokoAMDP2.AgentAffordance(SokoAMDP1.ACTIONTOROOM);
		SokoAMDP2.BlockAffordance a2BlockAffordance = new SokoAMDP2.BlockAffordance(SokoAMDP1.ACTIONBLOCKTOROOM);

		AffordanceBlackListWrapper.setBlackList(aa2domain, a2AgentAffordance);
		AffordanceBlackListWrapper.setBlackList(aa2domain, a2BlockAffordance);

		AffordanceBlackListWrapper.setGoal(aa2domain, new TFGoalCondition(this.goalTF));

		Soko2To0Planner planner = new Soko2To0Planner(this.domain, aa1domain, aa2domain, this.goalTF, new DiscreteStateHashFactory());
		MyTimer timer = new MyTimer();
		timer.start();
		planner.planFromState(this.initialState);
		timer.stop();
		Policy p = new DDPlannerPolicy(planner);

		EpisodeAnalysis ea = p.evaluateBehavior(this.initialState, new UniformCostRF(), goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));

		System.out.println("AMDP2 plan time: " + timer.getTime());

		StateParser sp = new StateYAMLParser(domain);
		ea.writeToFile("tstAMDP/plan", sp);

		Visualizer v = Sokoban2Visualizer.getVisualizer(20, 20, "oomdpResearch/robotImages");
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, "tstAMDP");
		evis.initGUI();

	}


	public static void main(String [] args){

		SokoAMDPExp exp = new SokoAMDPExp();

		//exp.setSimpleTask();
		//exp.testPlanners();

		//exp.setHardTask();
		exp.setHardTask2Blocks();
		exp.viewTask();

		//exp.testAMDP1();
		//exp.testAMDP1WithAffordances();
		//exp.testAMDP2();
		//exp.testAMDP2WithAffordances();
		//exp.testAStar();



	}

}
