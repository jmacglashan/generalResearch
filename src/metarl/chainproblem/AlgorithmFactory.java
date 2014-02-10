package metarl.chainproblem;

import metarl.EnvironmentAndTask;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

public class AlgorithmFactory {

	double learningRate;
	double epsilon;
	double [][] qInit;
	
	public AlgorithmFactory(double learningRate, double epsilon, double [][] qInit){
		this.learningRate = learningRate;
		this.epsilon = epsilon;
		this.qInit = qInit.clone();
	}
	
	public QLearning generateAlgorithm(EnvironmentAndTask et, int maxSteps){
		
		ValueFunctionInitialization vfi = new GraphDomainQInit();
		EpsilonGreedy egreedy = new EpsilonGreedy(epsilon);
		QLearning alg = new QLearning(et.domain, et.rf, et.tf, et.discount, new DiscreteStateHashFactory(), vfi, learningRate, egreedy, maxSteps);
		egreedy.setPlanner(alg);
		
		return alg;
	}
	
	
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer(256);
		buf.append(learningRate).append(" ").append(epsilon);
		for(int i = 0; i < qInit.length; i++){
			for(int j = 0; j < qInit[i].length; j++){
				buf.append(" " + qInit[i][j]);
			}
		}
		
		return buf.toString();
	}
	
	class GraphDomainQInit implements ValueFunctionInitialization{
		
		
		@Override
		public double value(State s) {
			throw new UnsupportedOperationException("Q Initialization supported, not state value function.");
		}

		@Override
		public double qValue(State s, AbstractGroundedAction a) {
			int cNodeId = s.getObjectsOfTrueClass(GraphDefinedDomain.CLASSAGENT).get(0).getDiscValForAttribute(GraphDefinedDomain.ATTNODE);
			int aId = this.actionId(a);
			return qInit[cNodeId][aId];
		}
		
		
		protected int actionId(AbstractGroundedAction a){
			String aName = a.actionName();
			String indPart = aName.substring(GraphDefinedDomain.BASEACTIONNAME.length());
			return Integer.parseInt(indPart);
		}
		
		
		
		
		
	}
	
}
