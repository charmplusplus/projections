package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  SimpleThresholdDialog
 *  by Chee Wai Lee
 *  2/20/2004
 * 
 *  SimpleThresholdDialog provides a dialog for tools (windows) to impose 
 *  a single value-based filter on data display.
 *
 *  For these purposes, the same dialog supporting a double-based value
 *  field is sufficient and the semantics may differ only by the supported
 *  type of the threshold. 
 * 
 *  Hence, developers of Projections should look at whether it is sufficient
 *  to introduce a new semantic type before attempting to develop a new
 *  subclass of SimpleThresholdDialog.
 */
public class SimpleThresholdDialog extends IntervalRangeDialog 
{
    // threshold semantic types
    // These semantic type "constants" should 
    // make no sense to any subclass and hence should be redefined
    public static final int NUM_TYPES = 1;
    public static final int TIME = 0;

    protected int thresholdType;

    // GUI components
    protected JPanel thresholdPanel;
    protected JLabel thresholdLabel;
    // the actual class for thresholdField is currently unknown and
    // depends on the type.
    protected JTextField thresholdField; 

    // threshold value. This value should not have any semantic meaning
    // for this tool to be general but is expected to be some kind of
    // time threshold (in usecs) to be applied to entry method execution
    // times.
    protected double threshold;

    public SimpleThresholdDialog(ProjectionsWindow mainWindow,
				 String titleString,
				 int thresholdType) {
	super(mainWindow, titleString);
	this.thresholdType = thresholdType;
	// setting up the initial threshold field value.
	threshold = 1000.0;
    }
    
    public void actionPerformed(ActionEvent evt) {
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

	createTypeDependentFields();
	thresholdField.addActionListener(this);
	thresholdField.addKeyListener(this);
	thresholdField.addFocusListener(this);

	thresholdPanel = new JPanel();
	thresholdPanel.setLayout(gbl);
	Util.gblAdd(thresholdPanel, thresholdLabel, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(thresholdPanel, thresholdField, gbc, 1,0, 1,1, 1,1);

	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, thresholdPanel, gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    void createTypeDependentFields() {
	switch(thresholdType) {
	case TIME:
	    thresholdLabel = new JLabel("Threshold time:", 
					JLabel.LEFT);
	    thresholdField = new JTimeTextField((long)threshold, 12);
	    break;
	default:
	    System.err.println("Internal Projections Error! Unknown " +
			       "threshold type for SimpleThresholdDialog. " +
			       "Please report to developers!");
	    System.exit(-1);
	}
    }

    void updateData(JTextField field) {
	super.updateData(field);
    }

    JTextField checkConsistent() {
	// The threshold should not be less than zero. It can, however,
	// be larger than anything else on the dialog.
	switch(thresholdType) {
	case TIME:
	    if (((JTimeTextField)thresholdField).getValue() < 0) {
		return thresholdField;
	    }
	    break;
	default:
	    System.err.println("Internal Projections Error! Unknown " +
			       "threshold type for SimpleThresholdDialog. " +
			       "Please report to developers!");
	    System.exit(-1);
	}
	return super.checkConsistent();
    }

    public boolean isModified() {
	switch(thresholdType) {
	case TIME:
	    return ((threshold != 
		     (double)((JTimeTextField)thresholdField).getValue()) || 
		    super.isModified());
	default:
	    System.err.println("Internal Projections Error! Unknown " +
			       "threshold type for SimpleThresholdDialog. " +
			       "Please report to developers!");
	    System.exit(-1);
	}
	return false;
    }

    void setParameters() {
	switch(thresholdType) {
	case TIME:
	    threshold = (double)((JTimeTextField)thresholdField).getValue();
	    break;
	default:
	    System.err.println("Internal Projections Error! Unknown " +
			       "threshold type for SimpleThresholdDialog. " +
			       "Please report to developers!");
	    System.exit(-1);
	}
	super.setParameters();
    }

    void updateFields() {
	switch(thresholdType) {
	case TIME:
	    ((JTimeTextField)thresholdField).setValue((long)threshold);
	    break;
	default:
	    System.err.println("Internal Projections Error! Unknown " +
			       "threshold type for SimpleThresholdDialog. " +
			       "Please report to developers!");
	    System.exit(-1);
	}
	super.updateFields();
	updateDerived();
    }

    void updateDerived() {
	// this class has no derived information.
	// this method is included for completeness.
    }

    // Accessor methods

    public double getThreshold() {
	return threshold;
    }

    public void setThreshold(double threshold) {
	this.threshold = threshold;
    }
}
