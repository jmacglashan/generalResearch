package tests.irltests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.RenderLayer;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;


/**
 * @author James MacGlashan.
 */
public class TrajectoryRenderer implements RenderLayer{

	protected List<EpisodeAnalysis> episodes;
	protected float strokeWidth;
	protected String agentClass;
	protected String xAtt;
	protected String yAtt;
	protected double maxX;
	protected double maxY;
	protected double minX;
	protected double minY;
	protected double xOffset;
	protected double yOffset;
	protected float circleRadius;
	protected List<Color> colors;

	public TrajectoryRenderer(List<EpisodeAnalysis> episodes, String agentClass, String xAtt, String yAtt, double [] minMaxX, double [] minMaxY, float strokeWidth, float circleRadius){
		this.episodes = episodes;
		this.agentClass = agentClass;
		this.xAtt = xAtt;
		this.yAtt = yAtt;
		this.minX = minMaxX[0];
		this.minY = minMaxY[0];
		this.maxX = minMaxX[1];
		this.maxY = minMaxY[1];
		this.xOffset = minMaxX[2];
		this.yOffset = minMaxY[2];
		this.strokeWidth = strokeWidth;
		this.circleRadius = circleRadius;
		this.colors = new ArrayList<Color>();
		this.colors.add(Color.black);
	}

	public void setColors(List<Color> colors){
		this.colors = colors;
	}

	@Override
	public void render(Graphics2D g2, float width, float height) {

		g2.setStroke(new BasicStroke(this.strokeWidth));

		for(int i = 0; i < this.episodes.size(); i++){
			g2.setColor(this.colors.get(i%this.colors.size()));

			double [][] points = this.getNormalizedPoints(this.episodes.get(i));
			GeneralPath polyLine = new GeneralPath(GeneralPath.WIND_EVEN_ODD, points.length);

			double [] s0 = this.convertToScreenSpace(points[0][0], points[0][1], width, height);
			polyLine.moveTo(s0[0], s0[1]);

			//paint start circle
			g2.fill(new Ellipse2D.Float((float)(s0[0]-this.circleRadius), (float)(s0[1]-this.circleRadius), this.circleRadius*2, this.circleRadius*2));

			for(int j = 1; j < points.length; j++){
				double [] sj = this.convertToScreenSpace(points[j][0], points[j][1], width, height);
				polyLine.lineTo(sj[0], sj[1]);
			}

			g2.draw(polyLine);

		}

	}

	protected double[] convertToScreenSpace(double x, double y, float width, float height){

		double sx = x * width;
		double sy = height - (y*height);

		return new double[]{sx,sy};

	}

	protected double [][] getNormalizedPoints(EpisodeAnalysis ea){
		double [][] points = new double[ea.numTimeSteps()][2];
		for(int t = 0; t < ea.numTimeSteps(); t++){
			double [] np = this.getNormalizedXY(ea.getState(t));
			points[t][0] = np[0];
			points[t][1] = np[1];
		}
		return points;
	}

	protected double [] getNormalizedXY(State s){
		ObjectInstance agent = s.getFirstObjectOfClass(this.agentClass);
		double x = agent.getNumericValForAttribute(this.xAtt) + this.xOffset;
		double y = agent.getNumericValForAttribute(this.yAtt) + this.yOffset;

		double rx = this.maxX - this.minX;
		double ry = this.maxY - this.minY;

		return new double[]{(x-this.minX)/rx, (y-this.minY)/ry};

	}
}
