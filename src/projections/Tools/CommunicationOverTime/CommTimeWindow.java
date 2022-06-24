package projections.Tools.CommunicationOverTime;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.*;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.GenericGraphColorer;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;


public class CommTimeWindow extends GenericGraphWindow
implements ActionListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	// Sent External code commented out and may be implemented later

	private CommTimeWindow      thisWindow;    

	//    private EntrySelectionDialog entryDialog;

	private JPanel	   mainPanel;
	private IntervalChooserPanel intervalPanel;

	private JPanel	   graphPanel;
	private JPanel viewSelectPanel;
	private JPanel         controlPanel;

	private JButton	   setRanges;
	private JLabel totalCount;
	//    private JButton	   epSelection;

	private ButtonGroup btg;
	private JRadioButton sentMsgs;
	private JRadioButton sentBytes;
	private JRadioButton receivedMsgs;
	private JRadioButton receivedBytes;
	//private JRadioButton sentExternalMsgs;
	//private JRadioButton sentExternalBytes;
	private JRadioButton receivedExternalMsgs;
	private JRadioButton receivedExternalBytes;

    private JRadioButton receivedExternalNodeMsgs;
	private JRadioButton receivedExternalNodeBytes;

	private int		   startInterval;
	private int		   endInterval;
	private int		   numIntervals;
	private int		   numEPs;
	private long	   intervalSize;
	private SortedSet<Integer> processorList;

	private String	   currentArrayName;

	// data required for entry selection dialog
	//    private String         typeLabelNames[] = {"Entry Points"};
	private boolean        stateArray[];
