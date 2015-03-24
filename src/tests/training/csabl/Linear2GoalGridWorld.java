package tests.training.csabl;

import auxiliary.DynamicVisualFeedbackEnvironment;
import behavior.learning.DomainEnvironmentWrapper;
import behavior.training.DynamicFeedbackGUI;
import behavior.training.taskinduction.strataware.CSABLAgent;
import burlap.behavior.singleagent.learnbydemo.mlirl.commonrfs.LinearStateDifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import sun.management.resources.agent;

/**
 * @author James MacGlashan.
 */
public class Linear2GoalGridWorld {

	GridWorldDomain gwd;
	Domain domain;
	Visualizer v;
	State initialState;

	public Linear2GoalGridWorld(){

		this.gwd = new GridWorldDomain(11, 11);
		this.gwd.horizontalWall(0, 10, 1);

		this.domain = gwd.generateDomain();

		this.v = GridWorldVisualizer.getVisualizer(this.gwd.getMap());

		this.initialState = GridWorldDomain.getOneAgentNoLocationState(this.domain);
		GridWorldDomain.setAgent(this.initialState, 5, 0);


	}


	public void launchExplorer(){
		VisualExplorer exp = new VisualExplorer(this.domain, this.v, this.initialState);
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		exp.initGUI();
	}

	public void interactiveTest(){

		DynamicVisualFeedbackEnvironment env = new DynamicVisualFeedbackEnvironment(domain);
		Domain domainEnvWrapper = (new DomainEnvironmentWrapper(domain, env)).generateDomain();

		RewardFunction trainerRF = env.getEnvRewardFunction();
		TerminalFunction trainerTF = env.getEnvTerminalFunction();

		DynamicFeedbackGUI gui = new DynamicFeedbackGUI(this.v, env);
		env.setGUI(gui);

		StateToFeatureVectorGenerator rfFV = new BinaryPositionFV(new IntPair(0, 0), new IntPair(10, 0));
		DifferentiableRF learningRF = new LinearStateDifferentiableRF(rfFV, rfFV.generateFeatureVectorFrom(this.initialState).length);

		double lr = 0.1;
		double beta = 3.;
		CSABLAgent agent = new CSABLAgent(domainEnvWrapper, this.domain, beta, 0.1, learningRF, trainerRF, trainerTF, new DiscreteStateHashFactory(), 0.1, 0.1, lr, 11, 1);


		boolean hasInitedGUI = false;
		for(int i = 0; i < 20; i++){
			env.setCurStateTo(this.initialState);
			if(!hasInitedGUI){
				hasInitedGUI = true;
				gui.initGUI();
				gui.launch();
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("Starting episode");
			//now start learning episode
			agent.runLearningEpisodeFrom(this.initialState);
		}

	}


	public static class BinaryPositionFV implements StateToFeatureVectorGenerator{

		IntPair [] locations;

		public BinaryPositionFV(IntPair...locations){
			this.locations = locations;
		}

		public int dim(){
			return locations.length;
		}

		@Override
		public double[] generateFeatureVectorFrom(State s) {

			ObjectInstance agent = s.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getDiscValForAttribute(GridWorldDomain.ATTY);

			double [] vec = new double[this.locations.length];
			for(int i = 0; i < vec.length; i++){
				if(ax == locations[i].x && ay == locations[i].y){
					vec[i] = 1.;
					break;
				}
			}

			return vec;
		}
	}


	public static class IntPair{

		public int x;
		public int y;

		public IntPair(int x, int y){
			this.x = x;
			this.y = y;
		}

	}

	public static void main(String[] args) {
		Linear2GoalGridWorld exp = new Linear2GoalGridWorld();
		//exp.launchExplorer();
		exp.interactiveTest();
	}

}
