package behavior.bfsp;

import burlap.debugtools.RandomFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class UniformWinner {


	Map<Integer, Polynomial> integratedPolyomical = new HashMap<Integer, Polynomial>();


	public double probWinner(int ind, LowerUpper...bounds){


		double ql = bounds[ind].l;
		double qu = bounds[ind].u;
		double qr = bounds[ind].r;

		double greatestLower = Double.NEGATIVE_INFINITY;
		for(LowerUpper lu : bounds){
			greatestLower = Math.max(greatestLower, lu.l);
		}

		if(greatestLower > ql && greatestLower >= qu){
			return 0.; //can't win!
		}


		if(ql == qu){
			int numTied = 0;
			boolean greaterExists = false;
			for(LowerUpper lu : bounds){
				if(lu.u > qu){
					greaterExists = true;
				}
				else if(lu.u == lu.l && lu.u == qu){
					numTied++;
				}
			}
			if(!greaterExists){
				//return 1. / (double)numTied; //use this for prob of selection among optimal
				return 1.; //use this for indicator that it's a winner
			}
			else{

				//we need to get the probability of all non point masses winning (P)
				//and set this probability to (1 - P)/numTied
				double sumNonPointProb = 0.;
				for(int i = 0; i < bounds.length; i++){
					if(bounds[i].l != bounds[i].u){
						double p = probWinner(i, bounds);
						sumNonPointProb += p;
					}
				}

				//double p = (1. - sumNonPointProb) / numTied; //use this for prob selection among optimal
				double p = 1. - sumNonPointProb; //this for indicator that it's a winner
				return p;

			}
		}





		bounds = this.sortedShaved(greatestLower, bounds);
		//LowerUpper[] rev = this.reverseOrder(bounds);

		//find index of query in sorted list
		int qind = -1;
		for(int i = 0; i < bounds.length; i++){
			if(bounds[i].l == ql && bounds[i].u == qu){
				qind = i;
				break;
			}
		}

		if(qind == -1){
			throw new RuntimeException("Cound not find query index in sorted list...");
		}

		//set up initial sum
		double sum = 0.;


		double curLower = greatestLower;
		for(int i = 0; i < bounds.length; i++){

			//current upper
			double curUpper = bounds[i].u;

			if(curUpper > qu){
				break;
			}

			//doesn't contribute anymore if bounds are equal
			if(curLower >= curUpper){
				curLower = curUpper;
				continue;
			}


			//first make sure there is not an element with a lower bound greater than the current upper bound
			//if there is, then the probability of winning in this range is zero, so we can cancel it out

			double usedUpper = Math.min(curUpper, qu);
			boolean greaterLowerExists = false;
			for(int j = i; j < bounds.length; j++){
				if(bounds[j].l >= usedUpper){
					greaterLowerExists = true;
					break;
				}
			}


			if(!greaterLowerExists) {

				//numerator of integral is (x - l_i) for all i from current index to end,
				//except the query point.
				//that means polynomial dimension is is n - index - 1
				int d = bounds.length - i - 1;
				Polynomial indefiniteIntegral = this.getIntegratedPolynomial(d);
				double[] coefficients = new double[d];
				double productSeries = 1.;
				//coefficients go from current index to upper bound, except with the query element
				//denominator does the same, but with lower bounds (no x and is not integrated)
				//TODO: how do we manage case when there is a element with a lower bound greater than the current upper bound? Whole thing goes to zero?
				int ci = 0;
				for (int j = i; j < bounds.length; j++) {
					if (j == qind) {
						continue;
					}
					coefficients[ci] = bounds[j].l;
					productSeries *= bounds[j].r;

					ci++;
				}


				double integralVal = indefiniteIntegral.evaluate(usedUpper, coefficients) - indefiniteIntegral.evaluate(curLower, coefficients);

				double fullIntegralVal = integralVal / productSeries;
				sum += fullIntegralVal;

			}


			/*
			//otherwise we set up integral
			int d = bounds.length - i - 1;
			Polynomial indefiniteIntegral = this.getIntegratedPolynomial(d);
			double[] coefficients = new double[d];
			//coefficients for poly integral go from lower to upper, but not last upper
			for (int j = i; j < bounds.length - 1; j++) {
				coefficients[j] = bounds[j].l;
			}
			double usedUpper = Math.min(curUpper, qu);
			double integralVal = indefiniteIntegral.evaluate(curUpper, coefficients) - indefiniteIntegral.evaluate(curLower, coefficients);

			//denominator product series goes from upper to lower
			double productSeries = 1.;
			for (int j = i; j < rev.length - 1; j++) {
				productSeries *= rev[j].r;
			}

			double fullIntegralVal = integralVal / productSeries;

			sum += fullIntegralVal;

			*/


			curLower = curUpper;

		}

		double prob = sum / qr;


		return prob;
	}




	public double approximateProbWinner(int ind, int numSamples, LowerUpper...bounds){

		double sum = 0.;
		double ql = bounds[ind].l;
		double qu = bounds[ind].u;

		//check for an instance with a greater lower bound than query's upper
		for(LowerUpper ul : bounds){
			if(ul.l >= qu){
				return 0.;
			}
		}

		//bounds = this.sortedShaved(ql, bounds);


		for(int i = 0; i < numSamples; i++){

			double x = urand(ql, qu);

			double productSeris = 1.;
			for(int j = 0; j < bounds.length; j++){

				if(j == ind){
					continue;
				}

				LowerUpper ul = bounds[j];
				//double pLess = (Math.min(x, ul.u) - ul.l) / ul.r;
				double pLess = probLess(x, ul);
				productSeris *= pLess;

			}

			sum += productSeris;


		}

		double estimate = sum / (double)numSamples;
		return estimate;

	}

	protected double probLess(double x, LowerUpper lu){

		if(x > lu.u){
			return 1.;
		}
		if(x < lu.l){
			return 0.;
		}
		return (x - lu.l) / lu.r;

	}


	public double [] completeSim(int nSims, LowerUpper...bounds){

		double [] probs = new double[bounds.length];


		for(int i = 0; i < nSims; i++){

			int maxIndex = -1;
			double maxVal = Double.NEGATIVE_INFINITY;
			for(int j = 0; j < bounds.length; j++){
				LowerUpper lu = bounds[j];
				double r = urand(lu.l, lu.u);
				if(r > maxVal){
					maxIndex = j;
					maxVal = r;
				}
			}

			probs[maxIndex] += 1.;

		}

		for(int j = 0; j < bounds.length; j++){
			probs[j] /= (double)nSims;
		}


		return probs;

	}



	protected LowerUpper[] reverseOrder(LowerUpper[] bounds){
		LowerUpper[] rev = new LowerUpper[bounds.length];
		int c = 0;
		for(int i = bounds.length-1; i >= 0; i--){
			rev[c] = bounds[i];
			c++;
		}
		return rev;
	}


	protected LowerUpper[] sortedShaved(double ql, LowerUpper[] bounds){

		bounds = bounds.clone();

		Arrays.sort(bounds, new ULComparator());

		int firstGreaterUpper = -1;
		for(int i = 0; i < bounds.length; i++){
			if(bounds[i].u >= ql){
				firstGreaterUpper = i;
				break;
			}
		}

		if(firstGreaterUpper == -1){
			return new LowerUpper[0];
		}

		LowerUpper[] shaved = new LowerUpper[bounds.length-firstGreaterUpper];
		int c = 0;
		for(int i = firstGreaterUpper; i < bounds.length; i++){
			shaved[c] = bounds[i];
			c++;
		}


		return shaved;

	}



	public Polynomial getIntegratedPolynomial(int degree){

		Polynomial stored = this.integratedPolyomical.get(degree);
		if(stored == null){
			Polynomial p = new Polynomial();
			p.constructXMinusC(degree);
			stored = p.integrate();
			this.integratedPolyomical.put(degree, stored);
		}

		return stored;

	}


	protected static double urand(double l, double u){
		return RandomFactory.getMapped(0).nextDouble()*(u-l) + l;
	}



	public static class LowerUpper {

		public double l;
		public double u;
		public double r;

		public LowerUpper(double l, double u){
			this.l = l;
			this.u = u;
			this.r = (u - l);
		}


	}


	public static class ULComparator implements Comparator<LowerUpper>{


		@Override
		public int compare(LowerUpper o1, LowerUpper o2) {
			return Double.compare(o1.u, o2.u);
		}

	}




	public static void main(String [] args){

		UniformWinner uw = new UniformWinner();

		LowerUpper[] bound21 = new LowerUpper[]{new LowerUpper(0, 30), new LowerUpper(8, 23)};
		//LowerUpper[] bound21 = new LowerUpper[]{new LowerUpper(0, 30), new LowerUpper(9.99, 10)};
		LowerUpper[] bound22 = new LowerUpper[]{new LowerUpper(0, 30), new LowerUpper(8, 23), new LowerUpper(10, 20)};

		double t1b = uw.probWinner(0, bound21);
		double t1c = uw.probWinner(1, bound21);

		double t1ab = uw.approximateProbWinner(0, 10000, bound21);
		double t1ac = uw.approximateProbWinner(1, 10000, bound21);

		double [] t1Sim = uw.completeSim(1000000, bound21);
		double t1sb = t1Sim[0];
		double t1sc = t1Sim[1];

		//System.out.println("p(b) ~~= " + t1sb + "; p(c) ~~= " + t1sc);
		System.out.println("p(b) ~= " + t1ab + "; p(c) ~= " + t1ac);
		System.out.println("p(b) = " + t1b + "; p(c) = " + t1c);




		double t2ab = uw.approximateProbWinner(0, 10000, bound22);
		double t2ac = uw.approximateProbWinner(1, 10000, bound22);
		double t2aa = uw.approximateProbWinner(2, 10000, bound22);

		double [] t2Sim = uw.completeSim(1000000, bound22);
		double t2sb = t2Sim[0];
		double t2sc = t2Sim[1];
		double t2sa = t2Sim[2];

		double t2b = uw.probWinner(0, bound22);
		double t2c = uw.probWinner(1, bound22);
		double t2a = uw.probWinner(2, bound22);

		System.out.println("2: p(b) ~~= " + t2sb + "; p(c) ~~= " + t2sc + "; p(a) ~~= " + t2sa);
		System.out.println("2: p(b) ~= " + t2ab + "; p(c) ~= " + t2ac + "; p(a) ~= " + t2aa);
		System.out.println("2: p(b) = " + t2b + "; p(c) = " + t2c + "; p(a) = " + t2a);



	}

}
