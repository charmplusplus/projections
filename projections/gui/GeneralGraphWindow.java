package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;

/**
 *  GeneralGraphWindow
 *  by Chee Wai Lee
 *
 *  Will replace The old GraphWindow class once a framework for displaying
 *  Legends are in place (and probably replace the name)
 */
public class GeneralGraphWindow extends GenericGraphWindow
    implements ActionListener, ColorSelectable
{
    private GeneralGraphWindow thisWindow;

    private EntrySelectionDialog entryDialog;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JButton epSelection;
    private JButton setRanges;

    // data used for intervalgraphdialog
    int startInterval;
    int endInterval;
    long intervalSize;
    OrderedIntList processorList;

    // data required for entry selection dialog
    private int numEPs;
    private String typeLabelNames[] = {"Entry Points"};
    private boolean stateArray[][];
    private boolean existsArray[][];
    private Color colorArray[][];
    private String entryNames[];

    // stored raw data
    private double[][] graphData;

    // output arrays
    private double[][] outputData;
    private Color[] outColors;

    public GeneralGraphWindow(MainWindow mainWindow, Integer myWindowID) {
	super("General Graph Tools", mainWindow, myWindowID);
	setGraphSpecificData();
	// the following data are statically known and can be initialized
	// here
	numEPs = Analysis.getNumUserEntries();
	stateArray = new boolean[1][numEPs];
	existsArray = new boolean[1][numEPs];
	colorArray = new Color[1][numEPs];
	entryNames = new String[numEPs];
	for (int ep=0; ep<numEPs; ep++) {
	    colorArray[0][ep] = Analysis.getEntryColor(ep);
	    entryNames[ep] = Analysis.getEntryName(ep);
	}
	mainPanel = new JPanel();
    	getContentPane().add(mainPanel);
	createLayout();
	pack();
	thisWindow = this;
	showDialog();
    }

    private void createLayout() {
	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();
	
	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);

	// control panel items
	epSelection = new JButton("Select Entry Points");
	epSelection.addActionListener(this);
	setRanges = new JButton("Select New Range");
	setRanges.addActionListener(this);
	controlPanel = new JPanel();
	controlPanel.setLayout(gbl);
	Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, setRanges,   gbc, 1,0, 1,1, 0,0);
	
	JPanel graphPanel = getMainPanel();
	Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,0, 0,0);
    }

    public void setGraphSpecificData() {
	setXAxis("Time in us","");
	setYAxis("Entry point execution time", "us");
    }

    public void showDialog() {
	if (dialog == null)
	    dialog = new IntervalRangeDialog(this, "Select Range");
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();
	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
			if (dialog.isModified()) {
			    Analysis.LoadGraphData(intervalSize,
						   startInterval,
						   endInterval,
						   true, processorList);
			    fillGraphData();
			}
			return null;
		    }
		    public void finished() {
			setOutputGraphData();
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
    }

    public void showWindow() {
	// nothing for now
    }

    private void fillGraphData() {
	// for now, just assume all processors are added to the mix.
	// LogReader is BROKEN and cannot deal with partial data properly.
	// Any current attempts to fix this will cause GraphWindow to fail
	// when partial data is actually read.
	int numIntervals = endInterval-startInterval+1;
	graphData = new double[numIntervals][numEPs];
	for (int ep=0; ep<numEPs; ep++) {
	    int[][] entryData = Analysis.getUserEntryData(ep, LogReader.TIME);
	    for (int pe=0; pe<entryData.length; pe++) {
		for (int interval=0; interval<numIntervals; interval++) {
		    graphData[interval][ep] += entryData[pe][interval];
		}
	    }
	    // set the exists array to accept non-zero entries only
	    // have initial state also display all existing data.
	    for (int interval=0; interval<numIntervals; interval++) {
		if (graphData[interval][ep] > 0) {
		    existsArray[0][ep] = true;
		    stateArray[0][ep] = true;
		    break;
		}
	    }
	}
    }

    public void applyDialogColors() {
	setOutputGraphData();
    }

    private void setOutputGraphData() {
	// need first pass to decide the size of the outputdata
	int outSize = 0;
	for (int ep=0; ep<numEPs; ep++) {
	    if (stateArray[0][ep]) {
		outSize++;
	    }
	}
	if (outSize == 0) {
	    // do nothing, just display empty graph
	} else {
	    // actually create and fill the data and color array
	    int numIntervals = endInterval-startInterval+1;
	    outputData = 
		new double[numIntervals][outSize];
	    outColors =
		new Color[outSize];
	    for (int i=0; i<numIntervals; i++) {
		int count = 0;
		for (int ep=0; ep<numEPs; ep++) {
		    if (stateArray[0][ep]) {
			outputData[i][count] = graphData[i][ep];
			outColors[count++] = colorArray[0][ep];
		    }
		}
	    }
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Entry point execution time", "us");
	    setDataSource("General Graph", outputData, outColors, thisWindow);
	    super.refreshGraph();
	}
    }

    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}

	// find the ep corresponding to the yVal
	int count = 0;
	String epName = "";
	for (int ep=0; ep<numEPs; ep++) {
	    if (stateArray[0][ep]) {
		if (count++ == yVal) {
		    epName = Analysis.getEntryName(ep);
		    break;
		}
	    }
	}

	String[] rString = new String[2];
	
	rString[0] = "Entry Method: " + epName;
	rString[1] = "Execution Time = " + U.t((long)(outputData[xVal][yVal]));
	return rString;
    }	

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
	    JButton b = (JButton)e.getSource();
	    if (b == epSelection) {
		if (entryDialog == null) {
		    entryDialog = 
			new EntrySelectionDialog(this, this,
						 typeLabelNames,
						 stateArray,colorArray,
						 existsArray,entryNames);
		}
		entryDialog.showDialog();
	    } else if (b == setRanges) {
		showDialog();
	    }
	}
    }
}
