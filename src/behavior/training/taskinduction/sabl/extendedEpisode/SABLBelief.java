package behavior.training.taskinduction.sabl.extendedEpisode;

import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.strataware.FeedbackStrategy;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SABLBelief {

	List<TaskProb> taskProbs;
	List <FeedbackStrategy> stratProbs;

	public SABLBelief(List<TaskProb> taskProbs, List<FeedbackStrategy> stratProbs) {
		this.taskProbs = taskProbs;
		this.stratProbs = stratProbs;
	}



	public String parseTaskToString(String delim){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < taskProbs.size(); i++){
			if(i > 0){
				sb.append(delim);
			}
			sb.append(this.taskProbs.get(i).toString());
		}

		return sb.toString();
	}

	public String parseStrategyToString(String delim){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < stratProbs.size(); i++){
			if(i > 0){
				sb.append(delim);
			}
			sb.append(this.stratProbs.get(i).toString());
		}

		return sb.toString();
	}


	@Override
	public String toString(){
		return this.parseTaskToString("\n") + "\n---\n" + this.parseStrategyToString("\n");
	}

}
