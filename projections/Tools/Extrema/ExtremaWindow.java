package projections.Tools.Extrema;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.awt.event.*;

import javax.swing.*;

import projections.Tools.TimeProfile.ThreadedFileReader;
import projections.Tools.Timeline.TimelineWindow;
import projections.analysis.*;
import projections.gui.Analysis;
import projections.gui.Clickable;
import projections.gui.ColorSelectable;
import projections.gui.GenericGraphWindow;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;
import projections.misc.*;

/**
 *  OutlierAnalysisWindow
 *  by Chee Wai Lee
 *  8/23/2006
 *
 */
public class ExtremaWindow extends GenericGraphWindow
implements ActionListener, ItemListener, ColorSelectable,
Clickable
{

	ExtremaWindow thisWindow;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	static int myRun = 0;

	private JPanel mainPanel;

	// private dialog data
	private int threshold;
	private int k;

	// Record which activity was chosen and is currently loaded
	private int selectedActivity;
	private int selectedAttribute;


	// control panel gui objects and support variables
	// **CW** Not so good for now, used by both Dialog and Window
	public String attributes[][] = {
			{ "Execution Time by Activity",
				"Least Idle Time",
				"Msgs Sent by Activity", 
				"Bytes Sent by Activity",
				"Most Idle Time",
				"Active Entry Methods",
				"Overhead",
			"Average Grain Size"},
			{ "Execution Time (us)",
				"Time (us)",
				"Number of Messages",
				"Number of Bytes",
				"Time (us)",
				" ",
				"Time (us)",
			"Time (us)"},
			{ "us",
				"us",
				"",
				"" ,
				"us",
				"",
				"us",
			"us"}
	};

	// derived data after analysis
	LinkedList outlierList;

	// meta data variables
	// These will be determined at load time.
	private int numActivities;
	private int numSpecials;
	private double[][] graphData;
	private Color[] graphColors;
	private LinkedList<Integer> outlierPEs;


	public ExtremaWindow(MainWindow mainWindow) {
		super("Projections Extrema Analysis Tool - " + 
				MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);

		createMenus();
		createLayout();
		pack();
		thisWindow = this;
		outlierPEs = new LinkedList<Integer>();

		// special behavior if initially used (to load the raw 
		// online-generated outlier information). Quick and dirty, use
		// static variables ... not possible if multiple runs are supported.
		if (MainWindow.runObject[myRun].rcReader.RC_OUTLIER_FILTERED) {
			// get necessary parameters (normally from dialog)

			// This is still a hack, there might be differentiation in the
			// online case.
			int defaultActivity = Analysis.PROJECTIONS;
			// default to execution time. Again a hack.
			int defaultAttribute = 0;

			// Now, read the generated outlier stats, rankings and the top
			// [threshold] log files
			loadOnlineData(0, MainWindow.runObject[myRun].getTotalTime());
		} else {
			showDialog();
		}
	}

	JButton bAddToTimelineJButton;

	private void createLayout() {
		mainPanel = getMainPanel();
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		bAddToTimelineJButton =  new JButton("Add Extrema PEs to Timeline");
		bAddToTimelineJButton.setToolTipText("The Timeline Tool must already be open!");
		bAddToTimelineJButton.addActionListener(new buttonHandler());
		getContentPane().add(bAddToTimelineJButton, BorderLayout.SOUTH);

	}


	private class buttonHandler implements ActionListener {
		public buttonHandler(){

		}

		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == bAddToTimelineJButton){
				// load each outlier PE into the Timeline Window
				Iterator<Integer> iter2 = outlierPEs.iterator();
				while(iter2.hasNext()){
					int pe = iter2.next();
					parentWindow.addProcessor(pe);
				}
			}
		}

	}



	protected void createMenus(){
		JMenuBar mbar = new JMenuBar();
		mbar.add(Util.makeJMenu("File", new Object[]
		                                           {
				"Select Processors",
				null,
				"Close"
		                                           },
		                                           null, this));
		mbar.add(Util.makeJMenu("Tools", new Object[]
		                                            {
				"Change Colors",
		                                            },
		                                            null, this));
		mbar.add(Util.makeJMenu("Help", new Object[]
		                                           {
				"Index",
				"About"
		                                           },
		                                           null, this));
		setJMenuBar(mbar);
	}

	ExtremaDialogExtension outlierDialogPanel;


	public void showDialog() {
		if (dialog == null) {
			outlierDialogPanel = new ExtremaDialogExtension(attributes[0]);
			dialog = new RangeDialog(this, "Select Time Range", outlierDialogPanel, false);
		}
		dialog.displayDialog();
		if (!dialog.isCancelled()){
			threshold = outlierDialogPanel.getThreshold();
			selectedActivity = outlierDialogPanel.getCurrentActivity();
			selectedAttribute = outlierDialogPanel.getCurrentAttribute();
			k = outlierDialogPanel.getK();
			thisWindow.setVisible(false);

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					constructToolData(dialog.getStartTime(), dialog.getEndTime());
					return null;
				}
				public void done() {
					// GUI code after Long non-gui code (above) is done.
					setGraphSpecificData();
					thisWindow.setVisible(true);
				}
			};
			worker.execute();


		}
	}


	void constructToolData(final  long startTime, final long endTime ) {
		// construct the necessary meta-data given the selected activity
		// type.
		double[][] tempData;
		Color[] tempGraphColors;
		numActivities = MainWindow.runObject[myRun].getNumActivity(selectedActivity); 
		tempGraphColors = MainWindow.runObject[myRun].getColorMap(selectedActivity);
		numSpecials = 0;

		if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4 || selectedAttribute == 5 || selectedAttribute == 6 || selectedAttribute == 7) {
			// **CWL NOTE** - this is currently a hack until I can find a way
			// to do this more cleanly!!!
			//
			// add Idle to the current set
			numSpecials = 1;
			if(selectedAttribute == 6 || selectedAttribute == 7){		
				numSpecials = 2;
			}
			graphColors = new Color[numActivities+numSpecials];
			for (int i=0;i<numActivities; i++) {
				graphColors[i] = tempGraphColors[i];
			}
			graphColors[numActivities] = Color.white;

			if( selectedAttribute == 6 ||selectedAttribute == 7){							
				graphColors[numActivities+1] = Color.yellow;
			}
		} else {
			graphColors = tempGraphColors;
		}
		int numActivityPlusSpecial = numActivities+numSpecials;

		OrderedIntList selectedPEs = dialog.getSelectedProcessors().copyOf();
		int numPEs = selectedPEs.size();
		tempData = new double[numPEs][];
		int count = 0;

		// Create a list of worker threads
		LinkedList<Thread> readyReaders = new LinkedList<Thread>();

		int pIdx=0;		
		selectedPEs.reset();
		while (selectedPEs.hasMoreElements()) {
			int nextPe = selectedPEs.nextElement();
			readyReaders.add( new ExtremaReaderThread(nextPe, pIdx, startTime, endTime, 
					numActivities, numActivityPlusSpecial, selectedActivity, selectedAttribute) );
			pIdx++;
		}


		// Determine a component to show the progress bar with
		Component guiRootForProgressBar = null;
		if(thisWindow!=null && thisWindow.isVisible()) {
			guiRootForProgressBar = thisWindow;
		} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
			guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
		}

		// Pass this list of threads to a class that manages/runs the threads nicely
		ThreadManager threadManager = new ThreadManager("Loading Extrema in Parallel", readyReaders, guiRootForProgressBar, true);
		threadManager.runThreads();


		// Retrieve results from each thread, storing them into tempData
		int pIdx2=0;
		Iterator iter = readyReaders.iterator();
		while (iter.hasNext()) {
			ExtremaReaderThread r = (ExtremaReaderThread) iter.next();
			tempData[pIdx2] = r.myData;
			pIdx2++;
		}

		// **CW** Use tempData as source to compute clusters using k
		// from the dialog.
		int clusterMap[] = new int[numPEs];
		double distanceFromClusterMean[] = new double[numPEs];

		KMeansClustering.kMeans(tempData, k, clusterMap, distanceFromClusterMean);

		// Construct average data for each cluster. If cluster is empty,
		// it will be ignored.
		double clusterAverage[][] = new double[k][numActivityPlusSpecial];
		int clusterCounts[] = new int[k];
		int numNonZero = 0;

		for (int p=0; p<numPEs; p++) {
			for (int ep=0; ep<tempData[p].length; ep++) {
				double v = tempData[p][ep];
				clusterAverage[clusterMap[p]][ep] += v;
				clusterCounts[clusterMap[p]]++;
			}
		}


		for (int myk=0; myk<k; myk++) {
			if (clusterCounts[myk] > 0) {
				for (int ep=0; ep<clusterAverage[myk].length; ep++) {
					clusterAverage[myk][ep] /= clusterCounts[myk];
				}
				numNonZero++;
			}
		}


		/*
	System.out.println("Time taken for processing [" + tempData.length +
			   " processors] = " + 
			   (System.currentTimeMillis() - time) +
			   " ms");
		 */

		// Now Analyze the data for outliers.
		// the final graph has 3 extra x-axis slots for 
		// 1) overall average
		// 2) non-outlier average
		// 3) outlier average
		//
		// In addition, there will be numNonZero cluster averages.

		// we know tmpOut has at least x slots. graphData is not ready
		// to be initialized until we know the number of Outliers.
		double[] tmpAvg = new double[numActivities+numSpecials];
		double[] processorDiffs = new double[selectedPEs.size()];
		int[] sortedMap = new int[selectedPEs.size()];
		String[] peNames = new String[selectedPEs.size()];

		// initialize sortedMap (maps indices to indices)
		selectedPEs.reset();
		for (int p=0; p<selectedPEs.size(); p++) {
			sortedMap[p] = p;
			peNames[p] = Integer.toString(selectedPEs.nextElement());
		}

		for (int p=0; p<selectedPEs.size(); p++) {
			if (selectedAttribute == 6) {
				double total_time = 0.0;
				for(int iact = 0; iact<numActivities+1; iact++)
					total_time += tempData[p][iact];			
				tempData[p][numActivities+numSpecials-1] = endTime-startTime - total_time;			
			}else if (selectedAttribute == 7){
				int __count_entries = 0;
				for(int iact = 0; iact<numActivities; iact++)				//tempData[p][numActivities+numSpecials-1] = 0;//processorDiffs[p];
					//System.out.println(" active methods" + processorDiffs[p] + "idle time=" + tempData[p][numActivities]);
				{
					if(tempData[p][iact] > 0)
						__count_entries++;
					tempData[p][numActivities+numSpecials-1] += tempData[p][iact];
				}
				if(__count_entries>0)
					tempData[p][numActivities+numSpecials-1] /= __count_entries;			
			}
		}

		// pass #1, determine global average
		for (int act=0; act<numActivities+numSpecials; act++) {
			for (int p=0; p<selectedPEs.size(); p++) {
				/*
		if (tempData[p][act] > 0) {
		    System.out.println("["+p+"] " + tempData[p][act]);
		}
				 */
				tmpAvg[act] += tempData[p][act];
			}
			tmpAvg[act] /= selectedPEs.size();
		}

		// pass #2, determine outliers by ranking them by distance from
		// average. Weight that by the global average values. It is not 
		// enough
		// to discover outliers by merely sorting them, the mapping has
		// to be preserved for display.

		// Maxmimum Black time, just use -1*sum(tempData[p][0...numActivities])
		// Active Entry methods, use count( tempData[p][0...numActivities-1] > 0 )

		for (int p=0; p<selectedPEs.size(); p++) {
			// this is an initial hack.
			if (selectedAttribute == 1) {
				// induce a sort by decreasing idle time
				processorDiffs[p] -= tempData[p][numActivities];
			} else if (selectedAttribute == 4) {
				// induce a sort by increasing idle time
				processorDiffs[p] += tempData[p][numActivities];
			} else if (selectedAttribute == 5) {
				// active entry method
				for(int iact = 0; iact<numActivities; iact++)
				{
					if(tempData[p][iact] > 0)
						processorDiffs[p]++;				
				}
			}else if (selectedAttribute == 6) {
				//black time totaltime - entrytime-idle time
				processorDiffs[p] = tempData[p][numActivityPlusSpecial-1];
			} else if(selectedAttribute == 7) {
				processorDiffs[p] = tempData[p][numActivityPlusSpecial-1];							
			}else {
				for (int act=0; act<numActivities; act++) {
					processorDiffs[p] += 
						Math.abs(tempData[p][act] - tmpAvg[act]) *
						tmpAvg[act];
				}
			}
		}


		// bubble sort it.
		for (int p=selectedPEs.size()-1; p>0; p--) {
			for (int i=0; i<p; i++) {
				if (processorDiffs[i+1] < processorDiffs[i]) {
					double temp = processorDiffs[i+1];
					processorDiffs[i+1] = processorDiffs[i];
					processorDiffs[i] = temp;
					int tempI = sortedMap[i+1];
					sortedMap[i+1] = sortedMap[i];
					sortedMap[i] = tempI;
				}
			}
		}

		// take the top threshold processors, create the final array
		// and copy the data in.
		int offset = selectedPEs.size()-threshold;
		graphData = new double[threshold+3+numNonZero][numActivities+numSpecials];
		outlierList = new LinkedList();
		for (int ii=0; ii<threshold; ii++) {
			for (int act=0; act<numActivities+numSpecials; act++) {
				graphData[ii+3+numNonZero][act] = tempData[sortedMap[ii+offset]][act];
			}
			// add to outlier list reverse sorted by significance
			int p = sortedMap[ii+offset];		
			String name = peNames[p];
			outlierList.add(name);
			Integer ival = Integer.parseInt(name);
			outlierPEs.add(ival);
		}


		// fill in cluster representative data
		int minDistanceIndex[] = new int[k];
		double minDistanceFromClusterMean[] = new double[k];
		for (int k=0; k<this.k; k++) {
			minDistanceFromClusterMean[k] = Double.MAX_VALUE;
			minDistanceIndex[k] = -1;
		}
		for (int p=0; p<distanceFromClusterMean.length; p++) {
			if (distanceFromClusterMean[p] <= 
				minDistanceFromClusterMean[clusterMap[p]]) {
				minDistanceIndex[clusterMap[p]] = p;
			}
		}
		int clusterIndex = 0;
		for (int k=0; k<this.k; k++) {
			if (minDistanceIndex[k] != -1) {
				for (int act=0; act<numActivities+numSpecials; act++) {
					graphData[3+clusterIndex][act] = 
						tempData[minDistanceIndex[k]][act];
				}
				outlierList.addFirst("C"+k+"R"+minDistanceIndex[k]);
				clusterIndex++;
			}
		}

		graphData[0] = tmpAvg;
		for (int act=0; act<numActivities+numSpecials; act++) {
			for (int i=0; i<offset; i++) {
				graphData[1][act] += tempData[sortedMap[i]][act];
			}
			if (offset != 0) {
				graphData[1][act] /= offset;
			}
			for (int i=offset; i<selectedPEs.size(); i++) {
				graphData[2][act] += tempData[sortedMap[i]][act];
			}
			if (threshold != 0) {
				graphData[2][act] /= threshold;
			}
		}

		// add the 3 special entries
		outlierList.addFirst("Out.");
		outlierList.addFirst("Non.");
		outlierList.addFirst("Avg");

	}

	private void loadOnlineData(final long startTime, final long endTime) {
		final SwingWorker worker = new SwingWorker() {
			public Object doInBackground() {
				readOutlierStats(startTime, endTime);
				return null;
			}
			public void done() {
				setGraphSpecificData();
				thisWindow.setVisible(true);
			}
		};
		worker.execute();
	}

	// This method will read the stats file generated during online
	// outlier analysis which will then determine which processor's
	// log data to read.
	void readOutlierStats(final long startTime, final long endTime) {
		Color[] tempGraphColors;
		numActivities = MainWindow.runObject[myRun].getNumActivity(selectedActivity); 
		tempGraphColors = MainWindow.runObject[myRun].getColorMap(selectedActivity);
		numSpecials = 1;
		graphColors = new Color[numActivities+numSpecials];
		for (int i=0;i<numActivities; i++) {
			graphColors[i] = tempGraphColors[i];
		}
		graphColors[numActivities] = Color.white;

		graphData = new double[threshold+3][numActivities+numSpecials];

		// Read the stats file for global average data.
		String statsFilePath =
			MainWindow.runObject[myRun].getLogDirectory() + File.separator + 
			MainWindow.runObject[myRun].getFilename() + ".outlier";
		try {
			BufferedReader InFile =
				new BufferedReader(new InputStreamReader(new FileInputStream(statsFilePath)));	
			String statsLine;
			statsLine = InFile.readLine();
			StringTokenizer st = new StringTokenizer(statsLine);
			for (int i=0; i<numActivities+numSpecials; i++) {
				graphData[0][i] = Double.parseDouble(st.nextToken());
			}

			// Now read the ranked list of processors and then taking the
			// top [threshold] number.
			statsLine = InFile.readLine();
			st = new StringTokenizer(statsLine);
			int offset = 0;
			OrderedIntList peList = MainWindow.runObject[myRun].getValidProcessorList(ProjMain.LOG);
			if (peList.size() > threshold) {
				offset = peList.size() - threshold;
			}
			int nextPe = 0;
			ProgressMonitor progressBar =
				new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, 
						"Reading log files",
						"", 0,
						threshold);
			progressBar.setNote("Reading");
			progressBar.setProgress(0);
			// clear offset values from the list on file
			for (int i=0; i<offset; i++) {
				st.nextToken();
			}
			outlierList = new LinkedList();
			// add the 3 special entries
			outlierList.add("Avg");    
			outlierList.add("Non.");
			outlierList.add("Out.");
			for (int i=0; i<threshold; i++) {
				nextPe = Integer.parseInt(st.nextToken());
				outlierList.add(nextPe + "");
				progressBar.setProgress(i);
				progressBar.setNote("[PE: " + nextPe +
						" ] Reading Data. (" + i + " of " +
						threshold + ")");
				if (progressBar.isCanceled()) {
					return;
				}
				readOnlineOutlierProcessor(nextPe,i+3, startTime, endTime);
			}
			progressBar.close();
		} catch (IOException e) {
			System.err.println("Error: Projections failed to read " +
					"outlier data file [" + statsFilePath +
			"].");
			System.err.println(e.toString());
			System.exit(-1);
		}	    
		// Calculate the outlier average. Non-outlier average will be
		// derived from the recorded global average and the outlier average.
		for (int act=0; act<numActivities+numSpecials; act++) {
			for (int i=0; i<threshold; i++) {
				graphData[2][act] += graphData[i+3][act];
			}
			// derive total contributed by non-outliers
			graphData[1][act] = 
				graphData[0][act]*MainWindow.runObject[myRun].getNumProcessors() -
				graphData[2][act];
			graphData[1][act] /= MainWindow.runObject[myRun].getNumProcessors() - threshold;
			graphData[2][act] /= threshold;
		}
	}

	private void readOnlineOutlierProcessor(int pe, int index, final long startTime, final long endTime) {
		GenericLogReader reader = 
			new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
		try {
			LogEntryData logData = new LogEntryData();
			logData.time = 0;
			// Jump to the first valid event
			boolean markedBegin = false;
			boolean markedIdle = false;
			long beginBlockTime = 0;
			logData = reader.nextEventOnOrAfter(startTime);
			while (logData.time <= endTime) {
				if (logData.type == ProjDefs.BEGIN_PROCESSING) {
					// check pairing
					if (!markedBegin) {
						markedBegin = true;
					}
					beginBlockTime = logData.time;
				} else if (logData.type == ProjDefs.END_PROCESSING) {
					// check pairing
					// if End without a begin, just ignore
					// this event.
					if (markedBegin) {
						markedBegin = false;
						graphData[index][logData.entry] +=
							logData.time - beginBlockTime;
					}
				} else if (logData.type == ProjDefs.BEGIN_IDLE) {
					// check pairing
					if (!markedIdle) {
						markedIdle = true;
					}
					// NOTE: This code assumes that IDLEs cannot
					// possibly be nested inside of PROCESSING
					// blocks (which should be true).
					beginBlockTime = logData.time;
				} else if (logData.type ==
					ProjDefs.END_IDLE) {
					// check pairing
					if (markedIdle) {
						markedIdle = false;
						graphData[index][numActivities] +=
							logData.time - beginBlockTime;
					}
				}
				logData = reader.nextEvent();
			}
			reader.close();
		} catch (EOFException e) {
			// close the reader and let the external loop continue.
			try {
				reader.close();
			} catch (IOException evt) {
				System.err.println("Outlier Analysis: Error in closing "+
						"file for processor " + pe);
				System.err.println(evt);
			}
		} catch (IOException e) {
			System.err.println("Outlier Analysis: Error in reading log "+
					"data for processor " + pe);
			System.err.println(e);
		}
	}

	protected void setGraphSpecificData() {
		setXAxis("Extrema", outlierList);
		setYAxis(attributes[1][selectedAttribute], 
				attributes[2][selectedAttribute]);
		setDataSource("Extrema: " + attributes[0][selectedAttribute] +
				" (Threshold = " + threshold + 
				" processors)", graphData, graphColors, this);
		refreshGraph();
	}


	public void applyDialogColors() {
		setDataSource("Outliers", graphData, graphColors, this);
		refreshGraph();
	}

	public String[] getPopup(int xVal, int yVal) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(3);
		if ((xVal < 0) || (yVal < 0)) {
			return null;
		}
		String[] rString = new String[3];
		if (xVal == 0) {
			rString[0] = "Global Average";
		} else if (xVal == 1) {
			rString[0] = "Non Outlier Average";
		} else if (xVal == 2) {
			rString[0] = "Outlier Average";
		} else {
			rString[0] = "Outlier Processor " +  (String)outlierList.get(xVal);
		}
		if ((yVal == numActivities)) {
			rString[1] = "Activity: Idle Time";
		} else if (yVal == numActivities+1){
			rString[1] = attributes[0][selectedAttribute];
		}else {
			rString[1] = "Activity: " + 
			MainWindow.runObject[myRun].getActivityNameByIndex(selectedActivity, yVal);
		}
		if (selectedActivity >= 2) {
			rString[2] = df.format(graphData[xVal][yVal]) + "";
		} else {
			rString[2] = U.t((long)(graphData[xVal][yVal]));
		}
		return rString;
	}

	public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
		/** only try to load bars that represent PEs */
		if(xVal > 2)
			parentWindow.addProcessor(Integer.parseInt((String)outlierList.get(xVal)));
	}


	public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Select Processors")) {
				showDialog();
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		// do nothing.
	}



}
