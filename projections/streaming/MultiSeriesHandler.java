package projections.streaming;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.analysis.StsReader;
import projections.ccs.CcsProgress;
import projections.ccs.CcsThread;
import projections.misc.LogLoadException;

public class MultiSeriesHandler {

	private CcsThread ccs;
	private String ccsHandler;

	private CategoryPlot plot;
	private DefaultCategoryDataset dataset;

	private boolean saveRepliesToFile = false;
	private boolean loadRepliesFromFile = true;
	
	private FileOutputStream savedRepliesFileStream = null;
	private ObjectOutputStream savedRepliesObjectOutStream;
	private FileInputStream savedRepliesFileInputStream = null;
	private ObjectInputStream savedRepliesObjectInputStream;
	private String replySavedFilename = "savedCCSReplies";

	private StsReader sts;

	/** A dataset that holds the sizes of the received messages */
	DefaultTableXYDataset sizeDataset;
	XYSeries sizeDataSeries;
	XYPlot sizePlot;
	int numSizesSoFar = 0;

	private String chartTitle;

	private	int nextXValue=0;


	/** Constructor */	
	MultiSeriesHandler(String hostname, int port, String ccsHandler, String filename, boolean saveRepliesToFile, boolean loadRepliesFromFile){
	
		System.out.println("StreamingDataHandler constructor");
	
		this.saveRepliesToFile = saveRepliesToFile;
		this.loadRepliesFromFile = loadRepliesFromFile;
	
		try {
			sts = new StsReader(filename);
		} catch (LogLoadException e) {
			System.out.println("Couldn't load sts file with name " + filename);
		}
	
		this.ccsHandler = ccsHandler;
	
		if(ccsHandler.equals("CkPerfSumDetail uchar")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed")) {
			chartTitle = "Utilization Stacked by EP All Processors";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed PE0")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		}
	
		createStackedUtilizationChart();
		createMessageSizeChart();
	
	
		if(loadRepliesFromFile){
			ReadDataFromFileDriver d = new ReadDataFromFileDriver();
			d.driver();
		} else {
	
	
			progressHandler h = new progressHandler();
			ccs = new CcsThread(h,hostname,port);
	
			/** Create first request */
			ccs.addRequest(new dataRequest());
	
		}
	
	}



	/** A driver routine that pulls previously stored CCS replies from a file and feeds them to the plotting routines */
	public class ReadDataFromFileDriver {

		public void driver(){
			if(loadRepliesFromFile){
				// open file
				try {
					savedRepliesFileInputStream = new FileInputStream(replySavedFilename);
					savedRepliesObjectInputStream = new ObjectInputStream(savedRepliesFileInputStream);
				} catch (FileNotFoundException e) {
					System.err.println("Error when trying to open input file to read CCS Replies: " + e.getMessage() );
					e.printStackTrace();
				} catch (IOException e) {
					System.err.println("Error when trying to open input file to read CCS Replies: " + e.getMessage() );
					e.printStackTrace();
				}

				boolean done = false;	
				while(!done){
					try {
						Object data = savedRepliesObjectInputStream.readObject();
						System.out.println("read an object from file: " + data);

						if(data instanceof byte[] ){
							processIncomingData((byte[]) data);
						} else {
							System.err.println("Read incorrect type of object from file");								
						}
					} catch (Exception e) {
						done = true;
					}
				}

			}

		}

	}


	/** An unused handler for CCS progress messages. */
	public class progressHandler implements CcsProgress {
		public progressHandler(){ }
		public void setText(String s) {	}
	}


	/** The class that handles CCS requests and responses. */
	private class dataRequest extends CcsThread.request{

		public dataRequest() {
			super(ccsHandler,0);
		}

		/** handler incoming CCS responses, possibly saving the resuls for replaing later */
		public void handleReply(byte[] data){

			if(saveRepliesToFile){
				// create a new file
				if(savedRepliesFileStream == null) {
					try {
						savedRepliesFileStream = new FileOutputStream("savedCCSReplies");
						savedRepliesObjectOutStream = new ObjectOutputStream(savedRepliesFileStream);
					} catch (FileNotFoundException e) {
						System.err.println("Error when trying to open output file to write CCS Replies into: " + e.getMessage() );
						e.printStackTrace();
					} catch (IOException e) {
						System.err.println("Error when trying to open output file to write CCS Replies into: " + e.getMessage() );
						e.printStackTrace();
					}
				}

				try {
					savedRepliesObjectOutStream.writeObject(data);
					savedRepliesObjectOutStream.flush();
				} catch (IOException e) {
					System.err.println("Error when trying to write a CCS Replies into the file: " + e.getMessage() );
					e.printStackTrace();
				}

			}

			processIncomingData(data);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Request more data
			ccs.addRequest(this, true);
		}
	}






	/** Generate the plot that will now be displayed */
	public void updatePlot(){

	}


	/** Create a window with the stacked utilization plot in it. Uses the jfreechart package. */
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


	/** Create a window and plot for the message sizes. */
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



	/** Deserialize the incoming CCS reply message and plot it */
	void processIncomingData(byte[] data) {
		System.out.println("processIncomingData(byte[] data) data.length=" + data.length);
		
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
								String epName = null;
								if(sts != null){
									epName = sts.getEntryNameByID(e);
									int p = epName.indexOf('('); // first occurrence of a '(' in the string
									epName = epName.substring(0, p);
								}

								//								System.out.println("name for " + e + " is " + epName);
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
		}
	}

}
