package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  TimeBinDialog
 *  by Chee Wai Lee
 *  2/20/2004
 * 
 *  Formerly named SimpleThresholdDialog which was ill-conceived.
 *
 *  This is the basic dialog abstraction to allow a user to specify
 *  a range of time in projections for which entities (events) are
 *  to be placed into a user-defined number of bins according to a 
 *  certain time (bin) size.
 *
 *  A corresponding time-based threshold value can be applied.
 *
 *  The obvious use of this is for Histograms of any sort.
 */
public class TimeBinDialog extends RangeDialog 
{
    // GUI components
    protected JPanel binPanel;
    protected JLabel numBinsLabel;
    protected JLabel binSizeLabel;
    protected JLabel thresholdLabel;

    protected JIntTextField numBinsField;
    protected JTimeTextField binSizeField;
    protected JTimeTextField thresholdField; 

    protected int numBins;
    protected long binSize;
    protected long threshold; // time in milliseconds

    public TimeBinDialog(ProjectionsWindow mainWindow,
			 String titleString) {
	super(mainWindow, titleString);
	// default values
	numBins = 100;
	binSize = 10000;
	threshold = 1000;
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

	numBinsLabel = new JLabel("Number of Bins:", JLabel.LEFT);
	numBinsField = new JIntTextField(numBins, 5);
	numBinsField.addActionListener(this);
	numBinsField.addKeyListener(this);
	numBinsField.addFocusListener(this);

	binSizeLabel = new JLabel("Size of Bin:", JLabel.LEFT);
	binSizeField = new JTimeTextField(binSize, 12);
	binSizeField.addActionListener(this);
	binSizeField.addKeyListener(this);
	binSizeField.addFocusListener(this);

	thresholdLabel = new JLabel("Min threshold:", JLabel.LEFT);
	thresholdField = new JTimeTextField(threshold, 12);
	thresholdField.addActionListener(this);
	thresholdField.addKeyListener(this);
	thresholdField.addFocusListener(this);

	binPanel = new JPanel();
	binPanel.setLayout(gbl);
	Util.gblAdd(binPanel, numBinsLabel,   gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(binPanel, numBinsField,   gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(binPanel, binSizeLabel,   gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(binPanel, binSizeField,   gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(binPanel, thresholdLabel, gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(binPanel, thresholdField, gbc, 1,1, 1,1, 1,1);

	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, binPanel,       gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JIntTextField) {
	    if (field == numBinsField) {
		numBins = numBinsField.getValue();
	    }
	} else if (field instanceof JTimeTextField) {
	    if (field == binSizeField) {
		binSize = binSizeField.getValue();
	    }
	}
	super.updateData(field);
    }

    JTextField checkConsistent() {
	return super.checkConsistent();
    }

    public boolean isModified() {
	return false;
    }

    void setParameters() {
	super.setParameters();
    }

    void updateFields() {
	super.updateFields();
	updateDerived();
    }

    void updateDerived() {
	// this class has no derived information.
	// this method is included for completeness.
    }

    // Accessor methods

    public int getNumBins() {
	return numBins;
    }

    public void setNumBins(int numBins) {
	this.numBins = numBins;
    }

    public long getBinSize() {
	return binSize;
    }

    public void setBinSize(long binSize) {
	this.binSize = binSize;
    }

    public long getThreshold() {
	return threshold;
    }

    public void setThreshold(long threshold) {
	this.threshold = threshold;
    }
}
