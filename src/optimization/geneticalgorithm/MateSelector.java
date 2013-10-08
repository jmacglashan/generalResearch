package optimization.geneticalgorithm;

import java.util.List;

public interface MateSelector {

	public List<MatePair> selectMates(List<Double> fitness, int nmatePairs);
	
	
	public class MatePair{
		public int m;
		public int f;
		
		public MatePair(int m, int f){
			this.m = m;
			this.f = f;
		}
		
	}
	
}
