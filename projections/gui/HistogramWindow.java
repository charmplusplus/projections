package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *  HistogramWindow
 *  modified by Chee Wai Lee
 *  2/23/2005
 *  - Defaults to visualizing time-based histograms. User can switch
 *    to msg-size based histograms.
 */
public class HistogramWindow extends GenericGraphWindow 
    implements ActionListener
{
    private static final int NUM_TYPES = 2;
    private static final int TYPE_TIME = 0;
    private static final int TYPE_MSG_SIZE = 1;

    // Gui components
    JButton entrySelectionButton;
    JButton epTableButton;

    JRadioButton timeBinButton;
    JRadioButton msgSizeBinButton;
    ButtonGroup binTypeGroup;

    // Data maintained by HistogramWindow
    // countData is indexed by type, then by bin index followed by ep id.
    // NOTE: bin indices need not be of the same size
    private double[][][] counts;
    private int binType;
    
    // variables (in addition to those in the super class) 
    // to be set by BinDialog.
    public int timeNumBins;
    public long timeBinSize;
    public long timeMinBinSize;
    public int msgNumBins;
    public long msgBinSize;
    public long msgMinBinSize;

    private HistogramWindow thisWindow;

    private boolean newDialog; // a temporary hack
    private DecimalFormat _format;

    void windowInit() {
	timeNumBins = 100;  // default to 100 bins
	timeBinSize = 1000; // 1ms default bin size
	timeMinBinSize = 0; // default, look at all bins
	msgNumBins = 200;   // default to 200 bins
	msgBinSize = 100;  // 100 bytes default bin size
	msgMinBinSize = 0; // default, look at all bins
	// use GenericGraphWindow's method for the rest.
	super.windowInit();
    }
    
    public HistogramWindow(MainWindow mainWindow, Integer myWindowID)
    {
	super("Projections Histograms", mainWindow, myWindowID);
	thisWindow = this;

	binType = TYPE_TIME;
	_format = new DecimalFormat();

	setTitle("Projections Histograms");

	createMenus();
	getContentPane().add(getMainPanel());

	pack();
	showDialog(); 
    }   
    
    public void close(){
	super.close();
    }

    /* 
     *  Show the BinDialog 
     */
    public void showDialog()
    {
	if (dialog == null) {
	    dialog = 
		new BinDialog(this, "Select Histogram Time Range");
	    newDialog = true;
	} else {
	    setDialogData();
	    newDialog = false;
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
	    getDialogData();
	    final SwingWorker worker = new SwingWorker() {
		    public Object construct() {
			if (dialog.isModified()) {
			    counts = thisWindow.getCounts();
			} else if (newDialog) { // temp hack
			    counts = thisWindow.getCounts();
			}
			return null;
		    }
		    public void finished() {
			setGraphSpecificData();
			refreshGraph();
			thisWindow.setVisible(true);
		    }
		};
	    worker.start();
	}
    }

    public void showWindow() {
	// do nothing for now
    }

    public void getDialogData() {
	BinDialog dialog = (BinDialog)this.dialog;
	timeNumBins = dialog.getTimeNumBins();
	timeBinSize = dialog.getTimeBinSize();
	timeMinBinSize = dialog.getTimeMinBinSize();
	msgNumBins = dialog.getMsgNumBins();
	msgBinSize = dialog.getMsgBinSize();
	msgMinBinSize = dialog.getMsgMinBinSize();
	// use GenericGraphWindow's method for the rest.
	super.getDialogData();
    }

    public void setDialogData() {
	BinDialog dialog = (BinDialog)this.dialog;
	dialog.setTimeBinSize(timeBinSize);
	dialog.setTimeNumBins(timeNumBins);
	dialog.setTimeMinBinSize(timeMinBinSize);
	dialog.setMsgBinSize(msgBinSize);
	dialog.setMsgNumBins(msgNumBins);
	dialog.setMsgMinBinSize(msgMinBinSize);
	super.setDialogData();
    }

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof JMenuItem) {
	    JMenuItem m = (JMenuItem)evt.getSource();
	    if(m.getText().equals("Set Range"))
		showDialog();
	    else if(m.getText().equals("Close"))
		close();
	} else if (evt.getSource() instanceof JRadioButton) {
	    JRadioButton b = (JRadioButton)evt.getSource();
	    if (b.getActionCommand().equals("Execution Time")) {
		// small optimization
		if (binType != TYPE_TIME) {
		    final SwingWorker worker = new SwingWorker() {
			    public Object construct() {
				binType = TYPE_TIME;
				setGraphSpecificData();
				refreshGraph();
				return null;
			    }
			    public void finished() {
			    }
			};
		    worker.start();
		}
	    } else if (b.getActionCommand().equals("Message Size")) {
		if (binType != TYPE_MSG_SIZE) {
		    final SwingWorker worker = new SwingWorker() {
			    public Object construct() {
				binType = TYPE_MSG_SIZE;
				setGraphSpecificData();
				refreshGraph();
				return null;
			    }
			    public void finished() {
			    }
			};
		    worker.start();
		}
	    }
	} else if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton)evt.getSource();
	    if (b.getText().equals("Select Entries")) {
		System.out.println("selecting entries for display");
	    } else if (b.getText().equals("Out-of-Range EPs")) {
		System.out.println("Showing out of range entries");
	    }
	}
    } 

    protected JPanel getMainPanel()
    {
	JPanel mainPanel = new JPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gbl = new GridBagLayout();

        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.setLayout(gbl);

	JPanel graphPanel = super.getMainPanel(); 

	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
					       "Histogram Controls"));

	entrySelectionButton = new JButton("Select Entries");
	entrySelectionButton.addActionListener(this);
	epTableButton = new JButton("Out-of-Range EPs");
	epTableButton.addActionListener(this);

	timeBinButton = new JRadioButton("Execution Time", true);
	timeBinButton.addActionListener(this);
	msgSizeBinButton = new JRadioButton("Message Size");
	msgSizeBinButton.addActionListener(this);

	binTypeGroup = new ButtonGroup();
	binTypeGroup.add(timeBinButton);
	binTypeGroup.add(msgSizeBinButton);

	buttonPanel.add(timeBinButton);
	buttonPanel.add(msgSizeBinButton);
	buttonPanel.add(entrySelectionButton);
	buttonPanel.add(epTableButton);

        Util.gblAdd(mainPanel, graphPanel,  gbc, 0,0, 1,1, 1,1);
        Util.gblAdd(mainPanel, buttonPanel, gbc, 0,1, 1,1, 0,0);

	return mainPanel;
    }  

    protected void setGraphSpecificData(){
	if (binType == TYPE_TIME) {
	    setXAxis("Bin Interval Size (" + U.t(timeBinSize) + ")", 
		     "Time", timeMinBinSize, timeBinSize);
	    setYAxis("Number of Occurrences", "");
	    setDataSource("Histogram", counts[TYPE_TIME], 
			  thisWindow);
	} else if (binType == TYPE_MSG_SIZE) {
	    setXAxis("Bin Interval Size (" + 
		     _format.format(msgBinSize) + " bytes)", 
		     "", msgMinBinSize, msgBinSize);
	    setYAxis("Number of Occurrences", "");
	    setDataSource("Histogram", counts[TYPE_MSG_SIZE], 
			  thisWindow);
	}
    }

    protected void refreshGraph()
    {
	super.refreshGraph();
    }

    public String[] getPopup(int xVal, int yVal) {
	if (binType == TYPE_TIME) {
	    return getTimePopup(xVal, yVal);
	} else if (binType == TYPE_MSG_SIZE) {
	    return getMsgSizePopup(xVal, yVal);
	}
	return null;
    }

    private String[] getTimePopup(int xVal, int yVal) {
	String bubbleText[] = new String[3];

	bubbleText[0] = Analysis.getEntryName(yVal);
	bubbleText[1] = "Count: " + counts[TYPE_TIME][xVal][yVal];
	if (xVal < timeNumBins) {
	    bubbleText[2] = "Bin: " + U.t(xVal*timeBinSize+timeMinBinSize) +
		" to " + U.t((xVal+1)*timeBinSize+timeMinBinSize);
	} else {
	    bubbleText[2] = "Bin: > " + U.t(timeNumBins*timeBinSize+
					    timeMinBinSize);
	}
	return bubbleText;
    }

    private String[] getMsgSizePopup(int xVal, int yVal) {
	String bubbleText[] = new String[3];

	bubbleText[0] = Analysis.getEntryName(yVal);
	bubbleText[1] = "Count: " + counts[TYPE_MSG_SIZE][xVal][yVal];
	if (xVal < msgNumBins) {
	    bubbleText[2] = "Bin: " + 
		_format.format(xVal*msgBinSize+msgMinBinSize) +
		" bytes to " + _format.format((xVal+1)*msgBinSize+
					      msgMinBinSize) +
		" bytes";
	} else {
	    bubbleText[2] = "Bin: > " + 
		_format.format(msgNumBins*msgBinSize+msgMinBinSize)+" bytes";
	}
	return bubbleText;
    }

    private double[][][] getCounts()
    {
	// Variables for use with the analysis
	long executionTime;
	long adjustedTime;
	long adjustedSize;

	int numEPs = Analysis.getNumUserEntries();

	OrderedIntList tmpPEs = validPEs.copyOf();
	GenericLogReader r;
        double[][][] countData = new double[NUM_TYPES][][];

	// we create an extra bin to hold overflows.
	countData[TYPE_TIME] = new double[timeNumBins+1][numEPs];
	countData[TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];

	LogEntryData logdata;
	logdata = new LogEntryData();

	LogEntryData tmpLogPtr; // temp pointer for swapping only

	// for maintaining "begin" entries for the data type we
	// wish to monitor.
	LogEntryData[] typeLogs
	    = new LogEntryData[NUM_TYPES];
	for (int i=0; i<NUM_TYPES; i++) {
	    typeLogs[i] = new LogEntryData();
	}
	// for maintaining interval-based events 
	boolean[] isActive
	    = new boolean[NUM_TYPES];

	ProgressMonitor progressBar = 
	    new ProgressMonitor(this, "Reading log files",
				"", 0, tmpPEs.size());
	int curPeCount = 0;
	while (tmpPEs.hasMoreElements()) {
	    int pe = tmpPEs.nextElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("Reading data for PE " + pe);
		progressBar.setProgress(curPeCount);
	    } else {
		progressBar.close();
	    }
	    curPeCount++;
	    r = new GenericLogReader(Analysis.getLogName(pe),
				     Analysis.getVersion());
	    try {
		r.nextEventOnOrAfter(startTime, logdata);
		boolean done = false;
		while (!done) {
		    switch (logdata.type) {
		    case ProjDefs.BEGIN_PROCESSING:
			// NOTE: If prior BEGIN never got terminated,
			// simply drop the data given current tracing
			// scheme (ie. do nothing)
			if (logdata.time > endTime) {
			    done = true;
			} else {
			    // swap logdata (ie. note the BEGIN event)
			    tmpLogPtr = logdata;
			    logdata = typeLogs[TYPE_TIME];
			    typeLogs[TYPE_TIME] = tmpLogPtr;
			    isActive[TYPE_TIME] = true;
			}
			break;
		    case ProjDefs.END_PROCESSING:
			if (!isActive[TYPE_TIME]) {
			    // NOTE: No corresponding BEGIN, so this
			    // instance of END must be ignored given
			    // current tracing scheme.
			    if (logdata.time > endTime) {
				done = true;
			    }
			    break;
			} else {
			    if (logdata.event != typeLogs[TYPE_TIME].event) {
				// The events are mismatched! Clear all.
				// Possible under current tracing scheme.
				isActive[TYPE_TIME] = false;
				break;
			    }
			    // NOTE: Even if the END event happens past 
			    // the range, it is recorded as the proper 
			    // execution time of the event.
			    executionTime = 
				logdata.time - typeLogs[TYPE_TIME].time;
			    adjustedTime = executionTime - timeMinBinSize;
			    // respect user threshold
			    if (adjustedTime >= 0) {
				int targetBin = 
				    (int)(adjustedTime/timeBinSize);
				if (targetBin >= timeNumBins) {
				    targetBin = timeNumBins;
				}
				countData[TYPE_TIME][targetBin][logdata.entry]
				    += 1.0;
			    }
			    isActive[TYPE_TIME] = false;
			}
			break;
		    case ProjDefs.CREATION:
			if (logdata.time > endTime) {
			    break;
			}
			// respect the user threshold.
			adjustedSize =
			    logdata.msglen - msgMinBinSize;
			if (adjustedSize >= 0) {
			    int targetBin = (int)(adjustedSize/msgBinSize);
			    if (targetBin >= msgNumBins) {
				targetBin = msgNumBins;
			    }
			    countData[TYPE_MSG_SIZE][targetBin]
				[logdata.entry]+=1.0;
			}
			break;
		    }
		    if (!done) {
			r.nextEvent(logdata);
		    }
		}
	    } catch(EOFException e) {
	     	// do nothing just reached end-of-file
	    } catch(Exception e) {
		System.out.println("Exception " + e);
		e.printStackTrace();
		System.exit(-1);
	    }
	}
	progressBar.close();
	return countData;
    }
    
    // override the super class' createMenus(), add any menu items in 
    // fileMenu if needed, add any new menus to the menuBar
    // then call super class' createMenus() to add the menuBar to the Window
    protected void createMenus()
    {
	fileMenu = Util.makeJMenu(fileMenu,
				  new Object[]
	    {
		"Select Entry Points"
	    },
				  null,
				  this);
	menuBar.add(Util.makeJMenu("View", 
				   new Object[]
	    {
		new JCheckBoxMenuItem("Show Longest EPs",true)
	    },
				   null,
				   this));
	super.createMenus();
    }
}
