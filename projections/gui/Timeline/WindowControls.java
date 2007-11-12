package projections.gui.Timeline;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.*;

import projections.gui.FloatTextField;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.OrderedUsageList;
import projections.gui.RangeDialog;
import projections.gui.SwingWorker;
import projections.gui.Util;

import java.text.*;

/**
 * A panel that contains all the buttons and labels and things at the bottom of
 * the timeline tool. It also handles all the events.
 * 
 */
public class WindowControls extends JPanel implements ActionListener,
ItemListener {

	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private TimelineWindow parentWindow;

	private ColorChooser colorWindow;

	Data data;

	// basic zoom controls
	private JButton bSelectRange, bColors, bDecrease, bIncrease, bReset;

	private JButton bZoomSelected, bLoadSelected;

//	// jump to graphs
//	private JButton bJumpProfile, bJumpGraph, bJumpHistogram, bJumpComm,
//	bJumpStl;

	private JTextField highlightTime, selectionBeginTime, selectionEndTime,
	selectionDiff;

	private DecimalFormat format;

	private FloatTextField scaleField;

	private JCheckBox cbPacks, cbMsgs, cbIdle, cbUser;

	private UserEventWindow userEventWindow;


	public WindowControls(TimelineWindow parentWindow_,
			Data data_) {

		data = data_;

		parentWindow = parentWindow_;

		format = new DecimalFormat();
		format.setGroupingUsed(true);
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(0);

		CreateLayout();

		System.out.println("Creating userEventWindow\n");
		userEventWindow = new UserEventWindow(cbUser,data);

	}

	public void showDialog() {
		System.out.println("showDialog()");

		if (parentWindow.dialog == null) {
			parentWindow.dialog = new RangeDialog(parentWindow,
			"Select Timeline Range");
		} else {
			parentWindow.setDialogData();
		}
		parentWindow.dialog.displayDialog();
		if (!parentWindow.dialog.isCancelled()) {
			parentWindow.getDialogData();
			if (parentWindow.dialog.isModified()) {
				// Create a worker that will load the trace logs and then will make 
				// the main window visible after it has finished loading
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						parentWindow.mainPanel.loadTimelineObjects(true);
						cbUser.setText("View User Events (" + data.getNumUserEvents() + ")");
						return null;
					}

					public void finished() {
						// Here we are basically at startup after the dialog window and the trace log has been read
						parentWindow.setSize(1000, 600);
						parentWindow.setVisible(true);
					}
				};
				worker.start();
			} else {
				parentWindow.setVisible(true);
			}
		}
	}



	private double calcLeftTime(double leftTime, double rightTime,
			double oldScale, double newScale) {
		double timeShowing = (rightTime - leftTime) * oldScale / newScale;
		double timeDiff = Math.abs((rightTime - leftTime) - timeShowing);
		if (oldScale > newScale) {
			return leftTime - timeDiff / 2;
		} else {
			return leftTime + timeDiff / 2;
		}
	}

	public void setHighlightTime(double time) {
		// System.out.println(time);
		highlightTime.setText(format.format(time));
	}

	public void unsetHighlightTime() {
		highlightTime.setText("");
	}

	public void setSelectedTime(double time1, double time2) {
		selectionBeginTime.setText(format.format(time1));
		selectionEndTime.setText(format.format(time2));
		format.setMinimumFractionDigits(3);
		format.setMaximumFractionDigits(3);
		selectionDiff.setText(format.format((time2 - time1) / 1000) + " ms");
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(0);
		
		bZoomSelected.setEnabled(true);
		bLoadSelected.setEnabled(true);
	}

	public void unsetSelectedTime() {
		selectionBeginTime.setText("");
		selectionEndTime.setText("");
		selectionDiff.setText("");
		
		bZoomSelected.setEnabled(false);
		bLoadSelected.setEnabled(false);
	}



	public void zoomSelected() {
		System.out.println("Zoom Selected");
		if (data.selectionValid()) {
			double selectionStartTime = data.leftSelectionTime();
			double selectionEndTime = data.rightSelectionTime();

			System.out.println("startTime="+selectionStartTime+" endTime="+selectionEndTime);
			unsetSelectedTime();
					
			data.setScaleFactor( (float) ((data.endTime() - data.beginTime()) / (selectionEndTime -
					selectionStartTime)) );
				
			// Set scroll to the place we want
			data.setPreferredViewTimeCenter((selectionStartTime+selectionEndTime) / 2.0);
		}
	}

	/** Load a new time region */
	public void loadSelected() {
		System.out.println("loadSelected");
		if (data.selectionValid()) {
			
	
			double startTime = data.leftSelectionTime();
			double endTime = data.rightSelectionTime();
			System.out.println("startTime="+startTime+" endTime="+endTime);

			
			data.invalidateSelection();
			unsetSelectedTime();
						
			if (startTime < data.beginTime()) { // This seems unlikely to happen
				startTime = data.beginTime();
			}
			if (endTime > data.endTime()) { // This seems unlikely to happen
				endTime = data.endTime();
			}

			System.out.println("calling setNewRange("+(long)(startTime+0.5)+","+(long)(endTime+0.5)+")");
			data.setNewRange((long)(startTime+0.5),(long)(endTime+0.5));

			scaleField.setText("" + 1.0);	
			
			System.out.println("Calling loadTimelineObjects()");
			parentWindow.mainPanel.loadTimelineObjects(true);
			
			cbUser.setText("View User Events (" + data.getNumUserEvents() + ")");
			
		} else{
			System.out.println("ERROR: somehow you clicked the loadSelected button which shouldn't have been enabled!");			
		}
	}

