package metarl.examplegraphs;

import java.awt.Color;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import burlap.debugtools.RandomFactory;


public class BiasVarianceGraph extends JFrame{

	private static final long serialVersionUID = 1L;
	
	
	protected double 							optimalPerformanceValue;
	
	protected XYSeries							truePerformance = new XYSeries("Test Performance");
	protected XYSeries							optimalPerformance = new XYSeries("Optimal Perfomrance");
	protected YIntervalSeries					samplePerformance = new YIntervalSeries("Sample Performance");
	
	
	protected YIntervalSeriesCollection			intervalCollection = new YIntervalSeriesCollection();
	protected XYSeriesCollection				lineCollection = new XYSeriesCollection();
	
	
	
	
	
	public BiasVarianceGraph(double optimalPerformance, int width, int height){
		this.optimalPerformanceValue = optimalPerformance;
		
		
		this.lineCollection.addSeries(this.truePerformance);
		this.lineCollection.addSeries(this.optimalPerformance);
		
		this.intervalCollection.addSeries(this.samplePerformance);
		
		final JFreeChart chart = ChartFactory.createXYLineChart("Bias-Variance", "Number of Environment Samples", "Performance", lineCollection);
		XYPlot plot = chart.getXYPlot();

		plot.setDataset(1, intervalCollection);
		plot.setRenderer(1, this.createDeviationRenderer());
		
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(width, height));
		this.getContentPane().add(chartPanel);
		
		
		this.pack();
		this.setVisible(true);
		
	}
	
	
	
	public void addSamplePerformance(double samplePerfomance, double generalizationeErrorAmount, double truePerformance){
		
		int m = this.truePerformance.getItemCount()+1;
		
		this.truePerformance.add(m, truePerformance);
		this.optimalPerformance.add(m, this.optimalPerformanceValue);
		this.samplePerformance.add(m, samplePerfomance, samplePerfomance-generalizationeErrorAmount, samplePerfomance);
	}
	
	
	
	/**
	 * Creates a DeviationRenderer to use for the trial average plots
	 * @return a DeviationRenderer
	 */
	protected DeviationRenderer createDeviationRenderer(){
		DeviationRenderer renderer = new DeviationRenderer(true, false);
		
		/*
		for(int i = 0; i < DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length; i++){
			Color c = (Color)DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[i];
			Color nc = new Color(c.getRed(), c.getGreen(), c.getBlue(), 100);
			renderer.setSeriesFillPaint(i, nc);
		}
		*/
		
		renderer.setSeriesFillPaint(0, Color.green);
		
		return renderer;
	}
	
	
	
	
	public static void main(String [] args){
		
		double optPerformance = 0.7;
		double sampleBest = 0.9;
		double xscal = 0.1;
		double xoffset = 0;
		
		BiasVarianceGraph graph = new BiasVarianceGraph(optPerformance, 700, 500);
		
		for(int i = 1; i <= 50; i++){
			double samp = evalSampleAtM(i, xscal, xoffset, sampleBest, optPerformance);
			double trueP = evalTrueAtM(i, xscal, xoffset, sampleBest, optPerformance);
			double err = sampleError(samp, trueP, 0.05);
			
			graph.addSamplePerformance(samp, err, trueP);
			
		}
		
		
	}
	
	
	
	static double evalSampleAtM(int m, double xscal, double xoffset, double sampleUpper, double sampleLower){
		double width = sampleUpper - sampleLower;
		double val = -mainFunction(m, xscal, xoffset) + 1;
		val *= width;
		return val + sampleLower;
	}
	
	static double evalTrueAtM(int m, double xscal, double xoffset, double sampleUpper, double sampleLower){
		double width = sampleUpper - sampleLower;
		double val = mainFunction(m, xscal, xoffset) + 1;
		val *= width;
		return val + 1.-sampleLower;
	}
	
	static double mainFunction(int m, double xscal, double xoffset){
		double expInner = -1. * (m*xscal + xoffset);
		double denom = 1. + Math.exp(expInner);
		
		double first = (1. / denom);
		
		return first;
		
	}
	
	
	static double sampleError(double samp, double trueP, double delta){
		double dist = Math.abs(samp - trueP);
		double w = 1.0005*dist;
		double baseError = dist - delta*w;
		
		double r = RandomFactory.getMapped(0).nextDouble()*w + baseError;
		
		return r;
		
	}
	
	
}
