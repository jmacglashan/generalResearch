package behavior.AMDP.premade.sokoban;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author James MacGlashan.
 */
public class SokoAMDP1 implements DomainGenerator{

	public static final String ATTCONNECTED = "connectedObjects";
	public static final String ATTINREGION = "inRegion";
	public static final String ATTADJACENT = "adjacent";

	public static final String ACTIONTODOOR = "gotoDoor";
	public static final String ACTIONTOROOM = "gotoRoom";

	public static final String ACTIONBLOCKTODOOR = "blockToDoor";
	public static final String ACTIONBLOCKTOROOM = "blockToRoom";

	public static final String ACTIONTOBLOCK = "gotoBlock";


	@Override
	public Domain generateDomain() {

		SADomain domain = new SADomain();

		Attribute conn = new Attribute(domain, ATTCONNECTED, Attribute.AttributeType.MULTITARGETRELATIONAL);
		Attribute inRegion = new Attribute(domain, ATTINREGION, Attribute.AttributeType.RELATIONAL);
		Attribute adj = new Attribute(domain, ATTADJACENT, Attribute.AttributeType.MULTITARGETRELATIONAL);


		ObjectClass agent = new ObjectClass(domain, Sokoban2Domain.CLASSAGENT);
		agent.addAttribute(inRegion);
		agent.addAttribute(adj);

		ObjectClass block = new ObjectClass(domain, Sokoban2Domain.CLASSBLOCK);
		block.addAttribute(inRegion);

		ObjectClass room = new ObjectClass(domain, Sokoban2Domain.CLASSROOM);
		room.addAttribute(conn);

		ObjectClass door = new ObjectClass(domain, Sokoban2Domain.CLASSDOOR);
		door.addAttribute(conn);

		new GotoRegion(ACTIONTODOOR, domain, Sokoban2Domain.CLASSDOOR);
		new GotoRegion(ACTIONTOROOM, domain, Sokoban2Domain.CLASSROOM);

		new TakeBlockToRegion(ACTIONBLOCKTODOOR, domain, Sokoban2Domain.CLASSDOOR);
		new TakeBlockToRegion(ACTIONBLOCKTOROOM, domain, Sokoban2Domain.CLASSROOM);

		new GoToBlock(ACTIONTOBLOCK, domain);


		return domain;
	}

