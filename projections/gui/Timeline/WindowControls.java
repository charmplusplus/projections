package projections.gui.Timeline;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.*;

import projections.gui.FloatJTextField;
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
	private JButton bDecrease, bIncrease, bReset;

	private JButton bZoomSelected, bLoadSelected;

	private JTextField highlightTime, selectionBeginTime, selectionEndTime,
	selectionDiff;

	private DecimalFormat format;

	private FloatJTextField scaleField;

	private JCheckBox cbPacks, cbMsgs, cbIdle, cbUser, cbUserTable;
	
	private JCheckBoxMenuItem cbTraceMessages, cbTraceArrayElementID;
	
	
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

		userEventWindow = new UserEventWindow(cbUserTable);

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
						cbUserTable.setText("View User Events (" + data.getNumUserEvents() + ")");
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
			
			cbUserTable.setText("View User Events (" + data.getNumUserEvents() + ")");
			
		} else{
			System.out.println("ERROR: somehow you clicked the loadSelected button which shouldn't have been enabled!");			
		}
	}

	public void actionPerformed(ActionEvent evt) {
		
		// If the event is a menu action
		
		if (evt.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem) evt.getSource()).getText();
			if (arg.equals("Close"))
				parentWindow.close();
			
			else if (arg.equals("Modify Ranges"))
				showDialog();
			
			else if (arg.equals("Change Entry Point Colors")) 
				ShowColorWindow();
			
			else if (arg.equals("Save Entry Point Colors")) 
				MainWindow.runObject[myRun].saveColors();
			
			else if (arg.equals("Restore Entry Point Colors")) {
				try {
					Util.restoreColors(data.entryColor(), "Timeline Graph");
					parentWindow.refreshDisplay(false);
				} catch (Exception e) {
					System.err.println("Attempt to read from color.map failed");
				}
			} 
			
			else if (arg.equals("Default Entry Point Colors")) {
				for (int i = 0; i < data.entryColor().length; i++)
					data.entryColor()[i] = MainWindow.runObject[myRun].getEntryColor(i);
				parentWindow.refreshDisplay(false);
			} 
			
			else if (arg.equals("Save as JPG or PNG")) {
				try{
					SaveImage p = new SaveImage(parentWindow.scrollingPanel);

					// Create a file chooser so the user can choose where to save the image
					JFileChooser fc = new JFileChooser();
					ImageFilter imageFilter = new ImageFilter();
					fc.setFileFilter(imageFilter);
					fc.setSelectedFile(new File("./TimelineScreenshot.png"));

					int returnVal = fc.showSaveDialog(parentWindow);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();

						if(imageFilter.isJPEG(file))       		
							p.saveImageJPEG(file.getCanonicalPath());

						if(imageFilter.isPNG(file))       		
							p.saveImagePNG(file.getCanonicalPath());

					} else {
						// Save command cancelled by user
					}
				} catch (IOException e){
					JOptionPane.showMessageDialog(this, "Error occurred while saving file:" + e.getLocalizedMessage());
				}
			}

			
			else if (arg.equals("Color by Default"))
				data.setColorByDefault();

			else if (arg.equals("Color by Event Index")) 
				data.setColorByIndex();

			else if (arg.equals("Color by User Supplied Parameter(timestep)")) 
				data.setColorByUserSupplied();

			else if (arg.equals("Color by Memory Usage"))
				data.setColorByMemoryUsage();

		}

		
		// If the event is a JButton
		
		if (evt.getSource() instanceof JButton) {
			JButton b = (JButton) evt.getSource();
			
			
			if (b == bZoomSelected) {
				zoomSelected();
				parentWindow.refreshDisplay(true);
			} 

			else if (b == bLoadSelected) {
				loadSelected();
				parentWindow.refreshDisplay(true);
			} 

			else if (b == bDecrease) {
				data.keepViewCentered(true); // Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
				data.decreaseScaleFactor();
				scaleField.setText("" + data.getScaleFactor());
				parentWindow.refreshDisplay(true);
			} 

			else if (b == bIncrease) {
				data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
				data.increaseScaleFactor();
				scaleField.setText("" + data.getScaleFactor());
				parentWindow.refreshDisplay(true);
			} 

			else if (b == bReset) {
				data.setScaleFactor(1.0f);
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
//		JMenuItem i1 = new JMenuItem("Print Timeline");
//		i1.setEnabled(false);
		JMenuItem i3 = new JMenuItem("Close");
//		fileMenu.add(i1);
//		fileMenu.addSeparator();
		fileMenu.add(i3);
//		i1.addActionListener(this);
		i3.addActionListener(this);
		mbar.add(fileMenu);

		JMenu toolsMenu = new JMenu("Tools");
		JMenuItem i4 = new JMenuItem("Modify Ranges");
		toolsMenu.add(i4);
		i4.addActionListener(this);
		mbar.add(toolsMenu);


		JMenu saveMenu = new JMenu("Screenshot");
		JMenuItem i9 = new JMenuItem("Save as JPG or PNG");
		saveMenu.add(i9);
		i9.addActionListener(this);
		mbar.add(saveMenu);
		
		JMenu colorMenu = new JMenu("Colors");
		
		JMenuItem i5 = new JMenuItem("Change Entry Point Colors");
		JMenuItem i6 = new JMenuItem("Save Entry Point Colors");
		JMenuItem i7 = new JMenuItem("Restore Entry Point Colors");
		JMenuItem i8 = new JMenuItem("Default Entry Point Colors");
		
		JMenuItem i10 = new JMenuItem("Color by Default");
		JMenuItem i11 = new JMenuItem("Color by Event Index");
		JMenuItem i12 = new JMenuItem("Color by User Supplied Parameter(timestep)");
		JMenuItem i13 = new JMenuItem("Color by Memory Usage");

		
		colorMenu.add(i5);
		colorMenu.add(i6);
		colorMenu.add(i7);
		colorMenu.add(i8);
		colorMenu.addSeparator();
		colorMenu.add(i10);
		colorMenu.add(i11);
		colorMenu.add(i12);
		colorMenu.add(i13);
		
		i5.addActionListener(this);
		i6.addActionListener(this);
		i7.addActionListener(this);
		i8.addActionListener(this);
		i10.addActionListener(this);
		i11.addActionListener(this);
		i12.addActionListener(this);
		i13.addActionListener(this);

		
		mbar.add(colorMenu);
		
		
		// Tracing menu
		JMenu tracingMenu = new JMenu("Tracing");
		
		cbTraceMessages = new JCheckBoxMenuItem("Trace Messages");
		cbTraceArrayElementID = new JCheckBoxMenuItem("Trace Event ID(Chare Array Index)");

		tracingMenu.add(cbTraceMessages);
		tracingMenu.add(cbTraceArrayElementID);
		
		cbTraceMessages.addItemListener(this);
		cbTraceArrayElementID.addItemListener(this);

		mbar.add(tracingMenu);
	
		
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
		cbUser = new JCheckBox("Display User Events", true);
		cbUserTable = new JCheckBox("Display User Events Window", false);

	
		cbPacks.addItemListener(this);
		cbMsgs.addItemListener(this);
		cbIdle.addItemListener(this);
		cbUser.addItemListener(this);
		cbUserTable.addItemListener(this);
		
				
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
		Util.gblAdd(cbPanel, cbUserTable, gbc, 4, 0, 1, 1, 1, 1);
		
		// BUTTON PANEL
		
		URL zoomInURL = ((Object)this).getClass().getResource("/projections/images/ZoomIn24.gif");
		URL zoomOutURL = ((Object)this).getClass().getResource("/projections/images/ZoomOut24.gif");
		
		bDecrease = new JButton(new ImageIcon(zoomOutURL));
		bIncrease = new JButton(new ImageIcon(zoomInURL));
		
		bReset = new JButton("Reset Zoom");

		bDecrease.addActionListener(this);
		bIncrease.addActionListener(this);
		bReset.addActionListener(this);

		JLabel lScale = new JLabel("Zoom Ratio: ", JLabel.CENTER);
		scaleField = new FloatJTextField(data.getScaleFactor(), 5);
		scaleField.addActionListener(this);
		
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(gbl);

		gbc.fill = GridBagConstraints.BOTH;

		Util.gblAdd(buttonPanel, bDecrease, gbc, 3, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, lScale, gbc, 4, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, scaleField, gbc, 5, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, bIncrease, gbc, 6, 0, 1, 1, 1, 1);
		Util.gblAdd(buttonPanel, bReset, gbc, 7, 0, 1, 1, 1, 1);

		// ZOOM PANEL

		bZoomSelected = new JButton("Zoom Selection");
		bLoadSelected = new JButton("Load Selection");

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
		Util.gblAdd(zoomPanel, new JLabel("Time At Mouse Cursor", JLabel.CENTER),
				gbc, 2, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel,
				new JLabel("Selection Begin Time", JLabel.CENTER), gbc, 3, 1,
				1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection End Time", JLabel.CENTER),
				gbc, 4, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection Length", JLabel.CENTER),
				gbc, 5, 1, 1, 1, 1, 1);

		
		this.setLayout(gbl);
		Util.gblAdd(this, cbPanel, gbc, 0, 1, 1, 1, 1, 0);
//		Util.gblAdd(this, cbPanel2, gbc, 0, 2, 1, 1, 1, 0);
		Util.gblAdd(this, buttonPanel, gbc, 0, 3, 1, 1, 1, 0);
		Util.gblAdd(this, zoomPanel, gbc, 0, 4, 1, 1, 1, 0);

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

		else if (c == cbTraceMessages)
			data.setTraceMessagesOnHover(evt.getStateChange() == ItemEvent.SELECTED);
				
		else if (c == cbTraceArrayElementID)
			data.setTraceOIDOnHover(evt.getStateChange() == ItemEvent.SELECTED);
				
		else if (c == cbUser)
			data.showUserEvents(evt.getStateChange() == ItemEvent.SELECTED);
		
		else if (c == cbUserTable) {
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
