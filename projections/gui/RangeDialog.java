package projections.gui;
import javax.swing.*;
import javax.swing.text.*;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.*;

import java.util.*;
import java.io.*;

import projections.analysis.*;

/**
 *  RangeDialog
 *  written by Chee Wai Lee
 *  6/28/2002
 *
 *  This class is the base dialog class expected to be called from a
 *  subclass of ProjectionsWindow in order to present a range input
 *  dialog box to the user.
 *
 *  This dialog box is designed to maintain old information as long
 *  as it was not destroyed and garbage collected.
 *
 *  Added a history combo box - 8/20/2002
 */

public class RangeDialog extends JDialog
    implements ActionListener, KeyListener, FocusListener, ItemListener
{
    // Constant variables
    private static final int DIALOG_OK = 0;
    private static final int DIALOG_CANCELLED = 1;
  
    ProjectionsWindow parentWindow;

    // inheritable GUI objects
    JPanel mainPanel, historyPanel, buttonPanel;

    JSelectField processorsField;
    JTimeTextField startTimeField;
    JTimeTextField endTimeField;

    JPanel timePanel, processorsPanel;
    JButton bOK, bUpdate, bCancel;

    JComboBox historyList;
    JButton bAddToHistory, bRemoveFromHistory, bSaveHistory;

    // private GUI objects
    private JLabel startTextLabel, endTextLabel, totalTimeTextLabel,
	processorTextLabel;
    private JLabel totalTimeLabel, validTimeRangeLabel, validProcessorsLabel;

    // Dialog state monitoring variables. These are based directly on the
    // field's stored data.
    // (inherited for the purposes of validation checks)
    protected long totalTime;
    protected long totalValidTime;
    protected int numProcessors;

    // inheritable old state preserving data items
    protected OrderedIntList validProcessors = new OrderedIntList();
    protected long startTime = -1;
    protected long endTime = -1;

    // history variables
    RangeHistory history;

    // flags
    private boolean layoutComplete = false;
    private int dialogState;
    private boolean disableRange = false;

    /**
     *  Wrapper Constructor. For disabling parts of the dialog.
     *  (eg. Usage Profile with only summary logs has to use full
     *   time range).
     */
    public RangeDialog(ProjectionsWindow parentWindow,
		       String titleString,
		       boolean disableRange) {
	this(parentWindow, titleString);
	this.disableRange = disableRange;
    }

    /**
     *  Constructor. Creation of the dialog object should be separate from
     *  the GUI layout. This allows for the proper inheritance from this
     *  base class.
     */
    public RangeDialog(ProjectionsWindow parentWindow, 
		       String titleString)
    {
	super((JFrame)parentWindow, titleString, true);
	this.parentWindow = parentWindow;

	// the only purpose of numProcessors is to determine the limit
	// of the processor list.
	numProcessors = Analysis.getNumProcessors();
	totalTime = Analysis.getTotalTime();
	totalValidTime = totalTime;

	history = new RangeHistory(Analysis.getLogDirectory() +
				   File.separator);
	this.setModal(true);
	dialogState = DIALOG_CANCELLED; // default state
    }   

    /**
     *  INHERITANCE NOTE:
     *  The subclass should call super.actionPerformed AFTER its own 
     *  actionPerformed routine to ensure the proper behaviour of events 
     *  with respect to parent class fields.
     */
    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
	    
	    if(b == bOK) {
		// point user to an inconsistent field.
		JTextField someField = checkConsistent();
		if (someField != null) {
		    someField.selectAll();
		    someField.requestFocus();
		    return;
		} else {
		   dialogState = DIALOG_OK;	// set local variable state 
		}
		setVisible(false);
	    } else if (b == bUpdate) {
		// update all text fields.
		updateData(processorsField);
		updateData(startTimeField);
		updateData(endTimeField);
		return;	// CHECK this return
	    }else if (b == bCancel){
		dialogState = DIALOG_CANCELLED;
		setVisible(false);
	    } else if (b == bAddToHistory) {
		long start = startTimeField.getValue();
		long end = endTimeField.getValue();
		history.add(start, end);
		String historyString = U.t(start) + " to " + U.t(end);
		historyList.insertItemAt(historyString,0);
		historyList.setSelectedIndex(0);
	    } else if (b == bRemoveFromHistory) {
		int selected = historyList.getSelectedIndex();
		if (selected != -1) {
		    history.remove(selected);
		    historyList.removeItemAt(selected);
		}
	    } else if (b == bSaveHistory) {
		try {
		    history.save();
		} catch (IOException e) {
		    System.err.println("Save Error: " + e.toString());
		}
	    }
	} else if (evt.getSource() instanceof JTextField) {
	    // perform an update in response to an "Enter" action event
	    // since the Enter key is typically hit after some keyboard
	    // input.
	    updateData((JTextField)evt.getSource());
	    bOK.setEnabled(true);
	}
    }   
    
    public void itemStateChanged(ItemEvent evt) {
	if (evt.getSource() instanceof JComboBox) {
	    if ((JComboBox)evt.getSource() == historyList) {
		int selection = historyList.getSelectedIndex();
		if (selection == -1) {
		    return;
		}
		startTimeField.setValue(history.getStartValue(selection));
		endTimeField.setValue(history.getEndValue(selection));
	    }
	}
    }

    public void focusGained(FocusEvent evt) {
	// do nothing
    }

    public void focusLost(FocusEvent evt) {
	// when keyboard focus is lost from a text field, it is assumed
	// that the user has confirmed the data. Hence, perform an update and
	// enable the OK button.
	if (evt.getComponent() instanceof JTextField) {
	    updateData((JTextField)evt.getComponent());
	    bOK.setEnabled(true);
	}
    }

    public void keyPressed(KeyEvent evt) {
	// do nothing
    }

    public void keyReleased(KeyEvent evt) {
	// do nothing
    }

    /**
     *  Look for changes in input to text fields.
     *
     *  INHERITANCE NOTE:
     *  If additional behavior is desired for subclass, super.keyTyped
     *  should be call after processing the event in the subclass.
     */
    public void keyTyped(KeyEvent evt) {
	int keycode = evt.getKeyCode();
	if (evt.getComponent() instanceof JTextField) {
	    JTextField field = (JTextField)evt.getComponent();
	    
	    switch (keycode) {
	    case KeyEvent.VK_ENTER:
		// hitting enter is typical of input confirmation. Update the
		// data and enable the OK button.
		updateData(field);
		bOK.setEnabled(true);
		break;
	    case KeyEvent.VK_TAB:
		// leaving keyboard focus for that field. Update the data and
		// enable the OK button.
		updateData(field);
		bOK.setEnabled(true);
		break;
	    default:
		// any other key assumed to be input. Hence disable OK button.
		// enable the Update button to alert user to the change.
		bOK.setEnabled(false);
		break;
	    }
	}
    }

    /**
     *  displayDialog is the public interface by which the parent window
     *  may display the dialog box.
     *
     *  INHERITANCE NOTE:
     *  This method should be OVERRIDDEN by the subclass if a different
     *  layout format is desired. Otherwise, leave it alone.
     */
    public void displayDialog() {
	// only layout the dialog once in its lifetime.
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
	    historyPanel = createHistoryLayout();
	    buttonPanel = createButtonLayout();
	    
	    this.getContentPane().setLayout(new BorderLayout());
	    this.getContentPane().add(mainPanel, BorderLayout.NORTH);
	    this.getContentPane().add(historyPanel, BorderLayout.CENTER);
	    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
	    layoutComplete = true;
	    pack();
	    setResizable(false);
	} else {
	    setParameters();
	}
        //added by Chao Mei, set the the Dialog center on the screen.
        //The method is only available since Sun Java JDK 1.4
        setLocationRelativeTo(parentWindow);

	setVisible(true);
    }

    /**
     *  createMainLayout creates the layout for basic time and processor
     *  range specification.
     *
     *  INHERITANCE NOTE:
     *  Subclasses should call super.createMainLayout to acquire the 
     *  default panels generated for the basic GUI. These panels can then
     *  be integrated into the panel being constructed by the subclass's
     *  createMainLayout method.
     *
     *  However, if the desire is to construct a brand new layout, this
     *  method should be OVERRIDDEN. The programmer would then have to
     *  use the inheritable references to the fields and his/her own 
     *  label objects to construct the layout.
     */
    JPanel createMainLayout() {

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
					  Analysis.getValidProcessorString(),
					  JLabel.LEFT);
	processorTextLabel = new JLabel("Processors :", JLabel.LEFT);
	processorsField = new JSelectField(Analysis.getValidProcessorString(),
					   12);
	// set listeners
	processorsField.addActionListener(this);
	// layout
	Util.gblAdd(processorsPanel, validProcessorsLabel, 
		    gbc, 0,0, 2,1, 1,1);
	Util.gblAdd(processorsPanel, processorTextLabel,
		    gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(processorsPanel, processorsField,
		    gbc, 1,1, 1,1, 1,1);

	// Default time range layout
	timePanel = new JPanel();
	timePanel.setLayout(gbl);
	validTimeRangeLabel = new JLabel("Valid Time Range = " +
					 U.t(0) + " to " +
					 U.t(Analysis.getTotalTime()), 
					 JLabel.LEFT);
	startTextLabel = new JLabel("Start Time :", JLabel.LEFT);
	startTimeField = new JTimeTextField(0, 12);
	endTextLabel = new JLabel("End Time :", JLabel.LEFT);
	endTimeField = new JTimeTextField(Analysis.getTotalTime(), 12);
	totalTimeTextLabel = new JLabel("Total Time selected :", JLabel.LEFT);
	totalTimeLabel = new JLabel(U.t(Analysis.getTotalTime()), JLabel.LEFT);

	if (disableRange) {
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
	if (disableRange) {
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
     *  createButtonLayout creates the layout for basic control buttons.
     *
     *  INHERITANCE NOTE:
     *  Subclasses should call super.createButtonLayout to acquire the 
     *  default panels generated for the basic GUI. These panels can then
     *  be integrated into the panel being constructed by the subclass's
     *  createButtonLayout method.
     *
     *  However, if the desire is to construct a brand new layout, this
     *  method should be OVERRIDDEN. The programmer would then have to
     *  use the inheritable references to the buttons and his/her own 
     *  button objects to construct the layout.
     */
    JPanel createButtonLayout() {
	JPanel buttonPanel = new JPanel();

	bOK     = new JButton("OK");
	bUpdate = new JButton("Update");
	bCancel = new JButton("Cancel");
	
        buttonPanel.add(bOK);
	buttonPanel.add(bUpdate);
	buttonPanel.add(bCancel);
	
	bOK.addActionListener    (this);
	bUpdate.addActionListener(this);
	bCancel.addActionListener(this);

	return buttonPanel;
    }

    /**
     *  createHistoryLayout is not intended to be inherited by subclasses.
     */
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

	if (disableRange) {
	    historyList.setEnabled(false);
	    bAddToHistory.setEnabled(false);
	    bRemoveFromHistory.setEnabled(false);
	    bSaveHistory.setEnabled(false);
	} else {
	    // set listeners
	    historyList.addItemListener(this);
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

    /**
     *  updateData stores the data values from text into the variables.
     *  It also attempts to make the various data fields consistent by
     *  updating the values of other data items which are dependent on 
     *  the changes to the current data item.

     *  INHERITANCE NOTE:
     *  Inheriting subclasses should make a superclass call after doing
     *  its own field updates.
     */
    void updateData(JTextField field) {
	if (field instanceof JTimeTextField) {
	    // if the field is a time-based field
	    if ((field == startTimeField) ||
		(field == endTimeField)) {
		totalTime = endTimeField.getValue()-startTimeField.getValue();
		totalTimeLabel.setText(U.t(totalTime));
	    }
	} else if (field instanceof JSelectField) {
	    // if the field is a range selection field
	    if (field == processorsField) {
		// nothing to do
	    }
	}
    }

    /**
     *  setParameters stores the current data of all fields into the
     *  old state variables. This is to facilitate the isModified()
     *  check.
     *
     *  INHERITANCE NOTE:
     *  Inheriting subclasses should make a superclass call after
     *  preserving its own state.
     */
    void setParameters() {
	startTime = startTimeField.getValue();
	endTime = endTimeField.getValue();
	validProcessors = getValidProcessors();
    }

    /**
     *  updateFields is essentialy the "reverse" of setParameters. It
     *  takes whatever is stored in the parameter variables and updates
     *  the appropriate fields. This is intended to be used immediately
     *  after a ProjectionsWindows subclass has updated this dialog's
     *  parameter variables.
     *  
     *  INHERITANCE NOTE:
     *  Inheriting subclasses should make a superclass call after
     *  updating its own fields.
     */
    void updateFields() {
	startTimeField.setValue(startTime);
	endTimeField.setValue(endTime);
	processorsField.setText(validProcessors.listToString());
	// this is required for the necessary derived information to be 
	// updated based on the new field information.
	updateDerived();
    }

    void updateDerived() {
	totalTime = endTimeField.getValue()-startTimeField.getValue();
	totalTimeLabel.setText(U.t(totalTime));
    }

    /**
     *  checkConsistent verifies the consistency of the various data fields
     *  and returns any one of the fields that are in violation.
     *
     *  INHERITANCE NOTE:
     *  Inheriting subclasses should make a superclass call after doing
     *  its own consistency checks.
     */
    JTextField checkConsistent() {
	// start time cannot be greater or equal to end time
	if (startTimeField.getValue() >= endTimeField.getValue()) {
	    return startTimeField;
	}
	// starting time cannot be less than zero
	if (startTimeField.getValue() < 0) {
	    return startTimeField;
	}
	// ending time cannot be greater than total time
	if (endTimeField.getValue() > totalValidTime) {
	    return endTimeField;
	}
	return null;
    }

    /**
     *  The API for asking the dialog box (after either the OK or the
     *  CANCELLED). No other way should be allowed.
     */
    public boolean isCancelled() {
	return (dialogState == DIALOG_CANCELLED);
    }

    /**
     *  isModified returns true if any of the text fields have values
     *  that differ from the stored old values. This can be used by
     *  tools that optimize for values that remain the same.
     *
     *  INHERITANCE NOTE: isModified of the subclass should perform
     *  an OR operation with the superclass's isModified method.
     */
    public boolean isModified() {
	return ((startTime != startTimeField.getValue()) ||
		(endTime != endTimeField.getValue()) ||
		(!validProcessors.equals(getValidProcessors())));
    }

    /**
     *  Accessors for Start Time
     */
    public long getStartTime() {
	return startTimeField.getValue();
    }

    public void setStartTime(long startTime) {
	this.startTime = startTime;
    }

    /**
     *  Accessors for End Time
     */
    public long getEndTime() {
	return endTimeField.getValue();
    }

    public void setEndTime(long endTime) {
	this.endTime = endTime;
    }

    /**
     *   Accessors for validProcessors (since it is more complex than
     *   a mere primitive). Both a String and OrderedIntList interface
     *   is provided. **CW** in the future, this should be standardized
     *   to use the OrderedIntList interface.
     */
    public OrderedIntList getValidProcessors() {
	return processorsField.getValue(numProcessors);
    }

    public String getValidProcessorString() {
	return processorsField.getText();
    }

    public void setValidProcessors(OrderedIntList validPEs) {
	this.validProcessors = validPEs;
    }

    public void setValidProcessors(String validPEString) {
	processorsField.setText(validPEString);
	this.validProcessors = processorsField.getValue(numProcessors);
    }
}

