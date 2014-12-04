package behavior.AMDP.premade.sokoban;

import behavior.training.experiments.interactive.soko.SokoAStarPlanner;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.*;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.DPrint;
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
public class Soko1To0Planner extends DeterministicPlanner{


	protected Domain adomain;
	protected SokoAStarPlanner astarPlanner;

	protected String agentName;

	public Soko1To0Planner(Domain domain, Domain adomain, TerminalFunction tf, StateHashFactory hashingFactory){

		if(!(tf instanceof TrajectoryModule.ConjunctiveGroundedPropTF)){
			throw new RuntimeException("Error; terminal function is not correct type.");
		}


		this.adomain = adomain;
		this.deterministicPlannerInit(domain, new UniformCostRF(), tf, new TFGoalCondition(tf), hashingFactory);

		this.astarPlanner = new SokoAStarPlanner();

	}

	@Override
	public void planFromState(State initialState) {

		TrajectoryModule.ConjunctiveGroundedPropTF ctf = (TrajectoryModule.ConjunctiveGroundedPropTF)tf;
		GroundedProp gp = ctf.gps.get(0);

		this.agentName = initialState.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT).getName();

		StateConditionTest projectedGC = null;

		if(gp.pf.getName().equals(Sokoban2Domain.PFAGENTINROOM)){
			projectedGC = new SokoAMDP1.InRegionGC(gp.params[1]);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFBLOCKINROOM)){
			//projectedGC = new SokoAMDP1.BlockInRegionGC(gp.params[0], gp.params[1]);
			projectedGC = new SokoAMDP1.BlockInRegionGC(ctf);
		}


		System.out.println("Starting A1 plan.");
		State ais = SokoAMDP1.projectToAMDPState(initialState, adomain);
		BFS bfs = new BFS(this.adomain, projectedGC, new NameDependentStateHashFactory());
		//DPrint.toggleCode(bfs.getDebugCode(), false);
		bfs.planFromState(ais);
		System.out.println("Finished A1 plan.");
		SDPlannerPolicy policy = new SDPlannerPolicy(bfs);
		EpisodeAnalysis ea = policy.evaluateBehavior(ais, new UniformCostRF(), new GoalConditionTF(projectedGC));

		State curState = initialState;
		for(int i = 0; i < ea.numTimeSteps()-1; i++){

			GroundedAction aga = ea.getAction(i);
			curState = this.decompose(curState, aga);

		}


	}

	@Override
	public void resetPlannerResults() {
		super.resetPlannerResults();
	}


	protected State decompose(State s, GroundedAction ga){

		System.out.println("Decomposing: " + ga.toString());

		TrajectoryModule.ConjunctiveGroundedPropTF gc = this.getGoalCondition(ga);

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

	protected TrajectoryModule.ConjunctiveGroundedPropTF getGoalCondition(GroundedAction ga){

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


	public static void main(String [] args){

		Sokoban2Domain soko = new Sokoban2Domain();
		soko.includePullAction(true);
		soko.inlcludeTouchingBlockPF(true);

		SokoAMDP1 asoko = new SokoAMDP1();

		Domain domain = soko.generateDomain();
		Domain adomain = asoko.generateDomain();

		State s = Sokoban2Domain.getClassicState(domain);

		List<GroundedProp> goalgp = new ArrayList<GroundedProp>(1);
		goalgp.add(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"block0", "room1"}));
		TerminalFunction goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(goalgp);

		Soko1To0Planner planner = new Soko1To0Planner(domain, adomain, goalTF, new DiscreteStateHashFactory());
		planner.planFromState(s);
		Policy p = new DDPlannerPolicy(planner);
		
		EpisodeAnalysis ea = p.evaluateBehavior(s, new UniformCostRF(), goalTF, 100);
		System.out.println(ea.getActionSequenceString("\n"));


	}


}
