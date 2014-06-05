package ethics.experiments.tbforagesteal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import optimization.OVarStringRep;
import optimization.OptVariables;
import optimization.PopulationOptimization;
import optimization.VarEvaluaiton;
import optimization.VarFitnessPair;
import optimization.VariableClamper;
import optimization.VariableRandomGenerator;
import optimization.geneticalgorithm.GeneticAlgorithm;
import optimization.geneticalgorithm.gamodules.FatherDuplicationMater;
import optimization.geneticalgorithm.gamodules.KillWorst;
import optimization.geneticalgorithm.gamodules.RandomKiller;
import optimization.geneticalgorithm.gamodules.RandomOrganismMutate;
import optimization.geneticalgorithm.gamodules.SoftMaxSingleParentMateSelector;
import optimization.geneticalgorithm.gamodules.TopInstanceMateSelector;
import optimization.infinitega.InfiniteGA;
import optimization.infinitega.modules.InfGARepDoubleWithUniMutate;
import optimization.infinitega.modules.InfGASoftMaxReproduce;
import optimization.infinitega.modules.RatioKillWorst;
import optimization.optmodules.ContinuousBoundedVarGen;
import optimization.optmodules.DiscStepVarClamp;
import burlap.debugtools.DPrint;
import burlap.oomdp.stochasticgames.JointReward;
import domain.stocasticgames.foragesteal.TBFSStandardReward;
import ethics.ParameterizedRF;
import ethics.ParameterizedRFFactory;
import ethics.experiments.tbforagesteal.aux.OVarSRStringRep;
import ethics.experiments.tbforagesteal.aux.RFParamVarEnumerator;
import ethics.experiments.tbforagesteal.aux.TBFSSubRFFactory;
import ethics.experiments.tbforagesteal.evaluators.FullyCachedMatchEvaluation;
import ethics.experiments.tbforagesteal.evaluators.InfGACachedVarEval;
import ethics.experiments.tbforagesteal.evaluators.LazyRoundRobinEval;

public class TBFSOptimizerExp {


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//testRandomGenerator();
		
