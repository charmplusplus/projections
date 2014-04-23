package projections.Tools.Histogram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import projections.gui.JIntTextField;
import projections.gui.JLongTextField;
import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.TimeTextField;
import projections.gui.U;
import projections.gui.Util;

/**
 *  BinDialog
 *  by Chee Wai Lee
 *  2/20/2004 (as TimeBinDialog)
 *  2/23/2005 (renamed)
 *  rewritten 2009
 * 
 *  This is the basic dialog abstraction to allow a user to specify
 *  a range of time in projections for which entities (events) are
 *  to be placed into a user-defined number of bins according to a 
 *  certain time (bin) size.
 *
 *  The obvious use of this is for Histograms of any sort.
 *  
 *  All data is stored inside the text fields.
 */
class BinDialogPanel extends RangeDialogExtensionPanel 
{
	// GUI components
	private JTabbedPane tabbedPane;
	private JPanel timeBinPanel;
	private JPanel msgBinPanel;
	private JPanel idleBinPanel;

	// Time-based bins
	private JLabel timeNumBinsLabel;
	private JLabel timeBinSizeLabel;
	private JLabel timeMinBinSizeLabel;
	private JLabel timeBinRangeLabel;

	private JIntTextField timeNumBinsField;
	private TimeTextField timeBinSizeField;
	private TimeTextField timeMinBinSizeField;

	// Message-based bins
	private JLabel msgNumBinsLabel;
	private JLabel msgBinSizeLabel;
	private JLabel msgMinBinSizeLabel;
	private JLabel msgBinRangeLabel;

	private JIntTextField msgNumBinsField;
	private JLongTextField msgBinSizeField;
	private JLongTextField msgMinBinSizeField;

	// Idle Percentage-based bins
	private JLabel idleNumBinsLabel;
	private JLabel idleBinSizeLabel;
	private JLabel idleMinBinSizeLabel;
	private JLabel idleBinRangeLabel;

	private JIntTextField idleNumBinsField;
	private JLongTextField idleBinSizeField;
	private JLongTextField idleMinBinSizeField;

	// A reference to the parent dialog box that I'm extending
	private RangeDialog parent;


	public BinDialogPanel() {
	
		GridBagLayout gbl      = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2,2,2,2);

		// Time Panel
		timeBinPanel = new JPanel();
		timeBinPanel.setLayout(gbl);
		timeBinPanel.setBorder(new TitledBorder(new LineBorder(Color.black), "TIME-BASED BINS"));

		timeNumBinsLabel = new JLabel("# of Time Bins:", JLabel.LEFT);
		timeNumBinsField = new JIntTextField(-1, 5);

		timeBinSizeLabel = new JLabel("Time Bin Size:", JLabel.LEFT);
		timeBinSizeField = new TimeTextField(-1, 12);

		timeMinBinSizeLabel = new JLabel("Starting Bin Size:", JLabel.LEFT);
		timeMinBinSizeField = new TimeTextField(-1, 12);

		timeBinRangeLabel = new JLabel("", JLabel.LEFT);

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
		msgNumBinsField = new JIntTextField(-1, 5);

		msgBinSizeLabel = new JLabel("Bin Size (bytes):", JLabel.LEFT);
		msgBinSizeField = new JLongTextField(-1, 12);

		msgMinBinSizeLabel = new JLabel("Starting Bin Size:", 
				JLabel.LEFT);
		msgMinBinSizeField = new JLongTextField(-1, 12);

		msgBinRangeLabel = new JLabel("", JLabel.LEFT);

