package behavior.training.experiments.simulated.grid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.StaticPainter;

public class StaticMultiValMapPainter implements StaticPainter {

	protected int [][] map;
	protected int 				dwidth;
	protected int 				dheight;
	protected Map<Integer, Color> special;
	protected Color nonZeroDefault = Color.blue;
	
	
	public StaticMultiValMapPainter(int [][] map){
		this.map = map;
		this.dwidth = map.length;
		this.dheight = map[0].length;
		this.special = new HashMap<Integer, Color>();
	}
	
	public void setSpecial(int code, Color col){
		this.special.put(code, col);
	}
	
	@Override
	public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {
		
		
		float domainXScale = this.dwidth;
		float domainYScale = this.dheight;
		
		//determine then normalized width
		float width = (1.0f / domainXScale) * cWidth;
		float height = (1.0f / domainYScale) * cHeight;
		
		//pass through each cell of the map and if it is a wall, draw it
		for(int i = 0; i < this.dwidth; i++){
			for(int j = 0; j < this.dheight; j++){
				
				if(this.map[i][j] != 0){
				
					Color col = this.special.get(this.map[i][j]);
					if(col == null){
						col = this.nonZeroDefault;
					}
					
					g2.setColor(col);
					
					float rx = i*width;
					float ry = cHeight - height - j*height;
				
					g2.fill(new Rectangle2D.Float(rx, ry, width, height));
					
				}
				
				
			}
		}
		

	}

	
	

}
