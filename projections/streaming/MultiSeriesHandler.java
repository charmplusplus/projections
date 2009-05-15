package projections.streaming;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Iterator;
import java.util.TreeMap;
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

	private String server;
	private int port;
	private String ccsHandler;

	CategoryPlot plot;
	DefaultCategoryDataset dataset;

	private String chartTitle;


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

			int unnecessaryRequests = 0; // Record how many times we requested something and got back nothing.

			if(data.length>1){

				if(ccsHandler.equals("CkPerfSumDetail compressed")) {

					int numData = 0;
					double sum = 0.0;
					System.out.println("\"CkPerfSumDetail compressed\" Received " + data.length + " byte data array");

					int numEPs = 1000;	
					int pos = 0;


					// start parsing this array
					int numBins = ByteParser.bytesToInt(data, pos);
					pos += 4;
					System.out.println("Number of bins in message = " + numBins);

					if(numBins < 1){
						unnecessaryRequests++; // Record how many times we requested something and got back nothing.
					} else {

						int numProcs = ByteParser.bytesToInt(data, pos);
						pos += 4;
						System.out.println("Number of processors contributing data in message = " + numProcs);

						int numBinsPerPlotSample = 100;
						int binsRemaining = numBins;
						int currentBin = 0;
						while(binsRemaining>0){

							double sums[] = new double[1024];
							for(int e=0;e<numEPs; e++){
								sums[e] = 0.0;
							}
							int samplesInThisBin = 0;
							for(int b=0; b<numBinsPerPlotSample; b++){

								if(pos+4 <= data.length){
									
									// Read the number of entries for this bin
									int numEntries = ByteParser.bytesToShort(data, pos);
									pos += 2;
									
									
								//	System.out.print("Number of entries in bin " + b + " is " + numEntries + ": ");
									
									for(int e=0;e<numEntries;e++){
										
										int ep = ByteParser.bytesToShort(data, pos);
										pos += 2;
										
										
//										float u = ByteParser.bytesToFloat(data, pos);
//										pos += 4;
										float u = ByteParser.bytesToUnsignedChar(data, pos);
										pos += 1;

										
										u = u / 2.5f; // The range of values supplied for u is 0 to 250.
										
//										System.out.print("("+ep+","+u+") ");
										sums[ep] += u;
										samplesInThisBin ++;
									}
//									System.out.println("");

									binsRemaining--;
								}
							}

							boolean added = false;
							String xvalue = "" + nextXValue;
							for(int e=0; e<numEPs; e++){
								double utilization = (double)sums[e]/(double)samplesInThisBin;
								if(utilization > 0.0){
									dataset.addValue(utilization, ""+e, xvalue );
									added = true;
								}
							}
							if(added == true){
								try {
									Thread.sleep(50);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								nextXValue ++;
							}

						}					}
				}

				while(unnecessaryRequests>0){
					try {
						Thread.sleep(150);
						unnecessaryRequests--;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// Request more data
			ccs.addRequest(this, true);
		}


	}


	/** Constructor */	
	MultiSeriesHandler(String hostname, int port, String ccsHandler){

		System.out.println("StreamingDataHandler constructor");

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
				"",                // domain axis label
				"Utilization Averaged Over Some # of Samples",                   // range axis label
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