//	public void jumpToGraph(String b) {
//		Rectangle rect = parentWindow.displayPanel.rubberBandBounds();
//		double jStart = parentWindow.axisPanel.canvasToTime(rect.x);
//		double jEnd = parentWindow.axisPanel.canvasToTime(rect.x + rect.width);
//
//		
//		MainWindow.runObject[myRun].setJTimeAvailable(true);
//		if (rect.width == 0) {
//			MainWindow.runObject[myRun].setJTime((long) (0),
//					MainWindow.runObject[myRun].getTotalTime());
//		} else {
//			MainWindow.runObject[myRun].setJTime((long) (jStart + 0.5),
//					(long) (jEnd + 0.5));
//		}
//
//		// **CW** DELIBERATE BUG (adding 0 to window open), just to make
//		// it compile for now.
//		if (b == "Profile") {
//			ProfileWindow profileWindow = new ProfileWindow(parentWindow.parentWindow,
//					new Integer(0));
//		} else if (b == "Graph") {
//			GraphWindow graphWindow = new GraphWindow(parentWindow.parentWindow,
//					new Integer(0));
//		} else if (b == "Histogram") {
//			HistogramWindow histogramWindow = new HistogramWindow(parentWindow.parentWindow,
//					new Integer(0));
//		} else if (b == "Comm") {
//			CommWindow commWindow = new CommWindow(parentWindow.parentWindow, new Integer(0));
//		} else if (b == "Stl") {
//			StlWindow stlWindow = new StlWindow(parentWindow.parentWindow, new Integer(0));
//		}
//	}

	public void actionPerformed(ActionEvent evt) {
		
		// If the event is a menu action
		
		if (evt.getSource() instanceof JMenuItem) {
			System.out.println("JMenuItem Event");
			String arg = ((JMenuItem) evt.getSource()).getText();
			if (arg.equals("Close"))
				parentWindow.close();
			else if (arg.equals("Modify Ranges"))
				showDialog();
			// else if (arg.equals("Print Timeline"))
			// PrintTimeline();
			else if (arg.equals("Change Entry Point Colors")) {
				ShowColorWindow();
			} else if (arg.equals("Save Entry Point Colors")) {
				// save all entry point colors to disk
				MainWindow.runObject[myRun].saveColors();
			} else if (arg.equals("Restore Entry Point Colors")) {
				// openColorFile();
				try {
					Util.restoreColors(data.entryColor(), "Timeline Graph");
					parentWindow.refreshDisplay(false);
				} catch (Exception e) {
					System.err.println("Attempt to read from color.map failed");
				}


			} else if (arg.equals("Default Entry Point Colors")) {

				for (int i = 0; i < data.entryColor().length; i++)
					data.entryColor()[i] = MainWindow.runObject[myRun].getEntryColor(i);
				parentWindow.refreshDisplay(false);

			} else if (arg.equals("Save PNG")) {
				SaveImage p = new SaveImage(parentWindow.scrollingPanel);
				p.saveImagePNG("TimelineOut.png");
			}
		}

		
		// If the event is a JButton
		
		if (evt.getSource() instanceof JButton) {
			JButton b = (JButton) evt.getSource();
			if (b == bSelectRange) {
				showDialog();
			} else if (b == bColors) {
				ShowColorWindow();
			} else if (b == bZoomSelected) {
				zoomSelected();
				parentWindow.refreshDisplay(true);
			} else if (b == bLoadSelected) {
				loadSelected();
				parentWindow.refreshDisplay(true);
			} else {

				if (b == bDecrease) {
					data.keepViewCentered(true); // Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
					data.decreaseScaleFactor();
				} else if (b == bIncrease) {
					data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
					data.increaseScaleFactor();
				} else if (b == bReset) {
					data.setScaleFactor(1.0f);
				}
				scaleField.setText("" + data.getScaleFactor());

				parentWindow.refreshDisplay(true);

			}
		}
		
		
		// If the action corresponds to the scale value changing(likely typed in)
		if (evt.getSource() instanceof FloatTextField) {
			FloatTextField b = (FloatTextField) evt.getSource();
			if (b == scaleField) {
				data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
				data.setScaleFactor(b.getValue());
				parentWindow.refreshDisplay(true);
			}
		}

		
		
		
	}

	public void CreateMenus() {
		JMenuBar mbar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		JMenuItem i1 = new JMenuItem("Print Timeline");
		i1.setEnabled(false);
		JMenuItem i3 = new JMenuItem("Close");
		fileMenu.add(i1);
		fileMenu.addSeparator();
		fileMenu.add(i3);
		i1.addActionListener(this);
		i3.addActionListener(this);
		mbar.add(fileMenu);

		JMenu toolsMenu = new JMenu("Tools");
		JMenuItem i4 = new JMenuItem("Modify Ranges");
		toolsMenu.add(i4);
		i4.addActionListener(this);
		mbar.add(toolsMenu);

		JMenu colorMenu = new JMenu("Colors");
		JMenuItem i5 = new JMenuItem("Change Entry Point Colors");
		JMenuItem i6 = new JMenuItem("Save Entry Point Colors");
		JMenuItem i7 = new JMenuItem("Restore Entry Point Colors");
		JMenuItem i8 = new JMenuItem("Default Entry Point Colors");
		colorMenu.add(i5);
		colorMenu.add(i6);
		colorMenu.add(i7);
		colorMenu.add(i8);
		i5.addActionListener(this);
		i6.addActionListener(this);
		i7.addActionListener(this);
		i8.addActionListener(this);
		mbar.add(colorMenu);

		JMenu saveMenu = new JMenu("Image");
		JMenuItem i9 = new JMenuItem("Save PNG");
		saveMenu.add(i9);
		i9.addActionListener(this);
		mbar.add(saveMenu);

		// JMenu helpMenu = new JMenu("Help");
		// JMenuItem i9 = new JMenuItem("Index");
		// JMenuItem i10 = new JMenuItem("About");
		// helpMenu.add(i9);
		// helpMenu.add(i10);
		// i9.addActionListener(this);
		// i10.addActionListener(this);
		// mbar.add(helpMenu);

		parentWindow.setJMenuBar(mbar);

	}

	public void userEventWindowSetData(){
		userEventWindow.setData(data);
	}

