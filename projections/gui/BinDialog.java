package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;

/**
 *  BinDialog
 *  by Chee Wai Lee
 *  2/20/2004 (as TimeBinDialog)
 *  2/23/2005 (renamed)
 * 
 *  This is the basic dialog abstraction to allow a user to specify
 *  a range of time in projections for which entities (events) are
 *  to be placed into a user-defined number of bins according to a 
 *  certain time (bin) size.
 *
 *  The obvious use of this is for Histograms of any sort.
 */
public class BinDialog extends RangeDialog 
{
	// GUI components
    protected JTabbedPane binPanel;
    protected JPanel timeBinPanel;
    protected JPanel msgBinPanel;

    // Time-based bins
    protected JLabel timeNumBinsLabel;
    protected JLabel timeBinSizeLabel;
    protected JLabel timeMinBinSizeLabel;
    protected JLabel timeBinRangeLabel;

    protected JIntTextField timeNumBinsField;
    protected JTimeTextField timeBinSizeField;
    protected JTimeTextField timeMinBinSizeField;

    // Message-based bins
    protected JLabel msgNumBinsLabel;
    protected JLabel msgBinSizeLabel;
    protected JLabel msgMinBinSizeLabel;
    protected JLabel msgBinRangeLabel;

    protected JIntTextField msgNumBinsField;
    protected JLongTextField msgBinSizeField;
    protected JLongTextField msgMinBinSizeField;

    // Dialog attributes
    protected int timeNumBins;
    protected long timeBinSize;
    protected long timeMinBinSize;

    protected int msgNumBins;
    protected long msgBinSize;
    protected long msgMinBinSize;

    private static final int TIME_DATA = 0;
    private static final int BYTES_DATA = 1;
    private DecimalFormat _format;

