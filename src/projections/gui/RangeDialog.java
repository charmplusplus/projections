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

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.analysis.RangeHistory;
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
	private JPanel mainPanel, historyPanel, buttonPanel, stepsPanel;

	/** A JPanel containing any other input components required by the tool using this dialog box */
	private RangeDialogExtensionPanel toolSpecificPanel;

	private JSelectField processorsField;
	private TimeTextField startTimeField;
	private TimeTextField endTimeField;
	
	private JPanel timePanel, processorsPanel;
	private JButton bOK, bCancel;

	private JComboBox historyList;
	private JButton bAddToHistory, bRemoveFromHistory, bSaveHistory;

	private JButton loadUserNotesButton;

	// private GUI objects
	private JLabel startTextLabel, endTextLabel, totalTimeTextLabel, processorTextLabel;
	private JLabel totalTimeLabel, validTimeRangeLabel, validProcessorsLabel;


	// history variables
	private RangeHistory history;

	// flags
	private boolean layoutComplete = false;
	private int dialogState;
	private boolean disableTimeRange = false;

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

		history = new RangeHistory(MainWindow.runObject[myRun].getLogDirectory() +
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
			bOK.setEnabled(true);
			bAddToHistory.setEnabled(true);
			bRemoveFromHistory.setEnabled(true);
		} else {
			//			System.out.println("Input is NOT valid");
			bOK.setEnabled(false);
			bAddToHistory.setEnabled(false);
			bRemoveFromHistory.setEnabled(false);
		}

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

			mainPanel = createMainLayout();
			mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

			historyPanel = createHistoryLayout();
			historyPanel.setBorder(BorderFactory.createEmptyBorder(15,5,5,5));

			buttonPanel = createButtonLayout();
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
			if(toolSpecificPanel != null){    	
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
		setVisible(true);


		// This is after the dialog box is closed:

		/** Store the newly chosen time/PE range */
		if(dialogState != DIALOG_CANCELLED){
			// Store this new time range for future use by this or other dialog boxes
			storeRangeToPersistantStorage();
		}

	}


	private void storeRangeToPersistantStorage(){
		MainWindow.runObject[myRun].persistantRangeData.update(startTimeField.getValue(), endTimeField.getValue(), processorsField.getValue());
	}


	/** Load the previously used time/PE range */
	private void initializeData(){
		startTimeField.setValue(MainWindow.runObject[myRun].persistantRangeData.begintime);
		endTimeField.setValue(MainWindow.runObject[myRun].persistantRangeData.endtime);
		processorsField.setText(MainWindow.runObject[myRun].persistantRangeData.plist.listToString());
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
		totalTimeTextLabel = new JLabel("Total Time selected :", JLabel.LEFT);
		totalTimeLabel = new JLabel(U.humanReadableString(MainWindow.runObject[myRun].getTotalTime()), JLabel.LEFT);

		if (disableTimeRange) {
			startTimeField.setEnabled(false);	    
			endTimeField.setEnabled(false);
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

	private JPanel createHistoryLayout() {
		// Standard Layout behavior for all subcomponents
		GridBagLayout      gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2, 2, 2, 2);

		// Default history layout
		JPanel historyPanel = new JPanel();
		historyPanel.setLayout(gbl);
		historyList = new JComboBox(history.getHistoryStrings());
		historyList.setEditable(false);
		historyList.setMaximumRowCount(RangeHistory.MAX_ENTRIES);
		historyList.setSelectedIndex(-1); // nothing selected at first

		bAddToHistory = new JButton("Add to History List");
		bRemoveFromHistory = new JButton("Remove selected History");
		bSaveHistory = new JButton("Save History to Disk");


		if (disableTimeRange) {
			historyList.setEnabled(false);
			bAddToHistory.setEnabled(false);
			bRemoveFromHistory.setEnabled(false);
			bSaveHistory.setEnabled(false);
		} else {
			// set listeners
			historyList.addActionListener(this);
			bAddToHistory.addActionListener(this);
			bRemoveFromHistory.addActionListener(this);
			bSaveHistory.addActionListener(this);
		}

		// layout
		Util.gblAdd(historyPanel, historyList,
				gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(historyPanel, bSaveHistory,
				gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(historyPanel, bAddToHistory,
				gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(historyPanel, bRemoveFromHistory,
				gbc, 1,1, 1,1, 1,1);

		return historyPanel;
	}

	private JPanel createloadStepsLayout() {
		JPanel loadStepsPanel = new JPanel();
		loadStepsPanel.setLayout(new BorderLayout());
		loadUserNotesButton = new JButton("Find annotated timesteps");
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


	public void setSelectedProcessors(OrderedIntList validPEs) {
		processorsField.setText(validPEs.listToString());		
		someInputChanged();
	}


	public OrderedIntList getSelectedProcessors() {
		return processorsField.getValue();
	}


	public int getNumSelectedProcessors(){
		return processorsField.getValue().size();
	}


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
			pack();
			determineStepsFromPEZero();			
		}

		else if (evt.getSource() == bAddToHistory) {
			long start = getStartTime();
			long end = getEndTime();
			history.add(start, end);
			String historyString = U.humanReadableString(start) + " to " + U.humanReadableString(end);
			historyList.insertItemAt(historyString,0);
			historyList.setSelectedIndex(0);
		} 

		else if (evt.getSource()  == bRemoveFromHistory) {
			int selected = historyList.getSelectedIndex();
			if (selected != -1) {
				history.remove(selected);
				historyList.removeItemAt(selected);
			}
		}

		else if (evt.getSource()  == bSaveHistory) {
			try {
				history.save();
			} catch (IOException e) {
				System.err.println("Error saving history to disk: " + e.toString());
			}
		}

		else if (evt.getSource()  == historyList) {
			int selection = historyList.getSelectedIndex();
			if (selection == -1) {
				return;
			}
			startTimeField.setValue(history.getStartValue(selection));
			endTimeField.setValue(history.getEndValue(selection));
		}

		someInputChanged();
	}

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
				GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
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
					availableStepStrings.add(new String("End"));
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

