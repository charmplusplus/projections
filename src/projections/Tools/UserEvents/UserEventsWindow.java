package projections.Tools.UserEvents;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.SortedSet;
import java.text.DecimalFormat;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.*;

/**
 *  UserEventsWindow
 *  by Chee Wai Lee
 *
 *  Will replace The old GraphWindow class once a framework for displaying
 *  Legends are in place (and probably replace the name)
 */
public class UserEventsWindow extends GenericGraphWindow
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
    private SortedSet<Integer> processorList;

    // meta data variables
    private int numActivities;
    // Normally derived from MainWindow.runObject[myRun].java
    private String activityNames[];

    // stored raw data
    private double[][] graphData;
    private double[][] timeSpent;
    private double[][] callRate;

    // buttons to switch between displaying "time spent" vs "call rate"
    private JRadioButton timeSpentButton;
    private JRadioButton callRateButton;

    private JRadioButton microseconds;
    private JRadioButton milliseconds;
    private JRadioButton seconds;

    private String yAxisLabel = "Time (us)";
    private String yAxisUnits = "us";

    private double unitTime = 1;
    private String unitTimeStr = "ms";

    private DecimalFormat _format;
    
    public UserEventsWindow(MainWindow mainWindow) {
    	super("Projections User Events Tool - " + 
    			MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
    	// hardcode start. Usually derived from MainWindow.runObject[myRun].java
    	numActivities = MainWindow.runObject[myRun].getNumUserDefinedEvents(); 
    	activityNames = MainWindow.runObject[myRun].getUserEventNames();
    	// Normally would set activity names here.

    	_format = new DecimalFormat("###,###.###");

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
	Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,1, 0,0);

	ButtonGroup display_bg = new ButtonGroup();
	timeSpentButton = new JRadioButton("Time Spent", true);
	callRateButton = new JRadioButton("Call Rate", false);

	timeSpentButton.addActionListener(this);
	callRateButton.addActionListener(this);

	display_bg.add(timeSpentButton);
	display_bg.add(callRateButton);

	JPanel displayTypePanel = new JPanel();
	displayTypePanel.setLayout(gbl);
	displayTypePanel.add(timeSpentButton);
	displayTypePanel.add(callRateButton);

	Util.gblAdd(controlPanel, displayTypePanel, gbc, 0,0, 1,1, 0,0);

	ButtonGroup unit_bg = new ButtonGroup();
    microseconds = new JRadioButton("Microseconds", false);
    milliseconds = new JRadioButton("Milliseconds", true);
    seconds = new JRadioButton("Seconds", false);

    microseconds.addActionListener(this);
    milliseconds.addActionListener(this);
    seconds.addActionListener(this);

    unit_bg.add(microseconds);
    unit_bg.add(milliseconds);
    unit_bg.add(seconds);

    JPanel unitPanel = new JPanel();
    unitPanel.setLayout(gbl);
    unitPanel.add(microseconds);
    unitPanel.add(milliseconds);
    unitPanel.add(seconds);

	Util.gblAdd(controlPanel, unitPanel, gbc, 0,1, 1,1, 0,0);
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
					milliseconds.setSelected(true);
					unitTime = 1;
					unitTimeStr = "ms";
					timeSpentButton.setSelected(true);
					
    				// GUI code after Long non-gui code (above) is done.
    				setGraphSpecificData();
    				thisWindow.setVisible(true);                        
    			}
    		};
    		worker.execute();
    	}
    }

    private void constructToolData() {
	int count = 0;
//	ProgressMonitor progressBar =
//	    new ProgressMonitor(MainWindow.runObject[myRun].guiRoot,
//				"Reading log files",
//				"", 0,
//				processorList.size());
//	progressBar.setNote("Reading");
//	progressBar.setProgress(0);
	timeSpent = new double[processorList.size()][];
	callRate = new double[processorList.size()][];

    LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

	for(Integer pe : processorList) {
        readyReaders.add(new ThreadedFileReader(pe, count, startInterval, endInterval, intervalSize, timeSpent, callRate));
		count++;
	}

    // Determine a component to show the progress bar with
    Component guiRootForProgressBar = null;
    if(thisWindow!=null && thisWindow.isVisible()) {
        guiRootForProgressBar = thisWindow;
    } else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
        guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
    }

    // Pass this list of threads to a class that manages/runs the threads nicely
    TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading User Events Data in Parallel", readyReaders, guiRootForProgressBar, true);
    threadManager.runAll();

	for (int peIdx = 0; peIdx < processorList.size(); peIdx++) {
		for (int eventIdx = 0; eventIdx < numActivities; eventIdx++) {
			callRate[peIdx][eventIdx] /= intervalSize;
		}
	}

	graphData = timeSpent;
    }
    
    
    
    
    

	/** A class that provides the colors for the display */
	public class UserEventColorer implements GenericGraphColorer {
		public Paint[] getColorMap() {
			int numUserEvents = MainWindow.runObject[myRun].getNumUserDefinedEvents(); 
			Paint[]  outColors = ColorManager.createColorMap(numUserEvents);
			return outColors;
		}
	}
    
    
    
    
    protected void setGraphSpecificData() {
	setXAxis("Processors", processorList);
    setYAxisLabelAndUnits();
	setYAxis(yAxisLabel, yAxisUnits);
	setDataSource("User Events", graphData, new UserEventColorer() , this);
	refreshGraph();
    }

    
    
    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}
	String[] rString = new String[3];
	
	rString[0] = "Name: " + activityNames[yVal];
	rString[1] = "Time Spent: " + U.humanReadableString((long)(timeSpent[xVal][yVal]));
	rString[2] = String.format("Rate: %s calls/%s (%s calls)",
		_format.format(callRate[xVal][yVal]),
		unitTimeStr,
		_format.format(callRate[xVal][yVal] * intervalSize/unitTime));
	return rString;
    }	

    public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
	        String arg = ((JMenuItem)e.getSource()).getText();
	        if (arg.equals("Close")) {
	            close();
	        } else if(arg.equals("Set Range")) {
	            showDialog();
	        }
	    } else if (e.getSource() == timeSpentButton) {
	    	graphData = timeSpent;
	    	setGraphSpecificData();
	    } else if (e.getSource() == callRateButton) {
	    	graphData = callRate;
	    	setGraphSpecificData();
	    }
	    else if (e.getSource() == microseconds) {
            scaleHistogramData(0.001);
            setGraphSpecificData();
            refreshGraph();
        }
        else if (e.getSource() == milliseconds) {
            scaleHistogramData(1.0);
            setGraphSpecificData();
            refreshGraph();
        }
        else if (e.getSource() == seconds) {
            scaleHistogramData(1000.0);
            setGraphSpecificData();
            refreshGraph();
        }
    }

    private void scaleHistogramData(double newUnit) {
        double scale = newUnit / unitTime;
        for (int peIdx = 0; peIdx < processorList.size(); peIdx++) {
			for (int eventIdx = 0; eventIdx < numActivities; eventIdx++) {
				callRate[peIdx][eventIdx] *= scale;
			}
		}
        unitTime = newUnit;
        if (unitTime == 0.001) unitTimeStr = "us";
        else if (unitTime == 1.0) unitTimeStr = "ms";
        else unitTimeStr = "s";
    }

    private void setYAxisLabelAndUnits() {
    	if (timeSpentButton.isSelected()) {
	    	yAxisLabel = "Time (us)";
	    	yAxisUnits = "us";
    	} else if (callRateButton.isSelected()) {
	    	yAxisLabel = "Calls/" + unitTimeStr;
	    	yAxisUnits = "";
    	}
    }

}
