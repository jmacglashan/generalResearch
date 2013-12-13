package metarl.chainproblem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import metarl.EnvironmentAndTask;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.DPrint;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class ComputeVRow {

	List<ChainGenerator> ms;
	List<AlgorithmFactory> as;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		ComputeVRow cvr = new ComputeVRow(2, 4);
		//cvr.computeV(2, 3);
		
		cvr.ms = EnvionrmentSampler.sampleClass(2, 1000);
		cvr.as = AlgorithmSampler.sampleClass(4, 1000);
		
		ChainGenerator cg = cvr.ms.get(0);
		EnvironmentAndTask env = cg.generateChainET();
		AlgorithmFactory af = cvr.as.get(8);
		
		System.out.println(cg);
		System.out.println(af.toString());
		
		
		QLearning ql = af.generateAlgorithm(env, 1000);
		EpisodeAnalysis ea = ql.runLearningEpisodeFrom(env.initialStateGenerator.generateState());
		System.out.println("Cumulative reward: " + ea.getDiscountedReturn(1.));
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			State s = ea.getState(i);
			GroundedAction ga = ea.getAction(i);
			double r = ea.getReward(i);
			System.out.println(nodeId(s) + " " + ga.toString() + " " + r);
		}
		
		System.out.println("---------------\nPolicy:");
		Policy p = new GreedyQPolicy(ql);
		for(int i = 0; i < 5; i++){
			State s = GraphDefinedDomain.getState(env.domain, i);
			System.out.println(i + " " + p.getAction(s).toString());
		}*/
		
		//The below is the actual code that is being commented out to test
		if(args.length != 5){
			System.out.println("Format: EnvClass AlgClass EnvRow rOutputFilePath pOutputFilePath");
			System.exit(0);
		}
		
		int ec = Integer.parseInt(args[0]);
		int ac = Integer.parseInt(args[1]);
		int er = Integer.parseInt(args[2]);
		String rOutputPath = args[3];
		String pOutputPath = args[4];
		
		ComputeVRow cvr = new ComputeVRow(ec, ac);
		System.out.println("Starting");
		//cvr.writeRowToFile(outputPath, er);
		cvr.writeRPRowsToFiles(rOutputPath, pOutputPath, er);
		System.out.println("Finished.");
		

	}
	
	
	public ComputeVRow(int mClass, int aClass){
		
		this.ms = EnvionrmentSampler.sampleClass(mClass, 1000);
		this.as = AlgorithmSampler.sampleClass(aClass, 1000);
		
		/*
		for(ChainGenerator e : this.ms){
			System.out.println(e.toString());
		}
		System.out.println("++++++++++++++++++++");
		for(AlgorithmFactory a : this.as){
			System.out.println(a.toString());
		}*/
		
	}
	
	public void writeRowToFile(String outputPath, int row){
		
		if(row >= this.ms.size()){
			return;
		}
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
			int n = this.as.size();
			//n = 20;
			for(int i = 0; i < n; i++){
				if(i > 0){
					out.write(" ");
				}
				double v = this.computeV(row, i);
				out.write(""+v);
				System.out.println("Computed col " + i);
			}
			out.write("\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void writeRPRowsToFiles(String outputPathR, String outputPathP, int row){
		
		if(row >= this.ms.size()){
			return;
		}
		
		try {
			BufferedWriter outR = new BufferedWriter(new FileWriter(outputPathR));
			BufferedWriter outP = new BufferedWriter(new FileWriter(outputPathP));
			int n = this.as.size();
			//n = 20;
			for(int i = 0; i < n; i++){
				if(i > 0){
					outR.write(" ");
					outP.write(" ");
				}
				double [] v = this.computeVReturnAndPolicy(row, i);
				outR.write(""+v[0]);
				outP.write(""+v[1]);
				System.out.println("Computed col " + i);
			}
			outR.write("\n");
			outP.write("\n");
			outR.close();
			outP.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double computeV(int mInd, int aInd){
		EnvironmentAndTask env = this.ms.get(mInd).generateChainET();
		AlgorithmFactory af = this.as.get(aInd);
		
		double sum = 0;
		int n = 256;
		
		for(int i = 0; i < n; i++){
			QLearning ql = af.generateAlgorithm(env, 1000);
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(env.initialStateGenerator.generateState());
			double r = ea.getDiscountedReturn(1.);
			sum += r;
			//System.out.println(i + " " + ea.numTimeSteps() + " " + r);
		}
		
		double v = sum / (double)n;
		
		return v;
		
	}
	
	
	
	public double [] computeVReturnAndPolicy(int mInd, int aInd){
		
		EnvironmentAndTask env = this.ms.get(mInd).generateChainET();
		AlgorithmFactory af = this.as.get(aInd);
		
		ValueIteration vi = new ValueIteration(env.domain, env.rf, env.tf, env.discount, new DiscreteStateHashFactory(), 0.01, 200);
		DPrint.toggleCode(vi.getDebugCode(), false);
		vi.planFromState(env.initialStateGenerator.generateState());
		Policy optPolicy = new GreedyQPolicy(vi);
		
		double sumR = 0;
		double sumP = 0;
		int n = 256;
		
		for(int i = 0; i < n; i++){
			QLearning ql = af.generateAlgorithm(env, 1000);
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(env.initialStateGenerator.generateState());
			double r = ea.getDiscountedReturn(1.);
			sumR += r;
			sumP += isOptimalPolicy(env.domain, optPolicy, new GreedyQPolicy(ql));
			//System.out.println(i + " " + ea.numTimeSteps() + " " + r);
		}
		
		double v = sumR / (double)n;
		double p = sumP / (double)n;
		
		
		return new double[]{v,p};
	}
	
	public static int isOptimalPolicy(Domain d, Policy opt, Policy query){
		
		for(int i = 0; i < 5; i++){
			State s = GraphDefinedDomain.getState(d, i);
			List<ActionProb> optDist = remove0Probs(opt.getActionDistributionForState(s));
			List<ActionProb> qDist = remove0Probs(query.getActionDistributionForState(s));
			if(optDist.size() != qDist.size()){
				return 0;
			}
			for(ActionProb oap : optDist){
				boolean found = false;
				for(ActionProb qap : qDist){
					if(oap.ga.action.getName().equals(qap.ga.action.getName())){
						found = true;
						break;
					}
				}
				if(!found){
					return 0;
				}
				
			}
		}
		
		return 1;
	}
	
	
	public static List <ActionProb> remove0Probs(List <ActionProb> input){
		List <ActionProb> res = new ArrayList<Policy.ActionProb>(input.size());
		for(ActionProb ap : input){
			if(ap.pSelection > 0.){
				res.add(ap);
			}
		}
		return res;
	}
	
	public static int nodeId(State s){
		ObjectInstance o = s.getFirstObjectOfClass(GraphDefinedDomain.CLASSAGENT);
		return o.getDiscValForAttribute(GraphDefinedDomain.ATTNODE);
	}
	

}
