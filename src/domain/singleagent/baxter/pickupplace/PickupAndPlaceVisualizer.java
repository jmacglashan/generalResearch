package domain.singleagent.baxter.pickupplace;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.Visualizer;

public class PickupAndPlaceVisualizer {

	public static Visualizer getVisualizer(double xLower, double xUpper, double zLower, double zUpper, double blockWidth){
		return new Visualizer(getStateRenderLayer(xLower, xUpper, zLower, zUpper, blockWidth));
	}
	
	public static StateRenderLayer getStateRenderLayer(double xLower, double xUpper, double zLower, double zUpper, double blockWidth){
		
		StateRenderLayer rl = new StateRenderLayer();
		rl.addObjectClassPainter(PickupAndPlaceDomain.CLASSOBJECT, new ObjectVisualizer(xLower, xUpper, zLower, zUpper, blockWidth));
		rl.addObjectClassPainter(PickupAndPlaceDomain.CLASSREGION, new RegionVisualizer(xLower, xUpper, zLower, zUpper));
		
		return rl;
		
	}
	
	
	
	public static class ObjectVisualizer implements ObjectPainter{

		double xLower;
		double xRange;
		double zLower;
		double zRange;
		
		double blockWidth;
		
		int fontSize = 12;
		
		
		public ObjectVisualizer(double xLower, double xUpper, double zLower, double zUpper, double blockWidth){
			this.xRange = xUpper - xLower;
			this.xLower = xLower;
			
			this.zRange = zUpper - zLower;
			this.zLower = zLower;
			
			this.blockWidth = blockWidth;
		}
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			g2.setColor(Color.gray);
			if(ob.getObjectClass().hasAttribute(PickupAndPlaceDomain.ATTCOLOR)){
				g2.setColor(this.getColorFromString(ob.getStringValForAttribute(PickupAndPlaceDomain.ATTCOLOR)));
			}
			
			
			double x = ob.getRealValForAttribute(PickupAndPlaceDomain.ATTX);
			double z = ob.getRealValForAttribute(PickupAndPlaceDomain.ATTZ);
			
			float nx = (float)((x-xLower)/xRange);
			float nz = (float)((z-zLower)/zRange);
			float nw = (float)(blockWidth/xRange);
			
			
			float sw = nw*cWidth;
			float sx = nx*cWidth - (sw/2f);
			float sy = cHeight - (nz * cHeight) - (sw/2f);
			
			
			g2.fill(new Rectangle2D.Float(sx, sy, sw, sw));
			
			g2.setColor(Color.black);
			g2.setFont(new Font("Helvetica", Font.PLAIN, fontSize));
			
			String valueString = ob.getName();
			int stringLen = (int)g2.getFontMetrics().getStringBounds(valueString, g2).getWidth();
			int stringHeight = (int)g2.getFontMetrics().getStringBounds(valueString, g2).getHeight();
			int stringX = (int)((sx + (sw/2)) - (stringLen/2));
			int stringY = (int)((sy + (sw/2)) + (stringHeight/2));
			
			g2.drawString(valueString, stringX, stringY);
			
		}
		
		
		public Color getColorFromString(String colName){
			if(colName.equals("red")){
				return Color.red;
			}
			else if(colName.equals("green")){
				return Color.green;
			}
			else if(colName.equals("blue")){
				return Color.blue;
			}
			else if(colName.equals("yellow")){
				return Color.blue;
			}
			throw new RuntimeException("Unknown color name: " + colName);
		}
		

	}
	
	
	
	
	public static class RegionVisualizer implements ObjectPainter{
		
		double xLower;
		double xRange;
		double zLower;
		double zRange;
		
		int fontSize = 12;
		
		public RegionVisualizer(double xLower, double xUpper, double zLower, double zUpper){
			this.xRange = xUpper - xLower;
			this.xLower = xLower;
			
			this.zRange = zUpper - zLower;
			this.zLower = zLower;

		}

		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			g2.setColor(Color.black);
			
			double x = ob.getRealValForAttribute(PickupAndPlaceDomain.ATTLEFT);
			double z = ob.getRealValForAttribute(PickupAndPlaceDomain.ATTBOTTOM);
			double xr = ob.getRealValForAttribute(PickupAndPlaceDomain.ATTRIGHT);
			double zt = ob.getRealValForAttribute(PickupAndPlaceDomain.ATTTOP);
			
			double width = xr-x;
			double height = zt - z;
			
			float nx = (float)((x-xLower)/xRange);
			float nz = (float)((z-zLower)/zRange);
			float nw = (float)(width/xRange);
			float nh = (float)(height/zRange);
			
			float sx = nx*cWidth;
			float sy = cHeight - (nz * cHeight);
			float sw = nw*cWidth;
			float sh = nh*cHeight;
			
			sy-=sh;
			
			g2.draw(new Rectangle2D.Float(sx, sy, sw, sh));
			
			g2.setFont(new Font("Helvetica", Font.PLAIN, fontSize));
			
			String valueString = ob.getName();
			int stringLen = (int)g2.getFontMetrics().getStringBounds(valueString, g2).getWidth();
			int stringHeight = (int)g2.getFontMetrics().getStringBounds(valueString, g2).getHeight();
			int stringX = (int)((sx + (sw/2)) - (stringLen/2));
			//int stringY = (int)((sy + (sh/2)) + (stringHeight/2));
			//int stringX = (int)((sx + (sw/2)));
			int stringY = (int)((sy + (sh/2)) - stringHeight);
			
			g2.drawString(valueString, stringX, stringY);
			
		}
		
	}
	
}
