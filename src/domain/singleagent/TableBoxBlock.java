package domain.singleagent;

import java.util.List;
import java.util.Set;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.informed.Heuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;

public class TableBoxBlock implements DomainGenerator {

	/*
	 * Define attribute name constants
	 */
	
	public static final String							ATTRAT = "robotAt";
	public static final String							ATTHAS = "has";
	public static final String							ATTCOLOR = "color";
	
	/*
	 * Define object class names constants
	 */
	
	public static final String							CLASSROBOT = "robot";
	public static final String							CLASSTABLE = "table";
	public static final String							CLASSBOX = "box";
	public static final String							CLASSBLOCK = "block";
	
	
	/*
	 * Define action name constants
	 */
	
	public static final String							ACTIONPUTINBOX = "putInBox";
	public static final String							ACTIONUNPACK = "unpack";
	public static final String							ACTIONGOTOTABLE = "goToTable";
	public static final String							ACTIONPICKUP = "pickup";
	public static final String							ACTIONPUTDOWN = "putdown";
	
	
	
	
	public TableBoxBlock() {
		
	}

	@Override
	public Domain generateDomain() {
		
		
		//initialize a single action domain object to define our domain
		Domain domain = new SADomain();
		
		/*
		 * Because we're making a relational domain, its a domain in which we should not treat states with different object name identifiers as the same state
		 * like OO-MDPs typically do with non-relational domains. The reason we do not want to do that is because determining if there is a bijection between
		 * relation domains requires solving graph isomorphism which is computationally difficult (thought, but not yet proven, to be NP-complete). Using the
		 * below method call on our domain indicates to planning and learning algorithms that changing the object name identifiers of a state induces a new 
		 * state that is unique from the previous one.
		 */
		domain.setObjectIdentiferDependence(true);
		
		
		//define a single-target relational attribute to indicate at which table the robot is
		Attribute rat = new Attribute(domain, ATTRAT, Attribute.AttributeType.RELATIONAL);
		
		/*
		 * The robot, box, and table will each be able to have objects associated wit them. The table will be able to have objects on it,
		 * the box will be able to have blocks in it, and the robot will be able to carry objects (one at a time for the robot). We will
		 * define a multi-target relational attribute that can be used to specify what each of those classes of objects have.
		 */
		Attribute has = new Attribute(domain, ATTHAS, Attribute.AttributeType.MULTITARGETRELATIONAL);
		
		//blocks will be painted different colors, so create a discrete attribute for specifying the color of the block
		Attribute color = new Attribute(domain, ATTCOLOR, Attribute.AttributeType.DISC);
		
		//set the color attribute to be either red or blue.
		color.setDiscValues(new String[]{"red", "blue"});
		
		
		
		/**
		 * We now will define a class for the robot. The robot can carry items and be at one of any number of tables in the world,
		 * so it will have an attribute for each those properties
		 */
		ObjectClass robot = new ObjectClass(domain, CLASSROBOT);
		robot.addAttribute(rat);
		robot.addAttribute(has);
		
		
		//The table class only needs to indicate which objects are on it (or that it "has")
		ObjectClass table = new ObjectClass(domain, CLASSTABLE);
		table.addAttribute(has);
		
		
		/*
		 * Similarly a box object contains objects. Boxes can be on different tables, but we're going to specifying this information
		 * via the has attribute of the table. We could represent this information in reverse, but the purpose of this domain
		 * is to illustrate the multi-target relational attributes, so we're going to do it this way.
		 */
		ObjectClass box = new ObjectClass(domain, CLASSBOX);
		box.addAttribute(has);
		
		
		/*
		 * Finally, we have block objects whose only property is their color. As with the box class, their position attribute
		 * will be captured by the object that has them.
		 */
		ObjectClass block = new ObjectClass(domain, CLASSBLOCK);
		block.addAttribute(color);
		
		
		
		//create our actions; note that actions will be automatically added to the domain through the constructors
		Action putinbox = new PutInBoxAction(domain);
		Action unpack = new UnpackAction(domain);
		Action gotoTable = new GoToTableAction(domain);
		
		//create pickup for both blocks and boxes
		Action pickupBlock = new PickupAction(domain, CLASSBLOCK);
		Action pickupBox = new PickupAction(domain, CLASSBOX);
		
		Action putdown = new PutdownAction(domain);
		
		
		//return our domain
		return domain;
	}
	
	
	
