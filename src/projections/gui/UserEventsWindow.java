package projections.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.misc.LogEntryData;

/**
 *  UserEventsWindow
 *  by Chee Wai Lee
 *
 *  Will replace The old GraphWindow class once a framework for displaying
 *  Legends are in place (and probably replace the name)
 */
class UserEventsWindow extends GenericGraphWindow
    implements ActionListener
{

	private UserEventsWindow thisWindow;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    private static int myRun = 0;

    private JPanel mainPanel;
    private JPanel controlPanel;

    private IntervalChooserPanel intervalPanel;

	// data used for intervalgraphdialog
    private int startInterval;
    private int endInterval;
    private long intervalSize;
    private OrderedIntList processorList;

    // meta data variables
    private int numActivities;
    // Normally derived from MainWindow.runObject[myRun].java
    private String activityNames[];

    // stored raw data
    private double[][] graphData;
    private long[][] numCalls;
    private Color[] graphColors;
    
    protected UserEventsWindow(MainWindow mainWindow) {
    	super("Projections User Events Tool - " + 
    			MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
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

//    protected void createMenus(){
//    	super.createMenus();
//    }

    public void showDialog() {
    	if (dialog == null) {
    		intervalPanel = new IntervalChooserPanel();    	
    		dialog = new RangeDialog(this, "Select Range", intervalPanel, false);    
    	}

    	dialog.displayDialog();
    	if (!dialog.isCancelled()){
    		intervalSize = intervalPanel.getIntervalSize();
    		startInterval = (int)intervalPanel.getStartInterval();
    		endInterval = (int)intervalPanel.getEndInterval();
    		processorList = dialog.getSelectedProcessors();
    		final SwingWorker worker =  new SwingWorker() {
    			public Object doInBackground() {
    				if (true) {
    					// Long non-gui (except progress) code here.
    					constructToolData();
    				}
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

    private void constructToolData() {
	int pe = 0;
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
		pe = processorList.nextElement();
		progressBar.setProgress(count);
		progressBar.setNote("[PE: " + pe +
		" ] Reading Data.");
		if (progressBar.isCanceled()) {
			return;
		}
		// process data here.
		graphData[count] = new double[numActivities];
		numCalls[count] = new long[numActivities];

		// READ - nothing here
		GenericLogReader reader = new GenericLogReader(MainWindow.runObject[myRun].getLog(pe), pe,
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
		}catch (EndOfLogSuccess e) {
			// do nothing
		} catch (IOException e) {
			System.out.println("Exception while reading log file " + pe); 
		}


		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
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

    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}
	String[] rString = new String[3];
	
	rString[0] = "Name: " + activityNames[yVal];
	rString[1] = "Time Spent: " + U.humanReadableString((long)(graphData[xVal][yVal]));
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
