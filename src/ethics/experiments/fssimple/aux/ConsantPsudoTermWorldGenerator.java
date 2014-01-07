package ethics.experiments.fssimple.aux;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.auxiliary.StateAbstraction;
import burlap.oomdp.auxiliary.common.NullAbstraction;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.WorldGenerator;

public class ConsantPsudoTermWorldGenerator implements WorldGenerator {

	protected SGDomain							domain;
	protected JointActionModel 					worldModel;
	protected JointReward						jointRewardModel;
	protected TerminalFunction					tf;
	protected SGStateGenerator					initialStateGenerator;
	
	protected StateConditionTest				pseduoTerm;
	
	protected StateAbstraction					abstractionForAgents;
	
	
	
	public ConsantPsudoTermWorldGenerator(SGDomain domain, JointActionModel jam, JointReward jr, TerminalFunction tf, SGStateGenerator sg, StateConditionTest pseudoTerm){
		this.CWGInit(domain, jam, jr, tf, sg, new NullAbstraction(), pseudoTerm);
	}
	
	
	
	public ConsantPsudoTermWorldGenerator(SGDomain domain, JointActionModel jam, JointReward jr, TerminalFunction tf, SGStateGenerator sg, StateAbstraction abstractionForAgents, StateConditionTest pseudoTerm){
		this.CWGInit(domain, jam, jr, tf, sg, abstractionForAgents, pseudoTerm);
	}
	
	protected void CWGInit(SGDomain domain, JointActionModel jam, JointReward jr, TerminalFunction tf, SGStateGenerator sg, StateAbstraction abstractionForAgents,
			StateConditionTest pseudoTerm){
		this.domain = domain;
		this.worldModel = jam;
		this.jointRewardModel = jr;
		this.tf = tf;
		this.initialStateGenerator = sg;
		this.abstractionForAgents = abstractionForAgents;
		this.pseduoTerm = pseudoTerm;
	}
	
	@Override
	public World generateWorld() {
		return new PseudoGameCountWorld(domain, worldModel, jointRewardModel, tf, initialStateGenerator, abstractionForAgents, pseduoTerm);
	}

}
