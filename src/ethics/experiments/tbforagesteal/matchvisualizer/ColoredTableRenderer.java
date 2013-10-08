package ethics.experiments.tbforagesteal.matchvisualizer;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

public class ColoredTableRenderer extends Canvas {

	private static final long serialVersionUID = 1L;

	
	protected int									valuePrecision;
	
	protected Map<String, ColoredTableCell>			cells;
	protected List<TableLabel>						labels;
	protected Color									bgColor;
	
	
	
	public static void main(String [] args){
		
		ColoredTableRenderer tabRen = new ColoredTableRenderer(2);
		
		LandmarkColorBlendInterpolation redwhiteblue = new LandmarkColorBlendInterpolation();
		redwhiteblue.addNextLandMark(-2., Color.red);
		redwhiteblue.addNextLandMark(0., Color.white);
		redwhiteblue.addNextLandMark(2., Color.blue);
		
		
		ColoredTableCell cell1 = new ColoredTableCell(redwhiteblue, 100, 100, 100, 100, 30);
		ColoredTableCell cell2 = new ColoredTableCell(redwhiteblue, 200, 100, 100, 100, 30);
		
		tabRen.putColorCell("cell1", cell1);
		tabRen.putColorCell("cell2", cell2);
		
		tabRen.setCellValue("cell1", -1.);
		tabRen.setCellValue("cell2", 1.);
		
		tabRen.addLabel(new TableLabel("label 1", 15, 150, 50));
		tabRen.addLabel(new TableLabel("label 2", 15, 250, 50));
		
		tabRen.setPreferredSize(new Dimension(800, 800));
		
		JFrame aFrame = new JFrame();
		aFrame.getContentPane().add(tabRen, BorderLayout.SOUTH);
		
		aFrame.pack();
		aFrame.setVisible(true);
		
	}
	

	public ColoredTableRenderer(int valuePrecision) {
		this.valuePrecision = valuePrecision;
		this.cells = new HashMap<String, ColoredTableRenderer.ColoredTableCell>();
		this.labels = new ArrayList<ColoredTableRenderer.TableLabel>();
		this.bgColor = Color.white;
	}
	
	public void setCellValuePrecision(int p){
		this.valuePrecision = p;
	}
	
	public int getCellValuePrecision(){
		return this.valuePrecision;
	}
	
	
	public void putColorCell(String name, ColoredTableCell cell){
		this.cells.put(name, cell);
	}
	
	public void setCellValue(String name, double v){
		this.cells.get(name).value = v;
	}
	
	public void addLabel(TableLabel label){
		this.labels.add(label);
	}
	
	
	public void paint(Graphics g){
		
		Graphics2D g2 = (Graphics2D) g;
		
		
		//clear screen
		g2.setColor(bgColor);
		g2.fill(new Rectangle(this.getWidth(), this.getHeight()));
	
		
		//render cells
		for(ColoredTableCell cell : cells.values()){
			
			//first draw cell
			Color fillColor = cell.getColor();
			g2.setColor(fillColor);
			g2.fill(new Rectangle2D.Float(cell.x, cell.y, cell.width, cell.height));
			g2.setColor(Color.black);
			g2.draw(new Rectangle2D.Float(cell.x, cell.y, cell.width, cell.height));
			
			
			//now write value
			g2.setColor(Color.black);
			g2.setFont(new Font("Helvetica", Font.PLAIN, cell.fontPointSize));
			
			String valueString = String.format("%."+valuePrecision+"f", cell.value);
			int stringLen = (int)g2.getFontMetrics().getStringBounds(valueString, g2).getWidth();
			int stringHeight = (int)g2.getFontMetrics().getStringBounds(valueString, g2).getHeight();
			int stringX = (int)((cell.x + (cell.width/2)) - (stringLen/2));
			int stringY = (int)((cell.y + (cell.height/2)) + (stringHeight/2));
			
			g2.drawString(valueString, stringX, stringY);
			
		}
		
		//render labels
		for(TableLabel label : this.labels){
			
			g2.setColor(Color.black);
			g2.setFont(new Font("Helvetica", Font.PLAIN, label.fontPointSize));
			
			int stringLen = (int)g2.getFontMetrics().getStringBounds(label.label, g2).getWidth();
			int stringHeight = (int)g2.getFontMetrics().getStringBounds(label.label, g2).getHeight();
			
			int stringX = label.cX - (stringLen / 2);
			int stringY = label.cY + (stringHeight / 2);
			
			g2.drawString(label.label, stringX, stringY);
			
			
		}
		
		
	}
	
	
	
	
	
	
	
	
	
	public static class ColoredTableCell{
		
		public double				value;
		public ColorBlend			colorBlend;
		
		public float				x;
		public float				y;
		public float				width;
		public float				height;
		
		public int					fontPointSize;
		
		
		
		public ColoredTableCell(ColorBlend colorBlend, float x, float y, float width, float height, int fontPointSize){
			this.colorBlend = colorBlend;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.fontPointSize = fontPointSize;
		}
		
		public Color getColor(){
			return this.colorBlend.color(this.value);
		}
		
		
		
	}
	
	
	
	
	
	public static class TableLabel{
		
		public String		label;
		public int			fontPointSize;
		
		public int			cX;
		public int			cY;
		
		
		public TableLabel(String label, int fontPointSize, int cX, int cY){
			this.label = label;
			this.fontPointSize = fontPointSize;
			this.cX = cX;
			this.cY = cY;
		}
		
		
	}
	
}