	/**
	 * Method for returning a state object with the specified number of objects of each class and one robot object.
	 * Note that none of the values for the objects will be set and must be done by the client. Each subsequent object
	 * will be named CLASSNAMEi where i is a unique number for each object from 0 to N-1 where N is the number of objects
	 * of that class. 
	 * @param d the domain object being used
	 * @param nTables the number of table objects
	 * @param nBlocks the number of block objects
	 * @param nBoxes the number of box objects
	 * @return a state with the given numbers of objects and one robot object
	 */
	public static State getCleanState(Domain d, int nTables, int nBlocks, int nBoxes){
		State s = new MutableState();
		
		//create robot
		ObjectInstance robot = new MutableObjectInstance(d.getObjectClass(CLASSROBOT), CLASSROBOT);
		s.addObject(robot);
		
		//create tables
		for(int i = 0; i < nTables; i++){
			ObjectInstance ob = new MutableObjectInstance(d.getObjectClass(CLASSTABLE), CLASSTABLE + i);
			s.addObject(ob);
		}
		
		//create blocks
		for(int i = 0; i < nBlocks; i++){
			ObjectInstance ob = new MutableObjectInstance(d.getObjectClass(CLASSBLOCK), CLASSBLOCK + i);
			s.addObject(ob);
		}
		
		//create boxes
		for(int i = 0; i < nBoxes; i++){
			ObjectInstance ob = new MutableObjectInstance(d.getObjectClass(CLASSBOX), CLASSBOX + i);
			s.addObject(ob);
		}
				
		
		
		return s;
	
	
	}
	
	/**
	 * Action class for putting blocks in a box. Takes two parameters, the block and the box in which to put them.
	 * @author James MacGlashan
	 *
	 */
	public static class PutInBoxAction extends Action{

		/**
		 * Constructor automatically specifies parameters as the block and box classes.
		 * @param d the domain for this action
		 */
		public PutInBoxAction(Domain d){
			super(ACTIONPUTINBOX, d, new String[]{CLASSBLOCK, CLASSBOX});
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){

			/*
			 * In order for this action to be applicable the box has to be on the same table where
			 * the robot is and the block either has to be in the robots hand or on the same table.
			 */
			
			//get robot object; assume only one robot in the world
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//get table where the robot is
			ObjectInstance tableWhereRobotIs = s.getObject(robot.getStringValForAttribute(ATTRAT));
			Set <String> itemsOnTable = tableWhereRobotIs.getAllRelationalTargets(ATTHAS);
			
			//make sure box is on the table
			if(!itemsOnTable.contains(params[1])){
				return false;
			}
			
			//make sure block is either on the table or in robots hand
			if(!itemsOnTable.contains(params[0]) && !robot.getAllRelationalTargets(ATTHAS).contains(params[0])){
				return false;
			}
			
			
			return true; 
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			/*
			 * in this method we will add the block to the contents of the box and remove it
			 * from either the table it was on, or the robots hand
			 */
			
			ObjectInstance box = s.getObject(params[1]);
			
			box.addRelationalTarget(ATTHAS, params[0]);
			
			//get robot object; assume only one robot in the world
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//we don't need to check if the robot was holding the block or not; if we just call the remove method and the robot wasn't holding it,
			//then nothing will happen
			robot.removeRelationalTarget(ATTHAS, params[0]);
			
			//get table where the robot is
			ObjectInstance tableWhereRobotIs = s.getObject(robot.getStringValForAttribute(ATTRAT));
			
			//as before, we don't need to check if the block was there, we can just tell it to be removed.
			tableWhereRobotIs.removeRelationalTarget(ATTHAS, params[0]);
			
			//return the state that we've modified
			return s;
		}
		
		
		
	}
	
	
	/**
	 * Action class for unpacking a block from a box onto the table where the robot currently is. Takes as
	 * parameters the the block to be unpacked and the box from which the block is to be unpacked
	 * @author James MacGlashan
	 *
	 */
	public static class UnpackAction extends Action{

