package projections.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.analysis.RangeHistory;
import projections.analysis.PhaseHistory;
import projections.misc.LogEntryData;

/**
 *  RangeDialogNew
 *  written by Chee Wai Lee 6/28/2002
 *  rewritten by Isaac Dooley 10/9/2009
 *
 *  This class is the dialog box presented by each of the tools when 
 *  requesting a time range and list of PEs to load.
 *  
 *  If the tool has its own specific set of additional GUI input components,
 *  the tool should create a class extending RangeDialogExtensionPanel that provides
 *  the GUI components (which can be anything). The class derives from JPanel,
 *  so all the components ought to be added into the class instance itself. A few
 *  simple methods must be implemented for the class for verifying its input data,
 *  for setting its initial data after the other stuff in this RangeDialog class has
 *  been loaded, and a few other things.
 *  
 *  The intended use of this class is something like this:
 *  
 *  	 RangeDialogExtensionPanel toolSpecificStuff = new BinDialogPanel();
 *		 dialog = new RangeDialog (this, "Dialog Box Title", toolSpecificStuff, false);
 *  
 *  To make the time ranges chosen by the user be available in all dialog boxes, 
 *  an instance of RangeDialogPersistantData is used by all dialog boxes.
 *  
 */

public final class RangeDialog extends JDialog
implements ActionListener, KeyListener, FocusListener, ItemListener, MouseListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	// Constant variables
	private static final int DIALOG_OK = 0;
	private static final int DIALOG_CANCELLED = 1;

	private ProjectionsWindow parentWindow;
	
	// inheritable GUI objects
	private JPanel stepsPanel;

	/** A JPanel containing any other input components required by the tool using this dialog box */
	private RangeDialogExtensionPanel toolSpecificPanel;

	private JSelectField processorsField;
	private TimeTextField startTimeField;
	private TimeTextField endTimeField;
	
	private JPanel timePanel, processorsPanel;
	private JButton bOK, bCancel;

	private JPanel phaseChoicePanel;
	private JComboBox historyList, phaseList;
	private JButton bAddToHistory, bRemoveFromHistory, bSaveHistory;
	private JRadioButton brangeButton, bphaseButton;

	private JButton loadUserNotesButton;

	// private GUI objects
	private JLabel startTextLabel, endTextLabel, totalTimeTextLabel, processorTextLabel;
	private JLabel totalTimeLabel, validTimeRangeLabel, validProcessorsLabel;


	// history variables
	private RangeHistory rangeHistory;
	private PhaseHistory phaseHistory;

	// flags
	private boolean layoutComplete = false;
	private int dialogState;
	private boolean disableTimeRange = false;

	//Temporarily hardcoded. The is the threshold of popping up warning for a large log file.
	private final static long WARN_THRESHOLD = 12800000000L;

	/**
	 *  Constructor.
	 *  
	 *  If a tool wants to provide its own extra input items, they should 
	 *  be put inside a RangeDialogExtensionPanel (basically a JPanel) passed in as toolSpecificPanel.
	 *  
	 */
	public RangeDialog(ProjectionsWindow parentWindow, String titleString, RangeDialogExtensionPanel toolSpecificPanel, boolean disableTimeRange)
	{
		super(parentWindow, titleString, true);
		this.parentWindow = parentWindow;
		this.disableTimeRange = disableTimeRange;
		
		if(toolSpecificPanel != null){
			this.toolSpecificPanel = toolSpecificPanel;
			toolSpecificPanel.setParentDialogBox(this);
		}

		rangeHistory = new RangeHistory(MainWindow.runObject[myRun].getLogDirectory() +
				File.separator);
		phaseHistory = new PhaseHistory(MainWindow.runObject[myRun].getLogDirectory() +
				File.separator);
		this.setModal(true);
		dialogState = DIALOG_CANCELLED; // default state
	}


	/** Called whenever any input item changes, either in this dialog box, or its possibly extended tool specific JPanel */
	public void someInputChanged() {
		//		System.out.println("Something changed. We should update everything, and enable/disable the OK button");

		if(isInputValid()){
			//			System.out.println("Input is valid");
			totalTimeLabel.setText(U.humanReadableString(getSelectedTotalTime()));
			if(toolSpecificPanel != null){
				toolSpecificPanel.updateFields();
			}
			if(!disableTimeRange) {
				bOK.setEnabled(true);
				bAddToHistory.setEnabled(true);
			}
		} else {
			//			System.out.println("Input is NOT valid");
			bOK.setEnabled(false);
			bAddToHistory.setEnabled(false);
		}

		pack();

	}

	public void displayDialog() {
		//  layout the dialog the first time it is used
		if (!layoutComplete) {
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					dialogState = DIALOG_CANCELLED;
					setVisible(false);
				}
			});

			JPanel mainPanel = createMainLayout();
			mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

			JPanel historyPanel = createHistoryLayout();
			historyPanel.setBorder(BorderFactory.createEmptyBorder(15,5,5,5));

			JPanel buttonPanel = createButtonLayout();
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

			getRootPane().setDefaultButton(bOK);
			
			stepsPanel = createloadStepsLayout();
			stepsPanel.setBorder(BorderFactory.createEmptyBorder(15,5,5,5));

			Container p = this.getContentPane();

			// Layout this dialog box as a series of JPanels flowing downwards
			p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
			p.add(mainPanel);
			p.add(historyPanel);
			p.add(stepsPanel);
			if(toolSpecificPanel != null && !disableTimeRange){
				toolSpecificPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
				p.add(Box.createRigidArea(new Dimension(0,10))); // add some vertical padding
				p.add(toolSpecificPanel);
				p.add(Box.createRigidArea(new Dimension(0,10))); // add some vertical padding
			}
			p.add(buttonPanel);

			layoutComplete = true;
			setResizable(true);
		}

		initializeData();
		initializeToolSpecificData();
		pack();
		setLocationRelativeTo(parentWindow);

		int selectedNumCores;
		long selectedTime;
		long estTotalTime;

		while(true){
			setVisible(true);
			if(dialogState == DIALOG_CANCELLED) {
				break;
			}

			selectedNumCores = getNumSelectedProcessors();
			selectedTime = getSelectedTotalTime();
			estTotalTime = selectedNumCores * selectedTime;

			//If a small range is selected, jump out of the loop
			if(estTotalTime < WARN_THRESHOLD)
				break;

			int choice = JOptionPane.showConfirmDialog(this, "This analysis may take a long time.\n " +
							"Do you want to continue?", "Warning",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
			);

			if (choice == 0)
				break;
		}

		/** Store the newly chosen time/PE range */
		storeRangeToPersistantStorage();

	}


	private void storeRangeToPersistantStorage(){
		MainWindow.runObject[myRun].persistantRangeData.update(startTimeField.getValue(), endTimeField.getValue(), processorsField.getValue());
	}


	/** Load the previously used time/PE range */
	private void initializeData(){
		if(!disableTimeRange) {
			startTimeField.setValue(MainWindow.runObject[myRun].persistantRangeData.begintime);
			endTimeField.setValue(MainWindow.runObject[myRun].persistantRangeData.endtime);
			processorsField.setText(Util.listToString(MainWindow.runObject[myRun].persistantRangeData.plist));
		}
	}

	private void initializeToolSpecificData() {
		if(toolSpecificPanel != null){
			toolSpecificPanel.setInitialFields();
		}
	}


	/**
	 *  createMainLayout creates the layout for basic time and processor
	 *  range specification.
	 */
	private JPanel createMainLayout() {

		JPanel inputPanel = new JPanel();

		// Standard Layout behavior for all subcomponents
		GridBagLayout      gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2, 2, 2, 2);

		// Default processor range layout
		processorsPanel = new JPanel();
		processorsPanel.setLayout(gbl);
		validProcessorsLabel = new JLabel("Valid Processors = " + 
				MainWindow.runObject[myRun].getValidProcessorString(),
				JLabel.LEFT);
		validProcessorsLabel.addMouseListener(this);
		processorTextLabel = new JLabel("Processors :", JLabel.LEFT);
		processorsField = new JSelectField(MainWindow.runObject[myRun].getValidProcessorString(), 12);
		// set listeners
		processorsField.addActionListener(this);
		processorsField.addKeyListener(this);
		processorsField.addFocusListener(this);

		// layout
		Util.gblAdd(processorsPanel, validProcessorsLabel, gbc, 0,0, 2,1, 1,1);
		Util.gblAdd(processorsPanel, processorTextLabel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(processorsPanel, processorsField, gbc, 1,1, 1,1, 1,1);

		// Default time range layout
		timePanel = new JPanel();
		timePanel.setLayout(gbl);
		validTimeRangeLabel = new JLabel("Valid Time Range = " +
				U.humanReadableString(0) + " to " +
				U.humanReadableString(MainWindow.runObject[myRun].getTotalTime()), 
				JLabel.LEFT);
		validTimeRangeLabel.addMouseListener(this);
		startTextLabel = new JLabel("Start Time :", JLabel.LEFT);
		startTimeField = new TimeTextField(" ", 12);
		endTextLabel = new JLabel("End Time :", JLabel.LEFT);
		endTimeField = new TimeTextField(" ", 12);
		totalTimeTextLabel = new JLabel("Total Time Selected :", JLabel.LEFT);
		totalTimeLabel = new JLabel(U.humanReadableString(MainWindow.runObject[myRun].getTotalTime()), JLabel.LEFT);

		if (disableTimeRange) {
			startTimeField.setEnabled(false);
			endTimeField.setEnabled(false);
			startTimeField.setValue(0);
			endTimeField.setValue(MainWindow.runObject[myRun].getTotalTime());
		} else {
			// set listeners
			startTimeField.addActionListener(this);
			endTimeField.addActionListener(this);
			startTimeField.addKeyListener(this);
			endTimeField.addKeyListener(this);
			startTimeField.addFocusListener(this);
			endTimeField.addFocusListener(this);
		}

		// layout
		Util.gblAdd(timePanel, validTimeRangeLabel,
				gbc, 0,0, 4,1, 1,1);
		Util.gblAdd(timePanel, startTextLabel, 
				gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(timePanel, startTimeField,
				gbc, 1,1, 1,1, 1,1);
		Util.gblAdd(timePanel, endTextLabel,
				gbc, 2,1, 1,1, 1,1);
		Util.gblAdd(timePanel, endTimeField,
				gbc, 3,1, 1,1, 1,1);
		Util.gblAdd(timePanel, totalTimeTextLabel,
				gbc, 0,2, 1,1, 1,1);
		Util.gblAdd(timePanel, totalTimeLabel,
				gbc, 1,2, 3,1, 1,1);
		if (disableTimeRange) {
			Util.gblAdd(timePanel, new JLabel("Summary data compatible only " +
			"with full time range."),
			gbc, 0,3, 4,1, 1,1);
		}

		// general layout
		inputPanel.setLayout(gbl);
		Util.gblAdd(inputPanel, processorsPanel,
				gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(inputPanel, timePanel,
				gbc, 0,1, 1,1, 1,1);

		return inputPanel;
	}

	/**
	 *  Creates the layout for basic control buttons.
	 */
	private JPanel createButtonLayout() {
		JPanel buttonPanel = new JPanel();

		bOK     = new JButton("OK");
		bCancel = new JButton("Cancel");

		buttonPanel.add(bOK);
		buttonPanel.add(bCancel);

		bOK.addActionListener    (this);
		bCancel.addActionListener(this);

		return buttonPanel;
	}

	@SuppressWarnings("unchecked")
	private JPanel createHistoryLayout() {
		// Standard Layout behavior for all subcomponents
		GridBagLayout      gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2, 2, 2, 2);

		// Default history layout
		JPanel historyPanel = new JPanel();
		historyPanel.setLayout(gbl);
		historyList = new JComboBox(rangeHistory.getHistoryStrings().toArray());
		historyList.setEditable(false);
		historyList.setMaximumRowCount(RangeHistory.MAX_ENTRIES);
		historyList.setSelectedIndex(-1); // nothing selected at first

		bAddToHistory = new JButton("Add to Range History List");
		bRemoveFromHistory = new JButton("Remove Selected Range History");
		bSaveHistory = new JButton("Save Range History to Disk");

		ButtonGroup group = new ButtonGroup();
		brangeButton = new JRadioButton("Range History");
		bphaseButton = new JRadioButton("Phase History");
		group.add(brangeButton);
		group.add(bphaseButton);
		brangeButton.setSelected(true);

		JLabel phaseListLabel = new JLabel("Choose a Phase:", JLabel.LEFT);
		phaseList = new JComboBox();
		phaseList.setEditable(false);

		phaseChoicePanel = new JPanel();
		phaseChoicePanel.setVisible(false);
		phaseChoicePanel.add(phaseListLabel);
		phaseChoicePanel.add(phaseList);

		if (disableTimeRange) {
			historyList.setEnabled(false);
			bAddToHistory.setEnabled(false);
			bRemoveFromHistory.setEnabled(false);
			bSaveHistory.setEnabled(false);
			brangeButton.setEnabled(false);
			bphaseButton.setEnabled(false);
			phaseList.setEnabled(false);
		} else {
			// set listeners
			historyList.addActionListener(this);
			bAddToHistory.addActionListener(this);
			bRemoveFromHistory.addActionListener(this);
			bSaveHistory.addActionListener(this);
			brangeButton.addActionListener(this);
			bphaseButton.addActionListener(this);
			phaseList.addActionListener(this);
		}

		// layout
		Util.gblAdd(historyPanel, brangeButton,
				gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(historyPanel, bphaseButton,
				gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(historyPanel, historyList,
				gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(historyPanel, bSaveHistory,
				gbc, 1,1, 1,1, 1,1);
		Util.gblAdd(historyPanel, bAddToHistory,
				gbc, 0,2, 1,1, 1,1);
		Util.gblAdd(historyPanel, bRemoveFromHistory,
				gbc, 1,2, 1,1, 1,1);
		Util.gblAdd(historyPanel, phaseChoicePanel,
				gbc, 0,3, 2,1, 1,1);

		return historyPanel;
	}

	private JPanel createloadStepsLayout() {
		JPanel loadStepsPanel = new JPanel();
		loadStepsPanel.setLayout(new BorderLayout());
		loadUserNotesButton = new JButton("Find Annotated Timesteps");
		loadUserNotesButton.addActionListener(this);
		loadUserNotesButton.setToolTipText("Choose start/end times from a list of user supplied notes on PE 0 that contain \"***\".");
		loadStepsPanel.add(loadUserNotesButton, BorderLayout.WEST);
		return loadStepsPanel;	
	}



	/** Check for validity of the input fields in this dialog box and any contained tool-specific Jpanel */
	private boolean isInputValid(){

		// start time cannot be greater or equal to end time
		if (getStartTime() >= getEndTime()) {
			startTextLabel.setForeground(Color.red);
			startTimeField.setForeground(Color.red);
			endTextLabel.setForeground(Color.red);
			endTimeField.setForeground(Color.red);
			return false;
		}
		// starting time cannot be less than zero
		if (getStartTime() < 0) {
			startTextLabel.setForeground(Color.red);
			startTimeField.setForeground(Color.red);
			return false;
		}

		// ending time cannot be greater than total time
		if (getEndTime() > getTotalTime()) {
			endTextLabel.setForeground(Color.red);
			endTimeField.setForeground(Color.red);
			return false;
		}

		if(! processorsField.rangeVerifier.verify(processorsField) ){
			processorTextLabel.setForeground(Color.red);
			processorsField.setForeground(Color.red);
			return false;
		}

		// Then the input is valid, so clear any of the red text 
		startTextLabel.setForeground(Color.black);
		startTimeField.setForeground(Color.black);
		endTextLabel.setForeground(Color.black);
		endTimeField.setForeground(Color.black);
		processorTextLabel.setForeground(Color.black);
		processorsField.setForeground(Color.black);


		if(toolSpecificPanel!=null){
			return toolSpecificPanel.isInputValid();	
		} else {
			return true;
		}
	}


	/**
	 *  The API for asking the dialog box (after either the OK or the
	 *  CANCELLED). No other way should be allowed.
	 */
	public boolean isCancelled() {
		return (dialogState == DIALOG_CANCELLED);
	}


	public long getStartTime() {
		return startTimeField.getValue();
	}

	public void setStartTime(long startTime) {
		startTimeField.setValue(startTime);
		someInputChanged();
	}

	public long getEndTime() {
		return endTimeField.getValue();
	}

	public void setEndTime(long endTime) {
		endTimeField.setValue(endTime);
		someInputChanged();
	}

	public long getSelectedTotalTime(){
		return getEndTime()-getStartTime();
	}

	public long getTotalTime(){
		return MainWindow.runObject[myRun].getTotalTime();
	}


	public void setSelectedProcessors(SortedSet<Integer> validPEs) {
		processorsField.setText(Util.listToString(validPEs));
		someInputChanged();
	}


	public SortedSet<Integer> getSelectedProcessors() {
		return processorsField.getValue();
	}


	public int getNumSelectedProcessors(){
		return processorsField.getValue().size();
	}


	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() == bOK) {   
			dialogState = DIALOG_OK;
			setVisible(false);
			return;
		}

		else if(evt.getSource() == bCancel){
			dialogState = DIALOG_CANCELLED;
			setVisible(false);
			return;
		}

		else if (evt.getSource() == loadUserNotesButton){
			stepsPanel.removeAll();
			JProgressBar progressBar;
			progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
			stepsPanel.add(new JLabel("Now Loading User Notes..."), BorderLayout.CENTER);
			stepsPanel.add(progressBar, BorderLayout.EAST);
			stepsPanel.invalidate();
			determineStepsFromPEZero();			
		}

		else if (evt.getSource() == bAddToHistory) {
			if(brangeButton.isSelected()) {
				long start = getStartTime();
				long end = getEndTime();
				String procRange = Util.listToString(processorsField.getValue());
				boolean invalidName = true;
				String s = "";
				while (invalidName) {
					s = JOptionPane.showInputDialog(null, "Enter a name for this time range,"
							+ " or leave blank. \nDo not use spaces," +
							" \"cancel\", \"ENTRY\", or \"NAMEENTRY\".");
					if (s == null) {
						s = "cancel";
						break;
					}
					if (!s.contains(" ")) invalidName = false;
					if (s.equals("ENTRY") || s.equals("NAMEENTRY") || s.equals("cancel")) invalidName = true;
				}
				if (!s.equals("cancel")) {
					String historyString = U.humanReadableString(start) + " to " + U.humanReadableString(end);
					if (procRange.length() > 10) historyString += " Proc(s): " + procRange.substring(0, 10) + "...";
					else historyString += " Proc(s): " + procRange;
					if (!s.equals("")) {
						if (s.length() > 10) historyString += " (" + s.substring(0, 10) + "...)";
						else historyString += " (" + s + ")";
					}
					rangeHistory.add(start, end, s, procRange);
					historyList.insertItemAt(historyString, 0);
					historyList.setSelectedIndex(0);
					if(bphaseButton.isSelected())
						bAddToHistory.setText("Edit Phase Config Entry");
					else if(brangeButton.isSelected())
						bAddToHistory.setText("Add to Range History List");
				}
			} else if(bphaseButton.isSelected()) {
				dialogState = DIALOG_CANCELLED;
				setVisible(false);
				parentWindow.parentWindow.openTool(new PhaseWindow(parentWindow.parentWindow, historyList.getSelectedIndex()));
			}
		} 

		else if (evt.getSource()  == bRemoveFromHistory) {
			int selected = historyList.getSelectedIndex();
			if (selected != -1) {
				if(brangeButton.isSelected()) {
					rangeHistory.remove(selected);
				} else if(bphaseButton.isSelected()) {
					phaseHistory.remove(selected);
				}
				historyList.setSelectedIndex(-1);
				historyList.removeItemAt(selected);
				if(bphaseButton.isSelected()) {
					phaseChoicePanel.setVisible(false);
					bAddToHistory.setText("Add to Phase History List");
				}
				else if(brangeButton.isSelected())
					bAddToHistory.setText("Add to Range History List");
			}
		}

		else if (evt.getSource()  == bSaveHistory) {
			try {
				if(brangeButton.isSelected())
					rangeHistory.save();
				else if(bphaseButton.isSelected())
					phaseHistory.save();
			} catch (IOException e) {
				System.err.println("Error saving history to disk: " + e.toString());
			}
		}

		else if (evt.getSource()  == historyList) {
			int selection = historyList.getSelectedIndex();
			if (selection == -1) {
				if(bphaseButton.isSelected())
					bAddToHistory.setText("Add to Phase History List");
				else if(brangeButton.isSelected())
					bAddToHistory.setText("Add to Range History List");
				return;
			}
			if(brangeButton.isSelected()) {
				startTimeField.setValue(rangeHistory.getStartValue(selection));
				endTimeField.setValue(rangeHistory.getEndValue(selection));
				String procRange = rangeHistory.getProcRange(selection);
				if (procRange != null) processorsField.setText(rangeHistory.getProcRange(selection));
			} else if(bphaseButton.isSelected()) {
				bAddToHistory.setText("Edit Phase Config Entry");

				startTimeField.setValue(phaseHistory.getStartValue(selection));
				endTimeField.setValue(phaseHistory.getEndValue(selection));
				String procRange = phaseHistory.getProcRange(selection);
				if (procRange != null)
					processorsField.setText(phaseHistory.getProcRange(selection));

				phaseList.removeActionListener(this);
				phaseList.removeAllItems();
				int size = phaseHistory.getNumPhases(selection);
				for(int i = 0; i < size; i++) {
					phaseList.addItem(phaseHistory.getPhaseString(selection, i));
				}
				phaseList.setMaximumRowCount(size);
				phaseList.setSelectedIndex(-1);
				phaseList.addActionListener(this);
				phaseChoicePanel.setVisible(true);
			}
		}

		else if (evt.getSource() == brangeButton) {
			bAddToHistory.setText("Add to Range History List");
			bRemoveFromHistory.setText("Remove Selected Range History");
			bSaveHistory.setText("Save Range History to Disk");

			historyList.removeActionListener(this);
			historyList.removeAllItems();
			for(Object o : rangeHistory.getHistoryStrings().toArray())
				historyList.addItem(o);
			historyList.setMaximumRowCount(RangeHistory.MAX_ENTRIES);
			historyList.setSelectedIndex(-1); // nothing selected at first
			historyList.addActionListener(this);
			initializeData();

			phaseChoicePanel.setVisible(false);
		}

		else if (evt.getSource() == bphaseButton) {
			bAddToHistory.setText("Add to Phase History List");
			bRemoveFromHistory.setText("Remove Selected Phase History");
			bSaveHistory.setText("Save Phase History to Disk");

			historyList.removeActionListener(this);
			historyList.removeAllItems();
			for(Object o : phaseHistory.getHistoryStrings().toArray())
				historyList.addItem(o);
			historyList.setMaximumRowCount(PhaseHistory.MAX_ENTRIES);
			historyList.setSelectedIndex(-1);
			historyList.addActionListener(this);

			phaseChoicePanel.setVisible(false);
		}

		else if (evt.getSource() == phaseList) {
			int selection = historyList.getSelectedIndex();
			if (selection == -1) {
				return;
			}
			int phaseSelection = phaseList.getSelectedIndex();
			if (phaseSelection == -1) {
				return;
			}
			startTimeField.setValue(phaseHistory.getStartOfPhase(selection, phaseSelection));
			endTimeField.setValue(phaseHistory.getEndOfPhase(selection, phaseSelection));
		}

		someInputChanged();
	}

	// These are Vectors because they're used with JComboBox
	private Vector<String> availableStepStrings;
//	Vector<String> availableStepStringsEnd;
	private Vector<Long> availableStepTimes;

	private void determineStepsFromPEZero() {

		if (!(MainWindow.runObject[myRun].hasLogData())){
			stepsPanel.removeAll();
			stepsPanel.add(new JLabel("No log data available"), BorderLayout.CENTER);
			stepsPanel.invalidate();
			pack();
		}

		// Labels containing the user notes found in the log
		availableStepStrings = new Vector<String>();
		availableStepTimes = new Vector<Long>();
		
		availableStepStrings.add("Beginning");
		availableStepTimes.add((long)0);

		
		final SwingWorker worker =  new SwingWorker() {
			public Object doInBackground() {
				int pe = 0;
				GenericLogReader reader = new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());
				try {	  

					int c = 0;
					while (true) {
						LogEntryData data = reader.nextEvent();

						if(data.type == ProjDefs.USER_SUPPLIED_NOTE){
							if(data.note.contains("***")){
								String pruned = data.note.replace("*** ", "");
								availableStepStrings.add("" + (c++) + ": " + pruned);
								availableStepTimes.add(data.time);
							}
						}
					}

				} catch (EndOfLogSuccess e) {			
					// Successfully read log file
					availableStepStrings.add("End");
					availableStepTimes.add(MainWindow.runObject[myRun].getTotalTime());
				} catch (IOException e) {
					System.err.println("Error occured while reading data for pe " + pe);
				}
				
				
				try {
					reader.close();
				} catch (IOException e1) {
					System.err.println("Error: could not close log file reader for processor " + pe );
				}
								
				
				return null;
			}
			
			public void done() {
				stepsPanel.removeAll();
							
				// Create the first drop down menu
				JComboBox popupStart = new JComboBox(availableStepStrings);
				popupStart.setSelectedIndex(0);
				popupStart.setEditable(false);
				PopupHandler phStart = new PopupHandler();
				phStart.useForStartTime();
				popupStart.addActionListener(phStart);
					
				// Create the second drop down menu
				JComboBox popupEnd = new JComboBox(availableStepStrings);
				popupEnd.setSelectedIndex(availableStepStrings.size()-1);
				popupEnd.setEditable(false);
				PopupHandler phEnd = new PopupHandler();
				phEnd.useForEndTime();
				popupEnd.addActionListener(phEnd);

				// Assemble these drop down manus with some labels into stepsPanel		    	
				stepsPanel.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();    	
				Util.gblAdd(stepsPanel, new JLabel("Choose a start time:",JLabel.RIGHT), gbc, 0, 0, 1,1, 1,1, 1,1,1,1);
				Util.gblAdd(stepsPanel, popupStart, gbc, 1, 0, 1,1, 1,1, 1,1,1,1);
				Util.gblAdd(stepsPanel, new JLabel("Choose an end time:",JLabel.RIGHT), gbc, 0, 1, 1,1, 1,1, 1,1,1,1);
				Util.gblAdd(stepsPanel, popupEnd, gbc, 1, 1, 1,1, 1,1, 1,1,1,1);
						
				stepsPanel.invalidate();
				pack();
			}
		};

		worker.execute();
		
	}


	
	private final class PopupHandler implements ActionListener {

		private boolean useForStart = false;
		private boolean useForEnd = false;
		
		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			int menuIndex = cb.getSelectedIndex();			
			
			if(useForStart){
				setStartTime(availableStepTimes.get(menuIndex));				
			}

			if(useForEnd){
				setEndTime(availableStepTimes.get(menuIndex));				
			}
		}

		private void useForEndTime() {
			useForEnd = true;
			useForStart = false;
		}

		private void useForStartTime() {
			useForEnd = false;
			useForStart = true;
		}
	}
	

