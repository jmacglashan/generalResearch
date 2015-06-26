package behavior.training.experiments.webparsing;

import behavior.training.experiments.interactive.soko.sokoamdp.SokoAMDPPlannerPolicyGen;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class OptimalChecker {

	Map<String, TerminalFunction> commandToTrueTask = new HashMap<String, TerminalFunction>();
	Domain domain;

	public OptimalChecker(Domain domain){

		this.domain = domain;

		commandToTrueTask.put("Move to the blue room", constructTF(Sokoban2Domain.PFAGENTINROOM, "agent0", "room1"));
		commandToTrueTask.put("Move to the yellow room", constructTF(Sokoban2Domain.PFAGENTINROOM, "agent0", "room2"));
		commandToTrueTask.put("Move the red bag to the blue room", constructTF(Sokoban2Domain.PFBLOCKINROOM, "block0", "room0"));
		commandToTrueTask.put("Move the blue chair to the purple room", constructTF(Sokoban2Domain.PFBLOCKINROOM, "block0", "room3"));

	}


	public TerminalFunction constructTF(String pfName, String arg0, String arg1){
		List<GroundedProp> goalgp = new ArrayList<GroundedProp>(1);
		goalgp.add(new GroundedProp(domain.getPropFunction(pfName), new String[]{arg0, arg1}));
		TerminalFunction goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(goalgp);
		return goalTF;
	}


	public int [] actionOptimality(String command, EpisodeAnalysis ea){

		SokoAMDPPlannerPolicyGen pgen = new SokoAMDPPlannerPolicyGen();
		TerminalFunction tf = this.commandToTrueTask.get(command);
		Policy p = pgen.getPolicy(domain, ea.getState(0), new UniformCostRF(), tf, new DiscreteStateHashFactory());


		int [] isOptimal = new int[ea.maxTimeStep()];
		for(int i = 0; i < ea.maxTimeStep(); i++){
			GroundedAction selected = ea.getAction(i);

			if(!tf.isTerminal(ea.getState(i))){
				GroundedAction optimal = (GroundedAction)p.getAction(ea.getState(i));
				if(selected.equals(optimal)){
					isOptimal[i] = 1;
				}
				else {
					isOptimal[i] = 0;
				}
			}
			else{
				if(selected.actionName().equals("noop")){
					isOptimal[i] = 1;
				}
				else{
					isOptimal[i] = 0;
				}
			}
		}

		return isOptimal;

	}

}
