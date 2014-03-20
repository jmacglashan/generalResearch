package metarl.examplegraphs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import burlap.debugtools.RandomFactory;

public class AlgorithmClassChoiceGraph extends JFrame{

	private static final long serialVersionUID = 1L;
	
	protected List<YIntervalSeries> 			algSeries;
	
	protected YIntervalSeriesCollection			intervalCollection = new YIntervalSeriesCollection();
	
	
	public AlgorithmClassChoiceGraph(int numAlgsToCompare, int width, int height){
		
		this.algSeries = new ArrayList<YIntervalSeries>(numAlgsToCompare);
		for(int i = 0; i < numAlgsToCompare; i++){
			String name = "Algorithm " + i;
			YIntervalSeries series = new YIntervalSeries(name);
			this.algSeries.add(series);
			this.intervalCollection.addSeries(series);
		}
		
		
		final JFreeChart chart = ChartFactory.createXYLineChart("Algorithm Class Lower Bound Comparison", "Number of Environment Samples", "Performance", intervalCollection);
		XYPlot plot = chart.getXYPlot();
		plot.setRenderer(this.createDeviationRenderer());
		
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(width, height));
		this.getContentPane().add(chartPanel);
		
		
		this.pack();
		this.setVisible(true);
		
	}
	
	public void addAlgorithmPerformance(int alg, double performance, double generalizationError){
		YIntervalSeries series = this.algSeries.get(alg);
		int m = series.getItemCount()+1;
		
		series.add(m, performance, performance-generalizationError, performance);
		
	}
	
	
	
	
	/**
	 * Creates a DeviationRenderer to use for the trial average plots
	 * @return a DeviationRenderer
	 */
	protected DeviationRenderer createDeviationRenderer(){
		DeviationRenderer renderer = new DeviationRenderer(true, false);
	
		for(int i = 0; i < DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length; i++){
			Color c = (Color)DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[i];
			Color nc = new Color(c.getRed(), c.getGreen(), c.getBlue(), 100);
			renderer.setSeriesFillPaint(i, nc);
		}
		
		return renderer;
	}
	
	
	
	public static void main(String [] args){
		
		AlgorithmClassChoiceGraph graph = new AlgorithmClassChoiceGraph(2, 700, 500);
		
		double optPerformance = 0.6;
		double sampleBest = 0.8;
		double xscal = 0.05;
		double xoffset = 0.1;
		
		
		for(int i = 1; i <= 50; i++){
			double samp = evalSampleAtM(i, xscal, xoffset, sampleBest, optPerformance);
			double trueP = evalTrueAtM(i, xscal, xoffset, sampleBest, optPerformance, 0.2);
			//double err = sampleError(samp, trueP, 0.7, 0.05, 1.000);
			double err = Math.abs(trueP-samp);
			err = RandomFactory.getMapped(0).nextDouble()*0.2*err + err;
			
			graph.addAlgorithmPerformance(0, samp, err);
			
		}
		
		
		
		optPerformance = 0.7;
		sampleBest = 0.9;
		xscal = 0.05;
		xoffset = 0.1;
		
		for(int i = 1; i <= 50; i++){
			double samp = evalSampleAtM(i, xscal, xoffset, sampleBest, optPerformance);
			double trueP = evalTrueAtM(i, xscal, xoffset, sampleBest, optPerformance, 0.3);
			//double err = sampleError(samp, trueP, 1., 0.5, 2.);
			double err = 2*Math.abs(trueP-samp);
			err = RandomFactory.getMapped(0).nextDouble()*0.2*err + err;
			
			graph.addAlgorithmPerformance(1, samp, err);
			
		}
		
		
	}
	
	
	
	
	static double evalSampleAtM(int m, double xscal, double xoffset, double sampleUpper, double sampleLower){
		double width = sampleUpper - sampleLower;
		double val = -mainFunction(m, xscal, xoffset) + 1;
		val *= width;
		return val + sampleLower;
	}
	
	static double evalTrueAtM(int m, double xscal, double xoffset, double sampleUpper, double sampleLower, double yscale){
		double width = sampleUpper - sampleLower;
		double val = mainFunction(m, xscal, xoffset) + 1;
		val *= width;
		return val + yscale;
	}
	
	static double mainFunction(int m, double xscal, double xoffset){
		double expInner = -1. * (m*xscal + xoffset);
		double denom = 1. + Math.exp(expInner);
		
		double first = (1. / denom);
		
		return first;
		
	}
	
	
	static double sampleError(double samp, double trueP, double constMult, double delta, double distMult){
		double dist = constMult*Math.abs(samp - trueP);
		double w = distMult*dist;
		double baseError = dist - delta*w;
		
		double r = RandomFactory.getMapped(0).nextDouble()*w + baseError;
		
		return r;
		
	}
	

}