		/**
		 * Automatically specifies the parameters
		 * @param d the domain of the action.
		 */
		public UnpackAction(Domain d){
			super(ACTIONUNPACK, d, new String[]{CLASSBLOCK, CLASSBOX});
		}
		
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			/**
			 * For it to be possible to apply this action, the box must be on a table,
			 * the box must contain the designated block, and the agent must be as the same table
			 */
			
			ObjectInstance box = s.getObject(params[1]);
			
			
			//make sure the box has the block
			if(!box.getAllRelationalTargets(ATTHAS).contains(params[0])){
				return false;
			}
			
			//get the table where the robot is (assume only one robot in the world)
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			ObjectInstance tableWhereRobotIs = s.getObject(robot.getStringValForAttribute(ATTRAT));
			
			/*
			 * make sure the table where the robot is is the same as the table where the box is 
			 * and simultaneously whether the box is at the table versus in the robots hand.
			 */
			if(!tableWhereRobotIs.getAllRelationalTargets(ATTHAS).contains(box.getName())){
				return false;
			}
			
			
			
			return true;
			
		}
		
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			/*
			 * In this method we will remove the block from the object, and add it to the table where the robot is
			 */
			
			ObjectInstance box = s.getObject(params[1]);
			
			//remove from box
			box.removeRelationalTarget(ATTHAS, params[0]);
			
			//get table where the robot is
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			ObjectInstance tableWhereRobotIs = s.getObject(robot.getStringValForAttribute(ATTRAT));
			
			//add block to table
			tableWhereRobotIs.addRelationalTarget(ATTHAS, params[0]);
			
