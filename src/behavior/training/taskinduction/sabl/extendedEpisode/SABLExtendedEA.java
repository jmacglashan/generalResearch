package behavior.training.taskinduction.sabl.extendedEpisode;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SABLExtendedEA extends EpisodeAnalysis{

	protected List<SABLBelief> beliefs;

	public SABLExtendedEA(State initialState, SABLBelief belief){
		super(initialState);
		this.beliefs.add(belief);
	}

	@Override
	protected void initializeDatastructures() {
		super.initializeDatastructures();
		this.beliefs = new ArrayList<SABLBelief>();
	}


	/**
	 * Records an transition event where the agent applied the usingAction action in the last
	 * state in this object's state sequence, transitioned to state nextState, and received reward r; also
	 * records the new task/strategy beliefs after the action was taken and the reward received
	 * @param usingAction the action the agent used that caused the transition
	 * @param nextState the next state to which the agent transitioned
	 * @param r the reward the agent received for this transition.
	 * @param nextBeliefs the next task and strategy beliefs
	 */
	public void recordTransitionTo(GroundedAction usingAction, State nextState, double r, SABLBelief nextBeliefs){
		stateSequence.add(nextState);
		actionSequence.add(usingAction);
		rewardSequence.add(r);
		this.beliefs.add(nextBeliefs);
	}


	public void writeBeliefsToFile(String filePath){

		String taskBeliefPath = filePath + ".tbeliefs";
		String stratBeliefPath = filePath + ".sbeliefs";

		File f = (new File(filePath)).getParentFile();
		if(f != null){
			f.mkdirs();
		}


		try{

			String str = this.parseTaskBeliefsIntoString();
			BufferedWriter out = new BufferedWriter(new FileWriter(taskBeliefPath));
			out.write(str);
			out.close();


		}catch(Exception e){
			System.out.println(e);
		}

		try{

			String str = this.parseStrategyBeliefsIntoString();
			BufferedWriter out = new BufferedWriter(new FileWriter(stratBeliefPath));
			out.write(str);
			out.close();


		}catch(Exception e){
			System.out.println(e);
		}


	}


	public String parseTaskBeliefsIntoString(){
		StringBuilder sb = new StringBuilder(256);
		for(int i = 0; i < this.beliefs.size(); i++){
			if(i > 0){
				sb.append("\n##\n");
			}
			sb.append(this.beliefs.get(i).parseTaskToString("\n"));
		}
		return sb.toString();
	}

	public String parseStrategyBeliefsIntoString(){
		StringBuilder sb = new StringBuilder(256);
		for(int i = 0; i < this.beliefs.size(); i++){
			if(i > 0){
				sb.append("\n##\n");
			}
			sb.append(this.beliefs.get(i).parseStrategyToString("\n"));
		}
		return sb.toString();
	}

}
