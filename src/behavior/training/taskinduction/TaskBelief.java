package behavior.training.taskinduction;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for describing the belief state at any given time, without maintaining things like copies of the policy.
 *
 * @author James MacGlashan.
 */
public class TaskBelief {


	public List<TaskBeliefEntry> beliefs;

	public TaskBelief(TaskPosterior post){
		this.beliefs = new ArrayList<TaskBeliefEntry>(post.tasks.size());
		for(TaskProb tp : post.tasks){
			this.beliefs.add(new TaskBeliefEntry(tp));
		}
	}


	public int size(){
		return this.beliefs.size();
	}


	public String parseToString(String delim){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < beliefs.size(); i++){
			if(i > 0){
				sb.append(delim);
			}
			sb.append(this.beliefs.get(i).toString());
		}

		return sb.toString();
	}


	@Override
	public String toString(){
		return this.parseToString("\n");
	}


	public static class TaskBeliefEntry{

		public double p;
		public TaskDescription td;

		public TaskBeliefEntry(TaskProb tp){
			this.p = tp.getProb();
			this.td = tp.getTask();
		}

		@Override
		public String toString(){
			return this.p + " " + this.td.tf.toString();
		}

	}

}
