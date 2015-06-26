package domain.singleagent.dogtraining;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;

public class DTVisualizer {

	
	public static Visualizer getVisualizer(Domain d, int [][] map){
		
		Visualizer v = new Visualizer();
		
		v.addStaticPainter(new MapPainter(d, map));
		
		v.addObjectClassPainter(DogTraining.CLASSLOCATION, new LocationPainter(map));
		v.addObjectClassPainter(DogTraining.CLASSDOG, new DogPainter(map));
		v.addObjectClassPainter(DogTraining.CLASSTOY, new ToyPainter(map));
		
		
		
		return v;
	}
	
	public static Visualizer getVisualizer(Domain d, int [][] map, String imagePath){
		
		Visualizer v = new Visualizer();
		
		v.addStaticPainter(new MapPainter(d, map));
		
		v.addObjectClassPainter(DogTraining.CLASSLOCATION, new LocationPainter(map));
		v.addObjectClassPainter(DogTraining.CLASSDOG, new DogPainter(map, imagePath));
		v.addObjectClassPainter(DogTraining.CLASSTOY, new ToyPainter(map));
		
		
		
		return v;
	}
	
	
	public static class MapPainter implements StaticPainter{

		protected int 				dwidth;
		protected int 				dheight;
		protected int [][] 			map;
		
		
		public MapPainter(Domain domain, int [][] map) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
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
					