		Util.gblAdd(msgBinPanel, msgNumBinsLabel,    gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(msgBinPanel, msgNumBinsField,    gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(msgBinPanel, msgBinSizeLabel,    gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(msgBinPanel, msgBinSizeField,    gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(msgBinPanel, msgMinBinSizeLabel, gbc, 0,1, 2,1, 1,1);
		Util.gblAdd(msgBinPanel, msgMinBinSizeField, gbc, 1,1, 2,1, 1,1);
		Util.gblAdd(msgBinPanel, msgBinRangeLabel,   gbc, 0,2, 4,1, 1,1);

		// Idle Percentages Panel
		idleBinPanel = new JPanel();
		idleBinPanel.setLayout(gbl);
		idleBinPanel.setBorder(new TitledBorder(new LineBorder(Color.black),
		"IDLE PERCENTAGE-BASED BINS"));

		idleNumBinsLabel = new JLabel("# of Idle % Bins:", JLabel.LEFT);
		idleNumBinsField = new JIntTextField(-1,5);

		idleBinSizeLabel = new JLabel("Idle % Bin Size", JLabel.LEFT);
		idleBinSizeField = new JLongTextField(-1,12);

		idleMinBinSizeLabel = new JLabel("Starting Bin Size:", JLabel.LEFT);
		idleMinBinSizeField = new JLongTextField(-1,12);

		idleBinRangeLabel = new JLabel("", JLabel.LEFT);

		Util.gblAdd(idleBinPanel, idleNumBinsLabel,    gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(idleBinPanel, idleNumBinsField,    gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(idleBinPanel, idleBinSizeLabel,    gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(idleBinPanel, idleBinSizeField,    gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(idleBinPanel, idleMinBinSizeLabel, gbc, 0,1, 2,1, 1,1);
		Util.gblAdd(idleBinPanel, idleMinBinSizeField, gbc, 1,1, 2,1, 1,1);
		Util.gblAdd(idleBinPanel, idleBinRangeLabel,   gbc, 0,2, 4,1, 1,1);
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Time", null, timeBinPanel, "Time-based bins");
		tabbedPane.addTab("Msgs", null, msgBinPanel, "Message Sizes");
		tabbedPane.addTab("Idle %", null, idleBinPanel, "Idle percentage-based bins");

		this.setLayout(new BorderLayout());
		this.add(tabbedPane, BorderLayout.CENTER);

	}


	// Accessor methods
	public int getTimeNumBins() {
		return timeNumBinsField.getValue();
	}

	public void setTimeNumBins(int numBins) {
		timeNumBinsField.setValue(numBins);
		parent.someInputChanged();
	}

	public long getTimeBinSize() {
		return timeBinSizeField.getValue();
	}

	public void setTimeBinSize(long binSize) {
		timeBinSizeField.setValue(binSize);
		parent.someInputChanged();
	}

	public long getTimeMinBinSize() {
		return timeMinBinSizeField.getValue();
	}

	public void setTimeMinBinSize(long size) {
		timeMinBinSizeField.setValue(size);
		parent.someInputChanged();
	}

	// Messages
	public int getMsgNumBins() {
		return msgNumBinsField.getValue();
	}

	public void setMsgNumBins(int numBins) {
		msgNumBinsField.setValue(numBins);
		parent.someInputChanged();
	}

	public long getMsgBinSize() {
		return msgBinSizeField.getValue();
	}

	public void setMsgBinSize(long binSize) {
		msgBinSizeField.setValue(binSize);
		parent.someInputChanged();
	}

	public long getMsgMinBinSize() {
		return msgMinBinSizeField.getValue();
	}

	public void setMsgMinBinSize(long size) {
		msgMinBinSizeField.setValue(size);
		parent.someInputChanged();
	}

	// Idle Percentages
	public int getIdleNumBins() {
		return idleNumBinsField.getValue();
	}

	public void setIdleNumBins(int numBins) {
		idleNumBinsField.setValue(numBins);
		parent.someInputChanged();
	}

	public long getIdleBinSize() {
		return idleBinSizeField.getValue();
	}

	public void setIdleBinSize(long binSize) {
		idleBinSizeField.setValue(binSize);
		parent.someInputChanged();
	}

	public long getIdleMinBinSize() {
		return idleMinBinSizeField.getValue();
	}

	public void setIdleMinBinSize(long size) {
		idleMinBinSizeField.setValue(size);
		parent.someInputChanged();
	}

	public boolean isInputValid() {
		return true;	
	}

	public void setInitialFields() {
		
		// default values for time 1ms to 100ms
		timeNumBinsField.setText("100");
		timeBinSizeField.setText("1000");
		timeMinBinSizeField.setText("0");

		// default values for messages 100 bytes to 2k
		msgNumBinsField.setText("200");
		msgBinSizeField.setText("100");
		msgMinBinSizeField.setText("0");

		// default values for idle percentages at 1% resolution to 100% total
		idleNumBinsField.setText("100");
		idleBinSizeField.setText("1");
		idleMinBinSizeField.setText("0");
	
		updateFields();
	}

	public void setParentDialogBox(RangeDialog parent) {
		this.parent = parent;
		timeNumBinsField.addActionListener(parent);
		timeNumBinsField.addKeyListener(parent);
		timeNumBinsField.addFocusListener(parent);
		timeBinSizeField.addActionListener(parent);
		timeBinSizeField.addKeyListener(parent);
		timeBinSizeField.addFocusListener(parent);
		timeMinBinSizeField.addActionListener(parent);
		timeMinBinSizeField.addKeyListener(parent);
		timeMinBinSizeField.addFocusListener(parent);
		msgNumBinsField.addActionListener(parent);
		msgNumBinsField.addKeyListener(parent);
		msgNumBinsField.addFocusListener(parent);
		msgBinSizeField.addActionListener(parent);
		msgBinSizeField.addKeyListener(parent);
		msgBinSizeField.addFocusListener(parent);
		msgMinBinSizeField.addActionListener(parent);
		msgMinBinSizeField.addKeyListener(parent);
		msgMinBinSizeField.addFocusListener(parent);
		idleNumBinsField.addActionListener(parent);
		idleNumBinsField.addKeyListener(parent);
		idleNumBinsField.addFocusListener(parent);
		idleBinSizeField.addActionListener(parent);
		idleBinSizeField.addKeyListener(parent);
		idleBinSizeField.addFocusListener(parent);
		idleMinBinSizeField.addActionListener(parent);
		idleMinBinSizeField.addKeyListener(parent);
		idleMinBinSizeField.addFocusListener(parent);
	}

	public void updateFields() {

		timeBinRangeLabel.setText("Bin size ranges from : " +
				U.humanReadableString(timeMinBinSizeField.getValue()) +
				" to " +
				U.humanReadableString(timeMinBinSizeField.getValue() +
						timeNumBinsField.getValue() *
						timeBinSizeField.getValue()));

		DecimalFormat _format = new DecimalFormat();

		msgBinRangeLabel.setText("Bin size ranges from : " +
				_format.format(msgMinBinSizeField.getValue()) +
				" bytes to " + 
				_format.format(msgMinBinSizeField.getValue() +
						msgNumBinsField.getValue() *
						msgBinSizeField.getValue()) + " bytes.");

		idleBinRangeLabel.setText("Bin size ranges from : " +
				_format.format(idleMinBinSizeField.getValue()) + 
				"% to " +
				_format.format(idleMinBinSizeField.getValue() +
							idleNumBinsField.getValue() *
							idleBinSizeField.getValue()) + "%.");
	}


	public int getSelectedType()
	{
		if(tabbedPane.getSelectedComponent() == timeBinPanel)
		{
			return HistogramWindow.TYPE_TIME;
		}
		else if(tabbedPane.getSelectedComponent() == msgBinPanel)
		{
			return HistogramWindow.TYPE_MSG_SIZE;
		}
		else return HistogramWindow.TYPE_IDLE_PERC;
	}	
}
