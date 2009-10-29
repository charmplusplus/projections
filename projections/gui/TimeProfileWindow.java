package projections.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;
/**
 *  TimeProfileWindow
 *  by Chee Wai Lee
 *
 *  Will replace The old GraphWindow class once a framework for displaying
 *  Legends are in place (and probably replace the name)
 */
public class TimeProfileWindow extends GenericGraphWindow
    implements ActionListener, ColorSelectable
{

	TimeProfileWindow thisWindow;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private EntrySelectionDialog entryDialog;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JButton epSelection;
    private JButton setRanges;

    // **CW** this really should be a default button with ProjectionsWindow
    private JButton saveColors;
    private JButton loadColors;

    // data used for intervalgraphdialog
    int startInterval;
    int endInterval;
    long intervalSize;
    OrderedIntList processorList;

    // data required for entry selection dialog
    int numEPs;
    //YSun add
    int special;
    private String typeLabelNames[] = {"Entry Points"};
    boolean stateArray[][];
    boolean existsArray[][];
    private Color colorArray[][];
    private String entryNames[];

    // stored raw data
    double[][] graphData;

    // output arrays
    private double[][] outputData;
    private Color[] outColors;
    
    // flag signifying callgraph has just begun
    boolean	   startFlag;

    //Chao Mei: variables related with ampi time profile    
    private JTabbedPane tabPane = null;
    AmpiTimeProfileWindow ampiGraphPanel = null;
    private JPanel epPanel = null;
    boolean ampiTraceOn = false;
    protected int ampiPanelTabIndex;
    protected int epPanelTabIndex;

    protected void windowInit() {
	// acquire data using parent class
	super.windowInit();

	intervalSize = 1000; // default 1ms 
	startInterval = 0;
	if (endTime%intervalSize == 0) {
	    endInterval = (int)(endTime/intervalSize - 1);
	} else {
	    endInterval = (int)(endTime/intervalSize);
	}
	processorList = MainWindow.runObject[myRun].getValidProcessorList();
    }

    public TimeProfileWindow(MainWindow mainWindow) {
	super("Projections Time Profile Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
	setGraphSpecificData();
	// the following data are statically known and can be initialized
	// here overhead, idle time
	special = 2;
	numEPs = MainWindow.runObject[myRun].getNumUserEntries();
	stateArray = new boolean[1][numEPs+special];
	existsArray = new boolean[1][numEPs+special];
	colorArray = new Color[1][];
	colorArray[0] = MainWindow.runObject[myRun].getColorMap();
	entryNames = new String[numEPs+special];
	for (int ep=0; ep<numEPs; ep++) {
	    entryNames[ep] = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
	}
	//YSun idle time
	//for(int ep = numEPs; ep<numEPs+special; ep++)
	entryNames[numEPs] = "Overhead";
	entryNames[numEPs+1] = "Idle time";
		
	mainPanel = new JPanel();
    	getContentPane().add(mainPanel);

        //creating ampi tabbed pane
        if(MainWindow.runObject[myRun].getNumFunctionEvents() > 0)
            ampiTraceOn = true;
        if(ampiTraceOn){
            tabPane = new JTabbedPane();
            ampiGraphPanel = new AmpiTimeProfileWindow(mainWindow);
        }

	createMenus();
	createLayout();
	pack();
	thisWindow = this;
	startFlag = true;
        thisWindow.setLocationRelativeTo(null);
	showDialog();
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
//        mbar.add(Util.makeJMenu("Tools", new Object[]
//            {
//                "Change Colors",
//            },
//                                null, this));
//        mbar.add(Util.makeJMenu("Help", new Object[]
//            {
//                "Index",
//                                    "About"
//            },
//                                null, this));
        setJMenuBar(mbar);
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
	saveColors = new JButton("Save Entry Colors");
	saveColors.addActionListener(this);
	loadColors = new JButton("Load Entry Colors");
	loadColors.addActionListener(this);
	controlPanel = new JPanel();
	controlPanel.setLayout(gbl);
	Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, setRanges,   gbc, 1,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, saveColors,  gbc, 2,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, loadColors,  gbc, 3,0, 1,1, 0,0);

        if(ampiTraceOn){            
            epPanel = new JPanel();
            epPanel.setLayout(gbl);
            JPanel graphPanel = getMainPanel();
            Util.gblAdd(epPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
            Util.gblAdd(epPanel, controlPanel, gbc, 0,1, 1,0, 0,0);

            JPanel ampiPanel = ampiGraphPanel.getAmpiMainPanel();
            tabPane.add("Entry Points", epPanel);
            tabPane.add("AMPI Functions", ampiPanel);
            epPanelTabIndex = tabPane.indexOfComponent(epPanel);
            ampiPanelTabIndex = tabPane.indexOfComponent(ampiPanel);
            mainPanel.setLayout(new GridLayout(1,1));
            mainPanel.add(tabPane);
        } else {
            JPanel graphPanel = getMainPanel();
            Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
            Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,0, 0,0);
        }		
    }

    public void setGraphSpecificData() {
	setXAxis("Time in us","");
	setYAxis("Entry point execution time", "us");
    }

    public void showDialog() {
	if (dialog == null) {
	    dialog = new IntervalRangeDialog(this, "Select Range");
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();

            //set range values for time profile window
            if(ampiTraceOn){
                ampiGraphPanel.getRangeVals(dialog.getStartTime(),dialog.getEndTime(),
                                            startInterval, endInterval, intervalSize, processorList);
            }

	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
			if (dialog.isModified()) {
			    int nextPe = 0;
			    int count = 0;
			    int numIntervals = endInterval-startInterval+1;
			    //entry number + idle
			    graphData = new double[numIntervals][numEPs+special];
			    long progressStart = System.currentTimeMillis();
			    ProgressMonitor progressBar =
				new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, 
						    "Reading log files",
						    "", 0,
						    processorList.size());
			    progressBar.setNote("Reading");
			    progressBar.setProgress(0);
			    while (processorList.hasMoreElements()) {
				nextPe = processorList.nextElement();
				progressBar.setProgress(count);
				progressBar.setNote("[PE: " + nextPe +
						    " ] Reading Data.");
				if (progressBar.isCanceled()) {
				    return null;
				}
				// inefficient, but temporary workaround
				OrderedIntList tempList = 
				    new OrderedIntList();
				tempList.insert(nextPe);
				MainWindow.runObject[myRun].LoadGraphData(intervalSize,
						       startInterval,
						       endInterval,
						       true, tempList);
                                if(ampiTraceOn){
                                    ampiGraphPanel.createAMPITimeProfileData(nextPe, count);
                                }
				fillGraphData();
				count++;
			    }
			    progressBar.close();
			    // set the exists array to accept non-zero 
			    // entries only have initial state also 
			    // display all existing data. Only do this 
			    // once in the beginning
			    if (startFlag) {
				for (int ep=0; ep<numEPs+special; ep++) {
				    for (int interval=0; 
					 interval<endInterval-startInterval+1;
					 interval++) {
					if (graphData[interval][ep] > 0) {
					    existsArray[0][ep] = true;
					    stateArray[0][ep] = true;
					    break;
					}
				    }
				}
			    }
			    if (startFlag)
				startFlag = false;
			}
			return null;
		    }
		    public void finished() {
			setOutputGraphData();
                        if(ampiTraceOn){
                            ampiGraphPanel.setOutputGraphData(true);
                        }
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

    void fillGraphData() {
	// for now, just assume all processors are added to the mix.
	// LogReader is BROKEN and cannot deal with partial data properly.
	// Any current attempts to fix this will cause GraphWindow to fail
	// when partial data is actually read.

	for (int ep=0; ep<numEPs; ep++) {
	    int[][] entryData = MainWindow.runObject[myRun].getUserEntryData(ep, LogReader.TIME);
	    for (int interval=0; interval<graphData.length; interval++) {
		graphData[interval][ep] += entryData[0][interval];
	    }
	}
	

	//YS add for idle time SYS_IDLE=2
	int[][] idleData = MainWindow.runObject[myRun].getSystemUsageData(2); //percent
    System.out.println("idle time= ");
    for (int interval=0; interval<graphData.length; interval++) {
    		graphData[interval][numEPs+1] += idleData[0][interval] * 0.01 * intervalSize;
            System.out.print( idleData[0][interval]+"  ");
    }
   System.out.println("====================="); 
    //YS overhead
    for(int interval=0; interval<graphData.length; interval++)
    {
    	for (int ep=0; ep<numEPs; ep++) // all work time
    	{
    		graphData[interval][numEPs] += graphData[interval][ep];
    	}
    	graphData[interval][numEPs] += graphData[interval][numEPs+1]; // worktime+idle time
    	
    	graphData[interval][numEPs] = intervalSize - graphData[interval][numEPs];
    }
    
    System.out.println("YS Debug: intervalSize="+intervalSize + "idle time 0" + graphData[0][numEPs+1] + "\t overhead=" + graphData[0][numEPs]);

    }

    public void applyDialogColors() {
	setOutputGraphData();
    }

    void setOutputGraphData() {
	// need first pass to decide the size of the outputdata
	int outSize = 0;
	for (int ep=0; ep<numEPs+special; ep++) {
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
		for (int ep=0; ep<numEPs+special; ep++) {
		    if (stateArray[0][ep]) {
			outputData[i][count] = graphData[i][ep];
			if(ep == numEPs)
				outColors[count++] = Color.black;
			else if (ep == numEPs+1)
				outColors[count++] = Color.white;
			else
				outColors[count++] = colorArray[0][ep];
		    }
		}

	    }
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Entry point execution time", "us");
	    setDataSource("Time Profile Graph", outputData, 
			  outColors, thisWindow);
	    refreshGraph();
	}
    }

    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}

	/******************* YSun change here */
	// find the ep corresponding to the yVal
	int count = 0;
	String epName = "";
	String epClassName = "";
	for (int ep=0; ep<numEPs; ep++) {
	    if (stateArray[0][ep]) {
		if (count++ == yVal) {
		    epName = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
		    epClassName = MainWindow.runObject[myRun].getEntryChareNameByIndex(ep);
		    break;
		}
	    }
	}
//	
//	if(yVal == outputData[xVal].length -2)
//	{
//		epName = "Overhead";
//	}else if(yVal == outputData[xVal].length -1)
//	{
//		epName = "Idle time";
//	}

	String[] rString = new String[4];
	
	rString[0] = "Time Interval: " + 
	    U.t((xVal+startInterval)*intervalSize) + " to " +
	    U.t((xVal+startInterval+1)*intervalSize);
	rString[1] = "Chare Name: " + epClassName;
	rString[2] = "Entry Method: " + epName;
	rString[3] = "Execution Time = " + U.t((long)(outputData[xVal][yVal]));
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
	    } else if (b == saveColors) {
		// save all entry point colors to disk
		MainWindow.runObject[myRun].saveColors();
	    } else if (b == loadColors) {
		// load all entry point colors from disk
		try {
		    ColorManager.loadActivityColors(Analysis.PROJECTIONS, colorArray[0]);
		    // silly inefficiency
		    setOutputGraphData();
		} catch (IOException exception) {
		    System.err.println("Failed to load colors!!");
		}
	    }
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