	public static State projectToAMDPState(State s, Domain aDomain){

		State as = new State();

		ObjectInstance aagent = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSAGENT), Sokoban2Domain.CLASSAGENT);
		as.addObject(aagent);


		List<ObjectInstance> rooms = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSROOM);
		for(ObjectInstance r : rooms){
			ObjectInstance ar = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSROOM), r.getName());
			as.addObject(ar);
		}

		List<ObjectInstance> doors = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSDOOR);
		for(ObjectInstance d : doors){
			ObjectInstance ad = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSDOOR), d.getName());
			as.addObject(ad);
		}


		//set agent position
		//first try room
		ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
		int ax = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
		int ay = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);
		ObjectInstance inRoom = Sokoban2Domain.roomContainingPoint(s, ax, ay);

		if(inRoom != null){
			aagent.setValue(ATTINREGION, inRoom.getName());
		}
		else{
			ObjectInstance inDoor = Sokoban2Domain.doorContainingPoint(s, ax, ay);
			aagent.setValue(ATTINREGION, inDoor.getName());
		}


		//now do blocks
		List<ObjectInstance> blocks = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSBLOCK);
		for(ObjectInstance b : blocks){
			ObjectInstance ab = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSBLOCK), b.getName());
			int bx = b.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int by = b.getDiscValForAttribute(Sokoban2Domain.ATTY);

			if(Math.abs(ax-bx) + Math.abs(ay-by) == 1){
				//adjacent
				//ab.addRelationalTarget(ATTADJACENT, aagent.getName());
				aagent.addRelationalTarget(ATTADJACENT, ab.getName());
			}

			ObjectInstance binRoom = Sokoban2Domain.roomContainingPoint(s, bx, by);
			if(binRoom != null){
				ab.setValue(ATTINREGION, binRoom.getName());
			}
			else{
				ObjectInstance binDoor = Sokoban2Domain.doorContainingPoint(s, bx, by);
				ab.setValue(ATTINREGION, binDoor.getName());
			}

			as.addObject(ab);
		}


		//now set room and door connections
		for(ObjectInstance r : rooms){

			int rt = r.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
			int rl = r.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
			int rb = r.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int rr = r.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);

			ObjectInstance ar = as.getObject(r.getName());

			for(ObjectInstance d : doors){

				int dt = d.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
				int dl = d.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
				int db = d.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
				int dr = d.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);

				if(rectanglesIntersect(rt, rl, rb, rr, dt, dl, db, dr)){
					ObjectInstance ad = as.getObject(d.getName());
					ar.addRelationalTarget(ATTCONNECTED, ad.getName());
					ad.addRelationalTarget(ATTCONNECTED, ar.getName());
				}

			}

		}


		return as;

	}


	protected static boolean rectanglesIntersect(int t1, int l1, int b1, int r1, int t2, int l2, int b2, int r2){

		return t2 >= b1 && b2 <= t1 && r2 >= l1 && l2 <= r1;

	}



	public static class GotoRegion extends Action {

		public GotoRegion(String name, Domain domain, String obClass){
			super(name, domain, obClass);
		}


		@Override
		public boolean applicableInState(State s, String[] params) {

			//get the region where the agent currently is
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			ObjectInstance curRegion = s.getObject(agent.getStringValForAttribute(ATTINREGION));

			//is the param connected to this region?
			if(curRegion.getAllRelationalTargets(ATTCONNECTED).contains(params[0])){
				return true;
			}

			return false;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			agent.addRelationalTarget(ATTINREGION, params[0]);
			agent.clearRelationalTargets(ATTADJACENT);
			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}

	public static class TakeBlockToRegion extends Action{


		public TakeBlockToRegion(String name, Domain domain, String obClass){
			super(name, domain, new String[]{Sokoban2Domain.CLASSBLOCK, obClass});

		}


		@Override
		public List<GroundedAction> getAllApplicableGroundedActions(State s){

			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);

			List<String> moveableBlocks = new ArrayList<String>(agent.getAllRelationalTargets(ATTADJACENT));

			List <List <String>> bindings = s.getPossibleBindingsGivenParamOrderGroups(new String[]{this.parameterClasses[1]}, new String[]{"region"});
			List<GroundedAction> res = new ArrayList<GroundedAction>(moveableBlocks.size()*bindings.size());
			for(String b : moveableBlocks){
				for(List<String> bind : bindings){
					String [] params = new String[]{b, bind.get(0)};
					if(this.applicableInState(s, params)){
						GroundedAction gp = new GroundedAction(this, params);
						res.add(gp);
					}
				}
			}

			return res;

		}


		@Override
		public boolean applicableInState(State s, String[] params) {


			//is the agent currently touching the block?
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			Set<String> touchedObjects = agent.getAllRelationalTargets(ATTADJACENT);
			if(!touchedObjects.contains(params[0])){
				return false;
			}

			//get the region where the block currently is
			ObjectInstance block = s.getObject(params[0]);
			ObjectInstance curRegion = s.getObject(block.getStringValForAttribute(ATTINREGION));

			//is the param connected to this region?
			if(curRegion.getAllRelationalTargets(ATTCONNECTED).contains(params[1])){
				return true;
			}

			return false;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance block = s.getObject(params[0]);

			String curBlockRegion = block.getStringValForAttribute(ATTINREGION);
			ObjectInstance regionOb = s.getObject(curBlockRegion);
			block.addRelationalTarget(ATTINREGION, params[1]);

			if(regionOb.getObjectClass().name.equals(Sokoban2Domain.CLASSDOOR)){
				ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
				agent.addRelationalTarget(ATTINREGION, curBlockRegion);
			}

			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}


	}

	public static class GoToBlock extends Action{

		public GoToBlock(String name, Domain domain){
			super(name, domain, Sokoban2Domain.CLASSBLOCK);
		}


		@Override
		public boolean applicableInState(State s, String[] params) {

			//get the region where the agent currently is
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			ObjectInstance curRegion = s.getObject(agent.getStringValForAttribute(ATTINREGION));

			//region where block currently is
			ObjectInstance block = s.getObject(params[0]);
			ObjectInstance curBlockRegion = s.getObject(block.getStringValForAttribute(ATTINREGION));

			//same region?
			if(curRegion.getName().equals(curBlockRegion.getName())){
				return true;
			}

			return false;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);

			agent.clearRelationalTargets(ATTADJACENT);
			agent.addRelationalTarget(ATTADJACENT, params[0]);

			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}


	}





	public static class InRegionGC implements StateConditionTest {

		String roomName;

		public InRegionGC(String roomName){
			this.roomName = roomName;
		}

		@Override
		public boolean satisfies(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			String curRoom = agent.getStringValForAttribute(ATTINREGION);
			if(curRoom.equals(this.roomName)){
				return true;
			}
			return false;
		}
	}

	public static class BlockInRegionGC implements StateConditionTest {

		public List<String> blockNames = new ArrayList<String>();
		public List<String> roomNames = new ArrayList<String>();

		public BlockInRegionGC(String blockName, String roomName){
			this.blockNames.add(blockName);
			this.roomNames.add(roomName);

		}

		public BlockInRegionGC(TrajectoryModule.ConjunctiveGroundedPropTF tf) {

			for(GroundedProp gp : tf.gps){
				this.blockNames.add(gp.params[0]);
				this.roomNames.add(gp.params[1]);
			}

		}


		@Override
		public boolean satisfies(State s) {

			for(int i = 0; i < this.blockNames.size(); i++){
				String bname = this.blockNames.get(i);
				String rname = this.roomNames.get(i);

				ObjectInstance block = s.getObject(bname);
				if(!block.getStringValForAttribute(SokoAMDP1.ATTINREGION).equals(rname)){
					return false;
				}
			}

			return true;
		}


	}


	public static class AgentMoveBlackList extends SokoParamAffordance{

		public AgentMoveBlackList(String associatedActionName) {
			super(associatedActionName);
		}

		@Override
		public boolean filter(State s, GroundedAction ga) {

			GroundedProp gp = this.tf.gps.get(0);
			String goalPF = gp.pf.getName();

			if(goalPF.equals(Sokoban2Domain.PFAGENTINDOOR) || goalPF.equals(Sokoban2Domain.PFAGENTINROOM)){
				return false; //agent movement always allowed
			}

			//otherwise we're in a block moving task, which means we can only move the agent alone if we are not touching the target block
			//and if we're not in the same region as the target block

			String targetBlockName = gp.params[0];
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			if(agent.getAllRelationalTargets(SokoAMDP1.ATTADJACENT).contains(targetBlockName)){
				return true;
			}

			String agentRegion = agent.getStringValForAttribute(SokoAMDP1.ATTINREGION);
			String blockRegion = s.getObject(targetBlockName).getStringValForAttribute(SokoAMDP1.ATTINREGION);

			if(agentRegion.equals(blockRegion)){
				return true;
			}

			return false;
		}
	}

	public static class BlockMoveBlackList extends SokoParamAffordance{

		public BlockMoveBlackList(String associatedActionName) {
			super(associatedActionName);
		}

		@Override
		public boolean filter(State s, GroundedAction ga) {

			String actionTargetBlock = ga.params[0];
			if(!this.goalParameters.contains(actionTargetBlock)){
				return true;
			}

			return false;
		}
	}

	public static class ToBlockBlackList extends SokoParamAffordance{

		public ToBlockBlackList(String associatedActionName) {
			super(associatedActionName);
		}

		@Override
		public boolean filter(State s, GroundedAction ga) {
			String actionTargetBlock = ga.params[0];
			if(!this.goalParameters.contains(actionTargetBlock)){
				return true;
			}


			return false;
		}
	}


	public static void main(String [] args){

		Sokoban2Domain soko = new Sokoban2Domain();
		Domain domain = soko.generateDomain();

		State s = Sokoban2Domain.getClassicState(domain);

		SokoAMDP1 asoko = new SokoAMDP1();
		Domain adomain = asoko.generateDomain();

		State as = SokoAMDP1.projectToAMDPState(s, adomain);

		System.out.println(as.toString());

		//TerminalExplorer exp = new TerminalExplorer(adomain);
		//exp.exploreFromState(as);



		//StateConditionTest gc = new InRegionGC("room1");
		StateConditionTest gc = new BlockInRegionGC("block0", "room1");
		BFS bfs = new BFS(adomain, gc, new NameDependentStateHashFactory());
		bfs.planFromState(as);

		Policy p = new SDPlannerPolicy(bfs);
		EpisodeAnalysis ea = p.evaluateBehavior(as, new UniformCostRF(), new GoalConditionTF(gc), 100);
		System.out.println(ea.getActionSequenceString("\n"));



	}

}
