package ethics.experiments.tbforagesteal.matchcaching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.WorldGenerator;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import burlap.oomdp.stochasticgames.tournament.MatchSelector;
import burlap.oomdp.stochasticgames.tournament.Tournament;
import burlap.oomdp.stochasticgames.tournament.common.AllPairWiseSameTypeMS;
import burlap.oomdp.stochasticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.TBFSAlternatingTurnSG;
import domain.stocasticgames.foragesteal.TBFSStandardMechanics;
import domain.stocasticgames.foragesteal.TBFSStandardReward;
import domain.stocasticgames.foragesteal.TBFSWhoStartedMechanics;
import domain.stocasticgames.foragesteal.TBForageSteal;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import ethics.ParameterizedRFFactory;
import ethics.experiments.tbforagesteal.aux.RFParamVarEnumerator;
import ethics.experiments.tbforagesteal.aux.TBFSSubRFFactory;
import ethics.experiments.tbforagesteal.aux.TBFSSubRFWSFactory;
import ethics.experiments.tbforagesteal.aux.TBFSSubRFWSFactory4P;

public class MatchCaching {

	protected List<OptVariables>					rfParamSet;
	protected WorldGenerator						worldGenerator;
	protected JointReward							objectiveReward;
	protected ParameterizedRFFactory				rewardFactory;

	protected AgentFactory							baseFactory;
	
	protected AgentType								fsAgentType;
	
	protected int									nTries;
	protected int									nGames;
	
	
	
	
	
	
	
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
		
		MatchCaching mc = new MatchCaching(lr);
		
		System.out.println("Beginning");
		if(args.length == 2){
			mc.cacheAll(outputFile);
		}
		else{
			int row = Integer.parseInt(args[2]);
			mc.cacheRow(outputFile, row);
		}
	

	}
	
	
	
	
	
	public MatchCaching(double learningRate){
		
		//this.standardGameMechanicsInit(learningRate);
		//this.whoStartedItMechanicsInit(learningRate);
		this.whoStartedItMechanicsAltInit(learningRate);
		
		
	}
	
	
	protected void standardGameMechanicsInit(double learningRate){
		
		rfParamSet = getPossibleRFParams(-1.5, 0.5, 9);
		
		objectiveReward = new TBFSStandardReward();
		this.nTries =  25;
		this.nGames = 1000;
		this.rewardFactory = new TBFSSubRFFactory(objectiveReward);
		
		TBForageSteal gen = new TBForageSteal();
		SGDomain domain = (SGDomain) gen.generateDomain();
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		double discount = 0.99;
		
		baseFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		worldGenerator = new ConstantWorldGenerator(domain, new TBFSStandardMechanics(), objectiveReward, 
				new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG(domain));
		
		fsAgentType = new AgentType("default", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		
		
	}
	
	protected void whoStartedItMechanicsInit(double learningRate){
		
		//rfParamSet = getPossibleRFParams(-1.5, 0.5, 9);
		rfParamSet = (new RFParamVarEnumerator(-1, 3, 1., 4)).allRFs;
		
		objectiveReward = new TBFSStandardReward();
		this.nTries =  25;
		this.nGames = 1000;
		//this.rewardFactory = new TBFSSubRFWSFactory(objectiveReward);
		this.rewardFactory = new TBFSSubRFWSFactory4P(objectiveReward);
		
		TBForageSteal gen = new TBForageSteal();
		SGDomain domain = (SGDomain) gen.generateDomain();
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		double discount = 0.99;
		
		baseFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		worldGenerator = new ConstantWorldGenerator(domain, new TBFSWhoStartedMechanics(), objectiveReward, 
				new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG(domain));
		
		fsAgentType = new AgentType("default", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		
	}
	
	
	protected void whoStartedItMechanicsAltInit(double learningRate){
		
		rfParamSet = getPossibleRFParams(-1.5, 0.5, 9);
		//rfParamSet = (new RFParamVarEnumerator(-1, 3, 1., 4)).allRFs;
		
		objectiveReward = new TBFSStandardReward(1, -1, -.1, -2, new double[]{-2, 0, 2, 0, 0});
		this.nTries =  25;
		this.nGames = 1000;
		this.rewardFactory = new TBFSSubRFWSFactory(objectiveReward);
		//this.rewardFactory = new TBFSSubRFWSFactory4P(objectiveReward);
		
		TBForageSteal gen = new TBForageSteal();
		gen.setNoopInFirstState(false);
		SGDomain domain = (SGDomain) gen.generateDomain();
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		double discount = 0.99;
		
		baseFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		worldGenerator = new ConstantWorldGenerator(domain, new TBFSWhoStartedMechanics(), objectiveReward, 
				new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG(domain, new double[]{1., 0.5, 0.5, 0., 0.}));
		
		fsAgentType = new AgentType("default", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		
	}
	
	
	public List<OptVariables> getPossibleRFParams(double low, double interval, int nItervals){
		
		List <OptVariables> paramSet = new ArrayList<OptVariables>();
		
		for(int i = 0; i < nItervals; i++){
			double pi = low + interval*i;
			for(int j = 0; j < nItervals; j++){
				double pj = low + interval*j;
				for(int k = 0; k < nItervals; k++){
					double pk = low + interval*k;
					
					double [] params = new double[]{pi,pj,pk};
					OptVariables ovars = new OptVariables(params);
					paramSet.add(ovars);
					
					
				}
			}
		}
		
		return paramSet;
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
		for(int i = 0; i < mr.results.size(); i++){
			if(i > 0){
				buf.append(",");
			}
			buf.append(mr.results.get(i).a);
		}
		buf.append("\n");
		
		buf.append(this.commaDelimString(v2)).append(";").append(this.commaDelimString(v1)).append("::").append(mr.avgB).append(",").append(mr.stdB).append("\n");
		for(int i = 0; i < mr.results.size(); i++){
			if(i > 0){
				buf.append(",");
			}
			buf.append(mr.results.get(i).b);
		}
		
		
		return buf.toString();
	}
	
	protected MatchResult getAverageMatch(OptVariables v1, OptVariables v2){
		
		List <DoublePair> results = new ArrayList<MatchCaching.DoublePair>(nTries);
		for(int i = 0; i < nTries; i++){
			results.add(runMatch(v1, v2));
		}
		
		return new MatchResult(results);
		
	}
	
	
	protected DoublePair runMatch(OptVariables v1, OptVariables v2){
		
		
		MatchSelector selector = new AllPairWiseSameTypeMS(fsAgentType, 2);
		Tournament tourn = new Tournament(1000, nGames, selector, worldGenerator);
		
		JointReward subjectiveRewardV1 = this.rewardFactory.generateRF(v1.vars);
		AgentFactory factV1 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV1);
		
		JointReward subjectiveRewardV2 = this.rewardFactory.generateRF(v2.vars);
		AgentFactory factV2 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV2);
		
		tourn.addAgent(factV1);
		tourn.addAgent(factV2);
		
		tourn.runTournament();
		
		DoublePair res = new DoublePair(tourn.getCumulativeRewardFor(0), tourn.getCumulativeRewardFor(1));
		
		return res;
		
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
