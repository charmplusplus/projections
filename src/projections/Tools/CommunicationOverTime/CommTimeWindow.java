package projections.Tools.CommunicationOverTime;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.*;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.GenericGraphColorer;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.Legend;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;
import projections.gui.graph.Graph;
import projections.gui.graph.YAxis;


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

	private JRadioButton avgSizeSent;
	private JRadioButton avgSizeReceived;
	private JRadioButton avgSizeExternal;
	private JRadioButton avgSizeExternalNode;

	private JCheckBox showLegendCheckBox;
	private Legend legendWindow;
	private static final int LEGEND_TOP_N = 10;
	// avg-size views show at most this many EPs (largest total bytes first),
	// so the chart and its legend stay 1:1 and readable
	private static final int AVG_SIZE_MAX_LINES = 10;
	// EPs with messages in fewer intervals than this are outliers (e.g. one
	// giant message in a single interval) and are left off the avg-size chart
	private static final int AVG_SIZE_MIN_INTERVALS = 2;

	private ButtonGroup yScaleGroup;
	private JRadioButton totalsButton;
	private JRadioButton rateButton;
	private JRadioButton ratePerPEButton;
	private JPanel yScalePanel;

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

	// avg-size line view state: column k of avgSizeOutput is EP avgSizeEPMap[k];
	// only the top AVG_SIZE_MAX_LINES EPs by total bytes get a column
	private boolean        inAvgSizeMode;
	private double[][]     avgSizeOutput;
	private int[]          avgSizeEPMap;
	private double[][]     avgSizeMsgSource;
	private double[][]     avgSizeByteSource;
	private int            avgSizeOmittedEPs;
	// colors to restore when leaving avg-size mode (which forces a white
	// background so the thin colored lines stay readable)
	private java.awt.Color savedBackground;
	private java.awt.Color savedForeground;

	// format for output
	private DecimalFormat  _format;

	private MyColorer commTimeColors;
	private AvgSizeColorer avgSizeColors;

	public CommTimeWindow(MainWindow mainWindow) {
		super("Projections Communication vs Time Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		setGraphSpecificData();
		// the following data are statically known and can be initialized
		// here
		numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		stateArray = new boolean[numEPs];
//		existsArray = new boolean[numEPs];
		commTimeColors = new MyColorer();
		avgSizeColors = new AvgSizeColorer();
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

	/** Y axis for the avg-size views: the data is log2(bytes+1), so integer
	 *  ticks are powers of two and get labeled with the actual size
	 *  (2, 4, ... 512, 1K, 2K, ... 1M ...). */
	private static class Log2BytesYAxis extends YAxis {
		private final String title;
		private final double max;
		Log2BytesYAxis(String title, double max) {
			this.title = title;
			this.max = max;
		}
		public String getTitle() { return title; }
		public double getMax() { return max; }
		public String getValueName(double value) {
			int v = (int) Math.round(value);
			if (v >= 30) return (1L << (v-30)) + "G";
			if (v >= 20) return (1L << (v-20)) + "M";
			if (v >= 10) return (1L << (v-10)) + "K";
			return String.valueOf(1L << v);
		}
	}

	/** Colors for the avg-size line view, whose columns are the filtered EP set */
	public class AvgSizeColorer implements GenericGraphColorer {

		public Paint[] getColorMap() {
			if (avgSizeEPMap == null) {
				return new Paint[0];
			}
			Paint[] outColors = new Paint[avgSizeEPMap.length];
			for (int k=0; k<avgSizeEPMap.length; k++) {
				outColors[k] = MainWindow.runObject[myRun].getEntryColor(avgSizeEPMap[k]);
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

		avgSizeSent = new JRadioButton("Avg Size Sent");
		avgSizeSent.addActionListener(this);
		avgSizeSent.setToolTipText("Average size (bytes/message) of messages sent, one line per entry method; intervals with no messages plot as 0");
		avgSizeReceived = new JRadioButton("Avg Size Recv");
		avgSizeReceived.addActionListener(this);
		avgSizeReceived.setToolTipText("Average size (bytes/message) of messages received, one line per entry method; intervals with no messages plot as 0");
		avgSizeExternal = new JRadioButton("Avg Size External Recv");
		avgSizeExternal.addActionListener(this);
		avgSizeExternal.setToolTipText("Average size (bytes/message) of messages received from a different PE, one line per entry method");
		avgSizeExternalNode = new JRadioButton("Avg Size External Node Recv");
		avgSizeExternalNode.addActionListener(this);
		avgSizeExternalNode.setToolTipText("Average size (bytes/message) of messages received from a different process, one line per entry method");

		btg = new ButtonGroup();
		btg.add(sentMsgs);
		btg.add(sentBytes);
		btg.add(receivedMsgs);
		btg.add(receivedBytes);
		btg.add(receivedExternalMsgs);
		btg.add(receivedExternalBytes);
		btg.add(receivedExternalNodeMsgs);
		btg.add(receivedExternalNodeBytes);
		btg.add(avgSizeSent);
		btg.add(avgSizeReceived);
		btg.add(avgSizeExternal);
		btg.add(avgSizeExternalNode);

		viewSelectPanel = new JPanel();
		Util.gblAdd(viewSelectPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalMsgs, gbc, 4,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalBytes, gbc, 5,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalNodeMsgs, gbc, 6,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, receivedExternalNodeBytes, gbc, 7,0, 1,1, 1,1);
		Util.gblAdd(viewSelectPanel, avgSizeSent, gbc, 0,1, 2,1, 1,1);
		Util.gblAdd(viewSelectPanel, avgSizeReceived, gbc, 2,1, 2,1, 1,1);
		Util.gblAdd(viewSelectPanel, avgSizeExternal, gbc, 4,1, 2,1, 1,1);
		Util.gblAdd(viewSelectPanel, avgSizeExternalNode, gbc, 6,1, 2,1, 1,1);

		totalsButton = new JRadioButton("Totals per interval", true);
		totalsButton.addActionListener(this);
		totalsButton.setToolTipText("Each bar is the unnormalized total over its time interval, summed over the selected PEs");
		rateButton = new JRadioButton("Rate (per second)");
		rateButton.addActionListener(this);
		rateButton.setToolTipText("Each bar is divided by the interval length in seconds; aggregated over the selected PEs, not per PE");
		ratePerPEButton = new JRadioButton("Rate per PE (per second)");
		ratePerPEButton.addActionListener(this);
		ratePerPEButton.setToolTipText("Rate divided by the number of selected PEs: average communication intensity per PE. Multiply by PEs per logical/physical node for the per-node rate.");

		yScaleGroup = new ButtonGroup();
		yScaleGroup.add(totalsButton);
		yScaleGroup.add(rateButton);
		yScaleGroup.add(ratePerPEButton);

		yScalePanel = new JPanel();
		Util.gblAdd(yScalePanel, new JLabel("Y-axis scale:"), gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(yScalePanel, totalsButton, gbc, 1,0, 1,1, 0,0);
		Util.gblAdd(yScalePanel, rateButton, gbc, 2,0, 1,1, 0,0);
		Util.gblAdd(yScalePanel, ratePerPEButton, gbc, 3,0, 1,1, 0,0);

		// control panel items
		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);

		totalCount = new JLabel();
		showLegendCheckBox = new JCheckBox("Show Legend");
		showLegendCheckBox.setToolTipText("Movable window naming the displayed entry methods; opens automatically for the Avg Size line views, where hover popups are unavailable");
		showLegendCheckBox.addActionListener(this);
		//	epSelection = new JButton("Select Entry Points");
		//	epSelection.addActionListener(this);
		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);
		//	Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, setRanges,   gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, totalCount,   gbc, 1,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, showLegendCheckBox, gbc, 2,0, 1,1, 0,0);

		graphPanel = getMainPanel();
		Util.gblAdd(mainPanel, graphPanel,     gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, viewSelectPanel,  gbc, 0,2, 1,1, 0,0);
		Util.gblAdd(mainPanel, yScalePanel,    gbc, 0,3, 1,1, 0,0);
		Util.gblAdd(mainPanel, controlPanel,   gbc, 0,4, 1,0, 0,0);
	}

	public long accumulateArray(double arr[][]) {
		double total = 0;
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				total += arr[i][j];
			}
		}
		return Math.round(total);
	}

	private boolean rateSelected() {
		return (rateButton != null && rateButton.isSelected()) || perPESelected();
	}

	private boolean perPESelected() {
		return ratePerPEButton != null && ratePerPEButton.isSelected();
	}

	private double rateDivisor() {
		double divisor = intervalSizeSeconds();
		if (perPESelected()) {
			divisor *= processorList.size();
		}
		return divisor;
	}

	// intervalSize is in microseconds
	private double intervalSizeSeconds() {
		return intervalSize / 1000000.0;
	}

	/** Return the array as-is (totals) or a copy scaled to per-second rates. */
	private double[][] scaleForDisplay(double[][] arr) {
		if (!rateSelected()) {
			return arr;
		}
		double divisor = rateDivisor();
		double[][] rates = new double[arr.length][];
		for (int i = 0; i < arr.length; i++) {
			rates[i] = new double[arr[i].length];
			for (int j = 0; j < arr[i].length; j++) {
				rates[i][j] = arr[i][j] / divisor;
			}
		}
		return rates;
	}

	private String yAxisSuffix() {
		if (perPESelected()) {
			return " per Second per PE";
		}
		return rateSelected() ? " per Second" : "";
	}

	public void changeView(JRadioButton cb) {
		boolean avgMode = (cb == avgSizeSent) || (cb == avgSizeReceived)
				|| (cb == avgSizeExternal) || (cb == avgSizeExternalNode);
		// Averages are not additive, so the avg-size views use unstacked lines
		// (one per EP) instead of stacked bars, and the rate scalings (which
		// would divide an already-normalized ratio) are disabled.
		totalsButton.setEnabled(!avgMode);
		rateButton.setEnabled(!avgMode);
		ratePerPEButton.setEnabled(!avgMode);
		if (avgMode != inAvgSizeMode && getGraphPanel() != null) {
			if (avgMode) {
				getGraphPanel().selectGraphType(Graph.LINE, false);
				graphCanvas.setHorizontalGridlines(true);
				// thin colored lines are hard to read on the default black
				// background: force white while in avg-size mode
				savedBackground = MainWindow.runObject[myRun].background;
				savedForeground = MainWindow.runObject[myRun].foreground;
				MainWindow.runObject[myRun].background = java.awt.Color.white;
				MainWindow.runObject[myRun].foreground = java.awt.Color.black;
				// line views have no hover popups, so bring up the legend
				if (!showLegendCheckBox.isSelected()) {
					showLegendCheckBox.setSelected(true);
				}
			} else {
				getGraphPanel().selectGraphType(Graph.BAR, true);
				graphCanvas.setHorizontalGridlines(false);
				if (savedBackground != null) {
					MainWindow.runObject[myRun].background = savedBackground;
					MainWindow.runObject[myRun].foreground = savedForeground;
				}
			}
		}
		inAvgSizeMode = avgMode;

		if(cb == sentMsgs) {
			setDataSource("Messages Sent Over Time", scaleForDisplay(sentMsgOutput), 
					commTimeColors, this);
			setPopupText("sentMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Sent" + yAxisSuffix(), "");
			totalCount.setText("Total messages sent: " + accumulateArray(sentMsgOutput));
			super.refreshGraph();
		}
		else if(cb == sentBytes){
			setDataSource("Bytes Sent Over Time", scaleForDisplay(sentByteOutput), 
					commTimeColors, this);
			setPopupText("sentByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Sent" + yAxisSuffix(), "");
			totalCount.setText("Total bytes sent: " + accumulateArray(sentByteOutput));
			super.refreshGraph();
		}
		else if(cb == receivedMsgs){
			setDataSource("Received Messages Over Time", scaleForDisplay(receivedMsgOutput), 
					commTimeColors, this);
			setPopupText("receivedMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received" + yAxisSuffix(), "");
			totalCount.setText("Total messages received: " + accumulateArray(receivedMsgOutput));
			super.refreshGraph();
		}
		else if(cb == receivedBytes){
			setDataSource("Received Bytes Over Time", scaleForDisplay(receivedByteOutput), 
					commTimeColors, this);
			setPopupText("receivedByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Received" + yAxisSuffix(), "");
			totalCount.setText("Total bytes received: " + accumulateArray(receivedByteOutput));
			super.refreshGraph();
		}
		else if(cb == receivedExternalMsgs){
			setDataSource("Received External Messages Over Time", scaleForDisplay(receivedExternalMsgOutput),
					commTimeColors, this);
			setPopupText("receivedExternalMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Messages Received Externally" + yAxisSuffix(), "");
			totalCount.setText("Total external messages received: " + accumulateArray(receivedExternalMsgOutput));
			super.refreshGraph();
		}
		else if(cb == receivedExternalBytes){
			setDataSource("Received External Bytes Over Time", scaleForDisplay(receivedExternalByteOutput),
					commTimeColors, this);
			setPopupText("receivedExternalByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("Bytes Received Externally" + yAxisSuffix(), "");
			totalCount.setText("Total external bytes received: " + accumulateArray(receivedExternalByteOutput));
			super.refreshGraph();
		}
        else if(cb == receivedExternalNodeMsgs){
			setDataSource("Received External Node Messages Over Time", scaleForDisplay(receivedExternalNodeMsgOutput),
					commTimeColors, this);
			setPopupText("receivedExternalNodeMsgCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("External Node Messages Received" + yAxisSuffix(), "");
			totalCount.setText("Total external node messages received: " + accumulateArray(receivedExternalNodeMsgOutput));
			super.refreshGraph();
		}
		else if(cb == receivedExternalNodeBytes){
			setDataSource("Received External Node Bytes Over Time", scaleForDisplay(receivedExternalNodeByteOutput),
					commTimeColors, this);
			setPopupText("receivedExternalNodeByteCount");
			setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
					startInterval*intervalSize, intervalSize);
			setYAxis("External Node Bytes Received" + yAxisSuffix(), "");
			totalCount.setText("Total external node bytes received: " + accumulateArray(receivedExternalNodeByteOutput));
			super.refreshGraph();
		}
		else if(cb == avgSizeSent){
			displayAvgSize("Average Sent Message Size Over Time", "avgSizeSent",
					sentMsgCount, sentByteCount, "sent");
		}
		else if(cb == avgSizeReceived){
			displayAvgSize("Average Received Message Size Over Time", "avgSizeReceived",
					receivedMsgCount, receivedByteCount, "received");
		}
		else if(cb == avgSizeExternal){
			displayAvgSize("Average External Received Message Size Over Time", "avgSizeExternal",
					receivedExternalMsgCount, receivedExternalByteCount, "external received");
		}
		else if(cb == avgSizeExternalNode){
			displayAvgSize("Average External Node Received Message Size Over Time", "avgSizeExternalNode",
					receivedExternalNodeMsgCount, receivedExternalNodeByteCount, "external node received");
		}

		refreshLegend();
	}

	/** Show per-interval average message size as one line per entry method,
	 *  on a log2(bytes) scale (message sizes span orders of magnitude).
	 *  Only the AVG_SIZE_MAX_LINES EPs with the largest total byte volume are
	 *  plotted, so the chart matches the legend 1:1. Intervals with no
	 *  messages are NaN: the line breaks there instead of dropping to zero,
	 *  and rare isolated messages show up as dots (see Graph.drawLineGraph). */
	private void displayAvgSize(String title, String key, double[][] msgs, double[][] bytes, String direction) {
		double totalMsgs = 0;
		double totalBytes = 0;
		double[] epMsgTotals = new double[numEPs];
		double[] epByteTotals = new double[numEPs];
		int[] intervalsPresent = new int[numEPs];
		for (int ep=0; ep<numEPs; ep++) {
			for (int interval=0; interval<numIntervals; interval++) {
				epMsgTotals[ep] += msgs[interval][ep];
				epByteTotals[ep] += bytes[interval][ep];
				if (msgs[interval][ep] > 0) {
					intervalsPresent[ep]++;
				}
			}
			totalMsgs += epMsgTotals[ep];
			totalBytes += epByteTotals[ep];
		}

		// EPs whose messages all land in a single interval are outliers
		// (one giant message would stretch the axis); leave them off unless
		// nothing else qualifies
		int eligible = 0;
		int outliers = 0;
		for (int ep=0; ep<numEPs; ep++) {
			if (epMsgTotals[ep] > 0) {
				if (intervalsPresent[ep] >= AVG_SIZE_MIN_INTERVALS) {
					eligible++;
				} else {
					outliers++;
				}
			}
		}
		boolean keepOutliers = (eligible == 0);
		if (keepOutliers) {
			eligible = outliers;
			outliers = 0;
		}

		// keep the AVG_SIZE_MAX_LINES eligible EPs with the most total bytes
		int outSize = Math.min(eligible, AVG_SIZE_MAX_LINES);
		avgSizeOmittedEPs = (eligible - outSize) + outliers;
		Integer[] byBytes = new Integer[numEPs];
		for (int ep=0; ep<numEPs; ep++) {
			byBytes[ep] = ep;
		}
		final double[] byteKey = epByteTotals;
		java.util.Arrays.sort(byBytes, new Comparator<Integer>() {
			public int compare(Integer a, Integer b) {
				return Double.compare(byteKey[b], byteKey[a]);
			}
		});
		avgSizeEPMap = new int[outSize];
		int count = 0;
		for (int i=0; i<numEPs && count<outSize; i++) {
			int ep = byBytes[i];
			if (epMsgTotals[ep] > 0
					&& (keepOutliers || intervalsPresent[ep] >= AVG_SIZE_MIN_INTERVALS)) {
				avgSizeEPMap[count++] = ep;
			}
		}
		// EP index order, so column order matches the other views
		java.util.Arrays.sort(avgSizeEPMap);

		double maxLog = 1;
		avgSizeOutput = new double[numIntervals][outSize];
		for (int k=0; k<outSize; k++) {
			int ep = avgSizeEPMap[k];
			for (int interval=0; interval<numIntervals; interval++) {
				double m = msgs[interval][ep];
				// log2(avg+1) keeps zero-length messages at y=0; NaN = no data
				if (m > 0) {
					double v = Math.log(bytes[interval][ep]/m + 1.0) / Math.log(2.0);
					avgSizeOutput[interval][k] = v;
					if (v > maxLog) {
						maxLog = v;
					}
				} else {
					avgSizeOutput[interval][k] = Double.NaN;
				}
			}
		}
		avgSizeMsgSource = msgs;
		avgSizeByteSource = bytes;

		setDataSource(title, avgSizeOutput, avgSizeColors, this);
		setPopupText(key);
		setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
				startInterval*intervalSize, intervalSize);
		// ticks are labeled with actual sizes (2, 4, ... 1K, 2K, ... 1M)
		setYAxis(new Log2BytesYAxis("Avg Message Size (log scale)", Math.ceil(maxLog)));
		if (totalMsgs > 0) {
			String note = "";
			if (avgSizeOmittedEPs > 0) {
				note = "; showing top " + outSize + " EPs by bytes, " + avgSizeOmittedEPs + " omitted";
				if (outliers > 0) {
					note += " (" + outliers + " single-interval)";
				}
			}
			totalCount.setText("Overall average " + direction + " message size: "
					+ formatBytes(totalBytes/totalMsgs) + " over " + Math.round(totalMsgs)
					+ " messages" + note);
		} else {
			totalCount.setText("No " + direction + " messages in the selected range");
		}
		super.refreshGraph();
	}

	private String formatBytes(double bytes) {
		if (bytes >= 1024.0*1024.0) {
			return _format.format(bytes/(1024.0*1024.0)) + " MB";
		}
		if (bytes >= 1024.0) {
			return _format.format(bytes/1024.0) + " KB";
		}
		return _format.format(bytes) + " B";
	}

	private void refreshLegend() {
		if (showLegendCheckBox != null && showLegendCheckBox.isSelected()) {
			showLegendWindow();
		} else {
			closeLegendWindow();
		}
	}

	/** Open (or refresh) the movable legend for the current view. */
	private void showLegendWindow() {
		Point oldLocation = null;
		if (legendWindow != null) {
			Legend l = legendWindow;
			legendWindow = null;
			oldLocation = l.getFrame().getLocation();
			l.dispose();
		}
		legendWindow = makeLegend();
		if (legendWindow == null) {
			showLegendCheckBox.setSelected(false);
			return;
		}
		if (oldLocation != null) {
			legendWindow.getFrame().setLocation(oldLocation);
		} else {
			legendWindow.getFrame().setLocationRelativeTo(thisWindow);
		}
		// Keep the checkbox in sync if the user closes the legend window directly
		legendWindow.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				legendWindow = null;
				showLegendCheckBox.setSelected(false);
			}
		});
	}

	private void closeLegendWindow() {
		if (legendWindow != null) {
			Legend l = legendWindow;
			legendWindow = null;
			l.dispose();
		}
	}

	/** One legend entry with its sorting value */
	private static class LegendEntry {
		final double value;
		final String label;
		final Paint paint;
		LegendEntry(double value, String label, Paint paint) {
			this.value = value;
			this.label = label;
			this.paint = paint;
		}
	}

	/** Build a legend for the current view: every displayed EP for the
	 *  avg-size line views (labeled with its overall average size), the
	 *  top-{@value #LEGEND_TOP_N} EPs by total for the bar views. */
	private Legend makeLegend() {
		List<LegendEntry> entries = new ArrayList<LegendEntry>();
		String title;

		if (inAvgSizeMode) {
			if (avgSizeEPMap == null || avgSizeMsgSource == null) {
				return null;
			}
			title = (avgSizeOmittedEPs > 0)
					? "Avg Msg Size (top " + avgSizeEPMap.length + " EPs by bytes)"
					: "Avg Msg Size Legend";
			for (int k=0; k<avgSizeEPMap.length; k++) {
				int ep = avgSizeEPMap[k];
				double m = 0;
				double b = 0;
				for (int interval=0; interval<numIntervals; interval++) {
					m += avgSizeMsgSource[interval][ep];
					b += avgSizeByteSource[interval][ep];
				}
				double avg = (m > 0) ? b/m : 0;
				entries.add(new LegendEntry(avg,
						formatBytes(avg) + " (" + Math.round(m) + " msgs)  "
						+ MainWindow.runObject[myRun].getPrettyEntryNameByIndex(ep),
						MainWindow.runObject[myRun].getEntryColor(ep)));
			}
		} else {
			double[][] data = currentOutputArray();
			if (data == null) {
				return null;
			}
			boolean isBytes = currentArrayName != null && currentArrayName.contains("Byte");
			title = "Legend (top " + LEGEND_TOP_N + ")";
			for (int ep=0; ep<numEPs; ep++) {
				double total = 0;
				for (int interval=0; interval<numIntervals; interval++) {
					total += data[interval][ep];
				}
				if (total > 0) {
					String amount = isBytes ? formatBytes(total) : Math.round(total) + " msgs";
					entries.add(new LegendEntry(total,
							amount + "  " + MainWindow.runObject[myRun].getPrettyEntryNameByIndex(ep),
							MainWindow.runObject[myRun].getEntryColor(ep)));
				}
			}
		}

		// largest first, so the legend reads top-down like the biggest lines/bars
		Collections.sort(entries, new Comparator<LegendEntry>() {
			public int compare(LegendEntry a, LegendEntry b) {
				return Double.compare(b.value, a.value);
			}
		});
		int max = inAvgSizeMode ? entries.size() : Math.min(entries.size(), LEGEND_TOP_N);

		List<String> names = new ArrayList<String>();
		List<Paint> paints = new ArrayList<Paint>();
		for (int i=0; i<max; i++) {
			names.add(entries.get(i).label);
			paints.add(entries.get(i).paint);
		}
		if (names.isEmpty()) {
			return null;
		}
		return new Legend(title, names, paints);
	}

	/** The raw per-interval array backing the currently selected bar view */
	private double[][] currentOutputArray() {
		if (currentArrayName == null) {
			return null;
		}
		if (currentArrayName.equals("sentMsgCount")) return sentMsgOutput;
		if (currentArrayName.equals("sentByteCount")) return sentByteOutput;
		if (currentArrayName.equals("receivedMsgCount")) return receivedMsgOutput;
		if (currentArrayName.equals("receivedByteCount")) return receivedByteOutput;
		if (currentArrayName.equals("receivedExternalMsgCount")) return receivedExternalMsgOutput;
		if (currentArrayName.equals("receivedExternalByteCount")) return receivedExternalByteOutput;
		if (currentArrayName.equals("receivedExternalNodeMsgCount")) return receivedExternalNodeMsgOutput;
		if (currentArrayName.equals("receivedExternalNodeByteCount")) return receivedExternalNodeByteOutput;
		return null;
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

	/** Format a popup value according to the selected y-axis scale. */
	private String formatPopupValue(double count, boolean bytes) {
		if (rateSelected()) {
			String rateUnit = (bytes ? "B/s" : "messages/s") + (perPESelected() ? "/PE" : "");
			return String.format("Rate = %s %s (%s %s)",
				_format.format(count / rateDivisor()),
				rateUnit,
				_format.format(count),
				bytes ? "bytes" : "messages");
		}
		return (bytes ? "Bytes: " : "Messages: ") + _format.format(count);
	}

	public String[] getPopup(int xVal, int yVal) {
		//System.out.println("CommWindow.getPopup()");
		//System.out.println(xVal +", " +yVal);
		if( (xVal < 0) || (yVal <0) || currentArrayName==null)
			return null;

		// avg-size views: yVal indexes the filtered EP columns, not stateArray.
		// (Line graphs currently never fire popups; this is here in case they do.)
		if (currentArrayName.startsWith("avgSize")) {
			if (avgSizeEPMap == null || yVal >= avgSizeEPMap.length || xVal >= numIntervals) {
				return null;
			}
			int ep = avgSizeEPMap[yVal];
			String[] avgString = new String[4];
			avgString[0] = "Time Interval: " +
			U.humanReadableString((xVal+startInterval)*intervalSize) + " to " +
			U.humanReadableString((xVal+startInterval+1)*intervalSize);
			avgString[1] = "Dest. Chare: " + MainWindow.runObject[myRun].getEntryChareNameByIndex(ep);
			avgString[2] = "Dest. EPid: " + MainWindow.runObject[myRun].getEntryNameByIndex(ep);
			double m = avgSizeMsgSource[xVal][ep];
			avgString[3] = (m > 0)
					? "Avg size: " + formatBytes(avgSizeByteSource[xVal][ep]/m) +
					" over " + Math.round(m) + " messages"
					: "No messages in this interval";
			return avgString;
		}

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
			rString[3] = formatPopupValue(sentMsgOutput[xVal][yVal], false);
		}
		else if(currentArrayName.equals("sentByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = formatPopupValue(sentByteOutput[xVal][yVal], true);
		}
		else if(currentArrayName.equals("receivedMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = formatPopupValue(receivedMsgOutput[xVal][yVal], false);
		}
		else if(currentArrayName.equals("receivedByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = formatPopupValue(receivedByteOutput[xVal][yVal], true);
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
			rString[3] = formatPopupValue(receivedExternalMsgOutput[xVal][yVal], false);
		}
		else if(currentArrayName.equals("receivedExternalByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = formatPopupValue(receivedExternalByteOutput[xVal][yVal], true);
		}
        else if(currentArrayName.equals("receivedExternalNodeMsgCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = formatPopupValue(receivedExternalNodeMsgOutput[xVal][yVal], false);
		}
		else if(currentArrayName.equals("receivedExternalNodeByteCount")) {
			rString[1] = "Dest. Chare: " + epClassName;
			rString[2] = "Dest. EPid: " + epName;	    
			rString[3] = formatPopupValue(receivedExternalNodeByteOutput[xVal][yVal], true);
		}

		return rString;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			if (b == setRanges) {
				showDialog();
			}
			
		} else if (e.getSource() == showLegendCheckBox) {
			refreshLegend();
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
			JRadioButton source = (JRadioButton)e.getSource();
			if (source == totalsButton || source == rateButton || source == ratePerPEButton) {
				// y-scale changed: redisplay whichever view is selected
				for (Enumeration<AbstractButton> buttons = btg.getElements(); buttons.hasMoreElements(); ) {
					AbstractButton button = buttons.nextElement();
					if (button.isSelected()) {
						changeView((JRadioButton) button);
					}
				}
			} else {
				changeView(source);
			}
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
