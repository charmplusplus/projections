package projections.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
public class IntervalChooserPanel extends RangeDialogExtensionPanel
{

	// Additional GUI objects
	TimeTextField sizeField;
	JLabel numIntervalsLabel;
//	JLabel validIntervalsLabel;
//	JLabel startIntervalLabel;
//	JLabel endIntervalLabel;

	// A reference to the parent dialog box that I'm extending
	RangeDialog parent;

	// dialog parameter variables
	public long intervalSize;
	JLabel sizeLabel;
	

	public IntervalChooserPanel() {
		this(1000);
	}
	
	public IntervalChooserPanel(long defaultIntervalSize) {

		GridBagLayout gbl      = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2,2,2,2);

		// create interval size panel

		this.setLayout(gbl);
		sizeLabel = new JLabel("Interval Size (Resolution) :", JLabel.LEFT);
		sizeField = new TimeTextField(defaultIntervalSize, 12); // will verify the the time is a valid time format before allowing the user to get out of the input box

		numIntervalsLabel = new JLabel("", JLabel.LEFT);
//		validIntervalsLabel = new JLabel("", JLabel.LEFT);
//		startIntervalLabel = new JLabel("", JLabel.LEFT);
//		endIntervalLabel = new JLabel("", JLabel.LEFT);

		Util.gblAdd(this, sizeLabel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(this, sizeField, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(this, numIntervalsLabel, gbc, 0,1, 1,1, 1,1);
//		Util.gblAdd(this, validIntervalsLabel, gbc, 0,2, 1,1, 1,1);
//		Util.gblAdd(this, startIntervalLabel, gbc, 0,3, 1,1, 1,1);
//		Util.gblAdd(this, endIntervalLabel, gbc, 1,3, 1,1, 1,1);

	}


	public void setParentDialogBox(RangeDialog parent) {
		this.parent = parent;	
		sizeField.addActionListener(parent);
		sizeField.addKeyListener(parent);
		sizeField.addFocusListener(parent);
	}

	public void setInitialFields(){
		updateFields();
	}

	/** derive all the values from the integer in sizeField */
	public void updateFields(){
//		validIntervalsLabel.setText("Total Valid Number of Intervals : " + getNumValidIntervals());
//		startIntervalLabel.setText("Start Interval : "+getStartInterval() );
//		endIntervalLabel.setText("End Interval : " + getEndInterval() );

		if(getNumSelectedIntervals() < 25){
			numIntervalsLabel.setText("Selected Number of Intervals : " + getNumSelectedIntervals() );
			numIntervalsLabel.setForeground(Color.red);
		} else {
			numIntervalsLabel.setText("Selected Number of Intervals : " + getNumSelectedIntervals() );
		}
	
	}

	private long getNumValidIntervals(){
		long validIntervals = parent.getTotalTime()/sizeField.getValue();
		if (parent.getSelectedTotalTime()%sizeField.getValue() != 0) {
			validIntervals++;
		}
		return validIntervals;
	}
	
	private long getNumSelectedIntervals(){
		return getEndInterval() - getStartInterval() + 1;
	}

	public boolean isInputValid() {
		
		if (sizeField.getValue() <= 0 || sizeField.getValue() > parent.getTotalTime()) 	{
			// interval size should not be less than or equal to zero us.
			// it should also not be larger than the selected time range.
			sizeLabel.setForeground(Color.red);
			sizeField.setForeground(Color.red);
			sizeField.requestFocus();
			return false;
		} 
				
		// reset to the normal colors
		numIntervalsLabel.setForeground(Color.black);
		sizeLabel.setForeground(Color.black);
		sizeField.setForeground(Color.black);
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
