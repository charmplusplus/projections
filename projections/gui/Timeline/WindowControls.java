package projections.gui.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.Toolkit;
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

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private TimelineWindow parentWindow;

	private ColorChooser colorWindow;

	Data data;

	// basic zoom controls
	private JButton bDecrease, bIncrease, bReset;

	private JButton bZoomSelected, bLoadSelected, bRanges;

	private JTextField highlightTime, selectionBeginTime, selectionEndTime,	selectionDiff;

	private DecimalFormat format;

	private FloatJTextField scaleField;

	private JCheckBox cbPacks, cbMsgs, cbIdle, cbUser, cbUserTable;

	private JCheckBoxMenuItem cbTraceMessages, cbTraceMessagesForward, cbTraceArrayElementID, cbCompactView, cbNestedUserEvents;

	private UserEventWindow userEventWindow;

	private JMenuItem mClose;
	private JMenuItem mModifyRanges;

	private JMenuItem mSaveScreenshot;
	private JMenuItem mSaveFullTimeline;
	private JMenuItem mSaveFullTimelineWhiteBG;
	
	private JMenuItem mSelectBGColor;
	private JMenuItem mSelectFGColor;

	private JMenuItem mChangeColors;
	private JMenuItem mSaveColors;
	private JMenuItem mRestoreColors;
	private JMenuItem mDefaultColors;

	private JMenuItem mColorByDefault;
	private JMenuItem mColorByObjectID;
	private JMenuItem mColorByUserRandom;
	private JMenuItem mColorByUserGradient;
	private JMenuItem mColorByUserObjRandom;
	private JMenuItem mColorByUserEIDRandom;
	private JMenuItem mColorByMemUsage;

	private JMenuItem mShiftTimelines;
	private JMenuItem mUserEventReport;
	private JMenuItem mDetermineTimeRangesUserSupplied;
	private JMenuItem mShowHideEntries;
	private JMenuItem mShowHideUserEvents;
	
	private JCheckBoxMenuItem cbDontLoadMessages;
	private JCheckBoxMenuItem cbDontLoadIdle;
	

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
	

	
	public JCheckBox dialogEnableEntryFiltering;
	public JTextField dialogMinEntryFiltering;
	public JCheckBox dialogEnableIdleFiltering;
	public JCheckBox dialogEnableMsgFiltering;
	
	/** Create a panel of input items specific to the timeline tool */
	public JPanel toolSpecificDialogPanel(){
		JPanel p = new JPanel();
	    p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
	    
	    // Create a JPanel for filtering out small entry methods
	    JPanel p1 = new JPanel();
	    p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
	    dialogEnableEntryFiltering = new JCheckBox();
	    p1.add(dialogEnableEntryFiltering);
	    p1.add(new JLabel("Filter out entries shorter than"));
	    dialogMinEntryFiltering = new JTextField(7);
	    dialogMinEntryFiltering.setText("30");
		dialogMinEntryFiltering.setEditable(false);
	    p1.add(dialogMinEntryFiltering);
	    p1.add(new JLabel("us"));
	    p1.add(Box.createHorizontalStrut(200)); // Add some empty space so that the textbox isn't huge
	    p1.add(Box.createHorizontalGlue());
	    dialogEnableEntryFiltering.addItemListener(this);

	    // Create a JPanel for filtering out idle time
	    JPanel p2 = new JPanel();
	    p2.setLayout(new BorderLayout());
	    dialogEnableIdleFiltering = new JCheckBox();
	    p2.add(dialogEnableIdleFiltering, BorderLayout.WEST);
	    p2.add(new JLabel("Filter out idle time regions"), BorderLayout.CENTER);
	    
	    // Create a JPanel for filtering out messages
	    JPanel p3 = new JPanel();
	    p3.setLayout(new BorderLayout());
	    dialogEnableMsgFiltering = new JCheckBox();
	    p3.add(dialogEnableMsgFiltering, BorderLayout.WEST);
	    p3.add(new JLabel("Filter out messages"), BorderLayout.CENTER);
	    
	    // Put the various rows into the panel
	    p.add(p1);
	    p.add(p2);
	    p.add(p3);

		return p;
	}

	
	
	public void showDialog() {

		if (parentWindow.dialog == null) {
			JPanel toolSpecificPanel = toolSpecificDialogPanel();
			
			parentWindow.dialog = new RangeDialog(parentWindow,
			"Select Timeline Range", toolSpecificPanel);
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
						
						parentWindow.data.skipLoadingIdleRegions(dialogEnableIdleFiltering.isSelected(), false);
						parentWindow.data.skipLoadingMessages(dialogEnableMsgFiltering.isSelected(), false);
						if(dialogEnableEntryFiltering.isSelected()){
							String s = dialogMinEntryFiltering.getText();
							data.setFilterEntryShorterThan(Integer.parseInt(s));
						}
						
						parentWindow.mainPanel.loadTimelineObjects(true, null);
						cbUserTable.setText("View User Events (" + data.getNumUserEvents() + ")");
						return null;
					}

					public void finished() {
						// Here we are basically at startup after the dialog window and the trace log has been read
						//						parentWindow.setSize(1000, 600);
						parentWindow.setSize(Toolkit.getDefaultToolkit().getScreenSize());
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
			// Set scroll to the place we want
			data.setPreferredViewTimeCenter((selectionStartTime+selectionEndTime) / 2.0);
			data.setScaleFactor( (float) ((data.endTime() - data.beginTime()) / (selectionEndTime -
					selectionStartTime)) );
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

			parentWindow.mainPanel.loadTimelineObjects(true, null);

			cbUserTable.setText("View User Events (" + data.getNumUserEvents() + ")");

		} else{
			System.out.println("ERROR: somehow you clicked the loadSelected button which shouldn't have been enabled!");			
		}
	}

	public void actionPerformed(ActionEvent evt) {

		// If the event is a menu action

		if(evt.getSource() == mClose)
			parentWindow.close();

		else if(evt.getSource() == mModifyRanges)
			showDialog();
		
		else if(evt.getSource() == mSaveFullTimeline){
			SaveImage p = new SaveImage();
			// Create a blank panel to put in the upper left position. The timeline tool currently only maintains the other three panels that are displayed.
			SolidColorJPanel upperLeftPanel = new SolidColorJPanel(data.getBackgroundColor(), parentWindow.labelPanel.getWidth(), parentWindow.axisPanel.getHeight() );
			// Create a panel that is rendered from the four panels we supply
			Render2by2PanelGrid gridPanel = new Render2by2PanelGrid(upperLeftPanel, parentWindow.axisPanel, parentWindow.labelPanel, parentWindow.mainPanel);
			// Save it to a file which is chosen by the user
			p.saveToFileChooserSelection(gridPanel);		
		}

		else if(evt.getSource() == mSaveFullTimelineWhiteBG){
			Color oldBG = data.getBackgroundColor();
			Color oldFG = data.getForegroundColor();
			data.setForegroundColor(Color.black);
			data.setBackgroundColor(Color.white);
				
			SaveImage p = new SaveImage();
			// Create a blank panel to put in the upper left position. The timeline tool currently only maintains the other three panels that are displayed.
			SolidColorJPanel upperLeftPanel = new SolidColorJPanel(data.getBackgroundColor(), parentWindow.labelPanel.getWidth(), parentWindow.axisPanel.getHeight() );
			// Create a panel that is rendered from the four panels we supply
			Render2by2PanelGrid gridPanel = new Render2by2PanelGrid(upperLeftPanel, parentWindow.axisPanel, parentWindow.labelPanel, parentWindow.mainPanel);
			// Save it to a file which is chosen by the user
			p.saveToFileChooserSelection(gridPanel);

			data.setForegroundColor(oldFG);
			data.setBackgroundColor(oldBG);
		
		}
		

		else if(evt.getSource() == mSaveScreenshot){
			SaveImage p = new SaveImage();
			p.saveToFileChooserSelection(parentWindow.scrollingPanel);
		}

		else if(evt.getSource() == mSelectBGColor)
			selectBackgroundColor();

		else if(evt.getSource() == 	mSelectFGColor)
			selectForegroundColor();

		else if(evt.getSource() == mChangeColors)
			ShowColorWindow();

		else if(evt.getSource() == mSaveColors)
			MainWindow.runObject[myRun].saveColors();

		else if(evt.getSource() == mRestoreColors){
			try {
				Util.restoreColors(data.entryColor(), "Timeline Graph");
				parentWindow.refreshDisplay(false);
			} catch (Exception e) {
				System.err.println("Attempt to read from color.map failed");
			}	
		}

		else if(evt.getSource() == mDefaultColors){
			for (int i = 0; i < data.entryColor().length; i++)
				data.entryColor()[i] = MainWindow.runObject[myRun].getEntryColor(i);
			parentWindow.refreshDisplay(false);
		} 

		else if(evt.getSource() == mColorByDefault)
			data.setColorByDefault();

		else if(evt.getSource() == mColorByObjectID)
			data.setColorByObjectID();

		else if(evt.getSource() == mColorByUserRandom)
			data.setColorByUserSupplied(Data.RandomColors);

		else if(evt.getSource() == mColorByUserGradient)
			data.setColorByUserSupplied(Data.BlueGradientColors);

		else if(evt.getSource() == mColorByUserObjRandom)
			data.setColorByUserSuppliedAndObjID(Data.RandomColors);

		else if(evt.getSource() == mColorByUserEIDRandom)
			data.setColorByUserSuppliedAndEID(Data.RandomColors);
		
		else if(evt.getSource() == mColorByMemUsage)
			data.setColorByMemoryUsage();

		else if(evt.getSource() == mShiftTimelines)
			data.fixTachyons();
		
		else if(evt.getSource() == 	mShowHideEntries){
			ChooseEntriesWindow chooseEntriesWindow = new ChooseEntriesWindow(data);
		}
		
		else if(evt.getSource() == 	mShowHideUserEvents){
			ChooseUserEventsWindow chooseUserEventsWindow = new ChooseUserEventsWindow(data);
		}

		else if(evt.getSource() == mUserEventReport){
			data.printUserEventInfo();
		}
		
		else if(evt.getSource() == mDetermineTimeRangesUserSupplied){
			UserSuppliedAnalyzer usa = new UserSuppliedAnalyzer(data);
		}

		else if (evt.getSource()  == bZoomSelected) {
			zoomSelected();
		} 

		else if (evt.getSource()  == bRanges) {
			showDialog();
		}

		else if (evt.getSource()  == bLoadSelected) {
			loadSelected();
			parentWindow.refreshDisplay(true);
		} 

		else if (evt.getSource()  == bDecrease) {
			data.keepViewCentered(true); // Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
			data.decreaseScaleFactor();
			scaleField.setText("" + data.getScaleFactor());
			parentWindow.refreshDisplay(true);
		} 

		else if (evt.getSource()  == bIncrease) {
			data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
			data.increaseScaleFactor();
			scaleField.setText("" + data.getScaleFactor());
			parentWindow.refreshDisplay(true);
		} 

		else if (evt.getSource()  == bReset) {
			data.setScaleFactor(1.0f);
			scaleField.setText("" + data.getScaleFactor());
			parentWindow.refreshDisplay(true);
		}



		// If the action corresponds to the scale value changing(likely typed in)
		else if (evt.getSource() == scaleField) {
			data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
			data.setScaleFactor(scaleField.getValue());
			parentWindow.refreshDisplay(true);
		}

	}









	public void CreateMenus() {
		JMenuBar mbar = new JMenuBar();

		// File Menu
		JMenu fileMenu = new JMenu("File");
		mClose = new JMenuItem("Close");
		mClose.addActionListener(this);
		fileMenu.add(mClose);
		mbar.add(fileMenu);

		// Ranges Menu
		JMenu toolsMenu = new JMenu("Ranges");
		mModifyRanges = new JMenuItem("Modify Ranges");
		mModifyRanges.addActionListener(this);
		toolsMenu.add(mModifyRanges);
		mbar.add(toolsMenu);

		// Screenshot Menu
		JMenu saveMenu = new JMenu("Screenshot");
		mSaveScreenshot = new JMenuItem("Save Visible Screen as JPG or PNG");
		mSaveScreenshot.addActionListener(this);
		saveMenu.add(mSaveScreenshot);
		mSaveFullTimeline = new JMenuItem("Save All PE Timelines as JPG or PNG");
		mSaveFullTimeline.addActionListener(this);
		saveMenu.add(mSaveFullTimeline);
		mbar.add(saveMenu);
		mSaveFullTimelineWhiteBG = new JMenuItem("Save All PE Timelines as JPG or PNG on White Background");
		mSaveFullTimelineWhiteBG.addActionListener(this);
		saveMenu.add(mSaveFullTimelineWhiteBG);

		
		
		// Color Menu
		JMenu colorMenu = new JMenu("Colors");

		mSelectBGColor = new JMenuItem("Select Background Color");
		mSelectFGColor = new JMenuItem("Select Foreground Color");

		mChangeColors = new JMenuItem("Change Entry Point Colors");
		mSaveColors = new JMenuItem("Save Entry Point Colors");
		mRestoreColors = new JMenuItem("Restore Entry Point Colors");
		mDefaultColors = new JMenuItem("Default Entry Point Colors");

		mColorByDefault = new JMenuItem("Color by Default");
		mColorByObjectID = new JMenuItem("Color by Object Index");
		mColorByUserRandom = new JMenuItem("Color by User Supplied Parameter(timestep) with Disjoint Colors");
		mColorByUserGradient = new JMenuItem("Color by User Supplied Parameter(timestep) with Gradient");
		mColorByUserObjRandom = new JMenuItem("Color by User Supplied Parameter(timestep) + Object ID with Disjoint Colors");
		mColorByUserEIDRandom =  new JMenuItem("Color by User Supplied Parameter(timestep) + Entry ID with Disjoint Colors");
		mColorByMemUsage = new JMenuItem("Color by Memory Usage ...");

		colorMenu.add(mSelectBGColor);
		colorMenu.add(mSelectFGColor);
		colorMenu.addSeparator();
		colorMenu.add(mChangeColors);
		colorMenu.add(mSaveColors);
		colorMenu.add(mRestoreColors);
		colorMenu.add(mDefaultColors);
		colorMenu.addSeparator();
		colorMenu.add(mColorByDefault);
		colorMenu.add(mColorByObjectID);
		colorMenu.add(mColorByUserRandom);
		colorMenu.add(mColorByUserGradient);
		colorMenu.add(mColorByUserObjRandom);
		colorMenu.add(mColorByUserEIDRandom);
		colorMenu.add(mColorByMemUsage);


		mSelectBGColor.addActionListener(this);
		mSelectFGColor.addActionListener(this);
		mChangeColors.addActionListener(this);
		mSaveColors.addActionListener(this);
		mRestoreColors.addActionListener(this);
		mDefaultColors.addActionListener(this);
		mColorByDefault.addActionListener(this);
		mColorByObjectID.addActionListener(this);
		mColorByUserRandom.addActionListener(this);
		mColorByUserObjRandom.addActionListener(this);
		mColorByUserGradient.addActionListener(this);
		mColorByMemUsage.addActionListener(this);
		mColorByUserEIDRandom.addActionListener(this);
		
		mbar.add(colorMenu);


		// Tracing menu
		JMenu tracingMenu = new JMenu("Tracing");

		cbTraceMessages = new JCheckBoxMenuItem("Trace Messages Back");
		cbTraceMessagesForward = new JCheckBoxMenuItem("Trace Messages Forward");
		cbTraceArrayElementID = new JCheckBoxMenuItem("Trace Event ID(Chare Array Index)");

		tracingMenu.add(cbTraceMessages);
		tracingMenu.add(cbTraceMessagesForward);
		tracingMenu.add(cbTraceArrayElementID);

		cbTraceMessages.addItemListener(this);
		cbTraceMessagesForward.addItemListener(this);
		cbTraceArrayElementID.addItemListener(this);

		mbar.add(tracingMenu);

		// View Menu
		JMenu viewMenu = new JMenu("View");

		cbCompactView = new JCheckBoxMenuItem("Compact View");
		cbCompactView.addItemListener(this);
		viewMenu.add(cbCompactView);
		
		mShowHideEntries = new JMenuItem("Show & Hide Entry Methods");
		mShowHideEntries.addActionListener(this);
		viewMenu.add(mShowHideEntries);
		
		mShowHideUserEvents = new JMenuItem("Show & Hide User Events");
		mShowHideUserEvents.addActionListener(this);
		viewMenu.add(mShowHideUserEvents);
		

		cbNestedUserEvents = new JCheckBoxMenuItem("Show Nested Bracketed User Events");
		cbNestedUserEvents.addItemListener(this);
		viewMenu.add(cbNestedUserEvents);

		mbar.add(viewMenu);

		// Experimental Features Menu
		JMenu experimentalMenu = new JMenu("Experimental Features");
		mShiftTimelines = new JMenuItem("Shift Timelines to fix inconsistent clocks");
		mShiftTimelines.addActionListener(this);
		experimentalMenu.add(mShiftTimelines);

		mUserEventReport = new JMenuItem("User Event Reporting");
		mUserEventReport.addActionListener(this);
		experimentalMenu.add(mUserEventReport);
	

		mDetermineTimeRangesUserSupplied = new JMenuItem("Determine Time Ranges for User Supplied Values");
		mDetermineTimeRangesUserSupplied.addActionListener(this);
		experimentalMenu.add(mDetermineTimeRangesUserSupplied);

		
		
		JMenu submenu = new JMenu("Speed/Memory Enhancements:");
		cbDontLoadMessages = new JCheckBoxMenuItem("Don't Load messages");
		cbDontLoadMessages.setSelected(false);
		cbDontLoadMessages.addItemListener(this);
		submenu.add(cbDontLoadMessages);
		cbDontLoadIdle = new JCheckBoxMenuItem("Don't Load Idle Time Blocks");
		cbDontLoadIdle.setSelected(false);
		cbDontLoadIdle.addItemListener(this);
		submenu.add(cbDontLoadIdle);
		experimentalMenu.add(submenu);

		
		mbar.add(experimentalMenu);
		parentWindow.setJMenuBar(mbar);
	}

	public void userEventWindowSetData(){
		userEventWindow.setData(data);
	}


	private void CreateLayout() {

		// // CHECKBOX PANEL
		cbPacks = new JCheckBox("Display Pack Times", data.showPacks());
		cbMsgs = new JCheckBox("Display Message Sends", data.showMsgs());
		cbIdle = new JCheckBox("Display Idle Time", data.showIdle());
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
		bRanges = new JButton("Load New Time/PE Range");

		// Ideally we would enable and disable the appropriate buttons, but this takes too long on some jvms
		bZoomSelected.setEnabled(false);
		bLoadSelected.setEnabled(false);
		bRanges.setEnabled(true);

		bZoomSelected.addActionListener(this);
		bLoadSelected.addActionListener(this);
		bRanges.addActionListener(this);


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

		Util.gblAdd(zoomPanel, new JLabel(" "),    gbc, 0, 0, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, bZoomSelected,      gbc, 0, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, bLoadSelected,      gbc, 1, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, highlightTime,      gbc, 2, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, selectionBeginTime, gbc, 3, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, selectionEndTime,   gbc, 4, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, selectionDiff,      gbc, 5, 2, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, bRanges,            gbc, 0, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Time At Mouse Cursor", JLabel.CENTER), gbc, 2, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection Begin Time", JLabel.CENTER), gbc, 3, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection End Time", JLabel.CENTER),	  gbc, 4, 1, 1, 1, 1, 1);
		Util.gblAdd(zoomPanel, new JLabel("Selection Length", JLabel.CENTER),     gbc, 5, 1, 1, 1, 1, 1);


		this.setLayout(gbl);
		Util.gblAdd(this, cbPanel, gbc, 0, 1, 1, 1, 1, 0);
		Util.gblAdd(this, buttonPanel, gbc, 0, 3, 1, 1, 1, 0);
		Util.gblAdd(this, zoomPanel, gbc, 0, 4, 1, 1, 1, 0);

	}


	public void itemStateChanged(ItemEvent evt) {
		
		if(evt.getSource() == dialogEnableEntryFiltering){
			dialogMinEntryFiltering.setEditable(dialogEnableEntryFiltering.isSelected());
			return;
		}
		
		
		if (data == null)
			return;

		Object c = evt.getItemSelectable();

		if (c == cbPacks)
			data.showPacks(evt.getStateChange() == ItemEvent.SELECTED);

		else if (c == cbMsgs)
			data.showMsgs(evt.getStateChange() == ItemEvent.SELECTED);

		else if (c == cbIdle)
			data.showIdle(evt.getStateChange() == ItemEvent.SELECTED);

		else if (c == cbTraceMessages)
			data.setTraceMessagesBackOnHover(evt.getStateChange() == ItemEvent.SELECTED);

		else if (c == cbTraceMessagesForward)
			data.setTraceMessagesForwardOnHover(evt.getStateChange() == ItemEvent.SELECTED);

		else if(c == cbCompactView)
			data.setCompactView(evt.getStateChange() == ItemEvent.SELECTED);

		else if(c == cbNestedUserEvents)
			data.showNestedUserEvents(evt.getStateChange() == ItemEvent.SELECTED);

		else if(c == cbDontLoadIdle)
			data.skipLoadingIdleRegions(evt.getStateChange() == ItemEvent.SELECTED, true);

		else if(c == cbDontLoadMessages)
			data.skipLoadingMessages(evt.getStateChange() == ItemEvent.SELECTED, true);

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

	private void selectBackgroundColor(){
		Color c = JColorChooser.showDialog(parentWindow, "Choose Background Color", data.getBackgroundColor()); 
		data.setBackgroundColor(c);
	}

	private void selectForegroundColor(){
		Color c = JColorChooser.showDialog(parentWindow, "Choose Foreground Color", data.getForegroundColor()); 
		data.setForegroundColor(c);
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
