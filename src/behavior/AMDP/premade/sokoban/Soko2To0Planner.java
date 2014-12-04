package behavior.AMDP.premade.sokoban;

import behavior.AMDP.blacklistAffordance.AffordanceBlackListWrapper;
import behavior.training.experiments.interactive.soko.SokoAStarPlanner;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class Soko2To0Planner extends DeterministicPlanner{

	protected Domain a1domain;
	protected Domain a2domain;

	protected SokoAStarPlanner astarPlanner;

	protected String agentName;

	protected boolean useA1Affordances = true;



	public Soko2To0Planner(Domain domain, Domain a1domain, Domain a2domain, TerminalFunction tf, StateHashFactory hashingFactory){

		if(!(tf instanceof TrajectoryModule.ConjunctiveGroundedPropTF)){
			throw new RuntimeException("Error; terminal function is not correct type.");
		}


		this.a1domain = a1domain;
		this.a2domain = a2domain;
		this.deterministicPlannerInit(domain, new UniformCostRF(), tf, new TFGoalCondition(tf), hashingFactory);

		this.astarPlanner = new SokoAStarPlanner();

	}

	public void setA1Affordances(boolean useA1Affordances){
		this.useA1Affordances = useA1Affordances;
	}


	@Override
	public void planFromState(State initialState) {

		TrajectoryModule.ConjunctiveGroundedPropTF ctf = (TrajectoryModule.ConjunctiveGroundedPropTF)tf;

		this.agentName = initialState.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT).getName();

		SokoAMDP1.BlockInRegionGC goal = new SokoAMDP1.BlockInRegionGC(ctf);

		State a2i = SokoAMDP2.projectFromA0(initialState, this.a2domain);

		System.out.println("Starting A2 planning");
		BFS bfs = new BFS(a2domain, goal, new NameDependentStateHashFactory());
		bfs.planFromState(a2i);
		System.out.println("Finished A2 planning.");

		SDPlannerPolicy policy = new SDPlannerPolicy(bfs);
		EpisodeAnalysis ea = policy.evaluateBehavior(a2i, new UniformCostRF(), new GoalConditionTF(goal));

		State curState = initialState;
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			GroundedAction aga = ea.getAction(i);
			curState = this.decomposeA2(curState, aga);
		}


	}

	protected State decomposeA2(State initialState, GroundedAction aga){

		StateConditionTest projectedGC = null;

		if(aga.actionName().equals(SokoAMDP1.ACTIONBLOCKTOROOM)){
			projectedGC = new SokoAMDP1.BlockInRegionGC(aga.params[0], aga.params[1]);
		}
		else if(aga.actionName().equals(SokoAMDP1.ACTIONTOROOM)){
			projectedGC = new SokoAMDP1.InRegionGC(aga.params[0]);
		}


		if(this.useA1Affordances){
			TrajectoryModule.ConjunctiveGroundedPropTF tf = this.A1LowLevelGoal(aga);
			StateConditionTest sg = new TFGoalCondition(tf);
			AffordanceBlackListWrapper.setGoal(a1domain, sg);
		}

		System.out.println("Starting A1 plan for: " + aga.toString());
		State ais = SokoAMDP1.projectToAMDPState(initialState, a1domain);
		BFS bfs = new BFS(this.a1domain, projectedGC, new NameDependentStateHashFactory());
		//DPrint.toggleCode(bfs.getDebugCode(), false);
		bfs.planFromState(ais);
		System.out.println("Finished A1 plan.");
		SDPlannerPolicy policy = new SDPlannerPolicy(bfs);
		EpisodeAnalysis ea = policy.evaluateBehavior(ais, new UniformCostRF(), new GoalConditionTF(projectedGC));


		State curState = initialState;
		for(int i = 0; i < ea.numTimeSteps()-1; i++){

			GroundedAction a1ga = ea.getAction(i);
			curState = this.decomposeA1(curState, a1ga);

		}

		return curState;
	}

	protected TrajectoryModule.ConjunctiveGroundedPropTF A1LowLevelGoal(GroundedAction ga){

		List<GroundedProp> gps = new ArrayList<GroundedProp>(1);
		if(ga.actionName().equals(SokoAMDP1.ACTIONBLOCKTOROOM)){
			gps.add(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), ga.params.clone()));
		}
		else if(ga.actionName().equals(SokoAMDP1.ACTIONTOROOM)){
			gps.add(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{this.agentName, ga.params[0]}));
		}

		TrajectoryModule.ConjunctiveGroundedPropTF tf = new TrajectoryModule.ConjunctiveGroundedPropTF(gps);


		return tf;
	}

	protected State decomposeA1(State s, GroundedAction ga){

		System.out.println("Decomposing A1: " + ga.toString());

		TrajectoryModule.ConjunctiveGroundedPropTF gc = this.getA0GoalCondition(ga);

		State curState = s;
		Policy p = this.astarPlanner.getPolicy(this.domain, curState, this.rf, gc, this.hashingFactory);
		EpisodeAnalysis lowDecomp = p.evaluateBehavior(curState, this.rf, gc);
		for(int t2 = 0; t2 < lowDecomp.numTimeSteps()-1; t2++){
			State ls = lowDecomp.getState(t2);
			StateHashTuple lsh = this.hashingFactory.hashState(ls);
			this.internalPolicy.put(lsh, lowDecomp.getAction(t2));
			this.mapToStateIndex.put(lsh,lsh);
		}
		curState = lowDecomp.getState(lowDecomp.numTimeSteps()-1);


		return curState;

	}


	protected TrajectoryModule.ConjunctiveGroundedPropTF getA0GoalCondition(GroundedAction ga){

		GroundedProp gp = null;

		if(ga.actionName().equals(SokoAMDP1.ACTIONTODOOR)){
			gp = new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINDOOR), new String[]{this.agentName, ga.params[0]});
		}
		else if(ga.actionName().equals(SokoAMDP1.ACTIONTOROOM)){
			gp = new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{this.agentName, ga.params[0]});
		}
		else if(ga.actionName().equals(SokoAMDP1.ACTIONTOBLOCK)){
			gp = new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFTOUCHINGBLOCK), new String[]{this.agentName, ga.params[0]});
		}
		else if(ga.actionName().equals(SokoAMDP1.ACTIONBLOCKTODOOR)){
			gp = new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFBLOCKINDOOR), new String[]{ga.params[0], ga.params[1]});
		}
		else if(ga.actionName().equals(SokoAMDP1.ACTIONBLOCKTOROOM)){
			gp = new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{ga.params[0], ga.params[1]});
		}
		else{
			throw new RuntimeException("Unkown action to decompose: " + ga.actionName());
		}


		List<GroundedProp> gps = new ArrayList<GroundedProp>();
		gps.add(gp);

		TrajectoryModule.ConjunctiveGroundedPropTF gc = new TrajectoryModule.ConjunctiveGroundedPropTF(gps);

		return gc;
	}

}
