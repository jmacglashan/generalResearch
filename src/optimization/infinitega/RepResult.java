package optimization.infinitega;

import java.util.List;

public class RepResult {
	public double repChange;
	public List<GenomeRatioFitness> nextPop;
	
	public RepResult(double repChange, List <GenomeRatioFitness> nextPop){
		this.repChange = repChange;
		this.nextPop = nextPop;
	}
	
}
