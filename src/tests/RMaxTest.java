package tests;

import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.learning.modellearning.artdp.ARTDP;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.behavior.singleagent.shaping.potential.PotentialFunction;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;

public class RMaxTest {

	GridWorldDomain 			gwdg;
	SADomain					domain;
	RewardFunction 				rf;
	TerminalFunction			tf;
	State 						initialState;
	DiscreteStateHashFactory	hashingFactory;
	double						gamma = 0.99;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RMaxTest test = new RMaxTest();
		test.experiment();

	}
	
	
	public RMaxTest(){
		this.gwdg = new GridWorldDomain(11, 11);
		this.gwdg.setMapToFourRooms();
		this.domain = (SADomain)this.gwdg.generateDomain();
		
		rf = new UniformCostRF(); //reward always returns -1 (no positive reward on goal state either; but since the goal state ends action it will still be favored)
		tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION)); //ends when the agent reaches a location
		
		//set up the initial state
		initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
		GridWorldDomain.setAgent(initialState, 0, 0);
		GridWorldDomain.setLocation(initialState, 0, 10, 10);
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList); //optional code line; uses only the agent position to perform hash calculations instead of the agent and all locations
		
		
	}
	
	public void experiment(){
		
		LearningAgentFactory sarsaLearningFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "SARSA";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new SarsaLam(domain, rf, tf, gamma, hashingFactory, 0.0, 0.1, 1.);
			}
		};
		
		LearningAgentFactory qLearningFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "Q-learning";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new QLearning(domain, rf, tf, gamma, hashingFactory, 0.0, 1.0);
			}
		};
		
		
		LearningAgentFactory rMaxFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "RMax";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new PotentialShapedRMax(domain, rf, tf, gamma, hashingFactory, 0, 1, 0.01, 100);
			}
		};
		
		final PotentialFunction potential = new PotentialFunction() {
			
			@Override
			public double potentialValue(State s) {
				
				if(s.getObjectsOfTrueClass(ModeledDomainGenerator.RMAXFICTIOUSSTATENAME).size() > 0){
					return 0.;
				}
				
				ObjectInstance agent = s.getObjectsOfTrueClass(GridWorldDomain.CLASSAGENT).get(0); //assume one agent
				ObjectInstance location = s.getObjectsOfTrueClass(GridWorldDomain.CLASSLOCATION).get(0); //assume one goal location in state
				
				//get agent position
				int ax = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
				int ay = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
				
				//get location position
				int lx = location.getDiscValForAttribute(GridWorldDomain.ATTX);
				int ly = location.getDiscValForAttribute(GridWorldDomain.ATTY);
				
				//compute Manhattan distance
				double mdist = Math.abs(ax-lx) + Math.abs(ay-ly);
				
				double sum = 0.;
				for(int i = 0; i < mdist; i++){
					sum += -1 * Math.pow(gamma, i);
				}
				
				return sum;
			}
		};
		
		LearningAgentFactory rMaxShapedFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "RMax Shaped";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new PotentialShapedRMax(domain, rf, tf, gamma, hashingFactory, potential, 1, 0.01, 100);
			}
		};
		
		
		LearningAgentFactory artdpFactory = new LearningAgentFactory() {
			
			@Override
			public String getAgentName() {
				return "ARTDP";
			}
			
			@Override
			public LearningAgent generateAgent() {
				return new ARTDP(domain, rf, tf, gamma, hashingFactory, 0);
			}
		};
		
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, this.gwdg.getMap());
		VisualActionObserver ob = new VisualActionObserver(domain, v);
		//ob.setFrameDelay(33);
		//ob.initGUI();
		//this.domain.addActionObserverForAllAction(ob);
		
		LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter(domain, rf, new ConstantStateGenerator(initialState), 10, 100, artdpFactory, rMaxFactory);
		//exp.toggleVisualPlots(false);
		exp.setUpPlottingConfiguration(500, 250, 2, 1000, TrialMode.MOSTRECENTANDAVERAGE, PerformanceMetric.CUMULATIVESTEPSPEREPISODE);
		
		exp.startExperiment();
		
	}

}
