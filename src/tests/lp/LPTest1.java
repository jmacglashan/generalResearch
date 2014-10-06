package tests.lp;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.constraints.LinearEqualsConstraint;
import scpsolver.constraints.LinearSmallerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.OptimizationRequest;


public class LPTest1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//org.apache.log4j.BasicConfigurator.configure();
		
		//joptExp();
		
		double[][] rps = new double[][] {
				{0., -1., 1.},
				{1., 0., -1.},
				{-1., 1., 0.}
		};
		
		
		//http://en.wikipedia.org/wiki/Zero-sum_game#Example
		double [][] example = new double[][]{
				{30., -10., 20.},
				{10, 20, -20}
				
		};
		
		double [][] exampleTransInvert = new double[][]{
				{-30., -10.},
				{10., -20.},
				{-20., 20}
		};
		
		
		//otherLibraryTest4();
		
		//hardCodedMatrixColPlayer();
		//hardCodedMatrixRowPlayer();
		
		//retest1();
		//printStrategies(rps);
		//printStrategies(example);
		//printStrategies(exampleTransInvert);
		
		//olCorrChicken();

		//feasibleTest();
		feasibleJOpt();
		
		
	}
	
	
	public static void printStrategies(double [][] payoffMatrix){
		double [] rowStrat = MinMaxSolver.getRowPlayersStrategy(payoffMatrix);
		double [] colStrat = MinMaxSolver.getColPlayersStrategy(MinMaxSolver.getNegativeMatrix(payoffMatrix));
		//double [] colStrat = MinMaxSolver.getColPlayersStrategyOtherForm(payoffMatrix);
		
		
		System.out.println("Row player's strategy:");
		for(double d : rowStrat){
			System.out.print(d + " ");
		}
		System.out.println("");
		
		System.out.println("Col player's strategy:");
		for(double d : colStrat){
			System.out.print(d + " ");
		}
		System.out.println("");
		
	}
	
	
	
	
	public static void forcedRPS(){
		
		//from http://www.slideshare.net/leingang/lesson-35-game-theory-and-linear-programming
		//NOTE: Payoff needs to be made positive; scale all by min+1 if necessary
		//Returns column player's streategy
		
		//Objective function
		double[] c = new double[] { -1., -1., -1.};
		
		//Inequalities
		double[][] G = new double[][] {
				{2., 1., 3.},
				{3., 2., 1.},
				{1., 3., 2.}
		};
		double [] h = new double[]{1,1,1};
		
		//Bounds on variable
		double [] lb = new double [] {0, 0, 0};
		double [] ub = new double [] {1, 1, 1};
		
		//optimization problem
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setC(c);
		or.setG(G);
		or.setH(h);
		or.setLb(lb);
		or.setUb(ub);
		or.setDumpProblem(true); 
		
		//optimization
		LPPrimalDualMethod opt = new LPPrimalDualMethod();
		opt.setLPOptimizationRequest(or);

		try {
			int returnCode = opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		double z = 0.;
		for(Double d : sol){
			System.out.println(d.doubleValue());
			z += d;
		}
		
		double v = 1/z;
		
		System.out.println("Policy:");
		for(Double d : sol){
			System.out.println((d*v));
		}
		
	}
	
	public static void simple1(){
		
		//http://www.sparknotes.com/math/algebra2/inequalities/section3.rhtml
		
		//Objective function
		double[] c = new double[] { -25., -30. };
		
		//Inequalities
		double[][] G = new double[][] {{1, 0},{0,1},{1,1},{-1, 1./3.}};
		double [] h = new double[]{5.,5.,8.,0.};
		
		//Bounds on variable
		double [] lb = new double [] {0, 0};
		double [] ub = new double [] {8, 8};
		
		//optimization problem
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setC(c);
		or.setG(G);
		or.setH(h);
		or.setLb(lb);
		or.setUb(ub);
		or.setDumpProblem(true); 
		
		//optimization
		LPPrimalDualMethod opt = new LPPrimalDualMethod();
		opt.setLPOptimizationRequest(or);

		try {
			int returnCode = opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		for(Double d : sol){
			System.out.println(d.doubleValue());
		}
		
	}
	
	public static void joptExp(){
		
		
		//Objective function
		double[] c = new double[] { -1., -1. };
		
		//Inequalities constraints
		double[][] G = new double[][] {{4./3., -1}, {-1./2., 1.}, {-2., -1.}, {1./3., 1.}};
		double[] h = new double[] {2., 1./2., 2., 1./2.};
		
		//Bounds on variables
		double[] lb = new double[] {0 , 0};
		double[] ub = new double[] {10, 10};
		
		//optimization problem
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setC(c);
		or.setG(G);
		or.setH(h);
		or.setLb(lb);
		or.setUb(ub);
		or.setDumpProblem(true); 
		
		//optimization
		LPPrimalDualMethod opt = new LPPrimalDualMethod();
		opt.setLPOptimizationRequest(or);

		try {
			int returnCode = opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		for(Double d : sol){
			System.out.println(d.doubleValue());
		}
		
	}
	
	
	public static void joptExp2(){
		
		
		// Objective function (plane)
		LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(new double[] { -1., -1. }, 4);

		//inequalities (polyhedral feasible set G.X<H )
		ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
		double[][] G = new double[][] {{4./3., -1}, {-1./2., 1.}, {-2., -1.}, {1./3., 1.}};
		double[] h = new double[] {2., 1./2., 2., 1./2.};
		inequalities[0] = new LinearMultivariateRealFunction(G[0], -h[0]);
		inequalities[1] = new LinearMultivariateRealFunction(G[1], -h[1]);
		inequalities[2] = new LinearMultivariateRealFunction(G[2], -h[2]);
		inequalities[3] = new LinearMultivariateRealFunction(G[3], -h[3]);
		
		//optimization problem
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setFi(inequalities);
		//or.setInitialPoint(new double[] {0.0, 0.0});//initial feasible point, not mandatory
		or.setToleranceFeas(1.E-9);
		or.setTolerance(1.E-9);
		
		//optimization
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);

		try {
			int returnCode = opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		for(Double d : sol){
			System.out.println(d.doubleValue());
		}
		
	}
	
	
	
	public static void otherLibraryTest1(){
		
		LinearProgram lp = new LinearProgram(new double[]{5.0,10.0}); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{3.0,1.0}, 8.0, "c1")); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.0,4.0}, 4.0, "c2")); 
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{2.0,0.0}, 2.0, "c3")); 
		lp.setMinProblem(true); 
		LinearProgramSolver solver  = SolverFactory.newDefault(); 
		double[] sol = solver.solve(lp);
		
		for(Double d : sol){
			System.out.println(d);
		}
		
	}
	
	public static void otherLibraryTest2(){
		
		
		//http://en.wikipedia.org/wiki/Zero-sum_game#Example
		double [][] example = new double[][]{
				{30., -10., 20.},
				{10, 20, -20}
				
		};
		
		
		LinearProgram lp = new LinearProgram(new double[]{1.0,1.0,1.0}); 
		
		double pc = 31.;
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{-30.+pc, 10.+pc, -20.+pc}, 1.0, "c1"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{-10+pc, -20+pc, 20+pc}, 1.0, "c2")); 
		
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{1.,0.,0.}, 0., "c3")); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.,1.,0.}, 0., "c4")); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.,0.,1.}, 0., "c5")); 

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
		
		for(Double d : sol){
			System.out.println(d);
		}
		
	}
	
	
	
	public static void otherLibraryTest3(){
		
		
		//http://en.wikipedia.org/wiki/Zero-sum_game#Example
		double [][] example = new double[][]{
				{30., -10., 20.},
				{10, 20, -20}
				
		};
		
		
		LinearProgram lp = new LinearProgram(new double[]{1.0,1.0}); 
		
		double pc = 21.;
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{30.+pc, 10.+pc}, 1.0, "c1"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{-10+pc, 20+pc}, 1.0, "c2"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{20+pc, -20+pc}, 1.0, "c3")); 
		
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{1.,0.}, 0., "c4")); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.,1.}, 0., "c5")); 

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
		
		for(Double d : sol){
			System.out.println(d);
		}
		
	}
	
	
	public static void otherLibraryTest4(){
		
		
		double[][] rps = new double[][] {
				{0., -1., 1.},
				{1., 0., -1.},
				{-1., 1., 0.}
		};
		
		
		LinearProgram lp = new LinearProgram(new double[]{1.0,1.0,1.0}); 
		
		double pc = 2.;
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.+pc, 1.+pc, -1.+pc}, 1.0, "c1"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{-1.+pc, 0.+pc, 1.+pc}, 1.0, "c2"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{1.+pc, -1.+pc, 0.+pc}, 1.0, "c3"));
	
		
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{1.,0.,0.}, 0., "c4")); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.,1.,0.}, 0., "c5")); 
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{0.,0.,1.}, 0., "c6")); 

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
		
		for(Double d : sol){
			System.out.println(d);
		}
		
	}
	
	
	public static void olCorrChicken(){
		
		LinearProgram lp = new LinearProgram(new double[]{12.0,9.0,9.0,0.0}); 
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{-1, 2, 0, 0}, 0.0, "c1"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 0, 1, -2}, 0.0, "c2"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{-1, 0, 2, 0}, 0.0, "c3"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 1, 0, -2}, 0.0, "c4"));
		
		lp.addConstraint(new LinearEqualsConstraint(new double[]{1., 1., 1., 1.}, 1., "c5"));
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{1, 0, 0, 0}, 0.0, "c6"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 1, 0, 0}, 0.0, "c7"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 0, 1, 0}, 0.0, "c8"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 0, 0, 1}, 0.0, "c9"));
		
		lp.setMinProblem(false);
		
		LinearProgramSolver solver  = SolverFactory.newDefault(); 
		double[] sol = solver.solve(lp);
		System.out.println(sol[0] + " " + sol[1] + "\n" + sol[2] + " " + sol[3]);
		
		
	}
	
	
	public static void retest1(){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		//http://en.wikipedia.org/wiki/Zero-sum_game#Example
		double [][] example = new double[][]{
				{30., -10., 20.},
				{10, 20, -20}
				
		};
		
		//first convert example into column player's payouts
		double [][] colPayouts = MinMaxSolver.getNegativeMatrix(example);
		
		//now get positive matrix (finds the minimum value and adds -min + 1 to all elements)
		double [][] posMatrix = MinMaxSolver.getPositiveMatrix(colPayouts);
		
		//because we want to switch to <= that joptimizer wants, multiply by -1
		double [][] G = MinMaxSolver.getNegativeMatrix(posMatrix);
		
		//RHS of inequality is also inverted to -1s
		double [] h = new double[]{-1., -1., -1.};
		
		//lower bound
		double [] lb = new double[]{0.,0.,0.};
		
		//objective
		double [] c = new double[]{1., 1., 1.};
		
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
			System.out.println(sol[i]);
		}
		
		
	}
	
	public static void retest2(){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		//http://en.wikipedia.org/wiki/Zero-sum_game#Example
		double [][] example = new double[][]{
				{30., -10., 20.},
				{10, 20, -20}
				
		};
		
		//first convert into row player being column player
		double [][] toCol = MinMaxSolver.transposeMatrix(example);
		
		//now get positive matrix (finds the minimum value and adds -min + 1 to all elements)
		double [][] posMatrix = MinMaxSolver.getPositiveMatrix(toCol);
		
		//because we want to switch to <= that joptimizer wants, multiply by -1
		double [][] G = MinMaxSolver.getNegativeMatrix(posMatrix);
		
		
		//RHS of inequality is also inverted to -1s
		double [] h = new double[]{-1., -1.};
		
		//lower bound
		double [] lb = new double[]{0.,0.};
		
		//objective
		double [] c = new double[]{1., 1.};
		
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
		
		//convert to probability vector
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
			System.out.println(sol[i]);
		}
		
	}
	
	
	
	
	
	
	public static void retest3(){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		double[][] rps = new double[][] {
				{0., -1., 1.},
				{1., 0., -1.},
				{-1., 1., 0.}
		};
		
		//first convert example into column player's payouts
		double [][] colPayouts = MinMaxSolver.getNegativeMatrix(rps);
		
		//now get positive matrix
		double [][] posMatrix = MinMaxSolver.getPositiveMatrix(colPayouts);
		
		//because we want to switch to <= that joptimizer wants, multiply by -1
		double [][] G = MinMaxSolver.getNegativeMatrix(posMatrix);
		
		//RHS of inequality is also inverted to -1s
		double [] h = new double[]{-1., -1., -1.};
		
		//lower bound
		double [] lb = new double[]{0.,0.,0.};
		
		//objective
		double [] c = new double[]{1., 1., 1.};
		
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
			System.out.println(sol[i]);
		}
		
		
	}
	
	
	public static void retest4(){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		double[][] rps = new double[][] {
				{0., -1., 1.},
				{1., 0., -1.},
				{-1., 1., 0.}
		};
		
		
		//first make row player the columns player
		double [][] toCol = MinMaxSolver.transposeMatrix(rps);
		
		//now get positive matrix
		double [][] posMatrix = MinMaxSolver.getPositiveMatrix(toCol);
		
		//because we want to switch to <= that joptimizer wants, multiply by -1
		double [][] G = MinMaxSolver.getNegativeMatrix(posMatrix);
		
		//RHS of inequality is also inverted to -1s
		double [] h = new double[]{-1., -1., -1.};
		
		//lower bound
		double [] lb = new double[]{0.,0.,0.};
		
		//objective
		double [] c = new double[]{1., 1., 1.};
		
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
			System.out.println(sol[i]);
		}
		
		
	}
	
	
	
	
	public static void hardCodedMatrixColPlayer(){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		/*http://en.wikipedia.org/wiki/Zero-sum_game#Example
		row player payouts are:
		30, -10, 20
		10, 20, -20
		---------------
		col player payouts are:
		-30, 10, -20
		-10, -20, 20
		*/
		
		
		//setup G matrix with col player payouts with a constant +31 added to make the matrix positive, and then
		//multiply by -1 to switch the inequality > to the inequality < that joptimizer expects
		
		double [][] G = new double[][]{
				{-1., -41., -11.},
				{-21., -11., -51.}
				
		};
		
		
		//RHS of inequality is also inverted to -1s
		double [] h = new double[]{-1., -1., -1.};
		
		//lower bound
		double [] lb = new double[]{0.,0.,0.};
		
		//objective
		double [] c = new double[]{1., 1., 1.};
		
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
		
		//convert the solution back into a probability vector
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
			System.out.println(sol[i]);
		}
		
		
	}
	
	
	public static void hardCodedMatrixRowPlayer(){
		
		org.apache.log4j.BasicConfigurator.configure();
		
		/*http://en.wikipedia.org/wiki/Zero-sum_game#Example
		row player payouts are:
		30, -10, 20
		10, 20, -20
		---------------
		col player payouts are:
		-30, 10, -20
		-10, -20, 20
		*/
		
		
		//setup G matrix with row player payouts with a constant +21 added to make the matrix positive, transpose it
		//to swap the row player to be the col player and then
		//multiply by -1 to switch the inequality > to the inequality < that joptimizer expects
		
		
		double [][] G = new double[][]{
				{-51., -31.},
				{-11., -41.},
				{-41., -1.}
				
		};
		
		
		//RHS of inequality is also negated to -1s
		double [] h = new double[]{-1., -1., -1.};
		
		//lower bound
		double [] lb = new double[]{0.,0.};
		
		//objective
		double [] c = new double[]{1., 1.};
		
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
		
		//convert the solution back into a probability vector
		double z = 0.;
		for(Double d : sol){
			z += d;
		}
		
		double v = 1/z;
		
		for(int i = 0; i < sol.length; i++){
			sol[i] *= v;
			System.out.println(sol[i]);
		}
		
		
	}
	
	
	public static void feasibleTest(){
		
		/*
		LinearProgram lp = new LinearProgram(new double[]{12.0,9.0,9.0,0.0}); 
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{-1, 2, 0, 0}, 0.0, "c1"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 0, 1, -2}, 0.0, "c2"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{-1, 0, 2, 0}, 0.0, "c3"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 1, 0, -2}, 0.0, "c4"));
		
		lp.addConstraint(new LinearEqualsConstraint(new double[]{1., 1., 1., 1.}, 1., "c5"));
		
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{1, 0, 0, 0}, 0.0, "c6"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 1, 0, 0}, 0.0, "c7"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 0, 1, 0}, 0.0, "c8"));
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double []{0, 0, 0, 1}, 0.0, "c9"));
		
		lp.setMinProblem(false);
		
		LinearProgramSolver solver  = SolverFactory.newDefault(); 
		double[] sol = solver.solve(lp);
		System.out.println(sol[0] + " " + sol[1] + "\n" + sol[2] + " " + sol[3]);
		*/
		
		
		
		LinearProgram lp = new LinearProgram(new double[]{-0.0, -48.5, 1.0, 1.0, 1.0, -95.03, -46.015, -97.0, 2.0, -94.03, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0});
		
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{47.015, -26.4925, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c1"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c2"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c3"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c4"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, 26.4925, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c5"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c6"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c7"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c8"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c9"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c10"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c11"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c12"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0}, 0., "c13"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015}, 0., "c14"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-1.0, 0.0, 0.0, 0.0, 0.0, -25.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c15"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-1.0, 0.0, 0.0, 0.0, 0.0, 49.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c16"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-1.0, 0.0, 0.0, 0.0, 0.0, -49.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c17"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{-1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 0., "c18"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 25.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 0., "c19"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 74.9925, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 0., "c20"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 0., "c21"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 0., "c22"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -49.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 0., "c23"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -74.9925, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 0., "c24"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -99.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 0., "c25"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -50.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 0., "c26"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 49.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 0., "c27"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 0., "c28"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 99.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 0., "c29"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 48.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 0., "c30"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 0., "c31"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 0., "c32"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 50.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 0., "c33"));
		lp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[]{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -48.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 0., "c34"));
		
		
		lp.addConstraint(new LinearEqualsConstraint(constantDoubleArray(1., 25), 1., "c35"));
		
		//also do lower bound
		for(int i = 0; i < 25; i++){
			lp.addConstraint(new LinearBiggerThanEqualsConstraint(zero1Array(i, 25), 0, "c" + (36+i)));
		}
		
		LinearProgramSolver solver  = SolverFactory.newDefault(); 
		double[] sol = solver.solve(lp);
		
		for(int i = 0; i < sol.length; i++){
			System.out.println(i + " " + sol[i]);
		}
		
	}
	
	public static void feasibleJOpt(){
		
		org.apache.log4j.BasicConfigurator.configure();
		LogManager.getRootLogger().setLevel(Level.OFF);
		
		double [] c = new double[]{-0.0, -48.5, 1.0, 1.0, 1.0, -95.03, -46.015, -97.0, 2.0, -94.03, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0};
		
		double [][] A = new double [][]{{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}};
		double [] b = new double[]{1.};
		
		//double [] lb = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		double [] lb = constantDoubleArray(-1e-5, 25);
		
		double [][] G = new double[][]{
				{47.015, -26.4925, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //0
				{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //1
				{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //2
				{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //3
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, 26.4925, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //4 
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //5
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //6
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //7
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //8
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //9 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, -25.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, 49.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, -49.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 25.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 74.9925, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -49.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -74.9925, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -99.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -50.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 49.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 99.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 48.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 50.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -48.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}};
		
		double [] h = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		//double [] h = constantDoubleArray(0.00001, G.length);
		
		
		//double [] proposedSold = new double []{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		double [] proposedSold = constantDoubleArray(0., c.length);
		proposedSold[12] = 1.;
		double [] sat = testSolOnConstraints(G, proposedSold);
		for(int i = 0; i < sat.length; i++){
			System.out.println(i + " " + sat[i]);
		}
		System.out.println("------");
		for(int i = 0; i < G.length; i++){
			if(G[i][12] != 0){
				System.out.println("Non zero on inequality " + i + "; " + G[i][12]);
			}
		}
		
		
		//optimization problem
		LPOptimizationRequest or = new LPOptimizationRequest();
		or.setC(c);
		or.setG(G);
		or.setH(h);
		or.setLb(lb);
		or.setA(A);
		or.setB(b);
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
		double [] rnded = roundNegativesToZero(sol);
		for(int i = 0; i < sol.length; i++){
			System.out.print(sol[i] + " ");
		}
		System.out.println("\n---");
		double sum = 0.;
		for(int i = 0; i < sol.length; i++){
			System.out.print(rnded[i] + " ");
			sum += rnded[i];
		}
		System.out.println("\n" + sum + "\n+++");
		
		double [] sat2 = testSolOnConstraints(G, rnded);
		for(int i = 0; i < sat.length; i++){
			System.out.println(sat2[i]);
		}
		
	}
	
	
	public static void feasibleJOpt2(){
		
		org.apache.log4j.BasicConfigurator.configure();
		LogManager.getRootLogger().setLevel(Level.OFF);
		
		double [] c = new double[]{-0.0, -48.5, 1.0, 1.0, 1.0, -95.03, -46.015, -97.0, 2.0, -94.03, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0, -0.0};
		
		double [][] A = new double [][]{{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}};
		double [] b = new double[]{1.};
		
		double [] lb = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		//double [] lb = constantDoubleArray(0.0, 25);
		
		double [][] G = new double[][]{
				{47.015, -26.4925, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-0.0, -49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, 26.4925, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, -47.015, -23.0075, 1.0, 1.0, -47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 49.5, -0.0, -0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 47.015, 23.0075, -1.0, -1.0, 47.015}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0}, 
				//{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0, -0.0, -0.0, -0.0, -0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, -25.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, 49.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, -49.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{-1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0}, 
				{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 25.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 74.9925, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0}, 
				{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -49.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -74.9925, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -99.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -50.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0}, 
				{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 49.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 99.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 48.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0}, 
				{0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -24.0075, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 50.985, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}, 
				{0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -48.015, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0}};
		
		double [] h = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		
		
		//optimization problem
		LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c, 0.);
		ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[G.length+c.length];
		for(int i = 0; i < G.length; i++){
			inequalities[i] = new LinearMultivariateRealFunction(G[i], -h[i]);
		}
		for(int i = 0; i < c.length; i++){
			inequalities[i+G.length] = new LinearMultivariateRealFunction(zero1Array(i, c.length), 0.);
		}

		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setFi(inequalities);
		or.setA(A);
		or.setB(b);
		
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);
		
		try {
			opt.optimize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		for(int i = 0; i < sol.length; i++){
			System.out.print(sol[i] + " ");
		}
		
		
	}
	
	
	public static double [] constantDoubleArray(double constant, int dimension){
		double [] a = new double[dimension];
		for(int i = 0; i < dimension; i++){
			a[i] = constant;
		}
		return a;
	}
	
	public static double [] zero1Array(int index, int dim){
		double [] a = new double[dim];
		for(int i = 0; i < dim; i++){
			if(i != index){
				a[i] = 0.;
			}
			else{
				a[i] = 1.;
			}
		}
		return a;
	}
	
	public static double [] zeroNeg1Array(int index, int dim){
		double [] a = new double[dim];
		for(int i = 0; i < dim; i++){
			if(i != index){
				a[i] = 0.;
			}
			else{
				a[i] = -1.;
			}
		}
		return a;
	}
	
	public static double [] testSolOnConstraints(double [][] G, double [] sol){
		
		double [] res = new double[G.length];
		
		for(int i = 0; i < G.length; i++){
			res[i] = dot(G[i], sol);
		}
		
		
		return res;
		
	}
	
	public static double dot(double [] a, double [] b){
		double sum = 0.;
		for(int i = 0; i < a.length; i++){
			sum += a[i] * b[i];
		}
		return sum;
	}
	
	public static double [] roundNegativesToZero(double [] a){
		double [] b = new double[a.length];
		for(int i = 0; i < a.length; i++){
			if(a[i] > 0){
				b[i] = a[i];
			}
			else{
				b[i] = 0.;
			}
		}
		return b;
	}
	

}
