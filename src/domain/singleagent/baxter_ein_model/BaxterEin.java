package domain.singleagent.baxter_ein_model;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class BaxterEin implements DomainGenerator {

	public static final String						ATTX = "x";
	public static final String						ATTY = "y";
	public static final String						ATTZ = "z";
	public static final String						ATTTYPE = "type";

	public static final String						CLASSPEDESTAL = "pedestal";
	public static final String						CLASSPEDESTALSOKO = "room";
	public static final String						CLASSOBJECT = "block";

	public static final String						ACTIONSTACK = "stack";

	public static final String						PFON = "on";
	public static final String						PFCOLORPED = "pedestalColor";
	public static final String						PFCOLOR = "color";
	public static final String						PFSHAPE = "shape";

	public static final String						PFONSOKO = "blockInRoom";
	public static final String						PFCOLORPEDSOKO = "roomIs";
	public static final String						PFCOLORSOKO = "blockIs";
	public static final String						PFSHAPESOKO = "shape";

	public static final String[] 					COLORS = new String[]{"blue",
			"green", "magenta",
			"red", "yellow"};

	public static final String[]					SHAPES = new String[]{"chair", "bag",
			"backpack", "basket", "table"};


	public boolean useSoko = false;

	public ObjectPropertyDB db = new ObjectPropertyDB();

	public BaxterEin(){

		this.db.setProperties("table1", new ObjectProperties("green", "table"));
		this.db.setProperties("table2", new ObjectProperties("blue", "table"));
		this.db.setProperties("table3", new ObjectProperties("red", "table"));
		this.db.setProperties("woodenBowl", new ObjectProperties("red", "table"));

		this.db.setProperties("brownMug", new ObjectProperties("blue", "chair"));

	}

	public static State addObjectToState(Domain domain, State s, String type, double x, double y, double z){
		String className = getObjectClassForType(domain, type);
		int index = s.getObjectsOfClass(className).size();
		ObjectInstance ob = new MutableObjectInstance(domain.getObjectClass(className), type);
		ob.setValue(ATTX, x);
		ob.setValue(ATTY, y);
		ob.setValue(ATTZ, z);
		ob.setValue(ATTTYPE, type);
		s.addObject(ob);
		return s;

	}

	public static String getObjectClassForType(Domain domain, String type){
		if(type.startsWith("table") || type.equals("woodenBowl")){
			if(domain.getObjectClass(CLASSPEDESTAL) != null){
				return CLASSPEDESTAL;
			}
			else{
				return CLASSPEDESTALSOKO;
			}
		}
		return CLASSOBJECT;
	}


	@Override
	public Domain generateDomain() {

		Domain domain = new SADomain();

		Attribute x = new Attribute(domain, ATTX, Attribute.AttributeType.REALUNBOUND);
		Attribute y = new Attribute(domain, ATTY, Attribute.AttributeType.REALUNBOUND);
		Attribute z = new Attribute(domain, ATTZ, Attribute.AttributeType.REALUNBOUND);
		Attribute type = new Attribute(domain, ATTTYPE, Attribute.AttributeType.STRING);

		ObjectClass oclass = new ObjectClass(domain, CLASSOBJECT);
		oclass.addAttribute(x);
		oclass.addAttribute(y);
		oclass.addAttribute(z);
		oclass.addAttribute(type);

		String pedestalClassName = useSoko ? CLASSPEDESTALSOKO : CLASSPEDESTAL;
		ObjectClass pedestal = new ObjectClass(domain, pedestalClassName);
		pedestal.addAttribute(x);
		pedestal.addAttribute(y);
		pedestal.addAttribute(z);
		pedestal.addAttribute(type);


		new ActionStack(domain);

		ObjectPropertyDB ndb = this.db.duplicate();

		String colorBase = useSoko ? PFCOLORSOKO : PFCOLOR;
		String colorPedBase = useSoko ? PFCOLORPEDSOKO : PFCOLORPED;
		for(String col : COLORS){
			new PFColor(conposition(colorBase, col), domain, CLASSOBJECT, col, ndb);
			new PFColor(conposition(colorPedBase, col), domain, pedestalClassName, col, ndb);
		}

		String shapeBase = useSoko ? PFSHAPESOKO : PFSHAPE;
		for(String shape : SHAPES){
			new PFShape(conposition(shapeBase, shape), domain, CLASSOBJECT, shape, ndb);
		}

		String onName = useSoko ? PFONSOKO : PFON;
		new PFOn(onName, domain, new String[]{CLASSOBJECT, pedestalClassName});


		return domain;
	}

	public static String conposition(String base, String value){
		String capped = firstLetterCapped(value);
		return base + capped;
	}

	protected static String firstLetterCapped(String s){
		String firstLetter = s.substring(0, 1);
		String remainder = s.substring(1);
		return firstLetter.toUpperCase() + remainder;
	}


	public static class ActionStack extends Action{

		public ActionStack(Domain domain){
			super(ACTIONSTACK, domain, new String[]{CLASSOBJECT, CLASSOBJECT});
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance source = s.getObject(params[0]);
			ObjectInstance target = s.getObject(params[1]);

			double tx = target.getRealValForAttribute(ATTX);
			double ty = target.getRealValForAttribute(ATTY);
			double tz = target.getRealValForAttribute(ATTZ);

			source.setValue(ATTX, tx);
			source.setValue(ATTY, ty);
			source.setValue(ATTZ, tz+0.01);

			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}

	public static class PFOn extends PropositionalFunction{

		public PFOn(String name, Domain domain, String[] params){
			super(name, domain, params);
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance sourceOb = s.getObject(params[0]);
			ObjectInstance targetOb = s.getObject(params[1]);

			double sx = sourceOb.getRealValForAttribute(ATTX);
			double sy = sourceOb.getRealValForAttribute(ATTY);
			double sz = sourceOb.getRealValForAttribute(ATTZ);

			double tx = targetOb.getRealValForAttribute(ATTX);
			double ty = targetOb.getRealValForAttribute(ATTY);
			double tz = targetOb.getRealValForAttribute(ATTZ);

			double dx = Math.abs(sx-tx);
			double dy = Math.abs(sy-ty);

			double dist = Math.sqrt(dx*dx + dy*dy);

			if(dist > 0.06){
				return false;
			}
			else return sz > tz;

		}
	}

	public static class PFColor extends PropositionalFunction{

		public ObjectPropertyDB db;
		public String color;

		public PFColor(String name, Domain domain, String className, String color, ObjectPropertyDB db){
			super(name, domain, className);
			this.color = color;
			this.db = db;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance ob = s.getObject(params[0]);
			String type = ob.getStringValForAttribute(ATTTYPE);
			return this.color.equals(this.db.getProperties(type).color);
		}
	}


	public static class PFShape extends PropositionalFunction{

		public ObjectPropertyDB db;
		public String shape;

		public PFShape(String name, Domain domain, String className, String shape, ObjectPropertyDB db){
			super(name, domain, className);
			this.shape = shape;
			this.db = db;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance ob = s.getObject(params[0]);
			String type = ob.getStringValForAttribute(ATTTYPE);
			return this.shape.equals(this.db.getProperties(type).shape);
		}
	}


	public static class PFActionObserver implements ActionObserver{

		Domain domain;

		public PFActionObserver(Domain domain){
			this.domain = domain;
		}

		@Override
		public void actionEvent(State s, GroundedAction ga, State sp) {
			List<GroundedProp> gps = PropositionalFunction.getAllGroundedPropsFromPFList(domain.getPropFunctions(), sp);
			for(GroundedProp gp : gps){
				if(gp.isTrue(sp)){
					System.out.print(gp.toString() + "; ");
				}
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {

		BaxterEin bein = new BaxterEin();
		Domain domain = bein.generateDomain();
		State s = new MutableState();

		BaxterEin.addObjectToState(domain, s, "woodenBowl", 0.44, 0.65, 0.);
		BaxterEin.addObjectToState(domain, s, "brownMug", 0.25, 0.75, 0.);


		PFActionObserver observer = new PFActionObserver(domain);
		observer.actionEvent(null, null, s);

		((SADomain)domain).addActionObserverForAllAction(observer);

		TerminalExplorer exp = new TerminalExplorer(domain);
		exp.exploreFromState(s);

	}
}
