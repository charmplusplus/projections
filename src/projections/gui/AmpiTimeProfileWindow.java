package projections.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;

import projections.analysis.AmpiFunctionData;
import projections.analysis.AmpiProcessProfile;

/**
 *  AmpiTimeProfileWindow
 *  by Chao Mei
 *
 *  This is class contains necessary implementations for generating ampi functions' time profile.
 *  (how ampi's function execution time distributes over the whole execution time)
 *  This class represents a tab pane in the TimeProfileWindow if the trace contains ampi functions
 *  It will be more concise if using graph canvas which excludes scale control buttons.
 *  But it complicates the integration with entry points time profile.
 */
public class AmpiTimeProfileWindow extends GenericGraphWindow
    implements ActionListener, ColorSelectable
{
	private AmpiTimeProfileWindow thisWindow = null;
//    private EntrySelectionDialog entryDialog = null;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    private static int myRun = 0;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JButton epSelection;
    private JButton setRanges;
    private JButton saveColors;
    private JButton loadColors;


    // data used for intervalgraphdialog
    private int startInterval;
    private int endInterval;
    private long intervalSize; //in terms of microseconds!
    private OrderedIntList processorList;    
    private Vector[] processProfiles = null; //every vector element is an instance of AmpiProcessProfile

    // The tool specific GUI for the dialog
    private IntervalChooserPanel intervalPanel;

    // data required for entry selection dialog
    private int numFunctions;
//    private String typeLabelNames[] = {"Ampi Functions"};

    //this variable shows whether this function (indexed by 2D_Y) needs to be displayed
    private boolean stateArray[];

    //this variable shows whether this function (indexed by 2D_Y) has its execution during
    //the selected interval
    private boolean existsArray[];

    private Color colorArray[];
    private String funcNames[];
    private double[][] graphData=null;

    //output arrays
    private double[][] outputData = null;
    private Color[] outColors = null;
    

    // flag signifying callgraph has just begun
    //private boolean	   startFlag;

    
    public AmpiTimeProfileWindow(MainWindow mainWindow) {
	super("Projections Time Profile Graph--AMPI - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
	setGraphSpecificData();	
        createLayout();
        thisWindow = this;
    }

    private void createLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gbl = new GridBagLayout();

        gbc.fill = GridBagConstraints.BOTH;

        mainPanel = new JPanel();
        mainPanel.setLayout(gbl);

        // control panel items
        epSelection = new JButton("Select Functions");
        epSelection.addActionListener(this);
        setRanges = new JButton("Select New Range");
        setRanges.addActionListener(this);
        saveColors = new JButton("Save Function Colors");
        saveColors.addActionListener(this);
        loadColors = new JButton("Load Function Colors");
        loadColors.addActionListener(this);
        controlPanel = new JPanel();
        controlPanel.setLayout(gbl);
        Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
        Util.gblAdd(controlPanel, setRanges,   gbc, 1,0, 1,1, 0,0);
        Util.gblAdd(controlPanel, saveColors,  gbc, 2,0, 1,1, 0,0);
        Util.gblAdd(controlPanel, loadColors,  gbc, 3,0, 1,1, 0,0);

        JPanel graphPanel = getMainPanel();
        Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
        Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,0, 0,0);        	
    }

    public JPanel getAmpiMainPanel(){
        return mainPanel;
    }

    public void setGraphSpecificData() {
	setXAxis("Time in us","");
	setYAxis("Function execution time", "us");
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
    		processProfiles = new Vector[processorList.size()];
    	}        
    }

    /**
     * procId: the processor ID
     * index: the order of this processor in the processorList
     */
    private void createAMPITimeProfileData(int procId, int index){        
        processProfiles[index] = new Vector();
        //currently read all log data thus obtaining the time profile across the whole timeline!
        MainWindow.runObject[myRun].createAMPITimeProfile(procId,0,MainWindow.runObject[myRun].getTotalTime(), processProfiles[index]);
    }
    
    public void getRangeVals(int beginI, int endI, long iSize, OrderedIntList procList){
        startInterval = beginI;
        endInterval = endI;
        intervalSize = iSize;
        processorList = procList.copyOf();
        processProfiles = new Vector[processorList.size()];        
    }


    /**
     * After collecting functions' data, we should begin to create interval data for the graph
     */
    private void fillGraphData() {
        int funcCnt=0;
        for(int i=0; i<processProfiles.length; i++){
            Vector v = processProfiles[i];
            for(int j=0; j<v.size(); j++){
                AmpiProcessProfile p = (AmpiProcessProfile)v.get(j);
                Stack funcStk = p.getFinalCallFuncStack();
                funcCnt += funcStk.size();
            }
        }

        numFunctions = funcCnt;
        outColors = ColorManager.createColorMap(funcCnt);
        funcNames = new String[funcCnt];
        colorArray = new Color[funcCnt];        
        //initialize the stateArray that all functions will be displayed. Values
        //may be changed after function selection dialog
        stateArray = new boolean[funcCnt];
        //initialize the existsArray that all function don't exist as its value 
        //is determined to by the interval!!
        existsArray = new boolean[funcCnt];
        for(int i=0; i<funcCnt; i++){
            colorArray[i] = outColors[i];
            stateArray[i] = true;
            existsArray[i] = false;
        }            

        int intervalsCnt = endInterval-startInterval+1;
        outputData = new double[intervalsCnt][funcCnt];
        for(int i=0; i<intervalsCnt; i++)
            for(int j=0; j<funcCnt; j++)
                outputData[i][j] = 0.0;

        
        funcCnt=0;
        long rangeStart = startInterval*intervalSize;
        long rangeEnd = endInterval*intervalSize;        
        for(int i=0; i<processProfiles.length; i++){
            Vector v = processProfiles[i];
            for(int j=0; j<v.size(); j++){
                AmpiProcessProfile p = (AmpiProcessProfile)v.get(j);
                Stack funcStk = p.getFinalCallFuncStack();                
                for(int k=0; k<funcStk.size(); k++){
                    AmpiFunctionData funcData = (AmpiFunctionData)funcStk.get(k);
                    funcNames[funcCnt] = funcData.getFunctionName();
                    if(stateArray[funcCnt] == false)
                        continue;
                    for(int l=0; l<funcData.execIntervalCnt(); l++){
                        AmpiFunctionData.AmpiFuncExecInterval oneInterval = funcData.getIntervalAt(l);
                        long startTime = (oneInterval.startTimestamp>rangeStart)?oneInterval.startTimestamp:rangeStart;
                        long endTime = (oneInterval.endTimestamp<rangeEnd)?oneInterval.endTimestamp:rangeEnd;

                        if(endTime<=startTime)
                            continue;

                        existsArray[funcCnt] = true;
                        int head = (int)((startTime-rangeStart)/intervalSize);
                        int tail = (int)((endTime-rangeStart)/intervalSize);

                        //System.out.println(startTime+":"+endTime+":"+head+":"+tail);
                        outputData[head][funcCnt] = ((head+1)*intervalSize+rangeStart-startTime)/(double)intervalSize;
                        outputData[tail][funcCnt] = (endTime-tail*intervalSize-rangeStart)/(double)intervalSize;
                        for(int m=head+1; m<tail; m++)
                            outputData[m][funcCnt] = 1;                        
                    }
                    funcCnt++;
                }                
            }
        }

        //fill in graphData array
        graphData = new double[outputData.length][];
        for(int i=0; i<outputData.length; i++){
            graphData[i] = new double[outputData[i].length];
            for(int j=0; j<outputData[i].length; j++)
                graphData[i][j] = outputData[i][j];
        }            
    }

    public void applyDialogColors() {
        //set outputData according to recently changed stateArray
        for(int i=0; i<numFunctions; i++){
            outColors[i] = colorArray[i];
        }
        for(int i=0; i<outputData.length; i++){
            for(int j=0; j<numFunctions; j++){
                if(stateArray[j]){
                    outputData[i][j] = graphData[i][j];
                } else {
                    outputData[i][j] = 0.0;
                }
            }
        }
        setOutputGraphData(false);            
    }

    public void setOutputGraphData(boolean reCompute) {
        if(reCompute)
            fillGraphData();        

        setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	setYAxis("AMPI function Execution Time (intervals)", "");
	setDataSource("Time Profile Graph", outputData, 
			  outColors, thisWindow);
	super.refreshGraph();
    }

    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}

	// find the function corresponding to the yVal
	int count = 0;
	String name = null;	
	for (int i=0; i<numFunctions; i++) {
	    if (stateArray[i]) {
		if (count++ == yVal) {
		    name = funcNames[i];		    
		    break;
		}
	    }
	}

        if(name==null) return null;

	String[] rString = new String[2];
	
	rString[0] = name;
	rString[1] = "Execution Time = " + (long)(outputData[xVal][yVal]*intervalSize/1000)+"ms";	

        //System.out.println(rString[0]+":"+rString[1]+"; ("+xVal+":"+yVal+")");
	return rString;
    }	

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
	    JButton b = (JButton)e.getSource();
	    if (b == epSelection) {
//		if (entryDialog == null) {
//		    entryDialog = 
//			new EntrySelectionDialog(this,
//						 typeLabelNames,
//						 stateArray,colorArray,
//						 existsArray,funcNames);
//		}
//		entryDialog.showDialog();
	    } else if (b == setRanges) {
		showDialog();

                processorList.reset();
                int index=0;
                while(processorList.hasMoreElements()){
                    int procId = processorList.nextElement();
                    createAMPITimeProfileData(procId,index++);
                }

                setOutputGraphData(true);
	    } else if (b == saveColors) {
		// save all entry point colors to disk
		MainWindow.runObject[myRun].saveColors();
	    } else if (b == loadColors) {
		//load all entry point colors from disk
		try {
		    ColorManager.loadActivityColors(Analysis.PROJECTIONS, colorArray);
		    // silly inefficiency
		    setOutputGraphData(false);
		} catch (IOException exception) {
		    System.err.println("Failed to load colors!!");
		}
	    }
	}
    }
}
