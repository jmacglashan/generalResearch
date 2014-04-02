package tests.irltests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class MacroGridWorld extends GridWorldDomain {
	public static final String							MCELL_INDEX = "mcelli";
	public static final String							MCELL_REWARD = "mcellreward";
	public static final String							CLASSMCELL = "mcellclass";
	public static final String							PFINMCELL = "inMacrocell";
	public static final String							PFINREWARDMCELL = "inRewardingMacrocell";
	public static final String							ATTSTEPS = "agentstepsattribute";
	public static final int								MIN_REWARD = 10;
	public static final int								MAX_REWARD = 10;
	public static final int								HEIGHT = 32;
	public static final int								WIDTH = 32;

	public static final int								MCELL_HEIGHT = 16;
	public static final int								MCELL_WIDTH = 16;
	public static final int								MCELL_COUNT = MCELL_HEIGHT*MCELL_WIDTH;
	public static final int								MCELL_FILLED = 5;

	private final int macroCellVerticalCount;
	private final int macroCellHorizontalCount;

	public MacroGridWorld() {
		super(WIDTH, HEIGHT); //default gridworld

		//There are 4 actions (cardinal directions)
		// 30% chance action goes in one of the other 3
		// directions
		this.setProbSucceedTransitionDynamics(.7);
		this.macroCellHorizontalCount = MCELL_WIDTH;
		this.macroCellVerticalCount = MCELL_HEIGHT;
	}

	public MacroGridWorld(int width, int height, int macroCellWidth, int macroCellHeight) {
		super(width, height);
		this.macroCellHorizontalCount = macroCellWidth;
		this.macroCellVerticalCount = macroCellHeight;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public int getMacroCellVerticalCount() {
		return this.macroCellVerticalCount;
	}

	public int getMacroCellHorizontalCount() {
		return this.macroCellHorizontalCount;
	}


	@Override
	public Domain generateDomain() {
		return super.generateDomain();
	}

	/**
	 * returns a state with one agent and all the macro cells set up
	 * 
	 * @param d
	 * @return
	 */
	public static State getOneAgentState(Domain d) {
		State s = new State();
		s.addObject(new ObjectInstance(d.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		return s;
	}

	public static State getRandomInitialState(Domain d) {
		Random r = new Random();
		State s = new State();
		ObjectInstance agent = new ObjectInstance(d.getObjectClass(CLASSAGENT), CLASSAGENT+0);
		agent.setValue(ATTX, r.nextInt(WIDTH));
		agent.setValue(ATTY, r.nextInt(HEIGHT));
		s.addObject(agent);
		return s;
	}

	public static PropositionalFunction[] getPropositionalFunctions(Domain domain) {
		int width = MacroGridWorld.WIDTH / MacroGridWorld.MCELL_WIDTH;
		int height = MacroGridWorld.HEIGHT / MacroGridWorld.MCELL_HEIGHT;
		int count = MacroGridWorld.MCELL_WIDTH * MacroGridWorld.MCELL_HEIGHT;
		PropositionalFunction[] functions = new PropositionalFunction[count];
		int index = 0;
		for (int i = 0; i < MacroGridWorld.MCELL_WIDTH; ++i) {
			int x = i * width;
			for (int j = 0; j < MacroGridWorld.MCELL_HEIGHT; ++j) {
				int y = j * height;
				functions[index] = new InMacroCellPF(domain, x, y, width, height);
				index++;
			}
		}
		return functions;
	}

	public static PropositionalFunction[] getPropositionalFunctions(Domain domain, MacroGridWorld gridWorld) {
		int macroCellWidth = gridWorld.getWidth() / gridWorld.getMacroCellHorizontalCount();
		int macroCellHeight = gridWorld.getHeight() / gridWorld.getMacroCellVerticalCount();
		int count = gridWorld.getMacroCellHorizontalCount() * gridWorld.getMacroCellVerticalCount();
		PropositionalFunction[] functions = new PropositionalFunction[count];
		int index = 0;
		for (int i = 0; i < gridWorld.getMacroCellHorizontalCount(); ++i) {
			int x = i * macroCellWidth;
			for (int j = 0; j < gridWorld.getMacroCellVerticalCount(); ++j) {
				int y = j * macroCellHeight;
				functions[index] = new InMacroCellPF(domain, x, y, macroCellWidth, macroCellHeight);
				index++;
			}
		}
		return functions;
	}

	//wrapper of old reward generation function
	public static Map<String, Double> generateRandomRewards(PropositionalFunction[] functions) {
		return generateRandomRewards(functions, 0);
	}

	public static Map<String, Double> generateRandomRewards(PropositionalFunction[] functions, int numberFilled) {

		Random rando = new Random();
		//reward function generation algorithm from Ng et al
		double[] weights = new double[functions.length];
		int numFilled = 0;
		while (numFilled < 2) {
			numFilled = 0;
			for (int i = 0; i < functions.length; i++) {
				if (rando.nextDouble() > .9) {
					weights[i] = rando.nextDouble();
					numFilled+=1;
				}
				else {
					weights[i] = 0.0;
				}
			}
		}
		//dont forget to renormalize
		double norm = 0.0;
		for (double w : weights) {
			norm += w*w;
		}
		norm = Math.sqrt(norm);
		for (int i = 0; i < functions.length; i++) {
			weights[i] = weights[i]/norm;
		}

		Map<String, Double> rewards = new HashMap<String, Double>();

		for (int i = 0; i < functions.length; i++) {
			rewards.put(functions[i].getName(), weights[i]);
			System.out.println(functions[i].getName() + " reward: " + weights[i]);
		}
		return rewards;
	}


	public static class InMacroCellPF extends PropositionalFunction{
		private int left, right, top, bottom;

		public InMacroCellPF(Domain domain, int x, int y, int width, int height) {
			super("[" + x + ", " + y + "]", domain, "");
			this.left = x;
			this.right = x + width;
			this.bottom = y;
			this.top = y + width;
		}

		@Override
		public boolean isTrue(State state, String[] params) {
			List<ObjectInstance> agents = state.getObjectsOfTrueClass(MacroGridWorld.CLASSAGENT);
			if (agents.size() == 0) {
				return false;
			}
			ObjectInstance agent = agents.get(0);
			int agentX = agent.getDiscValForAttribute(MacroGridWorld.ATTX);
			int agentY = agent.getDiscValForAttribute(MacroGridWorld.ATTY);
			return this.isTrue(agentX, agentY);
		}

		public boolean isTrue(int agentX, int agentY) {
			return (left <= agentX && agentX < right &&
					bottom <= agentY && agentY < top);
		}
	}
}
