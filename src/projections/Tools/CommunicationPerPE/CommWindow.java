package projections.Tools.CommunicationPerPE;

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
import java.util.SortedSet;
import java.util.TreeSet;
import java.text.DecimalFormat;

import javax.swing.*;

import projections.analysis.Analysis;
import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.Clickable;
import projections.gui.GenericGraphWindow;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.Util;

public class CommWindow extends GenericGraphWindow
implements ActionListener, Clickable
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	private double[][] sentMsgCount;
	private double[][] sentByteCount;
	private double[][] receivedMsgCount;
	private double[][] receivedByteCount;
	private double[][] externalRecv;
	private double[][] externalBytesRecv;
	private double[][] externalNodeRecv;
	private double[][] externalNodeBytesRecv;
	private int[][]    hopCount;
	private double[][] avgHopCount;
	private double[][] avgPeHopCount;

	// in microseconds, so 1 = "per us", 1000 = "per ms", 1000000 = "per s"
	// user will be able to change this from the UI
	private double unitTime = 1000.0;
	private double timeInterval;
	private String unitTimeStr = "ms";

	private DecimalFormat _format;

	private ArrayList<Integer> histogram;
	private int[]	histArray;
	private String 	currentArrayName;

	private JPanel	mainPanel;
	private JPanel	graphPanel;
	private JPanel viewSelectPanel;
	private JPanel      blueGenePanel;
	private JPanel	unitPanel;

	private JRadioButton sentMsgs;
	private JRadioButton sentBytes;
	private JRadioButton receivedMsgs;
	private JRadioButton receivedBytes;
	private JRadioButton receivedExternalMsgs;
	private JRadioButton receivedExternalBytes;
	private JRadioButton receivedExternalNodeMsgs;
	private JRadioButton receivedExternalNodeBytes;
	private JRadioButton hopCountBt;
	private JRadioButton peHopCountBt;

	private JRadioButton microseconds;
	private JRadioButton milliseconds;
	private JRadioButton seconds;

	private CommWindow  thisWindow;

	private SortedSet<Integer> peList;


	public CommWindow(MainWindow mainWindow) {
		super("Projections Communication - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		mainPanel = new JPanel();
		_format = new DecimalFormat("###,###.###");
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
			} else if(arg.equals("Set Range")) {
				showDialog();
			}
		} else if (e.getSource() instanceof JRadioButton) {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			handleBtn((JRadioButton)e.getSource());
			super.refreshGraph();
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void handleBtn(JRadioButton bt) {
		if (bt == sentMsgs) {
			setDataSource("Rate of Msgs Sent", sentMsgCount, this);
			setPopupText("sentMsgCount");
			setYAxis("Rate of Messages Sent", "");
			setXAxis("Processor", peList);
		} else if (bt == sentBytes) {
			//System.out.println("bytes");
			setDataSource("Rate of Bytes Sent", sentByteCount, this);
			setPopupText("sentByteCount");
			setYAxis("Rate of Bytes Sent", "bytes");
			setXAxis("Processor", peList);
		} else if (bt == receivedMsgs) {
			setDataSource("Rate of Msgs Received", receivedMsgCount, this);
			setPopupText("receivedMsgCount");
			setYAxis("Rate of Messages Received", "");
			setXAxis("Processor", peList);
		} else if (bt == receivedBytes) {
			setDataSource("Rate of Bytes Received",
					receivedByteCount, this);
			setPopupText("receivedByteCount");
			setYAxis("Rate of Bytes Received", "");
			setXAxis("Processor", peList);
		} else if (bt == receivedExternalMsgs) {
			setDataSource("Rate of External Msgs Received", externalRecv, this);
			setPopupText("externalRecv");
			setYAxis("Rate of External Messages Received", "");
			setXAxis("Processor", peList);
		} else if (bt == receivedExternalBytes) {
			setDataSource("Rate of External Bytes Received", externalBytesRecv, this);
			setPopupText("externalBytesRecv");
			setYAxis("Rate of External Bytes Received", "");
			setXAxis("Processor", peList);
		} else if (bt == receivedExternalNodeMsgs) {
			setDataSource("Rate of Node External Msgs Received", externalNodeRecv, this);
			setPopupText("externalNodeRecv");
			setYAxis("Rate of Node External Messages Received", "");
			setXAxis("Processor", peList);
		} else if (bt == receivedExternalNodeBytes) {
			setDataSource("Rate of Node External Bytes Received", externalNodeBytesRecv, this);
			setPopupText("externalNodeBytesRecv");
			setYAxis("Rate of Node External Bytes Received", "");
			setXAxis("Processor", peList);
		} else if (bt == hopCountBt) {
			if (avgHopCount == null) {
				avgHopCount = averageHops(hopCount, receivedMsgCount);
			}
			setDataSource("Average Hop Counts by Entry Point",
					avgHopCount, this);
			setPopupText("avgHopCount");
			setYAxis("Average Message Hop Counts", "");
			setXAxis("Processor", peList);
		} else if (bt == peHopCountBt) {
			if (avgPeHopCount == null) {
				avgPeHopCount = averagePEHops(hopCount, receivedMsgCount);
			}
			setDataSource("Average Hop Counts by Processor",
					avgPeHopCount, this);
			setPopupText("avgPeHopCount");
			setYAxis("Average Message Hop Counts (by PE)", "");
			setXAxis("Processor", peList);
		} else if (bt == microseconds) {
			scaleHistogramData(1.0);
		} else if (bt == milliseconds) {
			scaleHistogramData(1000.0);
		} else if (bt == seconds) {
			scaleHistogramData(1000000.0);
		}
	}

	private void scaleHistogramData(double newUnit) {
		double scale = newUnit / unitTime;
		for (int pIdx = 0; pIdx < sentMsgCount.length; pIdx++) {
			for (int ep = 0; ep < sentMsgCount[pIdx].length; ep++) {
				sentMsgCount[pIdx][ep] *= scale;
				sentByteCount[pIdx][ep] *= scale;
				receivedMsgCount[pIdx][ep] *= scale;
				receivedByteCount[pIdx][ep] *= scale;
				externalRecv[pIdx][ep] *= scale;
				externalBytesRecv[pIdx][ep] *= scale;
				externalNodeRecv[pIdx][ep] *= scale;
				externalNodeBytesRecv[pIdx][ep] *= scale;
			}
		}
		unitTime = newUnit;
        if (unitTime == 1.0) unitTimeStr = "us";
        else if (unitTime == 1000.0) unitTimeStr = "ms";
        else unitTimeStr = "s";
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
			rString[2] = String.format("Rate = %s messages/%s (%s messages)",
			 	_format.format(sentMsgCount[xVal][yVal]),
				unitTimeStr,
			 	_format.format(sentMsgCount[xVal][yVal] * timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("sentByteCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s B/%s (%s bytes)",
				_format.format(sentByteCount[xVal][yVal]),
				unitTimeStr,
				_format.format(sentByteCount[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("receivedMsgCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s messages/%s (%s messages)",
				_format.format(receivedMsgCount[xVal][yVal]),
				unitTimeStr,
				_format.format(receivedMsgCount[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("receivedByteCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s B/%s (%s bytes)",
				_format.format(receivedByteCount[xVal][yVal]),
				unitTimeStr,
				_format.format(receivedByteCount[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("externalRecv")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s messages/%s (%s messages)",
				_format.format(externalRecv[xVal][yVal]),
				unitTimeStr,
				_format.format(externalRecv[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("externalBytesRecv")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s B/%s (%s bytes)",
				_format.format(externalBytesRecv[xVal][yVal]),
				unitTimeStr,
				_format.format(externalBytesRecv[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("externalNodeRecv")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s messages/%s (%s messages)",
				_format.format(externalNodeRecv[xVal][yVal]),
				unitTimeStr,
				_format.format(externalNodeRecv[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if(currentArrayName.equals("externalNodeBytesRecv")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s B/%s (%s bytes)",
				_format.format(externalNodeBytesRecv[xVal][yVal]),
				unitTimeStr,
				_format.format(externalNodeBytesRecv[xVal][yVal]*timeInterval/unitTime));
			rString[3] = "Processor = " + xAxis.getIndexName(xVal);
		} else if (currentArrayName.equals("avgHopCount")) {
			rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
			rString[2] = String.format("Rate = %s hops/%s (%s hops)",
				_format.format(avgHopCount[xVal][yVal]),
				unitTimeStr,
				_format.format(avgHopCount[xVal][yVal]*timeInterval/unitTime));
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
		viewSelectPanel = new JPanel();
		unitPanel = new JPanel();
		if (MainWindow.BLUEGENE) {
			blueGenePanel = new JPanel();
		}

		sentMsgs = new JRadioButton("Msgs Sent");
		sentMsgs.addActionListener(this);
		sentMsgs.setToolTipText("Number of messages sent anywhere, including to same PE");
		sentBytes = new JRadioButton("Bytes Sent");
		sentBytes.addActionListener(this);
		sentBytes.setToolTipText("Number of bytes sent anywhere, including to same PE");
		receivedMsgs = new JRadioButton("Msgs Recv");
		receivedMsgs.addActionListener(this);
		receivedMsgs.setToolTipText("Number of messages received from anywhere, including from same PE");
		receivedBytes = new JRadioButton("Bytes Recv");
		receivedBytes.addActionListener(this);
		receivedBytes.setToolTipText("Number of bytes received from anywhere, including from same PE");
		receivedExternalMsgs = new JRadioButton("External Msgs Recv");
		receivedExternalMsgs.addActionListener(this);
		receivedExternalMsgs.setToolTipText("Number of messages received from a different PE");
		receivedExternalBytes = new JRadioButton("External Bytes Recv");
		receivedExternalBytes.addActionListener(this);
		receivedExternalBytes.setToolTipText("Number of bytes received from a different PE");
		receivedExternalNodeMsgs = new JRadioButton("External Node Msgs Recv", true);
		receivedExternalNodeMsgs.addActionListener(this);
		receivedExternalNodeMsgs.setToolTipText("Number of messages received from a different process");
		receivedExternalNodeBytes = new JRadioButton("External Node Bytes Recv");
		receivedExternalNodeBytes.addActionListener(this);
		receivedExternalNodeBytes.setToolTipText("Number of bytes received from a different process");

		ButtonGroup btg = new ButtonGroup();
		btg.add(sentMsgs);
		btg.add(sentBytes);
		btg.add(receivedMsgs);
		btg.add(receivedBytes);
		btg.add(receivedExternalMsgs);
		btg.add(receivedExternalBytes);
		btg.add(receivedExternalNodeMsgs);
		btg.add(receivedExternalNodeBytes);

		if (MainWindow.BLUEGENE) {
			hopCountBt = new JRadioButton("Avg Hop Count (EP)");
			hopCountBt.addActionListener(this);
			btg.add(hopCountBt);

			peHopCountBt = new JRadioButton("Avg Hop Count (procs)");
			peHopCountBt.addActionListener(this);
			btg.add(peHopCountBt);
		}

		Util.gblAdd(viewSelectPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalMsgs, gbc, 4,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalBytes, gbc, 5,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalNodeMsgs, gbc, 6,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalNodeBytes, gbc, 7,0, 1,1, 1,1);

		if (MainWindow.BLUEGENE) {
			Util.gblAdd(blueGenePanel, hopCountBt, gbc, 0,0, 1,1, 1,1);
			Util.gblAdd(blueGenePanel, peHopCountBt, gbc, 1,0, 1,1, 1,1);
		}

		microseconds = new JRadioButton("Microseconds");
		microseconds.addActionListener(this);
		milliseconds = new JRadioButton("Milliseconds", true);
		milliseconds.addActionListener(this);
		seconds = new JRadioButton("Seconds");
		seconds.addActionListener(this);

		ButtonGroup unit_btg = new ButtonGroup();
		unit_btg.add(microseconds);
		unit_btg.add(milliseconds);
		unit_btg.add(seconds);

		Util.gblAdd(unitPanel, microseconds, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(unitPanel, milliseconds, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(unitPanel, seconds, gbc, 2,0, 1,1, 1,1);

		Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, viewSelectPanel, gbc, 0,2, 1,1, 0,0);

		if (MainWindow.BLUEGENE) {
			Util.gblAdd(mainPanel, blueGenePanel, gbc, 0,3, 1,1, 0,0);
			Util.gblAdd(mainPanel, unitPanel, gbc, 0,4, 1,1, 0,0);
		} else {
			Util.gblAdd(mainPanel, unitPanel, gbc, 0,3, 1,1, 0,0);
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
					peList = new TreeSet<Integer>(dialog.getSelectedProcessors());
					getData(dialog.getStartTime(), dialog.getEndTime(), dialog.getSelectedProcessors());
					return null;
				}
				public void done() {
					milliseconds.setSelected(true);
					handleBtn(milliseconds);
					receivedExternalNodeMsgs.setSelected(true);
					handleBtn(receivedExternalNodeMsgs);
					thisWindow.setVisible(true);
					thisWindow.repaint();
				}
			};
			worker.execute();
		}
	}


	private void getData(long startTime, long endTime, SortedSet<Integer> pes){
		sentMsgCount = new double[pes.size()][];
		sentByteCount = new double[pes.size()][];
		receivedMsgCount = new double[pes.size()][];
		receivedByteCount = new double[pes.size()][];
		externalRecv = new double[pes.size()][];
		externalBytesRecv = new double[pes.size()][];
		externalNodeRecv = new double[pes.size()][];
		externalNodeBytesRecv = new double[pes.size()][];
		if (MainWindow.BLUEGENE) {
			hopCount = new int[pes.size()][];
		} else {
			hopCount = null;
		}

		this.timeInterval = endTime - startTime;

		histogram = new ArrayList<Integer>();
		
		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();
		int pIdx = 0;
    	for(Integer nextPe : pes){
			readyReaders.add(new ThreadedFileReader(nextPe, pIdx, startTime, endTime,
					sentMsgCount, sentByteCount,
					receivedMsgCount, receivedByteCount,
					externalRecv, externalBytesRecv,
					externalNodeRecv, externalNodeBytesRecv,
					hopCount));
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
		TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Communication Data in Parallel", readyReaders, guiRootForProgressBar, true);
		threadManager.runAll();
		
		
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

