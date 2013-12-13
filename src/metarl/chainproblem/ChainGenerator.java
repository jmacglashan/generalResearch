package metarl.chainproblem;

import metarl.EnvironmentAndTask;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;


public class ChainGenerator {

	int [] stateOrder;
	double slipProb;
	
	
	public ChainGenerator(int [] stateOrder, double slipProb){
		this.stateOrder = stateOrder.clone();
		this.slipProb = slipProb;
	}
	
	public EnvironmentAndTask generateChainET(){
		
		GraphDefinedDomain gdd = new GraphDefinedDomain(stateOrder.length);
		int sNodeId = stateOrder[0];
		int endNode = stateOrder[stateOrder.length-1];
		
		
		//handle transitions for all but end node
		for(int i = 0; i < stateOrder.length-1; i++){
			int sId = stateOrder[i];
			int nId = stateOrder[i+1];
			gdd.setTransition(sId, 0, nId, 1.-slipProb);
			gdd.setTransition(sId, 0, sNodeId, slipProb);
			
			gdd.setTransition(sId, 1, sNodeId, 1.-slipProb);
			gdd.setTransition(sId, 1, nId, slipProb);
			
		}
		
		//handle end node transition
		gdd.setTransition(endNode, 0, endNode, 1.-slipProb);
		gdd.setTransition(endNode, 0, sNodeId, slipProb);
		
		gdd.setTransition(endNode, 1, sNodeId, 1.-slipProb);
		gdd.setTransition(endNode, 1, endNode, slipProb);
		
		Domain domain = gdd.generateDomain();
		RewardFunction rf = new ChainRF(sNodeId, endNode);
		TerminalFunction tf = new NullTermination();
		double discount = 0.95;
		State initialState = GraphDefinedDomain.getState(domain, sNodeId);
		StateGenerator initialStateGenerator = new ConstantStateGenerator(initialState);
		
		EnvironmentAndTask et = new EnvironmentAndTask(domain, rf, tf, discount, initialStateGenerator);
		
		
		return et;
	}
	
	
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer(50);
		for(int i : this.stateOrder){
			buf.append(i + " ");
		}
		buf.append(slipProb);
		return buf.toString();
	}
	
	
	class ChainRF implements RewardFunction{

		int endNode;
		int startNode;
		
		public ChainRF(int startNode, int endNode){
			this.startNode = startNode;
			this.endNode = endNode;
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			int primeNodeId = GraphDefinedDomain.getNodeId(sprime);
			if(primeNodeId == this.startNode){
				return 2.;
			}
			else{
				int cNodeId = GraphDefinedDomain.getNodeId(s);
				if(cNodeId == endNode && primeNodeId == endNode){
					return 10.;
				}
			}
			
			return 0.;
		}
		
		
		
	}


}
