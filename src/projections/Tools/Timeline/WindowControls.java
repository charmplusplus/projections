package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import projections.gui.ChooseEntriesWindow;
import projections.gui.FloatJTextField;
import projections.gui.JPanelToImage;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.Util;

/**
 * A panel that contains all the buttons and labels and things at the bottom of
 * the timeline tool. It also handles all the events.
 * 
 */
class WindowControls extends JPanel implements ActionListener,
ItemListener {

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	private TimlineRangeDialogExtension toolSpecificDialogPanel;
	private RangeDialog dialog;

	private Data data;

	private TimelineWindow parentWindow;

//    private ColorChooser colorWindow;

	// basic zoom controls
	private JButton bDecrease, bIncrease, bReset;

	private JButton bZoomSelected, bLoadSelected, bRanges;

	private JTextField highlightTime, selectionBeginTime, selectionEndTime,	selectionDiff;

	private DecimalFormat format;

	private FloatJTextField scaleField;

	private JCheckBox cbPacks, cbMsgs, cbIdle, cbUser, cbUserTable;

	private JCheckBoxMenuItem cbTraceMessages, cbTraceMessagesForward, cbTraceCriticalPath, cbTraceArrayElementID, 
								cbNestedUserEvents, cbTraceCriticalPathOnClick, cbTraceForwardOnClick;
	private JRadioButtonMenuItem cbNormalView, cbCompactView, cbSuperCompactView;
	
	private UserEventWindow userEventWindow;

	private JMenuItem cbTraceArrayRemoveLines;
	
	private JMenuItem mClose;
	private JMenuItem mModifyRanges;

	private JMenuItem mSaveScreenshot;
	private JMenuItem mSaveFullTimeline;
	private JMenuItem mSaveFullTimelineWhiteBG;
	
	private JMenuItem mWhiteBG;
	private JMenuItem mBlackBG;

	private JMenuItem mColorChooser;
	private JMenuItem mSaveColors;
	private JMenuItem mRestoreColors;
	private JMenuItem mHighlightRestore;

	private JMenuItem mColorByDefault;
	private JMenuItem mColorByObjectID;
        private JMenuItem mColorByEntryMethod;
        private JMenuItem mColorByEntryMethodFrequency;
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
	

	private JMenuItem mDisplayLegend;

	private JMenuItem mDisplayTopTimesText;
	
	protected WindowControls(TimelineWindow parentWindow_,
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
	

	protected void showDialog() {
		if(dialog == null){
			toolSpecificDialogPanel = new TimlineRangeDialogExtension();    	
			dialog = new RangeDialog(parentWindow, "Select Range For Timeline", toolSpecificDialogPanel, false);
			parentWindow.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		}
		
		dialog.displayDialog();
		if (!dialog.isCancelled()) {
			
			data.setProcessorList(dialog.getSelectedProcessors());
	        data.setRange(dialog.getStartTime(),dialog.getEndTime());

			// Create a worker that will load the trace logs and then will make 
			// the main window visible after it has finished loading
			final SwingWorker worker = new SwingWorker() {
				public Object doInBackground() {

					parentWindow.data.skipLoadingMessages(toolSpecificDialogPanel.dialogEnableMsgFiltering.isSelected(), false);
					if(toolSpecificDialogPanel.dialogEnableEntryFiltering.isSelected()){
						data.setFilterEntryShorterThan(toolSpecificDialogPanel.dialogMinEntryFiltering.getValue());
					}
					parentWindow.data.skipLoadingUserEvents(toolSpecificDialogPanel.dialogEnableUserEventFiltering.isSelected());
					parentWindow.data.displayTopTimes(toolSpecificDialogPanel.dialogEnableTopTimes.isSelected());

					if(toolSpecificDialogPanel.dialogEnableTopTimes.isSelected()){
						data.amountTopTimes(toolSpecificDialogPanel.dialogAmountTopTimes.getValue());	
					}

					parentWindow.mainPanel.loadTimelineObjects(true, parentWindow, true);
					cbUserTable.setText("View " + data.getNumUserEvents() + " User Events");
					return null;
				}

				public void done() {
					// Here we are basically at startup after the dialog window and the trace log has been read

					parentWindow.refreshDisplay(true);
					parentWindow.setVisible(true);
					

					// Check for tachyons
					// long largestTachyon = data.findLargestTachyon();
					// if(largestTachyon > 20){
					// 	JLabel message = new JLabel("<html><body>There are Tachyons (messages sent backwards in time) present<br>in this data. Would you like them to be automatically corrected?<br>The longest one is of size " + largestTachyon + " us.</body></html>");
					// 	int result = JOptionPane.showConfirmDialog(null, message, "Perform Tachyon Correction?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					// 	if(result == JOptionPane.YES_OPTION){
					// 		data.fixTachyons();
					// 	}
					// }					

				}
			};
			worker.execute();
		}
	}


	public void setHighlightTime(double time) {
		highlightTime.setText(format.format(time));
	}

	protected void unsetHighlightTime() {
		highlightTime.setText("");
	}

	protected void setSelectedTime(double time1, double time2) {
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

	protected void unsetSelectedTime() {
		selectionBeginTime.setText(" ");
		selectionEndTime.setText(" ");
		selectionDiff.setText("no selection");
		bZoomSelected.setEnabled(false);
		bLoadSelected.setEnabled(false);
	}



	private void zoomSelected() {
		if (data.selectionValid()) {
			double selectionStartTime = data.leftSelectionTime();
			double selectionEndTime = data.rightSelectionTime();

			unsetSelectedTime();
			// Set scroll to the place we want
			data.setPreferredViewTimeCenter((selectionStartTime+selectionEndTime) / 2.0);
			data.setScaleFactor( (float) ((data.endTime() - data.startTime()) / (selectionEndTime -
					selectionStartTime)) );
		}
	}

	/** Load a new time region */
	private void loadSelected() {
			if (data.selectionValid()) {

				double startTime = data.leftSelectionTime();
				double endTime = data.rightSelectionTime();

				data.invalidateSelection();
				unsetSelectedTime();

				if (startTime < data.startTime()) { // This seems unlikely to happen
					startTime = data.startTime();
				}
				if (endTime > data.endTime()) { // This seems unlikely to happen
					endTime = data.endTime();
				}

				data.setNewRange((long)(startTime+0.5),(long)(endTime+0.5));

				scaleField.setText("" + 1.0);	

				parentWindow.mainPanel.loadTimelineObjects(true, parentWindow, true);

				cbUserTable.setText("View User Events (" + data.getNumUserEvents() + ")");

			} else{
				System.out.println("ERROR: somehow you clicked the loadSelected button which shouldn't have been enabled!");			
			}
	}

	public void actionPerformed(ActionEvent evt) {
		// If the event is a menu action
		Object c = evt.getSource();
		
		if(c == mClose)
			parentWindow.close();
		
		else if (c == cbTraceArrayRemoveLines)
			data.removeLines();

		else if(c == mModifyRanges) {
			showDialog();
		}
		
		else if(c == cbNormalView)
			data.setViewType(Data.ViewType.VIEW_NORMAL);
		else if(c == cbCompactView)
			data.setViewType(Data.ViewType.VIEW_COMPACT);
		else if(c == cbSuperCompactView)
			data.setViewType(Data.ViewType.VIEW_SUPERCOMPACT);
		
		else if(c == mSaveFullTimeline){
			// Create a blank panel to put in the upper left position. The timeline tool currently only maintains the other three panels that are displayed.
			SolidColorJPanel upperLeftPanel = new SolidColorJPanel(data.getBackgroundColor(), parentWindow.labelPanel.getWidth(), parentWindow.axisPanel.getHeight() );
			// Create a panel that is rendered from the four panels we supply
			Render2by2PanelGrid gridPanel = new Render2by2PanelGrid(upperLeftPanel, parentWindow.axisPanel, parentWindow.labelPanel, parentWindow.mainPanel);
			// Save it to a file which is chosen by the user
			JPanelToImage.saveToFileChooserSelection(gridPanel, "Save Timeline Image", "./TimelineScreenshot.png");		
		}

		else if(c == mSaveFullTimelineWhiteBG){
			Color oldBG = data.getBackgroundColor();
			Color oldFG = data.getForegroundColor();
			data.setForegroundColor(Color.black);
			data.setBackgroundColor(Color.white);
				
			// Create a blank panel to put in the upper left position. The timeline tool currently only maintains the other three panels that are displayed.
			SolidColorJPanel upperLeftPanel = new SolidColorJPanel(data.getBackgroundColor(), parentWindow.labelPanel.getWidth(), parentWindow.axisPanel.getHeight() );
			// Create a panel that is rendered from the four panels we supply
			Render2by2PanelGrid gridPanel = new Render2by2PanelGrid(upperLeftPanel, parentWindow.axisPanel, parentWindow.labelPanel, parentWindow.mainPanel);
			// Save it to a file which is chosen by the user
			JPanelToImage.saveToFileChooserSelection(gridPanel, "Save Timeline Image", "./TimelineScreenshot.png");

			data.setForegroundColor(oldFG);
			data.setBackgroundColor(oldBG);
		
		}
		

		else if(c == mSaveScreenshot){
			JPanelToImage.saveToFileChooserSelection(parentWindow.scrollingPanel, "Save Timeline Image", "./TimelineScreenshot.png");
		}

		else if(c == mWhiteBG){
			data.setBackgroundColor(Color.white);
			data.setForegroundColor(Color.black);
		}
		else if(c == mBlackBG){
			data.setBackgroundColor(Color.black);
			data.setForegroundColor(Color.white);
		}
		
		else if(c == mSaveColors)
			MainWindow.runObject[myRun].saveColors();

		
		else if(c == mRestoreColors){
			try {
				data.setColorByDefault();
				MainWindow.runObject[myRun].loadColors();
				data.displayMustBeRepainted();
				parentWindow.refreshDisplay(false);
				JOptionPane.showMessageDialog(null, "The colors have successfully been loaded.", "Colors Loaded", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, e.getMessage() + "\nPlease set your colors and save them.", "Error", JOptionPane.ERROR_MESSAGE);
			}	
		}
		
		
		else if(c == mColorChooser){
			new ChooseEntriesWindow(data, true, data);
		}

		else if(c == mHighlightRestore){
			data.restoreHighlights();
		}

		else if(c == mColorByDefault) {
			MainWindow.runObject[myRun].setDefaultColors();
			data.displayMustBeRepainted();
			parentWindow.refreshDisplay(false);
			data.setColorByDefault();
		}

		else if(c == mColorByObjectID)
			data.setColorByObjectID();

		else if(c == mColorByUserRandom)
			data.setColorByUserSupplied(Data.ColorScheme.RandomColors);

		else if(c == mColorByEntryMethod)
			data.setColorByEID();
		
		else if(c == mColorByEntryMethodFrequency) {
			data.setFrequencyColors();
			data.displayMustBeRepainted();
			parentWindow.refreshDisplay(false);
			data.setColorByEIDFreq();
		}

		else if(c == mColorByUserGradient)
			data.setColorByUserSupplied(Data.ColorScheme.BlueGradientColors);

		else if(c == mColorByUserObjRandom)
			data.setColorByUserSuppliedAndObjID(Data.ColorScheme.RandomColors);

		else if(c == mColorByUserEIDRandom)
			data.setColorByUserSuppliedAndEID(Data.ColorScheme.RandomColors);
		
		else if(c == mColorByMemUsage)
			data.setColorByMemoryUsage();

		else if(c == mShiftTimelines){
			data.fixTachyons();
			data.displayMustBeRedrawn();
		}
		
		else if(c == 	mShowHideEntries){
			new ChooseEntriesWindow(data, true, data);
		}
		
		else if(c == 	mShowHideUserEvents){
			new ChooseUserEventsWindow(data);
		}

		else if(c == mUserEventReport){
			data.printUserEventInfo();
		}
		
		else if(c == mDisplayLegend){
			data.displayLegend();
		}

		else if(c == mDisplayTopTimesText){
			data.displayTopTimesText();
		}
		
		else if(c == mDetermineTimeRangesUserSupplied){
			new UserSuppliedAnalyzer(data);
		}

		else if (c  == bZoomSelected) {
			zoomSelected();
		} 

		else if (c  == bRanges) {
			showDialog();
		}

		else if (c  == bLoadSelected) {
			loadSelected();
			parentWindow.refreshDisplay(true);
		} 

		else if (c  == bDecrease) {
			data.keepViewCentered(true); // Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
			data.decreaseScaleFactor();
			scaleField.setText("" + data.getScaleFactor());
			parentWindow.refreshDisplay(true);
		} 

		else if (c  == bIncrease) {
			data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
			data.increaseScaleFactor();
			scaleField.setText("" + data.getScaleFactor());
			parentWindow.refreshDisplay(true);
		} 

		else if (c  == bReset) {
			data.setScaleFactor(1.0f);
			scaleField.setText("" + data.getScaleFactor());
			parentWindow.refreshDisplay(true);
		}



		// If the action corresponds to the scale value changing(likely typed in)
		else if (c == scaleField) {
			data.keepViewCentered(true);// Instruct the layout manager(on its next layout) to keep the scrollbar in the same place
			data.setScaleFactor(scaleField.getValue());
			parentWindow.refreshDisplay(true);
		}

	}









	protected void CreateMenus() {
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
		JMenu saveMenu = new JMenu("Save To Image");
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

		mWhiteBG = new JMenuItem("White background");
		mBlackBG = new JMenuItem("Black background");
	
		mSaveColors = new JMenuItem("Save Entry Point Colors");
		mRestoreColors = new JMenuItem("Restore Entry Point Colors");
		mColorChooser = new JMenuItem("Choose Entry Point Colors");
		mHighlightRestore = new JMenuItem("Restore Highlighting");
		
		
		mColorByDefault = new JMenuItem("Color by Default");
		mColorByEntryMethod = new JMenuItem("Color by Entry Method");
		mColorByEntryMethodFrequency = new JMenuItem("Color by Entry Method Using Frequency");
		mColorByObjectID = new JMenuItem("Color by Object Index");
		mColorByUserRandom = new JMenuItem("Color by User Supplied Parameter(timestep) with Disjoint Colors");
		mColorByUserGradient = new JMenuItem("Color by User Supplied Parameter(timestep) with Gradient");
		mColorByUserObjRandom = new JMenuItem("Color by User Supplied Parameter(timestep) + Object ID with Disjoint Colors");
		mColorByUserEIDRandom =  new JMenuItem("Color by User Supplied Parameter(timestep) + Entry ID with Disjoint Colors");
		mColorByMemUsage = new JMenuItem("Color by Memory Usage ...");
		

		colorMenu.add(mWhiteBG);
		colorMenu.add(mBlackBG);
		colorMenu.addSeparator();
//		colorMenu.add(mChangeColors);
		colorMenu.add(mSaveColors);
		colorMenu.add(mRestoreColors);
		colorMenu.add(mColorChooser);
		colorMenu.add(mHighlightRestore);
		colorMenu.addSeparator();
		colorMenu.add(mColorByDefault);
		colorMenu.add(mColorByEntryMethod);
		colorMenu.add(mColorByEntryMethodFrequency);
		colorMenu.add(mColorByObjectID);
		colorMenu.add(mColorByUserRandom);
		colorMenu.add(mColorByUserGradient);
		colorMenu.add(mColorByUserObjRandom);
		colorMenu.add(mColorByUserEIDRandom);
		colorMenu.add(mColorByMemUsage);


		mWhiteBG.addActionListener(this);
		mBlackBG.addActionListener(this);
		mSaveColors.addActionListener(this);
		mRestoreColors.addActionListener(this);
		mColorChooser.addActionListener(this);
		mHighlightRestore.addActionListener(this);
		mColorByDefault.addActionListener(this);
		mColorByObjectID.addActionListener(this);
		mColorByEntryMethod.addActionListener(this);
		mColorByEntryMethod.addActionListener(this);
		mColorByEntryMethodFrequency.addActionListener(this);
		mColorByEntryMethodFrequency.addActionListener(this);
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
		cbTraceCriticalPath = new JCheckBoxMenuItem("Trace Critical Path");
		cbTraceArrayElementID = new JCheckBoxMenuItem("Trace Event ID(Chare Array Index)");
		cbTraceArrayRemoveLines = new JMenuItem("Clear all message tracing lines");
		cbTraceForwardOnClick = new JCheckBoxMenuItem("Trace Messages Forward on Left-Click");
		cbTraceCriticalPathOnClick = new JCheckBoxMenuItem("Trace Critical Path on Left-Click");

		tracingMenu.add(cbTraceMessages);
		tracingMenu.add(cbTraceMessagesForward);
		tracingMenu.add(cbTraceCriticalPath);
		tracingMenu.add(cbTraceArrayElementID);
		tracingMenu.add(cbTraceArrayRemoveLines);
		tracingMenu.add(cbTraceForwardOnClick);
		tracingMenu.add(cbTraceCriticalPathOnClick);

		cbTraceMessages.addItemListener(this);
		cbTraceMessagesForward.addItemListener(this);
		cbTraceCriticalPath.addItemListener(this);
		cbTraceArrayElementID.addItemListener(this);
		cbTraceArrayRemoveLines.addActionListener(this);
		cbTraceForwardOnClick.addItemListener(this);
		cbTraceCriticalPathOnClick.addItemListener(this);

		mbar.add(tracingMenu);

		// View Menu
		JMenu viewMenu = new JMenu("View");

		cbNormalView = new JRadioButtonMenuItem("Normal View");
		cbNormalView.addActionListener(this);
		cbNormalView.setSelected(true);
		viewMenu.add(cbNormalView);

		cbCompactView = new JRadioButtonMenuItem("Compact View");
		cbCompactView.addActionListener(this);
		viewMenu.add(cbCompactView);

		cbSuperCompactView = new JRadioButtonMenuItem("Super-Compact View");
		cbSuperCompactView.addActionListener(this);
		viewMenu.add(cbSuperCompactView);

	    ButtonGroup group = new ButtonGroup();
	    group.add(cbNormalView);
	    group.add(cbCompactView);
	    group.add(cbSuperCompactView);
		
	    viewMenu.addSeparator();
	    
	    mShowHideEntries = new JMenuItem("Show & Hide Entry Methods");
		mShowHideEntries.addActionListener(this);
		viewMenu.add(mShowHideEntries);
		
		mShowHideUserEvents = new JMenuItem("Show & Hide User Events");
		mShowHideUserEvents.addActionListener(this);
		viewMenu.add(mShowHideUserEvents);
		

		cbNestedUserEvents = new JCheckBoxMenuItem("Show Nested Bracketed User Events");
		cbNestedUserEvents.addItemListener(this);
		viewMenu.add(cbNestedUserEvents);

		mDisplayLegend = new JMenuItem("Display Legend");
		mDisplayLegend.addActionListener(this);
		viewMenu.add(mDisplayLegend);

		mDisplayTopTimesText = new JMenuItem("Display List of Longest Entry and Idle Times");
		mDisplayTopTimesText.addActionListener(this);
		viewMenu.add(mDisplayTopTimesText);
		
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
		experimentalMenu.add(submenu);

		
		mbar.add(experimentalMenu);
		parentWindow.setJMenuBar(mbar);
	}

	protected void userEventWindowSetData(){
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

		try {
			URL zoomInURL = ((Object) this).getClass().getResource("/projections/images/ZoomIn24.gif");
			URL zoomOutURL = ((Object) this).getClass().getResource("/projections/images/ZoomOut24.gif");

			bDecrease = new JButton(new ImageIcon(zoomOutURL));
			bIncrease = new JButton(new ImageIcon(zoomInURL));
		}
		catch (Exception ex) {
			bDecrease = new JButton("-");
			bIncrease = new JButton("+");
		}
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

		bZoomSelected.setEnabled(false);
		bLoadSelected.setEnabled(false);
		bRanges.setEnabled(true);

		bZoomSelected.addActionListener(this);
		bLoadSelected.addActionListener(this);
		bRanges.addActionListener(this);


		highlightTime = new JTextField(" ");
		selectionBeginTime = new JTextField(" ");
		selectionEndTime = new JTextField(" ");
		selectionDiff = new JTextField("no selection");
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
		Util.gblAdd(this, cbPanel, gbc, 0, 1, 1, 1, 1, 1);
		Util.gblAdd(this, buttonPanel, gbc, 0, 3, 1, 1, 1, 1);
		Util.gblAdd(this, zoomPanel, gbc, 0, 4, 1, 1, 1, 1);

	}


	public void itemStateChanged(ItemEvent evt) {
		
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
		
        else if (c == cbTraceForwardOnClick)
			data.setTraceMessagesForwardOnClick(evt.getStateChange() == ItemEvent.SELECTED);

		else if (c == cbTraceCriticalPath)
			data.setTraceCriticalPathOnHover(evt.getStateChange() == ItemEvent.SELECTED);
		
        else if (c == cbTraceCriticalPathOnClick)
			data.setTraceCriticalPathOnClick(evt.getStateChange() == ItemEvent.SELECTED);

		else if(c == cbNestedUserEvents)
			data.showNestedUserEvents(evt.getStateChange() == ItemEvent.SELECTED);

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


	/** Update the value in the scale factor label with the value in our data object */
	protected void updateScaleField() {
		scaleField.setText("" + data.getScaleFactor());
	}

}
