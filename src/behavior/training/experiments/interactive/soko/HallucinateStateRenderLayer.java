package behavior.training.experiments.interactive.soko;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.RenderLayer;
import burlap.oomdp.visualizer.StateRenderLayer;

public class HallucinateStateRenderLayer implements RenderLayer {

	StateRenderLayer srcLayer;
	State curState = null;
	float opactiy = 0.7f;
	
	public HallucinateStateRenderLayer(StateRenderLayer srcLayer){
		this.srcLayer = srcLayer;
	}
	
	public void setOpacity(float opacity){
		this.opactiy = opacity;
	}
	
	public void setSrcStateRenderLayer(StateRenderLayer srcLayer){
		this.srcLayer = srcLayer;
	}
	
	public void updateState(State s){
		this.curState = s;
		this.srcLayer.updateState(s);
	}
	
	@Override
	public void render(Graphics2D g2, float width, float height) {
		
		if(this.curState == null){
			return;
		}
		

		BufferedImage img = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg2 = img.createGraphics();
		this.srcLayer.render(sg2, width, height);
		
		Composite oldComp = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, this.opactiy));
		g2.drawImage(img, 0, 0, null);
		g2.setComposite(oldComp);
		
		sg2.dispose();
		
	}

}
