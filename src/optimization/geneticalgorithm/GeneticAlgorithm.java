package optimization.geneticalgorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import optimization.OVarStringRep;
import optimization.OptVariables;
import optimization.Optimization;
import optimization.PopulationOptimization;
import optimization.VarEvaluaiton;
import optimization.VarFitnessPair;
import optimization.VariableClamper;
import optimization.VariableRandomGenerator;
import optimization.geneticalgorithm.MateSelector.MatePair;
import optimization.geneticalgorithm.gamodules.ContinuousBlendMate;
import optimization.geneticalgorithm.gamodules.KillWorst;
import optimization.geneticalgorithm.gamodules.TopInstanceMateSelector;
import optimization.geneticalgorithm.gamodules.VariableSwapMutator;

public class GeneticAlgorithm implements PopulationOptimization{

	
	//modules
	protected VarEvaluaiton						evaluator;
	protected VariableRandomGenerator			varGen;
	protected VariableClamper					clamper;
	protected KillSelector						killSelector;
	protected MateSelector						mateSelector;
	protected Mate								mate;
	protected Mutator							mutator;
	
	//internal data
	protected List<OptVariables>				population;
	protected List <Double>						fitness;
	
	
	//GA parameters
	protected int								maxGenerations;
	protected int								nOffspringPerMates;
	protected int								nMatePairs;
	protected double							mutationRate;
	
	
	//statistics variables
	protected double							bestFitness;
	protected int								bestIndex;
	protected double							averageFitness;
	protected int								nEvals;
	
	protected int								shouldRecord = 0;
	protected String							outputPath;
	protected OVarStringRep						varRep;
	protected BufferedWriter					singleFileWriter;
	
	
	public GeneticAlgorithm(VarEvaluaiton eval, VariableRandomGenerator varGen, VariableClamper clamper, KillSelector kselect,
			MateSelector mselect, Mate mate, Mutator mutator, int maxGen, int nOffspring, int nMatePairs, double mutateRate, int n, int m){
		
		this.gaInit(eval, varGen, clamper, kselect, mselect, mate, mutator, maxGen, nOffspring, nMatePairs, mutateRate, n , m);
		
	}
	
	public GeneticAlgorithm(VarEvaluaiton eval, VariableRandomGenerator varGen, VariableClamper clamper, int maxGen, int nOffspring, 
			int nMatePairs, double mutateRate, int n, int m){
		this.gaInit(eval, varGen, clamper, new KillWorst(0.5), new TopInstanceMateSelector(), new ContinuousBlendMate(), new VariableSwapMutator(varGen),
				maxGen, nOffspring, nMatePairs, mutateRate, n , m);
	}
	
	
	protected void gaInit(VarEvaluaiton eval, VariableRandomGenerator varGen, VariableClamper clamper, KillSelector kselect,
			MateSelector mselect, Mate mate, Mutator mutator, int maxGen, int nOffspring, int nMatePairs, double mutateRate, int n, int m){
		
		this.evaluator = eval;
		this.varGen = varGen;
		this.clamper = clamper;
		this.killSelector = kselect;
		this.mateSelector = mselect;
		this.mate = mate;
		this.mutator = mutator;
		this.maxGenerations = maxGen;
		this.nOffspringPerMates = nOffspring;
		this.nMatePairs = nMatePairs;
		this.mutationRate = mutateRate;
		
		this.seedPopulation(n, m);
		
	}
	
	
	public void seedPopulation(int n, int m){
		
		population = new ArrayList<OptVariables>(n);
		
		for(int i = 0; i < n; i++){
			
			OptVariables instance = new OptVariables(m);
			for(int j = 0; j < m; j++){
				instance.set(j, varGen.valueForVar(j));
			}
			clamper.clamp(instance);
			population.add(instance);
			
		}
		
		fitness = evaluator.evaluate(population);
		nEvals += population.size();
		this.computeStatistics();
		
		
	}
	
	
	
