package ethics.experiments.fssimple;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleBTJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import ethics.ParameterizedRFFactory;
import ethics.experiments.fssimple.aux.ConsantPsudoTermWorldGenerator;
import ethics.experiments.fssimple.aux.FSRQInit;
import ethics.experiments.fssimple.aux.FSSimpleBTSG;
import ethics.experiments.fssimple.aux.FSSimpleSG;
import ethics.experiments.fssimple.aux.FSSubjectiveRF;
import ethics.experiments.fssimple.aux.PseudoGameCountWorld;
import ethics.experiments.fssimple.aux.RNPseudoTerm;
import ethics.experiments.fssimple.specialagents.OpponentOutcomeAgent;
import ethics.experiments.tbforagesteal.aux.RFParamVarEnumerator;

public class FSSMatchCaching {

	protected double								baseLearningRate;
	
	protected List<OptVariables>					rfParamSet;
	protected ConsantPsudoTermWorldGenerator		worldGenerator;
	protected FSSimpleJR							objectiveReward;
	protected ParameterizedRFFactory				rewardFactory;

	protected AgentFactory							baseFactory;
	
	protected AgentType								fsAgentType;
	
	protected int									nTries;
	protected int									nGames;
	
	protected SGDomain 								domain;
	
	
	
	
	protected boolean								replaceResultsWithAvgOnly = true;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 2 && args.length != 3){
			System.out.println("Wrong format. For full cache use:\n\tpathToCacheOutput learningRate\nFor row cache use:\n\t" +
								"pathToOutputDirectory learningRate cacheMatrixRow");
			System.exit(-1);
		}
		
		
		DPrint.toggleCode(284673923, false); //world printing debug code
		DPrint.toggleCode(25633, false); //tournament printing debug code
		
		String outputFile = args[0];
		double lr = Double.parseDouble(args[1]);
		
		FSSMatchCaching mc = new FSSMatchCaching(lr);
		
		System.out.println("Beginning");
		if(args.length == 2){
			mc.cacheAll(outputFile);
		}
		else{
			int row = Integer.parseInt(args[2]);
			mc.cacheRow(outputFile, row);
		}
		

	}
	
	
	public FSSMatchCaching(double learningRate){
		
		this.baseLearningRate = learningRate;
		
		//this.rfParamSet = (new RFParamVarEnumerator(-1.5, 2.5, 0.5, 2)).allRFs;
		this.rfParamSet = (new RFParamVarEnumerator(0, 1., 1., 5)).allRFs;
		
		
		//this.objectiveReward = new FSSimpleJR();
		this.objectiveReward = new FSSimpleJR(1., -0.5, -2.5, 0.);
		//this.nTries =  25;
		this.nTries = 1000;
		this.nGames = 1000;
		this.rewardFactory = new FSSubjectiveRF.FSSubjectiveRFFactory(objectiveReward);
		
		double probBackTurned = 0.2;
		
		FSSimple dgen = new FSSimple();
		this.domain = (SGDomain)dgen.generateDomain();
		//JointActionModel jam = new FSSimpleJAM();
		JointActionModel jam = new FSSimpleBTJAM(probBackTurned);
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		double discount = 0.95;
		
		this.baseFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory);
		
		//SGStateGenerator sg = new FSSimpleSG(domain);
		SGStateGenerator sg = new FSSimpleBTSG(domain, probBackTurned);
		
		this.worldGenerator = new ConsantPsudoTermWorldGenerator(domain, jam, objectiveReward, new NullTermination(), sg, new RNPseudoTerm());
		
		this.fsAgentType = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
		
	}
	
	
	protected void cacheAll(String outFilePath){
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(outFilePath));
			
			for(int i = 0; i < this.rfParamSet.size(); i++){
				System.out.println("beginning comparisons for " + i);
				OptVariables v1 = this.rfParamSet.get(i);
				for(int j = i; j < this.rfParamSet.size(); j++){
					OptVariables v2 = this.rfParamSet.get(j);
					String res = this.getMatchResultString(v1, v2);
					out.write(res);
					out.write("\n");
					
				}
				
			}
			
			System.out.println("Finished.");
			
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
	}
	
	protected void cacheRow(String outputDirectoryPath, int row){
		
		if(!outputDirectoryPath.endsWith("/")){
			outputDirectoryPath = outputDirectoryPath + "/";
		}
		
		String pathName = outputDirectoryPath + row + ".txt";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(pathName));
			
			System.out.println("beginning row comparisons for " + row);
			OptVariables v1 = this.rfParamSet.get(row);
			for(int j = row; j < this.rfParamSet.size(); j++){
				System.out.println("comparing against " + j);
				OptVariables v2 = this.rfParamSet.get(j);
				String res = this.getMatchResultString(v1, v2);
				out.write(res);
				out.write("\n");
				
			}
				
			
			
			System.out.println("Finished.");
			
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	
	protected String getMatchResultString(OptVariables v1, OptVariables v2){
		
		MatchResult mr = this.getAverageMatch(v1, v2);
		
		StringBuffer buf = new StringBuffer();
		
		//format:
		//v11,v12,v13;v21,v22,v23::avgV1,stdV1
		//return11,return12,...,return1N
		//v21,v22,v23;v11,v12,v13::avgV2,stdV2
		//return21,return22,...,return2N
		
		buf.append(this.commaDelimString(v1)).append(";").append(this.commaDelimString(v2)).append("::").append(mr.avgA).append(",").append(mr.stdA).append("\n");
		if(!this.replaceResultsWithAvgOnly){
			for(int i = 0; i < mr.results.size(); i++){
				if(i > 0){
					buf.append(",");
				}
				buf.append(mr.results.get(i).a);
			}
		}
		else{
			buf.append(mr.avgA);
		}
		buf.append("\n");
		
		buf.append(this.commaDelimString(v2)).append(";").append(this.commaDelimString(v1)).append("::").append(mr.avgB).append(",").append(mr.stdB).append("\n");
		if(!this.replaceResultsWithAvgOnly){
			for(int i = 0; i < mr.results.size(); i++){
				if(i > 0){
					buf.append(",");
				}
				buf.append(mr.results.get(i).b);
			}
		}
		else{
			buf.append(mr.avgB);
		}
		
		
		return buf.toString();
	}
	
	protected MatchResult getAverageMatch(OptVariables v1, OptVariables v2){
		
		List <DoublePair> results = new ArrayList<DoublePair>(nTries);
		for(int i = 0; i < nTries; i++){
			results.add(runMatch(v1, v2));
		}
		
		return new MatchResult(results);
		
	}
	
	
	protected DoublePair runMatch(OptVariables v1, OptVariables v2){
		
		return this.runMatchHardCoded(v1, v2);
		
	}
	
	protected DoublePair runMatchLearning(OptVariables v1, OptVariables v2){
		JointReward subjectiveRewardV1 = this.rewardFactory.generateRF(v1.vars);
		AgentFactory factV1 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV1);
		//FSRQInit v1QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV1);
		
		JointReward subjectiveRewardV2 = this.rewardFactory.generateRF(v2.vars);
		AgentFactory factV2 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV2);
		//FSRQInit v2QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV2);
		//FSRPunisherQInit v2QInit = new FSRPunisherQInit((FSSubjectiveRF)subjectiveRewardV2, (FSSimpleJR)this.objectiveReward);
		
		
		
		//role 1
		
		SGQLAgent a1 = (SGQLAgent)factV1.generateAgent();
		//a1.setQValueInitializer(v1QInit);
		a1.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.));
		a1.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		SGQLAgent a2 = (SGQLAgent)factV2.generateAgent();
		//a2.setQValueInitializer(v2QInit);
		a2.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(-6.5));
		a2.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		PseudoGameCountWorld w1 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a1.joinWorld(w1, this.fsAgentType);
		a2.joinWorld(w1, this.fsAgentType);
		
		w1.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r1 = w1.getCumulativeRewardForAgent(a1.getAgentName());
		double a2r1 = w1.getCumulativeRewardForAgent(a2.getAgentName());
		
		//role 2
		
		//FSRPunisherQInit v12QInit = new FSRPunisherQInit((FSSubjectiveRF)subjectiveRewardV1, (FSSimpleJR)this.objectiveReward);
		//FSRQInit v12QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV1);
		//FSRQInit v22QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV2);
		
		SGQLAgent a12 = (SGQLAgent)factV1.generateAgent();
		//a12.setQValueInitializer(v12QInit);
		a12.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(-6.5));
		a12.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		SGQLAgent a22 = (SGQLAgent)factV2.generateAgent();
		//a22.setQValueInitializer(v22QInit);
		a22.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.));
		a22.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		PseudoGameCountWorld w2 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a22.joinWorld(w2, this.fsAgentType); //switch join order
		a12.joinWorld(w2, this.fsAgentType);
		
		w2.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r2 = w2.getCumulativeRewardForAgent(a12.getAgentName());
		double a2r2 = w2.getCumulativeRewardForAgent(a22.getAgentName());
		
		double a1r = a1r1 + a1r2;
		double a2r = a2r1 + a2r2;
		
		DoublePair res = new DoublePair(a1r, a2r);
		
		
		
		return res;
	}
	
	
	protected DoublePair runMatchHardCoded(OptVariables v1, OptVariables v2){
		
		int [] hp1 = this.doubleToIntArray(v1.vars);
		int [] hp2 = this.doubleToIntArray(v2.vars);
		
		OpponentOutcomeAgent a1 = new OpponentOutcomeAgent(this.domain, hp1);
		OpponentOutcomeAgent a2 = new OpponentOutcomeAgent(this.domain, hp2);
		
		PseudoGameCountWorld w1 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a1.joinWorld(w1, this.fsAgentType);
		a2.joinWorld(w1, this.fsAgentType);
		
		w1.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r1 = w1.getCumulativeRewardForAgent(a1.getAgentName());
		double a2r1 = w1.getCumulativeRewardForAgent(a2.getAgentName());
		
		
		OpponentOutcomeAgent a12 = new OpponentOutcomeAgent(this.domain, hp1);
		OpponentOutcomeAgent a22 = new OpponentOutcomeAgent(this.domain, hp2);
		
		PseudoGameCountWorld w2 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a22.joinWorld(w2, this.fsAgentType); //switch join order
		a12.joinWorld(w2, this.fsAgentType);
		
		w2.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r2 = w2.getCumulativeRewardForAgent(a12.getAgentName());
		double a2r2 = w2.getCumulativeRewardForAgent(a22.getAgentName());
		
		double a1r = a1r1 + a1r2;
		double a2r = a2r1 + a2r2;
		
		DoublePair res = new DoublePair(a1r, a2r);
		
		
		return res;
		
	}
	
	protected int [] doubleToIntArray(double [] vars){
		int [] ia = new int[vars.length];
		for(int i = 0; i < vars.length; i++){
			ia[i] = (int)vars[i];
		}
		return ia;
	}
	
	
	protected String commaDelimString(OptVariables v){
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < v.vars.length; i++){
			if(i > 0){
				buf.append(",");
			}
			buf.append(v.vars[i]);
		}
		return buf.toString();
	}
	
	
	
	class MatchResult{
		
		public List <DoublePair> results;
		
		public double avgA;
		public double avgB;
		
		public double stdA;
		public double stdB;
		
		
		public MatchResult(List <DoublePair> results){
			this.results = results;
			
			
			double sumA = 0.;
			double sumB = 0.;
			for(DoublePair dp : results){
				sumA += dp.a;
				sumB += dp.b;
			}
			
			this.avgA = sumA / results.size();
			this.avgB = sumB / results.size();
			
			double sumVA = 0.;
			double sumVB = 0.;
			for(DoublePair dp : results){
				
				double diffA = dp.a - this.avgA;
				double diffB = dp.b - this.avgB;
				
				sumVA += diffA*diffA;
				sumVB += diffB*diffB;
				
			}
			
			this.stdA = Math.sqrt(sumVA / results.size());
			this.stdB = Math.sqrt(sumVB / results.size());
			
		}
		
		
	}
	
	
	class DoublePair{
		
		public double a;
		public double b;
		
		public DoublePair(double a, double b){
			this.a = a;
			this.b = b;
		}
		
		public DoublePair reverse(){
			return new DoublePair(this.b, this.a);
		}
		
	}
	

}
