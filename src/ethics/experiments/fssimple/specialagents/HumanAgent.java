package ethics.experiments.fssimple.specialagents;

import java.util.Map;
import java.util.Scanner;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;



public class HumanAgent extends Agent {

	protected Scanner scanIn;
	
	public HumanAgent(SGDomain domain){
		this.domain = domain;
		scanIn = new Scanner(System.in);;
	}
	
	@Override
	public void gameStarting() {
		
	}

	@Override
	public GroundedSingleAction getAction(State s) {
		System.out.println("-------------------------------------");
		
		System.out.println(s.getCompleteStateDescription());
		
		System.out.println("========");
		System.out.println("Action?");
		String input = this.scanIn.nextLine().trim();
		SingleAction selection = this.domain.getSingleAction(input);
		while(selection == null){
			System.out.println("Cannot find; re-enter");
			input = this.scanIn.nextLine().trim();
			selection = this.domain.getSingleAction(input);
		}
		GroundedSingleAction gsa = new GroundedSingleAction(this.worldAgentName, selection, "");
		
		return gsa;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		
		System.out.println("Joint Action:");
		System.out.println(jointAction.toString());
		System.out.println("Rewards:");
		for(Map.Entry<String, Double> e : jointReward.entrySet()){
			System.out.println(e.getKey() + ": " + e.getValue());
		}
		
	}

	@Override
	public void gameTerminated() {

	}

}
