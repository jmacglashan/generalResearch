package optimization.crossentropy;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.RandomGenerator;


import burlap.debugtools.RandomFactory;
import optimization.OptVariables;
import optimization.VariableRandomGenerator;

public class MultiVariateNormalGenerator extends VariableRandomGenerator {

	
	protected double[]										means;
	protected double[][]									covariance;
	
	protected MultivariateNormalDistribution				generator;
	
	
	
	
	public static void main(String [] args){
		
		double [] means = new double[]{0.09275966552324925, 0.15234659238387943};
		//double [] means = new double[]{10., 30.};
		//double [][]covariance = new double[][]{{0.17142487993031433, -0.1372348892873097}, {-0.1372348892873097, 0.1419595425891839}};
		double [][]covariance = new double[][]{{0.1714249, -0.1372349}, {-0.1372349, 0.1419595}};
		//double [][]covariance = new double[][]{{170.142487993031433, 0.}, {0., 0.01419595425891839}};
		
		
		MultivariateNormalDistribution mvng = new MultivariateNormalDistribution(new RGWrapper(0), means, covariance);
		
		double [] samp = mvng.sample();
		OptVariables v = new OptVariables(samp);
		System.out.println(v.toString());
		
		
	}
	
	
	public MultiVariateNormalGenerator(double [] means, double [][] covariance){
		
		
		this.means = means.clone();
		this.covariance = covariance.clone();
		
		this.makeZeroVarianceSmallValue();
		
		double det = this.determinantOfCov();
		while(det == 0.){
			this.permuteSingularMatrix();
			det = this.determinantOfCov();
		}
		
		generator = new MultivariateNormalDistribution(new RGWrapper(0), this.means, this.covariance);
		
		
	}
	
	
	public void setParams(double [] means, double [][] covariance){
		this.means = means.clone();
		this.covariance = covariance.clone();
		
		this.makeZeroVarianceSmallValue();
		
		double det = this.determinantOfCov();
		while(det == 0.){
			this.permuteSingularMatrix();
			det = this.determinantOfCov();
		}
		
		generator = new MultivariateNormalDistribution(new RGWrapper(0), this.means, this.covariance);
	}
	
	
	@Override
	//this method should not be called in isolation for an entire vector result because it would remove variable dependence
	public double valueForVar(int i) {
		OptVariables sample = this.getVars(means.length);
		return sample.v(i);
	}
	
	
	@Override
	public OptVariables getVars(int m){
		
		OptVariables var = new OptVariables(generator.sample());
		return var;
	}
	
	
	
	protected void makeZeroVarianceSmallValue(){
		double small = 1e-10;
		
		for(int i = 0; i < this.covariance.length; i++){
			if(this.covariance[i][i] == 0.){
				this.covariance[i][i] = small;
			}
		}
		
	}
	
	
	protected double determinantOfCov(){
		
		RealMatrix rmc = MatrixUtils.createRealMatrix(this.covariance);
		EigenDecomposition eg = new EigenDecomposition(rmc);
		
		return eg.getDeterminant();
	}
	
	protected void permuteSingularMatrix(){
		
		int n = this.covariance.length;
		
		double small = 1e-5;
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				if(i != j){
					this.covariance[i][j] = 0.;
				}
				else{
					this.covariance[i][j] += RandomFactory.getMapped(0).nextDouble()*small*2 - small;
				}
			}
		}
		
	}
	
	
	protected static class RGWrapper implements RandomGenerator{
		
		public int code;
		
		public RGWrapper(int c){
			code = c;
		}

		@Override
		public boolean nextBoolean() {
			return RandomFactory.getMapped(code).nextBoolean();
		}

		@Override
		public void nextBytes(byte[] arg0) {
			RandomFactory.getMapped(code).nextBytes(arg0);
		}

		@Override
		public double nextDouble() {
			return RandomFactory.getMapped(code).nextDouble();
		}

		@Override
		public float nextFloat() {
			return RandomFactory.getMapped(code).nextFloat();
		}

		@Override
		public double nextGaussian() {
			return RandomFactory.getMapped(code).nextGaussian();
		}

		@Override
		public int nextInt() {
			return RandomFactory.getMapped(code).nextInt();
		}

		@Override
		public int nextInt(int arg0) {
			return RandomFactory.getMapped(code).nextInt(arg0);
		}

		@Override
		public long nextLong() {
			return RandomFactory.getMapped(code).nextLong();
		}

		@Override
		public void setSeed(int arg0) {
			RandomFactory.getMapped(code).setSeed(arg0);
		}

		@Override
		public void setSeed(int[] arg0) {
			RandomFactory.getMapped(code).setSeed(arg0[0]);
		}

		@Override
		public void setSeed(long arg0) {
			RandomFactory.getMapped(code).setSeed(arg0);
		}
		
		
	}

}
