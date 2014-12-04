package behavior.AMDP.premade.sokoban;

import behavior.AMDP.blacklistAffordance.AffordanceBlackList;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoAMDP2 implements DomainGenerator{





	@Override
	public Domain generateDomain() {

		Domain domain = new SADomain();

		Attribute inRegion = new Attribute(domain, SokoAMDP1.ATTINREGION, Attribute.AttributeType.RELATIONAL);

		ObjectClass agent = new ObjectClass(domain, Sokoban2Domain.CLASSAGENT);
		agent.addAttribute(inRegion);

		ObjectClass block = new ObjectClass(domain, Sokoban2Domain.CLASSBLOCK);
		block.addAttribute(inRegion);

		new ObjectClass(domain, Sokoban2Domain.CLASSROOM);


		new AgentToRoomAction(domain);
		new BlockToRoomAction(domain);

		return domain;
	}


	public static State projectFromA0(State s, Domain a2Domain){

		State as = new State();

		ObjectInstance aagent = new ObjectInstance(a2Domain.getObjectClass(Sokoban2Domain.CLASSAGENT), Sokoban2Domain.CLASSAGENT);
		as.addObject(aagent);

		List<ObjectInstance> rooms = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSROOM);
		for(ObjectInstance r : rooms){
			ObjectInstance ar = new ObjectInstance(a2Domain.getObjectClass(Sokoban2Domain.CLASSROOM), r.getName());
			as.addObject(ar);
		}

		//set agent position
		//first try room
		ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
		int ax = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
		int ay = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);
		ObjectInstance inRoom = Sokoban2Domain.roomContainingPoint(s, ax, ay);

		if(inRoom != null){
			aagent.setValue(SokoAMDP1.ATTINREGION, inRoom.getName());
		}

		//if agent isn't in a room, then leave it unset

		//now do blocks
		List<ObjectInstance> blocks = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSBLOCK);
		for(ObjectInstance b : blocks){
			ObjectInstance ab = new ObjectInstance(a2Domain.getObjectClass(Sokoban2Domain.CLASSBLOCK), b.getName());
			int bx = b.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int by = b.getDiscValForAttribute(Sokoban2Domain.ATTY);


			ObjectInstance binRoom = Sokoban2Domain.roomContainingPoint(s, bx, by);
			if(binRoom != null){
				ab.setValue(SokoAMDP1.ATTINREGION, binRoom.getName());
			}

			//if block is not in a room, then leave it unset.


			as.addObject(ab);
		}


		return as;


	}



	public static class AgentToRoomAction extends Action {

		public AgentToRoomAction(Domain domain){
			super(SokoAMDP1.ACTIONTOROOM, domain, new String[]{Sokoban2Domain.CLASSROOM});
		}

		@Override
		public boolean applicableInState(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			if(agent.getStringValForAttribute(SokoAMDP1.ATTINREGION).equals(params[0])){
				return false;
			}
			return true;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			agent.addRelationalTarget(SokoAMDP1.ATTINREGION, params[0]);

			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}

	public static class BlockToRoomAction extends Action {

		public BlockToRoomAction(Domain domain){
			super(SokoAMDP1.ACTIONBLOCKTOROOM, domain, new String[]{Sokoban2Domain.CLASSBLOCK, Sokoban2Domain.CLASSROOM});
		}


		@Override
		public boolean applicableInState(State s, String[] params) {
			ObjectInstance block = s.getObject(params[0]);
			if(block.getStringValForAttribute(SokoAMDP1.ATTINREGION).equals(params[1])){
				return false;
			}
			return true;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance block = s.getObject(params[0]);
			block.addRelationalTarget(SokoAMDP1.ATTINREGION, params[1]);

			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			agent.clearRelationalTargets(SokoAMDP1.ATTINREGION);

			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}


	public static class AgentAffordance extends AffordanceBlackList{

		protected boolean blockTask = false;

		public AgentAffordance(String associatedActionName) {
			super(associatedActionName);
		}

		@Override
		public void setCurrentGoal(StateConditionTest gc) {
			TFGoalCondition tfgc = (TFGoalCondition)gc;
			TrajectoryModule.ConjunctiveGroundedPropTF tf = (TrajectoryModule.ConjunctiveGroundedPropTF)tfgc.getTf();
			GroundedProp gp = tf.gps.get(0);
			if(gp.pf.getName().equals(Sokoban2Domain.PFBLOCKINROOM)){
				this.blockTask = true;
			}
			this.blockTask = false;
		}

		@Override
		public boolean filter(State s, GroundedAction ga) {
			if(this.blockTask){
				return true;
			}
			return false;
		}
	}


	public static class BlockAffordance extends AffordanceBlackList{

		public List<String> blockNames = new ArrayList<String>();
		public List<String> roomNames = new ArrayList<String>();

		public BlockAffordance(String associatedActionName) {
			super(associatedActionName);
		}

		@Override
		public void setCurrentGoal(StateConditionTest gc) {

			TFGoalCondition tfgc = (TFGoalCondition)gc;
			TrajectoryModule.ConjunctiveGroundedPropTF tf = (TrajectoryModule.ConjunctiveGroundedPropTF)tfgc.getTf();

			this.blockNames.clear();
			this.roomNames.clear();

			for(GroundedProp gp : tf.gps){
				this.blockNames.add(gp.params[0]);
				this.roomNames.add(gp.params[1]);
			}

		}

		@Override
		public boolean filter(State s, GroundedAction ga) {

			for(int i = 0; i < blockNames.size(); i++){
				if(blockNames.get(i).equals(ga.params[0])){
					if(roomNames.get(i).equals(ga.params[1])){
						return false;
					}
				}
			}


			return true;
		}
	}


	public static void main(String [] args){


		Sokoban2Domain soko = new Sokoban2Domain();
		Domain domain = soko.generateDomain();

		State s = Sokoban2Domain.getClassicState(domain);

		SokoAMDP2 asoko = new SokoAMDP2();
		Domain adomain = asoko.generateDomain();

		State as = SokoAMDP2.projectFromA0(s, adomain);

		TerminalExplorer exp = new TerminalExplorer(adomain);
		exp.exploreFromState(as);



	}

}
