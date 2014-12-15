package behavior.training.taskinduction;

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
public class BeliefExtendedEA extends EpisodeAnalysis{

	protected List<TaskBelief> taskBeliefs;

	public BeliefExtendedEA(State initialState, TaskBelief tb){
		super(initialState);
		this.taskBeliefs.add(tb);
	}


	@Override
	protected void initializeDatastructures() {
		super.initializeDatastructures();
		this.taskBeliefs = new ArrayList<TaskBelief>();
	}


	/**
	 * Records an transition event where the agent applied the usingAction action in the last
	 * state in this object's state sequence, transitioned to state nextState, and received reward r; also
	 * records the new task beliefs after the action was taken and the reward received
	 * @param usingAction the action the agent used that caused the transition
	 * @param nextState the next state to which the agent transitioned
	 * @param r the reward the agent received for this transition.
	 * @param nextBeliefs the next taks beliefs
	 */
	public void recordTransitionTo(GroundedAction usingAction, State nextState, double r, TaskBelief nextBeliefs){
		stateSequence.add(nextState);
		actionSequence.add(usingAction);
		rewardSequence.add(r);
		this.taskBeliefs.add(nextBeliefs);
	}

	public TaskBelief getTaskBelief(int t){
		return this.taskBeliefs.get(t);
	}


	public void writeBeliefsToFile(String filePath){

		if(!filePath.endsWith(".beliefs")){
			filePath = filePath + ".beliefs";
		}

		File f = (new File(filePath)).getParentFile();
		if(f != null){
			f.mkdirs();
		}


		try{

			String str = this.parseBeliefsIntoString();
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
			out.write(str);
			out.close();


		}catch(Exception e){
			System.out.println(e);
		}


	}

	public String parseBeliefsIntoString(){
		StringBuilder sb = new StringBuilder(256);
		for(int i = 0; i < this.taskBeliefs.size(); i++){
			if(i > 0){
				sb.append("\n##\n");
			}
			sb.append(this.taskBeliefs.get(i).toString());
		}
		return sb.toString();
	}


}