public void focusGained(FocusEvent evt) {
	someInputChanged();
}

public void focusLost(FocusEvent evt) {
	someInputChanged();
}

public void keyPressed(KeyEvent evt) {
	someInputChanged();
}

public void keyReleased(KeyEvent evt) {
	someInputChanged();

	// If we just got an enter key, then if we have valid input, finish!
	if(evt.getKeyCode() == KeyEvent.VK_ENTER){
		if(isInputValid()){
			dialogState = DIALOG_OK;
			setVisible(false);
		}
	}

}

public void keyTyped(KeyEvent evt) {
	someInputChanged();
}

public void itemStateChanged(ItemEvent e) {
	someInputChanged();
}


public void mouseClicked(MouseEvent e) {
	if(e.getSource() == validProcessorsLabel){
		processorsField.setText(MainWindow.runObject[myRun].getValidProcessorString());
	} else if(e.getSource() == validTimeRangeLabel){
		startTimeField.setValue(0);
		endTimeField.setValue(MainWindow.runObject[myRun].getTotalTime());
	}
	someInputChanged();
}


public void mouseEntered(MouseEvent e) {
	someInputChanged();
}


public void mouseExited(MouseEvent e) {
	someInputChanged();
}


public void mousePressed(MouseEvent e) {
	someInputChanged();
}


public void mouseReleased(MouseEvent e) {
	someInputChanged();
}


}

