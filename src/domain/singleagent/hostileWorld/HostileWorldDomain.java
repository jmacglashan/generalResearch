package domain.singleagent.hostileWorld;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import behavior.vfa.heterogenousafd.stateenumerators.HashingFactoryEnumerator;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.LandmarkColorBlendInterpolation;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D.PolicyGlyphRenderStyle;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.StateValuePainter2D;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class HostileWorldDomain implements DomainGenerator {


	public static final String							ATTX = "x";
	public static final String							ATTY = "y";
	public static final String							ATTHEALTH = "health";
	public static final String							ATTLOCTYPE = "locType";
	
	public static final String							CLASSAGENT = "agent";
	public static final String							CLASSLOC = "location";
	public static final String							CLASSPREDATOR = "predator";
	
	

	public static final String							ACTIONNORTH = "north";
	public static final String							ACTIONSOUTH = "south";
	public static final String							ACTIONEAST = "east";
	public static final String							ACTIONWEST = "west";
	public static final String							ACTIONNOP = "noop";
	public static final String							ACTIONFIGHT = "fight";
	
	
	
	protected int 										width = 8;
	protected int 										height = 8;
	protected int										maxHealth = 15;
	protected int										fightCost = -3;
	protected int										thornsCost = -2;
	protected int										hungerDrain = -1;
	protected int										foodGain = 1;
	protected double									probPredator = 1./3.;
	protected double									baseProbCatch = 0.1;
	protected double									baseProbToLose = 0.1;
	
	protected Random									rand = RandomFactory.getMapped(0);
	
	
	
	
	
	
	public int getWidth() {
		return width;
	}


	public void setWidth(int width) {
		this.width = width;
	}


	public int getHeight() {
		return height;
	}


	public void setHeight(int height) {
		this.height = height;
	}


	public int getMaxHealth() {
		return maxHealth;
	}


	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}


	public int getFightCost() {
		return fightCost;
	}


	public void setFightCost(int fightCost) {
		this.fightCost = fightCost;
	}


	public int getThornsCost() {
		return thornsCost;
	}


	public void setThornsCost(int thornsCost) {
		this.thornsCost = thornsCost;
	}


	public int getHungerDrain() {
		return hungerDrain;
	}


	public void setHungerDrain(int hungerDrain) {
		this.hungerDrain = hungerDrain;
	}


	public int getFoodGain() {
		return foodGain;
	}


	public void setFoodGain(int foodGain) {
		this.foodGain = foodGain;
	}


	public double getProbPredator() {
		return probPredator;
	}


	public void setProbPredator(double probPredator) {
		this.probPredator = probPredator;
	}


	public double getBaseProbCatch() {
		return baseProbCatch;
	}


	public void setBaseProbCatch(double baseProbCatch) {
		this.baseProbCatch = baseProbCatch;
	}


	public double getBaseProbToLose() {
		return baseProbToLose;
	}


	public void setBaseProbToLose(double baseProbToLose) {
		this.baseProbToLose = baseProbToLose;
	}

	

	@Override
	public Domain generateDomain() {
		
		Domain domain = new SADomain();
		
		Attribute xatt = new Attribute(domain, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, this.width-1, 1);
		
		Attribute yatt = new Attribute(domain, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, this.height-1, 1);
		
		Attribute hatt = new Attribute(domain, ATTHEALTH, Attribute.AttributeType.DISC);
		hatt.setDiscValuesForRange(0, this.maxHealth, 1);
		
		Attribute latt = new Attribute(domain, ATTLOCTYPE, Attribute.AttributeType.DISC);
		latt.setDiscValuesForRange(0, 1, 1);
		
		ObjectClass agentClass = new ObjectClass(domain, CLASSAGENT);
		agentClass.addAttribute(xatt);
		agentClass.addAttribute(yatt);
		agentClass.addAttribute(hatt);
		
		ObjectClass locationClass = new ObjectClass(domain, CLASSLOC);
		locationClass.addAttribute(xatt);
		locationClass.addAttribute(yatt);
		locationClass.addAttribute(latt);
		
		ObjectClass predatorClass = new ObjectClass(domain, CLASSPREDATOR);
		predatorClass.addAttribute(xatt);
		predatorClass.addAttribute(yatt);
		
		
		Action northAction = new MovementAction(ACTIONNORTH, domain, 0, 1);
		Action southAction = new MovementAction(ACTIONSOUTH, domain, 0, -1);
		Action eastAction = new MovementAction(ACTIONEAST, domain, 1, 0);
		Action westAction = new MovementAction(ACTIONWEST, domain, -1, 0);
		//Action noopAction = new MovementAction(ACTIONNOP, domain, 0, 0);
		
		Action fightAction = new FightAction(ACTIONFIGHT, domain);
		
		return domain;
		
	}
	
	
	public static void setAgent(State s, int x, int y, int h){
		ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
		agent.setValue(ATTX, x);
		agent.setValue(ATTY, y);
		agent.setValue(ATTHEALTH, h);
	}
	
	public static void setLocation(State s, int i, int x, int y, int t){
		ObjectInstance l = s.getObjectsOfTrueClass(CLASSLOC).get(i);
		l.setValue(ATTX, x);
		l.setValue(ATTY, y);
		l.setValue(ATTLOCTYPE, t);
	}
	
	public static State getState(Domain domain, int nl){
		State s = new State();
		
		ObjectInstance agent = new ObjectInstance(domain.getObjectClass(CLASSAGENT), CLASSAGENT);
		s.addObject(agent);
		
		for(int i = 0; i < nl; i++){
			ObjectInstance l = new ObjectInstance(domain.getObjectClass(CLASSLOC), CLASSLOC+i);
			s.addObject(l);
		}
		
		return s;
	}
	
	
	protected void runStep(State s, Domain domain, int xdelta, int ydelta){
		
		ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
		
		int ax = agent.getDiscValForAttribute(ATTX);
		int ay = agent.getDiscValForAttribute(ATTY);
		int ah = agent.getDiscValForAttribute(ATTHEALTH);
		
		if(ah == 0){
			return; //agent cannot do anything when they're dead!
		}
		
		int nx = ax+xdelta;
		int ny = ay+ydelta;
		int nh = ah;
		
		
		//clamp
		if(nx < 0){
			nx = 0;
		}
		if(nx >= this.width){
			nx = this.width-1;
		}
		
		if(ny < 0){
			ny = 0;
		}
		if(ny >= this.height){
			ny = this.height-1;
		}
		
		List<ObjectInstance> locations = s.getObjectsOfTrueClass(CLASSLOC);
		List<ObjectInstance> foodSources = this.filterLocType(locations, 0);
		
		//handle hunger
		if(this.inALocation(foodSources, nx, ny)){
			nh = Math.min(nh + this.foodGain, this.maxHealth);
		}
		else{
			nh += this.hungerDrain;
		}
		
		//handle thorns
		List<ObjectInstance> thorns = this.filterLocType(locations, 1);
		if(this.inALocation(thorns, nx, ny)){
			nh += this.thornsCost;
		}
		
		
		//handle predator scenarios
		ObjectInstance predator = s.getFirstObjectOfClass(CLASSPREDATOR);
		if(predator == null){
			//does one appear?
			double r = this.rand.nextDouble();
			if(r <= this.probPredator){
				//he appears in agent's previous location
				predator = new ObjectInstance(domain.getObjectClass(CLASSPREDATOR), CLASSPREDATOR);
				predator.setValue(ATTX, ax);
				predator.setValue(ATTY, ay);
				s.addObject(predator);
			}
			
		}
		else{
			//predator exists, what happens?
			
			//fight time?
			if(this.sameLocation(predator, nx, ny)){
				
				double r = this.rand.nextDouble();
				double probToLose = this.probToLose(nh);
				if(r < probToLose){
					nh = 0;
				}
				else{
					//otherwise win at cost
					nh = Math.max(0, nh+this.fightCost);
					s.removeObject(predator);
				}
				
			}
			else{
				boolean lostPredator = true;
				int px = predator.getDiscValForAttribute(ATTX);
				int py = predator.getDiscValForAttribute(ATTY);
				
				if(Math.abs(nx-px) != 2 && Math.abs(ny-py) != 2){
					lostPredator = false;
				}
				else{
					//then chance to lose
					double r = this.rand.nextDouble();
					double probToBeCaught = this.probToBeCaught(nh);
					if(r < probToBeCaught){
						lostPredator = false;
					}
					
				}
				if(lostPredator){
					s.removeObject(predator);
				}
				else{
					predator.setValue(ATTX, nx);
					predator.setValue(ATTY, ny);
				}
			}
			
		}
		
		nh = Math.max(nh, 0);
		
		agent.setValue(ATTX, nx);
		agent.setValue(ATTY, ny);
		agent.setValue(ATTHEALTH, nh);
		
		
		
	}
	
	
	
	protected List<TransitionProbability> getFailSucceedOutcomes(State s, Domain domain, int xdelta, int ydelta){
		
		List<TransitionProbability> tps = new ArrayList<TransitionProbability>(2);
		State success = s.copy();
		State fail = s.copy();
		
		
		ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
		ObjectInstance sucAgent = success.getFirstObjectOfClass(CLASSAGENT);
		ObjectInstance failAgent = fail.getFirstObjectOfClass(CLASSAGENT);
		
		int ax = agent.getDiscValForAttribute(ATTX);
		int ay = agent.getDiscValForAttribute(ATTY);
		int ah = agent.getDiscValForAttribute(ATTHEALTH);
		
		if(ah == 0){
			TransitionProbability endTP = new TransitionProbability(success, 1.);
			tps.add(endTP);
			return tps;
		}
		
		int nx = ax+xdelta;
		int ny = ay+ydelta;
		int nh = ah;
		
		
		//clamp
		if(nx < 0){
			nx = 0;
		}
		if(nx >= this.width){
			nx = this.width-1;
		}
		
		if(ny < 0){
			ny = 0;
		}
		if(ny >= this.height){
			ny = this.height-1;
		}
		
		List<ObjectInstance> locations = s.getObjectsOfTrueClass(CLASSLOC);
		List<ObjectInstance> foodSources = this.filterLocType(locations, 0);
		
		//handle hunger
		if(this.inALocation(foodSources, nx, ny)){
			nh = Math.min(nh + this.foodGain, this.maxHealth);
		}
		else{
			nh += this.hungerDrain;
		}
		
		//handle thorns
		List<ObjectInstance> thorns = this.filterLocType(locations, 1);
		if(this.inALocation(thorns, nx, ny)){
			nh += this.thornsCost;
		}
		
		
		sucAgent.setValue(ATTX, nx);
		sucAgent.setValue(ATTY, ny);
		
		failAgent.setValue(ATTX, nx);
		failAgent.setValue(ATTY, ny);
		
		
		//handle predator scenarios
		ObjectInstance predator = s.getFirstObjectOfClass(CLASSPREDATOR);
		if(predator == null){
			
			TransitionProbability sucTP = new TransitionProbability(success, 1.-this.probPredator);
		
			//he appears in agent's previous location
			predator = new ObjectInstance(domain.getObjectClass(CLASSPREDATOR), CLASSPREDATOR);
			predator.setValue(ATTX, ax);
			predator.setValue(ATTY, ay);
			fail.addObject(predator);
			
			
			sucAgent.setValue(ATTHEALTH, Math.max(nh, 0));
			failAgent.setValue(ATTHEALTH, Math.max(nh, 0));
			
			
			TransitionProbability failTP = new TransitionProbability(fail, this.probPredator);
			tps.add(sucTP);
			tps.add(failTP);
			
			return tps;
			
			
		}
		else{
			//predator exists, what happens?
			
			//fight time?
			if(this.sameLocation(predator, nx, ny)){
				
				sucAgent.setValue(ATTHEALTH, Math.max(0, nh+this.fightCost));
				failAgent.setValue(ATTHEALTH, 0);
				success.removeObject(fail.getFirstObjectOfClass(CLASSPREDATOR)); //succcess predator is gone
			
				double probToLose = this.probToLose(nh);
				TransitionProbability sucTP = new TransitionProbability(success, 1.-probToLose);
				TransitionProbability failTP = new TransitionProbability(fail, probToLose);
				
				tps.add(sucTP);
				tps.add(failTP);
				
				return tps;
				
				
			}
			else{
				
				sucAgent.setValue(ATTHEALTH, Math.max(nh, 0));
				failAgent.setValue(ATTHEALTH, Math.max(nh, 0));
				
				int px = predator.getDiscValForAttribute(ATTX);
				int py = predator.getDiscValForAttribute(ATTY);
				
				ObjectInstance fPredator = fail.getFirstObjectOfClass(CLASSPREDATOR);
				fPredator.setValue(ATTX, nx);
				fPredator.setValue(ATTY, ny);
				
				success.removeObject(success.getFirstObjectOfClass(CLASSPREDATOR));
				
				if(Math.abs(nx-px) != 2 && Math.abs(ny-py) != 2){
					TransitionProbability failTP = new TransitionProbability(fail, 1.);
					tps.add(failTP);
					return tps;
				}
				else{

					double probToBeCaught = this.probToBeCaught(nh);
					TransitionProbability sucTP = new TransitionProbability(success, 1.-probToBeCaught);
					TransitionProbability failTP = new TransitionProbability(fail, probToBeCaught);
					tps.add(sucTP);
					tps.add(failTP);
					
					return tps;
					
				}
				
			}
			
		}
		
		
		
	}
	
	
	
	
	protected double probToBeCaught(int ah){
		double distToHealth = (double)(this.maxHealth-ah) / (double)this.maxHealth;
		double pRange = 1.-this.baseProbCatch;
		return distToHealth*pRange + this.baseProbCatch;
	}
	
	protected double probToLose(int ah){
		double distToHealth = (double)(this.maxHealth-ah) / (double)this.maxHealth;
		double pRange = 1.-this.baseProbToLose;
		return distToHealth*pRange + this.baseProbToLose;
		
	}
	
	
	protected List<ObjectInstance> filterLocType(List<ObjectInstance> locations, int locType){
		
		List<ObjectInstance> result = new ArrayList<ObjectInstance>(locations.size());
		for(ObjectInstance o : locations){
			int lt = o.getDiscValForAttribute(ATTLOCTYPE);
			if(lt == locType){
				result.add(o);
			}
		}
		return result;
		
	}
	
	protected boolean inALocation(List<ObjectInstance> locations, int ax, int ay){
		
		for(ObjectInstance o : locations){
			if(this.sameLocation(o, ax, ay)){
				return true;
			}
		}
		
		return false;
	}
	
	protected boolean sameLocation(ObjectInstance o, int ax, int ay){
		int lx = o.getDiscValForAttribute(ATTX);
		int ly = o.getDiscValForAttribute(ATTY);
		
		if(ax == lx && ay == ly){
			return true;
		}
		return false;
	}
	
	
	public class MovementAction extends Action{

		int xdelta;
		int ydelta;
		
		
		public MovementAction(String name, Domain domain, int xdelta, int ydelta){
			super(name, domain, "");
			this.xdelta = xdelta;
			this.ydelta = ydelta;
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			ObjectInstance predator = s.getFirstObjectOfClass(CLASSPREDATOR);
			if(predator == null){
				return true;
			}
			
			int px = predator.getDiscValForAttribute(ATTX);
			int py = predator.getDiscValForAttribute(ATTY);
			
			ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			
			if(ax == px && ay == py){
				return false;
			}
			
			return true;
		}
		
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			HostileWorldDomain.this.runStep(s, this.domain, this.xdelta, this.ydelta);
			return s;
		}
		
		
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return HostileWorldDomain.this.getFailSucceedOutcomes(s, domain, this.xdelta, this.ydelta);
		}
		
		
	}
	
	
	public class FightAction extends Action{

		public FightAction(String name, Domain domain){
			super(name, domain, "");
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			ObjectInstance predator = s.getFirstObjectOfClass(CLASSPREDATOR);
			if(predator == null){
				return false;
			}
			
			int px = predator.getDiscValForAttribute(ATTX);
			int py = predator.getDiscValForAttribute(ATTY);
			
			ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			
			if(ax == px && ay == py){
				return true;
			}
			
			return false;
			
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			HostileWorldDomain.this.runStep(s, this.domain, 0, 0);
			return s;
		}
		
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return HostileWorldDomain.this.getFailSucceedOutcomes(s, domain, 0, 0);
		}
		
	
	}

	
	
	
	
	
	
	
	
	
	
	public static void main(String [] args){
		HostileWorldDomain gen = new HostileWorldDomain();
		Domain domain = gen.generateDomain();
		
		State s = HostileWorldDomain.getState(domain, 20);
		HostileWorldDomain.setAgent(s, 0, 0, gen.getMaxHealth());
		
		HostileWorldDomain.setLocation(s, 0, 5, 5, 0);
		HostileWorldDomain.setLocation(s, 1, 5, 6, 0);
		HostileWorldDomain.setLocation(s, 2, 6, 5, 0);
		HostileWorldDomain.setLocation(s, 3, 6, 6, 0);
		
		int y = 3;
		for(int i = 4; i < 12; i+=2){
			HostileWorldDomain.setLocation(s, i, 1, y, 1);
			HostileWorldDomain.setLocation(s, i+1, 2, y, 1);
			y++;
		}
		
		int x = 4;
		for(int i = 12; i < 20; i+=2){
			HostileWorldDomain.setLocation(s, i, x, 2, 1);
			HostileWorldDomain.setLocation(s, i+1, x, 3, 1);
			x++;
		}
		
		
		explore(gen, domain, s);
		//runVI(gen, domain, s);
		
		
	}
	
	
	
	public static void explore(HostileWorldDomain gen, Domain domain, State s){
		Visualizer v = HostileWorldVisualizer.getVisualizer(gen.getWidth(), gen.getHeight());
		VisualExplorer exp = new VisualExplorer(domain, v, s);
		
		exp.addKeyAction("w", ACTIONNORTH);
		exp.addKeyAction("s", ACTIONSOUTH);
		exp.addKeyAction("d", ACTIONEAST);
		exp.addKeyAction("a", ACTIONWEST);
		exp.addKeyAction("x", ACTIONNOP);
		exp.addKeyAction("f", ACTIONFIGHT);
		
		exp.initGUI();
	}
	
	
	public static void runVI(HostileWorldDomain gen, Domain domain, State s){
		
		TerminalFunction tf = new TerminalFunction() {
			
			@Override
			public boolean isTerminal(State s) {
				ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
				return agent.getDiscValForAttribute(ATTHEALTH) == 0;
			}
		};
		
		RewardFunction rf = new NoSufferRF();
		//rf = new SufferRF();
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(CLASSAGENT, domain.getObjectClass(CLASSAGENT).attributeList);
		hashingFactory.setAttributesForClass(CLASSPREDATOR, domain.getObjectClass(CLASSPREDATOR).attributeList);
		
		//List<State> allStates = getAllLocationVersions(s, gen.getWidth(), gen.getHeight());
		List<State> allStates = getNoPredatorExpandedStates(s, domain, tf, hashingFactory);
		System.out.println("Num States To Render: " + allStates.size());
		
		
		ValueIteration vi = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.001, 10000);
		vi.planFromState(s);
		Policy p = new GreedyQPolicy(vi);
		
		
		
		LandmarkColorBlendInterpolation rb = new LandmarkColorBlendInterpolation();
		rb.addNextLandMark(0., Color.RED);
		rb.addNextLandMark(1., Color.BLUE);
		
		StateValuePainter2D svp = new StateValuePainter2D(rb);
		svp.setXYAttByObjectClass(HostileWorldDomain.CLASSAGENT, HostileWorldDomain.ATTX, 
				HostileWorldDomain.CLASSAGENT, HostileWorldDomain.ATTY);
		
		
		PolicyGlyphPainter2D spp = new PolicyGlyphPainter2D();
		spp.setXYAttByObjectClass(HostileWorldDomain.CLASSAGENT, HostileWorldDomain.ATTX, 
				HostileWorldDomain.CLASSAGENT, HostileWorldDomain.ATTY);
		spp.setActionNameGlyphPainter(HostileWorldDomain.ACTIONNORTH, new ArrowActionGlyph(0));
		spp.setActionNameGlyphPainter(HostileWorldDomain.ACTIONSOUTH, new ArrowActionGlyph(1));
		spp.setActionNameGlyphPainter(HostileWorldDomain.ACTIONEAST, new ArrowActionGlyph(2));
		spp.setActionNameGlyphPainter(HostileWorldDomain.ACTIONWEST, new ArrowActionGlyph(3));
		spp.setRenderStyle(PolicyGlyphRenderStyle.MAXACTION);
		
		ValueFunctionVisualizerGUI gui = new ValueFunctionVisualizerGUI(allStates, svp, vi);
		gui.setSpp(spp);
		gui.setPolicy(p);
		gui.setBgColor(Color.GRAY);
		gui.initGUI();
		
	}
	
	
	public static List<State> getNoPredatorExpandedStates(State srcState, Domain domain, TerminalFunction tf, StateHashFactory hashingFactory){
		
		Set<StateHashTuple> closed = new HashSet<StateHashTuple>();
		HashMap<StateHashTuple, StateHashTuple> noPred = new HashMap<StateHashTuple, StateHashTuple>();
		//Set<StateHashTuple> noPredNearest = new HashSet<StateHashTuple>();
		
		DiscreteMaskHashingFactory maskHashFactory = new DiscreteMaskHashingFactory();
		maskHashFactory.addAttributeForClass(CLASSAGENT, domain.getAttribute(ATTX));
		maskHashFactory.addAttributeForClass(CLASSAGENT, domain.getAttribute(ATTY));
		
		StateHashTuple shi = hashingFactory.hashState(srcState);
		List <Action> actions = domain.getActions();
		
		LinkedList <StateHashTuple> openList = new LinkedList<StateHashTuple>();
		openList.offer(shi);
		closed.add(shi);
		noPred.put(maskHashFactory.hashState(shi.s),maskHashFactory.hashState(shi.s));

		while(openList.size() > 0){
			StateHashTuple sh = openList.poll();
			
			
			if(tf.isTerminal(sh.s)){
				continue; //don't expand
			}
			
			List <GroundedAction> gas = sh.s.getAllGroundedActionsFor(actions);
			for(GroundedAction ga : gas){
				List <TransitionProbability> tps = ga.action.getTransitions(sh.s, ga.params);
				for(TransitionProbability tp : tps){
					StateHashTuple nsh = hashingFactory.hashState(tp.s);

					if(!closed.contains(nsh)){
						closed.add(nsh);
						
						if(!hasPred(nsh.s)){
							StateHashTuple rh = maskHashFactory.hashState(nsh.s);
							if(!noPred.containsKey(rh)){
								openList.offer(nsh);
								noPred.put(rh, rh);
							}
							else{
								StateHashTuple stored = noPred.get(rh);
								int sth = stored.s.getFirstObjectOfClass(CLASSAGENT).getDiscValForAttribute(ATTHEALTH);
								int rhh = rh.s.getFirstObjectOfClass(CLASSAGENT).getDiscValForAttribute(ATTHEALTH);
								if(rhh > sth){
									openList.offer(nsh);
									noPred.put(rh, rh);
								}
							}
						}
						
					}
				}
				
			}
		
			
		}
		
		List<State> result = new ArrayList<State>(noPred.size());
		for(StateHashTuple sh : noPred.values()){
			result.add(sh.s);
		}
		
		return result;
	}
		
	protected static boolean hasPred(State s){
		return s.getFirstObjectOfClass(CLASSPREDATOR) != null;
	}
	
	public static class NoSufferRF implements RewardFunction{

		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
			if(agent.getDiscValForAttribute(ATTHEALTH) == 0){
				return 0.;
			}
			return 1.;
		}
		
		
	}
	
	public static class SufferRF implements RewardFunction{

		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			ObjectInstance agentp = sprime.getFirstObjectOfClass(CLASSAGENT);
			int ah = agentp.getDiscValForAttribute(ATTHEALTH);
			int healthCost = 0;
			if(ah < 8){
				healthCost = -1;
			}
			
			ObjectInstance agento = s.getFirstObjectOfClass(CLASSAGENT);
			int aho = agento.getDiscValForAttribute(ATTHEALTH);
			
			int hurtCost = 0;
			if(ah - aho < -1){
				hurtCost = -1;
			}
			else if(ah - aho > 0){
				hurtCost = 2;
			}
			
			
			return healthCost + hurtCost;
		}
		
		
		
	}
	
	

}
