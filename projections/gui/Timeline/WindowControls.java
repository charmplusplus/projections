package projections.gui.Timeline;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;
import javax.swing.*;

import projections.gui.FloatJTextField;
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

	private FloatJTextField scaleField;

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

		userEventWindow = new UserEventWindow(cbUser);

	}

	public void showDialog() {

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
						parentWindow.mainPanel.loadTimelineObjects();
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

	public void setHighlightTime(double time) {
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
		if (data.selectionValid()) {
			double selectionStartTime = data.leftSelectionTime();
			double selectionEndTime = data.rightSelectionTime();

			unsetSelectedTime();
					
			data.setScaleFactor( (float) ((data.endTime() - data.beginTime()) / (selectionEndTime -
					selectionStartTime)) );
				
			// Set scroll to the place we want
			data.setPreferredViewTimeCenter((selectionStartTime+selectionEndTime) / 2.0);
		}
	}

	/** Load a new time region */
	public void loadSelected() {
		if (data.selectionValid()) {
			
	
			double startTime = data.leftSelectionTime();
			double endTime = data.rightSelectionTime();
			
			data.invalidateSelection();
			unsetSelectedTime();
						
			if (startTime < data.beginTime()) { // This seems unlikely to happen
				startTime = data.beginTime();
			}
			if (endTime > data.endTime()) { // This seems unlikely to happen
				endTime = data.endTime();
			}

			data.setNewRange((long)(startTime+0.5),(long)(endTime+0.5));

			scaleField.setText("" + 1.0);	

			parentWindow.mainPanel.loadTimelineObjects();
			
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
		if (evt.getSource() instanceof FloatJTextField) {
			FloatJTextField b = (FloatJTextField) evt.getSource();
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


	private void CreateLayout() {

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

		JLabel lScale = new JLabel("Scale: ", JLabel.CENTER);
		scaleField = new FloatJTextField(data.getScaleFactor(), 5);
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
