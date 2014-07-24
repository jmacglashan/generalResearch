package tests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class ActionSetsTst {

	public static void main(String [] args){
		
		GridWorldDomain dg = new GridWorldDomain(11, 11);
		Domain d = dg.generateDomain();
		
		List<Action> acitons = d.getActions();
		State s1 = GridWorldDomain.getOneAgentNoLocationState(d);
		State s2 = s1.copy();
		
		GridWorldDomain.setAgent(s1, 0, 0);
		GridWorldDomain.setAgent(s2, 2, 2);
		
		Set<GroundedAction> gas = new HashSet<GroundedAction>();
		List<GroundedAction> gas1 = s1.getAllGroundedActionsFor(acitons);
		List<GroundedAction> gas2 = s2.getAllGroundedActionsFor(acitons);
		
		for(GroundedAction ga : gas1){
			gas.add(ga);
		}
		for(GroundedAction ga : gas2){
			gas.add(ga);
		}
		
		for(GroundedAction ga : gas){
			System.out.println(ga.toString());
		}
		
		
	}
	
}
