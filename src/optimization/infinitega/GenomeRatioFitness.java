package optimization.infinitega;

public class GenomeRatioFitness implements Comparable<GenomeRatioFitness>{
	public GenomeRatio			gr;
	public double				fitness;
	
	public GenomeRatioFitness(GenomeRatio gr, double fitness){
		this.gr = gr;
		this.fitness = fitness;
	}

	@Override
	public int compareTo(GenomeRatioFitness arg0) {
		if(this.fitness < arg0.fitness){
			return -1;
		}
		if(this.fitness > arg0.fitness){
			return 1;
		}
		return 0;
	}
}
