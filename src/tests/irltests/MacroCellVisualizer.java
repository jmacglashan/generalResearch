package tests.irltests;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Random;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;

public class MacroCellVisualizer extends GridWorldVisualizer {
	private Random random;

	public MacroCellVisualizer() {
		// TODO Auto-generated constructor stub
	}

	public static Visualizer getVisualizer(Domain d, int [][] map, MacroGridWorld.InMacroCellPF[] propFunctions, Map<String, Double> rewardMap) {
		Visualizer v = new Visualizer();

		v.addStaticPainter(new MapPainter(d, map, propFunctions, rewardMap));
		v.addObjectClassPainter(GridWorldDomain.CLASSAGENT, new GridWorldVisualizer.CellPainter(Color.red, map));

		return v;
	}

	public static class MapPainter implements StaticPainter{

		protected int 				dwidth;
		protected int 				dheight;
		protected int [][] 			map;
		protected double [][]		rewardMap;

		public MapPainter(Domain domain, int [][] map, MacroGridWorld.InMacroCellPF[] propFunctions, Map<String, Double> rewardMap) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
			this.rewardMap = new double[map.length][map[0].length];
			for (int i = 0; i < this.rewardMap.length; i++) {
				for (int j = 0; j < this.rewardMap[0].length; j++) {
					for (int k = 0; k < propFunctions.length; k++) {
						if (propFunctions[k].isTrue(i, j)) {
							this.rewardMap[i][j] += rewardMap.get(propFunctions[k].getName());
						}
					}
				}
			}
		}

		@Override
		public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {

			//draw the walls; make them black
			g2.setColor(Color.black);

			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;

			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;

			//pass through each cell of the map and if it is a wall, draw it
			for(int i = 0; i < this.dwidth; i++){
				for(int j = 0; j < this.dheight; j++){
					double d = this.rewardMap[i][j];
					g2.setColor(new Color((int)(255 * this.rewardMap[i][j])));
					float rx = i*width;
					float ry = cHeight - height - j*height;
					g2.fill(new Rectangle2D.Float(rx, ry, width, height));						
				}
			}	
		}	
	}	

}
