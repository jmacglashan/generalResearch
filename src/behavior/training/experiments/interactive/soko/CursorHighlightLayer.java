package behavior.training.experiments.interactive.soko;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import burlap.oomdp.visualizer.RenderLayer;

public class CursorHighlightLayer implements RenderLayer {

	protected int		cellX = -1;
	protected int		cellY = -1;
	
	protected float		maxX;
	protected float		maxY;
	
	protected boolean	hide = false;
	
	public CursorHighlightLayer(int maxX, int maxY){
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	public void updateCell(int x, int y){
		this.cellX = x;
		this.cellY = y;
	}
	
	protected int getCellX(){
		return this.cellX;
	}
	protected int getCellY(){
		return this.cellY;
	}
	
	public void setDim(int maxX, int maxY){
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	public void setHidden(boolean hidden){
		this.hide = hidden;
	}
	
	@Override
	public void render(Graphics2D g2, float width, float height) {
		if(hide){
			return;
		}
		
		if(cellX >=0 && cellY >= 0){
			
			//determine then normalized width
			float nwidth = (1.0f / maxX) * width;
			float nheight = (1.0f / maxY) * height;
			
			float rx = this.cellX*nwidth;
			float ry = height - nheight - this.cellY*nheight;
			
			g2.setColor(Color.gray);
			g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, 0, 1f, new float[]{6f}, 0f));
			
			g2.draw(new Rectangle2D.Float(rx, ry, nwidth, nheight));
			
		}

	}

}
