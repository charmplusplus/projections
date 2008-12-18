package projections.gui.Timeline;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;
import projections.gui.ColorManager;
import projections.gui.ColorSelectable;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalRangeDialog;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.SwingWorker;
import projections.gui.U;
import projections.gui.Util;
import projections.misc.*;

/**
 *  UserEventsWindow
 *  by Chee Wai Lee
 *
 *  Will replace The old GraphWindow class once a framework for displaying
 *  Legends are in place (and probably replace the name)
 */
public class UserEventsWindow extends GenericGraphWindow
    implements ActionListener, ColorSelectable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	UserEventsWindow thisWindow;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private JPanel mainPanel;
    private JPanel controlPanel;

    // data used for intervalgraphdialog
    int startInterval;
    int endInterval;
    long intervalSize;
    OrderedIntList processorList;

    // meta data variables
    private int numActivities;
    // Normally derived from MainWindow.runObject[myRun].java
    private String activityNames[];

    // stored raw data
    private double[][] graphData;
    private long[][] numCalls;
    private Color[] graphColors;
    
    public UserEventsWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections User Events Tool - " + 
	      MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow, myWindowID);
	// hardcode start. Usually derived from MainWindow.runObject[myRun].java
	numActivities = MainWindow.runObject[myRun].getNumUserDefinedEvents(); 
	activityNames = MainWindow.runObject[myRun].getUserEventNames();
	// Normally would set activity names here.
	// Normally would get color maps from MainWindow.runObject[myRun].java.
	graphColors = ColorManager.createColorMap(numActivities);

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
	    dialog = new IntervalRangeDialog(this, "Select Range");
	} else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();
	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
			if (dialog.isModified()) {
			    // Long non-gui (except progress) code here.
			    constructToolData();
			}
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
    }

    public void getDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	intervalSize = dialog.getIntervalSize();
	startInterval = (int)dialog.getStartInterval();
	endInterval = (int)dialog.getEndInterval();
	processorList = dialog.getValidProcessors();
	super.getDialogData();
    }

    public void setDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	dialog.setIntervalSize(intervalSize);
	dialog.setValidProcessors(processorList);
	super.setDialogData();
    }

    void constructToolData() {
	int nextPe = 0;
	int count = 0;
	ProgressMonitor progressBar =
	    new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, 
				"Reading log files",
				"", 0,
				processorList.size());
	progressBar.setNote("Reading");
	progressBar.setProgress(0);
	graphData = new double[processorList.size()][];
	numCalls = new long[processorList.size()][];
	while (processorList.hasMoreElements()) {
	    nextPe = processorList.nextElement();
	    progressBar.setProgress(count);
	    progressBar.setNote("[PE: " + nextPe +
				" ] Reading Data.");
	    if (progressBar.isCanceled()) {
		return;
	    }
	    // process data here.
	    graphData[count] = new double[numActivities];
	    numCalls[count] = new long[numActivities];

	    // READ - nothing here
	    GenericLogReader reader = new GenericLogReader(nextPe,
							   MainWindow.runObject[myRun].getVersion());
	    LogEntryData logData;
	    LogEntryData logDataEnd;
	    
	    // Skip to the first begin.
	    try {
			logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
		    logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);

		while (logData.time < startInterval*intervalSize) {
			logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
		    logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
		}
		int eventIndex = 0;
		while (true) {
		    // process pair read previously
		    eventIndex = MainWindow.runObject[myRun].getUserDefinedEventIndex(logData.userEventID);
		    graphData[count][eventIndex] +=
			logDataEnd.time - logData.time;
		    numCalls[count][eventIndex]++;
		    logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
		    logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
		    if (logDataEnd.time > endInterval*intervalSize) {
			break;
		    }
		}
		reader.close();
	    } catch (EOFException e) {
		// file reading done, close the file and
		// just go on to the next processor loop
		try {
		    reader.close();
		} catch (IOException evt) {
		    System.err.println("ERROR: UserEvents Window failed " +
				       "to " + 
				       "read log file for processor " + 
				       nextPe);
		    System.err.println(e);
		    System.exit(-1);
		}
	    } catch (IOException e) {
		System.err.println("ERROR: UserEvents Window failed to " + 
				   "read log file for processor " + nextPe);
		System.err.println(e);
		System.exit(-1);
	    }
	    count++;
	}
	progressBar.close();
    }
    
    protected void setGraphSpecificData() {
	setXAxis("Processors", processorList);
	setYAxis("Time (us)", "us");
	setDataSource("User Events", graphData, graphColors, this);
	refreshGraph();
    }
    
    public void showWindow() {
	// nothing for now
    }

    public void applyDialogColors() {
	setDataSource("User Events", graphData, graphColors, this);
	refreshGraph();
    }

    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}
	String[] rString = new String[3];
	
	rString[0] = "Name: " + activityNames[yVal];
	rString[1] = "Time Spent: " + U.t((long)(graphData[xVal][yVal]));
	rString[2] = "Count: " + numCalls[xVal][yVal];
	return rString;
    }	

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
//	    JButton b = (JButton)e.getSource();
        } else if (e.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem)e.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if(arg.equals("Select Processors")) {
                showDialog();
            }
        }
    }
}
