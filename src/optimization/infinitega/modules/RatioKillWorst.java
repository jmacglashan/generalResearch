package optimization.infinitega.modules;

import java.util.ArrayList;
import java.util.List;

import optimization.infinitega.GenomeRatioFitness;
import optimization.infinitega.RatioKiller;
import optimization.infinitega.RepResult;

public class RatioKillWorst implements RatioKiller {

	@Override
	public void kill(List<GenomeRatioFitness> oldPop, RepResult nextPop) {
		
		List<GenomeRatioFitnessWithOrder> sorted = GenomeRatioFitnessWithOrder.getIndexedList(oldPop);
		double toRemove = nextPop.repChange;
		for(int i = 0; i < sorted.size(); i++){
			GenomeRatioFitnessWithOrder grfo = sorted.get(i);
			double r = grfo.grf.gr.ratio;
			if(r < toRemove){
				toRemove -= r;
				nextPop.nextPop.get(grfo.index).gr.ratio = 0.;
			}
			else{
				nextPop.nextPop.get(grfo.index).gr.ratio -= toRemove;
				break;
			}
		}

	}

	
	
	static class GenomeRatioFitnessWithOrder implements Comparable<GenomeRatioFitnessWithOrder>{
		GenomeRatioFitness grf;
		int index;
		
		public static List<GenomeRatioFitnessWithOrder> getIndexedList(List <GenomeRatioFitness> src){
			List <GenomeRatioFitnessWithOrder> res = new ArrayList<RatioKillWorst.GenomeRatioFitnessWithOrder>(src.size());
			for(int i = 0; i < src.size(); i++){
				res.add(new GenomeRatioFitnessWithOrder(src.get(i), i));
			}
			return res;
		}
		
		public GenomeRatioFitnessWithOrder(GenomeRatioFitness gr, int index){
			this.grf = gr;
			this.index = index;
		}

		@Override
		public int compareTo(GenomeRatioFitnessWithOrder arg0) {
			if(this.grf.fitness < arg0.grf.fitness){
				return -1;
			}
			if(this.grf.fitness > arg0.grf.fitness){
				return 1;
			}
			return 0;
		}
		
	}
	
}
