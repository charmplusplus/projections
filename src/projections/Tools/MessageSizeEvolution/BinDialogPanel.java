package projections.Tools.MessageSizeEvolution;

import projections.gui.JIntTextField;
import projections.gui.JLongTextField;
import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.U;
import projections.gui.Util;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Color;
import java.awt.BorderLayout;

import java.text.DecimalFormat;

class BinDialogPanel
        extends RangeDialogExtensionPanel {

    private JLabel timeBinRangeLabel;
    private JIntTextField timeNumBinsField;
    private long timeBinSize;
    private long startTime;

    private JLabel msgBinRangeLabel;
    private JIntTextField msgNumBinsField;
    private JLongTextField msgBinSizeField;
    private JLongTextField msgMinBinSizeField;
    private JCheckBox msgLogScale;
    private JCheckBox msgIncludeCreation;

    private boolean prevScale;

    private RangeDialog parent;

    BinDialogPanel() {
        JPanel timeBinPanel;
        JPanel msgBinPanel;

        JLabel timeNumBinsLabel;

        JLabel msgNumBinsLabel;
        JLabel msgBinSizeLabel;
        JLabel msgMinBinSizeLabel;

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Time Panel
        timeBinPanel = new JPanel();
        timeBinPanel.setLayout(gbl);
        timeBinPanel.setBorder(new TitledBorder(new LineBorder(Color.black), "TIME-BASED BINS"));

        timeNumBinsLabel = new JLabel("# of Time Bins:", JLabel.LEFT);
        timeNumBinsField = new JIntTextField(-1, 5);

        timeBinRangeLabel = new JLabel("", JLabel.LEFT);

        Util.gblAdd(timeBinPanel, timeNumBinsLabel, gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(timeBinPanel, timeNumBinsField, gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(timeBinPanel, timeBinRangeLabel, gbc, 0, 1, 4, 1, 1, 1);

        // Messages Panel
        msgBinPanel = new JPanel();
        msgBinPanel.setLayout(gbl);
        msgBinPanel.setBorder(new TitledBorder(new LineBorder(Color.black),
                "MESSAGE-BASED BINS"));

        msgNumBinsLabel = new JLabel("# of Msg Bins:", JLabel.LEFT);
        msgNumBinsField = new JIntTextField(-1, 5);

        msgBinSizeLabel = new JLabel("Bin Size (bytes):", JLabel.LEFT);
        msgBinSizeField = new JLongTextField(-1, 12);

        msgMinBinSizeLabel = new JLabel("Starting Bin Size:", JLabel.LEFT);
        msgMinBinSizeField = new JLongTextField(-1, 12);

        msgBinRangeLabel = new JLabel("", JLabel.LEFT);

        msgLogScale = new JCheckBox("Log Scale");
        msgIncludeCreation = new JCheckBox("Creation Events");

        Util.gblAdd(msgBinPanel, msgNumBinsLabel, gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgNumBinsField, gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgLogScale, gbc, 2, 0, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgIncludeCreation, gbc, 3, 0, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgMinBinSizeLabel, gbc, 0, 1, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgMinBinSizeField, gbc, 1, 1, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgBinSizeLabel, gbc, 2, 1, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgBinSizeField, gbc, 3, 1, 1, 1, 1, 1);
        Util.gblAdd(msgBinPanel, msgBinRangeLabel, gbc, 0, 2, 4, 1, 1, 1);

        this.setLayout(new BorderLayout());
        this.add(timeBinPanel, BorderLayout.CENTER);
        this.add(msgBinPanel, BorderLayout.SOUTH);
    }

    // Accessor methods
    int getTimeNumBins() {
        if (timeNumBinsField.getValue() - 1 >= 0)
            return timeNumBinsField.getValue() - 1;
        else
            return 0;
    }

    public void setTimeNumBins(int numBins) {
        timeNumBinsField.setValue(numBins);
        parent.someInputChanged();
    }

    long getTimeBinSize() {
        return timeBinSize;
    }

    public void setTimeBinSize(long binSize) {
        timeBinSize = binSize;
        parent.someInputChanged();
    }

    long getStartTime() {
        return startTime;
    }

    public void setStartTime(long size) {
        startTime = size;
        parent.someInputChanged();
    }

    // Messages
    int getMsgNumBins() {
        if (msgNumBinsField.getValue() - 1 >= 0)
            return msgNumBinsField.getValue() - 1;
        else
            return 0;
    }

    public void setMsgNumBins(int numBins) {
        msgNumBinsField.setValue(numBins);
        parent.someInputChanged();
    }

    long getMsgBinSize() {
        return msgBinSizeField.getValue();
    }

    public void setMsgBinSize(long binSize) {
        msgBinSizeField.setValue(binSize);
        parent.someInputChanged();
    }

    long getMsgMinBinSize() {
        return msgMinBinSizeField.getValue();
    }

    public void setMsgMinBinSize(long size) {
        msgMinBinSizeField.setValue(size);
        parent.someInputChanged();
    }

    boolean getMsgLogScale() {
        return msgLogScale.isSelected();
    }

    public void setMsgLogScale(boolean logScale) {
        msgLogScale.setSelected(logScale);
        parent.someInputChanged();
    }

    boolean getMsgCreationEvent() {
        return msgIncludeCreation.isSelected();
    }

    public void setMsgIncludeCreation(boolean includeCreation) {
        msgIncludeCreation.setSelected(includeCreation);
        parent.someInputChanged();
    }

    public boolean isInputValid() {
        return true;
    }

    public void setInitialFields() {
        // default values for time 1ms to 100ms
        timeNumBinsField.setText("100");
        startTime = parent.getStartTime();
        timeBinSize = parent.getSelectedTotalTime() / 100;

        // default values for messages 100 bytes to 2k
        msgNumBinsField.setText("10");
        msgBinSizeField.setText("1000");
        msgBinSizeField.setEnabled(false);
        msgMinBinSizeField.setText("128");
        msgLogScale.setSelected(true);
        msgIncludeCreation.setSelected(false);

        prevScale = true;
        updateFields();
    }

    public void setParentDialogBox(RangeDialog parent) {
        this.parent = parent;
        timeNumBinsField.addActionListener(parent);
        timeNumBinsField.addKeyListener(parent);
        timeNumBinsField.addFocusListener(parent);
        msgNumBinsField.addActionListener(parent);
        msgNumBinsField.addKeyListener(parent);
        msgNumBinsField.addFocusListener(parent);
        msgBinSizeField.addActionListener(parent);
        msgBinSizeField.addKeyListener(parent);
        msgBinSizeField.addFocusListener(parent);
        msgMinBinSizeField.addActionListener(parent);
        msgMinBinSizeField.addKeyListener(parent);
        msgMinBinSizeField.addFocusListener(parent);
        msgLogScale.addActionListener(parent);
    }

    public void updateFields() {
        startTime = parent.getStartTime();
        timeBinSize = parent.getSelectedTotalTime() / getTimeNumBins();

        timeBinRangeLabel.setText("Bin size ranges from : " +
                U.humanReadableString(startTime) +
                " to " +
                U.humanReadableString(startTime +
                        parent.getSelectedTotalTime()));

        boolean currScale = getMsgLogScale();
        DecimalFormat _format = new DecimalFormat();
        if (currScale) {
            if (!prevScale) {
                if (msgMinBinSizeField.getValue() < 128)
                    msgMinBinSizeField.setText("128");
                msgBinSizeField.setEnabled(false);
                prevScale = true;
            }

            msgBinRangeLabel.setText("Bin size ranges from : " +
                    _format.format(msgMinBinSizeField.getValue()) +
                    " bytes to " +
                    _format.format(msgMinBinSizeField.getValue() *
                            Math.pow(2, msgNumBinsField.getValue())) + " bytes.");
        } else {
            if (prevScale) {
                msgBinSizeField.setEnabled(true);
                prevScale = false;
            }
            msgBinRangeLabel.setText("Bin size ranges from : " +
                    _format.format(msgMinBinSizeField.getValue()) +
                    " bytes to " +
                    _format.format(msgMinBinSizeField.getValue() +
                            getMsgNumBins() * msgBinSizeField.getValue()) + " bytes.");
        }
    }
}
