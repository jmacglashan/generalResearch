package domain.singleagent.sokoban2;

import behavior.training.experiments.interactive.soko.SokoCommandTrainGUI;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;


import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoTurtleBot extends Sokoban2Domain{


	public static final String				ACTIONFORWARD = "forward";
	public static final String				ACTIONROTATE = "rotate";

	@Override
	public Domain generateDomain() {
		SADomain domain = new SADomain();

		Attribute xatt = new Attribute(domain, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, maxX, 1);

		Attribute yatt = new Attribute(domain, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, maxY, 1);

		Attribute topAtt = new Attribute(domain, ATTTOP, Attribute.AttributeType.DISC);
		topAtt.setDiscValuesForRange(0, maxY, 1);

		Attribute leftAtt = new Attribute(domain, ATTLEFT, Attribute.AttributeType.DISC);
		leftAtt.setDiscValuesForRange(0, maxX, 1);

		Attribute bottomAtt = new Attribute(domain, ATTBOTTOM, Attribute.AttributeType.DISC);
		bottomAtt.setDiscValuesForRange(0, maxY, 1);

		Attribute rightAtt = new Attribute(domain, ATTRIGHT, Attribute.AttributeType.DISC);
		rightAtt.setDiscValuesForRange(0, maxX, 1);

		Attribute colAtt = new Attribute(domain, ATTCOLOR, Attribute.AttributeType.DISC);
		colAtt.setDiscValues(COLORS);

		Attribute shapeAtt = new Attribute(domain, ATTSHAPE, Attribute.AttributeType.DISC);
		shapeAtt.setDiscValues(SHAPES);

		Attribute dirAtt = new Attribute(domain, ATTDIR, Attribute.AttributeType.DISC);
		dirAtt.setDiscValues(DIRECTIONS);


		ObjectClass agent = new ObjectClass(domain, CLASSAGENT);
		agent.addAttribute(xatt);
		agent.addAttribute(yatt);
		agent.addAttribute(domain.getAttribute(ATTDIR));


		ObjectClass block = new ObjectClass(domain, CLASSBLOCK);
		block.addAttribute(xatt);
		block.addAttribute(yatt);
		block.addAttribute(colAtt);
		block.addAttribute(shapeAtt);

		ObjectClass room = new ObjectClass(domain, CLASSROOM);
		this.addRectAtts(domain, room);
		room.addAttribute(colAtt);

		ObjectClass door = new ObjectClass(domain, CLASSDOOR);
		this.addRectAtts(domain, door);


		new MoveForwardAction(domain);
		new RotateAction(domain);


		new PFInRegion(PFAGENTINROOM, domain, new String[]{CLASSAGENT, CLASSROOM}, false);
		new PFInRegion(PFBLOCKINROOM, domain, new String[]{CLASSBLOCK, CLASSROOM}, false);

		new PFInRegion(PFAGENTINDOOR, domain, new String[]{CLASSAGENT, CLASSDOOR}, true);
		new PFInRegion(PFBLOCKINDOOR, domain, new String[]{CLASSBLOCK, CLASSDOOR}, true);

		for(String col : COLORS){
			new PFIsColor(PFRoomColorName(col), domain, new String[]{CLASSROOM}, col);
			new PFIsColor(PFBlockColorName(col), domain, new String[]{CLASSBLOCK}, col);
		}

		for(String shape : SHAPES){
			new PFIsShape(PFBlockShapeName(shape), domain, new String[]{CLASSBLOCK}, shape);
		}

		if(this.includeWallPFs){
			new PFWallTest(PFWALLNORTH, domain, 0, 1);
			new PFWallTest(PFWALLSOUTH, domain, 0, -1);
			new PFWallTest(PFWALLEAST, domain, 1, 0);
			new PFWallTest(PFWALLWEST, domain, -1, 0);
		}

		if(this.includeTouchingBlockPF){
			new PFTouchingBlock(domain);
		}


		return domain;
	}


	/**
	 * Class that rotates clockwise.
	 */
	public static class RotateAction extends Action{


		public RotateAction(Domain domain){
			super(ACTIONROTATE, domain, "");
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int dir = agent.getDiscValForAttribute(Sokoban2Domain.ATTDIR);

			/*
			0: north
			1: south
			2: east
			3: west
			 */

			if(dir == 0){
				agent.setValue(Sokoban2Domain.ATTDIR, 2);
			}
			else if(dir == 1){
				agent.setValue(Sokoban2Domain.ATTDIR, 3);
			}
			else if(dir == 2){
				agent.setValue(Sokoban2Domain.ATTDIR, 1);
			}
			else if(dir == 3){
				agent.setValue(Sokoban2Domain.ATTDIR, 0);
			}
			else{
				throw new RuntimeException("Error, cannot roate because of unknown direction index: " + dir);
			}

			return s;
		}


		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}

	public static class MoveForwardAction extends Action{


		public MoveForwardAction(Domain domain){
			super(ACTIONFORWARD, domain, "");
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getDiscValForAttribute(Sokoban2Domain.ATTY);
			int dir = agent.getDiscValForAttribute(Sokoban2Domain.ATTDIR);

			int xdelta = 0;
			int ydelta = 0;

			if(dir == 0){
				ydelta = 1;
			}
			else if(dir == 1){
				ydelta = -1;
			}
			else if(dir == 2){
				xdelta = 1;
			}
			else if(dir == 3){
				xdelta = -1;
			}
			else{
				throw new RuntimeException("Error, cannot roate because of unknown direction index: " + dir);
			}

			int nx = ax+xdelta;
			int ny = ay+ydelta;

			ObjectInstance roomContaining = regionContainingPoint(s.getObjectsOfTrueClass(CLASSROOM), ax, ay, true);


			boolean permissibleMove = false;
			ObjectInstance pushedBlock = blockAtPoint(s, nx, ny);
			if(pushedBlock != null){
				int bx = pushedBlock.getDiscValForAttribute(ATTX);
				int by = pushedBlock.getDiscValForAttribute(ATTY);

				int nbx = bx + xdelta;
				int nby = by + ydelta;

				if(!wallAt(s, roomContaining, nbx, nby) && blockAtPoint(s, nbx, nby) == null){
					permissibleMove = true;

					//move the block
					pushedBlock.setValue(ATTX, nbx);
					pushedBlock.setValue(ATTY, nby);

				}

			}
			else if(roomContaining == null || !wallAt(s, roomContaining, nx, ny)){
				permissibleMove = true;
			}

			if(permissibleMove){
				agent.setValue(ATTX, nx);
				agent.setValue(ATTY, ny);
			}



			return s;


		}


		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}






	public static void main(String [] args){

		SokoTurtleBot dgen = new SokoTurtleBot();

		Domain domain = dgen.generateDomain();

		State s = Sokoban2Domain.getClassicState(domain);

		Visualizer v = Sokoban2Visualizer.getVisualizer("oomdpResearch/robotImages");
		VisualExplorer exp = new VisualExplorer(domain, v, s);

		exp.addKeyAction("w", ACTIONFORWARD);
		exp.addKeyAction("d", ACTIONROTATE);

		exp.initGUI();

	}



}
