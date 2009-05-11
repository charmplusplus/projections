package projections.streaming;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.ccs.CcsProgress;
import projections.ccs.CcsThread;

public class MultiSeriesHandler {

	CcsThread ccs;

	Vector<Float> averageTimes;         
	Vector<Float> allTimes;


	private String server;
	private int port;
	private String ccsHandler;

	CategoryPlot plot;
	DefaultCategoryDataset dataset;

	private String chartTitle;
	

	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}


	public class progressHandler implements CcsProgress {
		public progressHandler(){

		}

		public void setText(String s) {
			//			System.out.println("CCS Message: " + s);
		}


	}


	private class dataRequest extends CcsThread.request{
		private	int nextXValue;

		public dataRequest() {
			super(ccsHandler,0);
			nextXValue = 0;
		}

		public void handleReply(byte[] data){

			if(data.length>1){

				if(ccsHandler.equals("CkPerfSumDetail uchar")){

					int numData = 0;
					double sum = 0.0;
					System.out.println("Received " + data.length + " byte data array");

					int numEPs = unsignedByteToInt(data[0]);
					int numBins = (data.length - 1) / numEPs;
					System.out.println("Received " + numBins + " bins each with " + numEPs + "EPs");

					double sums[] = new double[numEPs];
					for(int e=0;e<numEPs; e++){
						sums[e] = 0.0;
					}

					for(int i=0; i<numBins; i++){
						for(int e=0;e<numEPs; e++){
							sums[e] += unsignedByteToInt(data[1+i*numEPs+e]);						
						}					
					}

					boolean added = false;
					String xvalue = "" + nextXValue;
					for(int e=0;e<numEPs; e++){
						double average = sums[e]/numBins;
						double utilization = average / 2.0;
						if(average > 0.0){
							String stackCategory = ""+e;
							dataset.addValue(utilization, stackCategory, xvalue );					
							added = true;
						}
					}	
					if(added = true){
						nextXValue ++;
					}

				} else if(ccsHandler.equals("CkPerfSumDetail compressed") || ccsHandler.equals("CkPerfSumDetail compressed PE0")) {

					int numData = 0;
					double sum = 0.0;
					System.out.println("Received " + data.length + " byte data array");

					// start parsing this array
					int numEPs = 1024;
					double sums[] = new double[1024];
					for(int e=0;e<numEPs; e++){
						sums[e] = 0.0;
					}

					int numBins = 0;
					int pos = 0;
					while(pos < data.length-1){
						numBins ++;
						int numEpForBin = unsignedByteToInt(data[pos]);
						pos++;
						double utilizationForThisBin = 0.0;
						for(int i=0; i<numEpForBin; i++){
							int ep = unsignedByteToInt(data[pos]);
							pos++;
							int utilization = unsignedByteToInt(data[pos]);
							pos++;

							// Because we are just averaging all the samples in the ccs message, we can just sum them here.
							sums[ep] += utilization;
							utilizationForThisBin += utilization;
							//							System.out.println("ep="+ep+" utilization="+utilization);
						}
						//						System.out.println("utilizationForThisBin="+utilizationForThisBin);
					}

					boolean added = false;
					String xvalue = "" + nextXValue;
					for(int e=0;e<numEPs; e++){
						double average = (double)sums[e]/(double)numBins;
						double utilization = average / 2.0;
						if(average > 0.0){
							dataset.addValue(utilization, ""+e, xvalue );					
							added = true;
						}
					}	
					if(added = true){
						nextXValue ++;
					}

				}

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Request more data
			ccs.addRequest(this, true);
		}


	}


	/** Constructor */	
	MultiSeriesHandler(String hostname, int port, String ccsHandler){

		System.out.println("StreamingDataHandler constructor");

		averageTimes = new Vector<Float>();
		allTimes = new Vector<Float>();

		this.server = hostname;
		this.port = port;
		this.ccsHandler = ccsHandler;

		progressHandler h = new progressHandler();
		ccs = new CcsThread(h,this.server,this.port);


		if(ccsHandler.equals("CkPerfSumDetail uchar")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed")) {
			chartTitle = "Utilization Stacked by EP All Processors";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed PE0")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		}

		createPlotInFrameJFreeChart();

		/** Create first request */
		ccs.addRequest(new dataRequest());

	}

	/** Generate the plot that will now be displayed */
	public void updatePlot(){

	}


	/** Create a window with a simple plot in it. Uses the publicly available jfreechart package. */
	public void createPlotInFrameJFreeChart(){

		dataset = new DefaultCategoryDataset();

		JFreeChart chart = ChartFactory.createStackedAreaChart(
				chartTitle,
				"CCS Reply Messages",                // domain axis label
				"Average Utilization in the CCS request",                   // range axis label
				dataset,                   // data
				PlotOrientation.VERTICAL,  // orientation
				true,                      // include legend
				true,
				false
		);

		chart.setBackgroundPaint(Color.white);


		plot = (CategoryPlot) chart.getPlot();

		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		domainAxis.setCategoryMargin(0.0);

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(chart);

		// Put the chartpanel in a new window(JFrame)
		JFrame window = new JFrame("Streaming Utilization Plot");
		window.setLayout(new BorderLayout());
		window.add(chartpanel, BorderLayout.CENTER);


		// Display the window	
		window.pack();
		window.setVisible(true);
	}



}
