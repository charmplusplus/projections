package projections.Tools.CommunicationOverTime;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.GenericGraphColorer;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;


public class CommTimeWindow extends GenericGraphWindow
implements ItemListener, ActionListener
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
	private JPanel	   checkBoxPanel;
	private JPanel         controlPanel;

	private JButton	   setRanges;
	//    private JButton	   epSelection;

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

	// output arrays    
	private double[][]     sentMsgOutput;
	private double[][]     sentByteOutput;
	private double[][]     receivedMsgOutput;
	private double[][]     receivedByteOutput;
	//private double[][]     sentExternalMsgOutput;
	//private double[][]     sentExternalByteOutput;
	private double[][]     receivedExternalMsgOutput;
	private double[][]     receivedExternalByteOutput;

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

		// checkbox panel items
		cbg = new CheckboxGroup();
		sentMsgs = new Checkbox("Msgs Sent", cbg, true);
		sentMsgs.addItemListener(this);
		sentBytes = new Checkbox("Bytes Sent", cbg, false);
		sentBytes.addItemListener(this);
		receivedMsgs = new Checkbox("Msgs Recv", cbg, false);
		receivedMsgs.addItemListener(this);
		receivedBytes = new Checkbox("Bytes Recv", cbg, false);
		receivedBytes.addItemListener(this);
		receivedExternalMsgs = new Checkbox("External Msgs Recv", cbg, false);
		receivedExternalMsgs.addItemListener(this);
		receivedExternalBytes = new Checkbox("External Bytes Recv", cbg, false);
		receivedExternalBytes.addItemListener(this);
		checkBoxPanel = new JPanel();
		Util.gblAdd(checkBoxPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, receivedExternalMsgs, gbc, 4,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, receivedExternalBytes, gbc, 5,0, 1,1, 1,1);

		// control panel items
		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);
		//	epSelection = new JButton("Select Entry Points");
		//	epSelection.addActionListener(this);
		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);
		//	Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, setRanges,   gbc, 0,0, 1,1, 0,0);

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
			setDataSource("Messages Sent Over Time", sentMsgOutput, 
					commTimeColors, this);
			setPopupText("sentMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Sent", "");
			super.refreshGraph();
		}
		else if(cb == sentBytes){
			setDataSource("Bytes Sent Over Time", sentByteOutput, 
					commTimeColors, this);
			setPopupText("sentByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Sent", "");
			super.refreshGraph();
		}
		else if(cb == receivedMsgs){
			setDataSource("Received Messages Over Time", receivedMsgOutput, 
					commTimeColors, this);
			setPopupText("receivedMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received", "");
			super.refreshGraph();
		}
		else if(cb == receivedBytes){
			setDataSource("Received Bytes Over Time", receivedByteOutput, 
					commTimeColors, this);
			setPopupText("receivedByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Received", "");
			super.refreshGraph();
		}
		else if(cb == receivedExternalMsgs){
			setDataSource("Received External Messages Over Time", receivedExternalMsgOutput,
					commTimeColors, this);
			setPopupText("receivedExternalMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received Externally", "");
			super.refreshGraph();
		}
		else if(cb == receivedExternalBytes){
			setDataSource("Received External Bytes Over Time", receivedExternalByteOutput,
					commTimeColors, this);
			setPopupText("receivedExternalByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
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
			intervalPanel = new IntervalChooserPanel();
			dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()) {
			intervalSize = intervalPanel.getIntervalSize();
			startInterval = (int)intervalPanel.getStartInterval();
			endInterval = (int)intervalPanel.getEndInterval();
			numIntervals = endInterval-startInterval+1;
			processorList = dialog.getSelectedProcessors().copyOf();

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					getData();
					return null;
				}
				public void done() {
					setOutputGraphData();
					Checkbox cb = cbg.getSelectedCheckbox();
					setCheckboxData(cb);
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
		
		
		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();
		int pIdx = 0;
		for (Integer nextPe : processorList){
			readyReaders.add( new ThreadedFileReader(nextPe, intervalSize, startInterval, endInterval, sentMsgCount, receivedMsgCount, sentByteCount, receivedByteCount, receivedExternalMsgCount, receivedExternalByteCount) );
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

			int count=0;
			for (int ep=0; ep<numEPs; ep++) {
				if (stateArray[ep]) {
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
			rString[3] = "Count = " + 
			_format.format(sentMsgOutput[xVal][yVal]);    	
		}
		else if(currentArrayName.equals("sentByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = "Bytes = " + 
			_format.format(sentByteOutput[xVal][yVal]);
		}
		else if(currentArrayName.equals("receivedMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = "Count = " + 
			_format.format(receivedMsgOutput[xVal][yVal]);
		}
		else if(currentArrayName.equals("receivedByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = "Bytes = " + 
			_format.format(receivedByteOutput[xVal][yVal]);
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
			rString[3] = "Count = " + 
			_format.format(receivedExternalMsgOutput[xVal][yVal]);
		}
		else if(currentArrayName.equals("receivedExternalByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = "Bytes = " + 
			_format.format(receivedExternalByteOutput[xVal][yVal]);
		}
		return rString;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			//	    if (b == epSelection) {
			//		if (entryDialog == null) {
			//		    entryDialog = 
			//			new EntrySelectionDialog(this,
			//						 typeLabelNames,
			//						 stateArray,colorArray,
			//						 existsArray,entryNames);
			//		}
			//		entryDialog.showDialog();
			//		setOutputGraphData();
			//		Checkbox cb = cbg.getSelectedCheckbox();
			//		setCheckboxData(cb);
			//	    } 

			if (b == setRanges) {
				showDialog();
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

	public void repaint() {
		super.refreshGraph();
	}

	private void setPopupText(String input){
		currentArrayName = input;
	}
}
