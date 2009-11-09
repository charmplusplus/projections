package projections.gui;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
public class IntervalChooserPanel extends RangeDialogExtensionPanel
{

	// Additional GUI objects
	JTimeTextField sizeField;
	JLabel numIntervalsLabel;
	JLabel validIntervalsLabel;
	JLabel startIntervalLabel;
	JLabel endIntervalLabel;

	// A reference to the parent dialog box that I'm extending
	RangeDialog parent;

	// dialog parameter variables
	public long intervalSize;

	public IntervalChooserPanel() {

		GridBagLayout gbl      = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2,2,2,2);

		// create interval size panel

		this.setLayout(gbl);
		JLabel sizeLabel = new JLabel("Interval Size :", JLabel.LEFT);
		sizeField = new JTimeTextField(1000, 12); // will verify the the time is a valid time format before allowing the user to get out of the input box

		validIntervalsLabel = new JLabel("", JLabel.LEFT);
		numIntervalsLabel = new JLabel("", JLabel.LEFT);
		startIntervalLabel = new JLabel("", JLabel.LEFT);
		endIntervalLabel = new JLabel("", JLabel.LEFT);

		Util.gblAdd(this, sizeLabel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(this, sizeField, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(this, numIntervalsLabel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(this, validIntervalsLabel, gbc, 0,2, 1,1, 1,1);
		Util.gblAdd(this, startIntervalLabel, gbc, 0,3, 1,1, 1,1);
		Util.gblAdd(this, endIntervalLabel, gbc, 1,3, 1,1, 1,1);

	}

	public void setParentDialogBox(RangeDialog parent) {
		this.parent = parent;	
		sizeField.addActionListener(parent);
		sizeField.addKeyListener(parent);
		sizeField.addFocusListener(parent);
	}

	public void setInitialFields(){
		// Initially we should have about 200 intervals
		long initialIntervals = 200;
		long timePerInterval = parent.getSelectedTotalTime()/initialIntervals;
		sizeField.setValue(timePerInterval);

		updateFields();
	}

	/** derive all the values from the integer in sizeField */	
	public void updateFields(){

		long temp = sizeField.getValue();
		long startInterval = parent.getStartTime()/temp;
		long endInterval = parent.getEndTime()/temp;
		long validIntervals = parent.getSelectedTotalTime()/temp;
		if (parent.getSelectedTotalTime()%temp != 0) {
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


	public boolean isInputValid() {
		// interval size should not be less than or equal to zero us.
		// it should also not be larger than the selected time range.
		if (sizeField.getValue() <= 0 || sizeField.getValue() > parent.getTotalTime()) 	{
			sizeField.requestFocus();
			return false;
		}
		else
			return true;
	}

	
	// Accessor methods (including convenience accessors for startInterval
	// and endInterval which cannot be set).

	public long getIntervalSize() {
		return sizeField.getValue();
	}

	public void setIntervalSize(long size) {
		this.intervalSize = size;
		parent.someInputChanged();
	}

	public long getStartInterval() {
		return parent.getStartTime()/sizeField.getValue();
	}

	public long getEndInterval() {
		return parent.getEndTime()/sizeField.getValue();
	}


}
