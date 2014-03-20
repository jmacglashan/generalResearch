package tests.rtdp;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.rtdp.RTDP;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import domain.singleagent.binarylock.BinaryLockDomain;

public class RTDPConvergeTest {

	public static void main(String [] args){
		
		//RandomFactory.seedMapped(0, 1937);
		
		
		BinaryLockDomain dgen = new BinaryLockDomain(15);
		Domain domain = dgen.generateDomain();
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		State initialState = BinaryLockDomain.getStartState(domain, 11);
		RewardFunction rf = new UniformCostRF();
		TerminalFunction tf = new TerminateOnBitSequence(1,1,1,1,1,1,1,1,1,1);
		
		RTDP rtdp = new RTDP(domain, rf, tf, 0.99, hashingFactory, 0, 100000, 0.01, 20);
		
		rtdp.planFromState(initialState);
		
		
		GreedyQPolicy policy = new GreedyQPolicy(rtdp);
		
		
		EpisodeAnalysis ea = policy.evaluateBehavior(initialState, rf, tf);
		
		System.out.println(ea.getState(ea.numTimeSteps()-1).getCompleteStateDescription());
		
		
		
		
		/*
		int [] testBitSequence = new int[]{1,0,1,0};
		List<State> testStates = new ArrayList<State>();
		for(int i = 0; i < testBitSequence.length; i++){
			int [] bitSeq = new int[testBitSequence.length];
			for(int j = 0; j < i; j++){
				bitSeq[j] = testBitSequence[j];
			}
			for(int j = i; j < testBitSequence.length; j++){
				bitSeq[j] = 2;
			}
			
			State s = BinaryLockDomain.getState(domain, i, bitSeq);
			testStates.add(s);
		}
		
		
		for(int i = 0; i < testStates.size(); i++){
			State s = testStates.get(i);
			double v = rtdp.value(s);
			System.out.println(i + ": " + v);
		}
		*/
		
		
		
	}
	
	
	
	
	public static class TerminateOnBitSequence implements TerminalFunction{

		int [] bitSequence;
		
		
		public TerminateOnBitSequence(int...bitSequence){
			this.bitSequence = bitSequence;
		}
		
		@Override
		public boolean isTerminal(State s) {
			
			int hpos = s.getFirstObjectOfClass(BinaryLockDomain.CLASSHEAD).getDiscValForAttribute(BinaryLockDomain.ATTCURBIT);
			List<ObjectInstance> bits = s.getObjectsOfTrueClass(BinaryLockDomain.CLASSBIT);
			
			if(hpos >= bits.size()){
				return true;
			}
			
			for(int i = 0; i < this.bitSequence.length; i++){
				int b = this.bitSequence[i];
				int actual = bits.get(i).getDiscValForAttribute(BinaryLockDomain.ATTBITVAL);
				if(b != actual){
					return false;
				}
				
			}
			
			return true;
		}
		
		
		
		
	}
	
}
