package projections.Tools.CommunicationPerPE;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import projections.analysis.ThreadManager;
import projections.gui.Analysis;
import projections.gui.Clickable;
import projections.gui.GenericGraphWindow;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.RangeDialog;
import projections.gui.Util;

public class CommWindow extends GenericGraphWindow
implements ItemListener, ActionListener, Clickable
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	private double[][] 	sentMsgCount;
	private double[][] 	sentByteCount;
	private double[][] 	receivedMsgCount;
	private double[][] 	receivedByteCount;
	private double[][]	exclusiveRecv;
	private double[][]	exclusiveBytesRecv;
	private int[][]     hopCount;
	private double[][]  avgHopCount;
	private double[][]  avgPeHopCount;

	private ArrayList<Integer>	histogram;
	private int[]	histArray;
	private String 	currentArrayName;

	private JPanel	mainPanel;
	private JPanel	graphPanel;
	private JPanel	checkBoxPanel;
	private JPanel      blueGenePanel;

	private Checkbox    sentMsgs;
	private Checkbox	sentBytes;
	private Checkbox	receivedMsgs;
	private Checkbox    receivedBytes;
	private Checkbox	recvExclusive;
	private Checkbox	recvExclusiveBytes;
	private Checkbox    hopCountCB;
	private Checkbox    peHopCountCB;

	private CommWindow  thisWindow;

	private OrderedIntList peList;


	public CommWindow(MainWindow mainWindow) {
		super("Projections Communication - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		mainPanel = new JPanel();
		setLayout(mainPanel);
		//getContentPane().add(mainPanel);
		createMenus();
		createLayout();
		// setPopupText("histArray");
		pack();
		thisWindow = this;
		showDialog();
	}

	public void repaint() {
		super.refreshGraph();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Select Processors")) {
				showDialog();
			}
		}
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

		Analysis a = MainWindow.runObject[myRun];

		String[] rString = new String[4];

		rString[0] = "Processor " + xVal;

		if(currentArrayName.equals("sentMsgCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Count = " + sentMsgCount[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("sentByteCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Bytes = " + sentByteCount[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("receivedMsgCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Count = " + receivedMsgCount[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("receivedByteCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Count = " + receivedByteCount[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("exclusiveRecv")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Count = " + exclusiveRecv[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("exclusiveBytesRecv")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Bytes = " + exclusiveBytesRecv[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if (currentArrayName.equals("avgHopCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = "Count = " + avgHopCount[xVal][yVal];
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if (currentArrayName.equals("avgPeHopCount")) {
			rString[1] = "Count = " + avgPeHopCount[xVal][yVal];
			rString[2] = "Processor = " + xAxis.getIndexName(xVal);
			rString[3] = "";
		}
		return rString;
	}

	public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
		parentWindow.addProcessor(xVal);	
	}

	public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
	}

	protected void createMenus(){
		super.createMenus();
//	
//		menuBar.add(Util.makeJMenu("Tools", new Object[]
//		                                            {
//				"Change Colors",
//		                                            },
//		                                            this));
	}

	private void createLayout() {
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
			dialog = new RangeDialog(this, "select Range", null, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()){
			
			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					peList = dialog.getSelectedProcessors().copyOf();
					getData(dialog.getStartTime(), dialog.getEndTime(), dialog.getSelectedProcessors());
					return null;
				}
				public void done() {
					setDataSource("Communications", sentMsgCount, thisWindow);
					setPopupText("sentMsgCount");
					setYAxis("Messages Sent", "");
					setXAxis("Processor", peList);
					thisWindow.setVisible(true);
					thisWindow.repaint();
				}
			};
			worker.execute();
		}
	}


	private void getData(long startTime, long endTime, OrderedIntList pes){
		sentMsgCount = new double[pes.size()][];
		sentByteCount = new double[pes.size()][];
		receivedMsgCount = new double[pes.size()][];
		receivedByteCount = new double[pes.size()][];
		exclusiveRecv = new double[pes.size()][];
		exclusiveBytesRecv = new double[pes.size()][];
		if (MainWindow.BLUEGENE) {
			hopCount = new int[pes.size()][];
		} else {
			hopCount = null;
		}

		histogram = new ArrayList<Integer>();
		
		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();
		int pIdx = 0;
		OrderedIntList processorList = pes.copyOf();
		while (processorList.hasMoreElements()) {
			int nextPe = processorList.nextElement();
			readyReaders.add( new ThreadedFileReader(nextPe, pIdx, startTime, endTime, sentMsgCount, sentByteCount, receivedMsgCount, receivedByteCount, exclusiveRecv, exclusiveBytesRecv, hopCount ) );
			pIdx++;
		}
		
		// Determine a component to show the progress bar with
		Component guiRootForProgressBar = null;
		if(thisWindow!=null && thisWindow.isVisible()) {
			guiRootForProgressBar = thisWindow;
		} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
			guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
		}

		// Pass this list of threads to a class that manages/runs the threads nicely
		ThreadManager threadManager = new ThreadManager("Loading Communication Data in Parallel", readyReaders, guiRootForProgressBar, true);
		threadManager.runThreads();
		
		
		// Combine histograms from all processors
		
		Iterator<Runnable> iter = readyReaders.iterator();
		while(iter.hasNext()){
			ThreadedFileReader t = (ThreadedFileReader) iter.next();
			histogram.addAll(t.localHistogram);
		}
		
		// Do some post processing
		
		// **CW** Highly inefficient ... needs to be re-written as a
		// bin-based solution instead.

		int max;
		int min;
		if(histogram.size()>0){
			max = histogram.get(0).intValue();
			min = histogram.get(0).intValue();
		} else {
			min = 0;
			max = 0;
		}

		for(int k=1; k<histogram.size(); k++){
			if((histogram.get(k)).intValue() < min)
				min = (histogram.get(k)).intValue();
			if((histogram.get(k)).intValue() > max)
				max = (histogram.get(k)).intValue();
		}

		histArray = new int[max+1];
		for(int k=0; k<Array.getLength(histArray); k++){
			histArray[k] = 0;
		}

		int index;
		for(int k=0; k<histogram.size(); k++){
			index = (histogram.get(k)).intValue();
			//System.out.println("index = "+ index);
			histArray[index] += 1;
		}
	}

	// NOTE: This is a torus-based Manhattan distance computation!
	static int manhattanDistance(int destPe, int srcPe) {
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
		// Sanity Check
		if (distance < 0) {
			System.err.println("Internal Error: Negative Manhatten " +
					"distance " +
					distance + " Destination PE [" + destPe +
					"], Source PE [" + srcPe +"]");
			System.err.println("["+destTriple[0]+"]["+destTriple[1]+"]["+
					destTriple[2]+"] by ["+srcTriple[0]+"]["+
					srcTriple[1]+"]["+srcTriple[2]+"] on a ["+
					MainWindow.BLUEGENE_SIZE[0]+"]["+
					MainWindow.BLUEGENE_SIZE[1]+"]["+
					MainWindow.BLUEGENE_SIZE[2]+"] BG Torus");
			System.exit(-1);
		}
		return distance;
	}

	private static int[] peToTriple(int pe) {
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
		returnTriple[2]*MainWindow.BLUEGENE_SIZE[1];
		// x - fastest changer
		// this is an alternative to pe - y*Nx - z*Nx*Ny
		returnTriple[0] = pe%MainWindow.BLUEGENE_SIZE[0];

		if (returnTriple[0] < 0 || returnTriple[1] < 0 
				|| returnTriple[2] < 0) {
			System.err.println("Internal Error: Triple [" + returnTriple[0] +
					"][" + returnTriple[1] + "][" +
					returnTriple[2] + "]");
			System.exit(-1);
		}

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

	private static int[] peToTripleA(int pe) {
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

	private static int manhattenDistanceA(int srcPe, int destPe) {
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

