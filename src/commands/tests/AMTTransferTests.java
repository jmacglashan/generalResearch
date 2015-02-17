package commands.tests;

import burlap.oomdp.core.Domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class AMTTransferTests {

	protected Domain domain;


	public AMTTransferTests(Domain domain){
		this.domain = domain;
	}


	public List<StateAndGoal> getTestsFor(String identifier){

		List<StateAndGoal> tests = new ArrayList<StateAndGoal>();

		if(identifier.startsWith("agent2orange")){
			tests.add(new StateAndGoal(SokoStateTester.agent2Organge_1(this.domain), "agentInRoom(agent0, room1)"));
			tests.add(new StateAndGoal(SokoStateTester.agent2Organge_2(this.domain), "agentInRoom(agent0, room0)"));
			tests.add(new StateAndGoal(SokoStateTester.agent2Organge_3(this.domain), "agentInRoom(agent0, room2)"));
		}
		else if(identifier.startsWith("agent2tan")){
			tests.add(new StateAndGoal(SokoStateTester.agent2Tan_1(this.domain), "agentInRoom(agent0, room2)"));
			tests.add(new StateAndGoal(SokoStateTester.agent2Tan_2(this.domain), "agentInRoom(agent0, room1)"));
			tests.add(new StateAndGoal(SokoStateTester.agent2Tan_3(this.domain), "agentInRoom(agent0, room0)"));
		}
		else if(identifier.startsWith("agent2teal")){
			tests.add(new StateAndGoal(SokoStateTester.agent2Teal_1(this.domain), "agentInRoom(agent0, room0)"));
			tests.add(new StateAndGoal(SokoStateTester.agent2Teal_2(this.domain), "agentInRoom(agent0, room2)"));
			tests.add(new StateAndGoal(SokoStateTester.agent2Teal_3(this.domain), "agentInRoom(agent0, room1)"));
		}
		else if(identifier.startsWith("star2orange")){
			tests.add(new StateAndGoal(SokoStateTester.star2Organge_1(this.domain), "blockInRoom(block0, room1)"));
			tests.add(new StateAndGoal(SokoStateTester.star2Organge_2(this.domain), "blockInRoom(block0, room0)"));
			tests.add(new StateAndGoal(SokoStateTester.star2Organge_3(this.domain), "blockInRoom(block0, room2)"));
		}
		else if(identifier.startsWith("star2tan")){
			tests.add(new StateAndGoal(SokoStateTester.star2Tan_1(this.domain), "blockInRoom(block0, room2)"));
			tests.add(new StateAndGoal(SokoStateTester.star2Tan_2(this.domain), "blockInRoom(block0, room1)"));
			tests.add(new StateAndGoal(SokoStateTester.star2Tan_3(this.domain), "blockInRoom(block0, room0)"));
		}
		else if(identifier.startsWith("star2teal")){
			tests.add(new StateAndGoal(SokoStateTester.star2Teal_1(this.domain), "blockInRoom(block0, room0)"));
			tests.add(new StateAndGoal(SokoStateTester.star2Teal_2(this.domain), "blockInRoom(block0, room2)"));
			tests.add(new StateAndGoal(SokoStateTester.star2Teal_3(this.domain), "blockInRoom(block0, room1)"));
		}


		if(tests.size() > 0){
			return tests;
		}

		throw new RuntimeException("Could not find test for train identifer " + identifier);

	}


}
