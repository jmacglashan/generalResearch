package behavior.bfsp;

import java.util.Comparator;

/**
 * @author James MacGlashan.
 */
public class LowerUpper {

	public double l;
	public double u;
	public double r;

	public LowerUpper(double l, double u){
		this.l = l;
		this.u = u;
		this.r = (u - l);
	}


	public static class ULComparator implements Comparator<LowerUpper> {


		@Override
		public int compare(LowerUpper o1, LowerUpper o2) {
			return Double.compare(o1.u, o2.u);
		}

	}
}
