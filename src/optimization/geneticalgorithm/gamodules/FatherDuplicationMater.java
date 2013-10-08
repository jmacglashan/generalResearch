package optimization.geneticalgorithm.gamodules;

import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import optimization.geneticalgorithm.Mate;

public class FatherDuplicationMater implements Mate {

	public FatherDuplicationMater() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<OptVariables> mate(OptVariables m, OptVariables f, int nOffspring) {
		
		List <OptVariables> offspring = new ArrayList<OptVariables>(nOffspring);
		
		for(int i = 0; i < nOffspring; i++){
			offspring.add(new OptVariables(f));
		}
		
		return offspring;
	}

}
