package behavior.bfsp;

import generalalgs.numericalintegration.NumericFunction;
import generalalgs.numericalintegration.Simpsons38;

/**
 * @author James MacGlashan.
 */
public class ProbOptimal {

	protected double absLower;
	protected double absUpper;
	protected double absRange;


	public ProbOptimal(double absLower, double absUpper){

		this.absLower = absLower;
		this.absUpper = absUpper;
		this.absRange = absUpper - absLower;

	}


	public double [] computeMargTerms(LowerUpper...bounds){
		MarginalizationTerm mterm = new MarginalizationTerm(bounds);
		double [] vals = new double[bounds.length];

		//find max lower bound
		double maxL = Double.NEGATIVE_INFINITY;
		for(LowerUpper lu : bounds){
			maxL = Math.max(lu.l, maxL);
		}

		for(int i = 0; i < bounds.length; i++){
			mterm.setQueryAction(i);
			vals[i] = Simpsons38.integrate(mterm, maxL, bounds[i].u, 30);
		}

		return vals;
	}


	public class MarginalizationTerm implements  NumericFunction{


		protected int queryAction;
		protected LowerUpper [] bounds;
		protected OtherActionMargLogTerm [] margTerms;


		public MarginalizationTerm(LowerUpper...bounds){
			this.bounds = bounds;
			this.margTerms = new OtherActionMargLogTerm[bounds.length];
			for(int i = 0; i < bounds.length; i++){
				margTerms[i] = new OtherActionMargLogTerm(bounds[i].l, bounds[i].u);
			}
		}

		public void setQueryAction(int queryAction){
			this.queryAction = queryAction;
		}


		@Override
		public double eval(double... args) {

			double qstar = args[0];

			double pqstar = 1./(ProbOptimal.this.absUpper - ProbOptimal.this.absLower);

			LowerUpper queryBounds = this.bounds[this.queryAction];

			double pqlower = 1. / (qstar - ProbOptimal.this.absLower);
			double pqupper = 1. / (ProbOptimal.this.absUpper - qstar);

			if(qstar == ProbOptimal.this.absLower){
				pqlower = 1.;
			}
			if(qstar == ProbOptimal.this.absUpper){
				pqupper = 1.;
			}


			//yes the order of (lower - upper) is correct
			//double constantForEachArm = 1. / (pqlower * (ProbOptimal.this.absLower - ProbOptimal.this.absUpper));
			//double fullConstant = Math.pow(constantForEachArm, this.bounds.length-1);

			//double initialTerm = pqstar*pqlower*pqupper * fullConstant;
			double initialTerm = pqstar*pqlower*pqupper;

			double finalTerm = initialTerm;
			double [] mvals = new double[this.margTerms.length];
			for(int i = 0; i < this.margTerms.length; i++){
				if(i == this.queryAction){
					continue;
				}
				double mval = this.margTerms[i].eval(qstar);
				mvals[i] = mval;
				finalTerm *= mval;

			}

			if(finalTerm < 0. || Double.isNaN(finalTerm)){
				System.out.println("negative.");
			}

			return finalTerm;
		}
	}


	public class OtherActionMargLogTerm implements NumericFunction{


		protected double lower;
		protected double upper;




		public OtherActionMargLogTerm(double lower, double upper){
			this.lower = lower;
			this.upper = upper;
		}


		@Override
		public double eval(double... args) {

			double integeralUpper = Math.min(args[0], this.upper);

			double num = (integeralUpper - ProbOptimal.this.absUpper) * (this.lower - ProbOptimal.this.absLower);
			double denom = (integeralUpper - ProbOptimal.this.absLower) * (this.lower - ProbOptimal.this.absUpper);

			double innerVal = num / denom;

			double logVal = Math.log(innerVal);

			double lowerRange = 1. / (integeralUpper - ProbOptimal.this.absLower);
			if(integeralUpper == ProbOptimal.this.absLower){
				lowerRange = 1.;
			}

			double coeff = 1. / (lowerRange * -ProbOptimal.this.absRange);
			double finalVal = coeff * logVal;

			if(Double.isNaN(finalVal)){
				System.out.println("error..");
			}

			return finalVal;
		}
	}






	public static void main(String [] args){

		double small = 1e-1;
		ProbOptimal po = new ProbOptimal(0.-small, 30.+small);

		double [] ans = po.computeMargTerms(new LowerUpper(28, 30.), new LowerUpper(0., 30.));

		double sum = 0.;
		for(int i = 0; i < ans.length; i++){
			System.out.println(i + ": " + ans[i]);
			sum += ans[i];
		}

		System.out.println();
		for(int i = 0; i < ans.length; i++){
			System.out.println(i + ": " + (ans[i] / sum));
		}


	}


}
