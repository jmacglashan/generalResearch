package behavior.bfsp;

import burlap.debugtools.RandomFactory;

public class UniGreaterProb {


	public static double probXGreaterOrEqualThanY(double lx, double ux, double ly, double uy){
		
		if(lx == ly && ux == uy){
			return 1.;
		}
		
		double maxl = Math.max(lx, ly);
		double minu = Math.min(ux, uy);
		
		double probYLessThanX = probLess(ly, uy, maxl);
		double probXBound = probXInBound(lx, ux, maxl, minu);
		double probYBound = probXInBound(ly, uy, maxl, minu);
		double probXGreaterThanY = probGreater(lx, ux, minu);
		
		double t2 = .5*probXBound*probYBound;
		double t3 = probXGreaterThanY*(1.-probYLessThanX);
		
		double finalProb = probYLessThanX + t2 + t3;
		
		return finalProb;
	}

	public static double probIndGreaterOrEqual(double [][] boundedArms, int queryInd){

		double ql = boundedArms[queryInd][0];
		double qu = boundedArms[queryInd][1];

		double product = 1.;
		for(int i = 0; i < boundedArms.length; i++){
			if(i == queryInd){
				continue;
			}

			double p = probXGreaterOrEqualThanY(ql, qu, boundedArms[i][0], boundedArms[i][1]);
			product *= p;

		}

		return product;
	}


	public static double probIndWinner(double [][] boundedArms, int queryInd){

		double ql = boundedArms[queryInd][0];
		double qu = boundedArms[queryInd][1];

		double product = 1.;
		for(int i = 0; i < boundedArms.length; i++){
			if(i == queryInd){
				continue;
			}

			double p = 1. - probXGreaterOrEqualThanY(ql, qu, boundedArms[i][0], boundedArms[i][1]);
			product *= p;

		}

		return 1. - product;

	}
	
	public static double probLess(double l, double u, double v){
		if(l >= v){
			return 0.;
		}
		if(u <= v){
			return 1.;
		}
		double p = (v - l) / (u - l);
		return p;
	}
	
	public static double probGreater(double l, double u, double v){
		
		if(u <= v){
			return 0.;
		}
		if(l >= v){
			return 1.;
		}
		double p = (u - v) / (u - l);
		return p;
		
	}
	
	public static double probXInBound(double lx, double ux, double lr, double ur){
		if(lr > ux || ur < lx){
			return 0.;
		}
		
		if(ux == lx){
			if(lr == lx && ux == ur){
				return 1.;
			}
		}
		
		double p = (ur - lr) / (ux - lx);
		return p;
	}
	
	protected static void compare(double lx, double ux, double ly, double uy, int sims){
		double anal = probXGreaterOrEqualThanY(lx, ux, ly, uy);
		double sim = simXGY(lx, ux, ly, uy, sims);
		
		System.out.println("Comparing (" + lx + ", " + ux + ") (" + ly + ", " + uy + ")");
		System.out.println("Hard: " + anal + "\nSims: " + sim);
		System.out.printf("Diff: %.4f\n", Math.abs(anal - sim));
		System.out.println("----------------");
		
	}
	
	protected static double simXGY(double lx, double ux, double ly, double uy, int sims){
		int sum = 0;
		for(int i = 0; i < sims; i++){
			sum += isMax(lx, ux, ly, uy);
		}
		return (double)sum/(double)sims;
	}
	
	protected static int isMax(double lx, double ux, double ly, double uy){
		double x = urand(lx, ux);
		double y = urand(ly, uy);
		return x > y ? 1 : 0;
	}


	protected static double [] sampledWins(double [][] arms, int sims){

		double [] sums = new double[arms.length];
		for(int i = 0; i < sims; i++){
			sums[sampledMaxInd(arms)] += 1;
		}
		//normalize
		for(int i = 0; i < sums.length; i++){
			sums[i] /= (double)sims;
		}

		return sums;
	}

	protected static int sampledMaxInd(double [][] arms){

		int maxInd = -1;
		double maxVal = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < arms.length; i++){
			double r = urand(arms[i][0], arms[i][1]);
			if(r > maxVal){
				maxVal = r;
				maxInd = i;
			}
		}

		return maxInd;

	}
	
	protected static double urand(double l, double u){
		return RandomFactory.getMapped(0).nextDouble()*(u-l) + l;
	}
	
	public static void main(String [] args){


		double [][] testArms1 = {
				{0, 30},
				{8, 23}
		};

		double [][] testArms2 = {
				{0, 30},
				{8, 23},
				{10, 20}
		};

		int nsims = 1000000;

//		double t1B = probIndGreaterOrEqual(testArms1, 0);
//		double t1C = probIndGreaterOrEqual(testArms1, 1);
//
//		double t2B = probIndGreaterOrEqual(testArms2, 0);
//		double t2C = probIndGreaterOrEqual(testArms2, 1);
//		double t2A = probIndGreaterOrEqual(testArms2, 2);


		double t1B = probIndWinner(testArms1, 0);
		double t1C = probIndWinner(testArms1, 1);

		double t2B = probIndWinner(testArms2, 0);
		double t2C = probIndWinner(testArms2, 1);
		double t2A = probIndWinner(testArms2, 2);

		System.out.println("Without A:\n" + t1B + ", " + t1C);

		double [] t1SampledProbs = sampledWins(testArms1, nsims);
		System.out.println("Without A (simulated):\n" + t1SampledProbs[0] + ", " + t1SampledProbs[1] + "\n");


		System.out.println("With A:\n" + t2B + ", " + t2C + ", " + t2A);
		double [] t2SampledProbs = sampledWins(testArms2, nsims);
		System.out.println("With A (simulated):\n" + t2SampledProbs[0] + ", " + t2SampledProbs[1] + "\n");


		/*
		int nsims = 1000000;
		compare(0,2,1,3, nsims);
		compare(1,3,0,2,nsims);
		
		compare(1,2,0,3,nsims);
		compare(0,3,1,2,nsims);
		
		compare(0,1,0,1,nsims);
		
		compare(0,1,2,3,nsims);
		compare(2,3,0,1,nsims);
		*/
	}
	
	
}
