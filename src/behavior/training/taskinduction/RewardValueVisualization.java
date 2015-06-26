package behavior.training.taskinduction;

import burlap.oomdp.visualizer.RenderLayer;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * @author James MacGlashan.
 */
public class RewardValueVisualization implements RenderLayer {

	protected double currentReward = 0.;

	protected float width;
	protected float height;
	protected float x;
	protected float y;


	public RewardValueVisualization(float width, float height, float x, float y) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
	}

	public void setCurrentReward(double reward){
		this.currentReward = reward;
	}


	@Override
	public void render(Graphics2D g2, float width, float height) {

		if(this.currentReward == 0.){
			return;
		}

		if(this.currentReward == 1.){
			g2.setColor(Color.GREEN);
		}
		else if(this.currentReward == -1.){
			g2.setColor(Color.red);
		}


		g2.fill(new Ellipse2D.Float(this.x, this.y, this.width, this.height));


	}
}
