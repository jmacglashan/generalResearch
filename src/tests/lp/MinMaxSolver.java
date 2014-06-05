package tests.lp;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.OptimizationRequest;

public class MinMaxSolver {

	public static void printMatrix(double [][] matrix){
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[i].length; j++){
				System.out.print(matrix[i][j] + " " );
			}
			System.out.println("");
		}
	}
	
	
	/**
	 * 
	 * @param payoffMatrix payoffs for the row player
	 * @return the strategy of the row player
	 */
	public static double [] getRowPlayersStrategy(double [][] payoffMatrix){
		double [][] t = transposeMatrix(payoffMatrix);
		return getColPlayersStrategy(t);
	}
	
	
	
	
	
	/**
	 * 
	 * @param payoffMatrix payoffs for column player
	 * @return strategy of the column player
	 */
	public static double [] getColPlayersStrategy(double [][] payoffMatrix){
		
		org.apache.log4j.BasicConfigurator.configure();
		LogManager.getRootLogger().setLevel(Level.OFF);
		
		
		//get positive matrix (finds the minimum value and adds -min + 1 to all elements)
		double [][] posMatrix = MinMaxSolver.getPositiveMatrix(payoffMatrix);
		
		//because we want to switch to <= that joptimizer wants, multiply by -1
		double [][] G = MinMaxSolver.getNegativeMatrix(posMatrix);
		
		//RHS of inequality is also inverted to -1s
		double [] h = constantDoubleArray(-1., G.length);

		//lower bound
		double [] lb = constantDoubleArray(0., G[0].length);
		
		//objective
		double [] c = constantDoubleArray(1., G[0].length);
		
		//optimization problem
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setC(c);
		or.setG(G);
		or.setH(h);
		or.setLb(lb);
		or.setDumpProblem(true); 
		
		//optimization
		LPPrimalDualMethod opt = new LPPrimalDualMethod();
		opt.setLPOptimizationRequest(or);
		
		
		try {
			opt.optimize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
		}
		
		
		
		return sol;
		
	}
	
	
	public static double [] constantDoubleArray(double constant, int dimension){
		double [] a = new double[dimension];
		for(int i = 0; i < dimension; i++){
			a[i] = constant;
		}
		return a;
	}
	
	
	
	/**
	 * 
	 * @param payoffMatrix payoffs for column player
	 * @return strategy of the column player
	 */
	public static double [] getColPlayersStrategyLPSolve(double [][] payoffMatrix){
		
		int dim = payoffMatrix[0].length;
		double [] objective = new double[dim];
		for(int i = 0; i < dim; i++){
			objective[i] = 1.;
		}
		
		double [][] positivePayoff = getPositiveMatrix(payoffMatrix);
		
		LinearProgram lp = new LinearProgram(objective);
		
		for(int i = 0; i < positivePayoff.length; i++){
			lp.addConstraint(new LinearBiggerThanEqualsConstraint(positivePayoff[i], 1.0, "c"+i));
		}
		
		for(int i = 0; i < dim; i++){
			double [] diagi = new double[dim];
			for(int j = 0; j < dim; j++){
				if(j != i){
					diagi[j] = 0.;
				}
				else{
					diagi[i] = 1.;
				}
			}
			lp.addConstraint(new LinearBiggerThanEqualsConstraint(diagi, 0.0, "c"+(i + positivePayoff.length)));
		}
		
		lp.setMinProblem(true); 
		LinearProgramSolver solver  = SolverFactory.newDefault(); 
		double[] sol = solver.solve(lp);
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
		}
		
		return sol;
	}
	
	
	public static double [] getColPlayersStrategyOld(double [][] payoffMatrix){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		int dim = payoffMatrix[0].length;
		
		System.out.println("Payoff");
		printMatrix(payoffMatrix);
		System.out.println("Postiive");
		double [][] G = getPositiveMatrix(payoffMatrix);
		printMatrix(G);
		double [] h = new double[dim];
		for(int i = 0; i < dim; i++){
			h[i] = -1;
		}
		
		//Bounds on variable
		double [] lb = new double[dim];
		for(int i = 0; i < dim; i++){
			lb[i] = 0.;
		}
		
		
		
		//Objective function
		double [] c = new double[dim];
		for(int i = 0; i < dim; i++){
			c[i] = 1;
		}

		
		//optimization problem
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setC(c);
		or.setG(G);
		or.setH(h);
		or.setLb(lb);
		//or.setUb(ub);
		or.setDumpProblem(true); 
		//or.setDumpProblem(false); 
		//or.setPresolvingDisabled(false);
		
		//optimization
		LPPrimalDualMethod opt = new LPPrimalDualMethod();
		//JOptimizer opt = new JOptimizer();
		opt.setLPOptimizationRequest(or);
		//opt.setOptimizationRequest(or);

		try {
			opt.optimize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
		}
		
		return sol;
		
	}
	
	public static double [] getColPlayersStrategyOtherForm(double [][] payoffMatrix){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		double [][] G = getPositiveMatrix(payoffMatrix);
		//double [] h = new double[]{1,1,1};
		double [] h = new double[]{-1,-1,-1};
		
		//Bounds on variable
		double [] lb = new double [] {0, 0, 0};
		//double [] ub = new double [] {1, 1, 1};
		
		//Objective function
		//double[] c = new double[] { -1., -1., -1.};
		double[] c = new double[] { 1., 1., 1.};
		
		
		// Objective function (plane)
		LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c, 0.);
		
		
		ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[G.length];
		for(int i = 0; i < inequalities.length; i++){
			//should be -h? why?
			inequalities[i] = new LinearMultivariateRealFunction(G[i], -h[i]);
		}
		
		//optimization problem
		
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setFi(inequalities);
		//or.setInitialPoint(new double[] {0.0, 0.0});//initial feasible point, not mandatory
		or.setToleranceFeas(1.E-9);
		or.setTolerance(1.E-9);
		
		
		/*
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setF0(objectiveFunction);
		or.setFi(inequalities);
		or.setLb(lb);
		//or.setInitialPoint(new double[] {0.0, 0.0});//initial feasible point, not mandatory
		or.setToleranceFeas(1.E-9);
		or.setTolerance(1.E-9);
		*/
		
		//optimization
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);
		
		try {
			opt.optimize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
		}
		
		return sol;
		
	}
	
	
	protected static double [][] getNegativeMatrix(double [][] m){
		double [][] m2 = new double[m.length][m[0].length];
		
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[i].length; j++){
				m2[i][j] = -m[i][j];
			}
		}
		
		return m2;
	}
	
	protected static double [][] getPositiveMatrix(double [][] m){
		double [][] m2 = new double[m.length][m[0].length];
		double min = Double.POSITIVE_INFINITY;
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[i].length; j++){
				if(m[i][j] < min){
					min = m[i][j];
				}
			}
		}
		
		if(min > 0.){
			return m.clone();
		}
		
		double add = -min + 1.;
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[i].length; j++){
				m2[i][j] = m[i][j] + add;
			}
		}
		
		return m2;
	}
	
	protected static double [][] transposeMatrix(double [][] m){
		double [][] m2 = new double[m[0].length][m.length];
		
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[i].length; j++){
				m2[j][i] = m[i][j];
			}
		}
		
		return m2;
	}
	
}
