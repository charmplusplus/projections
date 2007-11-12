package projections.gui;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;
import projections.gui.Timeline.TimelineWindow;
import projections.guiUtils.*;
import projections.misc.*;

/**
 *  OutlierAnalysisWindow
 *  by Chee Wai Lee
 *  8/23/2006
 *
 */
public class OutlierAnalysisWindow extends GenericGraphWindow
    implements ActionListener, ItemListener, ColorSelectable,
	       Clickable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	OutlierAnalysisWindow thisWindow;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private JPanel mainPanel;

    // private dialog data
    private int threshold;
    private int currentActivity;
    private int currentAttribute;
    private int k;

    // control panel gui objects and support variables
    // **CW** Not so good for now, used by both Dialog and Window
    public String attributes[][] = {
	{ "Execution Time by Activity",
	  "Idle Time",
	  "Msgs Sent by Activity", 
	  "Bytes Sent by Activity" },
	{ "Execution Time (us)",
	  "Time (us)",
	  "Number of Messages",
	  "Number of Bytes" },
	{ "us",
	  "us",
	  "",
	  "" }
    };

    // derived data after analysis
    LinkedList outlierList;

    // meta data variables
    // These will be determined at load time.
    private int numActivities;
    private int numSpecials;
    private double[][] graphData;
    private Color[] graphColors;
    public OrderedIntList outlierPEs;

    DecimalFormat df = new DecimalFormat();

    public OutlierAnalysisWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections Outlier Analysis Tool - " + 
	      MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow, myWindowID);

	createMenus();
	createLayout();
	pack();
	thisWindow = this;
	// special behavior if initially used (to load the raw 
	// online-generated outlier information). Quick and dirty, use
	// static variables ... not possible if multiple runs are supported.
	if (MainWindow.runObject[myRun].rcReader.RC_OUTLIER_FILTERED.booleanValue()) {
	    // get necessary parameters (normally from dialog)

	    // This is still a hack, there might be differentiation in the
	    // online case.
	    currentActivity = ActivityManager.PROJECTIONS;
	    // default to execution time. Again a hack.
	    currentAttribute = 0;
	    // finally, something that's not a hack
	    validPEs = MainWindow.runObject[myRun].getValidProcessorList(ProjMain.LOG);
	    outlierPEs = new OrderedIntList();
	    // these might change later when online time ranges are
	    // permitted.
	    startTime = 0;
	    endTime = MainWindow.runObject[myRun].getTotalTime();
	    if (MainWindow.runObject[myRun].getNumProcessors() <= 256) {
		threshold = (int)Math.ceil(0.1*MainWindow.runObject[myRun].getNumProcessors());
	    } else {
		threshold = 20;
	    }

	    // Now, read the generated outlier stats, rankings and the top
	    // [threshold] log files
	    loadOnlineData();
	} else {
	    showDialog();
	}
    }

    private void createLayout() {
	mainPanel = getMainPanel();
    	getContentPane().add(mainPanel);
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
    
    public void showDialog() {
	if (dialog == null) {
	    dialog = new OutlierDialog(this, "Select Range");
	} else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();
	    if (dialog.isModified()) {
		thisWindow.setVisible(false);
		loadData();
	    }
	}
    }

    public void getDialogData() {
	OutlierDialog mydialog = (OutlierDialog)dialog;

	threshold = mydialog.threshold;
	k = mydialog.k;
	currentActivity = mydialog.currentActivity;
	currentAttribute = mydialog.currentAttribute;
	super.getDialogData();
    }

    private void loadData() {
	final SwingWorker worker =  new SwingWorker() {
		public Object construct() {
		    constructToolData();
		    return null;
		}
		public void finished() {
		    // GUI code after Long non-gui code (above) is done.
		    setGraphSpecificData();
		    thisWindow.setVisible(true);
		}
	    };
	worker.start();
    }

    void constructToolData() {
	// construct the necessary meta-data given the selected activity
	// type.
	double[][] tempData;
	Color[] tempGraphColors;
	numActivities = MainWindow.runObject[myRun].getNumActivity(currentActivity); 
	tempGraphColors = MainWindow.runObject[myRun].getColorMap(currentActivity);
	numSpecials = 0;
	if (currentAttribute <= 1) {
	    // **CWL NOTE** - this is currently a hack until I can find a way
	    // to do this more cleanly!!!
	    //
	    // add Idle to the current set
	    numSpecials = 1;
	    graphColors = new Color[numActivities+numSpecials];
	    for (int i=0;i<numActivities; i++) {
		graphColors[i] = tempGraphColors[i];
	    }
	    graphColors[numActivities] = Color.white;
	} else {
	    graphColors = tempGraphColors;
	}
	tempData = 
	    new double[validPEs.size()][numActivities+numSpecials];
	int nextPe = 0;
	int count = 0;
	ProgressMonitor progressBar =
	    new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, 
				"Reading log files",
				"", 0,
				validPEs.size());
	progressBar.setNote("Reading");
	progressBar.setProgress(0);
	// this reset is required because this code is called multiple times!
	validPEs.reset();
	while (validPEs.hasMoreElements()) {
	    nextPe = validPEs.nextElement();
	    progressBar.setProgress(count);
	    progressBar.setNote("[PE: " + nextPe +
				" ] Reading Data.");
	    if (progressBar.isCanceled()) {
		return;
	    }
	    // Construct tempData (read) array here
	    //
	    // **NOTE** We really need a generic interface to a "data"
	    // object. Re-writing the reading code each time for each
	    // tool is starting to get really painful.
	    //
	    // Right now, we restrict ourselves to reading logs (since
	    // we wanna support User Events, the nature of which 
	    // unfortunately requires us to write a different read loop
	    // for it.
	    GenericLogReader reader = 
		new GenericLogReader(nextPe, MainWindow.runObject[myRun].getVersion());
	    try {
		if (currentActivity == ActivityManager.USER_EVENTS) {
		    LogEntryData logData = new LogEntryData();
		    LogEntryData logDataEnd = new LogEntryData();
		    logData.time = 0;
		    while (logData.time < startTime) {
			reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR,
					       logData);
			reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR,
					       logDataEnd);
		    }
		    int eventIndex = 0;
		    while (true) {
			// process pair read previously
			eventIndex = 
			    MainWindow.runObject[myRun].getUserDefinedEventIndex(logData.userEventID);
			tempData[count][eventIndex] +=
			    logDataEnd.time - logData.time;
			reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR,
					       logData);
			reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR,
					       logDataEnd);
			if (logDataEnd.time > endTime) {
			    break;
			}
		    }
		} else {
		    LogEntryData logData = new LogEntryData();
		    logData.time = 0;
		    // dealing with book-keeping events
		    boolean isFirstEvent = true;
		    // Jump to the first valid event
		    boolean markedBegin = false;
		    boolean markedIdle = false;
		    long beginBlockTime = startTime;
		    reader.nextEventOnOrAfter(startTime, logData);
		    while (logData.time <= endTime) {
			LogEntryData BE = reader.getLastBE();
			switch (logData.type) {
			case ProjDefs.CREATION:
			    if (isFirstEvent) {
				if ((BE != null) && 
				    (BE.type == ProjDefs.BEGIN_PROCESSING)) {
				    beginBlockTime = startTime;
				    markedBegin = true;
				}
			    }
			    if (markedBegin) {
				int eventIndex = logData.entry;
				if (currentAttribute == 2) {
				    tempData[count][eventIndex]++;
				} else if (currentAttribute == 3) {
				    tempData[count][eventIndex] +=
					logData.msglen;
				}
			    }
			    break;
			case ProjDefs.BEGIN_PROCESSING:
			    isFirstEvent = false;
			    // check pairing
			    if (!markedBegin) {
				markedBegin = true;
			    }
			    if (currentAttribute <= 1) {
				// even if a previous begin is found, just
				// overwrite the begin time, we're
				// not expecting nesting here.
				beginBlockTime = logData.time;
			    }
			    break;
			case ProjDefs.END_PROCESSING:
			    if (isFirstEvent) {
				markedBegin = true;
				beginBlockTime = startTime;
			    }
			    isFirstEvent = false;
			    if (markedBegin) {
				markedBegin = false;
				if (currentAttribute <= 1) {
				    tempData[count][logData.entry] +=
					logData.time - beginBlockTime;
				}
			    }
			    break;
			case ProjDefs.BEGIN_IDLE:
			    isFirstEvent = false;
			    // check pairing
			    if (!markedIdle) {
				markedIdle = true;
			    }
			    // NOTE: This code assumes that IDLEs cannot
			    // possibly be nested inside of PROCESSING
			    // blocks (which should be true).
			    if (currentAttribute <= 1) {
				beginBlockTime = logData.time;
			    }
			    break;
			case ProjDefs.END_IDLE:
			    if (isFirstEvent) {
				markedIdle = true;
				beginBlockTime = startTime;
			    }
			    // check pairing
			    if (markedIdle) {
				markedIdle = false;
				if (currentAttribute <= 1) {
				    tempData[count][numActivities] +=
					logData.time - beginBlockTime;
				}
			    }
			    break;
			}
			reader.nextEvent(logData);
		    }
		    LogEntryData beginEvent = reader.getLastBE();
		    // Now handle the tail case.
		    switch (logData.type) {
		    case ProjDefs.END_PROCESSING:
			// lastBE is empty by design in this case, so
			// use beginBlockTime recorded from previously.
			if (currentAttribute <= 1) {
			    tempData[count][logData.entry] +=
				endTime - beginBlockTime;
			}
			break;
		    case ProjDefs.END_IDLE:
			// lastBE is empty by design in this case, so
			// use beginBlockTime recorded from previously.
			if (currentAttribute <= 1) {
			    tempData[count][numActivities] +=
				endTime - beginBlockTime;
			}
			break;
		    default:
			// all other cases. Ignore if no beginEvent is
			// found. Otherwise, use it's begin time if
			// it is greater than startTime, otherwise, use
			// startTime.
			if (beginEvent != null) {
			    if (beginEvent.time > startTime) {
				beginBlockTime = beginEvent.time;
			    } else {
				beginBlockTime = startTime;
			    }
			    switch (beginEvent.type) {
			    case ProjDefs.BEGIN_PROCESSING:
				if (currentAttribute <= 1) {
				    tempData[count][beginEvent.entry] +=
					endTime - beginBlockTime;
				}
				break;
			    case ProjDefs.BEGIN_IDLE:
				if (currentAttribute == 1) {
				    tempData[count][numActivities] +=
					endTime - beginBlockTime;
				}
				break;
			    }
			}
		    }
		    reader.close();
		}
	    } catch (EOFException e) {
		// close the reader and let the external loop continue.
		try {
		    reader.close();
		} catch (IOException evt) {
		    System.err.println("Outlier Analysis: Error in closing "+
				       "file for processor " + nextPe);
		    System.err.println(evt);
		}
	    } catch (IOException e) {
		System.err.println("Outlier Analysis: Error in reading log "+
				   "data for processor " + nextPe);
		System.err.println(e);
	    }
	    count++;
	}
	progressBar.close();

	// **CW** Use tempData as source to compute clusters using k
	// from the dialog.

	int clusterMap[] = new int[tempData.length];
	double distanceFromClusterMean[] = new double[tempData.length];
	// long time = System.currentTimeMillis();
	KMeansClustering.kMeans(tempData, k, clusterMap, 
				distanceFromClusterMean);
	// Construct average data for each cluster. If cluster is empty,
	// it will be ignored.
	double clusterAverage[][] = new double[k][tempData[0].length];
	int clusterCounts[] = new int[k];
	int numNonZero = 0;
	for (int p=0; p<tempData.length; p++) {
	    for (int ep=0; ep<tempData[p].length; ep++) {
		clusterAverage[clusterMap[p]][ep] += tempData[p][ep];
		clusterCounts[clusterMap[p]]++;
	    }
	}
	for (int k=0; k<this.k; k++) {
	    if (clusterCounts[k] > 0) {
		for (int ep=0; ep<clusterAverage[k].length; ep++) {
		    clusterAverage[k][ep] /= clusterCounts[k];
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
	double[] processorDiffs = new double[validPEs.size()];
	int[] sortedMap = new int[validPEs.size()];
	String[] peNames = new String[validPEs.size()];

	// initialize sortedMap (maps indices to indices)
	validPEs.reset();
	for (int p=0; p<validPEs.size(); p++) {
	    sortedMap[p] = p;
	    peNames[p] = Integer.toString(validPEs.nextElement());
	}

	// pass #1, determine global average
	for (int act=0; act<numActivities+numSpecials; act++) {
	    for (int p=0; p<validPEs.size(); p++) {
		/*
		if (tempData[p][act] > 0) {
		    System.out.println("["+p+"] " + tempData[p][act]);
		}
		*/
		tmpAvg[act] += tempData[p][act];
	    }
	    tmpAvg[act] /= validPEs.size();
	}

	// pass #2, determine outliers by ranking them by distance from
	// average. Weight that by the global average values. It is not 
	// enough
	// to discover outliers by merely sorting them, the mapping has
	// to be preserved for display.
	for (int p=0; p<validPEs.size(); p++) {
	    // this is an initial hack.
	    if (currentAttribute == 1) {
		// induce a sort by decreasing idle time
		processorDiffs[p] -= tempData[p][numActivities];
	    } else {
		for (int act=0; act<numActivities; act++) {
		    processorDiffs[p] += 
			Math.abs(tempData[p][act] - tmpAvg[act]) *
			tmpAvg[act];
		}
	    }
	}
	// bubble sort it.
	for (int p=validPEs.size()-1; p>0; p--) {
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
	int offset = validPEs.size()-threshold;
	graphData = 
	    new double[threshold+3+numNonZero][numActivities+numSpecials];
	outlierList = new LinkedList();
	for (int i=0; i<threshold; i++) {
	    for (int act=0; act<numActivities+numSpecials; act++) {
		graphData[i+3+numNonZero][act] =
		    tempData[sortedMap[i+offset]][act];
	    }
	    // add to outlier list reverse sorted by significance
	    outlierList.add(peNames[sortedMap[i+offset]]);
	    System.out.println(peNames[sortedMap[i+offset]]);
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
		System.out.println(minDistanceIndex[k]);
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
	    for (int i=offset; i<validPEs.size(); i++) {
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
    
    private void loadOnlineData() {
	final SwingWorker worker = new SwingWorker() {
		public Object construct() {
		    readOutlierStats();
		    return null;
		}
		public void finished() {
		    setGraphSpecificData();
		    thisWindow.setVisible(true);
		}
	    };
	worker.start();
    }
    
    // This method will read the stats file generated during online
    // outlier analysis which will then determine which processor's
    // log data to read.
    void readOutlierStats() {
	Color[] tempGraphColors;
	numActivities = MainWindow.runObject[myRun].getNumActivity(currentActivity); 
	tempGraphColors = MainWindow.runObject[myRun].getColorMap(currentActivity);
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
	    if (validPEs.size() > threshold) {
		offset = validPEs.size() - threshold;
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
		readOnlineOutlierProcessor(nextPe,i+3);
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

    private void readOnlineOutlierProcessor(int pe, int index) {
	GenericLogReader reader = 
	    new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
	try {
	    LogEntryData logData = new LogEntryData();
	    logData.time = 0;
	    // Jump to the first valid event
	    boolean markedBegin = false;
	    boolean markedIdle = false;
	    long beginBlockTime = 0;
	    reader.nextEventOnOrAfter(startTime, logData);
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
		reader.nextEvent(logData);
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
	setXAxis("Outliers", outlierList);
	setYAxis(attributes[1][currentAttribute], 
		 attributes[2][currentAttribute]);
	setDataSource("Outliers: " + attributes[0][currentAttribute] +
		      " (Threshold = " + threshold + 
		      " processors)", graphData, graphColors, this);
	refreshGraph();
    }
    
    public void showWindow() {
	// nothing for now
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
	    rString[0] = "Outlier Processor " + 
		(String)outlierList.get(xVal);
	}
	if ((yVal == numActivities)) {
	    rString[1] = "Activity: Idle Time";
	} else {
	    rString[1] = "Activity: " + 
		MainWindow.runObject[myRun].getActivityNameByIndex(currentActivity, yVal);
	}
	if (currentActivity >= 2) {
	    rString[2] = df.format(graphData[xVal][yVal]) + "";
	} else {
	    rString[2] = U.t((long)(graphData[xVal][yVal]));
	}
	return rString;
    }	

    public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
	// no response if the bars for average values are clicked
	if (xVal >= 3) {
	    if (parentWindow.childWindows[MainWindow.TIMELINE_WIN][0] !=
		null) {
		final int myX = xVal;
		// potentially expensive, so apply SwingWorker to this
		final SwingWorker worker =  new SwingWorker() {
			public Object construct() {
			    ((TimelineWindow)parentWindow.childWindows[MainWindow.TIMELINE_WIN][0]).addProcessor(Integer.parseInt((String)outlierList.get(myX)));
			    return null;
			}
			public void finished() {
			    // GUI code after Long non-gui code.
			    // Which in this case, is nothing.
			}
		    };
		worker.start();
	    } else {
		System.err.println("You wanted to load processor " +
				   (String)outlierList.get(xVal) +
				   "'s data onto Timeline. However," +
				   "the ability to open a new " +
				   "timeline window from Outlier Analysis " +
				   "is not supported yet!");
	    }
	}
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
