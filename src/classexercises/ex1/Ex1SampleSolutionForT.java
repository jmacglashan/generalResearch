package classexercises.ex1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import classexercises.ex1.Ex1SampleSolution.LocRF;
import classexercises.ex1.Ex1SampleSolution.LocTF;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class Ex1SampleSolutionForT {

	GridWorldDomain		gwd;
	Domain				domain;
	TerminalFunction 	tf;
	RewardFunction		rf;
	Policy				p;
	State 				s;
	
	
	public Ex1SampleSolutionForT() {
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
			
			@Override
			public boolean isDefinedFor(State s) {
				return true;
			}
		};
		
		s = GridWorldDomain.getOneAgentNLocationState(domain, 0); //get a state for us that consists of an agent object instance and 0 location object instances
		GridWorldDomain.setAgent(s, 0, 0); //set the agent position in state s to be at 0,0 (bottom left corner)
	}
	
	
	public double [] getSamples(int n){
		
		double [] samples = new double[n];
		
		double sum = 0.;
		for(int i = 0; i < n; i++){
			EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 10000); //sample the policy from state s until a terminal state is hit or it takes 10,000 steps
			samples[i] = ea.getDiscountedReturn(1.);
		}
		
		return samples; //return the average discounted return
		
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
		
		Ex1SampleSolutionForT ex = new Ex1SampleSolutionForT();
		
		int n = 8500;
		System.out.println("Getting samples...");
		double [] samples = ex.getSamples(n);
		System.out.println("finished getting samples.");
		
		String path = "/Users/alerus/Desktop/tmp/s" + n +".csv";
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write("return\n");
			for(double s : samples){
				out.write(s+"\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
