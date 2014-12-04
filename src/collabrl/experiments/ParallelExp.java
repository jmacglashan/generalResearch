package collabrl.experiments;

import collabrl.MatrixCellEval;

/**
 * @author James MacGlashan.
 */
public class ParallelExp {


	public static void main(String [] args){

		if(args.length != 3){
			System.out.println("Format: taskId agentId output directory");
			System.exit(0);
		}

		int tId = Integer.parseInt(args[0]);
		int aId = Integer.parseInt(args[1]);



		//DefaultMatrixConstructor constructor = new DefaultMatrixConstructor();
		DefaultMatrixConstructor constructor = new DefaultMatrixConstructor(1, 50);
		constructor.addAllDefaultAgents();
		//constructor.addAllDefaultTasks();
		constructor.addAllMaxTimeStepTasks();

		MatrixCellEval matrix = constructor.getMatrix();


		if(tId >= matrix.nTasks()){
			throw new RuntimeException("Error: requested task " + tId + " is >= number of tasks: " + matrix.nTasks());
		}
		if(aId >= matrix.nAgents()){
			throw new RuntimeException("Error: requested agent " + aId + " is >= number of agents: " + matrix.nAgents());
		}

		MatrixCellEval.CellResults eval = matrix.evaluate(tId, aId);

		System.out.println(eval.toString());

		eval.writeToFile(args[2]);



	}

}
