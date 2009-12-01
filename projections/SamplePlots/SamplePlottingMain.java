package projections.SamplePlots;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.gui.graph.DataSource;
import projections.gui.graph.DataSource1D;
import projections.gui.graph.Graph;
import projections.gui.graph.XAxis;
import projections.gui.graph.XAxisFixed;
import projections.gui.graph.YAxis;
import projections.gui.graph.YAxisFixed;


public class SamplePlottingMain {

	public static void main(String args[]){
			System.out.println("Displaying two Sample graphs");		
			createPlotInFrameJFreeChart();
			createPlotInFrameGraph();
	}

	
	
	/** Create a window with a simple plot in it. Uses the publicly available jfreechart package. */
	public static void createPlotInFrameJFreeChart(){
		
		// create data
        XYSeries s = new XYSeries("All Event Types", true, false);
        for(int i=0;i<20;i++){
        	s.add(i,Math.random());
        }	

        // Create a dataset
        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        dataset.addSeries(s);

        // Create axis labels
        NumberAxis domainAxis = new NumberAxis("My X Axis Label");
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());       
        NumberAxis rangeAxis = new NumberAxis("My Y Axis Label");

        // Create renderer
        StackedXYBarRenderer renderer = new StackedXYBarRenderer();
        renderer.setDrawBarOutline(true);
        
        // Create the plot, using the renderer and the dataset and the axis
        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

        // Create a chart using the plot
        JFreeChart chart = new JFreeChart("Plot Name", plot);

        // Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(chart);
	
		// Put the chartpanel in a new window(JFrame)
		JFrame window = new JFrame("Plot Window jfreechart");
		window.setLayout(new BorderLayout());
		window.add(chartpanel, BorderLayout.CENTER);
		
		
		JButton mybutton = new JButton("Click Me");
		window.add(mybutton, BorderLayout.SOUTH);
				
		MyActionHandler h =	new MyActionHandler(plot);

		mybutton.addActionListener( h);
		
	
		// Display the window	
		window.pack();
		window.setVisible(true);
	}
	
	
	
	/** Create a window with a simple plot in it. Uses projection's own home-brewed graph class. */
	public static void createPlotInFrameGraph(){

		// Create axis
		XAxis x=new XAxisFixed("My X Axis Label", "u");
		YAxis y=new YAxisFixed("My Y Axis Label", "u", 0);
		
		// create data
		int[] data = new int[20];
        for(int i=0;i<20;i++){
        	data[i] = (int)(Math.random()*10.0);
        }	
		DataSource d = new DataSource1D("Plot Name", data);
		
		
		// Create a new graph
		Graph g=new Graph();
	
		
		
		// Tell our graph to use our axis labels and our data
		g.setData(d,x,y);
		
		
		// Put the graph in a new window(JFrame)
		JFrame window = new JFrame("Plot Window projection.gui.graph");
		window.setLayout(new BorderLayout());
		window.add(g, BorderLayout.CENTER);
	
		// Display the window	
		window.pack();
		window.setVisible(true);
		
		
	}

	

	
	
}
