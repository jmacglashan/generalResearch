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
	
	protected static double urand(double l, double u){
		return RandomFactory.getMapped(0).nextDouble()*(u-l) + l;
	}
	
	public static void main(String [] args){
		int nsims = 1000000;
		compare(0,2,1,3, nsims);
		compare(1,3,0,2,nsims);
		
		compare(1,2,0,3,nsims);
		compare(0,3,1,2,nsims);
		
		compare(0,1,0,1,nsims);
		
		compare(0,1,2,3,nsims);
		compare(2,3,0,1,nsims);
	}
	
	
}
