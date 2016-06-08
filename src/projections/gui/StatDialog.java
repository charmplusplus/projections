package projections.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Checkbox;
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
import java.io.IOException;
import java.util.SortedSet;

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

/*
	Stat Dialog created by Joshua Lew 7/5/16
	Used by UserStatOverTime Tool for adding and customizing a dataset
	Allows the user to choose:
	Pe's involved
	How the Pe's are aggregated
	Which X values to use (Wall Timer, Ordered, User Specified)
	Left or Right Y Axis
	Color
	Lines/Points/Both
	Which Stat to plot

	This Dialog is specialized for the UserStatOverTime tool
	 and would require a little work to use with other tools
*/


public final class StatDialog extends JDialog
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
	private JPanel mainPanel, buttonPanel;

	/** A JPanel containing any other input components required by the tool using this dialog box */

	private JSelectField processorsField;
	private TimeTextField startTimeField;
	private TimeTextField endTimeField;

	private JPanel timePanel, processorsPanel,statPanel;
	private JButton bOK, bCancel;



	// private GUI objects
	private JLabel startTextLabel, endTextLabel, totalTimeTextLabel, processorTextLabel;
	private JLabel totalTimeLabel, validTimeRangeLabel, validProcessorsLabel;

	//GUI objects for stat panel;
	private JLabel selectStatLabel, xValueLabel, yAxisLabel, colorLabel, aggregateLabel;
	private JComboBox selectStatBox, xValueBox,yAxisBox, colorBox,aggregateBox;
	private Checkbox pointBox, lineBox;

	private int statIndex;
	private String xValueString, yAxisString, colorString, aggregateString;
	private boolean pointBool, lineBool;
	// flags
	private boolean layoutComplete = false;
	private int dialogState;
	private boolean disableTimeRange = false;

	private String[] 	statNames;
	//Temporarily hardcoded. The is the threshold of popping up warning for a large log file.
	private final static long WARN_THRESHOLD = 12800000000L;



	public StatDialog(ProjectionsWindow parentWindow, String titleString,String[] names, boolean disableTimeRange)
	{
		super(parentWindow, titleString, true);
		this.parentWindow = parentWindow;
		this.disableTimeRange = disableTimeRange;
		this.statNames = names;

		this.setModal(true);
		dialogState = DIALOG_CANCELLED; // default state
	}

	/** Called whenever any input item changes, either in this dialog box, or its possibly extended tool specific JPanel */
	public void someInputChanged() {
		//		System.out.println("Something changed. We should update everything, and enable/disable the OK button");

		if(isInputValid()){
			//			System.out.println("Input is valid");
			totalTimeLabel.setText(U.humanReadableString(getSelectedTotalTime()));
		}
		 else {
			//			System.out.println("Input is NOT valid");
			bOK.setEnabled(false);
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


			buttonPanel = createButtonLayout();
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

			getRootPane().setDefaultButton(bOK);


			Container p = this.getContentPane();

			// Layout this dialog box as a series of JPanels flowing downwards
			p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
			p.add(mainPanel);
				p.add(Box.createRigidArea(new Dimension(0,10))); // add some vertical padding
				p.add(Box.createRigidArea(new Dimension(0,10))); // add some vertical padding
			p.add(buttonPanel);

			layoutComplete = true;
			setResizable(true);
		}

		initializeData();
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
		startTimeField.setValue(MainWindow.runObject[myRun].persistantRangeData.begintime);
		endTimeField.setValue(MainWindow.runObject[myRun].persistantRangeData.endtime);
		processorsField.setText(Util.listToString(MainWindow.runObject[myRun].persistantRangeData.plist));
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

		//statPanel
		String[] xValueChoices = {"Wall Time","Ordered", "User Specified"};
		String[] yAxisChoices = {"Left", "Right"};
		String[] colorChoices = {"Red","Blue","Green","Black","Cyan","Yellow","Pink","Magenta","Gray"};
		String[] aggregateChoices = {"Sum", "Min", "Max", "Average"};

		statPanel = new JPanel();
		statPanel.setLayout(gbl);
		selectStatLabel	= new JLabel("Select Stat: ", JLabel.LEFT);
		selectStatBox = new JComboBox(statNames);
		selectStatBox.setSelectedIndex(0);
		xValueLabel = new JLabel("Select X Values: ", JLabel.LEFT);
		xValueBox = new JComboBox(xValueChoices);
		xValueBox.setSelectedIndex(0);

		yAxisLabel = new JLabel("Select Y Axis: ", JLabel.LEFT);
		yAxisBox = new JComboBox(yAxisChoices);
		yAxisBox.setSelectedIndex(0);

		colorLabel = new JLabel("Choose Line Color: ", JLabel.LEFT);
		colorBox = new JComboBox(colorChoices);
		colorBox.setSelectedIndex(0);

		aggregateLabel = new JLabel("How to Aggregate accross PE's: ", JLabel.LEFT);
		aggregateBox = new JComboBox(aggregateChoices);
		aggregateBox.setSelectedIndex(0);

		pointBox = new Checkbox("Display Points?",true);
		lineBox = new Checkbox("Display Lines?",true);

		Util.gblAdd(statPanel, selectStatLabel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(statPanel, selectStatBox, gbc, 1,0, 2,1, 1,1);
		Util.gblAdd(statPanel, xValueLabel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(statPanel, xValueBox, gbc, 1,1, 2,1, 1,1);
		Util.gblAdd(statPanel, aggregateLabel, gbc, 0,2, 1,1, 1,1);
		Util.gblAdd(statPanel, aggregateBox, gbc, 1,2, 2,1, 1,1);
		Util.gblAdd(statPanel, yAxisLabel, gbc, 0,3, 1,1, 1,1);
		Util.gblAdd(statPanel, yAxisBox, gbc, 1,3, 2,1, 1,1);
		Util.gblAdd(statPanel, colorLabel, gbc, 0,4, 1,1, 1,1);
		Util.gblAdd(statPanel, colorBox, gbc, 1,4, 2,1, 1,1);
		Util.gblAdd(statPanel, pointBox, gbc, 0,5, 2,1, 1,1);
		Util.gblAdd(statPanel, lineBox, gbc, 1,5, 2,1, 1,1);

		statIndex = 0;
		xValueString = "Wall Time";
		yAxisString = "Left";
		colorString = "Red";
		pointBool = true;
		lineBool = true;
		aggregateString = "Sum";

		selectStatBox.addActionListener(this);
		xValueBox.addActionListener(this);
		yAxisBox.addActionListener(this);
		colorBox.addActionListener(this);
		aggregateBox.addActionListener(this);
		pointBox.addItemListener(this);
		lineBox.addItemListener(this);
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
		Util.gblAdd(inputPanel, statPanel,
				gbc, 0,2, 1,1, 1,1);

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


		return true;
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

	public int getStatIndex(){
		return statIndex;
	}
	public String getXValue(){
		return xValueString;
	}
	public String getYValue(){
		return yAxisString;
	}
	public String getColor(){
		return colorString;
	}
	public String getAggregate(){
		return aggregateString;
	}
	public boolean getPointSet(){
		return pointBool;
	}
	public boolean getLineSet(){
		return lineBool;
	}

	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource() instanceof JComboBox){
			JComboBox b = (JComboBox)evt.getSource();

			if(b == selectStatBox) {
				statIndex = b.getSelectedIndex();
			}

			else if(b == xValueBox) {
				xValueString = (String) b.getSelectedItem();
			}
			else if(b == yAxisBox) {
				yAxisString = (String) b.getSelectedItem();
			}
			else if(b == colorBox) {
				colorString = (String) b.getSelectedItem();
			}
			else if(b == aggregateBox) {
				aggregateString = (String) b.getSelectedItem();
			}

			return;
		}



		else if (evt.getSource() == bOK) {
			dialogState = DIALOG_OK;
			setVisible(false);
			return;
		}

		else if(evt.getSource() == bCancel){
			dialogState = DIALOG_CANCELLED;
			setVisible(false);
			return;
		}



		someInputChanged();
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

	if (e.getSource() instanceof Checkbox){
		Checkbox b = (Checkbox)e.getSource();

		if(b == pointBox) {
			pointBool = b.getState();
		}

		else if(b == lineBox) {
			lineBool = b.getState();
		}
		return;
	}

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

