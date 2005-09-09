package projections.gui;

import javax.swing.*;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;

public class IntervalRangeDialog extends RangeDialog
    implements ActionListener, KeyListener, FocusListener
{
    // Additional GUI objects
    JPanel sizePanel;

    JTimeTextField sizeField;
    JLabel numIntervalsLabel;
    JLabel validIntervalsLabel;
    JLabel startIntervalLabel;
    JLabel endIntervalLabel;

    // dialog parameter variables
    protected long intervalSize = -1;

    // additional state variables (not intended as parameters)
    private long numIntervals;
    private long validIntervals;
    private long startInterval;
    private long endInterval;
    
    public IntervalRangeDialog(ProjectionsWindow parentWindow,
			       String titleString)
    {
	super(parentWindow, titleString);
	long initialIntervalSize = 1000;
	validIntervals = totalValidTime/initialIntervalSize;
	if (totalValidTime%initialIntervalSize != 0) {
	    validIntervals++;
	}
	startInterval = startTime/initialIntervalSize;
	endInterval = endTime/initialIntervalSize;
	numIntervals = endInterval - startInterval + 1;
    }
    
    public void actionPerformed(ActionEvent evt) 
    {
	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
	    if (b == bOK) {
		// point user to an inconsistent field.
		JTextField someField = checkConsistent();
		if (someField != null) {
		    someField.selectAll();
		    someField.requestFocus();
		    return;
		} 
	    } else if (b == bUpdate) {
		// update all subclass text fields.
		updateData(sizeField);
	    }
	} else if (evt.getSource() instanceof JTextField) {
	    // this class needs to catch changes to the text fields to
	    // manipulate its own information fields.
	    updateData((JTextField)evt.getSource());
	}
	// let superclass handle its own action routines.
	super.actionPerformed(evt);
    }

    JPanel createMainLayout() {
	JPanel inputPanel = new JPanel();
	JPanel baseMainPanel = super.createMainLayout();
	
	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(2,2,2,2);

	// create interval size panel
	sizePanel = new JPanel();
	sizePanel.setLayout(gbl);
	JLabel sizeLabel = new JLabel("Interval Size :", JLabel.LEFT);
	sizeField = new JTimeTextField(1000, 12);
	sizeField.addActionListener(this);
	sizeField.addKeyListener(this);
	sizeField.addFocusListener(this);

	// temporary variables for convenience
	long startInterval = startTimeField.getValue()/sizeField.getValue();
	long endInterval = endTimeField.getValue()/sizeField.getValue();
	validIntervalsLabel = new JLabel("Valid Total Number of Intervals : " +
					 validIntervals, JLabel.LEFT);
	numIntervalsLabel = new JLabel("Selected Number of Intervals : " +
				       (endInterval - startInterval + 1), 
				       JLabel.LEFT);
	startIntervalLabel = new JLabel("Start Interval : " +
					startInterval, JLabel.LEFT);
	endIntervalLabel = new JLabel("End Interval : " +
				      endInterval, JLabel.LEFT);
	Util.gblAdd(sizePanel, sizeLabel, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(sizePanel, sizeField, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(sizePanel, numIntervalsLabel, gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(sizePanel, validIntervalsLabel, gbc, 0,2, 1,1, 1,1);
	Util.gblAdd(sizePanel, startIntervalLabel, gbc, 0,3, 1,1, 1,1);
	Util.gblAdd(sizePanel, endIntervalLabel, gbc, 1,3, 1,1, 1,1);

	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, sizePanel,      gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JTimeTextField) {
	    long temp = sizeField.getValue();
	    long startInterval = startTimeField.getValue()/temp;
	    long endInterval = endTimeField.getValue()/temp;
	    if (field == sizeField) {
		// temporary variables are used to prevent overwriting
		// old state.
		long validIntervals = totalValidTime/temp;
		if (totalValidTime%temp != 0) {
		    validIntervals++;
		}
		validIntervalsLabel.setText("Total Valid Number of " + 
					    "Intervals : " +
					    validIntervals);
		numIntervalsLabel.setText("Selected Number of Intervals : " +
					  (endInterval - startInterval + 1));
		startIntervalLabel.setText("Start Interval : "+startInterval);
		endIntervalLabel.setText("End Interval : " + endInterval);
	    } else if (field == startTimeField) {
		startIntervalLabel.setText("Start Interval : "+startInterval);
		numIntervalsLabel.setText("Selected Number of Intervals : " +
					  (endInterval - startInterval + 1));
		startTime = startTimeField.getValue();
	    } else if (field == endTimeField) {
		endIntervalLabel.setText("End Interval : " + endInterval);
		numIntervalsLabel.setText("Selected Number of Intervals : " +
					  (endInterval - startInterval + 1));
		endTime = endTimeField.getValue();
	    }
	} 
	super.updateData(field);
    }

    JTextField checkConsistent() {
	// interval size should not be less than or equal to zero us.
	// it should also not be larger than the selected time range.
	if (sizeField.getValue() <= 0 || sizeField.getValue() > totalTime) {
	    return sizeField;
	}
	return super.checkConsistent();
    }

    public boolean isModified() {
	return ((intervalSize != sizeField.getValue()) || super.isModified());
    }

    void setParameters() {
	intervalSize = sizeField.getValue();
	super.setParameters();
    }

    void updateFields() {
	sizeField.setValue(intervalSize);
	super.updateFields();
	updateDerived();
    }

    void updateDerived() {
	long temp = sizeField.getValue();
	long startInterval = startTimeField.getValue()/temp;
	long endInterval = endTimeField.getValue()/temp;
	long validIntervals = totalValidTime/temp;
	if (totalValidTime%temp != 0) {
	    validIntervals++;
	}
	validIntervalsLabel.setText("Total Valid Number of " +
				    "Intervals : " +
				    validIntervals);
	numIntervalsLabel.setText("Selected Number of Intervals : " +
				  (endInterval - startInterval + 1));
	startIntervalLabel.setText("Start Interval : "+startInterval);
	endIntervalLabel.setText("End Interval : " + endInterval);
    }

    // Accessor methods (including convenience accessors for startInterval
    // and endInterval which cannot be set).

    public long getIntervalSize() {
	return sizeField.getValue();
    }

    public void setIntervalSize(long size) {
	this.intervalSize = size;
    }

    public long getStartInterval() {
	return startTimeField.getValue()/sizeField.getValue();
    }

    public long getEndInterval() {
	return endTimeField.getValue()/sizeField.getValue();
    }
}
