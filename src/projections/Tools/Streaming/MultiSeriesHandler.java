package projections.Tools.Streaming;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

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

class MultiSeriesHandler {

	private CcsThread ccs;
	private String ccsHandler;
	
	
	private boolean saveRepliesToFile = false;
	private boolean loadRepliesFromFile = false;

	private FileOutputStream savedRepliesFileStream = null;
	private ObjectOutputStream savedRepliesObjectOutStream;
	private FileInputStream savedRepliesFileInputStream = null;
	private ObjectInputStream savedRepliesObjectInputStream;
	private String replySavedFilename = "savedCCSReplies";

	private StsReader sts;

	/** A chart holds the sizes of the received messages */
private //	DefaultTableXYDataset sizeDataset;
	XYSeries sizeDataSeries;
//	XYPlot sizePlot;
	private int numSizesSoFar = 0;


	/** A chart holding the scrolling stacked utilization  */
	private JFreeChart scrollingChart;
	private CategoryPlot scrollingPlot;


	/** A chart holding the detailed utilization */
	private JFreeChart detailedChart;
	private CategoryPlot detailedPlot;


	/** Store the portion of the dataset that is to be plotted */
	private TreeMap<Integer, TreeMap<String, Double> > streamingData;
	private ArrayList<String> categories;
	Vector<byte[]> detailedData;

	private int updateCount = 0;


	private String chartTitle;

	private	int nextXValue=0;


	/** Constructor */	
	MultiSeriesHandler(String hostname, int port, String ccsHandler, String filename, boolean saveRepliesToFile, boolean loadRepliesFromFile){

		streamingData = new TreeMap<Integer, TreeMap<String, Double> >();
		categories = new ArrayList<String>();
		detailedData = new Vector<byte[]>();


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
			chartTitle = "Utilization Stacked by EP";
		} else if (ccsHandler.equals("CkPerfSumDetail compressed PE0")) {
			chartTitle = "Utilization Stacked by EP on Processor 0";
		}


		createStackedUtilizationChart();
		createDetailedUtilizationChart();
		createMessageSizeChart();