//	private boolean        existsArray[];
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
	private double[][]	   receivedExternalNodeMsgCount;
	private double[][]     receivedExternalNodeByteCount;

	// output arrays    
	private double[][]     sentMsgOutput;
	private double[][]     sentByteOutput;
	private double[][]     receivedMsgOutput;
	private double[][]     receivedByteOutput;
	//private double[][]     sentExternalMsgOutput;
	//private double[][]     sentExternalByteOutput;
	private double[][]     receivedExternalMsgOutput;
	private double[][]     receivedExternalByteOutput;

	private double[][]     receivedExternalNodeMsgOutput;
	private double[][]     receivedExternalNodeByteOutput;

	// format for output
	private DecimalFormat  _format;

	private MyColorer commTimeColors;

	public CommTimeWindow(MainWindow mainWindow) {
		super("Projections Communication vs Time Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		setGraphSpecificData();
		// the following data are statically known and can be initialized
		// here
		numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		stateArray = new boolean[numEPs];
//		existsArray = new boolean[numEPs];
		commTimeColors = new MyColorer();
		entryNames = new String[numEPs];
		for (int ep=0; ep<numEPs; ep++) {
			entryNames[ep] = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
		}
		mainPanel = new JPanel();
		_format = new DecimalFormat("###,###.###");
		setLayout(mainPanel);
		//getContentPane().add(mainPanel);
		createMenus();
		createLayout();
		pack();
		thisWindow = this;

		showDialog();
	}

	

	/** A class that provides the colors for the display */
	public class MyColorer implements GenericGraphColorer {
		
		public Paint[] getColorMap() {

			int outSize = 0;
			for (int ep=0; ep<numEPs; ep++) {
				if (stateArray[ep]) {
					outSize++;
				}
			}
			
			Paint[]  outColors = new Paint[outSize];

			int count=0;
			for (int ep=0; ep<numEPs; ep++) {
				if (stateArray[ep]) {
					outColors[count++] = MainWindow.runObject[myRun].getEntryColor(ep);
				}
			}
		
			return outColors;
		}
	}
	
	
	protected void createMenus(){
		super.createMenus();
	}

	private void createLayout() {
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);

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

		btg = new ButtonGroup();
		btg.add(sentMsgs);
		btg.add(sentBytes);
		btg.add(receivedMsgs);
		btg.add(receivedBytes);
		btg.add(receivedExternalMsgs);
		btg.add(receivedExternalBytes);
		btg.add(receivedExternalNodeMsgs);
		btg.add(receivedExternalNodeBytes);

		viewSelectPanel = new JPanel();
		Util.gblAdd(viewSelectPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalMsgs, gbc, 4,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalBytes, gbc, 5,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalNodeMsgs, gbc, 6,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalNodeBytes, gbc, 7,0, 1,1, 1,1);

		// control panel items
		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);

		totalCount = new JLabel();
		//	epSelection = new JButton("Select Entry Points");
		//	epSelection.addActionListener(this);
		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);
		//	Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, setRanges,   gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, totalCount,   gbc, 1,0, 1,1, 0,0);

		graphPanel = getMainPanel();
		Util.gblAdd(mainPanel, graphPanel,     gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, viewSelectPanel,  gbc, 0,2, 1,1, 0,0);
		Util.gblAdd(mainPanel, controlPanel,   gbc, 0,3, 1,0, 0,0);
	}

	public long accumulateArray(double arr[][]) {
		double total = 0;
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				total += arr[i][j];
			}
		}
		// arr is in terms of rates, so multiply summed rate by
		// one interval in seconds to get total
		return Math.round(total * intervalSize / 1000);
	}

	public void changeView(JRadioButton cb) {
		if(cb == sentMsgs) {
			setDataSource("Messages Sent Over Time", sentMsgOutput, 
					commTimeColors, this);
			setPopupText("sentMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Sent/s", "");
			totalCount.setText("Total messages sent: " + accumulateArray(sentMsgOutput));
			super.refreshGraph();
		}
		else if(cb == sentBytes){
			setDataSource("Bytes Sent Over Time", sentByteOutput, 
					commTimeColors, this);
			setPopupText("sentByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Sent/s", "");
			totalCount.setText("Total bytes sent: " + accumulateArray(sentByteOutput));
			super.refreshGraph();
		}
		else if(cb == receivedMsgs){
			setDataSource("Received Messages Over Time", receivedMsgOutput, 
					commTimeColors, this);
			setPopupText("receivedMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received/s", "");
			totalCount.setText("Total messages received: " + accumulateArray(receivedMsgOutput));
			super.refreshGraph();
		}
		else if(cb == receivedBytes){
			setDataSource("Received Bytes Over Time", receivedByteOutput, 
					commTimeColors, this);
			setPopupText("receivedByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Received/s", "");
			totalCount.setText("Total bytes received: " + accumulateArray(receivedByteOutput));
			super.refreshGraph();
		}
		else if(cb == receivedExternalMsgs){
			setDataSource("Received External Messages Over Time", receivedExternalMsgOutput,
					commTimeColors, this);
			setPopupText("receivedExternalMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received Externally/s", "");
			totalCount.setText("Total external messages received: " + accumulateArray(receivedExternalMsgOutput));
			super.refreshGraph();
		}
		else if(cb == receivedExternalBytes){
			setDataSource("Received External Bytes Over Time", receivedExternalByteOutput,
					commTimeColors, this);
			setPopupText("receivedExternalByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Received Externally/s", "");
			totalCount.setText("Total external bytes received: " + accumulateArray(receivedExternalByteOutput));
			super.refreshGraph();
		}
        else if(cb == receivedExternalNodeMsgs){
			setDataSource("Received External Node Messages Over Time", receivedExternalNodeMsgOutput,
					commTimeColors, this);
			setPopupText("receivedExternalNodeMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received Externally Node/s", "");
			totalCount.setText("Total external node messages received: " + accumulateArray(receivedExternalNodeMsgOutput));
			super.refreshGraph();
		}
		else if(cb == receivedExternalNodeBytes){
			setDataSource("Received External Node Bytes Over Time", receivedExternalNodeByteOutput,
					commTimeColors, this);
			setPopupText("receivedExternalNodeByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Received Externally Node/s", "");
			totalCount.setText("Total external node bytes received: " + accumulateArray(receivedExternalNodeByteOutput));
			super.refreshGraph();
		}
	}

	protected void setGraphSpecificData(){
		setXAxis("Time", "");
		setYAxis("Count", "");
	}

	public void showDialog() {
		if (dialog == null) {    
			intervalPanel = new IntervalChooserPanel();
			dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()) {
			intervalSize = intervalPanel.getIntervalSize();
			startInterval = (int)intervalPanel.getStartInterval();
			endInterval = (int)intervalPanel.getEndInterval();
			numIntervals = endInterval-startInterval+1;
			processorList = new TreeSet<Integer>(dialog.getSelectedProcessors());

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					getData();
					return null;
				}
				public void done() {
					setOutputGraphData();
					for (Enumeration<AbstractButton> buttons = btg.getElements(); buttons.hasMoreElements(); ) {
						AbstractButton button = buttons.nextElement();

						if (button.isSelected()) {
							changeView((JRadioButton) button);
						}
					}

					thisWindow.setVisible(true);
					thisWindow.repaint();
				}
			};
			worker.execute();
		}
	}




	private void getData() {

		
		sentMsgCount = new double[numIntervals][numEPs];
		sentByteCount = new double[numIntervals][numEPs];
		receivedMsgCount = new double[numIntervals][numEPs];
		receivedByteCount = new double[numIntervals][numEPs];
		receivedExternalMsgCount = new double[numIntervals][numEPs];
		receivedExternalByteCount = new double[numIntervals][numEPs];
		receivedExternalNodeMsgCount = new double[numIntervals][numEPs];
		receivedExternalNodeByteCount = new double[numIntervals][numEPs];
		
		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();
		int pIdx = 0;
		for (Integer nextPe : processorList){
			//readyReaders.add( new ThreadedFileReader(nextPe, intervalSize, startInterval, endInterval, sentMsgCount, receivedMsgCount, sentByteCount, receivedByteCount, receivedExternalMsgCount, receivedExternalByteCount) );
			readyReaders.add( new ThreadedFileReader(nextPe, intervalSize, startInterval, endInterval, sentMsgCount, receivedMsgCount, sentByteCount, receivedByteCount, receivedExternalMsgCount, receivedExternalByteCount, receivedExternalNodeMsgCount, receivedExternalNodeByteCount) );
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

		// Set the exists array to accept non-zero entries only
		// Have initial state also display all existing data.

		for (int ep=0; ep<numEPs; ep++) {
			stateArray[ep] = true;

			//    		for (int interval=0; interval<numIntervals; interval++) {
			//    			if ( (tempMessageArray[interval][ep]>0) || (tempMessageArray[interval][ep+numEPs]>0) ) {
			//    				existsArray[ep] = true;
			//    				stateArray[ep] = true;
			//    				break;
			//    			}
			//    		}

		}
	}


	private void setOutputGraphData() {
		// need first pass to decide the size of the outputdata
		int outSize = 0;
		for (int ep=0; ep<numEPs; ep++) {
			if (stateArray[ep]) {
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
            receivedExternalNodeMsgOutput = 
				new double[numIntervals][outSize];
			receivedExternalNodeByteOutput =
				new double[numIntervals][outSize];

			for (int ep=0; ep<numEPs; ep++) {
				if (stateArray[ep]) {
					for (int interval=0; interval<numIntervals; interval++) {
						sentMsgOutput[interval][ep] = sentMsgCount[interval][ep];
						sentByteOutput[interval][ep] = sentByteCount[interval][ep];
						receivedMsgOutput[interval][ep] = receivedMsgCount[interval][ep];
						receivedByteOutput[interval][ep] = receivedByteCount[interval][ep];
						//sentExternalMsgOutput[interval][count] = sentExternalMsgCount[interval][ep];
						//sentExternalByteOutput[interval][count] = sentExternalByteCount[interval][ep];
						receivedExternalMsgOutput[interval][ep] = receivedExternalMsgCount[interval][ep];
						receivedExternalByteOutput[interval][ep] = receivedExternalByteCount[interval][ep];
						receivedExternalNodeMsgOutput[interval][ep] = receivedExternalNodeMsgCount[interval][ep];
						receivedExternalNodeByteOutput[interval][ep] = receivedExternalNodeByteCount[interval][ep];
					}
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
		String epClassName = "";
		for (int ep=0; ep<numEPs; ep++) {
			if (stateArray[ep]) {
				if (count++ == yVal) {
					epName = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
					epClassName = MainWindow.runObject[myRun].getEntryChareNameByIndex(ep);
					break;
				}
			}
		}

		String[] rString = new String[4];

		rString[0] = "Time Interval: " +
		U.humanReadableString((xVal+startInterval)*intervalSize) + " to " +
		U.humanReadableString((xVal+startInterval+1)*intervalSize);

		if (currentArrayName.equals("sentMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;
			rString[3] = String.format("Rate = %s messages/s (%s messages)",
				_format.format(sentMsgOutput[xVal][yVal]),
				_format.format(sentMsgOutput[xVal][yVal] * intervalSize / 1000));    	
		}
		else if(currentArrayName.equals("sentByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s B/s (%s bytes)",
				_format.format(sentByteOutput[xVal][yVal]),
				_format.format(sentByteOutput[xVal][yVal] * intervalSize / 1000));
		}
		else if(currentArrayName.equals("receivedMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s messages/s (%s messages)", 
				_format.format(receivedMsgOutput[xVal][yVal]),
				_format.format(receivedMsgOutput[xVal][yVal] * intervalSize / 1000));
		}
		else if(currentArrayName.equals("receivedByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s B/s (%s bytes)",
				_format.format(receivedByteOutput[xVal][yVal]),
				_format.format(receivedByteOutput[xVal][yVal] * intervalSize / 1000));
		}
		/*
	else if (currentArrayName.equals("sentExternalMsgCount")) {
	    rString[1] = "Chare: " + epClassName;
	    rString[2] = "Destination EP: " + epName;
	    rString[3] = "Messages Sent Externally: " + 
	        sentExternalMsgOutput[xVal][yVal];    	
	}
	else if(currentArrayName.equals("sentExternalByteCount")) {
	    rString[1] = "Chare: " + epClassName;
	    rString[2] = "Destination EP: " + epName;	    
	    rString[3] = "Bytes Sent Externally: " + 
                sentExternalByteOutput[xVal][yVal];
	}
		 */
		else if(currentArrayName.equals("receivedExternalMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s messages/s (%s messages)", 
				_format.format(receivedExternalMsgOutput[xVal][yVal]),
				_format.format(receivedExternalMsgOutput[xVal][yVal] * intervalSize / 1000));
		}
		else if(currentArrayName.equals("receivedExternalByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s B/s (%s bytes)",
				_format.format(receivedExternalByteOutput[xVal][yVal]),
				_format.format(receivedExternalByteOutput[xVal][yVal] * intervalSize / 1000));
		}
        else if(currentArrayName.equals("receivedExternalNodeMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s messages/s (%s messages)",
				_format.format(receivedExternalMsgOutput[xVal][yVal]),
				_format.format(receivedExternalNodeMsgOutput[xVal][yVal] * intervalSize / 1000));
		}
		else if(currentArrayName.equals("receivedExternalNodeByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = String.format("Rate = %s B/s (%s bytes)",
				_format.format(receivedExternalNodeByteOutput[xVal][yVal]),
				_format.format(receivedExternalNodeByteOutput[xVal][yVal] * intervalSize / 1000));
		}

		return rString;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			if (b == setRanges) {
				showDialog();
			}
			
		} else if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Set Range")) {
				showDialog();
			}
		}
		else if (e.getSource() instanceof JRadioButton) {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			changeView((JRadioButton)e.getSource());
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void repaint() {
		super.refreshGraph();
	}

	private void setPopupText(String input){
		currentArrayName = input;
	}
}
