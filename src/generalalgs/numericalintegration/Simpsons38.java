package generalalgs.numericalintegration;

/**
 * Integration using composite version of Simpson's 3/8ths rule. Assumes the function is single variable.
 * @author James MacGlashan.
 */
public class Simpsons38 {

	/**
	 * Integrates function f on interval [a, b] using n+1 integration points (plus one for evaluation at a and then n points after).
	 * n must be a multiple of 3.
	 * @param f the function to integrate
	 * @param a the interval lower bound
	 * @param b the interval upper bound
	 * @param n the number of integration points not counting evaluation at f(a); must be a multiple of 3.
	 * @return the approximate value of the integral
	 */
	public static double integrate(NumericFunction f, double a, double b, int n){
		if(n % 3 != 0){
			throw new RuntimeException("Number of integraiton points for Simpson's 3/8th rule must be a multiple of 3");
		}

		double h = (b - a) / ((double)n);
		double sum = f.eval(a) + f.eval(b);
		for(int i = 1; i < n-3; i+=3){
			double xi = a + i*h;
			sum += 3 * f.eval(xi);
			sum += 3 * f.eval(xi+=h);
			sum += 2 * f.eval(xi+h);
		}
		double xi = a + (n-2)*h;
		sum += 3 * f.eval(xi);
		sum += 3 * f.eval(xi+h);

		double coeff = h*3./8.;
		double finalVal = coeff*sum;

		return finalVal;

	}




	public static void main(String [] args){

		NumericFunction f = new NumericFunction() {
			@Override
			public double eval(double... args) {
				double x = args[0];
				//return 3.*x*x*x - 2.*x*x + 4.*x - 1.;
				return -x*x*x*x + 3*x*x + x + 1;
			}
		};

		double val = Simpsons38.integrate(f, -1.2, 0.6, 9);
		//double val = Simpsons38.integrate(f, 0, 10., 9);
		System.out.println(val);

	}
}
