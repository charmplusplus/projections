package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.io.*;
import javax.swing.*;

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
    private static final int TYPE_TIME = 0;
    private static final int TYPE_MSG_SIZE = 1;

    // Gui components
    JButton entrySelectionButton;
    JButton epTableButton;

    JRadioButton timeBinButton;
    JRadioButton msgSizeBinButton;
    ButtonGroup binTypeGroup;

    // Data maintained by HistogramWindow
    // countData is indexed by bin index followed by ep id.
    private double[][] counts;
    private int binType;
    
    // variables (in addition to those in the super class) 
    // to be set by BinDialog.
    public int numBins;
    public long binSize;
    public long minBinSize;

    private HistogramWindow thisWindow;

    private boolean newDialog; // a temporary hack
    private DecimalFormat _format;

    void windowInit() {
	numBins = 100;  // default to 100 bins
	binSize = 100; // 1ms default bin size
	minBinSize = 0; // default, look at all bins
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
	setGraphSpecificData();

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
			setDataSource("Histogram", counts, thisWindow);
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
	numBins = dialog.getNumBins();
	binSize = dialog.getBinSize();
	minBinSize = dialog.getMinBinSize();
	// use GenericGraphWindow's method for the rest.
	super.getDialogData();
    }

    public void setDialogData() {
	BinDialog dialog = (BinDialog)this.dialog;
	dialog.setBinSize(binSize);
	dialog.setNumBins(numBins);
	dialog.setMinBinSize(minBinSize);
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
	buttonPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Histogram Controls"));

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
	    setXAxis("Bin Interval Size (" + U.t(binSize) + ")", 
		     "Time", minBinSize, binSize);
	    setYAxis("Number of Occurrences", "");
	} else if (binType == TYPE_MSG_SIZE) {
	    setXAxis("Bin Interval Size (" + 
		     _format.format(binSize) + " bytes)", 
		     "", minBinSize, binSize);
	    setYAxis("Number of Occurrences", "");
	}
    }

    protected void refreshGraph()
    {
	// get new counts and redraw the graph
	counts = getCounts();
	setDataSource("Histogram", counts, thisWindow);
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
	bubbleText[1] = "Count: " + counts[xVal][yVal];
	if (xVal < numBins) {
	    bubbleText[2] = "Bin: " + U.t(xVal*binSize+minBinSize) +
		" to " + U.t((xVal+1)*binSize+minBinSize);
	} else {
	    bubbleText[2] = "Bin: > " + U.t(numBins*binSize+minBinSize);
	}
	return bubbleText;
    }

    private String[] getMsgSizePopup(int xVal, int yVal) {
	String bubbleText[] = new String[3];

	bubbleText[0] = Analysis.getEntryName(yVal);
	bubbleText[1] = "Count: " + counts[xVal][yVal];
	if (xVal < numBins) {
	    bubbleText[2] = "Bin: " + 
		_format.format(xVal*binSize+minBinSize) +
		" bytes to " + _format.format((xVal+1)*binSize+minBinSize) +
		" bytes";
	} else {
	    bubbleText[2] = "Bin: > " + 
		_format.format(numBins*binSize+minBinSize) + " bytes";
	}
	return bubbleText;
    }

    private double[][] getCounts() {
	if (binType == TYPE_TIME) {
	    return getTimeCounts();
	} else if (binType == TYPE_MSG_SIZE) {
	    return getMsgSizeCounts();
	}
	return null;
    }

    private double[][] getTimeCounts()
    {
	int numEPs = Analysis.getNumUserEntries();

	OrderedIntList tmpPEs = validPEs.copyOf();
	GenericLogReader r;
	// we create an extra bin to hold overflows.
        double [][] countData = new double[numBins+1][numEPs];

	LogEntryData logdata,logdata2;
	logdata = new LogEntryData();
	logdata2 = new LogEntryData();
	
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
		r.nextEventOnOrAfter(startTime,logdata);
		while(true){
		    r.nextEventOfType(ProjDefs.BEGIN_PROCESSING,logdata);
		    r.nextEventOfType(ProjDefs.END_PROCESSING,logdata2);

		    long executionTime = (logdata2.time - logdata.time);
		    long adjustedTime = executionTime - minBinSize;

		    // respect the user threshold.
		    if (adjustedTime >= 0) {
			int targetBin = (int)(adjustedTime/binSize);
			if (targetBin >= numBins) {
			    targetBin = numBins;
			}
			countData[targetBin][logdata.entry]+=1.0;
		    }
		    if (logdata2.time > endTime) {
			break;
		    }
		}
	    } catch(EOFException e) {
	     	// do nothing just reached end-of-file
	    } catch(Exception e) {
		System.out.println("Exception " + e);
		e.printStackTrace();
	    }
	}
	progressBar.close();
	return countData;
    }
    
    private double[][] getMsgSizeCounts() {
	int numEPs = Analysis.getNumUserEntries();

	OrderedIntList tmpPEs = validPEs.copyOf();
	GenericLogReader r;
	// we create an extra bin to hold overflows.
        double [][] countData = new double[numBins+1][numEPs];

	LogEntryData logdata;
	logdata = new LogEntryData();

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
		r.nextEventOnOrAfter(startTime,logdata);
		while(true){
		    r.nextEventOfType(ProjDefs.CREATION,logdata);
		    if (logdata.time > endTime) {
			break;
		    }
		    // respect the user threshold.
		    if (logdata.msglen >= minBinSize) {
			int targetBin = (int)(logdata.msglen/binSize);
			if (targetBin >= numBins) {
			    targetBin = numBins;
			}
			countData[targetBin][logdata.entry]+=1.0;
		    }
		}
	    } catch(EOFException e) {
	     	// do nothing just reached end-of-file
	    } catch(Exception e) {
		System.out.println("Exception " + e);
		e.printStackTrace();
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
