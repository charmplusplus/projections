package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class HistogramWindow extends GenericGraphWindow 
    implements ActionListener
{
    // GUI sub-components
    private JButton entrySelectionButton;
    private JButton epTableButton;

    // Data maintained by HistogramWindow
    // countData is indexed by bin index followed by ep id.
    private double[][] counts;
    private long longestExecutionTime;
    
    //variables to be passed to EntrySelectionDialog
    private EntrySelectionDialog entryDialog;
    private boolean stateArray[][];
    private boolean existsArray[][];
    private Color colorArray[][];
    private String [] entryNames; 
    
    private EntryPointWindow epFrame = null;

    // variables (in addition to those in the super class) 
    // to be set by TimeBinDialog.
    public int numBins;
    public long binSize;
    public long threshold;

    private HistogramWindow thisWindow;

    void windowInit() {
	threshold = 1000; // 1ms default
	numBins = 100;  // default to 100 bins
	binSize = 1000; // 1ms default bin size
	// use GenericGraphWindow's method for the rest.
	super.windowInit();
    }
    
    public HistogramWindow(MainWindow mainWindow, Integer myWindowID)
    {
	super("Projections Histograms", mainWindow, myWindowID);
	thisWindow = this;

	setTitle("Projections Histograms");
	setGraphSpecificData();

	epFrame = new EntryPointWindow();
	epFrame.setSize(600,600);

	// initializing data fields
	int noEPs = Analysis.getNumUserEntries();
	existsArray = new boolean[1][noEPs];
	for (int i=0; i<noEPs; i++) {
	    existsArray[0][i] = true;
	}
	stateArray = new boolean[1][noEPs];	
	for (int i=0; i<noEPs; i++) {
	    stateArray[0][i] = true;
	}
	String names[][] = Analysis.getEntryNames();
	entryNames = new String[noEPs];
	for (int i=0; i<noEPs ; i++) {
	    entryNames[i] = names[i][0];
	}
	colorArray = new Color[1][noEPs];
	for (int i=0; i<noEPs; i++) {
	    colorArray[0][i] = Analysis.getEntryColor(i);
	}
	
	createMenus();
	getContentPane().add(getMainPanel());
	
	pack();
	showDialog();
    }   
    
    /* if there is an epFrame existing, dispose it before disposing 
       the window 
    */
    public void close(){
	if(epFrame != null)
	    epFrame.dispose();
	super.close();
    }

    /* 
     *  Show the TimeBinDialog 
     */
    public void showDialog()
    {
	if (dialog == null) {
	    dialog = 
		new TimeBinDialog(this, "Select Histogram Time Range");
	} else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
	    getDialogData();
	    System.out.println("Bin Size is " + U.t(binSize));
	    System.out.println("Threshold is " + U.t(threshold));
	    final SwingWorker worker = new SwingWorker() {
		    public Object construct() {
			if (dialog.isModified()) {
			    counts = thisWindow.getCounts();
			}
			return null;
		    }
		    public void finished() {
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
	TimeBinDialog dialog = (TimeBinDialog)this.dialog;
	threshold = dialog.getThreshold();
	numBins = dialog.getNumBins();
	binSize = dialog.getBinSize();
	// use GenericGraphWindow's method for the rest.
	super.getDialogData();
    }

    public void setDialogData() {
	TimeBinDialog dialog = (TimeBinDialog)this.dialog;
	dialog.setBinSize(binSize);
	dialog.setNumBins(numBins);
	dialog.setThreshold(threshold);
	super.setDialogData();
    }

    /* Show the EntrySelectionDialog to select Entrypoints to be considered */
    // **CW** 1/12/2004 this should really be the legend panel. Will be fixed
    // once the generic legend panel gets written.
    void showEntryDialog()
    {
	int noEPs = Analysis.getNumUserEntries();
	String typeLabelStrings[] = {"Entry Points"};

	if (entryDialog == null)
	    entryDialog = 
		new EntrySelectionDialog(this, typeLabelStrings, stateArray,
					 colorArray,existsArray,entryNames);
	entryDialog.showDialog();
	refreshGraph();
    }

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof JMenuItem) {
	    JMenuItem m = (JMenuItem)evt.getSource();
	    if(m.getText().equals("Set Range"))
		showDialog();
	    else if(m.getText().equals("Select Entry Points"))
		showEntryDialog();
	    else if(m.getText().equals("Close"))
		close();
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

	buttonPanel.add(entrySelectionButton);
	buttonPanel.add(epTableButton);

	/*
	mainPanel.add(Box.createRigidArea(new Dimension(0,6)));
	mainPanel.add(new JScrollPane(statusArea,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

	*/
        Util.gblAdd(mainPanel, graphPanel,  gbc, 0,0, 1,1, 1,1);
        Util.gblAdd(mainPanel, buttonPanel, gbc, 0,1, 1,1, 0,0);

	return mainPanel;
    }  

    protected void setGraphSpecificData(){
	setXAxis("Entry Point Execution Time (us)","us", 0, binSize);
	setYAxis("Instances", "");
    }

    protected void refreshGraph()
    {
	// get new counts and redraw the graph
	counts = getCounts();
	setDataSource("Histogram",counts);
	super.refreshGraph();
    }

    public String[] getPopup(int xVal, int yVal) {
	String bubbleText[] = new String[10];

	bubbleText[0] = "Entry: " + entryNames[yVal];
	bubbleText[1] = "Count: " + counts[xVal][yVal];
	bubbleText[2] = "Bin: " + U.t(xVal*binSize) +
	    " to " + U.t((xVal+1)*binSize);

	return bubbleText;
    }

    private void showEpFrame() {
	epFrame.setVisible(true);
    }

    private double[][] getCounts()
    {
	int instances = 0;
	longestExecutionTime=0;
	int numEPs = Analysis.getNumUserEntries();

	OrderedIntList tmpPEs = validPEs.copyOf();
	GenericLogReader r;
        double [][] countData = new double[numBins][numEPs];
	for (int i=0; i<numBins; i++) {
	    for (int j=0; j<numEPs; j++) {
		countData[i][j] = 0.0;
	    }
	}

	// each time we reset data, we have to assume everything exists until
	// proven otherwise ...
	for (int i=0; i<numEPs; i++) {
	    existsArray[0][i] = true;
	}

	// we also clear away the data in the epFrame
	epFrame.clearTableData();

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
		    // if the entry method is selected, count it
		    if (stateArray[0][logdata.entry]) {
			long executionTime = (logdata2.time - logdata.time);
			
			// apply thresholding feature
			if (executionTime < threshold) {
			    break;
			}
			
			int targetBin = (int)(executionTime/binSize);
			if (targetBin >= numBins) {
			    // if not within range, enter the data into 
			    // the table
			    epFrame.writeToTable(pe,entryNames[logdata.entry],
						 logdata.time,logdata2.time,
						 colorArray[0][logdata.entry]);
			    if (executionTime > longestExecutionTime) {
				longestExecutionTime = executionTime;
			    }
			    targetBin = numBins-1;
			}
			countData[targetBin][logdata.entry]+=1.0;
			instances++;
		    }
		    if (logdata2.time > endTime) {
			break;
		    }
		}
	    }catch(EOFException e){
	     	// do nothing just reached end-of-file
	    }catch(Exception e){
		System.out.println("Exception " + e);
		e.printStackTrace();
	    }
	}
	progressBar.close();

	// now filter away those EPs that never showed up
	for (int ep=0; ep<numEPs; ep++) {
	    double epCount = 0.0;
	    for (int bin=0; bin<numBins; bin++) {
		epCount += counts[bin][ep];
	    }
	    if (epCount == 0.0) {
		existsArray[0][ep] = false;
	    }
	}
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
