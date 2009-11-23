package projections.TimeProfile;

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.analysis.*;
import projections.gui.AmpiTimeProfileWindow;
import projections.gui.Analysis;
import projections.gui.ColorManager;
import projections.gui.ColorSelectable;
import projections.gui.EntrySelectionDialog;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;

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
	MainWindow mainWindow;
	
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private EntrySelectionDialog entryDialog;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JButton epSelection;
    private JButton setRanges;

    private IntervalChooserPanel intervalPanel;
    
    // **CW** this really should be a default button with ProjectionsWindow
    private JButton saveColors;
    private JButton loadColors;

    long intervalSize;

	int startInterval;

	int endInterval;

	// data used for intervalgraphdialog
    OrderedIntList processorList;

    // data required for entry selection dialog
    int numEPs;
    //YSun add
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

    
	// the following data are statically known and can be initialized
	// here overhead, idle time
    private final int special = 2;

    public TimeProfileWindow(MainWindow mainWindow) {
	super("Projections Time Profile Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
	setGraphSpecificData();

	this.mainWindow = mainWindow;
	
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
		intervalPanel = new IntervalChooserPanel();
		dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
	}

	dialog.displayDialog();
	if (!dialog.isCancelled()){
		intervalSize = intervalPanel.getIntervalSize();
		startInterval = (int)intervalPanel.getStartInterval();
		endInterval = (int)intervalPanel.getEndInterval();
		processorList = dialog.getSelectedProcessors();

		//set range values for time profile window
		if(ampiTraceOn){
			ampiGraphPanel.getRangeVals(dialog.getStartTime(),dialog.getEndTime(),
					startInterval, endInterval, intervalSize, processorList);
		}

		
		final SwingWorker worker =  new SwingWorker() {
		    public Object doInBackground() {

				int numIntervals = endInterval-startInterval+1; 
			    graphData = new double[numIntervals][numEPs+special]; //entry number + idle

		    	int numProcessors = processorList.size();
		    	int numUserEntries = MainWindow.runObject[myRun].getNumUserEntries();
		    	
			    if( MainWindow.runObject[myRun].hasLogFiles() || MainWindow.runObject[myRun].hasSumDetailFiles() ) {
				    // Do parallel loading because we have full logs

			    	Date time1  = new Date();

			    	// Create a list of worker threads
			    	LinkedList<Thread> readyReaders = new LinkedList<Thread>();

			    	// Create multiple result arrays to reduce contention for accumulating
			    	int numResultAccumulators = 8;
			    	double[][][] graphDataAccumulators = new double[numResultAccumulators][numIntervals][numEPs+special];
				 
	
			    	int pIdx=0;		
			    	while (processorList.hasMoreElements()) {
			    		int nextPe = processorList.nextElement();
			    		readyReaders.add( new ThreadedFileReader(nextPe, pIdx, intervalSize, myRun, 
			    					startInterval, endInterval, ampiTraceOn, 
			    					numIntervals, numUserEntries, numProcessors, numEPs, graphDataAccumulators[pIdx%numResultAccumulators]) );
			    		pIdx++;
			    	}

			    	Date time2  = new Date();

			    	
			    	// Determine a component to show the progress bar with
					Component guiRootForProgressBar = null;
					if(thisWindow!=null && thisWindow.isVisible()) {
						guiRootForProgressBar = thisWindow;
					} else if(mainWindow!=null && mainWindow.isVisible()){
						guiRootForProgressBar = mainWindow;
					} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
						guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
					}
			    	
			    	// Pass this list of threads to a class that manages/runs the threads nicely
			    	ThreadManager threadManager = new ThreadManager("Loading Time Profile in Parallel", readyReaders, guiRootForProgressBar);
			    	threadManager.runThreads();

			    	Date time3  = new Date();

			    	
			    	// Merge resulting graphData structures together.
			    	for(int a=0; a< numResultAccumulators; a++){
			    		for(int i=0;i<numIntervals;i++){
			    			for(int j=0;j<numEPs+special;j++){
			    				graphData[i][j] += graphDataAccumulators[a][i][j];
			    			}
			    		}	
			    	}

			    	Date time4  = new Date();
			    	
			    	double totalTime = ((time4.getTime() - time1.getTime())/1000.0);
			    	System.out.println("Time to read " + threadManager.numInitialThreads +  
			    			" input files(using " + threadManager.numConcurrentThreads + " concurrent threads): " + 
			    			totalTime + "sec");
//			    	System.out.println("Time to setup threads : " + ((time2.getTime() - time1.getTime())/1000.0) + "sec");
//			    	System.out.println("Time to load logs : " + ((double)(time3.getTime() - time2.getTime())/1000.0) + "sec");
//			    	System.out.println("Time to accumulate results : " + ((double)(time4.getTime() - time3.getTime())/1000.0) + "sec");
//			    	System.out.println("Logs loaded per second : " + (numProcessors / totalTime) );
			    			    	    	
			    }
			    else if( MainWindow.runObject[myRun].hasSumFiles()){
			    	// Do serial file reading because all we have is the sum files	    	
			    	
			    	// The data we use is
				    // 	systemUsageData[2][*][*]
				    // userEntryData[*][LogReader.TIME][*][*]

			    	int[][][]  systemUsageData = new int[3][][];
			    	systemUsageData[2] = new int[numProcessors][];

//			    	int[][][][] systemMsgsData = new int[5][3][numProcessors][];
			    	int[][][][] systemMsgsData = null;

			    	int[][][][] userEntryData  = new int[numUserEntries][][][];
			    	for(int n=0;n<numUserEntries;n++){
			    		userEntryData[n]  = new int[3][][];
			    		userEntryData[n][LogReader.TIME] = new int[numProcessors][];
			    	}
			    	
			    	int[][] temp = MainWindow.runObject[myRun].sumAnalyzer.getSystemUsageData(startInterval, endInterval, intervalSize);
			    	processorList.reset();
			    	systemUsageData[1] = new int[processorList.size()][endInterval-startInterval+1];
			    	for (int pIdx=0; pIdx<processorList.size(); pIdx++) {
			    		systemUsageData[1][pIdx] = temp[processorList.nextElement()];
			    	} 


			    	// Extract data and put it into the graph
			    	for(int peIdx=0; peIdx< numProcessors; peIdx++){
			    		for (int ep=0; ep<numEPs; ep++) {
			    			int[][] entryData = userEntryData[ep][LogReader.TIME];
			    			for (int interval=0; interval<numIntervals; interval++) {
			    				graphData[interval][ep] += entryData[peIdx][interval];
			    				graphData[interval][numEPs] -= entryData[peIdx][interval]; // overhead = -work time
			    			}
			    		}

			    		//YS add for idle time SYS_IDLE=2
			    		int[][] idleData = systemUsageData[2]; //percent
			    		for (int interval=0; interval<numIntervals; interval++) {
			    			if(idleData[peIdx] != null && idleData[peIdx].length>interval){
			    				graphData[interval][numEPs+1] += idleData[peIdx][interval] * 0.01 * intervalSize;
			    				graphData[interval][numEPs] -= idleData[peIdx][interval] * 0.01 * intervalSize; //overhead = - idle time
			    				graphData[interval][numEPs] += intervalSize;  
			    			}
			    		}
			    	}					
				}
				    
				
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
			
			return null;
		    }
		    public void done() {
			setOutputGraphData();
                        if(ampiTraceOn){
                            ampiGraphPanel.setOutputGraphData(true);
                        }
			thisWindow.setVisible(true);                        
		    }
		};
	    worker.execute();
	}
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
	String[] rString = new String[4];
	
	rString[0] = "Time Interval: " + 
	    U.t((xVal+startInterval)*intervalSize) + " to " +
	    U.t((xVal+startInterval+1)*intervalSize);
	rString[1] = "Chare Name: " + epClassName;
	rString[2] = "Entry Method: " + epName;
    rString[3] = "Execution Time = " + U.t((long)(outputData[xVal][yVal]));
    //deal with idle and overhead time
    if(yVal == outputData[xVal].length -2)
	{
        rString[1] = "";
        rString[2] = "Overhead";
        rString[3] = "Time = " + U.t((long)(outputData[xVal][yVal]));
	}else if(yVal == outputData[xVal].length -1)
	{
        rString[1] = "";
        rString[2] = "Idle time";
        rString[3] = "Time = " + U.t((long)(outputData[xVal][yVal]));
	}
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
