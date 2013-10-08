package optimization.geneticalgorithm.gamodules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import optimization.OptVariables;
import optimization.geneticalgorithm.Mate;

public class ContinuousBlendMate implements Mate {

	protected Random rand;
	
	public ContinuousBlendMate(){
		rand = RandomFactory.getMapped(0);
	}
	
	@Override
	public List<OptVariables> mate(OptVariables m, OptVariables f, int nOffspring) {
		
		List <OptVariables> offspring = new ArrayList<OptVariables>();
		
		for(int i = 0; i < nOffspring; i++){
			
			OptVariables child = new OptVariables(m.size());
			for(int j = 0; j < child.size(); j++){
				double beta = rand.nextDouble();
				double mv = m.v(j);
				double fv = f.v(j);
				child.set(j, beta*(fv-mv) + mv);
			}
			
			offspring.add(child);
			
		}
		
		return offspring;
	}

}
