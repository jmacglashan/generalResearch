package optimization.infinitega;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import optimization.OVarStringRep;
import optimization.OptVariables;
import optimization.Optimization;
import optimization.VarEvaluaiton;
import optimization.VarFitnessPair;

public class InfiniteGA implements Optimization {

	protected VarEvaluaiton				varEval;
	protected RatioReproduce			ratioReproduce;
	protected RatioKiller				ratioKiller;
	
	
	protected List <GenomeRatioFitness> popDist;
	protected List <OptVariables>		genomes;
	
	
	protected int						maxGenerations;
	
	
	protected int						bestIndex;
	protected double					bestFitness;
	protected double					averageFitness;
	protected int						nEvals;
	
	protected int						shouldRecord = 0;
	protected String					outputPath;
	protected OVarStringRep				varRep;
	protected BufferedWriter			singleFileWriter;

	
	
	public InfiniteGA(VarEvaluaiton varEval, RatioReproduce ratioReproduce, RatioKiller ratioKiller, List <OptVariables> possibleGenomes, int maxGenerations){
		this.varEval = varEval;
		this.ratioReproduce = ratioReproduce;
		this.ratioKiller = ratioKiller;
		this.genomes = possibleGenomes;
		this.maxGenerations = maxGenerations;
		
		
		popDist = new ArrayList<GenomeRatioFitness>(this.genomes.size());
		double sr = 1. / this.genomes.size();
		
		for(OptVariables v : this.genomes){
			GenomeRatioFitness grf = new GenomeRatioFitness(new GenomeRatio(v, sr), Double.NEGATIVE_INFINITY);
			this.popDist.add(grf);
		}
		
	}
	
	
	@Override
	public void optimize() {
		
		this.updateFitness();
		this.recordToFile(0);
		printStats();
		
		for(int i = 0; i < maxGenerations; i++){
			this.runGeneration();
			this.recordToFile(i+1);
			printStats();
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
		
		RepResult res = this.ratioReproduce.ratioReproduce(popDist);
		//this.ratioKiller.kill(popDist, res);
		this.popDist = res.nextPop;
		this.updateFitness();
		this.nEvals += popDist.size();
		
	}

	@Override
	public OptVariables getBest() {
		return this.popDist.get(this.bestIndex).gr.var;
	}

	@Override
	public double getBestFitness() {
		return this.bestFitness;
	}

	public void printStats(){
		System.out.println("" + nEvals + ": " + bestFitness + "\t" + averageFitness);
	}
	
	@Override
	public void enableOptimzationFileRecording(int recordMode, OVarStringRep rep, String outputPathDirectory) {
		this.varRep = rep;
		this.outputPath = outputPathDirectory;
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
	
	
	public GenomeRatio ratioForGenome(int i){
		return popDist.get(i).gr;
	}
	
	public List <GenomeRatioFitness> getFinalResults(){
		return this.popDist;
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
	
	
	protected void updateFitness(){
		
		List <Double> fitness = this.varEval.evaluate(genomes);
		
		this.bestFitness = Double.NEGATIVE_INFINITY;
		this.averageFitness = 0.;
		this.bestIndex = -1;
		
		for(int i = 0; i < genomes.size(); i++){
			double f = fitness.get(i);
			GenomeRatioFitness grf = this.popDist.get(i);
			grf.fitness = f;
			
			if(grf.gr.ratio > 0.){
				this.averageFitness += f*grf.gr.ratio;
			}
			if(f > this.bestFitness){
				this.bestFitness = f;
				this.bestIndex = i;
			}
			
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
			e.printStackTrace();
		}
	}
	
	
	protected String populationRecord(){
		StringBuffer sbuf = new StringBuffer();
		
		List <GenomeRatioFitness> copy = new ArrayList<GenomeRatioFitness>(this.popDist);
		Collections.sort(copy, Collections.reverseOrder());
		
		for(GenomeRatioFitness grf : copy){
			sbuf.append(grf.fitness).append("\n");
			sbuf.append(grf.gr.ratio).append("\n");
			sbuf.append(this.varRep.getStringRep(grf.gr.var)).append("\n");
			sbuf.append("+++++++++++++++++++++++++\n");
		}
		
		
		return sbuf.toString();
	}
	
	
}
