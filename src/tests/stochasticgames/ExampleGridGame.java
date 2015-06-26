package tests.stochasticgames;

import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.explorers.SGTerminalExplorer;
import burlap.oomdp.stochasticgames.explorers.SGVisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

/**
 * @author James MacGlashan.
 */
public class ExampleGridGame {

	public static void main(String[] args) {
		GridGame gg = new GridGame();


		SGDomain d = (SGDomain)gg.generateDomain();

		//State s = GridGame.getCorrdinationGameInitialState(d);
		//State s = GridGame.getTurkeyInitialState(d);
		State s = GridGame.getPrisonersDilemmaInitialState(d);

		//System.out.println(s.getCompleteStateDescription());


		JointActionModel jam = new GridGameStandardMechanics(d);


		/*
		Visualizer v = GGVisualizer.getVisualizer(12, 12);
		SGVisualExplorer exp = new SGVisualExplorer(d, v, s, jam);
		exp.setRewardFunction(new GGJointRewardFunction(d));
		exp.setTerminalFunction(new GGTerminalFunction(d));

		exp.setJAC("c"); //press c to execute the constructed joint action

		exp.addKeyAction("w", GridGame.CLASSAGENT+"0:"+GridGame.ACTIONNORTH);
		exp.addKeyAction("s", GridGame.CLASSAGENT+"0:"+GridGame.ACTIONSOUTH);
		exp.addKeyAction("d", GridGame.CLASSAGENT+"0:"+GridGame.ACTIONEAST);
		exp.addKeyAction("a", GridGame.CLASSAGENT+"0:"+GridGame.ACTIONWEST);
		exp.addKeyAction("q", GridGame.CLASSAGENT+"0:"+GridGame.ACTIONNOOP);

		exp.addKeyAction("i", GridGame.CLASSAGENT+"1:"+GridGame.ACTIONNORTH);
		exp.addKeyAction("k", GridGame.CLASSAGENT+"1:"+GridGame.ACTIONSOUTH);
		exp.addKeyAction("l", GridGame.CLASSAGENT+"1:"+GridGame.ACTIONEAST);
		exp.addKeyAction("j", GridGame.CLASSAGENT+"1:"+GridGame.ACTIONWEST);
		exp.addKeyAction("u", GridGame.CLASSAGENT+"1:"+GridGame.ACTIONNOOP);

		exp.initGUI();

		*/

		SGTerminalExplorer exp = new SGTerminalExplorer(d);
		exp.setRewardFunction(new GridGame.GGJointRewardFunction(d));
		exp.setTerminalFunction(new GGTerminalFunction(d));

		exp.exploreFromState(s);


	}

}
