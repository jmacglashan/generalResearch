package classexercises.vi;

import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class VITester {

	GridWorldDomain 			gwdg;
	Domain						domain;
	StateParser 				sp;
	RewardFunction 				rf;
	TerminalFunction			tf;
	State 						initialState;
	DiscreteStateHashFactory	hashingFactory;
	
	
	public VITester() {
		
		gwdg = new GridWorldDomain(11, 11); //create an 11x11 grid world
		//gwdg.setProbSucceedTransitionDynamics(0.8); //optional to make transition dynamics stochastic
		gwdg.setMapToFourRooms(); //will use the standard four rooms layout
		domain = gwdg.generateDomain();
		sp = new GridWorldStateParser(domain); //for writing states to a file
		
		rf = new UniformCostRF(); //reward always returns -1 (no positive reward on goal state either; but since the goal state ends action it will still be favored)
		tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION)); //ends when the agent reaches any location
		
		//set up the initial state
		initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
		GridWorldDomain.setAgent(initialState, 0, 0);
		GridWorldDomain.setLocation(initialState, 0, 10, 10); //a goal location at 10, 10
		
		//set up the state hashing system
		//this class will compute a hash value based on the discrete values of the attributes of objects
		hashingFactory = new DiscreteStateHashFactory();
		
		//in particular, tell the hashing function to compute hash codes with respect to the attributes of the agent class only
		//when computing hash values this will ignore the attributes of the location objects. since location objects cannot be moved
		//by any action, there is no reason to include the in the computation for our task.
		//if the below line was not included, the hashingFactory would use every attribute of every object class
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);
		
		
		
	}

	
	/**
	 * launch an interactive visualizer of our domain/task
	 */
	public void interactive(){
		
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		VisualExplorer exp = new VisualExplorer(domain, v, initialState);
		
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		
		exp.initGUI();
		
		
	}
	
	
	/**
	 * launch an episode viewer for episodes saved to files .
	 * @param outputPath the path to the directory containing the saved episode files
	 */
	public void visualizeEpisode(String outputPath){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gwdg.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	
	/**
	 * This will method will perform VI planning and save a sample of the policy.
	 * @param outputPath the path to the directory in which the policy sample will be saved
	 */
	public void ValueIterationExample(String outputPath){
		
		//for consistency make sure the path ends with a '/'
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		OOMDPPlanner planner = new VIInClass(domain, rf, tf, 0.99, hashingFactory, 0.001);
		
		
		//run planner from our initial state
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "VIResult", sp);
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		VITester tester = new VITester();
		
		String outputPath = "output"; //directory to record results
		
		//tester.interactive(); //uncomment to run the interactive visualizer
		
		tester.ValueIterationExample(outputPath); //performs planning and save a policy sample in outputPath
		tester.visualizeEpisode(outputPath); //visualizers the policy sample

	}

}
