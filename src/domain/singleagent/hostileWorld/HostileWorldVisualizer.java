package domain.singleagent.hostileWorld;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;

public class HostileWorldVisualizer {

	
	
	public static Visualizer getVisualizer(int width, int height){
		StateRenderLayer r = getRenderLayer(width, height);
		Visualizer v = new Visualizer(r);
		return v;
	}
	
	
	public static StateRenderLayer getRenderLayer(int width, int height){
		
		StateRenderLayer slr = new StateRenderLayer();
		
		slr.addStaticPainter(new AgentHealthPainter(width, height));
		
		slr.addObjectClassPainter(HostileWorldDomain.CLASSLOC, new LocationPainter(width, height));
		slr.addObjectClassPainter(HostileWorldDomain.CLASSAGENT, new CellPainter(1, Color.gray, width, height));
		slr.addObjectClassPainter(HostileWorldDomain.CLASSPREDATOR, new CellPainter(1, Color.red, width, height));
		
		return slr;
	}
	
	
	
	
	
	
	
	public static class AgentHealthPainter implements StaticPainter{

		int width;
		int height;
		
		public AgentHealthPainter(int width, int height){
			this.width = width;
			this.height = height;
		}
		
		
		@Override
		public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {
			
			//set the color of the object
			g2.setColor(Color.black);
			
			float domainXScale = this.width;
			float domainYScale = this.height;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = (this.width-1)*width;
			float ry = cHeight - height - (0)*height;
			
			
			ObjectInstance agent = s.getFirstObjectOfClass(HostileWorldDomain.CLASSAGENT);
			int ah = agent.getIntValForAttribute(HostileWorldDomain.ATTHEALTH);
			String ahs = "" + ah;
			
			g2.setFont(new Font("Helvetica", Font.PLAIN, 48));
			
			int stringLen = (int)g2.getFontMetrics().getStringBounds(ahs, g2).getWidth();
			int stringHeight = (int)g2.getFontMetrics().getStringBounds(ahs, g2).getHeight();
			
			int stringX = (int)rx + (stringLen / 2);
			int stringY = (int)ry + (stringHeight / 2);
			
			g2.drawString(ahs, stringX, stringY);
			
		}
		
		
		
		
	}
	
	
	
	
	
	/**
	 * A painter for a grid world cell which will fill the cell with a given color and where the cell position
	 * is indicated by the x and y attribute for the mapped object instance
	 * @author James MacGlashan
	 *
	 */
	public static class CellPainter implements ObjectPainter{

		protected Color			col;
		protected int			dwidth;
		protected int			dheight;
		protected int			shape = 0; //0 for rectangle 1 for ellipse
		
		

		public CellPainter(Color col, int width, int height) {
			this.col = col;
			this.dwidth = width;
			this.dheight = height;

		}
		

		public CellPainter(int shape, Color col, int width, int height) {
			this.col = col;
			this.dwidth = width;
			this.dheight = height;
			this.shape = shape;
		}

		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			
			//set the color of the object
			g2.setColor(this.col);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = ob.getIntValForAttribute(HostileWorldDomain.ATTX)*width;
			float ry = cHeight - height - ob.getIntValForAttribute(HostileWorldDomain.ATTY)*height;
			
			if(this.shape == 0){
				g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			}
			else{
				g2.fill(new Ellipse2D.Float(rx, ry, width, height));
			}
			
			
			
		}
		
		
	}
	
	
	
	

	public static class LocationPainter implements ObjectPainter{

		protected int			dwidth;
		protected int			dheight;
		protected int			shape = 0; //0 for rectangle 1 for ellipse
		

		public LocationPainter(int width, int height) {
			this.dwidth = width;
			this.dheight = height;

		}
		

		public LocationPainter(int shape, int width, int height) {
			this.dwidth = width;
			this.dheight = height;
			this.shape = shape;
		}

		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			int type = ob.getIntValForAttribute(HostileWorldDomain.ATTLOCTYPE);
			Color col = Color.red.darker();
			if(type == 0){
				col = Color.blue;
			}
			
			//set the color of the object
			g2.setColor(col);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = ob.getIntValForAttribute(HostileWorldDomain.ATTX)*width;
			float ry = cHeight - height - ob.getIntValForAttribute(HostileWorldDomain.ATTY)*height;
			
			if(this.shape == 0){
				g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			}
			else{
				g2.fill(new Ellipse2D.Float(rx, ry, width, height));
			}
			
		}
		
		
	}
	
	
	
}