//	**CW** Note that this is still quite a hack, but it should provide
//	a prototype for when other tools want to add stuff to timeline.
	public void addProcessor(int p) {
		parentWindow.addProcessor(p);
	}

	private void CreateLayout() {
		System.out.println("CreateLayout() for TimelineWindowController\n");

		// // CHECKBOX PANEL
		cbPacks = new JCheckBox("Display Pack Times", data.showPacks);
		cbMsgs = new JCheckBox("Display Message Sends", data.showMsgs);
		cbIdle = new JCheckBox("Display Idle Time", data.showIdle);
		cbUser = new JCheckBox("Display User Event Window", false);

		cbPacks.addItemListener(this);
		cbMsgs.addItemListener(this);
		cbIdle.addItemListener(this);
		cbUser.addItemListener(this);

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;

		JPanel cbPanel = new JPanel();
		cbPanel.setLayout(gbl);

		Util.gblAdd(cbPanel, cbPacks, gbc, 0, 0, 1, 1, 1, 1);
		Util.gblAdd(cbPanel, cbMsgs, gbc, 1, 0, 1, 1, 1, 1);
		Util.gblAdd(cbPanel, cbIdle, gbc, 2, 0, 1, 1, 1, 1);
		Util.gblAdd(cbPanel, cbUser, gbc, 3, 0, 1, 1, 1, 1);

		// // BUTTON PANEL
		bSelectRange = new JButton("Select Ranges");
		bColors = new JButton("Change Entry Point Colors");
		bDecrease = new JButton("<<");
		bIncrease = new JButton(">>");
		bReset = new JButton("Reset");

		bSelectRange.addActionListener(this);
		bColors.addActionListener(this);
		bDecrease.addActionListener(this);
		bIncrease.addActionListener(this);
		bReset.addActionListener(this);

		JLabel lScale = new JLabel("SCALE: ", JLabel.CENTER);
		scaleField = new FloatTextField(data.getScaleFactor(), 5);
		scaleField.addActionListener(this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(gbl);

		gbc.fill = GridBagConstraints.BOTH;

		Util.gblAdd(buttonPanel, bSelectRange, gbc, 0, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, bColors, gbc, 1, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, bDecrease, gbc, 3, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, lScale, gbc, 4, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, scaleField, gbc, 5, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, bIncrease, gbc, 6, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, bReset, gbc, 7, 0, 1, 1, 1, 1);

		// ZOOM PANEL

		bZoomSelected = new JButton("Zoom Selected");
		bLoadSelected = new JButton("Load Selected");

		bZoomSelected.setEnabled(false);
		bLoadSelected.setEnabled(false);
			
		bZoomSelected.addActionListener(this);
		bLoadSelected.addActionListener(this);

		highlightTime = new JTextField("");
		selectionBeginTime = new JTextField("");
		selectionEndTime = new JTextField("");
		selectionDiff = new JTextField("");
		highlightTime.setEditable(false);
		selectionBeginTime.setEditable(false);
		selectionEndTime.setEditable(false);
		selectionDiff.setEditable(false);

		JPanel zoomPanel = new JPanel();
		zoomPanel.setLayout(gbl);
		gbc.fill = GridBagConstraints.BOTH;

		Util.gblAdd(zoomPanel, new JLabel(" "), gbc, 0, 0, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, bZoomSelected, gbc, 0, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, bLoadSelected, gbc, 1, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, highlightTime, gbc, 2, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, selectionBeginTime, gbc, 3, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, selectionEndTime, gbc, 4, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, selectionDiff, gbc, 5, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Highlight Time", JLabel.CENTER),
				gbc, 2, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel,
				new JLabel("Selection Begin Time", JLabel.CENTER), gbc, 3, 1,
				1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection End Time", JLabel.CENTER),
				gbc, 4, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection Length", JLabel.CENTER),
				gbc, 5, 1, 1, 1, 1, 1);

		// JUMP TO GRAPH
//		bJumpProfile = new JButton("Usage Profile");
//		bJumpGraph = new JButton("Graph");
//		bJumpHistogram = new JButton("Histogram");
//		bJumpComm = new JButton("Communication");
//		bJumpStl = new JButton("Overview");
//
//		bJumpProfile.addActionListener(this);
//		bJumpGraph.addActionListener(this);
//		bJumpHistogram.addActionListener(this);
//		bJumpComm.addActionListener(this);
//		bJumpStl.addActionListener(this);

//		JPanel jumpPanel = new JPanel();
//		jumpPanel.setLayout(gbl);
//		gbc.fill = GridBagConstraints.BOTH;
//
//		Util.gblAdd(jumpPanel, new JLabel(" "), gbc, 0, 0, 1, 1, 1, 1);
//		Util.gblAdd(jumpPanel, new JLabel("Jump to graph: ", JLabel.LEFT), gbc,
//				1, 1, 1, 1, 1, 1);
//		Util.gblAdd(jumpPanel, bJumpProfile, gbc, 3, 1, 1, 1, 1, 1);
//		Util.gblAdd(jumpPanel, bJumpGraph, gbc, 4, 1, 1, 1, 1, 1);
//		Util.gblAdd(jumpPanel, bJumpHistogram, gbc, 5, 1, 1, 1, 1, 1);
//		Util.gblAdd(jumpPanel, bJumpComm, gbc, 6, 1, 1, 1, 1, 1);
//		Util.gblAdd(jumpPanel, bJumpStl, gbc, 7, 1, 1, 1, 1, 1);

		// // WINDOW

		this.setLayout(gbl);
		Util.gblAdd(this, cbPanel, gbc, 0, 1, 1, 1, 1, 0);
		Util.gblAdd(this, buttonPanel, gbc, 0, 2, 1, 1, 1, 0);
		Util.gblAdd(this, zoomPanel, gbc, 0, 3, 1, 1, 1, 0);
//		Util.gblAdd(this, jumpPanel, gbc, 0, 4, 1, 1, 1, 0);

	}

	public long getBeginTime() {
		return data.beginTime();
	}

	public long getEndTime() {
		return data.endTime();
	}

	public int[] getEntries() {
		return data.entries();
	}

	public Color[] getEntryColors() {
		if (data == null)
			return null;
		else
			return data.entryColor();
	}

	public OrderedUsageList[] getEntryUsageData() {
		if (data == null)
			return null;
		else
			return data.entryUsageList;
	}

	public Color getGraphColor(int e) {
		return MainWindow.runObject[myRun].getEntryColor(e);
		// return parentWindow.getGraphColor(e);
	}

	public float[] getIdleUsageData() {
		if (data == null)
			return null;
		else
			return data.idleUsage;
	}

	public float[] getPackUsageData() {
		if (data == null)
			return null;
		else
			return data.packUsage;
	}

	public OrderedIntList getProcessorList() {
		if (data == null)
			return null;
		else
			return data.processorList();
	}

	public void itemStateChanged(ItemEvent evt) {
		if (data == null)
			return;

		Object c = evt.getItemSelectable();

		if (c == cbPacks)
			data.showPacks = (evt.getStateChange() == ItemEvent.SELECTED);
		else if (c == cbMsgs)
			data.showMsgs = (evt.getStateChange() == ItemEvent.SELECTED);
		else if (c == cbIdle)
			data.showIdle = (evt.getStateChange() == ItemEvent.SELECTED);
		else if (c == cbUser) {
			if (evt.getStateChange() == ItemEvent.SELECTED){
				userEventWindow.pack();
				userEventWindow.setVisible(true); // pop up window
			}
			else
				userEventWindow.setVisible(false);
		}

		parentWindow.refreshDisplay(false);

	}

	private void PrintTimeline() {
//		PrintJob pjob = getToolkit().getPrintJob(this, "Print Timeline", null);

//		if (pjob == null)
//		return;

//		Dimension d = pjob.getPageDimension();

//		int marginLeft;
//		int marginTop;
//		if (d.width < d.height) {
//		marginLeft = (int) (0.6 * d.width / 8.5);
//		marginTop = (int) (0.6 * d.height / 11.0);
//		} else {
//		marginLeft = (int) (0.6 * d.width / 11.0);
//		marginTop = (int) (0.6 * d.height / 8.5);
//		}

//		int printWidth = d.width - 2 * marginLeft;
//		int printHeight = d.height - 2 * marginTop;

//		// Determine what time range we're going to print
//		int hsbval = HSB.getValue();

//		long minx = (long) Math.floor((hsbval - data.offset)
//		/ data.pixelIncrement)
//		* data.timeIncrement;
//		if (minx < 0)
//		minx = 0;
//		minx += data.beginTime;

//		long maxx = (long) Math.ceil((hsbval + data.vpw - data.offset)
//		/ data.pixelIncrement)
//		* data.timeIncrement;
//		maxx += data.beginTime;
//		if (maxx > data.endTime)
//		maxx = data.endTime;

//		// Determine the range of processors to print
//		int vsbval = VSB.getValue();
//		int miny = (int) Math.floor((double) vsbval / data.tluh);
//		int maxy = (int) Math.floor((double) (vsbval + data.vph) / data.tluh);

//		if (miny < 0)
//		miny = 0;
//		if (maxy > data.numPs - 1)
//		maxy = data.numPs - 1;

//		// Get our first page
//		Graphics pg = pjob.getGraphics();
//		pg.setColor(Color.white);
//		pg.fillRect(0, 0, d.width, d.height);
//		pg.setFont(new Font("SansSerif", Font.PLAIN, 10));
//		pg.translate(marginLeft, marginTop);
//		FontMetrics pfm = pg.getFontMetrics(pg.getFont());

//		// Figure out how many pages this thing will need
//		int textheight = pfm.getHeight();
//		int titleht = 2 * textheight + 12;
//		int axisht = 20 + textheight;
//		int footht = textheight + 5;

//		int[] entries = new int[MainWindow.runObject[myRun].getNumUserEntries()];
//		for (int i = 0; i < MainWindow.runObject[myRun].getNumUserEntries(); i++)
//		entries[i] = 0;

//		int idle = 0;
//		for (int p = miny; p <= maxy; p++) {
//		for (int o = data.tloArray[p].length - 1; o >= 0; o--) {
//		long bt = data.tloArray[p][o].getBeginTime();
//		long et = data.tloArray[p][o].getEndTime();

//		if (bt > maxx || et < minx)
//		continue;

//		int entry = data.tloArray[p][o].getEntry();
//		if (entry < 0)
//		idle = 1;
//		else
//		entries[entry] = 1;
//		}
//		}

//		int legendcount = 0;
//		for (int i = 0; i < MainWindow.runObject[myRun].getNumUserEntries(); i++)
//		{
//		if (entries[i] > 0)
//		legendcount++;
//		}

//		if (idle == 1 && data.showIdle == true)
//		legendcount += 1;

//		System.out.println("The number of items to be shown in the legend is "
//		+ legendcount);

//		int legendht = ((int) Math.ceil(legendcount / 2.0) + 2)
//		* (textheight + 2);

//		System.out.println("The legend height = " + legendht);

//		int numpgs = 1;

//		int tlht = titleht + 2 * axisht + footht;
//		for (int i = miny; i <= maxy; i++) {
//		tlht += data.tluh;
//		if (tlht > printHeight) {
//		numpgs++;
//		tlht = titleht + 2 * axisht + footht + data.tluh;
//		}
//		}

//		tlht += legendht;
//		while (tlht > printHeight) {
//		numpgs++;
//		tlht = tlht - printHeight + titleht + footht;
//		}

//		System.out.println("It will take " + numpgs + " to fit this data");

//		// Figure out the scales to print to the page.
//		int plabellen = pfm.stringWidth("Processor " + maxy);
//		int pareawidth = plabellen + 20;

//		int tlareawidth = printWidth - pareawidth;

//		int leftoffset = 5 + pfm.stringWidth("" + minx) / 2;
//		int rightoffset = 5 + pfm.stringWidth("" + maxx) / 2;
//		long length = maxx - minx + 1;
//		int width = tlareawidth - leftoffset - rightoffset;

//		int timeIncrement = (int) Math.ceil(5 / ((double) width / length));
//		timeIncrement = Util.getBestIncrement(timeIncrement);
//		int numIntervals = (int) Math.ceil(length / timeIncrement) + 1;
//		double pixelIncrement = (double) width / numIntervals;

//		int labelIncrement = (int) Math.ceil((pfm.stringWidth("" + maxx) + 10)
//		/ pixelIncrement);
//		labelIncrement = Util.getBestIncrement(labelIncrement);

//		data.processorList.reset();
//		for (int p = 0; p < miny; p++)
//		data.processorList.nextElement();

//		NumberFormat df = NumberFormat.getInstance();
//		String[][] names = MainWindow.runObject[myRun].getEntryNames();

//		int curp = miny;
//		int curlegenditem = 0;

//		boolean drawingLegend = false;

//		for (int page = 0; page < numpgs; page++) {
//		if (pg == null) {
//		pg = pjob.getGraphics();
//		pg.setFont(new Font("SansSerif", Font.PLAIN, 10));
//		pg.translate(marginLeft, marginTop);
//		}

//		int curheight = 0;

//		// DRAW THE TITLE
//		String title = "PROJECTIONS TIMELINE FOR "
//		+ MainWindow.runObject[myRun].getFilename();
//		pg.setColor(Color.black);
//		curheight += textheight;
//		pg.drawString(title, (printWidth - pfm.stringWidth(title)) / 2,
//		curheight);

//		curheight += 10;

//		// DRAW THE TOP AXIS
//		int axislength = (int) ((numIntervals - 1) * pixelIncrement);
//		int curx;
//		String tmp;

//		if (!drawingLegend) {
//		curheight += 10 + textheight;

//		pg.setColor(Color.black);
//		pg.drawLine(pareawidth + leftoffset, curheight, pareawidth
//		+ leftoffset + axislength, curheight);

//		for (int x = 0; x < numIntervals; x++) {
//		curx = pareawidth + leftoffset + (int) (x * pixelIncrement);
//		if (curx > maxx)
//		break;

//		if (x % labelIncrement == 0) {
//		tmp = "" + (minx + x * timeIncrement);
//		pg.drawLine(curx, curheight - 5, curx, curheight + 5);
//		pg.drawString(tmp, curx - pfm.stringWidth(tmp) / 2,
//		curheight - 10);
//		} else {
//		pg.drawLine(curx, curheight - 2, curx, curheight + 2);
//		}
//		}
//		}

//		// Draw the processor info
//		curheight += 10;

//		int axisheight;
//		for (int p = curp; p <= maxy; p++) {
//		if (curheight + data.tluh + axisht + footht > printHeight)
//		break;

//		curp++;
//		curheight += data.tluh;
//		axisheight = curheight - data.tluh / 2;
//		pg.setColor(Color.black);
//		tmp = "Processor " + data.processorList.nextElement();
//		pg.drawString(tmp, 10, axisheight);

//		pg.setColor(Color.gray);
//		tmp = "(" + df.format(data.processorUsage[p]) + "%)";
//		pg.drawString(tmp, 20, axisheight + pfm.getHeight() + 2);

//		pg.setColor(Color.gray);
//		pg.drawLine(pareawidth + leftoffset, axisheight, pareawidth
//		+ leftoffset + axislength, axisheight);

//		for (int o = data.tloArray[p].length - 1; o >= 0; o--) {
//		long bt = data.tloArray[p][o].getBeginTime();
//		long et = data.tloArray[p][o].getEndTime();

//		if (bt > maxx || et < minx)
//		continue;

//		int xpos = (int) ((bt - minx) * pixelIncrement / timeIncrement);
//		if (bt < minx)
//		xpos = -5;

//		pg.translate(pareawidth + leftoffset + xpos,
//		axisheight - 10);

//		data.tloArray[p][o].print(pg, minx, maxx, pixelIncrement,
//		timeIncrement);

//		pg.translate(-(pareawidth + leftoffset + xpos),
//		-(axisheight - 10));
//		}

//		}

//		// Draw the bottom axis

//		if (!drawingLegend) {
//		curheight += 10;
//		pg.setColor(Color.black);
//		pg.drawLine(pareawidth + leftoffset, curheight, pareawidth
//		+ leftoffset + axislength, curheight);

//		for (int x = 0; x < numIntervals; x++) {
//		curx = pareawidth + leftoffset + (int) (x * pixelIncrement);

//		if (curx > maxx)
//		break;

//		if (x % labelIncrement == 0) {
//		tmp = "" + (minx + x * timeIncrement);
//		pg.drawLine(curx, curheight - 5, curx, curheight + 5);
//		pg.drawString(tmp, curx - pfm.stringWidth(tmp) / 2,
//		curheight + 10 + pfm.getHeight());
//		} else {
//		pg.drawLine(curx, curheight - 2, curx, curheight + 2);
//		}
//		}

//		curheight += (10 + textheight);
//		}

//		// Draw the legend
//		if (curp > maxy) {
//		curheight += (10 + textheight);
//		drawingLegend = true;
//		pg.setColor(Color.black);
//		String s = "LEGEND";
//		pg.drawString(s, (printWidth - pfm.stringWidth(s)) / 2,
//		curheight);
//		curheight += 2 * textheight;

//		int textx = 0;
//		for (int i = curlegenditem; i < entries.length; i++) {
//		curlegenditem++;
//		if (entries[i] > 0) {
//		pg.setColor(data.entryColor[i]);
//		s = names[i][1] + "::" + names[i][0];
//		pg.drawString(s, textx, curheight);

//		if (textx == 0) {
//		textx = (int) (printWidth / 2.0);

//		// Java seems to have a problem with the division in
//		// the following line!
//		// System.out.println("PW=" + printWidth + " PW/2="
//		// + (printWidth/2));
//		} else {
//		textx = 0;
//		curheight += (textheight + 2);
//		if (curheight + footht > printHeight)
//		break;
//		}
//		}
//		}
//		}
//		// Draw the footer
//		curheight = printHeight;
//		pg.setColor(Color.black);
//		String footer = "Page " + (page + 1) + " of " + numpgs;
//		pg.drawString(footer, (printWidth - pfm.stringWidth(footer)) / 2,
//		curheight);

//		// Send the page to be printed
//		pg.dispose();
//		pg = null;
//		}

//		pjob.end();
	}


	private void ShowColorWindow() {
		if (colorWindow == null)
			colorWindow = new ColorChooser(parentWindow, data, parentWindow);
		colorWindow.setVisible(true);
	}

	public void CloseColorWindow() {
		colorWindow = null;
	}

	/** Update the value in the scale factor label with the value in our data object */
	public void updateScaleField() {
		scaleField.setText("" + data.getScaleFactor());
	}

}
