package domain.singleagent.dogtraining;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldDomain.AtLocationPF;
import burlap.domain.singleagent.gridworld.GridWorldDomain.MovementAction;
import burlap.domain.singleagent.gridworld.GridWorldDomain.WallToPF;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class DogTraining implements DomainGenerator {

	//Constants
	public static final String							ATTX = "x";
	public static final String							ATTY = "y";
	public static final String							ATTWAITING = "waiting";
	public static final String							ATTLOOKING = "looking";
	public static final String							ATTHOLDING = "holding";
	public static final String							ATTLOCID = "locID";
	
	public static final String							CLASSDOG = "dog";
	public static final String							CLASSLOCATION = "location";
	public static final String							CLASSTOY = "toy";
	
	public static final String							ACTIONNORTH = "north";
	public static final String							ACTIONSOUTH = "south";
	public static final String							ACTIONEAST = "east";
	public static final String							ACTIONWEST = "west";
	public static final String							ACTIONWAIT = "wait";
	public static final String							ACTIONPICKUP = "pickup";
	public static final String							ACTIONPUTDOWN = "putdown";
	
	public static final String							PFWALLNORTH = "wallToNorth";
	public static final String							PFWALLSOUTH = "wallToSouth";
	public static final String							PFWALLEAST = "wallToEast";
	public static final String							PFWALLWEST = "wallToWest";
	public static final String							PFHASTOY = "hasToy";
	public static final String							PFHASATOY = "hasAToy";
	public static final String							PFNOTOY = "hasNoToy";
	public static final String							PFDOGAT = "dogAt";
	public static final String							PFTOYAT = "toyAt";
	public static final String							PFWAIT = "waiting";
	
	public static final String							LIDRED = "red";
	public static final String							LIDGREEN = "green";
	public static final String							LIDBLUE = "blue";
	public static final String							LIDHOME = "home";
	
	public static final String							PFCLASSLOC = "dogLoc";
	public static final String							PFCLASSTLOC = "toyLoc";
	public static final String							PFCLASSWALL = "wall";
	public static final String							PFCLASSID = "locid";
	public static final String							PFCLASSTC = "toyCheck";
	public static final String							PFCLASSHT = "hasToy";
	public static final String							PFCLASSWAIT = "waiting";
	
	
	
	//data members
	protected int										width;
	protected int										height;
	protected int [][]									map;
	protected double[][]								transitionDynamics;
	protected boolean									haltOnWait;
	
	
	
	/**
	 * Constructs an empty map with deterministic transitions
	 * @param width width of the map
	 * @param height height of the map
	 */
	public DogTraining(int width, int height, boolean haltOnWait){
		this.width = width;
		this.height = height;
		this.setDeterministicTransitionDynamics();
		this.makeEmptyMap();
		this.haltOnWait = haltOnWait;
	}
	
	
	
	
	
	/**
	 * Constructs a deterministic world based on the provided map.
	 * @param map the first index is the x index, the second the y; 1 entries indicate a wall
	 */
	public DogTraining(int [][] map){
		this.setMap(map);
		this.setDeterministicTransitionDynamics();
	}
	
	
	
	
	/**
	 * Will set the domain to use deterministic action transitions.
	 */
	public void setDeterministicTransitionDynamics(){
		int na = 4;
		transitionDynamics = new double[na][na];
		for(int i = 0; i < na; i++){
			for(int j = 0; j < na; j++){
				if(i != j){
					transitionDynamics[i][j] = 0.;
				}
				else{
					transitionDynamics[i][j] = 1.;
				}
			}
		}
	}
	
	/**
	 * Sets the domain to use probabilistic transitions. Dog will move in the intended direction with probability probSucceed. Dog
	 * will move in a random direction with probability 1 - probSucceed
	 * @param probSucceed probability to move the in intended direction
	 */
	public void setProbSucceedTransitionDynamics(double probSucceed){
		int na = 4;
		double pAlt = (1.-probSucceed)/3.;
		transitionDynamics = new double[na][na];
		for(int i = 0; i < na; i++){
			for(int j = 0; j < na; j++){
				if(i != j){
					transitionDynamics[i][j] = pAlt;
				}
				else{
					transitionDynamics[i][j] = probSucceed;
				}
			}
		}
	}
	
	/**
	 * Will set the movement direction probabilities based on the action chosen. The index (0,1,2,3) indicates the
	 * direction north,south,east,west, respectively and the matrix is organized by transitionDynamics[selectedDirection][actualDirection].
	 * For instance, the probability of the dog moving east when selecting north would be specified in the entry transitionDynamics[0][2]
	 * 
	 * @param transitionDynamics entries indicate the probability of movement in the given direction (second index) for the given action selected (first index).
	 */
	public void setTransitionDynamics(double [][] transitionDynamics){
		this.transitionDynamics = transitionDynamics.clone();
	}
	
	
	/**
	 * Makes the map empty
	 */
	public void makeEmptyMap(){
		this.map = new int[this.width][this.height];
		for(int i = 0; i < this.width; i++){
			for(int j = 0; j < this.height; j++){
				this.map[i][j] = 0;
			}
		}
	}
	
	/**
	 * Set the map of the world.
	 * @param map the first index is the x index, the second the y; 1 entries indicate a wall
	 */
	public void setMap(int [][] map){
		this.width = map.length;
		this.height = map[0].length;
		this.map = map.clone();
	}
	
	
	/**
	 * Creates a horizontal wall.
	 * @param xi The starting x coordinate of the wall
	 * @param xf The ending x coordinate of the wall
	 * @param y The y coordinate of the wall
	 */
	public void horizontalWall(int xi, int xf, int y){
		for(int x = xi; x <= xf; x++){
			this.map[x][y] = 1;
		}
	}
	
	/**
	 * Creates a horizontal wall.
	 * @param yi The stating y coordinate of the wall
	 * @param yf The ending y coordinate of the wall
	 * @param x	The x coordinate of the wall
	 */
	public void verticalWall(int yi, int yf, int x){
		for(int y = yi; y <= yf; y++){
			this.map[x][y] = 1;
		}
	}
	
	public void setObstacleInCell(int x, int y){
		this.map[x][y] = 1;
	}
	
	
	/**
	 * Returns the map being used for the domain
	 * @return the map being used in the domain
	 */
	public int [][] getMap(){
		return this.map.clone();
	}
	
	
	
	@Override
	public Domain generateDomain() {
		
		Domain DOMAIN = new SADomain();
		
		//Creates a new Attribute object
		Attribute xatt = new Attribute(DOMAIN, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, this.width-1, 1); //-1 due to inclusivity vs exclusivity
		
		Attribute yatt = new Attribute(DOMAIN, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, this.height-1, 1); //-1 due to inclusivity vs exclusivity
		
		Attribute watt = new Attribute(DOMAIN, ATTWAITING, Attribute.AttributeType.DISC);
		watt.setDiscValuesForRange(0, 1, 1);
		
		Attribute latt = new Attribute(DOMAIN, ATTLOOKING, Attribute.AttributeType.DISC);
		latt.setDiscValuesForRange(0, 4, 1);
		
		Attribute hatt = new Attribute(DOMAIN, ATTHOLDING, Attribute.AttributeType.DISC);
		hatt.setDiscValuesForRange(0, this.width*this.height - 1, 1);
		
		Attribute locAtt = new Attribute(DOMAIN, ATTLOCID, Attribute.AttributeType.DISC);
		locAtt.setDiscValues(new String[]{LIDRED, LIDGREEN, LIDBLUE, LIDHOME});
		
		
		
		ObjectClass dogClass = new ObjectClass(DOMAIN, CLASSDOG);
		dogClass.addAttribute(xatt);
		dogClass.addAttribute(yatt);
		dogClass.addAttribute(latt);
		dogClass.addAttribute(watt);
		dogClass.addAttribute(hatt);
		
		ObjectClass toyClass = new ObjectClass(DOMAIN, CLASSTOY);
		toyClass.addAttribute(xatt);
		toyClass.addAttribute(yatt);
		
		ObjectClass locClass = new ObjectClass(DOMAIN, CLASSLOCATION);
		locClass.addAttribute(xatt);
		locClass.addAttribute(yatt);
		locClass.addAttribute(locAtt);
		
		
		Action north = new MovementAction(ACTIONNORTH, DOMAIN, this.transitionDynamics[0]);
		Action south = new MovementAction(ACTIONSOUTH, DOMAIN, this.transitionDynamics[1]);
		Action east = new MovementAction(ACTIONEAST, DOMAIN, this.transitionDynamics[2]);
		Action west = new MovementAction(ACTIONWEST, DOMAIN, this.transitionDynamics[3]);
		Action wait = new WaitAction(ACTIONWAIT, DOMAIN);
		Action pickup = new PickupAction(ACTIONPICKUP, DOMAIN);
		Action putdown = new PutdownAction(ACTIONPUTDOWN, DOMAIN);
		
		PropositionalFunction atLocationPF = new AtLocationPF(PFDOGAT, DOMAIN, new String[]{CLASSDOG, CLASSLOCATION}, PFCLASSLOC);
		PropositionalFunction toyAtLocationPF = new AtLocationPF(PFTOYAT, DOMAIN, new String[]{CLASSTOY, CLASSLOCATION}, PFCLASSTLOC);
		
		PropositionalFunction wallToNorthPF = new WallToPF(PFWALLNORTH, DOMAIN, new String[]{CLASSDOG}, PFCLASSWALL, 0);
		PropositionalFunction wallToSouthPF = new WallToPF(PFWALLSOUTH, DOMAIN, new String[]{CLASSDOG}, PFCLASSWALL, 1);
		PropositionalFunction wallToEastPF = new WallToPF(PFWALLEAST, DOMAIN, new String[]{CLASSDOG}, PFCLASSWALL, 2);
		PropositionalFunction wallToWestPF = new WallToPF(PFWALLWEST, DOMAIN, new String[]{CLASSDOG}, PFCLASSWALL, 3);
		
		PropositionalFunction hasToyPF = new HasToyPF(PFHASTOY, DOMAIN, new String[]{CLASSDOG, CLASSTOY}, PFCLASSHT);
		
		PropositionalFunction hasNoToyPF = new ToyCheck(PFNOTOY, DOMAIN, new String[]{CLASSDOG}, PFCLASSTC, false);
		PropositionalFunction hasANoToyPF = new ToyCheck(PFHASATOY, DOMAIN, new String[]{CLASSDOG}, PFCLASSTC, true);
		
		PropositionalFunction colRedPF = new LocIDPF(LIDRED, DOMAIN, new String[]{CLASSLOCATION}, PFCLASSID, LIDRED);
		PropositionalFunction colGreenPF = new LocIDPF(LIDGREEN, DOMAIN, new String[]{CLASSLOCATION}, PFCLASSID, LIDGREEN);
		PropositionalFunction colBluePF = new LocIDPF(LIDBLUE, DOMAIN, new String[]{CLASSLOCATION}, PFCLASSID, LIDBLUE);
		PropositionalFunction colHomePF = new LocIDPF(LIDHOME, DOMAIN, new String[]{CLASSLOCATION}, PFCLASSID, LIDHOME);
		
		PropositionalFunction waitPF = new WaitPF(PFWAIT, DOMAIN, new String[]{CLASSDOG}, PFCLASSWAIT);
		
		
		return DOMAIN;
	}
	
	
	
	
	
	
	/**
	 * Will return a state object with a single dog object, n location, and m toy objects
	 * @param d the domain object that is used to specify the min/max dimensions
	 * @param n the number of location objects
	 * @param m the number of toy objects
	 * @return a state object with a single dog object, n location objects, and m toy objects
	 */
	public static State getOneDogNLocationNToyState(Domain d, int n, int m){
		
		State s = new State();
		
		for(int i = 0; i < n; i++){
			s.addObject(new ObjectInstance(d.getObjectClass(CLASSLOCATION), CLASSLOCATION+i));
		}
		
		s.addObject(new ObjectInstance(d.getObjectClass(CLASSDOG), CLASSDOG+0));
		
		for(int i = 0; i < m; i++){
			s.addObject(new ObjectInstance(d.getObjectClass(CLASSTOY), CLASSTOY+i));
		}
		
		return s;
	}
	
	
	/**
	 * Sets the first dog object in s to the specified x and y position.
	 * @param s the state with the dog whose position to set
	 * @param x the x position of the dog
	 * @param y the y position of the dog
	 * @param lookDirection the direction the dog is looking ([0, 4] for north,south,east,west,up, respectively)
	 * @param waiting whether the dog is currently waiting (not doing anything)
	 */
	public static void setDog(State s, int x, int y, int lookDirection, int waiting){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSDOG).get(0);
		
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTLOOKING, lookDirection);
		o.setValue(ATTWAITING, waiting);
		o.setValue(ATTHOLDING, 0);
	}
	
	/**
	 * Sets the first dog object in s to the specified x and y position.
	 * @param s the state with the dog whose position to set
	 * @param x the x position of the dog
	 * @param y the y position of the dog
	 * @param lookDirection the direction the dog is looking ([0, 4] for north,south,east,west,up, respectively)
	 * @param waiting whether the dog is currently waiting (not doing anything)
	 * @param holding which ball the dog is holding (index is 1-based, 0 means no ball being held)
	 */
	public static void setDog(State s, int x, int y, int lookDirection, int waiting, int holding){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSDOG).get(0);
		
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTLOOKING, lookDirection);
		o.setValue(ATTWAITING, waiting);
		o.setValue(ATTHOLDING, holding);
	}
	
	
	/**
	 * Sets whether the dog is waiting or not
	 * @param s the state in which to set the dog
	 * @param t which status to set the dog's waiting
	 */
	public static void setDogWaiting(State s, int waiting){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSDOG).get(0);
		o.setValue(ATTWAITING, waiting);
	}
	
	/**
	 * Sets the i'th location object to the specified x and y position
	 * @param s the state with the location object
	 * @param i specifies which location object index to set
	 * @param x the x position of the location
	 * @param y the y position of the location
	 * @param locid the location id ("red", "green", "blue", or "home")
	 */
	public static void setLocation(State s, int i, int x, int y, String locid){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSLOCATION).get(i);
		
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTLOCID, locid);
	}
	
	
	
	/**
	 * Sets the i'th toy object to the specified x and y position
	 * @param s the state with the toy object
	 * @param i specifies which toy object index to set
	 * @param x the x position of the toy
	 * @param y the y position of the toy
	 */
	public static void setToy(State s, int i, int x , int y){
		
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSTOY).get(i);
		
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		
	}
	
	
	/**
	 * Attempts to move the dog into the given position, taking into account walls and blocks
	 * @param the current state
	 * @param the attempted new X position of the dog
	 * @param the attempted new Y position of the dog
	 */
	protected void move(State s, int xd, int yd){
		
		ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
		int dx = dog.getDiscValForAttribute(ATTX);
		int dy = dog.getDiscValForAttribute(ATTY);
		
		if(xd == 1){
			dog.setValue(ATTLOOKING, 2);
		}
		else if(xd == -1){
			dog.setValue(ATTLOOKING, 3);
		}
		
		if(yd == 1){
			dog.setValue(ATTLOOKING, 0);
		}
		else if(yd == -1){
			dog.setValue(ATTLOOKING, 1);
		}
		
		int nx = dx+xd;
		int ny = dy+yd;
		
		//hit wall, so do not change position
		if(nx < 0 || nx >= this.width || ny < 0 || ny >= this.height || this.map[nx][ny] == 1){
			nx = dx;
			ny = dy;
		}
		
		dog.setValue(ATTX, nx);
		dog.setValue(ATTY, ny);
		
		
		int heldToy = dog.getDiscValForAttribute(ATTHOLDING);
		if(heldToy > 0){
			ObjectInstance toy = s.getObjectsOfTrueClass(CLASSTOY).get(heldToy-1);
			toy.setValue(ATTX, nx);
			toy.setValue(ATTY, ny);
		}
		
		dog.setValue(ATTWAITING, 0);
		
	}
	
	
	protected int [] movementDirectionFromIndex(int i){
		
		int [] result = null;
		
		switch (i) {
		case 0:
			result = new int[]{0,1};
			break;
			
		case 1:
			result = new int[]{0,-1};
			break;
			
		case 2:
			result = new int[]{1,0};
			break;
			
		case 3:
			result = new int[]{-1,0};

		default:
			break;
		}
		
		return result;
	}
	
	
	protected int firstToyAtDogLocation(State s){
		ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
		int x = dog.getDiscValForAttribute(ATTX);
		int y = dog.getDiscValForAttribute(ATTY);
		
		List <ObjectInstance> toys = s.getObjectsOfTrueClass(CLASSTOY);
		if(toys.size() == 0){
			return -1;
		}
		for(int i = 0; i < toys.size(); i++){
			ObjectInstance toy = toys.get(i);
			int tx = toy.getDiscValForAttribute(ATTX);
			int ty = toy.getDiscValForAttribute(ATTY);
			if(tx == x && ty == y){
				return i;
			}
		}
		
		return -1;
		
	}
	
	
	protected boolean dogIsWaiting(State s){
		ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
		int w = dog.getDiscValForAttribute(ATTWAITING);
		return w == 1;
	}
	
	
	public class MovementAction extends Action{

		protected double [] directionProbs;
		protected Random rand;
		
		public MovementAction(String name, Domain domain, double [] directions){
			super(name, domain, "");
			this.directionProbs = directions;
			this.rand = RandomFactory.getMapped(0);
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){
			if(DogTraining.this.haltOnWait){
				return !DogTraining.this.dogIsWaiting(s);
			}
			return true;
		}
		
		@Override
		protected State performActionHelper(State st, String[] params) {
			
			double roll = rand.nextDouble();
			double curSum = 0.;
			int dir = 0;
			for(int i = 0; i < directionProbs.length; i++){
				curSum += directionProbs[i];
				if(roll < curSum){
					dir = i;
					break;
				}
			}
			
			int [] dcomps = DogTraining.this.movementDirectionFromIndex(dir);
			DogTraining.this.move(st, dcomps[0], dcomps[1]);
			
			return st;
		}
		
		public List<TransitionProbability> getTransitions(State st, String [] params){
			
			List <TransitionProbability> transitions = new ArrayList<TransitionProbability>();
			for(int i = 0; i < directionProbs.length; i++){
				double p = directionProbs[i];
				if(p == 0.){
					continue; //cannot transition in this direction
				}
				State ns = st.copy();
				int [] dcomps = DogTraining.this.movementDirectionFromIndex(i);
				DogTraining.this.move(ns, dcomps[0], dcomps[1]);
				
				//make sure this direction doesn't actually stay in the same place and replicate another no-op
				boolean isNew = true;
				for(TransitionProbability tp : transitions){
					if(tp.s.equals(ns)){
						isNew = false;
						tp.p += p;
						break;
					}
				}
				
				if(isNew){
					TransitionProbability tp = new TransitionProbability(ns, p);
					transitions.add(tp);
				}
			
				
			}
			
			
			return transitions;
		}
		
		
		
	}
	
	
	
	public class WaitAction extends Action{

		public WaitAction(String name, Domain d){
			super(name, d, "");
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance o = s.getObjectsOfTrueClass(CLASSDOG).get(0);
			o.setValue(ATTWAITING, 1);
			o.setValue(ATTLOOKING, 4);
			
			return s;
		}
		
	}
	
	public class PickupAction extends Action{

		public PickupAction(String name, Domain d){
			super(name, d, "");
		}
		
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			if(DogTraining.this.haltOnWait){
				if(DogTraining.this.dogIsWaiting(s)){
					return false;
				}
			}
			
			ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
			if(dog.getDiscValForAttribute(ATTHOLDING) > 0){
				return false; //already holding something
			}
			int toyIndex = DogTraining.this.firstToyAtDogLocation(s);
			if(toyIndex == -1){
				return false; //nothing to pick up
			}
			return true;
			
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
			if(dog.getDiscValForAttribute(ATTHOLDING) > 0){
				return s; //already holding something
			}
			
			int toyIndex = DogTraining.this.firstToyAtDogLocation(s);
			if(toyIndex == -1){
				return s; //nothing to pick up
			}
			
			dog.setValue(ATTHOLDING, toyIndex+1); //0 indicates not holding; otherwise the object index in true class list -1
			dog.setValue(ATTWAITING, 0);
			
			return s;
		}
		
		
		
	}
	
	public class PutdownAction extends Action{

		public PutdownAction(String name, Domain d){
			super(name, d, "");
		}
		
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			if(DogTraining.this.haltOnWait){
				if(DogTraining.this.dogIsWaiting(s)){
					return false;
				}
			}
			
			ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
			if(dog.getDiscValForAttribute(ATTHOLDING) > 0){
				return true; //is holding something to put down
			}
			return false;
			
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance dog = s.getObjectsOfTrueClass(CLASSDOG).get(0);
			dog.setValue(ATTHOLDING, 0);
			dog.setValue(ATTWAITING, 0);
			return s;
		}
		
		
		
	}
	
	
	
	public class AtLocationPF extends PropositionalFunction{

		public AtLocationPF(String name, Domain domain, String[] params, String pfClassName) {
			super(name, domain, params, pfClassName);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			
			ObjectInstance ob = st.getObject(params[0]);
			ObjectInstance location = st.getObject(params[1]);
			
			int ax = ob.getDiscValForAttribute(ATTX);
			int ay = ob.getDiscValForAttribute(ATTY);
			
			int lx = location.getDiscValForAttribute(ATTX);
			int ly = location.getDiscValForAttribute(ATTY);
			
			if(ax == lx && ay == ly){
				return true;
			}
			
			return false;
		}
		

	}
	
	
	
	public class WallToPF extends PropositionalFunction{

		
		protected int xdelta;
		protected int ydelta;
		
		
		public WallToPF(String name, Domain domain, String[] parameterClasses, String pfClassName, int direction) {
			super(name, domain, parameterClasses, pfClassName);
			int [] dcomps = DogTraining.this.movementDirectionFromIndex(direction);
			xdelta = dcomps[0];
			ydelta = dcomps[1];
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			
			ObjectInstance dog = st.getObject(params[0]);
			
			int cx = dog.getDiscValForAttribute(ATTX) + xdelta;
			int cy = dog.getDiscValForAttribute(ATTY) + ydelta;
			
			if(cx < 0 || cx >= DogTraining.this.width || cy < 0 || cy >= DogTraining.this.height || DogTraining.this.map[cx][cy] == 1){
				return true;
			}
			
			return false;
		}
		
		
	}
	
	
	public class LocIDPF extends PropositionalFunction{

		String targetID;
		
		public LocIDPF(String name, Domain domain, String [] parameterClasses,
				String pfClassName, String targetID) {
			super(name, domain, parameterClasses, pfClassName);
			this.targetID = targetID;
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			
			ObjectInstance loc = st.getObject(params[0]);
			String locID = loc.getStringValForAttribute(ATTLOCID);
			if(locID.equals(targetID)){
				return true;
			}
			
			return false;
		}
		
		
	}
	
	public class HasToyPF extends PropositionalFunction{

		
		public HasToyPF(String name, Domain domain,
				String[] parameterClasses,
				String pfClassName) {
			super(name, domain, parameterClasses, pfClassName);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			
			ObjectInstance dog = st.getObject(params[0]);
			ObjectInstance toy = st.getObject(params[1]);
			
			int heldToy = dog.getDiscValForAttribute(ATTHOLDING);
			if(heldToy == 0){
				return false;
			}
			
			if(st.getObjectsOfTrueClass(CLASSTOY).get(heldToy-1) == toy){
				return true;
			}
			
			return false;
		}
		
	}
	
	
	public class ToyCheck extends PropositionalFunction{

		boolean trueToggle;
		
		public ToyCheck(String name, Domain domain, String[] parameterClasses,
				String pfClassName, boolean trueToggle) {
			super(name, domain, parameterClasses, pfClassName);
			this.trueToggle = trueToggle;
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			ObjectInstance dog = st.getObject(params[0]);
			
			int heldToy = dog.getDiscValForAttribute(ATTHOLDING);
			boolean hasToy = false;
			if(heldToy > 0){
				hasToy = true;
			}
			if(trueToggle == false){
				hasToy = !hasToy;
			}
			
			return hasToy;
		}
		
		
		
		
	}
	
	
	
	public class WaitPF extends PropositionalFunction{
		
		public WaitPF(String name, Domain domain, String[] parameterClasses,
				String pfClassName) {
			super(name, domain, parameterClasses, pfClassName);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			ObjectInstance dog = st.getObject(params[0]);
			int waiting = dog.getDiscValForAttribute(ATTWAITING);
			return waiting==1;
		}
		
		
	}
	
	
	
	
	
	
	
	public static void main(String [] args){
		
		DogTraining dt = new DogTraining(5, 5, true);
		Domain domain = dt.generateDomain();
		
		State initialState = DogTraining.getOneDogNLocationNToyState(domain, 4, 1);
		DogTraining.setDog(initialState, 2, 1, 4, 0);
		
		DogTraining.setLocation(initialState, 0, 2, 0, LIDHOME);
		DogTraining.setLocation(initialState, 1, 0, 4, LIDRED);
		DogTraining.setLocation(initialState, 2, 2, 4, LIDGREEN);
		DogTraining.setLocation(initialState, 3, 4, 4, LIDBLUE);
		
		DogTraining.setToy(initialState, 0, 1, 4);
		
		Visualizer v = DTVisualizer.getVisualizer(domain, dt.map);
		VisualExplorer exp = new VisualExplorer(domain, v, initialState);
		
		
		//use w-s-a-d-x
		exp.addKeyAction("w", ACTIONNORTH);
		exp.addKeyAction("s", ACTIONSOUTH);
		exp.addKeyAction("a", ACTIONWEST);
		exp.addKeyAction("d", ACTIONEAST);
		exp.addKeyAction("e", ACTIONPICKUP);
		exp.addKeyAction("q", ACTIONPUTDOWN);
		exp.addKeyAction("f", ACTIONWAIT);
		
		exp.initGUI();
		
		
	}
	

}
