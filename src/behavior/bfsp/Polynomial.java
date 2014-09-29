package behavior.bfsp;

import java.util.LinkedList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class Polynomial {


	public List<Term> terms = new LinkedList<Term>();


	public void constructXMinusC(int n){


		if(n > 0) {
			//first add our most significant order term
			terms.add(new Term(n));

			//now iterate through the rest down to order 1
			for (int i = n - 1; i > 0; i--) {

				int k = n - i;
				double sgn = this.evenOddSign(k);
				int[] comb = initialComb(k);
				do {
					terms.add(new Term(i, sgn, comb.clone()));
				} while (nextComb(comb, k, n) == 1);

			}
		}

		//and handle order 0 specially
		terms.add(new Term(0, this.evenOddSign(n), initialComb(n)));

	}


	public double evaluate(double x, double...coefficients){

		double sum = 0.;
		for(Term t : this.terms){
			sum += t.evaluate(x, coefficients);
		}

		return sum;

	}


	public Polynomial integrate(){
		Polynomial np = new Polynomial();
		for(Term t : this.terms){
			np.terms.add(t.integrate());
		}
		return np;
	}




	@Override
	public String toString(){

		StringBuilder b = new StringBuilder();
		boolean doneFirst = false;
		for(Term t : this.terms){
			if(doneFirst){b.append(" ");}
			b.append(t.toString());
			doneFirst = true;
		}

		return b.toString();

	}



	protected double evenOddSign(int i){
		if(i % 2 == 0){
			return 1.;
		}
		return -1.;
	}

	public static class Term{

		int pow;
		int [] coefficients;
		double scalar = 1.;

		public Term(int pow){
			this.pow = pow;
			this.coefficients = new int[0];
		}

		public Term(int pow, double scalar, int...coefficients){
			this.pow = pow;
			this.scalar = scalar;
			this.coefficients = coefficients;
		}

		public double evaluate(double xVal, double...coefficientVals){
			double coProd = 1.;
			for(int c : this.coefficients){
				coProd *= coefficientVals[c];
			}
			return coProd*this.scalar*Math.pow(xVal, pow);
		}

		public Term integrate(){
			int nPow = this.pow+1;
			double nScalar = this.scalar * 1. / (double)nPow;
			Term nt = new Term(nPow, nScalar, this.coefficients.clone());
			return nt;
		}

		@Override
		public String toString(){
			String scalRep = Double.toString(this.scalar);
			String powRep = Integer.toString(this.pow);
			StringBuilder b = new StringBuilder(scalRep.length() + powRep.length() + 2*this.coefficients.length + 4);
			if(this.scalar != 1.){
				if(this.scalar == -1.){
					b.append("-");
				}
				else {
					b.append(scalRep);
				}
			}
			else if(this.pow != 0){
				b.append("+");
			}
			else{
				b.append("+1");
			}

			for(int c : this.coefficients){
				b.append("c" + (c+1));
			}
			if(this.pow == 0){
				//do nothing
			}
			else if(this.pow == 1){
				b.append("x");
			}
			else {
				b.append("x^").append(powRep);
			}

			return b.toString();
		}

	}









	private static int [] initialComb(int k){
		int [] res = new int[k];
		for(int i = 0; i < k; i++){
			res[i] = i;
		}

		return res;
	}


	/**
	 * Iterates through combinations.
	 * Modified code from: http://compprog.wordpress.com/tag/generating-combinations/
	 * @param comb the last combination of elements selected
	 * @param k number of elements in any combination (n choose k)
	 * @param n number of possible elements (n choose k)
	 * @return 0 when there are no more combinations; 1 when a new combination is generated
	 */
	private static int nextComb(int [] comb, int k, int n){

		int i = k-1;
		comb[i]++;

		while(i > 0 && comb[i] >= n-k+1+i){
			i--;
			comb[i]++;
		}

		if(comb[0] > n-k){
			return 0;
		}

		/* comb now looks like (..., x, n, n, n, ..., n).
		Turn it into (..., x, x + 1, x + 2, ...) */
		for(i = i+1; i < k; i++){
			comb[i] = comb[i-1] + 1;
		}

		return 1;
	}























	public static void main(String[] args){
		Polynomial p = new Polynomial();
		p.constructXMinusC(0);
		System.out.println(p.toString());

		Polynomial integratedP = p.integrate();
		System.out.println(integratedP.toString());

		//System.out.println(p.evaluate(6, 1,2,3));
	}

}
