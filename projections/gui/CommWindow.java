package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import javax.swing.*;

import projections.analysis.*;
import projections.gui.graph.*;
import projections.misc.LogEntryData;

public class CommWindow extends GenericGraphWindow 
    implements ItemListener
{
    private double[][] 	msgCount;		// rename to sentMsgCount
    private double[][] 	byteCount;		// rename to sentBytCount
    private double[][] 	recivedMsgCount;		
    private double[][]	exclusiveSent;
    private ArrayList	histogram;
    private int[]	histArray;
    private String 	currentArrayName;
    private String[][]	popupText;
    private String[][]	EPNames;
    private JPanel	mainPanel;
    private JPanel	graphPanel;
    private JPanel	checkBoxPanel;
    private Checkbox    sentMssgs;
    private Checkbox	sentBytes;
    private Checkbox    histogramCB;
    private Checkbox	recivedMssgs;
    private Checkbox	sentExclusive;

    private CommWindow  thisWindow;

    public CommWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections Communications", mainWindow, myWindowID);
	setGraphSpecificData();
	mainPanel = new JPanel();
    	getContentPane().add(mainPanel);
	createLayout();
	setPopupText("histArray");
	pack();
	thisWindow = this;
	showDialog();
    }
    
    public void repaint() {
	super.refreshGraph();
    }
	
    public void itemStateChanged(ItemEvent ae){
	if(ae.getSource() instanceof Checkbox){
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));			
	    Checkbox cb = (Checkbox)ae.getSource();
	    if(cb == histogramCB) {
		setDataSource("Histogram", histArray, this);
		setPopupText("histArray");
		setYAxis("Frequency", null);
		setXAxis("Byte Size", "bytes");
		super.refreshGraph();
	    } else if(cb == sentMssgs) {
		setDataSource("Communications", msgCount, this);
		setPopupText("msgCount");
		setYAxis("Messages Sent", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == sentBytes){
		//System.out.println("bytes");
		setDataSource("Communications", byteCount, this);
		setPopupText("byteCount");
		setYAxis("Bytes Sent", "bytes");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == recivedMssgs){
		setDataSource("Communications", recivedMsgCount, this);
		setPopupText("recivedMsgCount");
		setYAxis("Mssages Recived", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == sentExclusive){
		setDataSource("Communications", exclusiveSent, this);
		setPopupText("exclusiveSent");
		setYAxis("Messages Sent Externally", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }
	    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
    }
    
    private void setPopupText(String input){
	currentArrayName = input;
    }
    
    public String[] getPopup(int xVal, int yVal){
	//System.out.println("CommWindow.getPopup()");
	//System.out.println(xVal +", " +yVal);
	if( (xVal < 0) || (yVal <0) || currentArrayName==null)
	    return null;
	
	if(EPNames == null)
	    EPNames = Analysis.getEntryNames();
	
	String[] rString = new String[2];
	
	if (currentArrayName.equals("histArray")) {
	    rString[0] = xVal + " bytes";
	    rString[1] = "Count = " +  histArray[xVal];
	} else if(currentArrayName.equals("msgCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + msgCount[xVal][yVal];
	} else if(currentArrayName.equals("byteCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Bytes = " + byteCount[xVal][yVal];
	} else if(currentArrayName.equals("recivedMsgCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + recivedMsgCount[xVal][yVal];
	} else if(currentArrayName.equals("exclusiveSent")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + exclusiveSent[xVal][yVal];
	}
	return rString;
    }	
    
    protected void createLayout() {
	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();
	
	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);
	
	graphPanel = getMainPanel();
	checkBoxPanel = new JPanel();
	
	CheckboxGroup cbg = new CheckboxGroup();
	histogramCB = new Checkbox("Histogram", cbg, true);
	sentMssgs = new Checkbox("Messages Sent (mssgs)", cbg, false);
	sentBytes = new Checkbox("Messages Sent (bytes)", cbg, false);
	recivedMssgs = new Checkbox("Messages Recived", cbg, false);
	sentExclusive = new Checkbox("Messages Sent Externally", cbg, false);
	
	histogramCB.addItemListener(this);
	sentMssgs.addItemListener(this);
	sentBytes.addItemListener(this);
	recivedMssgs.addItemListener(this);
	sentExclusive.addItemListener(this);
	
	Util.gblAdd(checkBoxPanel, histogramCB, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentMssgs, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentBytes, gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, recivedMssgs, gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentExclusive, gbc, 4,0, 1,1, 1,1);
	
	Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(mainPanel, checkBoxPanel, gbc, 0,2, 1,1, 0,0);
    }
    
    protected void setGraphSpecificData(){
	setXAxis("Byte Size", "");
	setYAxis("Frequency", "");
    }
    
    public void showDialog() {
	if (dialog == null) {
	    dialog = new RangeDialog(this, "select Range");
	} else {
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()){
	    getDialogData();
	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
			msgCount = new double[validPEs.size()][];
			byteCount = new double[validPEs.size()][];
			recivedMsgCount = new double[validPEs.size()][];
			exclusiveSent = new double[validPEs.size()][];
			getData();
			return null;
		    }
		    public void finished() {
			setDataSource("Histogram", histArray, thisWindow);
			thisWindow.setVisible(true);
			thisWindow.repaint();
		    }
		};
	    worker.start();
	}
    }

    public void showWindow() {
	// do nothing for now
    }

    // reuse generic graph window's method
    public void getDialogData() {
	super.getDialogData();
    }

    // reuse generic graph window's method
    public void setDialogData() {
	super.setDialogData();
    }

    public void getData(){
	GenericLogReader glr;
	LogEntryData logdata = new LogEntryData();
	
	OrderedIntList peList = validPEs.copyOf();
	
	int numPe = peList.size();
	int numEPs = Analysis.getNumUserEntries();
	histogram = new ArrayList();
		
	int curPeArrayIndex = 0;
	
	ProgressMonitor progressBar = 
	    new ProgressMonitor(this, "Reading log files",
				"", 0, numPe);
	while (peList.hasMoreElements()) {
	    int pe = peList.nextElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("Reading data for PE " + pe);
		progressBar.setProgress(curPeArrayIndex+1);
		validate();
	    } else {
		progressBar.close();
		return;
	    }
	    glr = new GenericLogReader(Analysis.getLogName(pe), 
				       Analysis.getVersion());
	    try {
		msgCount[curPeArrayIndex] = new double[numEPs];
		byteCount[curPeArrayIndex] = new double[numEPs];
		recivedMsgCount[curPeArrayIndex] = new double[numEPs];
		exclusiveSent[curPeArrayIndex] = new double[numEPs];
		int EPid;
		
		glr.nextEventOnOrAfter(startTime, logdata);
		// we'll just use the EOFException to break us out of 
		// this loop :)
		while (true) { 		
		    glr.nextEvent(logdata);
		    if (logdata.type == ProjDefs.CREATION) {
			EPid = logdata.entry;
			msgCount[curPeArrayIndex][EPid]++;
			byteCount[curPeArrayIndex][EPid] += logdata.msglen;
			histogram.add(new Integer(logdata.msglen));
		    } else if (logdata.type == ProjDefs.BEGIN_PROCESSING) {
			EPid = logdata.entry;
			recivedMsgCount[curPeArrayIndex][EPid]++;
			if (logdata.pe == pe) {
			    exclusiveSent[curPeArrayIndex][EPid] ++;
			}
		    }
		}
	    } catch (java.io.EOFException e) {
		// used to break out of inner while(true) loop
	    } catch (java.io.IOException e) {
                System.out.println("Exception: " +e);
                e.printStackTrace();
	    }
	    curPeArrayIndex++;
	}
	progressBar.close();

	// **CW** Highly inefficient ... needs to be re-written as a 
	// bin-based solution instead.
	int max = ((Integer)histogram.get(0)).intValue();
	int min = ((Integer)histogram.get(0)).intValue();
	
	for(int k=1; k<histogram.size(); k++){
	    if(((Integer)histogram.get(k)).intValue() < min)
		min = ((Integer)histogram.get(k)).intValue();
	    if(((Integer)histogram.get(k)).intValue() > max)
		max = ((Integer)histogram.get(k)).intValue();
	}
	
	histArray = new int[max+1];
	for(int k=0; k<Array.getLength(histArray); k++){
	    histArray[k] = 0;
	}
	
	int index;
	for(int k=0; k<histogram.size(); k++){
	    index = ((Integer)histogram.get(k)).intValue();
	    histArray[index] += 1;
	}
	
	for(int k=0; k<numPe; k++){
	    for(int j=0; j<numEPs; j++){
		exclusiveSent[k][j] = msgCount[k][j] - exclusiveSent[k][j];
		
		// Apurva - i'm doing this to prevent any negitive numbers 
		// from getting sent into the stack array because it messes up
		// the drawing				
		if (exclusiveSent[k][j] < 0)
		    exclusiveSent[k][j] = 0;
	    }
	}
    }
}