    public BinDialog(ProjectionsWindow mainWindow,
			 String titleString) {
	super(mainWindow, titleString, null);
	// default values for time 1ms to 100ms
	timeNumBins = 100;
	timeBinSize = 1000;
	timeMinBinSize = 0;

	// default values for messages 100 bytes to 2k
	msgNumBins = 200;
	msgBinSize = 100;
	msgMinBinSize = 0;

	_format = new DecimalFormat();
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
		setBinRangeText(TIME_DATA);
		setBinRangeText(BYTES_DATA);
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

	// Time Panel
	timeBinPanel = new JPanel();
	timeBinPanel.setLayout(gbl);
	timeBinPanel.setBorder(new TitledBorder(new LineBorder(Color.black),
						"TIME-BASED BINS"));

	timeNumBinsLabel = new JLabel("# of Time Bins:", JLabel.LEFT);
	timeNumBinsField = new JIntTextField(timeNumBins, 5);
	timeNumBinsField.addActionListener(this);
	timeNumBinsField.addKeyListener(this);
	timeNumBinsField.addFocusListener(this);

	timeBinSizeLabel = new JLabel("Time Bin Size:", JLabel.LEFT);
	timeBinSizeField = new JTimeTextField(timeBinSize, 12);
	timeBinSizeField.addActionListener(this);
	timeBinSizeField.addKeyListener(this);
	timeBinSizeField.addFocusListener(this);

	timeMinBinSizeLabel = new JLabel("Starting Bin Size:", 
					 JLabel.LEFT);
	timeMinBinSizeField = new JTimeTextField(timeMinBinSize, 12);
	timeMinBinSizeField.addActionListener(this);
	timeMinBinSizeField.addKeyListener(this);
	timeMinBinSizeField.addFocusListener(this);

	timeBinRangeLabel = new JLabel("", JLabel.LEFT);
	setBinRangeText(TIME_DATA);

	Util.gblAdd(timeBinPanel, timeNumBinsLabel,    gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(timeBinPanel, timeNumBinsField,    gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(timeBinPanel, timeBinSizeLabel,    gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(timeBinPanel, timeBinSizeField,    gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(timeBinPanel, timeMinBinSizeLabel, gbc, 0,1, 2,1, 1,1);
	Util.gblAdd(timeBinPanel, timeMinBinSizeField, gbc, 1,1, 2,1, 1,1);
	Util.gblAdd(timeBinPanel, timeBinRangeLabel,   gbc, 0,2, 4,1, 1,1);

	// Messages Panel
	msgBinPanel = new JPanel();
	msgBinPanel.setLayout(gbl);
	msgBinPanel.setBorder(new TitledBorder(new LineBorder(Color.black),
					       "MESSAGE-BASED BINS"));

	msgNumBinsLabel = new JLabel("# of Msg Bins:", JLabel.LEFT);
	msgNumBinsField = new JIntTextField(msgNumBins, 5);
	msgNumBinsField.addActionListener(this);
	msgNumBinsField.addKeyListener(this);
	msgNumBinsField.addFocusListener(this);

	msgBinSizeLabel = new JLabel("Bin Size (bytes):", JLabel.LEFT);
	msgBinSizeField = new JLongTextField(msgBinSize, 12);
	msgBinSizeField.addActionListener(this);
	msgBinSizeField.addKeyListener(this);
	msgBinSizeField.addFocusListener(this);

	msgMinBinSizeLabel = new JLabel("Starting Bin Size:", 
					JLabel.LEFT);
	msgMinBinSizeField = new JLongTextField(msgMinBinSize, 12);
	msgMinBinSizeField.addActionListener(this);
	msgMinBinSizeField.addKeyListener(this);
	msgMinBinSizeField.addFocusListener(this);

	msgBinRangeLabel = new JLabel("", JLabel.LEFT);
	setBinRangeText(BYTES_DATA);

	Util.gblAdd(msgBinPanel, msgNumBinsLabel,    gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(msgBinPanel, msgNumBinsField,    gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(msgBinPanel, msgBinSizeLabel,    gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(msgBinPanel, msgBinSizeField,    gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(msgBinPanel, msgMinBinSizeLabel, gbc, 0,1, 2,1, 1,1);
	Util.gblAdd(msgBinPanel, msgMinBinSizeField, gbc, 1,1, 2,1, 1,1);
	Util.gblAdd(msgBinPanel, msgBinRangeLabel,   gbc, 0,2, 4,1, 1,1);

	binPanel = new JTabbedPane();
	binPanel.addTab("Time", null, timeBinPanel, "Time-based bins");
	binPanel.addTab("Msgs", null, msgBinPanel, "Message Sizes");

	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, binPanel,       gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JIntTextField) {
	    if (field == timeNumBinsField) {
		timeNumBins = timeNumBinsField.getValue();
		setBinRangeText(TIME_DATA);
	    } else if (field == msgNumBinsField) {
		msgNumBins = msgNumBinsField.getValue();
		setBinRangeText(BYTES_DATA);
	    }
	} else if (field instanceof JLongTextField) {
	    if (field == msgBinSizeField) {
		msgBinSize = msgBinSizeField.getValue();
	    } else if (field == msgMinBinSizeField) {
		msgMinBinSize = msgMinBinSizeField.getValue();
	    }
	    setBinRangeText(BYTES_DATA);
	} else if (field instanceof JTimeTextField) {
	    if (field == timeBinSizeField) {
		timeBinSize = timeBinSizeField.getValue();
	    } else if (field == timeMinBinSizeField) {
		timeMinBinSize = timeMinBinSizeField.getValue();
	    }
	    setBinRangeText(TIME_DATA);
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

    void setBinRangeText(int datatype) {
	switch (datatype) {
	case TIME_DATA:
	    timeBinRangeLabel.setText("Bin size ranges from : " +
				      U.t(timeMinBinSizeField.getValue()) +
				      " to " +
				      U.t(timeMinBinSizeField.getValue() +
					  timeNumBinsField.getValue() *
					  timeBinSizeField.getValue()));
	    break;
	case BYTES_DATA:
	    msgBinRangeLabel.setText("Bin size ranges from : " +
				     _format.format(msgMinBinSizeField.getValue()) +
				     " bytes to " + 
				     _format.format(msgMinBinSizeField.getValue() +
						    msgNumBinsField.getValue() *
						    msgBinSizeField.getValue()) +
				     " bytes.");
	    break;
	}
    }

    // Accessor methods
    // Time
    public int getTimeNumBins() {
	return timeNumBins;
    }

    public void setTimeNumBins(int numBins) {
	this.timeNumBins = numBins;
    }

    public long getTimeBinSize() {
	return timeBinSize;
    }

    public void setTimeBinSize(long binSize) {
	this.timeBinSize = binSize;
    }

    public long getTimeMinBinSize() {
	return timeMinBinSize;
    }

    public void setTimeMinBinSize(long size) {
	this.timeMinBinSize = size;
    }

    // Messages
    public int getMsgNumBins() {
	return msgNumBins;
    }

    public void setMsgNumBins(int numBins) {
	this.msgNumBins = numBins;
    }

    public long getMsgBinSize() {
	return msgBinSize;
    }

    public void setMsgBinSize(long binSize) {
	this.msgBinSize = binSize;
    }

    public long getMsgMinBinSize() {
	return msgMinBinSize;
    }

    public void setMsgMinBinSize(long size) {
	this.msgMinBinSize = size;
    }
}
