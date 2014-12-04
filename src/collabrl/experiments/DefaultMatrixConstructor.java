package collabrl.experiments;

import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldRewardFunction;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import collabrl.MatrixCellEval;
import collabrl.TaskAndTrain;
import collabrl.algevals.MultiStepReturnEval;
import collabrl.algevals.ReturnEval;
import collabrl.lagenerators.ARTDPGen;
import collabrl.lagenerators.QLearnGen;
import collabrl.lagenerators.RMaxGen;
import collabrl.lagenerators.SarsaGen;

import java.util.Random;

/**
 * @author James MacGlashan.
 */
public class DefaultMatrixConstructor {

	protected MatrixCellEval matrix;

	public DefaultMatrixConstructor(){
		this.matrix = new MatrixCellEval(new ReturnEval());
	}

	public DefaultMatrixConstructor(int nTrials){
		this.matrix = new MatrixCellEval(new ReturnEval(nTrials));
	}

	public DefaultMatrixConstructor(int nTrials, int nTimeInervals){
		this.matrix = new MatrixCellEval(new MultiStepReturnEval(nTrials, nTimeInervals));
	}

	public MatrixCellEval getMatrix(){
		return this.matrix;
	}


	public void addAllDefaultAgents(){

		//q learning
		this.addQLearning(false, 0.1, 0.05); // 0
		this.addQLearning(false, 0.1, 0.2);
		this.addQLearning(false, 1.0, 0.05);
		this.addQLearning(false, 1.0, 0.2);
		this.addQLearning(true, 0.1, 0.05);
		this.addQLearning(true, 0.1, 0.2); //5
		this.addQLearning(true, 1.0, 0.05);
		this.addQLearning(true, 1.0, 0.2);

		//sarsa
		this.addSarsa(false, 0.1, 0.05, 0.);
		this.addSarsa(false, 0.1, 0.2, 0.);
		this.addSarsa(false, 1.0, 0.05, 0.); //10
		this.addSarsa(false, 1.0, 0.2, 0.);
		this.addSarsa(true, 0.1, 0.05, 0.);
		this.addSarsa(true, 0.1, 0.2, 0.);
		this.addSarsa(true, 1.0, 0.05, 0.);
		this.addSarsa(true, 1.0, 0.2, 0.); //15
		this.addSarsa(false, 0.1, 0.05, 0.5);
		this.addSarsa(false, 0.1, 0.2, 0.5);
		this.addSarsa(false, 1.0, 0.05, 0.5);
		this.addSarsa(false, 1.0, 0.2, 0.5);
		this.addSarsa(true, 0.1, 0.05, 0.5); //20
		this.addSarsa(true, 0.1, 0.2, 0.5);
		this.addSarsa(true, 1.0, 0.05, 0.5);
		this.addSarsa(true, 1.0, 0.2, 0.5);
		this.addSarsa(false, 0.1, 0.05, 0.9);
		this.addSarsa(false, 0.1, 0.2, 0.9); //25
		this.addSarsa(false, 1.0, 0.05, 0.9);
		this.addSarsa(false, 1.0, 0.2, 0.9);
		this.addSarsa(true, 0.1, 0.05, 0.9);
		this.addSarsa(true, 0.1, 0.2, 0.9);
		this.addSarsa(true, 1.0, 0.05, 0.9); //30
		this.addSarsa(true, 1.0, 0.2, 0.9);

		//artdp
		this.addARTDP(0.01);
		this.addARTDP(0.1);
		this.addARTDP(1.0);


		//RMAX
		this.addRMax(1); //35
		this.addRMax(10);



	}

	public void addAllDefaultTasks(){
		this.addGridWorld(10, true, false, 20000); //0
		this.addGridWorld(10, true, false, 100000);
		this.addGridWorld(10, true, true, 20000);
		this.addGridWorld(10, true, true, 100000);
		this.addGridWorld(10, false, false, 20000);
		this.addGridWorld(10, false, false, 100000); //5
		this.addGridWorld(10, false, true, 20000);
		this.addGridWorld(10, false, true, 100000);

		this.addGridWorld(30, true, false, 100000);
		this.addGridWorld(30, true, false, 500000);
		this.addGridWorld(30, true, true, 100000); //10
		this.addGridWorld(30, true, true, 500000);
		this.addGridWorld(30, false, false, 100000);
		this.addGridWorld(30, false, false, 500000);
		this.addGridWorld(30, false, true, 100000);
		this.addGridWorld(30, false, true, 500000); //15
	}