		//optimiztion(args);
		//optimiztionFullGenome(args);
		//controlledGATest();
		//optimiztionInfiniteGA(args);
		optimiztionInfiniteGASoftMax(args);
		
		
	}
	
	
	public static void controlledGATest(){
		
		List <VarRatio> ratios = new ArrayList<TBFSOptimizerExp.VarRatio>();
		
		
		ratios.add(new VarRatio(new OptVariables(new double[]{0.,1.5,0.}), 0.5));
		ratios.add(new VarRatio(new OptVariables(new double[]{0.,2.,2.}), 0.5));
		
		
		/*
		ratios.add(new VarRatio(new OptVariables(new double[]{0.,1.5,0.}), 1./3.));
		ratios.add(new VarRatio(new OptVariables(new double[]{0.,0.,0.}), 1./3.));
		ratios.add(new VarRatio(new OptVariables(new double[]{0.,2.,2.}), 1./3.));
		*/
		
		List <OptVariables> seedPop = VarRatio.getPopulation(ratios, 100);
		
		runControlledGA(seedPop, 100, "ethics/Results/cacheRes/cachelr5.txt", "ethics/Results/cga/r0150-022.txt");
		
	}
	
	public static void optimiztion(String [] args){
		
		if(args.length != 4){
			System.out.println("Error: Incorrect format");
			System.out.println("\tpopulationSize nGAGenerations cacheFileInput outputDirectory");
			System.exit(-1);
		}
		
		int nPop = Integer.parseInt(args[0]);
		int nGAGen = Integer.parseInt(args[1]);
		//double lr = Double.parseDouble(args[2]);
		String cacheFileInput = args[2];
		String outputDir = args[3];
		
		//testEvaluator();
		//runOptimizer(nPop, nGAGen, lr, outputDir);
		runFullyCachedOptimizer(nPop, nGAGen, cacheFileInput, outputDir);
	}
	
	public static void optimiztionFullGenome(String [] args){
		
		if(args.length != 5){
			System.out.println("Error: Incorrect format");
			System.out.println("\tnGAGenerations softMax mutaiton cacheFileInput outputFilePath");
			System.exit(-1);
		}
		
		
		int nGAGen = Integer.parseInt(args[0]);
		double softMax = Double.parseDouble(args[1]);
		double mutation = Double.parseDouble(args[2]);
		String cacheFileInput = args[3];
		String outputDir = args[4];
		
		runFullyCachedFullGenomeOptimizer(nGAGen, softMax, mutation, cacheFileInput, outputDir);
		
		//testEvaluator();
		//runOptimizer(nPop, nGAGen, lr, outputDir);
		//runFullyCachedOptimizer(nPop, nGAGen, cacheFileInput, outputDir);
	}
	
	
	public static void optimiztionInfiniteGA(String [] args){
		
		if(args.length != 5){
			System.out.println("Error: Incorrect format");
			System.out.println("\tnGAGenerations selectSize mutaiton cacheFileInput outputFilePath");
			System.exit(-1);
		}
		
		
		int nGAGen = Integer.parseInt(args[0]);
		double selectSize = Double.parseDouble(args[1]);
		double mutation = Double.parseDouble(args[2]);
		String cacheFileInput = args[3];
		String outputPath = args[4];
		
		runInfinteGA(nGAGen, selectSize, mutation, cacheFileInput, outputPath);
		
		
	}
	
	public static void optimiztionInfiniteGASoftMax(String [] args){
		
		if(args.length != 4){
			System.out.println("Error: Incorrect format");
			System.out.println("\tnGAGenerations temperature cacheFileInput outputFilePath");
			System.exit(-1);
		}
		
		DPrint.toggleCode(InfGASoftMaxReproduce.debugCode, false);
		
		int nGAGen = Integer.parseInt(args[0]);
		double temperature = Double.parseDouble(args[1]);
		String cacheFileInput = args[2];
		String outputPath = args[3];
		
		runInfinteGASoftMax(nGAGen, temperature, cacheFileInput, outputPath);
		
		
	}
	
	
	public static void testRandomGenerator(){
		
		double lval = -2.0;
		double hval = 2.0;
		
		int pn = 1;
		
		double [] low = new double[pn];
		double [] high = new double[pn];
		
		for(int i = 0; i < pn; i++){
			low[i] = lval;
			high[i] = hval;
		}
		
		VariableClamper clamp = new DiscStepVarClamp(low, high, 0.5);
		VariableRandomGenerator varGen = new ContinuousBoundedVarGen(low, high);
		
		for(int i = 0; i < 100; i++){
			OptVariables var = varGen.getVars(pn);
			clamp.clamp(var);
			System.out.println(var.toString());
		}
		
	}
	
	
	public static void testEvaluator(){
		
		DPrint.toggleCode(284673923, false); //world printing debug code
		DPrint.toggleCode(25633, false); //tournament printing debug code
		
		
		JointReward objectiveRewardFunction = new TBFSStandardReward();
		ParameterizedRFFactory subrff = new TBFSSubRFFactory(objectiveRewardFunction);
		
		double lval = -2.;
		double hval = 2.;
		
		int pn = subrff.parameterSize();
		
		double [] low = new double[pn];
		double [] high = new double[pn];
		
		for(int i = 0; i < pn; i++){
			low[i] = lval;
			high[i] = hval;
		}
		
		VariableClamper clamp = new DiscStepVarClamp(low, high, 0.25);
		VariableRandomGenerator varGen = new ContinuousBoundedVarGen(low, high);
		VarEvaluaiton eval = new LazyRoundRobinEval(100, 1000, subrff, objectiveRewardFunction, 0.1);
		
		List <OptVariables> tests = new ArrayList<OptVariables>(2);
		for(int i = 0; i < 2; i++){
			OptVariables v = varGen.getVars(pn);
			clamp.clamp(v);
			tests.add(v);
		}
		
		System.out.println("Starting!");
		
		List <Double> fitness = eval.evaluate(tests);
		
		for(int i = 0; i < tests.size(); i++){
			System.out.println("Fitness : " + fitness.get(i));
			subrff.generateRF(tests.get(i).vars).printParameters();
			System.out.println("+++++++++++++++++++++++++");
		}
		
		System.out.println("Running again!");
		
		fitness = eval.evaluate(tests);
		
		for(int i = 0; i < tests.size(); i++){
			System.out.println("Fitness : " + fitness.get(i));
			subrff.generateRF(tests.get(i).vars).printParameters();
			System.out.println("+++++++++++++++++++++++++");
		}
		
		
	}
	
	
	
	public static void runInfinteGA(int nGenerations, double selectSize, double mutation, String cacheFilePath, String outputPath){
		
		System.out.println("Parsing CacheFile");
		InfGACachedVarEval eval = new InfGACachedVarEval(cacheFilePath);
		System.out.println("Finished Parsing CacheFile and starting GA");
		
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator();
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator(-1, 2, 1., 3);
		RFParamVarEnumerator rfenum = new RFParamVarEnumerator(-1.5, 2.5, 0.5, 2);
		
		InfiniteGA ga = new InfiniteGA(eval, new InfGARepDoubleWithUniMutate(selectSize, mutation), new RatioKillWorst(), rfenum.allRFs, nGenerations);
		eval.setInfGA(ga);
		
		OVarStringRep rep = new OVarStringRep() {
			
			@Override
			public String getStringRep(OptVariables vars) {
				return vars.toString();
			}
		};
		ga.enableOptimzationFileRecording(2, rep, outputPath);

		ga.optimize();
		
		System.out.println("Finished\n-----------------------------");
		
		
	}
	
	
	public static void runInfinteGASoftMax(int nGenerations, double temperature, String cacheFilePath, String outputPath){
		
		System.out.println("Parsing CacheFile");
		InfGACachedVarEval eval = new InfGACachedVarEval(cacheFilePath);
		System.out.println("Finished Parsing CacheFile and starting GA");
		
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator();
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator(-1, 2, 1., 3);
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator(-1.5, 2.5, 0.5, 2);
		//RFParamVarEnumerator rfenum = new RFParamVarEnumerator(0, 1, 1, 5);
		RFParamVarEnumerator rfenum = new RFParamVarEnumerator(0, 1, 1, 7);
		
		
		//List<OptVariables> selectHCAgents = rfenum.allRFs;
		//selectHCAgents.remove(new OptVariables(new double[]{1,0,1,1,1}));
		
		
		/*
		List<OptVariables> selectHCAgents = new ArrayList<OptVariables>(9);
		selectHCAgents.add(new OptVariables(new double[]{0,0,0,0,0}));
		selectHCAgents.add(new OptVariables(new double[]{0,0,0,1,1}));
		selectHCAgents.add(new OptVariables(new double[]{0,0,0,1,0}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,1,0,0}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,1,1,1}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,1,1,0}));
		selectHCAgents.add(new OptVariables(new double[]{1,0,1,0,0}));
		selectHCAgents.add(new OptVariables(new double[]{1,0,1,1,1}));
		selectHCAgents.add(new OptVariables(new double[]{1,0,1,1,0}));
		
		
		selectHCAgents.add(new OptVariables(new double[]{0,0,0,0,1}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,1,0,1}));
		selectHCAgents.add(new OptVariables(new double[]{1,0,1,0,1}));
		
		
		selectHCAgents.add(new OptVariables(new double[]{1,1,0,0,0}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,0,1,1}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,0,1,0}));
		selectHCAgents.add(new OptVariables(new double[]{1,1,0,0,1}));
		*/
		
		InfiniteGA ga = new InfiniteGA(eval, new InfGASoftMaxReproduce(temperature), new RatioKillWorst(), rfenum.allRFs, nGenerations);
		//InfiniteGA ga = new InfiniteGA(eval, new InfGASoftMaxReproduce(temperature), new RatioKillWorst(), selectHCAgents, nGenerations);
		eval.setInfGA(ga);
		
		OVarStringRep rep = new OVarStringRep() {
			
			@Override
			public String getStringRep(OptVariables vars) {
				return vars.toString();
			}
		};
		ga.enableOptimzationFileRecording(2, rep, outputPath);

		ga.optimize();
		
		System.out.println("Finished\n-----------------------------");
		
		
	}
	
	
	public static void runControlledGA(List <OptVariables> seedPop, int nGenerations, String cacheFilePath, String outputPathDirectory){
		
		JointReward objectiveRewardFunction = new TBFSStandardReward();
		ParameterizedRFFactory subrff = new TBFSSubRFFactory(objectiveRewardFunction);
		
		double lval = -1.5;
		double hval = 2.5;
		
		int pn = subrff.parameterSize();
		
		double [] low = new double[pn];
		double [] high = new double[pn];
		
		for(int i = 0; i < pn; i++){
			low[i] = lval;
			high[i] = hval;
		}
		
		VariableClamper clamp = new DiscStepVarClamp(low, high, 0.5);
		VariableRandomGenerator varGen = new ContinuousBoundedVarGen(low, high);
		
		
		System.out.println("Parsing CacheFile");
		VarEvaluaiton eval = new FullyCachedMatchEvaluation(cacheFilePath);
		System.out.println("Finished Parsing CacheFile and starting GA");
		
		
		GeneticAlgorithm ga = new GeneticAlgorithm(eval, varGen, clamp, new KillWorst(), new TopInstanceMateSelector(), new FatherDuplicationMater(), 
				new RandomOrganismMutate(varGen), nGenerations, 1, 1, 0.0, seedPop.size(), pn);
		
		ga.setInitialPopulation(seedPop);
		
		OVarStringRep rep = new OVarStringRep() {
			
			@Override
			public String getStringRep(OptVariables vars) {
				return vars.toString();
			}
		};
		ga.enableOptimzationFileRecording(2, rep, outputPathDirectory);

		ga.optimize();
		
		System.out.println("Finished\n-----------------------------");
		
		
		/*
		List <VarFitnessPair> population = ga.getPopulation();
		Collections.sort(population, Collections.reverseOrder());
		for(VarFitnessPair vfp : population){
		
			System.out.println("Fitness Evaluation: " + vfp.fitness);
			ParameterizedRF bestR = subrff.generateRF(vfp.var.vars);
			bestR.printParameters();
			
			System.out.println("+++++++++++++++++++++++++");
			
			
			
		}
		*/
	}
	
	
	public static void runFullyCachedOptimizer(int nPop, int nGenerations, String cacheFilePath, String outputPathDirectory){
		
		JointReward objectiveRewardFunction = new TBFSStandardReward();
		ParameterizedRFFactory subrff = new TBFSSubRFFactory(objectiveRewardFunction);
		
		double lval = -1.5;
		double hval = 2.5;
		
		int pn = subrff.parameterSize();
		
		double [] low = new double[pn];
		double [] high = new double[pn];
		
		for(int i = 0; i < pn; i++){
			low[i] = lval;
			high[i] = hval;
		}
		
		VariableClamper clamp = new DiscStepVarClamp(low, high, 0.5);
		VariableRandomGenerator varGen = new ContinuousBoundedVarGen(low, high);
		
		System.out.println("Parsing CacheFile");
		VarEvaluaiton eval = new FullyCachedMatchEvaluation(cacheFilePath);
		System.out.println("Finished Parsing CacheFile and starting GA");
		
		PopulationOptimization opt = new GeneticAlgorithm(eval, varGen, clamp, new RandomKiller(), 
				new SoftMaxSingleParentMateSelector(.05), new FatherDuplicationMater(), new RandomOrganismMutate(varGen), nGenerations, 1, 1, 0.1, nPop, pn);
		
		OVarStringRep rep = new OVarStringRep() {
			
			@Override
			public String getStringRep(OptVariables vars) {
				return vars.toString();
			}
		};
		
		opt.enableOptimzationFileRecording(2, rep, outputPathDirectory);

		opt.optimize();
		
		System.out.println("Finished\n-----------------------------");
		
		List <VarFitnessPair> population = opt.getPopulation();
		Collections.sort(population, Collections.reverseOrder());
		for(VarFitnessPair vfp : population){
		
			System.out.println("Fitness Evaluation: " + vfp.fitness);
			ParameterizedRF bestR = subrff.generateRF(vfp.var.vars);
			bestR.printParameters();
			
			System.out.println("+++++++++++++++++++++++++");
			
			
			
		}
		
	}
	
	
	public static void runFullyCachedFullGenomeOptimizer(int nGenerations, double softMaxTemp, double mutation, String cacheFilePath, String outputFilePath){
		
		JointReward objectiveRewardFunction = new TBFSStandardReward();
		ParameterizedRFFactory subrff = new TBFSSubRFFactory(objectiveRewardFunction);
		
		double lval = -1.5;
		double hval = 2.5;
		
		int pn = subrff.parameterSize();
		
		double [] low = new double[pn];
		double [] high = new double[pn];
		
		for(int i = 0; i < pn; i++){
			low[i] = lval;
			high[i] = hval;
		}
		
		VariableClamper clamp = new DiscStepVarClamp(low, high, 0.5);
		VariableRandomGenerator varGen = new ContinuousBoundedVarGen(low, high);
		
		System.out.println("Parsing CacheFile");
		VarEvaluaiton eval = new FullyCachedMatchEvaluation(cacheFilePath);
		System.out.println("Finished Parsing CacheFile and starting GA");
		
		RFParamVarEnumerator enumerator = new RFParamVarEnumerator();
		
		
		GeneticAlgorithm opt = new GeneticAlgorithm(eval, varGen, clamp, new RandomKiller(), 
				new SoftMaxSingleParentMateSelector(softMaxTemp), new FatherDuplicationMater(), new RandomOrganismMutate(varGen), nGenerations, 1, 1, mutation, enumerator.allRFs.size(), pn);
		
		opt.setInitialPopulation(enumerator.allRFs);
		
		OVarStringRep rep = new OVarStringRep() {
			
			@Override
			public String getStringRep(OptVariables vars) {
				return vars.toString();
			}
		};
		
		opt.enableOptimzationFileRecording(2, rep, outputFilePath);

		opt.optimize();
		
		System.out.println("Finished\n-----------------------------");
		
		List <VarFitnessPair> population = opt.getPopulation();
		Collections.sort(population, Collections.reverseOrder());
		for(VarFitnessPair vfp : population){
		
			System.out.println("Fitness Evaluation: " + vfp.fitness);
			ParameterizedRF bestR = subrff.generateRF(vfp.var.vars);
			bestR.printParameters();
			
			System.out.println("+++++++++++++++++++++++++");
			
			
			
		}
		
	}
	
	public static void runOptimizer(int nPop, int nGenerations, double learningRate, String outputPathDirectory){
		
		DPrint.toggleCode(284673923, false); //world printing debug code
		DPrint.toggleCode(25633, false); //tournament printing debug code
		
		JointReward objectiveRewardFunction = new TBFSStandardReward();
		ParameterizedRFFactory subrff = new TBFSSubRFFactory(objectiveRewardFunction);
		
		double lval = -2.;
		double hval = 2.;
		
		int pn = subrff.parameterSize();
		
		double [] low = new double[pn];
		double [] high = new double[pn];
		
		for(int i = 0; i < pn; i++){
			low[i] = lval;
			high[i] = hval;
		}
		
		VariableClamper clamp = new DiscStepVarClamp(low, high, 0.5);
		VariableRandomGenerator varGen = new ContinuousBoundedVarGen(low, high);
		VarEvaluaiton eval = new LazyRoundRobinEval(25, 1000, subrff, objectiveRewardFunction, learningRate);
		
		PopulationOptimization opt = new GeneticAlgorithm(eval, varGen, clamp, new RandomKiller(), 
				new SoftMaxSingleParentMateSelector(.3), new FatherDuplicationMater(), new RandomOrganismMutate(varGen), nGenerations, 1, 1, 0.2, nPop, pn);
		
		opt.enableOptimzationFileRecording(1, new OVarSRStringRep(subrff), outputPathDirectory);

		opt.optimize();
		
		System.out.println("Finished\n-----------------------------");
		
		List <VarFitnessPair> population = opt.getPopulation();
		Collections.sort(population, Collections.reverseOrder());
		for(VarFitnessPair vfp : population){
		
			System.out.println("Fitness Evaluation: " + vfp.fitness);
			ParameterizedRF bestR = subrff.generateRF(vfp.var.vars);
			bestR.printParameters();
			
			System.out.println("+++++++++++++++++++++++++");
			
			
			
		}
		
	}
	
	
	
	
	
	static class VarRatio{
		
		OptVariables v;
		double r;
		
		public VarRatio(OptVariables v, double r){
			this.v = v;
			this.r = r;
		}
		
		public static List<OptVariables> getPopulation(List <VarRatio> ratios, int n){
			
			List <OptVariables> pop = new ArrayList<OptVariables>(n);
			for(VarRatio r : ratios){
				int m = (int)(n*r.r);
				//System.out.println(m);
				for(int i = 0; i < m; i++){
					pop.add(new OptVariables(r.v));
				}
			}
			
			//System.out.println(pop.size());
			
			return pop;
			
		}
		
	}
	
}
