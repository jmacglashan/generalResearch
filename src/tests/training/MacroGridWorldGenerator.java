package tests.training;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class MacroGridWorldGenerator extends GridWorldDomain {

	public static final String							ATTTBOUND = "top";
	public static final String							ATTLBOUND = "left";
	public static final String							ATTBBOUND = "bottom";
	public static final String							ATTRBOUND = "right";
	
	public static final String							CLASSAREA = "area";
	
	public static final String							PFINAREA = "inArea";
	
	
	public MacroGridWorldGenerator(int width, int height) {
		super(width, height);
	}
	
	/**
	 * Constructs a deterministic world based on the provided map.
	 * @param map the first index is the x index, the second the y; 1 entries indicate a wall
	 */
	public MacroGridWorldGenerator(int [][] map){
		super(map);
	}

	@Override
	public Domain generateDomain() {
		
		Domain domain = super.generateDomain();
		
		//add extra stuff
		Attribute attt = new Attribute(domain, ATTTBOUND, AttributeType.DISC);
		attt.setDiscValuesForRange(0, height-1, 1);
		
		Attribute attl = new Attribute(domain, ATTLBOUND, AttributeType.DISC);
		attl.setDiscValuesForRange(0, width-1, 1);
		
		Attribute attb = new Attribute(domain, ATTBBOUND, AttributeType.DISC);
		attb.setDiscValuesForRange(0, height-1, 1);
		
		Attribute attr = new Attribute(domain, ATTRBOUND, AttributeType.DISC);
		attr.setDiscValuesForRange(0, width-1, 1);
		
		
		ObjectClass areaClass = new ObjectClass(domain, CLASSAREA);
		areaClass.addAttribute(attt);
		areaClass.addAttribute(attl);
		areaClass.addAttribute(attb);
		areaClass.addAttribute(attr);
		
		PropositionalFunction pfarea = new PFInArea(domain);
		
		
		
		
		return domain;
		
	}
	
	public static void setArea(State s, int i, int t, int l, int b, int r){
		ObjectInstance o = s.getObjectsOfClass(CLASSAREA).get(i);
		o.setValue(ATTTBOUND, t);
		o.setValue(ATTLBOUND, l);
		o.setValue(ATTBBOUND, b);
		o.setValue(ATTRBOUND, r);
	}
	
	public static State getOneAgentNLocationMAreaState(Domain d, int n, int m){
		
		State s = getOneAgentNLocationState(d, n);
		
		for(int i = 0; i < m; i++){
			ObjectInstance o = new MutableObjectInstance(d.getObjectClass(CLASSAREA), CLASSAREA+i);
			s.addObject(o);
		}
		
		
		return s;
		
	}
	
	class PFInArea extends PropositionalFunction{
		
		public PFInArea(Domain domain){
			super(PFINAREA, domain, new String[]{CLASSAGENT, CLASSAREA});
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			
			ObjectInstance agent = s.getObject(params[0]);
			ObjectInstance area = s.getObject(params[1]);
			
			int ax = agent.getIntValForAttribute(ATTX);
			
			
			int l = area.getIntValForAttribute(ATTLBOUND);
			if(ax < l){
				return false;
			}
			
			int r = area.getIntValForAttribute(ATTRBOUND);
			if(ax > r){
				return false;
			}
			
			int ay = agent.getIntValForAttribute(ATTY);
			int b = area.getIntValForAttribute(ATTBBOUND);
			if(ay < b){
				return false;
			}
			
			int t = area.getIntValForAttribute(ATTTBOUND);
			if (ay > t){
				return false;
			}
			
			return true;
		}
		
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		MacroGridWorldGenerator mgw = new MacroGridWorldGenerator(11, 11);
		mgw.setMapToFourRooms();
		
		Domain d = mgw.generateDomain();
		
		State s = getOneAgentNLocationMAreaState(d, 0, 4);
		setAgent(s, 2, 2);
		setArea(s, 0, 4, 0, 0, 4);
		setArea(s, 1, 10, 0, 6, 4);
		setArea(s, 2, 10, 6, 5, 10);
		setArea(s, 3, 3, 6, 0, 10);
		
		Visualizer v = GridWorldVisualizer.getVisualizer(d, mgw.getMap());
		VisualExplorer exp = new VisualExplorer(d, v, s);
		
		//use w-s-a-d-x
		exp.addKeyAction("w", ACTIONNORTH);
		exp.addKeyAction("s", ACTIONSOUTH);
		exp.addKeyAction("a", ACTIONWEST);
		exp.addKeyAction("d", ACTIONEAST);
		
		exp.initGUI();
		

	}

}