					if(this.map[i][j] == 1){
					
						float rx = i*width;
						float ry = cHeight - height - j*height;
					
						g2.fill(new Rectangle2D.Float(rx, ry, width, height));
						
					}
					
				}
			}
			
		}
		
		
	}
	
	
	
	
	
	public static class ToyPainter implements ObjectPainter{

		protected int						dwidth;
		protected int						dheight;
		protected int [][]					map;
		
		
		public ToyPainter(int [][] map) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
			
		}
		
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			
			ObjectInstance dog = s.getObjectsOfClass(DogTraining.CLASSDOG).get(0);
			int holding = dog.getIntValForAttribute(DogTraining.ATTHOLDING);
			if(holding > 0){
				ObjectInstance heldToy = s.getObjectsOfClass(DogTraining.CLASSTOY).get(holding-1);
				if(heldToy == ob){
					return; //don't render a held toy because that will be handled by the dog renderer
				}
			}
			
			g2.setColor(Color.yellow);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = ob.getIntValForAttribute(DogTraining.ATTX)*width;
			float ry = cHeight - height - ob.getIntValForAttribute(DogTraining.ATTY)*height;
			
			float dwidth = 0.2f*width;
			float dheight = 0.2f*height;
			
			rx += (width-dwidth)/2f;
			ry += (height-dheight)/2f;
			
			g2.fill(new Ellipse2D.Float(rx, ry, dwidth, dheight));
			
			
			
		}

		
	}
	
	
	
	public static class DogPainter implements ObjectPainter, ImageObserver{

		protected int						dwidth;
		protected int						dheight;
		protected int [][]					map;
		
		protected BufferedImage []			dogImages;
		
		public DogPainter(int [][] map) {
			this(map, "dataFiles/Resources/dogimages/");
		}
		
		public DogPainter(int [][] map, String imagePath) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
			
			if(!imagePath.endsWith("/")){
				imagePath = imagePath + "/";
			}
			
			dogImages = new BufferedImage[5];
			try{
				dogImages[0] = ImageIO.read(new File(imagePath + "dognorth.png"));
				dogImages[1] = ImageIO.read(new File(imagePath + "dogsouth.png"));
				dogImages[2] = ImageIO.read(new File(imagePath + "dogeast.png"));
				dogImages[3] = ImageIO.read(new File(imagePath + "dogwest.png"));
				dogImages[4] = ImageIO.read(new File(imagePath + "dogwait.png"));
			}catch(Exception e){
				System.out.println(e);
			}
			
		}
		
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			g2.setColor(Color.cyan);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = ob.getIntValForAttribute(DogTraining.ATTX)*width;
			float ry = cHeight - height - ob.getIntValForAttribute(DogTraining.ATTY)*height;
			
			float dwidth = 0.9f*width;
			float dheight = 0.9f*height;
			
			float bx = rx + (width-dwidth)/2f;
			float by = ry + (height-dheight)/2f;
			
			//g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
			
			
			//g2.fill(new Rectangle2D.Float(bx, by, dwidth, dheight));
			
			
			
			int lookingDir = ob.getIntValForAttribute(DogTraining.ATTLOOKING);
			
			
			
			float hx=0f;
			float hy=0f;
			float hwidth=0f;
			float hheight=0f;
			
			float nr = width/7f;
			
			float cx = rx + (width/2);
			float cy = ry + (height/2);
			
			float tx=0f;
			float ty=0f;
			float tw = 0.2f*width;
			float th = 0.2f*height;
			
			if(lookingDir == 0){
				//look north
				hx = cx-nr;
				hy = ry;
				hwidth = 2*nr;
				hheight = height/2f;
				
				tx = cx-(tw/2);
				ty = ry-(th/2);
				
			}
			else if(lookingDir == 1 || lookingDir == 4){
				//look south
				hx = cx-nr;
				hy = cy;
				hwidth = 2*nr;
				hheight = height/2f;
				
				tx = cx-(tw/2);
				ty = (ry+height)-(th/2);
			}
			else if(lookingDir == 2){
				//look east
				hx = cx;
				hy = cy-nr;
				hwidth = width/2f;
				hheight = 2*nr;
				
				tx = (rx+width)-(tw/2);
				ty = cy-(th/2);
				
			}
			else if(lookingDir == 3){
				//look west
				hx = rx;
				hy = cy-nr;
				hwidth = width/2f;
				hheight = 2*nr;
				
				tx = rx-(tw/2);
				ty = cy-(th/2);
			}
			
			//g2.fill(new Rectangle2D.Float(bx, by, dwidth, dheight));
			g2.drawImage(dogImages[lookingDir], (int)bx, (int)by, (int)dwidth, (int)dheight, this);
			
			
			
			
			/*
			if(lookingDir != 4){ //only render head if not looking up
				g2.fill(new Rectangle2D.Float(hx, hy, hwidth, hheight));
				int holding = ob.getIntValForAttribute(DogTraining.ATTHOLDING);
				if(holding > 0){
					g2.setColor(Color.yellow);
					g2.fill(new Ellipse2D.Float(tx, ty, tw, th));
				}
			}
			*/
			
			int holding = ob.getIntValForAttribute(DogTraining.ATTHOLDING);
			if(holding > 0){
				g2.setColor(Color.yellow);
				g2.fill(new Ellipse2D.Float(tx, ty, tw, th));
			}
			
			
		}


		@Override
		public boolean imageUpdate(Image arg0, int arg1, int arg2, int arg3,
				int arg4, int arg5) {
			return false;
		}

		
	}
	
	
	
	public static class LocationPainter implements ObjectPainter{


		protected int						dwidth;
		protected int						dheight;
		protected int [][]					map;
		protected Map<String, Color>		attColorMap;	
		
		
		
		public LocationPainter(int [][] map) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
			
			this.attColorMap = new HashMap<String, Color>();
			this.attColorMap.put(DogTraining.LIDRED, Color.red);
			this.attColorMap.put(DogTraining.LIDGREEN, Color.green);
			this.attColorMap.put(DogTraining.LIDBLUE, Color.blue);
			this.attColorMap.put(DogTraining.LIDHOME, Color.gray);
			
		}

		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			
			//set the color of the object
			g2.setColor(this.attColorMap.get(ob.getStringValForAttribute(DogTraining.ATTLOCID)));
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = ob.getIntValForAttribute(DogTraining.ATTX)*width;
			float ry = cHeight - height - ob.getIntValForAttribute(DogTraining.ATTY)*height;
			
			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
		
	}
	
	
	
	
	public static class CellPainter implements ObjectPainter{

		protected Color			col;
		protected int			dwidth;
		protected int			dheight;
		protected int [][]		map;
		
		public CellPainter(Color col, int [][] map) {
			this.col = col;
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
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
			
			float rx = ob.getIntValForAttribute(DogTraining.ATTX)*width;
			float ry = cHeight - height - ob.getIntValForAttribute(DogTraining.ATTY)*height;
			
			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
		
	}
	
}
