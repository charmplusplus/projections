package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  TimeBinDialog
 *  by Chee Wai Lee
 *  2/20/2004
 * 
 *  This is the basic dialog abstraction to allow a user to specify
 *  a range of time in projections for which entities (events) are
 *  to be placed into a user-defined number of bins according to a 
 *  certain time (bin) size.
 *
 *  The obvious use of this is for Histograms of any sort.
 */
public class TimeBinDialog extends RangeDialog 
{
    // GUI components
    protected JPanel binPanel;
    protected JLabel numBinsLabel;
    protected JLabel binSizeLabel;
    protected JLabel minBinSizeLabel;
    protected JLabel binRangeLabel;

    protected JIntTextField numBinsField;
    protected JTimeTextField binSizeField;
    protected JTimeTextField minBinSizeField;

    protected int numBins;
    protected long binSize;
    protected long minBinSize;

    public TimeBinDialog(ProjectionsWindow mainWindow,
			 String titleString) {
	super(mainWindow, titleString);
	// default values
	numBins = 100;
	binSize = 100;
	minBinSize = 0;
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
		setBinRangeText();
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

	minBinSizeLabel = new JLabel("Starting Bin Size:", JLabel.LEFT);
	minBinSizeField = new JTimeTextField(minBinSize, 12);
	minBinSizeField.addActionListener(this);
	minBinSizeField.addKeyListener(this);
	minBinSizeField.addFocusListener(this);

	binRangeLabel = new JLabel("", JLabel.LEFT);
	setBinRangeText();

	binPanel = new JPanel();
	binPanel.setLayout(gbl);
	Util.gblAdd(binPanel, numBinsLabel,    gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(binPanel, numBinsField,    gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(binPanel, binSizeLabel,    gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(binPanel, binSizeField,    gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(binPanel, minBinSizeLabel, gbc, 0,1, 2,1, 1,1);
	Util.gblAdd(binPanel, minBinSizeField, gbc, 1,1, 2,1, 1,1);
	Util.gblAdd(binPanel, binRangeLabel,   gbc, 0,2, 4,1, 1,1);

	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, binPanel,       gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JIntTextField) {
	    if (field == numBinsField) {
		numBins = numBinsField.getValue();
		setBinRangeText();
	    }
	} else if (field instanceof JTimeTextField) {
	    if (field == binSizeField) {
		binSize = binSizeField.getValue();
	    } else if (field == minBinSizeField) {
		minBinSize = minBinSizeField.getValue();
	    }
	    setBinRangeText();
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

    void setBinRangeText() {
	binRangeLabel.setText("Bin size ranges from : " +
			      U.t(minBinSizeField.getValue()) +
			      " to " + 
			      U.t(minBinSizeField.getValue() +
				  numBinsField.getValue() *
				  binSizeField.getValue()));
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

    public long getMinBinSize() {
	return minBinSize;
    }

    public void setMinBinSize(long size) {
	this.minBinSize = size;
    }
}
