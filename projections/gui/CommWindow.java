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
    private double[][] 	sentMsgCount;
    private double[][] 	sentByteCount;
    private double[][] 	receivedMsgCount;
    private double[][] 	receivedByteCount;
    private double[][]	exclusiveSent;
    private int[][]     hopCount;
    private double[][]  avgHopCount;

    private ArrayList	histogram;
    private int[]	histArray;
    private String 	currentArrayName;
    private String[][]	popupText;
    private String[][]	EPNames;

    private JPanel	mainPanel;
    private JPanel	graphPanel;
    private JPanel	checkBoxPanel;

    private Checkbox    sentMsgs;
    private Checkbox	sentBytes;
    private Checkbox    histogramCB;
    private Checkbox	receivedMsgs;
    private Checkbox    receivedBytes;
    private Checkbox	sentExclusive;
    private Checkbox    hopCountCB;

    private CommWindow  thisWindow;

    void windowInit() {
	// parameter initialization is completely handled by GenericGraphWindow
	super.windowInit();
    }

    public CommWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections Communications", mainWindow, myWindowID);
	setGraphSpecificData();
	mainPanel = new JPanel();
	setLayout(mainPanel);
	//getContentPane().add(mainPanel);
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
	    } else if(cb == sentMsgs) {
		setDataSource("Communications", sentMsgCount, this);
		setPopupText("sentMsgCount");
		setYAxis("Messages Sent", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == sentBytes){
		//System.out.println("bytes");
		setDataSource("Communications", sentByteCount, this);
		setPopupText("sentByteCount");
		setYAxis("Bytes Sent", "bytes");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == receivedMsgs){
		setDataSource("Communications", receivedMsgCount, this);
		setPopupText("receivedMsgCount");
		setYAxis("Messages Received", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == receivedBytes){
		setDataSource("Communications", receivedByteCount, this);
		setPopupText("receivedByteCount");
		setYAxis("Bytes Received", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    }else if(cb == sentExclusive){
		setDataSource("Communications", exclusiveSent, this);
		setPopupText("exclusiveSent");
		setYAxis("Messages Sent Externally", "");
		setXAxis("Processor", "");
		super.refreshGraph();
	    } else if (cb == hopCountCB) {
		if (avgHopCount == null) {
		    avgHopCount = averageHops(hopCount, receivedMsgCount);
		}
		setDataSource("Communications", avgHopCount, this);
		setPopupText("avgHopCount");
		setYAxis("Average Message Hop Counts", "");
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
	} else if(currentArrayName.equals("sentMsgCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + sentMsgCount[xVal][yVal];
	} else if(currentArrayName.equals("sentByteCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Bytes = " + sentByteCount[xVal][yVal];
	} else if(currentArrayName.equals("receivedMsgCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + receivedMsgCount[xVal][yVal];
	} else if(currentArrayName.equals("receivedByteCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + receivedByteCount[xVal][yVal];
	} else if(currentArrayName.equals("exclusiveSent")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + exclusiveSent[xVal][yVal];
	} else if (currentArrayName.equals("avgHopCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + avgHopCount[xVal][yVal];
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
	sentMsgs = new Checkbox("Messages Sent", cbg, false);
	sentBytes = new Checkbox("Bytes Sent", cbg, false);
	receivedMsgs = new Checkbox("Messages Received", cbg, false);
	receivedBytes = new Checkbox("Bytes Received", cbg, false);
	sentExclusive = new Checkbox("Messages Sent Externally", cbg, false);
	if (MainWindow.BLUEGENE) {
	    hopCountCB = new Checkbox("Hop Count (BG only)", cbg, false);
	}

	histogramCB.addItemListener(this);
	sentMsgs.addItemListener(this);
	sentBytes.addItemListener(this);
	receivedMsgs.addItemListener(this);
	receivedBytes.addItemListener(this);
	sentExclusive.addItemListener(this);
	if (MainWindow.BLUEGENE) {
	    hopCountCB.addItemListener(this);
	}

	Util.gblAdd(checkBoxPanel, histogramCB, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentMsgs, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentBytes, gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedMsgs, gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedBytes, gbc, 4,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentExclusive, gbc, 5,0, 1,1, 1,1);
	if (MainWindow.BLUEGENE) {
	    Util.gblAdd(checkBoxPanel, hopCountCB, gbc, 6,0, 1,1, 1,1);
	}

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
			sentMsgCount = new double[validPEs.size()][];
			sentByteCount = new double[validPEs.size()][];
			receivedMsgCount = new double[validPEs.size()][];
			receivedByteCount = new double[validPEs.size()][];
			exclusiveSent = new double[validPEs.size()][];
			if (MainWindow.BLUEGENE) {
			    hopCount = new int[validPEs.size()][];
			} else {
			    hopCount = null;
			}
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
		sentMsgCount[curPeArrayIndex] = new double[numEPs];
		sentByteCount[curPeArrayIndex] = new double[numEPs];
		receivedMsgCount[curPeArrayIndex] = new double[numEPs];
		receivedByteCount[curPeArrayIndex] = new double[numEPs];
		exclusiveSent[curPeArrayIndex] = new double[numEPs];
		if (MainWindow.BLUEGENE) {
		    hopCount[curPeArrayIndex] = new int[numEPs];
		}
		int EPid;

		glr.nextEventOnOrAfter(startTime, logdata);
		// we'll just use the EOFException to break us out of
		// this loop :)
		while (true) {
		    if (logdata.time > endTime) {
			if ((logdata.type == ProjDefs.CREATION) ||
			    (logdata.type == ProjDefs.BEGIN_PROCESSING)) {
			    // past endtime. no more to do.
			    break;
			}
		    }
		    glr.nextEvent(logdata);
		    if (logdata.type == ProjDefs.CREATION) {
			EPid = logdata.entry;
			sentMsgCount[curPeArrayIndex][EPid]++;
			sentByteCount[curPeArrayIndex][EPid] += logdata.msglen;
			histogram.add(new Integer(logdata.msglen));
		    } else if (logdata.type == ProjDefs.BEGIN_PROCESSING) {
			EPid = logdata.entry;
			receivedMsgCount[curPeArrayIndex][EPid]++;
			receivedByteCount[curPeArrayIndex][EPid] += logdata.msglen;
			if (logdata.pe == pe) {
			    exclusiveSent[curPeArrayIndex][EPid] ++;
			}
			if (MainWindow.BLUEGENE) {
			    hopCount[curPeArrayIndex][EPid] +=
				manhattenDistance(curPeArrayIndex,
						  logdata.pe);
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
	    //System.out.println("index = "+ index);
	    histArray[index] += 1;
	}

	for(int k=0; k<numPe; k++){
	    for(int j=0; j<numEPs; j++){
		exclusiveSent[k][j] = sentMsgCount[k][j] - exclusiveSent[k][j];

		// Apurva - i'm doing this to prevent any negitive numbers
		// from getting sent into the stack array because it messes up
		// the drawing
		if (exclusiveSent[k][j] < 0)
		    exclusiveSent[k][j] = 0;
	    }
	}
    }

    private int manhattenDistance(int destPe, int srcPe) {
	int distance = 0;

	int destTriple[] = peToTriple(destPe);
	int srcTriple[] = peToTriple(srcPe);

	for (int dim=0; dim<3; dim++) {
	    if (destTriple[dim] < srcTriple[dim]) {
		distance += srcTriple[dim] - destTriple[dim];
	    } else {
		distance += destTriple[dim] - srcTriple[dim];
	    }
	}

	return distance;
    }

    private int[] peToTriple(int pe) {
	int returnTriple[] = new int[3];

	// z - slowest changer
	returnTriple[2] = pe/(MainWindow.BLUEGENE_SIZE[1]*
			      MainWindow.BLUEGENE_SIZE[0]);
	// y 
	returnTriple[1] = pe/MainWindow.BLUEGENE_SIZE[0];
	// x - fastest changer
	returnTriple[0] = pe%MainWindow.BLUEGENE_SIZE[0];

	return returnTriple;
    }

    // stupid hack ... don't really want to do the code for
    // manhatten distance using doubles.
    private double[][] averageHops(int hopArray[][], 
				   double msgReceived[][]) {
	double returnValue[][] = 
	    new double[hopArray.length][hopArray[0].length];

	for (int i=0; i<hopArray.length; i++) {
	    for (int j=0; j<hopArray[i].length; j++) {
		if (msgReceived[i][j] > 0) {
		    returnValue[i][j] = 
			(double)hopArray[i][j] /
			msgReceived[i][j];
		} else {
		    returnValue[i][j] = 0.0;
		}
	    }
	}
	return returnValue;
    }
}

