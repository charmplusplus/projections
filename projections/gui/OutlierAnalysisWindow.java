package projections.gui;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;
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
    private OutlierAnalysisWindow thisWindow;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JIntTextField thresholdField;
    
    // private control panel gui objects and support variables
    private JComboBox attributeList;
    private JComboBox activityList;
    private String attributes[][] = {
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
    private JButton loadDataButton;

    // derived data after analysis
    LinkedList outlierList;

    // meta data variables
    // These will be determined at load time.
    private int currentAttribute;
    private int currentActivity;
    private int numActivities;
    private double[][] graphData;
    private Color[] graphColors;

    // Threshold values apply only to processors, so that's the unit
    // to be used.
    private int threshold;
    private int lastThreshold;
    
    // History flags. These are set/overriden only when a user successfully
    // loads new data.
    private int lastAttributeIndex;
    private int lastActivityIndex;
    DecimalFormat df = new DecimalFormat();

    public OutlierAnalysisWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections Outlier Analysis Tool - " + 
	      Analysis.getFilename() + ".sts", mainWindow, myWindowID);

	// initialize this to always load execution time at start.
	currentAttribute = 0;
	// initialize this to always load projections-based data at start.
	currentActivity = 0;
	// initialize default threshold to display the top 10% deviants
	// for # processors 256 or less. The top 20 otherwise.
	if (Analysis.getNumProcessors() <= 256) {
	    threshold = (int)Math.ceil(0.1*Analysis.getNumProcessors());
	} else {
	    threshold = 20;
	}
	lastThreshold = 0;
        df.setMaximumFractionDigits(3);

	lastAttributeIndex = -1;
	lastActivityIndex = -1;

	createMenus();
	createLayout();
	pack();
	thisWindow = this;
	showDialog();
    }

    private void createLayout() {
	mainPanel = new JPanel();
    	getContentPane().add(mainPanel);

	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();
	
	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);

	// Assume no special control features for now
	controlPanel = new JPanel();
	controlPanel.setLayout(gbl);

	attributeList = new JComboBox(attributes[0]);
	attributeList.setSelectedIndex(currentAttribute);
	JLabel attributeLabel = new JLabel("Attribute: ", JLabel.RIGHT);
	attributeList.addItemListener(this);

	activityList = new JComboBox(ActivityManager.NAMES);
	activityList.setSelectedIndex(currentActivity);
	JLabel activityLabel = new JLabel("Activity: ", JLabel.RIGHT);
	activityList.addItemListener(this);

	loadDataButton = new JButton("Load Data");
	loadDataButton.addActionListener(this);
       
	JLabel thresholdLabel = new JLabel("Outlier Threshold: ", 
					   JLabel.RIGHT);
	thresholdField = new JIntTextField(threshold, 8);
	JLabel thresholdPost = new JLabel("Processors", JLabel.LEFT);
	thresholdField.addActionListener(this);

	Util.gblAdd(controlPanel, attributeLabel, gbc, 0,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, attributeList,  gbc, 1,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, activityLabel,  gbc, 2,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, activityList,   gbc, 3,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, loadDataButton, gbc, 4,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, thresholdLabel, gbc, 5,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, thresholdField, gbc, 6,0, 1,0, 0,0);
	Util.gblAdd(controlPanel, thresholdPost,  gbc, 7,0, 1,0, 0,0);

	JPanel graphPanel = getMainPanel();
	Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,0, 0,0);
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
	    dialog = new RangeDialog(this, "Select Range");
	} else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();
	    // The Dialog time range has changed, so make sure new data
	    // is always loaded.
	    lastAttributeIndex = -1;
	    lastActivityIndex = -1;
	    if (dialog.isModified()) {
		loadData();
	    }
	}
    }

    private void loadData() {
	if (!isModified()) {
	    return;
	}
	final SwingWorker worker =  new SwingWorker() {
		public Object construct() {
		    constructToolData();
		    return null;
		}
		public void finished() {
		    // GUI code after Long non-gui code (above) is done.
		    setGraphSpecificData();
		    // capture the last loaded threshold value
		    lastThreshold = threshold;
		    thisWindow.setVisible(true);
		}
	    };
	worker.start();
    }
    
    private void constructToolData() {
	// construct the necessary meta-data given the selected activity
	// type.
	numActivities = Analysis.getNumActivity(currentActivity);
	graphColors = Analysis.getColorMap(currentActivity);
	double[][] tempData;
	if (currentAttribute == 1) {
	    // **CWL NOTE** - this is currently a hack until I can find a way
	    // to do this more cleanly!!!
	    numActivities = 1;
	    graphColors = new Color[1];
	    graphColors[0] = Color.white;
	}
	tempData = 
	    new double[validPEs.size()][numActivities];
	int nextPe = 0;
	int count = 0;
	ProgressMonitor progressBar =
	    new ProgressMonitor(Analysis.guiRoot, 
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
		new GenericLogReader(nextPe, Analysis.getVersion());
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
			    Analysis.getUserDefinedEventIndex(logData.userEventID);
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
		    // Jump to the first valid event
		    boolean markedBegin = false;
		    boolean markedIdle = false;
		    long beginBlockTime = 0;
		    reader.nextEventOnOrAfter(startTime, logData);
		    while (logData.time <= endTime) {
			if (logData.type == ProjDefs.CREATION) {
			    if (markedBegin) {
				int eventIndex = logData.entry;
				if (currentAttribute == 2) {
				    tempData[count][eventIndex]++;
				} else if (currentAttribute == 3) {
				    tempData[count][eventIndex] +=
					logData.msglen;
				}
			    }
			} else if (logData.type == 
				   ProjDefs.BEGIN_PROCESSING) {
			    // check pairing
			    if (!markedBegin) {
				markedBegin = true;
			    }
			    if (currentAttribute == 0) {
				// even if a previous begin is found, just
				// overwrite the begin time, we're
				// not expecting nesting here.
				beginBlockTime = logData.time;
			    }
			} else if (logData.type ==
				   ProjDefs.END_PROCESSING) {
			    // check pairing
			    // if End without a begin, just ignore
			    // this event.
			    if (markedBegin) {
				markedBegin = false;
				if (currentAttribute == 0) {
				    tempData[count][logData.entry] +=
					logData.time - beginBlockTime;
				}
			    }
			} else if (logData.type ==
				   ProjDefs.BEGIN_IDLE) {
			    // check pairing
			    if (!markedIdle) {
				markedIdle = true;
			    }
			    // NOTE: This code assumes that IDLEs cannot
			    // possibly be nested inside of PROCESSING
			    // blocks (which should be true).
			    if (currentAttribute == 1) {
				beginBlockTime = logData.time;
			    }
			} else if (logData.type ==
				   ProjDefs.END_IDLE) {
			    // check pairing
			    if (markedIdle) {
				markedIdle = false;
				if (currentAttribute == 1) {
				    tempData[count][0] +=
					logData.time - beginBlockTime;
				}
			    }
			}
			reader.nextEvent(logData);
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
	/*
	for (int i=0; i<tempData.length; i++) {
	    for (int j=0; j<tempData[i].length; j++) {
		System.out.print(tempData[i][j] + " ");
	    }
	    System.out.println();
	}
	*/
	// Now Analyze the data for outliers.
	// the final graph has 3 extra x-axis slots for 
	// 1) overall average
	// 2) non-outlier average
	// 3) outlier average
       
	// we know tmpOut has at least x slots. graphData is not ready
	// to be initialized until we know the number of Outliers.
	double[] tmpAvg = new double[numActivities];
	double[] processorDiffs = new double[validPEs.size()];
	int[] sortedMap = new int[validPEs.size()];

	// initialize sortedMap
	for (int p=0; p<validPEs.size(); p++) {
	    sortedMap[p] = p;
	}

	// pass #1, determine global average
	for (int act=0; act<numActivities; act++) {
	    for (int p=0; p<validPEs.size(); p++) {
		tmpAvg[act] += tempData[p][act];
	    }
	    tmpAvg[act] /= validPEs.size();
	}

	// pass #2, determine outliers by ranking them by distance from
	// average. Weight that by the original values. It is not enough
	// to discover outliers by merely sorting them, the mapping has
	// to be preserved for display.
	for (int p=0; p<validPEs.size(); p++) {
	    for (int act=0; act<numActivities; act++) {
		processorDiffs[p] += 
		    Math.abs(tempData[p][act] - tmpAvg[act]) *
		    tempData[p][act];
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
	// take the top threshold percentage, create the final array
	// and copy the data in.
	int offset = validPEs.size()-threshold;
	graphData = new double[threshold+3][numActivities];
	outlierList = new LinkedList();
	for (int i=0; i<threshold; i++) {
	    for (int act=0; act<numActivities; act++) {
		graphData[i+3][act] =
		    tempData[sortedMap[i+offset]][act];
	    }
	    // add to outlier list reverse sorted by significance
	    outlierList.add(Integer.toString(sortedMap[i+offset]));
	}
	graphData[0] = tmpAvg;
	for (int act=0; act<numActivities; act++) {
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
	if (currentActivity == 1) {
	    rString[1] = "Activity: Idle Time";
	} else {
	    rString[1] = "Activity: " + 
		Analysis.getActivityNameByIndex(currentActivity, yVal);
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
	if (e.getSource() instanceof JButton) {
	    JButton b = (JButton)e.getSource();
	    if (b == loadDataButton) {
		loadData();
	    }
        } else if (e.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem)e.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if(arg.equals("Select Processors")) {
                showDialog();
            }
        } else if (e.getSource() instanceof JIntTextField) {
	    JIntTextField field = (JIntTextField)e.getSource();
	    if (field.isValueValid()) {
		threshold = field.getValue();
	    }
	}
    }

    public void itemStateChanged(ItemEvent evt) {
        if (evt.getSource() instanceof JComboBox) {
	    if ((activityList.getSelectedIndex() >= 0) &&
		(attributeList.getSelectedIndex() >= 0)) {
		// filter out the options that may not work with each other
		if ( // **FIXME** Find a way to get around having to hardcode
		    // pose dop activities have no meaning here
		    (activityList.getSelectedIndex() == 
		     ActivityManager.POSE_DOP) ||
		    // no function support for now either ... *sigh*
		    (activityList.getSelectedIndex() ==
		     ActivityManager.FUNCTIONS) ||
		    // no communication properties associated with user events
		    ((activityList.getSelectedIndex() == 
		      ActivityManager.USER_EVENTS) &&
		     (attributeList.getSelectedIndex() >= 1))
		    ) {
		    loadDataButton.setEnabled(false);
		} else {
		    loadDataButton.setEnabled(true);
		}
	    } else {
		loadDataButton.setEnabled(false);
	    }
	    currentActivity = activityList.getSelectedIndex();
	    currentAttribute = attributeList.getSelectedIndex();
        }
    }

    /**
     *  Checks if the currently selected set of attributes and activities
     *  is different from the last loaded set.
     */
    private boolean isModified() {
	return ((activityList.getSelectedIndex() != lastActivityIndex) ||
		(attributeList.getSelectedIndex() != lastAttributeIndex) ||
		threshold != lastThreshold);
    }
}
