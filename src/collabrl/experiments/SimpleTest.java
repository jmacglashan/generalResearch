package collabrl.experiments;

import collabrl.MatrixCellEval;

/**
 * @author James MacGlashan.
 */
public class SimpleTest {

	public static void main(String [] args){
		//DefaultMatrixConstructor constructor = new DefaultMatrixConstructor();
		DefaultMatrixConstructor constructor = new DefaultMatrixConstructor(1, 4);
		constructor.addAllDefaultAgents();
		//constructor.addAllDefaultTasks();
		constructor.addAllMaxTimeStepTasks();

		MatrixCellEval matrix = constructor.getMatrix();

		MatrixCellEval.CellResults eval = matrix.evaluate(0, 35);
		//MatrixCellEval.CellResults eval2 = matrix.evaluate(0, 1);

		System.out.println(eval.toString());
		System.out.println("--");
		//System.out.println(eval2.toString());

		eval.writeToFile("oomdpResearch/testCollabData");
		//eval2.writeToFile("oomdpResearch/testCollabData");

	}

}