			//return the state that we modified
			return s;
		}
		
		

	}
	
	
	/**
	 * Action class for having the robot go to a designated table.
	 * @author James MacGlashan
	 *
	 */
	public static class GoToTableAction extends Action{

		
		/**
		 * Initializes with automatic parameters for the table where the robot will go.
		 * @param d the domain of the action
		 */
		public GoToTableAction(Domain d){
			super(ACTIONGOTOTABLE, d, CLASSTABLE);
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){
			/*
			 * Make sure the robot isn't already at the targetd table. 
			 */
			
			//get the robot instance
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//check if robot is already there
			if(robot.getStringValForAttribute(ATTRAT).equals(params[0])){
				return false;
			}
			
			return true;
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			/*
			 * Will change the robots position to the designated target
			 */
			
			//get the robot
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//change position
			robot.setValue(ATTRAT, params[0]);
			
			//return the state that we modifiedgo
			return s;
		}
		
		
		
		
	}
	
	
	/**
	 * Action class for picking up a object as long the robot is not already carrying and object and is at the same table
	 * as the object.
	 * @author James MacGlashan
	 *
	 */
	public static class PickupAction extends Action{

		/**
		 * Creates a pickup action for objects of the given class. Action will be named uniquely for the class it picks up.
		 * @param d the domain of the action
		 * @param objectClassToPickup the class of the object this action can pickup
		 */
		public PickupAction(Domain d, String objectClassToPickup){
			super(ACTIONPICKUP + "_" + objectClassToPickup, d, objectClassToPickup);
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			/*
			 * Can only apply this action if the robot is not holding anything and if
			 * the object is at the same table as the robot
			 */
			
			//get robot
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//make sure robot is not holding anything
			if(robot.getAllRelationalTargets(ATTHAS).size() > 0){
				return false;
			}
			
			//get the table of the robot
			ObjectInstance table = s.getObject(robot.getStringValForAttribute(ATTRAT));
			
			//make sure object is at the same table
			if(!table.getAllRelationalTargets(ATTHAS).contains(params[0])){
				return false;
			}
			
			return true;
			
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			/*
			 * Will add the object to what the robot is holding and remove it from the current table of the robot
			 */
			
			//get robot
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//add to what robot is holding (also could have used addRelationalTarget method)
			robot.setValue(ATTHAS, params[0]);
			
			//get table of the robot
			ObjectInstance table = s.getObject(robot.getStringValForAttribute(ATTRAT));
			
			//remove the object from the table
			table.removeRelationalTarget(ATTHAS, params[0]);
			
			//return modified state
			return s;
		}
		

		
	}
	
	/**
	 * Action class for putting down the object the robot is currently holding at the table where the robot currently is.
	 * Robot must be holding something to be able to execute the action.
	 * @author James MacGlashan
	 *
	 */
	public static class PutdownAction extends Action{

		/**
		 * Creates a put down action; will putdown whatever object the robot is hold at the table where the robot is.
		 * @param d the domain of the action
		 */
		public PutdownAction(Domain d){
			super(ACTIONPUTDOWN, d, "");
		}
		
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			/*
			 * Action requires the robot to be holding something 
			 */
			
			//get robot
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//make sure robot is holding something
			if(robot.getAllRelationalTargets(ATTHAS).size() == 0){
				return false;
			}
			
			
			return true;
			
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			/*
			 * Will remove the object from the robots hand and place it on the table
			 */
			
			//get robot
			ObjectInstance robot = s.getFirstObjectOfClass(CLASSROBOT);
			
			//get table of the robot
			ObjectInstance table = s.getObject(robot.getStringValForAttribute(ATTRAT));
			
			//put object on table
			table.addRelationalTarget(ATTHAS, robot.getStringValForAttribute(ATTHAS));
			
			//clear the object from the robots hands
			robot.clearRelationalTargets(ATTHAS);
			
			//return the state that we modified
			return s;
		}
		
		
		
	}
	
	
	public static State getDefaultStateWithBox(Domain d){
		
		//get state with 2 tables, 8 blocks and 1 box
		State s = TableBoxBlock.getCleanState(d, 2, 8, 1);
		
		//set robot to be at first table
		s.getFirstObjectOfClass(CLASSROBOT).setValue(ATTRAT, s.getFirstObjectOfClass(CLASSTABLE).getName());
		
		//set box to be at first table
		s.getFirstObjectOfClass(CLASSTABLE).addRelationalTarget(ATTHAS, s.getFirstObjectOfClass(CLASSBOX).getName());
		
		//set half the blocks to be at the first table, half to be at the second, the even numebred blocks to red and the odd numebred to be blue
		List<ObjectInstance> blocks = s.getObjectsOfClass(CLASSBLOCK);
		ObjectInstance firstTable = s.getFirstObjectOfClass(CLASSTABLE);
		ObjectInstance secondTable = s.getObjectsOfClass(CLASSTABLE).get(1);
		for(int i = 0; i < blocks.size(); i++){
			
			ObjectInstance block = blocks.get(i);
			if(i % 2 == 0){
				block.setValue(ATTCOLOR, "red");
			}
			else{
				block.setValue(ATTCOLOR, "blue");
			}
			
			if(i < blocks.size() / 2){
				firstTable.addRelationalTarget(ATTHAS, block.getName());
			}
			else{
				secondTable.addRelationalTarget(ATTHAS, block.getName());
			}
			
		}
		
		return s;
		
	}
	
	public static State getDefaultStateWithoutBox(Domain d){
		
		//get state with 2 tables, 8 blocks and 0 boxes
		State s = TableBoxBlock.getCleanState(d, 2, 8, 0);
		
		//set robot to be at first table
		s.getFirstObjectOfClass(CLASSROBOT).setValue(ATTRAT, s.getFirstObjectOfClass(CLASSTABLE).getName());
		
		//set half the blocks to be at the first table, half to be at the second, the even numebred blocks to red and the odd numebred to be blue
		List<ObjectInstance> blocks = s.getObjectsOfClass(CLASSBLOCK);
		ObjectInstance firstTable = s.getFirstObjectOfClass(CLASSTABLE);
		ObjectInstance secondTable = s.getObjectsOfClass(CLASSTABLE).get(1);
		for(int i = 0; i < blocks.size(); i++){
			
			ObjectInstance block = blocks.get(i);
			if(i % 2 == 0){
				block.setValue(ATTCOLOR, "red");
			}
			else{
				block.setValue(ATTCOLOR, "blue");
			}
			
			if(i < blocks.size() / 2){
				firstTable.addRelationalTarget(ATTHAS, block.getName());
			}
			else{
				secondTable.addRelationalTarget(ATTHAS, block.getName());
			}
			
		}
		
		return s;
		
	}
	
	
	/**
	 * Sets up a terminal explorer which prints the value of the state to the terminal and allows the user to
	 * input actions. If an action requires parameters then they should be separated by spaces. For instance:
	 * "putInBox block1 box0"
	 */
	public static void terminalExplorer(){
		
		TableBoxBlock gen = new TableBoxBlock();
		Domain d = gen.generateDomain();
		
		State s = getDefaultStateWithBox(d);
		
		//setup terminal explorer
		TerminalExplorer exp = new TerminalExplorer(d);
		exp.exploreFromState(s);
	
		
	}
	
	
	/**
	 * An example of using A* on this domain (with a null heuristic which effectively makes it
	 * uniform cost search). You can change which input state is used by changing the commented out line.
	 * One input state has a box available to use (which is useful) and the other does not. Moving
	 * between tables takes more energy than packing and unpacking objects from the box, so it
	 * is useful to use the box to move objects between the tables.
	 * 
	 */
	public static void planningExample(){
		
		TableBoxBlock gen = new TableBoxBlock();
		Domain d = gen.generateDomain();
		
		//State s = getDefaultStateWithBox(d);
		State s = getDefaultStateWithoutBox(d);
		
		/*
		 * define a reward function that has a uniform cost for all actions exception changing tables
		 * which takes more energy and has a cost of 10. This makes using the box more appealing.
		 * Note that costs are always represented by negative numbers and that A* always requires
		 * non-positive values to be returned by the reward function.
		 */
		RewardFunction energyUsageReward = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				if(a.action.getName().equals(ACTIONGOTOTABLE)){
					return -10.;
				}
				return -1.;
			}
		};
		
		/*
		 * Define a goal condition that has all red blocks on the first table and
		 * all blue blocks on the second table
		 */
		StateConditionTest goalCondition = new StateConditionTest() {
			
			@Override
			public boolean satisfies(State s) {
				
				/*
				 * the robot cannot be holding anything so first make sure
				 * that the agent isn't
				 */
				if(s.getFirstObjectOfClass(CLASSROBOT).getAllRelationalTargets(ATTHAS).size() > 0){
					return false;
				}
				
				
				//No blocks can be in any box either
				for(ObjectInstance box : s.getObjectsOfClass(CLASSBOX)){
					if(box.getAllRelationalTargets(ATTHAS).size() > 0){
						return false;
					}
				}
				 
				
				
				//get all tables
				List <ObjectInstance> tables = s.getObjectsOfClass(CLASSTABLE);
				ObjectInstance firstTable = tables.get(0);
				ObjectInstance secondTable = tables.get(1);
				
				//are all block objects on the first table red?
				for(String obName : firstTable.getAllRelationalTargets(ATTHAS)){
					ObjectInstance ob = s.getObject(obName);
					if(ob.getObjectClass().name.equals(CLASSBLOCK)){
						if(ob.getStringValForAttribute(ATTCOLOR).equals("blue")){
							return false;
						}
					}
				}
				
				//are all block objects on the second table blue?
				for(String obName : secondTable.getAllRelationalTargets(ATTHAS)){
					ObjectInstance ob = s.getObject(obName);
					if(ob.getObjectClass().name.equals(CLASSBLOCK)){
						if(ob.getStringValForAttribute(ATTCOLOR).equals("red")){
							return false;
						}
					}
				}
				
				
				return true;
			}
		};
		
		//get a null heuristic function
		Heuristic h = new NullHeuristic();
		
		/*
		 * Create a NameDependent StateHashFactory which is the kind of factory that we should be using with relational
		 * domains since we treat states with different object name identifiers as unique states.
		 */
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		
		//create planner
		AStar planner = new AStar(d, energyUsageReward, goalCondition, hashingFactory, h);
		
		//perform planning
		planner.planFromState(s);
		
		//capture plan in static policy
		SDPlannerPolicy policy = new SDPlannerPolicy(planner);
		
		//evaluate the plan/policy from our initial state; wrap a terminal state function around the goal condition
		EpisodeAnalysis ea = policy.evaluateBehavior(s, energyUsageReward, new GoalConditionTF(goalCondition));
		
		//print out the action plan that was created
		System.out.println(ea.getActionSequenceString("\n"));
		
		//print the cost of the plan
		System.out.println("Plan reward cost: " + ea.getDiscountedReturn(1.));
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//terminalExplorer();
		planningExample();

	}

}
