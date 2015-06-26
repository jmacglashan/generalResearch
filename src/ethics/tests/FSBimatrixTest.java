package ethics.tests;

import java.util.List;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.debugtools.DPrint;
import burlap.domain.stochasticgames.normalform.SingleStageNormalFormGame;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;

public class FSBimatrixTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		SingleStageNormalFormGame game = new SingleStageNormalFormGame(new String[][]{{"forage", "steal"},{"punish", "nothing"}}, 
				new double[][][]{
					{
					{0, 0},
					{-2, 1}
					},
	
					{
					{0, 0},
					{-3, -1}
					}
				});
		
		SGDomain domain = (SGDomain)game.generateDomain();
		JointReward r = game.getJointRewardFunction();
		JointActionModel jam = SingleStageNormalFormGame.getRepatedGameActionModel();
		AgentType at = SingleStageNormalFormGame.getAgentTypeForAllPlayers(domain);
		
		World w = new World(domain, jam, r, new NullTermination(), new FSBSG());
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		//SGQLAgent a0 = new SGQLAgent(domain, 0.99, 0.01, hashingFactory);
		SGNaiveQLAgent a0 = new SGNaiveQLAgent(domain, .99, 0.1, 1., hashingFactory);
		//SGQLAgent a1 = new SGQLAgent(domain, 0.99, 0.01, hashingFactory);
		SGNaiveQLAgent a1 = new SGNaiveQLAgent(domain, .99, 0.1, 1., hashingFactory);
		
		
		a0.joinWorld(w, at);
		a1.joinWorld(w, at);
		
		DPrint.toggleCode(w.getDebugId(), false);
		
		w.runGame(200);
		
		System.out.println("Starting test.");
		DPrint.toggleCode(w.getDebugId(), true);
		w.runGame(100);

	}
	
	
	
	public static class FSBSG extends SGStateGenerator{

		@Override
		public State generateState(List<Agent> agents) {
			
			State s = new MutableState();
			
			ObjectInstance a0 = this.getAgentObjectInstance(agents.get(0));
			a0.setValue(SingleStageNormalFormGame.ATTPN, 0);
			s.addObject(a0);
			
			ObjectInstance a1 = this.getAgentObjectInstance(agents.get(1));
			a1.setValue(SingleStageNormalFormGame.ATTPN, 1);
			s.addObject(a1);
			
			return s;
		}
		
		
		
		
	}

}