		if(loadRepliesFromFile){
			System.out.println("Using previously saved data instead of CCS connection\n");
			ReadDataFromFileDriver d = new ReadDataFromFileDriver();
			d.run();
		} else {
			System.out.println("Using CCS to connect\n");
			progressHandler h = new progressHandler();
			ccs = new CcsThread(h,hostname,port);

			/** Create first request */
			ccs.addRequest(new dataRequest());

		}

	}



	/** A driver routine that pulls previously stored CCS replies from a file and feeds them to the plotting routines */
	private class ReadDataFromFileDriver implements Runnable {

		private void driver(){
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
							//							Thread.sleep(800);
						} else {
							System.err.println("Read incorrect type of object from file");								
						}
					} catch (Exception e) {
						done = true;
					}
				}

			}

		}

		public void run() {
			driver();
		}

	}


	/** An unused handler for CCS progress messages. */
	private class progressHandler implements CcsProgress {
		public progressHandler(){ }
		public void setText(String s) {	}
	}


	/** The class that handles CCS requests and responses. */
	private class dataRequest extends CcsThread.request{

		public dataRequest() {
			super(ccsHandler,0);
		}

		/** handler incoming CCS responses, possibly saving the results for replaying later */
		public void handleReply(byte[] data){

			System.out.println("handleReply(data.length="+data.length+")");

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

			} else {
				//				System.out.println("before processIncomingData()");
				processIncomingData(data);
				//				System.out.println("after processIncomingData()");
			}



			updatePlots();

			try {
				Thread.sleep(900);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Request more data
			//			System.out.println("Adding CCS request: " + ccs);
			ccs.addRequest(this, true);
		}
	}



	/** Create a window with the stacked utilization plot in it. Uses the jfreechart package. */
	private void createDetailedUtilizationChart(){

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		detailedChart = ChartFactory.createStackedAreaChart(
				chartTitle,
				"",                // domain axis label
				"Percent Utilization",                   // range axis label
				dataset,                   // data
				PlotOrientation.VERTICAL,  // orientation
				true,                      // include legend
				true,
				false
		);

		detailedChart.setBackgroundPaint(Color.white);

		detailedPlot = (CategoryPlot) detailedChart.getPlot();

		//		CategoryItemRenderer renderer = plot.getRenderer();

		CategoryAxis domainAxis = detailedPlot.getDomainAxis();
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		domainAxis.setCategoryMargin(0.0);
		domainAxis.setAxisLineVisible(false);
		domainAxis.setTickLabelsVisible(false);

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(detailedChart);

		// Put the chartpanel in a new window(JFrame)
		JFrame window = new JFrame("Detailed Utilization Plot");
		window.setLayout(new BorderLayout());
		window.add(chartpanel, BorderLayout.CENTER);

		// Display the window	
		window.pack();
		window.setLocation(720,30);
		window.setVisible(true);
	}



	/** Create a window with the stacked utilization plot in it. Uses the jfreechart package. */
	private void createStackedUtilizationChart(){

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		scrollingChart = ChartFactory.createStackedAreaChart(
				chartTitle,
				"",                // domain axis label
				"Percent Utilization",                   // range axis label
				dataset,                   // data
				PlotOrientation.VERTICAL,  // orientation
				true,                      // include legend
				true,
				false
		);

		scrollingChart.setBackgroundPaint(Color.white);

		scrollingPlot = (CategoryPlot) scrollingChart.getPlot();

		//		CategoryItemRenderer renderer = plot.getRenderer();

		CategoryAxis domainAxis = scrollingPlot.getDomainAxis();
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		domainAxis.setCategoryMargin(0.0);
		domainAxis.setAxisLineVisible(false);
		domainAxis.setTickLabelsVisible(false);

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(scrollingChart);

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
	private void createMessageSizeChart(){

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


	/** add a data point to the plot. updatePlot needs to be called after this */
	private void addDataPointToPlot(Double utilization, String epName, Integer nextXValue){
		if(!categories.contains(epName)){
			categories.add(epName);
		}

		if(streamingData.containsKey(nextXValue)){
			TreeMap<String, Double> record = streamingData.get(nextXValue);
			record.put(epName, utilization);
		} else {
			TreeMap<String, Double> record = new TreeMap();
			record.put(epName, utilization);
			streamingData.put(nextXValue, record);
		}

		// Remove old entries if we have too much data
		if(streamingData.size() > 100){
			Integer firstKey = streamingData.firstKey();
			streamingData.remove(firstKey);
		}

	}







	/** Update all the plots */		
	private void updatePlots(){
		updateStreamingPlot();
		updateCount ++;
		if(updateCount % 20 == 0){
			updateDetailedPlot();
		}
	}


	private void addKnownCategories(Integer xValue, DefaultCategoryDataset dataset){
		dataset.addValue(0.0, "Other", xValue);
		Iterator<String> categoryIter = categories.iterator();
		while(categoryIter.hasNext()){
			String c = categoryIter.next();
			if(! c.equals("Other")){
				dataset.addValue(0.0, c, xValue);
			}
		}
	}


	private String getName(int ep, int numEPs){
		String epName;
		if(ep >= 0 && ep < numEPs){
			if(sts == null){
				return "" + ep;
			} else {
				epName = sts.getEntryNameByID(ep);
				int p = epName.indexOf('('); // first occurrence of a '(' in the string
				if(p>0){
					epName = epName.substring(0, p);
				}
			}	
		} else {
			epName = "Other";
		}
		return epName;
	}


	void updateDetailedPlot(){

		DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

		if(detailedData.size() < 1)
			return;

		byte[] data = detailedData.lastElement();		

		if(data.length<=1)
			return;

		System.out.println("Creating detailed plot for data in " + data.length + " byte array");

		int pos = 0;

		// start parsing this array
		int numBins = ByteParser.bytesToInt(data, pos);
		pos += 4;
		System.out.println("detailed plot Number of bins in message = " + numBins);

		if(numBins < 1)
			return;

		int numProcs = ByteParser.bytesToInt(data, pos);
		pos += 4;
		System.out.println("detailed plot Number of processors contributing data in message = " + numProcs);

//		int numBinsPerPlotSample = 100;
		for(int b=0; b<numBins && b<250; b++){
			if(pos+2>data.length)
				return;

			if(b==0)
				addKnownCategories(new Integer(b), newDataset);


			// Read the number of entries for this bin
			int numEntries = ByteParser.bytesToShort(data, pos);
			pos += 2;

			for(int e=0;e<numEntries;e++){

				int ep = ByteParser.bytesToShort(data, pos);
				pos += 2;

				float u = ByteParser.bytesToUnsignedChar(data, pos);
				pos += 1;


				u = u / 2.5f; // The range of values supplied for u is 0 to 250.

				if(u > 0.0){
					// add datapoint to plot
					String epName = getName(ep,1000);
					newDataset.addValue(u, epName, new Integer(b));
				}

			}
		}
		detailedPlot.setDataset(newDataset);
	}


	/** replace the streaming chart with a new updated version */
	private void updateStreamingPlot(){

		DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

		//		TreeMap<Integer, TreeMap<String, Double> > plotData;

		Iterator<Integer> stepIter = streamingData.keySet().iterator();
		Integer xValue = new Integer(0);

		boolean firstTime = true;

		while(stepIter.hasNext()){
			xValue = stepIter.next();

			// Add all the known categories so that the colors do not change from previous times
			if(firstTime){
				addKnownCategories(xValue, newDataset);
			}

			TreeMap<String, Double> map = streamingData.get(xValue);
			Iterator<String> entryIter = map.keySet().iterator();
			while(entryIter.hasNext()){
				String entryName = entryIter.next();
				Double utilization = map.get(entryName);
				newDataset.addValue(utilization, entryName, xValue);
			}
		}

		// Add all entry name categories to the chart with 0 values;

		//		System.out.println("categories sizes=" + categories.size());

		scrollingPlot.setDataset(newDataset);
	}


	/** Deserialize the incoming CCS reply message and plot it */
	private void processIncomingData(byte[] data) {
		//		System.out.println("processIncomingData(byte[] data) data.length=" + data.length);
		detailedData.add(data);

		if(data.length>1){

			if(ccsHandler.equals("CkPerfSumDetail compressed")) {

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

					int numBinsPerPlotSample = 100;
					int binsRemaining = numBins;
					while(binsRemaining>0){
						//						System.out.println("binsRemaining = " + binsRemaining);

						double sums[] = new double[1024];
						double otherUtilization = 0.0;
						for(int e=0;e<numEPs; e++){
							sums[e] = 0.0;
						}
						int samplesInThisBin = 0;
						for(int b=0; b<numBinsPerPlotSample; b++){

							if(pos+2 <= data.length){

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
						for(int e=0; e<numEPs; e++){
							double utilization = (double)sums[e]/(double)samplesInThisBin;
							if(utilization > 0.0){
								String epName = null;
								if(sts != null){
									epName = sts.getEntryNameByID(e);
									int p = epName.indexOf('('); // first occurrence of a '(' in the string
									if(p>0){
										epName = epName.substring(0, p);
									}
								}

								//								System.out.println("name for " + e + " is " + epName);
								if(epName != null)
									addDataPointToPlot(utilization, epName, nextXValue);
								else
									addDataPointToPlot(utilization, ""+e, nextXValue);


								added = true;
							}
						}

						// Add an other category as well
						double utilization = otherUtilization/(double)samplesInThisBin;
						if(utilization > 0.0){
							addDataPointToPlot(utilization, "Other", nextXValue);
							//							System.out.println("Adding other with utilization " + utilization + " and samplesInThisBin=" + samplesInThisBin);
							added = true;
						}

						// advance to next x axis point
						if(added == true){
							nextXValue ++;

							updatePlots();
							try {
								Thread.sleep(20);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						}



					}

//					double avg = totalSum/numBins;
					//					System.out.println("Average Utilization=" + avg);

				}


			}
		}
	}

}
