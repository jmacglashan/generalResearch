package classexercises.ex1;

import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.debugtools.MyTimer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class Ex1SampleSolution {

	GridWorldDomain		gwd;
	Domain				domain;
	TerminalFunction 	tf;
	RewardFunction		rf;
	Policy				p;
	State 				s;
	
	
	public Ex1SampleSolution() {
		
		
		gwd = new GridWorldDomain(4, 3); //grid world width = 4, height = 3
		gwd.setObstacleInCell(1, 1); //obstacle at 1,1
		
		//with prob 0.8, try to go in intended direction; with prob 0.2 go in one of the two orthogonal directions
		double [][] transitionDynamics = new double [][]{{0.8, 0., 0.1, 0.1},{0., 0.8, 0.1, 0.1},{0.1, 0.1, 0.8, 0.},{0.1, 0.1, 0., 0.8}};
		gwd.setTransitionDynamics(transitionDynamics);
		
		domain = gwd.generateDomain(); //create the domain for us!
		
		
		tf = new LocTF(3, 2, 3, 1); //goal at 3,2; pit at 3,1
		rf = new LocRF(3, 2, 3, 1, 1., -1., -0.5); //goal at 3,2; pit at 3,1; goal reward = +1; pit reward = -1; otherwise -0.5 step
		
		p = new Policy() {
			
			@Override
			public boolean isStochastic() {
				return false; //for a given state the policy will always select the same action
			}
			
			@Override
			public List<ActionProb> getActionDistributionForState(State s) {
				return this.getDeterministicPolicy(s); //policy action probability distribution is deterministic and can be determined by using getAction method
			}
			
			@Override
			public GroundedAction getAction(State s) {
				return new GroundedAction(domain.getAction(GridWorldDomain.ACTIONNORTH), ""); //always select action north, which is a parameter-less action (hence the empty quotes)
			}
		};
		
		s = GridWorldDomain.getOneAgentNLocationState(domain, 0); //get a state for us that consists of an agent object instance and 0 location object instances
		GridWorldDomain.setAgent(s, 0, 0); //set the agent position in state s to be at 0,0 (bottom left corner)
		
		
	}

	
	public void percentWithinEpsilonForSampleSize(int goldStandardN, double epsilon){
		
		int m = 1000; //number of samples to compute confidence
		
		double goldStandard = this.evaluatePolicy(goldStandardN); //gold standard value of state
		System.out.println("Gold Standard: " + goldStandard);
		
		//find upper bound at which N for 95% confidence
		for(int i = 1; i < goldStandardN; i*=2){
			
			//what is the confidence of achieving an value < epsilon of the gold standard?
			double p = this.ratio(i, m, goldStandard, epsilon);
			
			System.out.println("" + p + " within " + epsilon + " for n=" + i);
			
			if(p > 0.95){
				//now that upper bound is found, search between it and the last value that was less than 95% confident
				System.out.println("Binary Search Starting");
				double [] result = this.binarySearch(i/2, i, goldStandard, epsilon, m);
				System.out.println("Smallest n=" + (int)result[0] + " giving " + result[1] + " within " + epsilon);
				break;
			}
			
		}
		
	}
	
	
	/**
	 * 
	 * @param lbound left boundary of search space
	 * @param rbound right boundary of search space
	 * @param goldStandard value to compare against to compute difference
	 * @param epsilon difference from gold standard that we want to be within
	 * @param m number of samples to compute confidence
	 * @return a double array containing the minimum n with at least 95% confidence, and the actual confidence of it
	 */
	public double [] binarySearch(int lbound, int rbound, double goldStandard, double epsilon, int m){
		
		//confidence threshold we want to achieve
		double threshold = 0.95;
		
		//find the mid index between our bounds
		int mind = (lbound + rbound) / 2;
		
		//what is the confidence of achieving an value < epsilon of the gold standard?
		double p = this.ratio(mind, m, goldStandard, epsilon);
		
		System.out.println("" + p + " within " + epsilon + " for n=" + mind);
		
		//found it exactly, so return it
		if(p == threshold){
			return new double []{mind, p};
		}
		
		
		if(p < threshold){
			
			//then there is nothing left to search, return this
			if(mind == rbound){
				return new double []{mind, p};
			}
			
			//to avoid integer rounding infinitely checking the left bound when the current range is 2, force it to the right bound
			if(mind == rbound-1){
				return this.binarySearch(rbound, rbound, goldStandard, epsilon, m);
			}
			
			//recursively search
			return this.binarySearch(mind, rbound, goldStandard, epsilon, m);
			
		}
		
		if(p > threshold){
			
			//then there is nothing left to search, return this
			if(mind == lbound){
				return new double []{mind, p};
			}
			
			double [] recursive = this.binarySearch(lbound, mind, goldStandard, epsilon, m);
			
			//because 0.95 might not exist, we may need to return a value above 0.95
			if(recursive[1] < threshold){
				//then return this instead since it was the first n over
				return new double []{mind, p};
			}
			else{
				return recursive;
			}
			
		}
		
		
		
		return null;
	}
	
	
	public double ratio(int n, int m, double goldStandard, double epsilon){
		
		int nUnder = 0;
		for(int j = 0; j < m; j++){
			double r = this.evaluatePolicy(n); //average return of n samples
			if(Math.abs(r - goldStandard) <= epsilon){ //if within epsilon of gold standard, count it
				nUnder++;
			}
		}
		
		double p = ((double)nUnder / (double)m); //ratio that were within epsilon using n samples
		
		return p;
		
	}
	
	public double evaluatePolicy(int n){
		
		double sum = 0.;
		for(int i = 0; i < n; i++){
			EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 10000); //sample the policy from state s until a terminal state is hit or it takes 10,000 steps
			sum += ea.getDiscountedReturn(1.0); //accumulate the discounted return from initial state in the sample using a discount factor of 1.0
		}
		
		return sum/n; //return the average discounted return
		
	}
	
	
	class LocTF implements TerminalFunction{

		int gx,gy,px,py; //store position of goal and pit
		
		public LocTF(int gx, int gy, int px, int py){
			this.gx = gx;
			this.gy = gy;
			this.px = px;
			this.py = py;
		}
		
		@Override
		public boolean isTerminal(State s) {
			
			//get the list of objects that belong to the class with the name stored in the string GridWorldDomain.CLASSAGENT
			//then retrieve the first of those object instances (we only expect there to ever be one agent object!)
			ObjectInstance agent = s.getObjectsOfTrueClass(GridWorldDomain.CLASSAGENT).get(0);
			
			//extract the agent object's x and y position
			int ax = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
			
			//return true (s is a terminal state) if the agent is in the same position as the goal loction or pit location
			return (ax==this.gx && ay==this.gy) || (ax==this.px && ay==this.py);
		}
		

	}
	
	
	class LocRF implements RewardFunction{

		
		int gx,gy,px,py; //store position of goal and pit
		double goalR,pitR,stepR; //store reward for transition to the goal, transition to the pit, or a general cost if the agent doesn't transition to one of those locations
		
		public LocRF(int gx, int gy, int px, int py, double goalR, double pitR, double stepR){
			this.gx = gx;
			this.gy = gy;
			this.px = px;
			this.py = py;
			
			this.goalR = goalR;
			this.pitR = pitR;
			this.stepR = stepR;
			
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			//get the list of objects that belong to the class with the name stored in the string GridWorldDomain.CLASSAGENT
			//then retrieve the first of those object instances (we only expect there to ever be one agent object!)
			//note that the object is retrieved from sprime because for the reward we want to know where the agent ended up!
			ObjectInstance agent = sprime.getObjectsOfTrueClass(GridWorldDomain.CLASSAGENT).get(0);
			
			//extract the agent object's x and y position
			int ax = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
			
			//in goal?
			if(ax==this.gx && ay==this.gy){
				return goalR;
			}
			
			//in pit?
			if(ax==this.px && ay==this.py){
				return pitR;
			}
			
			//nowhere special :(
			return stepR;
		}
		
		
		
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Ex1SampleSolution ex = new Ex1SampleSolution();
		
		//create timer to see how long this takes
		MyTimer timer = new MyTimer();
		
		//find smallest n for 95% confidence within epsilon 1.0; use 1000000 samples to compute gold standard value of state
		timer.start();
		ex.percentWithinEpsilonForSampleSize(1000000, 1.0);
		timer.stop();
		
		System.out.println("Compute time: " + timer.getTime());
		
		
	}

}
