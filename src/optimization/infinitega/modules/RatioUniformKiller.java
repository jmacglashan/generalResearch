package optimization.infinitega.modules;

import java.util.List;

import optimization.infinitega.GenomeRatioFitness;
import optimization.infinitega.RatioKiller;
import optimization.infinitega.RepResult;

public class RatioUniformKiller implements RatioKiller {

	@Override
	public void kill(List<GenomeRatioFitness> oldPop, RepResult nextPop) {
		
		for(int i = 0; i < oldPop.size(); i++){
			nextPop.nextPop.get(i).gr.ratio -= oldPop.get(i).gr.ratio*nextPop.repChange;
		}
		
	}

}
