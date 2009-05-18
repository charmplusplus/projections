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

import projections.SamplePlots.MyActionHandler;
import projections.analysis.StsReader;
import projections.ccs.CcsProgress;
import projections.ccs.CcsThread;
import projections.misc.LogLoadException;

public class MultiSeriesHandler {

	CcsThread ccs;

	private String server;
	private int port;
	private String ccsHandler;

	CategoryPlot plot;
	DefaultCategoryDataset dataset;

	StsReader sts;

	/** A dataset that holds the sizes of the received messages */
	DefaultTableXYDataset sizeDataset;
	XYSeries sizeDataSeries;
	XYPlot sizePlot;
	int numSizesSoFar = 0;

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

			if(data.length>1){

				if(ccsHandler.equals("CkPerfSumDetail compressed")) {

					int numData = 0;
					double sum = 0.0;
					System.out.println("\"CkPerfSumDetail compressed\" Received " + data.length + " byte data array");

					int numEPs = 1000;	
					int pos = 0;
					double totalSum = 0.0;

					// start parsing this array
					int numBins = ByteParser.bytesToInt(data, pos);
					pos += 4;
					System.out.println("Number of bins in message = " + numBins);

					if(numBins < 1){
						//						unnecessaryRequests++; // Record how many times we requested something and got back nothing.
					} else {
						sizeDataSeries.add(numSizesSoFar++, data.length);

						int numProcs = ByteParser.bytesToInt(data, pos);
						pos += 4;
						System.out.println("Number of processors contributing data in message = " + numProcs);

						int numBinsPerPlotSample = 200;
						int binsRemaining = numBins;
						int currentBin = 0;
						while(binsRemaining>0){

							double sums[] = new double[1024];
							double otherUtilization = 0.0;
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

										float u = ByteParser.bytesToUnsignedChar(data, pos);
										pos += 1;


										u = u / 2.5f; // The range of values supplied for u is 0 to 250.

										totalSum += u;

										if(ep >= 0 && ep < sums.length){
											// Normal EP values
											sums[ep] += u;
										} else {
											// Other category
											otherUtilization += u;
										}

									}
									
									samplesInThisBin ++;
									binsRemaining--;
								}
							}

							boolean added = false;
							String xvalue = "" + nextXValue;
							for(int e=0; e<numEPs; e++){
								double utilization = (double)sums[e]/(double)samplesInThisBin;
								if(utilization > 0.0){
									String epName = sts.getEntryNameByID(e);
									
							        int p = epName.indexOf('('); // first occurrence of a '(' in the string
							        epName = epName.substring(0, p);
							        
									System.out.println("name for " + e + " is " + epName);
									if(epName != null)
										dataset.addValue(utilization, epName, xvalue );
									else
										dataset.addValue(utilization, ""+e, xvalue );

									added = true;
								}
							}
							// Add an other category as well
							double utilization = otherUtilization/(double)samplesInThisBin;
							if(utilization > 0.0){
								dataset.addValue(utilization, "Other", xvalue );
								System.out.println("Adding other with utilization " + utilization + " and samplesInThisBin=" + samplesInThisBin);
								added = true;
							}




							// advance to next x axis point
							if(added == true){
								nextXValue ++;
							}

						}

						double avg = totalSum/numBins;
						System.out.println("Average Utilization=" + avg);

					}


				}

				try {
					Thread.sleep(100);
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

		this.server = hostname;
		this.port = port;
		this.ccsHandler = ccsHandler;

		progressHandler h = new progressHandler();
		ccs = new CcsThread(h,this.server,this.port);


		// try to load sts file
		String filename = "/tmp/jacobi2d.sts";

		try {
			sts = new StsReader(filename);
		} catch (LogLoadException e) {
			System.out.println("Couldn't load sts file with name " + filename);
		}



		if(ccsHandler.equals("CkPerfSumDetail uchar")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed")) {
			chartTitle = "Utilization Stacked by EP All Processors";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed PE0")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		}

		createStackedUtilizationChart();
		createMessageSizeChart();

		/** Create first request */
		ccs.addRequest(new dataRequest());

	}

	/** Generate the plot that will now be displayed */
	public void updatePlot(){

	}


	/** Create a window with a simple plot in it. Uses the publicly available jfreechart package. */
	public void createStackedUtilizationChart(){

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
		domainAxis.setAxisLineVisible(false);
		domainAxis.setTickLabelsVisible(false);

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(chart);

		// Put the chartpanel in a new window(JFrame)
		JFrame window = new JFrame("Streaming Utilization Plot");
		window.setLayout(new BorderLayout());
		window.add(chartpanel, BorderLayout.CENTER);


		// Display the window	
		window.pack();
		window.setLocation(20,30);
		window.setVisible(true);
	}


	/** Create a window with a simple plot in it. Uses the publicly available jfreechart package. */
	public void createMessageSizeChart(){

		sizeDataSeries = new XYSeries("CCS Reply Message Sizes", true, false);

		// Create a dataset
		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		dataset.addSeries(sizeDataSeries);

		// Create axis labels
		NumberAxis domainAxis = new NumberAxis("CCS Non-Empty Reply Message");
		domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());       
		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);
		NumberAxis rangeAxis = new NumberAxis("Size (Bytes)");
		rangeAxis.setAutoRangeIncludesZero(false);

		// Create renderer
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

		// Create the plot, using the renderer and the dataset and the axis
		XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

		// Create a chart using the plot
		JFreeChart chart = new JFreeChart("CCS Reply Message Sizes", plot);

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(chart);

		chart.setAntiAlias(true);
		chart.setBackgroundPaint(Color.white);
		chart.removeLegend();

		// Put the chartpanel in a new window(JFrame)
		JFrame window = new JFrame("Message Sizes");
		window.setLayout(new BorderLayout());
		window.add(chartpanel, BorderLayout.CENTER);


		// Display the window	
		window.pack();
		window.setLocation(20,500);
		window.setVisible(true);

	}

}
