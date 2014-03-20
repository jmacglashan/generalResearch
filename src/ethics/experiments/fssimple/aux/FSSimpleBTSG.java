package ethics.experiments.fssimple.aux;

import java.util.List;

import domain.stocasticgames.foragesteal.simple.FSSimple;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;

public class FSSimpleBTSG extends SGStateGenerator {

	SGDomain domain;
	double probBT;
	
	public FSSimpleBTSG(SGDomain domain, double probBackTurned){
		this.domain = domain;
		this.probBT = probBackTurned;
	}
	
	@Override
	public State generateState(List<Agent> agents) {
		
		String p0Name = agents.get(0).getAgentName();
		String p1Name = agents.get(1).getAgentName();
		
		int bt = 0;
		double r = RandomFactory.getMapped(0).nextDouble();
		if(r < this.probBT){
			bt = 1;
		}
		
		State s = FSSimple.getInitialState(this.domain, p0Name, p1Name, bt, 0);
		
		return s;
	}

}