	public void addAllMaxTimeStepTasks(){

		this.addGridWorld(10, true, false, 100000); //0
		this.addGridWorld(10, true, true, 100000);
		this.addGridWorld(10, false, false, 100000);
		this.addGridWorld(10, false, true, 100000); //3

		this.addGridWorld(30, true, false, 500000);
		this.addGridWorld(30, true, true, 500000);
		this.addGridWorld(30, false, false, 500000);
		this.addGridWorld(30, false, true, 500000); //7

	}


	public void addQLearning(boolean pessimistic, double learningRate, double epsilon){
		QLearnGen agentGen = new QLearnGen(pessimistic, learningRate, epsilon);
		this.matrix.addAgentGenerator(agentGen);
	}

	public void addSarsa(boolean pessimistic, double learningRate, double epsilon, double lambda){
		SarsaGen agentGen = new SarsaGen(pessimistic, learningRate, epsilon, lambda);
		this.matrix.addAgentGenerator(agentGen);
	}

	public void addARTDP(double temperature){
		ARTDPGen agentGen = new ARTDPGen(temperature);
		this.matrix.addAgentGenerator(agentGen);
	}

	public void addRMax(int c){
		RMaxGen agentGen = new RMaxGen(c);
		this.matrix.addAgentGenerator(agentGen);
	}

	public void addGridWorld(final int width, boolean deterministic, boolean free, final int numTrainingSteps){
		GridWorldDomain gwd = new GridWorldDomain(width, width);
		final Domain domain = gwd.generateDomain();
		gwd.makeEmptyMap();
		if(!deterministic){
			gwd.setProbSucceedTransitionDynamics(0.8);
		}

		final int gxy = width / 2;
		TerminalFunction tf = new GridWorldTerminalFunction(gxy, gxy);

		double rMin = -1;

		RewardFunction rf = null;
		if(free){
			rf = new GridWorldRewardFunction(width, width, -1);
			((GridWorldRewardFunction)rf).setReward(gxy, gxy, 10);
			rMin = -5;
		}
		else{
			rf = new GridWorldPuddleRF(gxy, gxy);
		}

		StateGenerator sg = new StateGenerator() {
			@Override
			public State generateState() {

				Random r = RandomFactory.getMapped(0);
				State s = GridWorldDomain.getOneAgentNoLocationState(domain);

				int ax = r.nextInt(width);
				int ay = r.nextInt(width);
				while(ax == gxy && ay == gxy){
					ax = r.nextInt(width);
					ay = r.nextInt(width);
				}

				GridWorldDomain.setAgent(s, ax, ay);

				return s;
			}

		};

		String detString = "deterministic";
		if(!deterministic){
			detString = "stohcastic";
		}

		String freeString = "free";
		if(!free){
			freeString = "puddles";
		}

		String description = "gw_" + width + "_" + detString + "_" + freeString + "_" + numTrainingSteps;

		double vMin = rMin / (1. - 0.99);

		TaskAndTrain task = new TaskAndTrain(domain, rf, tf, 0.99, sg, vMin, 10., numTrainingSteps, true, new String[]{GridWorldDomain.CLASSAGENT}, description);

		this.matrix.addTask(task);

	}











	public static class GridWorldPuddleRF implements RewardFunction{

		int gx;
		int gy;

		public GridWorldPuddleRF(int gx, int gy){
			this.gx = gx;
			this.gy = gy;
		}

		@Override
		public double reward(State s, GroundedAction a, State sprime) {

			ObjectInstance agent = sprime.getFirstObjectOfClass(GridWorldDomain.CLASSAGENT);
			int ax = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
			int ay = agent.getDiscValForAttribute(GridWorldDomain.ATTY);

			if(ax == gx && ay == gy){
				return 10;
			}
			if(isPuddle(ax, ay)){
				return -5;
			}

			return -1;
		}


		public static boolean isPuddle(int x, int y){
			if(y % 2 == 0){
				//then puddle on odd
				if(x % 2 == 1){
					return true;
				}
				return false;
			}
			else{
				//then puddle on even
				if(x % 2 == 0){
					return true;
				}
				return false;
			}
		}
	}

}
