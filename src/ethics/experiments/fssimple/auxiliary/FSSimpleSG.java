package ethics.experiments.fssimple.auxiliary;

import java.util.List;

import domain.stocasticgames.foragesteal.simple.FSSimple;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;

public class FSSimpleSG extends SGStateGenerator {

	SGDomain domain;
	
	public FSSimpleSG(SGDomain domain){
		this.domain = domain;
	}
	
	@Override
	public State generateState(List<Agent> agents) {
		
		String p0Name = agents.get(0).getAgentName();
		String p1Name = agents.get(1).getAgentName();
		
		State s = FSSimple.getInitialState(this.domain, p0Name, p1Name, 0, 0);
		
		return s;
	}

}
