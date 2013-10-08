package optimization.infinitega.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import optimization.infinitega.GenomeRatioFitness;
import optimization.infinitega.RatioReproduce;
import optimization.infinitega.RepResult;


public class InfGARepDoubleWithUniMutate implements RatioReproduce {

	protected double selectionPercent;
	protected double mutation;
	
	
	public InfGARepDoubleWithUniMutate(double selectionPercent, double mutation){
		this.selectionPercent = selectionPercent;
		this.mutation = mutation;
	}
	
	@Override
	public RepResult ratioReproduce(List<GenomeRatioFitness> popDist) {
		
		List<GenomeRatioFitnessWithOrder> sorted = GenomeRatioFitnessWithOrder.getIndexedList(popDist);
		Collections.sort(sorted, Collections.reverseOrder());
		
		RepResult res = new RepResult(selectionPercent, new ArrayList<GenomeRatioFitness>(popDist));
		
		double doublingAmount = this.selectionPercent - this.mutation;
		double uni = this.mutation * (1. / popDist.size());
		
		for(int i = 0; i < sorted.size(); i++){
			
			GenomeRatioFitnessWithOrder grfo = sorted.get(i);
			double r = grfo.grf.gr.ratio;
			
			if(r < doublingAmount){
				res.nextPop.get(grfo.index).gr.ratio += r;
				doublingAmount -= r;
			}
			else{
				res.nextPop.get(grfo.index).gr.ratio += doublingAmount;
				break;
			}
			
		}
		
		//adding uniform random mutation and re normalize
		for(GenomeRatioFitness grf : res.nextPop){
			grf.gr.ratio += uni;
			grf.gr.ratio /= (1. + this.selectionPercent);
		}
		
		
		
		return res;
	}
	
	
	
	static class GenomeRatioFitnessWithOrder implements Comparable<GenomeRatioFitnessWithOrder>{
		GenomeRatioFitness grf;
		int index;
		
		public static List<GenomeRatioFitnessWithOrder> getIndexedList(List <GenomeRatioFitness> src){
			List <GenomeRatioFitnessWithOrder> res = new ArrayList<InfGARepDoubleWithUniMutate.GenomeRatioFitnessWithOrder>(src.size());
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
