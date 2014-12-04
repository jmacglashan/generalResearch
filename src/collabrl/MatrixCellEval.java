package collabrl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class MatrixCellEval {

	AlgEvaluator eval;
	List<TaskAndTrain> tasks;
	List<LearningAgentForTaskGenerator> agents;


	public MatrixCellEval(AlgEvaluator eval){
		this.eval = eval;
		this.tasks = new ArrayList<TaskAndTrain>();
		this.agents = new ArrayList<LearningAgentForTaskGenerator>();
	}

	public MatrixCellEval(List<TaskAndTrain> tasks, List<LearningAgentForTaskGenerator> agents, AlgEvaluator eval){
		this.eval = eval;
		this.tasks = tasks;
		this.agents = agents;
	}

	public void addTask(TaskAndTrain task){
		this.tasks.add(task);
	}

	public void addAgentGenerator(LearningAgentForTaskGenerator agentGen){
		this.agents.add(agentGen);
	}


	public int nTasks(){
		return this.tasks.size();
	}

	public int nAgents(){
		return this.agents.size();
	}

	public CellResults evaluate(int taskId, int agentId){
		TaskAndTrain task = this.tasks.get(taskId);
		LearningAgentForTaskGenerator agent = this.agents.get(agentId);
		double [] p = this.eval.eval(task, agent);
		CellResults cr = new CellResults(task, agent, p, this.eval.timeSteps(task), taskId, agentId);

		return cr;
	}



	public static class CellResults{
		public String taskDescription;
		public String algDescription;
		public double [] performance;
		public int [] timeSteps;

		int row;
		int col;


		public CellResults(TaskAndTrain task, LearningAgentForTaskGenerator agentGen, double [] performance, int [] timeSteps, int r, int c){
			this.taskDescription = task.description;
			this.algDescription = agentGen.getAgentName();
			this.performance = performance;
			this.timeSteps = timeSteps;
			this.row = r;
			this.col = c;
		}


		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(row + " " + col + "\n");
			sb.append(taskDescription+"\n");
			sb.append(algDescription+"\n");

			for(int i = 0; i < this.timeSteps.length; i++){
				if(i > 0){
					sb.append(" ");
				}
				sb.append(this.timeSteps[i]);
			}
			sb.append("\n");

			for(int i = 0; i < this.performance.length; i++){
				if(i > 0){
					sb.append(" ");
				}
				sb.append(this.performance[i]);
			}
			sb.append("\n");



			return sb.toString();
		}


		public void writeToFile(String directoryPath){
			if(!directoryPath.endsWith("/")){
				directoryPath += "/";
			}
			String path = directoryPath + this.row + "_" + this.col + ".txt";
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(path));
				writer.write(this.toString()+"\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}


}
