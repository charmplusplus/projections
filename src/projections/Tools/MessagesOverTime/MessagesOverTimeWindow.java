package projections.Tools.MessagesOverTime;

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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

import projections.gui.EntryMethodVisibility;
import projections.gui.GenericGraphColorer;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.Legend;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;

/**
 *  How many messages were processed over time, from summary detail (.sumd)
 *  files, broken down by the entry method that processed them.
 *
 *  A .sumd file records, for every entry method in every interval, both the
 *  time it spent running and the number of times it ran. An entry method runs
 *  once per message delivered to it, so that count is the number of messages
 *  the interval processed. It is the only communication measure a summary
 *  trace carries: there are no message sizes, no senders and no record of
 *  messages sent, so this counts arrivals, and only those that were executed
 *  within the selected range.
 *
 *  Communication Over Time answers the same question for .log traces, in more
 *  detail than this can. This tool exists because summary traces are what
 *  large runs can afford to write.
 */
public class MessagesOverTimeWindow extends GenericGraphWindow
implements ActionListener, EntryMethodVisibility
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple runs.
	private static int myRun = 0;

	private MessagesOverTimeWindow thisWindow;

	private JPanel mainPanel;
	private JPanel graphPanel;
	private JPanel controlPanel;
	private JPanel yScalePanel;
	private IntervalChooserPanel intervalPanel;

	private JButton setRanges;
	private JLabel totalCount;
	private JCheckBox packUnpackCheckBox;
	private JCheckBox showLegendCheckBox;
	private Legend legendWindow;
	private static final int LEGEND_TOP_N = 10;

	private ButtonGroup measureGroup;
	private JRadioButton messagesButton;
	private JRadioButton bytesButton;

	private ButtonGroup yScaleGroup;
	private JRadioButton totalsButton;
	private JRadioButton rateButton;
	private JRadioButton ratePerPEButton;

	private int startInterval;
	private int endInterval;
	private int numIntervals;
	private int numEPs;
	private long intervalSize;
	private SortedSet<Integer> processorList;

	/** [interval][ep], raw counts over the selected PEs. Any per-second or
	 *  per-PE scaling happens when the data is handed to the chart. */
	private double[][] msgCount;
	/** [interval][ep], bytes of those messages. All zero for a trace written
	 *  before charm recorded message sizes, which is why the Bytes view turns
	 *  itself off rather than drawing an empty chart. */
	private double[][] msgBytes;
	private boolean existsArray[];
	private DecimalFormat _format;
	private MyColorer colorer;

	/** The runtime's packing and unpacking pseudo entry methods, or -1.
	 *
	 *  charm counts a run of one of these every time it serializes a message,
	 *  from endPack and endUnpack rather than from a message being delivered
	 *  (trace-common.C registers them as "dummy_pack_ep" and
	 *  "dummy_unpack_ep"). They are not messages processed, and on a run that
	 *  packs heavily they can be most of the count -- two thirds of it on the
	 *  trace this tool was written against, which buries the application's own
	 *  traffic. Left out unless asked for. */
	private int packEP = -1;
	private int unpackEP = -1;

	public MessagesOverTimeWindow(MainWindow mainWindow) {
		// Opened from the Communication Over Time menu entry, which for a
		// summary trace can only lead here, so the window is named for it.
		super("Projections Communication Over Time - " +
				MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		setGraphSpecificData();
		numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		existsArray = new boolean[numEPs];
		for (int ep=0; ep<numEPs; ep++) {
			String name = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
			if ("dummy_pack_ep".equals(name)) {
				packEP = ep;
			} else if ("dummy_unpack_ep".equals(name)) {
				unpackEP = ep;
			}
		}
		colorer = new MyColorer();
		_format = new DecimalFormat("###,###.###");
		mainPanel = new JPanel();
		setLayout(mainPanel);
		createMenus();
		createLayout();
		pack();
		thisWindow = this;

		showDialog();
	}

	/** One color per entry method, in the chart's column order. */
	public class MyColorer implements GenericGraphColorer {
		public Paint[] getColorMap() {
			Paint[] outColors = new Paint[numEPs];
			for (int ep=0; ep<numEPs; ep++) {
				outColors[ep] = MainWindow.runObject[myRun].getEntryColor(ep);
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

		messagesButton = new JRadioButton("Messages", true);
		messagesButton.addActionListener(this);
		messagesButton.setToolTipText("Number of messages each entry method processed");
		bytesButton = new JRadioButton("Bytes");
		bytesButton.addActionListener(this);
		bytesButton.setToolTipText("Bytes those messages carried, envelope included; recorded only by traces written by a charm that knows how");
		measureGroup = new ButtonGroup();
		measureGroup.add(messagesButton);
		measureGroup.add(bytesButton);

		totalsButton = new JRadioButton("Totals per interval", true);
		totalsButton.addActionListener(this);
		totalsButton.setToolTipText("Each bar is the number of messages processed in its interval, summed over the selected PEs");
		rateButton = new JRadioButton("Rate (per second)");
		rateButton.addActionListener(this);
		rateButton.setToolTipText("Each bar divided by the interval length in seconds; aggregated over the selected PEs, not per PE");
		ratePerPEButton = new JRadioButton("Rate per PE (per second)");
		ratePerPEButton.addActionListener(this);
		ratePerPEButton.setToolTipText("Rate divided by the number of selected PEs: messages processed per second by an average PE");

		yScaleGroup = new ButtonGroup();
		yScaleGroup.add(totalsButton);
		yScaleGroup.add(rateButton);
		yScaleGroup.add(ratePerPEButton);

		yScalePanel = new JPanel();
		Util.gblAdd(yScalePanel, new JLabel("Show:"), gbc, 0,1, 1,1, 0,0);
		Util.gblAdd(yScalePanel, messagesButton, gbc, 1,1, 1,1, 0,0);
		Util.gblAdd(yScalePanel, bytesButton, gbc, 2,1, 1,1, 0,0);
		Util.gblAdd(yScalePanel, new JLabel("Y-axis scale:"), gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(yScalePanel, totalsButton, gbc, 1,0, 1,1, 0,0);
		Util.gblAdd(yScalePanel, rateButton, gbc, 2,0, 1,1, 0,0);
		Util.gblAdd(yScalePanel, ratePerPEButton, gbc, 3,0, 1,1, 0,0);

		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);
		totalCount = new JLabel();
		packUnpackCheckBox = new JCheckBox("Count pack/unpack");
		packUnpackCheckBox.setToolTipText("Include the runtime's message packing and unpacking, which charm counts as entry method runs even though no message was delivered");
		packUnpackCheckBox.addActionListener(this);
		showLegendCheckBox = new JCheckBox("Show Legend");
		showLegendCheckBox.setToolTipText("Movable window naming the entry methods that processed the most messages");
		showLegendCheckBox.addActionListener(this);

		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);
		Util.gblAdd(controlPanel, setRanges, gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, totalCount, gbc, 1,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, packUnpackCheckBox, gbc, 2,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, showLegendCheckBox, gbc, 3,0, 1,1, 0,0);

		graphPanel = getMainPanel();
		Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, yScalePanel, gbc, 0,2, 1,1, 0,0);
		Util.gblAdd(mainPanel, controlPanel, gbc, 0,3, 1,0, 0,0);
	}

	protected void setGraphSpecificData(){
		setXAxis("Time", "");
		setYAxis("Messages Processed", "");
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

			final SwingWorker worker = new SwingWorker() {
				public Object doInBackground() {
					getData();
					return null;
				}
				public void done() {
					displayData();
					thisWindow.setVisible(true);
					thisWindow.repaint();
				}
			};
			worker.execute();
		}
	}

	private void getData() {
		msgCount = MainWindow.runObject[myRun].getSumDetailMsgsPerInterval(
				intervalSize, startInterval, endInterval, processorList);
		msgBytes = MainWindow.runObject[myRun].getSumDetailBytesPerInterval(
				intervalSize, startInterval, endInterval, processorList);

		for (int ep=0; ep<numEPs; ep++) {
			existsArray[ep] = false;
			for (int interval=0; interval<numIntervals; interval++) {
				if (msgCount[interval][ep] > 0) {
					existsArray[ep] = true;
					break;
				}
			}
		}
	}

	/** Hand the data to the chart in whichever scale is selected. */
	private void displayData() {
		if (msgCount == null) {
			return;
		}
		// A trace from a charm that did not record message sizes has none to
		// show; say so rather than drawing an empty chart.
		if (showingBytes() && !hasBytes()) {
			messagesButton.setSelected(true);
			totalCount.setText("This trace records no message sizes: its .sumd files were " +
					"written before charm recorded them.");
			return;
		}

		String what = showingBytes() ? "Bytes Received" : "Messages Processed";
		setDataSource(what + " Over Time", scaleForDisplay(displayCounts()), colorer, this);
		setXAxis("Time (" + U.humanReadableString(intervalSize) + " resolution)", "Time",
				startInterval*intervalSize, intervalSize);
		setYAxis(what + yAxisSuffix(), "");

		String label = (showingBytes() ? "Total bytes received: " : "Total messages processed: ") +
				_format.format(total(false)) + " over " + processorList.size() + " PEs";
		double packUnpack = total(true) - total(false);
		if (packUnpack > 0 && !countPackUnpack()) {
			label += "  (" + _format.format(packUnpack) +
					(showingBytes() ? " from pack/unpack not counted)" : " pack/unpack runs not counted)");
		}
		totalCount.setText(label);
		refreshLegend();
		super.refreshGraph();
	}

	private boolean showingBytes() {
		return bytesButton != null && bytesButton.isSelected();
	}

	/** Does this trace carry message sizes at all? charm only started writing
	 *  them in 2026; everything older reads as zeros. */
	private boolean hasBytes() {
		if (msgBytes == null) {
			return false;
		}
		for (int interval=0; interval<numIntervals; interval++) {
			for (int ep=0; ep<numEPs; ep++) {
				if (msgBytes[interval][ep] > 0) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean countPackUnpack() {
		return packUnpackCheckBox != null && packUnpackCheckBox.isSelected();
	}

	private boolean isPackOrUnpack(int ep) {
		return ep == packEP || ep == unpackEP;
	}

	/** The counts as the chart should show them: everything, or everything the
	 *  application itself processed. */
	private double[][] displayCounts() {
		double[][] source = sourceArray();
		if (countPackUnpack() || (packEP < 0 && unpackEP < 0)) {
			return source;
		}
		double[][] filtered = new double[numIntervals][numEPs];
		for (int interval=0; interval<numIntervals; interval++) {
			for (int ep=0; ep<numEPs; ep++) {
				filtered[interval][ep] = isPackOrUnpack(ep) ? 0.0 : source[interval][ep];
			}
		}
		return filtered;
	}

	/** The raw array behind the current view. */
	private double[][] sourceArray() {
		return showingBytes() ? msgBytes : msgCount;
	}

	private double total(boolean includePackUnpack) {
		double[][] source = sourceArray();
		double total = 0;
		for (int interval=0; interval<numIntervals; interval++) {
			for (int ep=0; ep<numEPs; ep++) {
				if (includePackUnpack || !isPackOrUnpack(ep)) {
					total += source[interval][ep];
				}
			}
		}
		return total;
	}

	private boolean rateSelected() {
		return (rateButton != null && rateButton.isSelected()) || perPESelected();
	}

	private boolean perPESelected() {
		return ratePerPEButton != null && ratePerPEButton.isSelected();
	}

	private double rateDivisor() {
		// intervalSize is in microseconds
		double divisor = intervalSize / 1000000.0;
		if (perPESelected()) {
			divisor *= processorList.size();
		}
		return divisor;
	}

	/** The counts as they are (totals), or a copy scaled to per-second rates. */
	private double[][] scaleForDisplay(double[][] arr) {
		if (!rateSelected()) {
			return arr;
		}
		double divisor = rateDivisor();
		double[][] rates = new double[arr.length][];
		for (int i=0; i<arr.length; i++) {
			rates[i] = new double[arr[i].length];
			for (int j=0; j<arr[i].length; j++) {
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

	public String[] getPopup(int xVal, int yVal) {
		if ((xVal < 0) || (yVal < 0) || msgCount == null ||
				xVal >= numIntervals || yVal >= numEPs) {
			return null;
		}
		String[] rString = new String[4];
		rString[0] = "Time Interval: " +
			U.humanReadableString((xVal+startInterval)*intervalSize) + " to " +
			U.humanReadableString((xVal+startInterval+1)*intervalSize);
		rString[1] = "Chare: " + MainWindow.runObject[myRun].getEntryChareNameByIndex(yVal);
		rString[2] = "Entry Method: " + MainWindow.runObject[myRun].getEntryNameByIndex(yVal);
		double count = sourceArray()[xVal][yVal];
		String unit = showingBytes() ? "bytes" : "messages";
		if (rateSelected()) {
			rString[3] = "Rate = " + _format.format(count / rateDivisor()) + " " + unit + "/s" +
				(perPESelected() ? "/PE" : "") + " (" + _format.format(count) + " " + unit + ")";
		} else {
			rString[3] = (showingBytes() ? "Bytes received: " : "Messages processed: ")
					+ _format.format(count);
		}
		return rString;
	}

	private void refreshLegend() {
		if (showLegendCheckBox != null && showLegendCheckBox.isSelected()) {
			showLegendWindow();
		} else {
			closeLegendWindow();
		}
	}

	/** Open (or refresh) the movable legend: the entry methods that processed
	 *  the most messages in the loaded range. */
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
		// Keep the checkbox in step if the legend window is closed directly
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

	/** One legend entry with the count it is ranked by */
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

	private Legend makeLegend() {
		if (msgCount == null) {
			return null;
		}
		List<LegendEntry> entries = new ArrayList<LegendEntry>();
		for (int ep=0; ep<numEPs; ep++) {
			if (isPackOrUnpack(ep) && !countPackUnpack()) {
				continue;
			}
			double total = 0;
			for (int interval=0; interval<numIntervals; interval++) {
				total += sourceArray()[interval][ep];
			}
			if (total > 0) {
				entries.add(new LegendEntry(total,
						_format.format(total) + (showingBytes() ? " bytes  " : " msgs  ") +
						MainWindow.runObject[myRun].getPrettyEntryNameByIndex(ep),
						MainWindow.runObject[myRun].getEntryColor(ep)));
			}
		}
		// largest first, so the legend reads top-down like the biggest bands
		Collections.sort(entries, new Comparator<LegendEntry>() {
			public int compare(LegendEntry a, LegendEntry b) {
				return Double.compare(b.value, a.value);
			}
		});
		int max = Math.min(entries.size(), LEGEND_TOP_N);
		List<String> names = new ArrayList<String>();
		List<Paint> paints = new ArrayList<Paint>();
		for (int i=0; i<max; i++) {
			names.add(entries.get(i).label);
			paints.add(entries.get(i).paint);
		}
		if (names.isEmpty()) {
			return null;
		}
		return new Legend("Legend (top " + LEGEND_TOP_N +
				(showingBytes() ? " by bytes)" : " by messages)"), names, paints);
	}

	/** Restrict "Choose Entry Colors" to the entry methods that processed
	 *  something in the loaded range. */
	protected EntryMethodVisibility getEntryFilter() {
		return (msgCount != null) ? this : null;
	}

	public int[] getEntriesArray() {
		int[] present = new int[numEPs];
		for (int ep=0; ep<numEPs; ep++) {
			boolean shown = existsArray[ep] && (countPackUnpack() || !isPackOrUnpack(ep));
			present[ep] = shown ? 1 : 0;
		}
		return present;
	}

	public boolean hasEntryList() {
		return true;
	}

	/** Idle and overhead are times, not messages, so they have no place here. */
	public boolean handleIdleOverhead() {
		return false;
	}

	public boolean entryIsVisibleID(Integer id) {
		return true;
	}

	public void makeEntryVisibleID(Integer id) {
		// visibility checkboxes are not shown for this tool's chooser
	}

	public void makeEntryInvisibleID(Integer id) {
	}

	public void displayMustBeRedrawn() {
		repaint();
	}

	/** Dispatch on which control was used rather than on what kind of control
	 *  it is: a check box is not a JButton, and testing for the type first is
	 *  how the pack/unpack box came to be wired to nothing at all. */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == setRanges) {
			showDialog();
		} else if (source == showLegendCheckBox) {
			refreshLegend();
		} else if (source == packUnpackCheckBox || source == messagesButton
				|| source == bytesButton || source == totalsButton
				|| source == rateButton || source == ratePerPEButton) {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			displayData();
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		} else if (source instanceof JMenuItem) {
			String arg = ((JMenuItem)source).getText();
			if (arg.equals("Close")) {
				close();
			} else if (arg.equals("Set Range")) {
				showDialog();
			}
		}
	}

	public void repaint() {
		super.refreshGraph();
	}
}