	public void setInitialPopulation(List <OptVariables> seedPop){
		this.population = new ArrayList<OptVariables>(seedPop);
		
		fitness = evaluator.evaluate(population);
		nEvals = population.size();
		this.computeStatistics();
	}
	
	
	@Override
	public void optimize(){
		
		//this.printDist(this.populationDist());
		
		this.printStats();
		this.recordToFile(0);
		
		for(int i = 0; i < maxGenerations; i++){
			this.runGeneration();
			this.printStats();
			this.recordToFile(i+1);
			//this.printDist(this.populationDist());
		}
		
		if(this.shouldRecord == 2){
			try {
				this.singleFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	public void runGeneration(){
		
		
		//select mates
		List <MatePair> mates = mateSelector.selectMates(fitness, nMatePairs);
		
		//mate
		List <OptVariables> offspring = this.getOffspring(mates);
		
		//mutate
		mutator.mutate(offspring, mutationRate);
		
		//kill off members of population
		this.killInstnaces();
		
		//make sure the mutation and offspring generator created valid entries and add to population to replace killed instances
		for(OptVariables v : offspring){
			clamper.clamp(v);
			population.add(v);
		}
		
		
		//compute new fitness
		fitness = evaluator.evaluate(population);
		nEvals += population.size();
		this.computeStatistics();
		
	}
	
	
	protected void killInstnaces(){
		Set <Integer> selections = killSelector.selectInstancesToKill(fitness, nMatePairs*nOffspringPerMates);
		
		List <OptVariables> rpop = new ArrayList<OptVariables>(population.size());
		List <Double> rfitness = new ArrayList<Double>(fitness.size());
		
		for(int i = 0; i < population.size(); i++){
			if(!selections.contains(i)){
				rpop.add(population.get(i));
				rfitness.add(fitness.get(i));
			}
		}
		
		population = rpop;
		fitness = rfitness;
		
	}
	
	
	protected List <OptVariables> getOffspring(List <MatePair> mates){
		List <OptVariables> offspring = new ArrayList<OptVariables>(mates.size()*nOffspringPerMates);
		for(MatePair mp : mates){
			OptVariables m = population.get(mp.m);
			OptVariables f = population.get(mp.f);
			offspring.addAll(mate.mate(m, f, nOffspringPerMates));
		}
		return offspring;
	}
	
	
	protected void computeStatistics(){
		bestFitness = Double.NEGATIVE_INFINITY;
		averageFitness = 0.;
		
		for(int i = 0; i < fitness.size(); i++){
			double f = fitness.get(i);
			if(f > bestFitness){
				bestFitness = f;
				bestIndex = i;
			}
			averageFitness += f;
		}
		
		averageFitness = averageFitness / fitness.size();
		
	}
	
	@Override
	public OptVariables getBest(){
		return population.get(bestIndex);
	}
	
	public void printStats(){
		System.out.println("" + nEvals + ": " + bestFitness + "\t" + averageFitness);
	}

	@Override
	public double getBestFitness() {
		return fitness.get(bestIndex);
	}

	@Override
	public List<VarFitnessPair> getPopulation() {
		
		List <VarFitnessPair> pop = new ArrayList<VarFitnessPair>(population.size());
		for(int i = 0; i < population.size(); i++){
			pop.add(new VarFitnessPair(population.get(i), fitness.get(i)));
		}
		
		return pop;
	}

	@Override
	public void enableOptimzationFileRecording(int recordMode, OVarStringRep rep, String outputPath) {
		this.varRep = rep;
		this.outputPath = outputPath;
		this.shouldRecord = recordMode;
		
		if(recordMode == 1 && !this.outputPath.endsWith("/")){
			this.outputPath = this.outputPath + "/";
		}
		else if(recordMode == 2){
			try{
				singleFileWriter = new BufferedWriter(new FileWriter(outputPath));
			}catch(Exception e){
				System.out.println(e);
			}
		}
		
	}

	@Override
	public void disableOptimizationFileRecording() {
		this.shouldRecord = 0;
	}
	
	
	public void recordToFile(int gen){
		
		if(this.shouldRecord == 0){
			return ;
		}
		
		else if(this.shouldRecord == 1){
			this.separateFileRecord(gen);
		}
		else if(this.shouldRecord == 2){
			this.singleFileRecord(gen);
		}
		
		
		
	}
	
	
	protected void separateFileRecord(int gen){
		String filePath = this.outputPath + "GAGen-" + gen + ".txt";
		
		File f = (new File(filePath)).getParentFile();
		f.mkdirs();
		
		String record = this.populationRecord();
		
		
		try{
			
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
			out.write(record);
			out.close();
			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	protected void singleFileRecord(int gaGen){
		try {
			this.singleFileWriter.write("\n\n*GAGEN " + gaGen + "\n");
			String record = this.populationRecord();
			this.singleFileWriter.write(record);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	protected String populationRecord(){
		StringBuffer sbuf = new StringBuffer();
		
		List <VarFitnessPair> population = this.getPopulation();
		Collections.sort(population, Collections.reverseOrder());
		
		for(VarFitnessPair vfp : population){
			sbuf.append("Fitness Evaluation: ").append(vfp.fitness).append("\n");
			sbuf.append(this.varRep.getStringRep(vfp.var)).append("\n");
			sbuf.append("+++++++++++++++++++++++++\n");
		}
		
		return sbuf.toString();
	}
	
	
	protected void printDist(Map <String, Integer> dist){
		
		for(Map.Entry<String, Integer> pair : dist.entrySet()){
			System.out.println(pair.getKey() + ": " + pair.getValue());
		}
		
	}
	
	protected Map <String, Integer> populationDist(){
		
		Map <String, Integer> dist = new HashMap<String, Integer>();
		
		for(OptVariables v : this.population){
			int cur = 0;
			if(dist.containsKey(v.toString())){
				cur = dist.get(v.toString());
			}
			dist.put(v.toString(), cur+1);
		}
		
		return dist;
	}
	
}
