package domain.singleagent.baxter.pickupplace;

import java.util.List;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class PickupAndPlaceDomain implements DomainGenerator {

	public static final String						ATTX = "x";
	public static final String						ATTY = "y";
	public static final String						ATTZ = "z";
	public static final String						ATTLEFT = "left";
	public static final String						ATTRIGHT = "right";
	public static final String						ATTTOP = "top";
	public static final String						ATTBOTTOM = "bottom";
	public static final String						ATTHEIGHT = "height";
	public static final String						ATTCOLOR = "color";
	
	
	public static final String						CLASSOBJECT = "object";
	public static final String						CLASSREGION = "region";
	
	public static final String						ACTIONPICKPLACE = "move";
	
	public static final String						PFINREGION = "inRegion";
	public static final String						PFREGIONCLEAR = "regionClear";
	
	
	public static final String []					COLORS = new String[]{"red","green","blue","yellow"};
	
	
	protected boolean								includeColors;
	protected boolean								requiresClear = false;
	
	
	public PickupAndPlaceDomain(boolean includeColors){
		this.includeColors = includeColors;
	}
	
	public PickupAndPlaceDomain(boolean includeColors, boolean requiresClear){
		this.includeColors = includeColors;
		this.requiresClear = requiresClear;
	}
	
	@Override
	public Domain generateDomain() {
		
		Domain domain = new SADomain();
		
		Attribute x = new Attribute(domain, ATTX, AttributeType.REALUNBOUND);
		Attribute y = new Attribute(domain, ATTY, AttributeType.REALUNBOUND);
		Attribute z = new Attribute(domain, ATTZ, AttributeType.REALUNBOUND);
		
		Attribute color = null;
		if(this.includeColors){
			color = new Attribute(domain, ATTCOLOR, AttributeType.DISC);
			color.setDiscValues(COLORS);
		}
		
		Attribute left = new Attribute(domain, ATTLEFT, AttributeType.REALUNBOUND);
		Attribute right = new Attribute(domain, ATTRIGHT, AttributeType.REALUNBOUND);
		Attribute top = new Attribute(domain, ATTTOP, AttributeType.REALUNBOUND);
		Attribute bottom = new Attribute(domain, ATTBOTTOM, AttributeType.REALUNBOUND);
		Attribute height = new Attribute(domain, ATTHEIGHT, AttributeType.REALUNBOUND);
		
		
		ObjectClass object = new ObjectClass(domain, CLASSOBJECT);
		object.addAttribute(x);
		object.addAttribute(y);
		object.addAttribute(z);
		if(this.includeColors){
			object.addAttribute(color);
		}
		
		ObjectClass region = new ObjectClass(domain, CLASSREGION);
		region.addAttribute(left);
		region.addAttribute(right);
		region.addAttribute(top);
		region.addAttribute(bottom);
		region.addAttribute(height);
		
		
		new PickAndPlaceAction(domain, this.requiresClear);
		new InRegionPF(PFINREGION, domain, new String[]{CLASSOBJECT, CLASSREGION});
		new RegionClearPF(PFREGIONCLEAR, domain, new String[]{CLASSREGION});
		
		return domain;
	}
	
	
	
	public class PickAndPlaceAction extends Action{

		boolean requiresClear = false;
		
		public PickAndPlaceAction(Domain domain){
			super(ACTIONPICKPLACE, domain, new String[]{CLASSOBJECT, CLASSREGION});
		}
		
		public PickAndPlaceAction(Domain domain, boolean requiresClear){
			super(ACTIONPICKPLACE, domain, new String[]{CLASSOBJECT, CLASSREGION});
			this.requiresClear = requiresClear;
		}
		
		public boolean applicableInState(State s, String [] params){
			
			if(this.requiresClear){
				PropositionalFunction rclear = this.domain.getPropFunction(PFREGIONCLEAR);
				return rclear.isTrue(s, params[1]);
			}
			return true;
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance r = s.getObject(params[1]);
			double rl = r.getRealValForAttribute(ATTLEFT);
			double rr = r.getRealValForAttribute(ATTRIGHT);
			double rt = r.getRealValForAttribute(ATTTOP);
			double rb = r.getRealValForAttribute(ATTBOTTOM);
			double rh = r.getRealValForAttribute(ATTHEIGHT);
			
			double nx = (rl + rr) / 2.;
			double ny = rh;
			double nz = (rt + rb) / 2.;
			
			ObjectInstance object = s.getObject(params[0]);
			object.setValue(ATTX, nx);
			object.setValue(ATTY, ny);
			object.setValue(ATTZ, nz);
			
			return s;
		}
		

	}
	
	
	public static State getCleanState(Domain domain, int nObjects, int nRegions){
		State s = new State();
		
		for(int i = 0; i < nObjects; i++){
			ObjectInstance o = new ObjectInstance(domain.getObjectClass(CLASSOBJECT), CLASSOBJECT+i);
			s.addObject(o);
		}
		
		for(int i = 0; i < nRegions; i++){
			ObjectInstance o = new ObjectInstance(domain.getObjectClass(CLASSREGION), CLASSREGION+i);
			s.addObject(o);
		}
		
		return s;
	}
	
	
	public static void tileRegions(State s, int rows, int cols, double xLower, double xUpper, double zLower, double zUpper, double height){
		
		List<ObjectInstance> regions = s.getObjectsOfTrueClass(CLASSREGION);
		
		if(regions.size() != rows*cols){
			throw new RuntimeException("Error: number of rows and columns does not match number of regions in state. Requested " + (rows*cols) + " but had " + regions.size() + " regions.");
		}
		
		double xRange = xUpper - xLower;
		double zRange = zUpper - zLower;
		
		double regionWidth = xRange / cols;
		double regionHeight = zRange / rows;
		
		
		int c = 0;
		for(int i = 0; i < rows; i++){
			double bottom = i*regionHeight;
			double top = bottom + regionHeight;
			for(int j = 0; j < cols; j++){
				double left = j*regionWidth;
				double right = left+regionWidth;
				setRegion(s, c, left, right, top, bottom, height);
				c++;
			}
		}
		
		
	}
	
	
	public static void setObject(State s, int i, double x, double y, double z){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSOBJECT).get(i);
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTZ, z);
	}
	
	public static void setObject(State s, int i, double x, double y, double z, String color){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSOBJECT).get(i);
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTZ, z);
		o.setValue(ATTCOLOR, color);
	}
	
	
	public static void setRegion(State s, int i, double left, double right, double top, double bottom, double height){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSREGION).get(i);
		o.setValue(ATTLEFT, left);
		o.setValue(ATTRIGHT, right);
		o.setValue(ATTTOP, top);
		o.setValue(ATTBOTTOM, bottom);
		o.setValue(ATTHEIGHT, height);
	}
	
	
	public class InRegionPF extends PropositionalFunction{

		public double fuzzyHeight = Double.POSITIVE_INFINITY; //ignore height
		
		public InRegionPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			
			ObjectInstance r = s.getObject(params[1]);
			double rl = r.getRealValForAttribute(ATTLEFT);
			double rr = r.getRealValForAttribute(ATTRIGHT);
			double rt = r.getRealValForAttribute(ATTTOP);
			double rb = r.getRealValForAttribute(ATTBOTTOM);
			double rh = r.getRealValForAttribute(ATTHEIGHT);
			
			
			ObjectInstance o = s.getObject(params[0]);
			double x = o.getRealValForAttribute(ATTX);
			double y = o.getRealValForAttribute(ATTY);
			double z = o.getRealValForAttribute(ATTZ);
			
			if(x >= rl && x < rr && z >= rb && z < rt && Math.abs(y - rh) > this.fuzzyHeight){
				return true;
			}
			
			return false;
		}
		

	}
	
	
	public static class RegionClearPF extends PropositionalFunction{

		public double fuzzyHeight = Double.POSITIVE_INFINITY; //ignore height
		
		public RegionClearPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			
			ObjectInstance r = s.getObject(params[0]);
			double rl = r.getRealValForAttribute(ATTLEFT);
			double rr = r.getRealValForAttribute(ATTRIGHT);
			double rt = r.getRealValForAttribute(ATTTOP);
			double rb = r.getRealValForAttribute(ATTBOTTOM);
			double rh = r.getRealValForAttribute(ATTHEIGHT);
			
			List<ObjectInstance> objects = s.getObjectsOfTrueClass(CLASSOBJECT);
			for(ObjectInstance o : objects){
				double x = o.getRealValForAttribute(ATTX);
				double y = o.getRealValForAttribute(ATTY);
				double z = o.getRealValForAttribute(ATTZ);
				
				if(x >= rl && x < rr && z >= rb && z < rt && Math.abs(y - rh) < this.fuzzyHeight){
					return false;
				}
			}
			
			return true;
		}
		
		
		
	}
	
	
	
	
	public static void main(String [] args){
		
		PickupAndPlaceDomain dgen = new PickupAndPlaceDomain(true);
		SADomain domain = (SADomain)dgen.generateDomain();
		
		State s = PickupAndPlaceDomain.getCleanState(domain, 3, 9);
		PickupAndPlaceDomain.tileRegions(s, 3, 3, 0, 100., 0, 100, 20);
		PickupAndPlaceDomain.setObject(s, 0, 20, 20, 20, "red");
		PickupAndPlaceDomain.setObject(s, 1, 80, 20, 20, "green");
		PickupAndPlaceDomain.setObject(s, 2, 20, 20, 80, "blue");
		
		//s = domain.getAction(ACTIONPICKPLACE).performAction(s, new String[]{"object0", "region5"});
		//s = domain.getAction(ACTIONPICKPLACE).performAction(s, new String[]{"object0", "region0"});
		
		Visualizer v = PickupAndPlaceVisualizer.getVisualizer(0, 100., 0., 100., 20);
		
		
		VisualExplorer exp = new VisualExplorer(domain, v, s);
		exp.initGUI();
		
		
		/*
		TerminalExplorer exp = new TerminalExplorer(domain);
		exp.exploreFromState(s);
		*/
		
	}
	

}
