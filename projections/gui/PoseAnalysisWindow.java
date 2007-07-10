package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;

/**
 *  PoseAnalysisWindow.java
 *  by Chee Wai Lee
 *  10/26/2005
 *
 *  Provides a set of views meant to analyze additional POSE information
 *  if the data is found to be available by Projections.
 */
public class PoseAnalysisWindow extends ProjectionsWindow
    implements ActionListener
{
    PoseAnalysisWindow thisWindow;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JButton setRanges;
    private JTabbedPane subToolsPanel;
    PoseRTDopDisplayPanel realTimeDopDisplayPanel;
    PoseVTDopDisplayPanel virtTimeDopDisplayPanel;

    // override default for ProjectionsWindow
    PoseRangeDialog dialog;

    // data used for PoseRangeDialog
    long realStartTime;
    long realEndTime;
    int realStartInterval;
    int realEndInterval;
    long realIntervalSize;

    long virtStartTime;
    long virtEndTime;
    int virtStartInterval;
    int virtEndInterval;
    long virtIntervalSize;

    OrderedIntList processorList;

    // Data from poselog files.
    PoseDopReader reader;
    int[][] dopRealData;
    int[] dopVirtData;
    
    // flag signifying the window has just begun
    boolean	   startFlag;

    void windowInit() {
	// acquire data using parent class
	// super.windowInit(); base class is abstract

	realStartTime = 0;
	realEndTime = MainWindow.runObject[myRun].getPoseTotalTime();
	realIntervalSize = 1000; // default 1ms 
	realStartInterval = 0;
	if (realEndTime%realIntervalSize == 0) {
	    realEndInterval = (int)(realEndTime/realIntervalSize - 1);
	} else {
	    realEndInterval = (int)(realEndTime/realIntervalSize);
	}

	virtStartTime = 0;
	virtEndTime = MainWindow.runObject[myRun].getPoseTotalVirtualTime();
	// Virtual values really vary a lot, so divide the range into
	// approximately 100 intervals by default, if there's enough time.
	virtIntervalSize = virtEndTime/100 + 1;
	virtStartInterval = 0;
	if (virtEndTime%virtIntervalSize == 0) {
	    virtEndInterval = (int)(virtEndTime/virtIntervalSize - 1);
	} else {
	    virtEndInterval = (int)(virtEndTime/virtIntervalSize);
	}
	processorList = MainWindow.runObject[myRun].getValidProcessorList(ProjMain.DOP);
    }

    public PoseAnalysisWindow(MainWindow mainWindow, Integer myWindowID) {
	super("POSE Analysis Tools", mainWindow, myWindowID);
	mainPanel = new JPanel();
    	getContentPane().add(mainPanel);
	createLayout();
	pack();
	thisWindow = this;
	startFlag = true;
	showDialog();
    }

    private void createLayout() {
	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();
	
	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);

	// subtool panel items
	subToolsPanel = new JTabbedPane();
	
	// the empty subtool
        realTimeDopDisplayPanel = new PoseRTDopDisplayPanel();
	virtTimeDopDisplayPanel = new PoseVTDopDisplayPanel();
	subToolsPanel.insertTab("DOP: Real", null, 
				realTimeDopDisplayPanel,
				"Degrees of Parallelism in Real Time", 0);
	subToolsPanel.insertTab("DOP: Virtual", null, 
				virtTimeDopDisplayPanel,
				"Degrees of Parallelism in Virtual Time", 0);

	// control panel items
	setRanges = new JButton("Select New Range");
	setRanges.addActionListener(this);
	controlPanel = new JPanel();
	controlPanel.setLayout(gbl);
	
	Util.gblAdd(controlPanel, setRanges,   gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(mainPanel, subToolsPanel,  gbc, 0,0, 1,1, 1,1, 5,5,5,5);
	Util.gblAdd(mainPanel, controlPanel,   gbc, 0,1, 1,1, 1,0);
    }

    public void showDialog() {
	if (dialog == null) {
	    dialog = new PoseRangeDialog(this, "Select Range");
	} else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();
	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
			if (dialog.isModified()) {
			    reader = MainWindow.runObject[myRun].getPoseDopReader();
			    /*
			    System.out.println(realIntervalSize + " " +
					       realStartTime + " " +
					       realEndTime + " " +
					       virtIntervalSize + " " +
					       virtStartTime + " " +
					       virtEndTime);
			    */
			    // calculate derived data
			    realStartInterval = 
				(int)(realStartTime/realIntervalSize);
			    realEndInterval =
				(int)(realEndTime/realIntervalSize);
			    virtStartInterval =
				(int)(virtStartTime/virtIntervalSize);
			    virtEndInterval =
				(int)(virtEndTime/virtIntervalSize);
			    reader.read(realIntervalSize, 
					realStartInterval, realEndInterval,
					virtIntervalSize, 
					virtStartInterval, virtEndInterval,
					processorList);
			}
			return null;
		    }
		    public void finished() {
			dopRealData = reader.getRealTimeDopData();
			realTimeDopDisplayPanel.setGraphData(dopRealData,
						     realIntervalSize,
						     realStartInterval,
						     realEndInterval);
			dopVirtData = reader.getVirtualTimeDopData();
			virtTimeDopDisplayPanel.setGraphData(dopVirtData,
							     virtIntervalSize,
							     virtStartInterval,
							     virtEndInterval);
			if (startFlag) {
			    thisWindow.setVisible(true);
			}
			if (startFlag)
			    startFlag = false;
			realTimeDopDisplayPanel.refreshGraph();
			virtTimeDopDisplayPanel.refreshGraph();
		    }
		};
	    worker.start();
	}
    }

    public void getDialogData() {
	PoseRangeDialog dialog = this.dialog;
	realStartTime = dialog.getRealStartTime();
	realEndTime = dialog.getRealEndTime();
	realIntervalSize = dialog.getRealIntervalSize();
	virtStartTime = dialog.getVirtStartTime();
	virtEndTime = dialog.getVirtEndTime();
	virtIntervalSize = dialog.getVirtIntervalSize();
	processorList = dialog.getValidProcessors();
    }

    public void setDialogData() {
	PoseRangeDialog dialog = this.dialog;
	dialog.setRealIntervalSize(realIntervalSize);
	dialog.setRealStartTime(realStartTime);
	dialog.setRealEndTime(realEndTime);
	dialog.setVirtIntervalSize(virtIntervalSize);
	dialog.setVirtStartTime(virtStartTime);
	dialog.setVirtEndTime(virtEndTime);
	dialog.setValidProcessors(processorList);
    }

    public void showWindow() {
	// nothing for now
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
	    JButton b = (JButton)e.getSource();
	    if (b == setRanges) {
		showDialog();
	    }
	}
    }
}
