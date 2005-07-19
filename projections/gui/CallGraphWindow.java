package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import javax.swing.*;

import projections.analysis.*;
import projections.gui.graph.*;

public class CallGraphWindow extends GenericGraphWindow
    implements ItemListener, ActionListener, ColorSelectable
{
    // Sent External code commented out and may be implemented later

    private CallGraphWindow      thisWindow;    

    private EntrySelectionDialog entryDialog;
        
    private JPanel	   mainPanel;
    private JPanel	   graphPanel;
    private JPanel	   checkBoxPanel;
    private JPanel         controlPanel;
    
    private JButton	   setRanges;
    private JButton	   epSelection;
    private JButton        saveColors;
    private JButton        loadColors;
    
    private CheckboxGroup  cbg;
    private Checkbox	   sentMsgs;
    private Checkbox	   sentBytes;
    private Checkbox	   receivedMsgs;
    private Checkbox	   receivedBytes;
    //private Checkbox	   sentExternalMsgs;
    //private Checkbox	   sentExternalBytes;
    private Checkbox	   receivedExternalMsgs;
    private Checkbox	   receivedExternalBytes;
    
    private int		   startInterval;
    private int		   endInterval;
    private int		   numIntervals;
    private int		   numEPs;
    private long	   intervalSize;
    private OrderedIntList processorList;
    
    private String	   currentArrayName;
        
    // data required for entry selection dialog
    private String         typeLabelNames[] = {"Entry Points"};
    private boolean        stateArray[][];
    private boolean        existsArray[][];
    private Color          colorArray[][];
    private String         entryNames[];
    
    // stored raw data
    private double[][]	   sentMsgCount;
    private double[][]     sentByteCount;
    private double[][]	   receivedMsgCount;
    private double[][]     receivedByteCount;
    //private double[][]     sentExternalMsgCount;
    //private double[][]     sentExternalByteCount;
    private double[][]	   receivedExternalMsgCount;
    private double[][]     receivedExternalByteCount;
    
    // output arrays    
    private Color[]        outColors;
    private double[][]     sentMsgOutput;
    private double[][]     sentByteOutput;
    private double[][]     receivedMsgOutput;
    private double[][]     receivedByteOutput;
    //private double[][]     sentExternalMsgOutput;
    //private double[][]     sentExternalByteOutput;
    private double[][]     receivedExternalMsgOutput;
    private double[][]     receivedExternalByteOutput;

    // flag signifying callgraph has just begun
    private boolean	   startFlag;
            
    void windowInit() {
        // acquire data using parent class
	super.windowInit();
	
	intervalSize = 1000; // default 1ms 
	startInterval = 0;
	if (endTime%intervalSize == 0) {
	    endInterval = (int)(endTime/intervalSize - 1);
	} 
	else {
	    endInterval = (int)(endTime/intervalSize);
	}
	numIntervals = endInterval-startInterval+1;
	processorList = Analysis.getValidProcessorList();
    }

    public CallGraphWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections Call Graph", mainWindow, myWindowID);
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
	setLayout(mainPanel);
	//getContentPane().add(mainPanel);
	createLayout();
	pack();
	thisWindow = this;
	startFlag = true;
	
	showDialog();
    }

    protected void createLayout() {
	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();

	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);

	// checkbox panel items
	cbg = new CheckboxGroup();
	sentMsgs = new Checkbox("Messages Sent", cbg, true);
	sentMsgs.addItemListener(this);
	sentBytes = new Checkbox("Bytes Sent", cbg, false);
	sentBytes.addItemListener(this);
	receivedMsgs = new Checkbox("Messages Received", cbg, false);
	receivedMsgs.addItemListener(this);
	receivedBytes = new Checkbox("Bytes Received", cbg, false);
	receivedBytes.addItemListener(this);
	//sentExternalMsgs = new Checkbox("Messages Sent Externally", cbg, false);
	//sentExternalMsgs.addItemListener(this);
	//sentExternalBytes = new Checkbox("Bytes Sent Externally", cbg, false);
	//sentExternalBytes.addItemListener(this);
	receivedExternalMsgs = new Checkbox("Messages Received Externally", cbg, false);
	receivedExternalMsgs.addItemListener(this);
	receivedExternalBytes = new Checkbox("Bytes Received Externally", cbg, false);
	receivedExternalBytes.addItemListener(this);
	checkBoxPanel = new JPanel();
	Util.gblAdd(checkBoxPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
	//Util.gblAdd(checkBoxPanel2, sentExternalMsgs, gbc, 0,0, 1,1, 1,1);
	//Util.gblAdd(checkBoxPanel2, sentExternalBytes, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedExternalMsgs, gbc, 4,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedExternalBytes, gbc, 5,0, 1,1, 1,1);
	
	// control panel items
	setRanges = new JButton("Select New Range");
	setRanges.addActionListener(this);
	epSelection = new JButton("Select Entry Points");
	epSelection.addActionListener(this);
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
	
	graphPanel = getMainPanel();
	Util.gblAdd(mainPanel, graphPanel,     gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(mainPanel, checkBoxPanel,  gbc, 0,2, 1,1, 0,0);
	Util.gblAdd(mainPanel, controlPanel,   gbc, 0,3, 1,0, 0,0);
    }

    public void itemStateChanged(ItemEvent ae){
	if(ae.getSource() instanceof Checkbox){
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));
	    Checkbox cb = (Checkbox)ae.getSource();
	    setCheckboxData(cb);
	    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
    }
    
    public void setCheckboxData(Checkbox cb) {
	if(cb == sentMsgs) {
	    setDataSource("Call Graph", sentMsgOutput, outColors, this);
	    setPopupText("sentMsgCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Sent", "");
	    super.refreshGraph();
	}
	else if(cb == sentBytes){
	    setDataSource("Call Graph", sentByteOutput, outColors, this);
	    setPopupText("sentByteCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Sent", "");
	    super.refreshGraph();
	}
	else if(cb == receivedMsgs){
	    setDataSource("Call Graph", receivedMsgOutput, outColors, this);
	    setPopupText("receivedMsgCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Received", "");
	    super.refreshGraph();
	}
	else if(cb == receivedBytes){
	    setDataSource("Call Graph", receivedByteOutput, outColors, this);
	    setPopupText("receivedByteCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Received", "");
	    super.refreshGraph();
	}
/*
	else if(cb == sentExternalMsgs) {
	    setDataSource("Call Graph", sentExternalMsgOutput, outColors, this);
	    setPopupText("sentExternalMsgCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Sent Externally", "");
	    super.refreshGraph();
	}
	else if(cb == sentExternalBytes){
	    setDataSource("Call Graph", sentExternalByteOutput, outColors, this);
	    setPopupText("sentExternalByteCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Sent Externally", "");
	    super.refreshGraph();
        }
*/
	else if(cb == receivedExternalMsgs){
	    setDataSource("Call Graph", receivedExternalMsgOutput, outColors, this);
	    setPopupText("receivedExternalMsgCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Received Externally", "");
	    super.refreshGraph();
	}
	else if(cb == receivedExternalBytes){
	    setDataSource("Call Graph", receivedExternalByteOutput, outColors, this);
	    setPopupText("receivedExternalByteCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Received Externally", "");
	    super.refreshGraph();
	}
    }
    
    
    protected void setGraphSpecificData(){
	setXAxis("Time", "");
	setYAxis("Count", "");
    }

    public void showDialog() {
	if (dialog == null) {
	    dialog = new IntervalRangeDialog(this, "select Range");
	}
	else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
	    getDialogData();
	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
		        fillGraphData();
		        return null;
		    }
		    public void finished() {
		        setOutputGraphData();
			Checkbox cb = cbg.getSelectedCheckbox();
			setCheckboxData(cb);
			thisWindow.setVisible(true);
		        thisWindow.repaint();
	            }
	    };
	    worker.start();
	}
    }
    
    public void getDialogData() {
	//super.getDialogData();
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	intervalSize = dialog.getIntervalSize();
	startInterval = (int)dialog.getStartInterval();
	endInterval = (int)dialog.getEndInterval();
	numIntervals = endInterval-startInterval+1;
	processorList = dialog.getValidProcessors();
    }

    public void setDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	dialog.setIntervalSize(intervalSize);
	dialog.setValidProcessors(processorList);
	super.setDialogData();
    }
    
    public void showWindow() {
	// do nothing for now
    }
    
    public void fillGraphData() {
    	// Utilize CallGraph.java in analysis folder
 	CallGraph cg = new CallGraph(startInterval, endInterval, 
	                             intervalSize, processorList);
	cg.GatherData(this);
	
	// Arrays returned are implemented such that first half of
	//   the array shows sent and second half shows received 
	// Size = [numIntervals][2*numEPs]
	double[][] messageArray = cg.getMessageArray();
	double[][] byteArray = cg.getByteArray();
	double[][] externalMessageArray = cg.getExternalMessageArray();
	double[][] externalByteArray = cg.getExternalByteArray();

        sentMsgCount = new double[numIntervals][numEPs];
	sentByteCount = new double[numIntervals][numEPs];
	receivedMsgCount = new double[numIntervals][numEPs];
	receivedByteCount = new double[numIntervals][numEPs];
        //sentExternalMsgCount = new double[numIntervals][numEPs];
        //sentExternalByteCount = new double[numIntervals][numEPs];
	receivedExternalMsgCount = new double[numIntervals][numEPs];
	receivedExternalByteCount = new double[numIntervals][numEPs];
	for (int interval=0; interval<numIntervals; interval++) {
	    for (int ep=0; ep<numEPs; ep++) {
                sentMsgCount[interval][ep] = messageArray[interval][ep];
		sentByteCount[interval][ep] = byteArray[interval][ep];
		receivedMsgCount[interval][ep] = messageArray[interval][ep+numEPs];
		receivedByteCount[interval][ep] = byteArray[interval][ep+numEPs];
                //sentExternalMsgCount[interval][ep] = externalMessageArray[interval][ep];
		//sentExternalByteCount[interval][ep] = externalByteArray[interval][ep];
		receivedExternalMsgCount[interval][ep] = externalMessageArray[interval][ep+numEPs];
		receivedExternalByteCount[interval][ep] = externalByteArray[interval][ep+numEPs];
	    }
        }
        
	// Set the exists array to accept non-zero entries only
        // Have initial state also display all existing data.
	// Only do this once at the beginning
	if (startFlag) {
	    for (int ep=0; ep<numEPs; ep++) {
	        for (int interval=0; interval<numIntervals; interval++) {
		    if ( (messageArray[interval][ep]>0) || (messageArray[interval][ep+numEPs]>0) ) {
		        existsArray[0][ep] = true;
		        stateArray[0][ep] = true;
		        break;
		    }
	        }
	    } 
	    startFlag = false;
	}
    }

    public void applyDialogColors() {
	setOutputGraphData();
	repaint();
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
	}
	else {
	    // actually create and fill the data and color arrays
	    sentMsgOutput = 
	        new double[numIntervals][outSize];
	    sentByteOutput =
	        new double[numIntervals][outSize];
	    receivedMsgOutput = 
	        new double[numIntervals][outSize];
	    receivedByteOutput =
	        new double[numIntervals][outSize];
/*
	    sentExternalMsgOutput = 
	        new double[numIntervals][outSize];
	    sentExternalByteOutput =
	        new double[numIntervals][outSize];
*/
	    receivedExternalMsgOutput = 
	        new double[numIntervals][outSize];
	    receivedExternalByteOutput =
	        new double[numIntervals][outSize];
	    outColors =
		new Color[outSize];
	    int count=0;
	    for (int ep=0; ep<numEPs; ep++) {
	        if (stateArray[0][ep]) {
	    	    for (int interval=0; interval<numIntervals; interval++) {
			sentMsgOutput[interval][count] = sentMsgCount[interval][ep];
			sentByteOutput[interval][count] = sentByteCount[interval][ep];
			receivedMsgOutput[interval][count] = receivedMsgCount[interval][ep];
			receivedByteOutput[interval][count] = receivedByteCount[interval][ep];
			//sentExternalMsgOutput[interval][count] = sentExternalMsgCount[interval][ep];
			//sentExternalByteOutput[interval][count] = sentExternalByteCount[interval][ep];
			receivedExternalMsgOutput[interval][count] = receivedExternalMsgCount[interval][ep];
			receivedExternalByteOutput[interval][count] = receivedExternalByteCount[interval][ep];
		    }
		    outColors[count++] = colorArray[0][ep];
		}
	    }
	}
    }
    
    public String[] getPopup(int xVal, int yVal) {
	//System.out.println("CommWindow.getPopup()");
	//System.out.println(xVal +", " +yVal);
	if( (xVal < 0) || (yVal <0) || currentArrayName==null)
	    return null;

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
	
	if (currentArrayName.equals("sentMsgCount")) {
	    rString[0] = "Dest. EPid: " + epName;
	    rString[1] = "Count = " + sentMsgOutput[xVal][yVal];    	
	}
	else if(currentArrayName.equals("sentByteCount")) {
	    rString[0] = "Dest. EPid: " + epName;	    
	    rString[1] = "Bytes = " + sentByteOutput[xVal][yVal];
	}
	else if(currentArrayName.equals("receivedMsgCount")) {
	    rString[0] = "Dest. EPid: " + epName;	    
	    rString[1] = "Count = " + receivedMsgOutput[xVal][yVal];
	}
	else if(currentArrayName.equals("receivedByteCount")) {
	    rString[0] = "Dest. EPid: " + epName;	    
	    rString[1] = "Bytes = " + receivedByteOutput[xVal][yVal];
	}
/*
	else if (currentArrayName.equals("sentExternalMsgCount")) {
	    rString[0] = "Chare: " + epClassName;
	    rString[1] = "Destination EP: " + epName;
	    rString[2] = "Messages Sent Externally: " + sentExternalMsgOutput[xVal][yVal];    	
	}
	else if(currentArrayName.equals("sentExternalByteCount")) {
	    rString[0] = "Chare: " + epClassName;
	    rString[1] = "Destination EP: " + epName;	    
	    rString[2] = "Bytes Sent Externally: " + sentExternalByteOutput[xVal][yVal];
	}
*/
	else if(currentArrayName.equals("receivedExternalMsgCount")) {
	    rString[0] = "Dest. EPid: " + epName;	    
	    rString[1] = "Count = " + receivedExternalMsgOutput[xVal][yVal];
	}
	else if(currentArrayName.equals("receivedExternalByteCount")) {
	    rString[0] = "Dest. EPid: " + epName;	    
	    rString[1] = "Bytes = " + receivedExternalByteOutput[xVal][yVal];
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
		setOutputGraphData();
		Checkbox cb = cbg.getSelectedCheckbox();
		setCheckboxData(cb);
	    } 
	    else if (b == setRanges) {
		showDialog();
	    }
	    else if (b == saveColors) {
		// save all entry point colors to disk
		try {
		    ColorSaver.save(colorArray[0]);
		}
		catch (IOException exception) {
		    System.err.println("Failed to save colors!!");
		}
	    }
	    else if (b == loadColors) {
		// load all entry point colors from disk
		try {
		    colorArray[0] = ColorSaver.loadColors();
		    // silly inefficiency
		    setOutputGraphData();
		} 
		catch (IOException exception) {
		    System.err.println("Failed to load colors!!");
		}
	    }
	}
    }
    
    public void repaint() {
	super.refreshGraph();
    }
    
    private void setPopupText(String input){
	currentArrayName = input;
    }
}
