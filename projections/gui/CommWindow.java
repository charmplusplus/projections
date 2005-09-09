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
    private double[][]	exclusiveRecv;
    private double[][]	exclusiveBytesRecv;
    private int[][]     hopCount;
    private double[][]  avgHopCount;
    private double[][]  avgPeHopCount;

    private ArrayList	histogram;
    private int[]	histArray;
    private String 	currentArrayName;
    private String[][]	popupText;
    private String[][]	EPNames;

    private JPanel	mainPanel;
    private JPanel	graphPanel;
    private JPanel	checkBoxPanel;
    private JPanel      blueGenePanel;

    private Checkbox    sentMsgs;
    private Checkbox	sentBytes;
    private Checkbox    histogramCB;
    private Checkbox	receivedMsgs;
    private Checkbox    receivedBytes;
    private Checkbox	recvExclusive;
    private Checkbox	recvExclusiveBytes;
    private Checkbox    hopCountCB;
    private Checkbox    peHopCountCB;

    private CommWindow  thisWindow;

    private OrderedIntList peList;

    void windowInit() {
	// parameter initialization is completely handled by GenericGraphWindow
	super.windowInit();
    }

    public CommWindow(MainWindow mainWindow, Integer myWindowID) {
	super("Projections Communications", mainWindow, myWindowID);
	mainPanel = new JPanel();
	setLayout(mainPanel);
	//getContentPane().add(mainPanel);
	createLayout();
	// setPopupText("histArray");
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
	    if (cb == sentMsgs) {
		setDataSource("Total #Msgs Sent", sentMsgCount, this);
		setPopupText("sentMsgCount");
		setYAxis("Messages Sent", "");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    }else if(cb == sentBytes){
		//System.out.println("bytes");
		setDataSource("Total Bytes Sent", sentByteCount, this);
		setPopupText("sentByteCount");
		setYAxis("Bytes Sent", "bytes");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    }else if(cb == receivedMsgs){
		setDataSource("Total #Msgs Received", receivedMsgCount, this);
		setPopupText("receivedMsgCount");
		setYAxis("Messages Received", "");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    }else if(cb == receivedBytes){
		setDataSource("Total Bytes Received", 
			      receivedByteCount, this);
		setPopupText("receivedByteCount");
		setYAxis("Bytes Received", "");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    }else if(cb == recvExclusive){
		setDataSource("#Msgs Received Externally", 
			      exclusiveRecv, this);
		setPopupText("exclusiveRecv");
		setYAxis("Messages Received Externally", "");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    } else if(cb == recvExclusiveBytes){
		setDataSource("Bytes Received Externally", 
			      exclusiveBytesRecv, this);
		setPopupText("exclusiveBytesRecv");
		setYAxis("Bytes Received Externally", "");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    }else if (cb == hopCountCB) {
		if (avgHopCount == null) {
		    avgHopCount = averageHops(hopCount, receivedMsgCount);
		}
		setDataSource("Average Hop Counts by Entry Point", 
			      avgHopCount, this);
		setPopupText("avgHopCount");
		setYAxis("Average Message Hop Counts", "");
		setXAxis("Processor", peList);
		super.refreshGraph();
	    }else if (cb == peHopCountCB) {
		if (avgPeHopCount == null) {
		    avgPeHopCount = averagePEHops(hopCount, receivedMsgCount);
		}
		setDataSource("Average Hop Counts by Processor", 
			      avgPeHopCount, this);
		setPopupText("avgPeHopCount");
		setYAxis("Average Message Hop Counts (by PE)", "");
		setXAxis("Processor", peList);
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

	String[] rString = new String[3];

	if(currentArrayName.equals("sentMsgCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + sentMsgCount[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if(currentArrayName.equals("sentByteCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Bytes = " + sentByteCount[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if(currentArrayName.equals("receivedMsgCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + receivedMsgCount[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if(currentArrayName.equals("receivedByteCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + receivedByteCount[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if(currentArrayName.equals("exclusiveRecv")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + exclusiveRecv[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if(currentArrayName.equals("exclusiveBytesRecv")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Bytes = " + exclusiveBytesRecv[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if (currentArrayName.equals("avgHopCount")) {
	    rString[0] = "EPid: " + EPNames[yVal][0];
	    rString[1] = "Count = " + avgHopCount[xVal][yVal];
	    rString[2] = "Processor = " + xAxis.getIndexName(xVal);
	} else if (currentArrayName.equals("avgPeHopCount")) {
	    rString[0] = "Count = " + avgPeHopCount[xVal][yVal];
	    rString[1] = "Processor = " + xAxis.getIndexName(xVal);
	    rString[2] = "";
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
	if (MainWindow.BLUEGENE) {
	    blueGenePanel = new JPanel();
	}

	CheckboxGroup cbg = new CheckboxGroup();
	//	histogramCB = new Checkbox("Histogram", cbg, true);
	sentMsgs = new Checkbox("Msgs Sent To", cbg, true);
	sentBytes = new Checkbox("Bytes Sent To", cbg, false);
	receivedMsgs = new Checkbox("Msgs Recv By", cbg, false);
	receivedBytes = new Checkbox("Bytes Recv By", cbg, false);
	recvExclusive = new Checkbox("External Msgs Recv By", cbg, 
				     false);
	recvExclusiveBytes = new Checkbox("External Bytes Recv By", cbg, 
					  false);
	
	if (MainWindow.BLUEGENE) {
	    hopCountCB = new Checkbox("Avg Hop Count (EP)", cbg, false);
	    peHopCountCB = new Checkbox("Avg Hop Count (procs)", cbg, false);
	}

	//	histogramCB.addItemListener(this);
	sentMsgs.addItemListener(this);
	sentBytes.addItemListener(this);
	receivedMsgs.addItemListener(this);
	receivedBytes.addItemListener(this);
	recvExclusive.addItemListener(this);
	recvExclusiveBytes.addItemListener(this);
	if (MainWindow.BLUEGENE) {
	    hopCountCB.addItemListener(this);
	    peHopCountCB.addItemListener(this);
	}

	//	Util.gblAdd(checkBoxPanel, histogramCB, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, recvExclusive, gbc, 4,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, recvExclusiveBytes, gbc, 5,0, 1,1, 1,1);

	if (MainWindow.BLUEGENE) {
	    Util.gblAdd(blueGenePanel, hopCountCB, gbc, 0,0, 1,1, 1,1);
	    Util.gblAdd(blueGenePanel, peHopCountCB, gbc, 1,0, 1,1, 1,1);
	}

	Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(mainPanel, checkBoxPanel, gbc, 0,2, 1,1, 0,0);
	
	if (MainWindow.BLUEGENE) {
	    Util.gblAdd(mainPanel, blueGenePanel, gbc, 0,3, 1,1, 0,0);
	}
    }

    public void setGraphSpecificData() {
	// do nothing. **CW** Reconsider such an interface requirement
	// for GenericGraphWindow.
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
			exclusiveRecv = new double[validPEs.size()][];
			exclusiveBytesRecv = new double[validPEs.size()][];
			if (MainWindow.BLUEGENE) {
			    hopCount = new int[validPEs.size()][];
			} else {
			    hopCount = null;
			}
			getData();
			return null;
		    }
		    public void finished() {
			// setDataSource("Histogram", histArray, thisWindow);
			setDataSource("Communications", sentMsgCount, 
				      thisWindow);
			setPopupText("sentMsgCount");
			setYAxis("Messages Sent", "");
			setXAxis("Processor", peList);
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

	peList = validPEs.copyOf();

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
		progressBar.setNote("[PE: " + pe + " ] Reading data.");
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
		exclusiveRecv[curPeArrayIndex] = new double[numEPs];
		exclusiveBytesRecv[curPeArrayIndex] = new double[numEPs];
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
			sentByteCount[curPeArrayIndex][EPid] += 
			    logdata.msglen;
			histogram.add(new Integer(logdata.msglen));
		    } else if (logdata.type == ProjDefs.BEGIN_PROCESSING) {
			EPid = logdata.entry;
			receivedMsgCount[curPeArrayIndex][EPid]++;
			receivedByteCount[curPeArrayIndex][EPid] += 
			    logdata.msglen;
			// testing if the send was from outside the processor
			if (logdata.pe != pe) {
			    exclusiveRecv[curPeArrayIndex][EPid]++;
			    exclusiveBytesRecv[curPeArrayIndex][EPid] += 
				logdata.msglen;
			    if (MainWindow.BLUEGENE) {
				hopCount[curPeArrayIndex][EPid] +=
				    manhattenDistance(pe,logdata.pe);
			    }
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
    }

    // NOTE: This is a torus-based manhatten distance computation!
    private int manhattenDistance(int destPe, int srcPe) {
	int distance = 0;
	int dimDistance = 0;

	int destTriple[] = peToTriple(destPe);
	int srcTriple[] = peToTriple(srcPe);

	for (int dim=0; dim<3; dim++) {
	    if (destTriple[dim] < srcTriple[dim]) {
		dimDistance = srcTriple[dim] - destTriple[dim];
	    } else {
		dimDistance = destTriple[dim] - srcTriple[dim];
	    }
	    // apply torus factor
	    if (dimDistance > MainWindow.BLUEGENE_SIZE[dim]/2.0) {
		dimDistance = MainWindow.BLUEGENE_SIZE[dim] - dimDistance;
	    }
	    distance += dimDistance;
	}

	return distance;
    }

    private int[] peToTriple(int pe) {
	int returnTriple[] = new int[3];

	// **CW** Crisis of academic faith "Help! I can't do math anymore!"
	// pe = x + y*Nx + z*Nx*Ny

	// z - slowest changer
	// = pe/Nx*Ny
	returnTriple[2] = pe/(MainWindow.BLUEGENE_SIZE[1]*
			      MainWindow.BLUEGENE_SIZE[0]);
	// y
	// = pe/Nx - z*Ny
	returnTriple[1] = pe/MainWindow.BLUEGENE_SIZE[0] -
	    returnTriple[2]*MainWindow.BLUEGENE_SIZE[0];
	// x - fastest changer
	// this is an alternative to pe - y*Nx - z*Nx*Ny
	returnTriple[0] = pe%MainWindow.BLUEGENE_SIZE[0];

	return returnTriple;
    }

    private double[][] averageHops(int hopArray[][], 
				   double msgReceived[][]) {
	double returnValue[][] = 
	    new double[hopArray.length][hopArray[0].length];

	for (int i=0; i<hopArray.length; i++) {
	    for (int j=0; j<hopArray[i].length; j++) {
		if (msgReceived[i][j] > 0) {
		    returnValue[i][j] = 
			hopArray[i][j]/msgReceived[i][j];
		} else {
		    returnValue[i][j] = 0.0;
		}
	    }
	}
	return returnValue;
    }

    private double[][] averagePEHops(int hopArray[][],
				     double msgReceived[][]) {
	double returnValue[][] =
	    new double[hopArray.length][1];
	double totalRecv[] = new double[hopArray.length];

	for (int pe=0; pe<hopArray.length; pe++) {
	    for (int ep=0; ep<hopArray[pe].length; ep++) {
		totalRecv[pe] += msgReceived[pe][ep];
		returnValue[pe][0] += hopArray[pe][ep];
	    }
	    if (totalRecv[pe] > 0) {
		returnValue[pe][0] /= totalRecv[pe];
	    }
	}
	return returnValue;
    }

    public static int[] peToTripleA(int pe) {
        int returnTriple[] = new int[3];
	int BG[] = {8, 8, 16};

	System.out.println("BG triplet = " + BG[0] + " " + BG[1] + " " +
			   BG[2]);

        // **CW** Crisis of academic faith "Help! I can't do math anymore!"
        // pe = x + y*Nx + z*Nx*Ny

        // z - slowest changer
        // = pe/Nx*Ny
        returnTriple[2] = pe/(BG[0]*BG[1]);

        // y
        // = pe/Nx - z*Ny
        returnTriple[1] = pe/BG[0] -
            returnTriple[2]*BG[0];
        // x - fastest changer
        // this is an alternative to pe - y*Nx - z*Nx*Ny
        returnTriple[0] = pe%BG[0];

        return returnTriple;
    }

    public static int manhattenDistanceA(int srcPe, int destPe) {
        int distance = 0;
        int dimDistance = 0;
	int BG[] = {8, 8, 16};

        int destTriple[] = peToTripleA(destPe);
        int srcTriple[] = peToTripleA(srcPe);

	System.out.println("Source triple = " + srcTriple[0] +
			   " " + srcTriple[1] + " " + srcTriple[2]);
	System.out.println("Dest triple = " + destTriple[0] +
			   " " + destTriple[1] + " " + destTriple[2]);

        for (int dim=0; dim<3; dim++) {
            if (destTriple[dim] < srcTriple[dim]) {
                dimDistance = srcTriple[dim] - destTriple[dim];
            } else {
                dimDistance = destTriple[dim] - srcTriple[dim];
            }
            // apply torus factor
            if (dimDistance > BG[dim]/2.0) {
                dimDistance =  BG[dim] - dimDistance;
            }
            distance += dimDistance;
        }

        return distance;
    }

    public static void main(String args[]) {
	int distance;

	// (2,7,15) to (7,7,15) wrap-around expected
	// result should be 3
	distance = manhattenDistanceA(1018, 1023);
	System.out.println(distance);

	// (2,7,15) to (4,7,15) no wrap-around
	// result should be 2
	distance = manhattenDistanceA(1018, 1020);
	System.out.println(distance);

	// (2,7,15) to (4,1,15) no wrap-around on x, wrap around on y
	// result should be 2+2 = 4
	distance = manhattenDistanceA(1018, 972);
	System.out.println(distance);

	// (2,7,15) to (4,4,15) no wrap-around on x, no wrap around on y
	// result should be 2+3 = 5
	distance = manhattenDistanceA(1018, 996);
	System.out.println(distance);

	// (2,7,6) to (7,2,10) wrap on x, wrap on y, no wrap on z
	// result should be 3+3+4 = 10
	distance = manhattenDistanceA(442, 663);
	System.out.println(distance);

	// (2,7,15) to (4,0,5) no wrap on x, wrap on y, wrap on z
	// result should be 2+1+6 = 9
	distance = manhattenDistanceA(1018, 324);
	System.out.println(distance);
    }
}

