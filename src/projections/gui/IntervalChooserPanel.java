package projections.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
public class IntervalChooserPanel extends RangeDialogExtensionPanel implements ItemListener
{

	// Additional GUI objects
	private TimeTextField sizeField;
	private JLabel numIntervalsLabel;
	private JLabel sizeLabel;
	JCheckBox t;

	private final int DEFAULT_NUM_INTERVALS = 1000;

	// A reference to the parent dialog box that I'm extending
	private RangeDialog parent;


	public IntervalChooserPanel() {

		// create interval size panel

		this.setLayout(new GridBagLayout());
		sizeLabel = new JLabel("Interval Size:", JLabel.LEFT);
		sizeField = new TimeTextField("1ms", 12); // will verify the the time is a valid time format before allowing the user to get out of the input box
		sizeLabel.setBorder(BorderFactory.createEmptyBorder(3, 40, 3, 3));

		numIntervalsLabel = new JLabel(" ", JLabel.LEFT);
		numIntervalsLabel.setBorder(BorderFactory.createEmptyBorder(3, 40, 3, 3));
		
		t = new JCheckBox("Manually Specify Resolution (# of intervals):");
		t.setSelected(false);
		t.addItemListener(this);

		createLayout(t.isSelected());
	}

	private void createLayout(boolean manuallySpecify){
		this.removeAll();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2,2,2,2);

		if(manuallySpecify){
			Util.gblAdd(this, t, gbc, 0,0, 1,1, 1,1);
			Util.gblAdd(this, sizeLabel, gbc, 0,1, 1,1, 1,1);
			Util.gblAdd(this, sizeField, gbc, 1,1, 1,1, 1,1);
			Util.gblAdd(this, numIntervalsLabel, gbc, 0,2, 1,1, 1,1);
		} else {
			Util.gblAdd(this, t, gbc, 0,0, 1,1, 1,1);
			Util.gblAdd(this, numIntervalsLabel, gbc, 0,2, 1,1, 1,1);	
		}

		repackParentWindow();
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

		if(! t.isSelected()){
			// Automatically determine the interval width
			long duration = parent.getEndTime() - parent.getStartTime();
			sizeField.setText(U.humanReadableString(duration/DEFAULT_NUM_INTERVALS));
		}

		numIntervalsLabel.setText("Selected Number of Intervals : " + getNumSelectedIntervals() );

	}

	
	private void repackParentWindow(){
		if(parent != null && parent.isVisible()){
			parent.pack();
		}
	}

	private long getNumSelectedIntervals(){
		return getEndInterval() - getStartInterval() + 1;
	}

	public boolean isInputValid() {
		
		// This one is just a warning, so don't return false
		if(getNumSelectedIntervals() < 25){
			numIntervalsLabel.setText("Selected Number of Intervals : " + getNumSelectedIntervals() );
			numIntervalsLabel.setForeground(Color.red);
		} else {
			numIntervalsLabel.setForeground(Color.black);
		}

		
		// This is an actual error, so return false
		if (sizeField.getValue() <= 0 || sizeField.getValue() > parent.getTotalTime()) 	{
			// interval size should not be less than or equal to zero us.
			// it should also not be larger than the selected time range.
			sizeLabel.setForeground(Color.red);
			sizeField.setForeground(Color.red);
			return false;
		} 

		// reset to the normal colors
		sizeLabel.setForeground(Color.black);
		sizeField.setForeground(Color.black);
		return true;
	}


	// Accessor methods (including convenience accessors for startInterval
	// and endInterval which cannot be set).

	public long getIntervalSize() {
		return sizeField.getValue();
	}


	public long getStartInterval() {
		return parent.getStartTime()/sizeField.getValue();
	}

	public long getEndInterval() {
		return parent.getEndTime()/sizeField.getValue();
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == t){
			createLayout(t.isSelected());
			updateFields();
		}
	}


}
