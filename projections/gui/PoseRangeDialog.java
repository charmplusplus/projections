package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;

/**
 *  PoseRangeDialog
 *  by Chee Wai Lee
 *  10/21/2005
 * 
 *  This is a highly specialized dialog box for use with POSE dop (degrees
 *  of parallism) data. This involves the tracking of Virtual Time Ranges
 *  in addition to real time.
 *
 */
public class PoseRangeDialog extends JDialog
    implements ActionListener, KeyListener, FocusListener
{
    ProjectionsWindow parentWindow;

    // Constant variables
    private static final int DIALOG_OK = 0;
    private static final int DIALOG_CANCELLED = 1;

    // GUI components
    protected JPanel processorsPanel;
    protected JTabbedPane timePanel;
    protected JPanel realTimePanel;
    protected JPanel virtTimePanel;
    protected JPanel buttonPanel;
    protected JPanel mainPanel;

    // Control buttons
    protected JButton bOK, bUpdate, bCancel;

    // Processor Fields
    protected JSelectField processorsField;

    // Real Time component
    protected JTimeTextField realIntervalSizeField;
    protected JTimeTextField realStartTimeField;
    protected JTimeTextField realEndTimeField;

    // Virtual Time Component
    protected JLongTextField virtIntervalSizeField;
    protected JLongTextField virtStartTimeField;
    protected JLongTextField virtEndTimeField;

    // private GUI objects
    private JLabel 
	realStartFieldLabel, realEndFieldLabel, realIntervalSizeFieldLabel,
	virtStartFieldLabel, virtEndFieldLabel, virtIntervalSizeFieldLabel,
	processorsFieldLabel;
    private JLabel 
	totalSelectedRealTimeLabel, validRealTimeLabel, 
	totalNumRealIntervalsLabel, 
	realStartIntervalLabel, realEndIntervalLabel,
	totalSelectedVirtTimeLabel, validVirtTimeLabel, 
	totalNumVirtIntervalsLabel, 
	virtStartIntervalLabel, virtEndIntervalLabel,
	validProcessorsLabel;

    // Dialog attributes
    protected OrderedIntList validProcessors;

    protected long realIntervalSize;
    protected long realStartTime;
    protected long realEndTime;

    protected long virtIntervalSize;
    protected long virtStartTime;
    protected long virtEndTime;

    // Internal Variables maintained
    private String validProcessorsString;

    private long numRealIntervals;
    private long realStartInterval;
    private long realEndInterval;

    private long numVirtIntervals;
    private long virtStartInterval;
    private long virtEndInterval;

    private static final int NUM_TYPES = 2;
    private static final int REAL_TIME = 0;
    private static final int VIRT_TIME = 1;
    private DecimalFormat _format;

    // flags
    private boolean layoutComplete = false;
    private int dialogState;

    public PoseRangeDialog(ProjectionsWindow parentWindow,
			   String titleString) {
	super((JFrame)parentWindow, titleString, true);
	this.parentWindow = parentWindow;

	// Get default values
	validProcessors = Analysis.getValidProcessorList(Analysis.DOP);
	validProcessorsString = validProcessors.listToString();

	realIntervalSize = 1000; // default to 1ms.
	realStartTime = 0;
	realEndTime = Analysis.getPoseTotalTime(); // default full range.
	realStartInterval = 0;
	realEndInterval = realEndTime/realIntervalSize;
	numRealIntervals = realEndInterval - realStartInterval + 1;

	virtStartTime = 0;
	virtEndTime = Analysis.getPoseTotalVirtualTime(); // full range.
	// default to (very) roughly 100 interval's worth
	virtIntervalSize = virtEndTime/100 + 1;
	virtStartInterval = 0;
	virtEndInterval = virtEndTime/virtIntervalSize;
	numVirtIntervals = virtEndInterval - virtStartInterval + 1;

	_format = new DecimalFormat();
    }
    
    public void actionPerformed(ActionEvent evt)
    {
        if (evt.getSource() instanceof JButton) {
            JButton b = (JButton) evt.getSource();
	    
            if(b == bOK) {
                // point user to an inconsistent field.
                JTextField someField = checkConsistent();
                if (someField != null) {
                    someField.selectAll();
                    someField.requestFocus();
                    return;
                } else {
		    dialogState = DIALOG_OK;     // set local variable state
                }
                setVisible(false);
            } else if (b == bUpdate) {
                // update all text fields.
                updateData(processorsField);
                updateData(realStartTimeField);
                updateData(realEndTimeField);
                updateData(virtStartTimeField);
                updateData(virtEndTimeField);
                return; // CHECK this return
            }else if (b == bCancel){
                dialogState = DIALOG_CANCELLED;
                setVisible(false);
            }
        } else if (evt.getSource() instanceof JTextField) {
	    // perform an update in response to an "Enter" action event
	    // since the Enter key is typically hit after some keyboard
	    // input.
            updateData((JTextField)evt.getSource());
            bOK.setEnabled(true);
        }
    }

    public void focusGained(FocusEvent evt) {
        // do nothing
    }

    public void focusLost(FocusEvent evt) {
        // when keyboard focus is lost from a text field, it is assumed
        // that the user has confirmed the data. Hence, perform an update and
        // enable the OK button.
        if (evt.getComponent() instanceof JTextField) {
            updateData((JTextField)evt.getComponent());
            bOK.setEnabled(true);
        }
    }

    public void keyPressed(KeyEvent evt) {
        // do nothing
    }

    public void keyReleased(KeyEvent evt) {
        // do nothing
    }

    public void keyTyped(KeyEvent evt) {
        int keycode = evt.getKeyCode();
        if (evt.getComponent() instanceof JTextField) {
            JTextField field = (JTextField)evt.getComponent();

            switch (keycode) {
            case KeyEvent.VK_ENTER:
                // hitting enter is typical of input confirmation. Update the
                // data and enable the OK button.
                updateData(field);
                bOK.setEnabled(true);
                break;
            case KeyEvent.VK_TAB:
                // leaving keyboard focus for that field. Update the data and
                // enable the OK button.
                updateData(field);
                bOK.setEnabled(true);
                break;
            default:
                // any other key assumed to be input. Hence disable OK button.
                // enable the Update button to alert user to the change.
                bOK.setEnabled(false);
                break;
            }
        }
    }

    public void displayDialog() {
        // only layout the dialog once in its lifetime.
        if (!layoutComplete) {
            addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent e)
                    {
                        dialogState = DIALOG_CANCELLED;
                        setVisible(false);
                    }
                });
            mainPanel = createMainLayout();
            buttonPanel = createButtonLayout();
	    
            this.getContentPane().setLayout(new BorderLayout());
            this.getContentPane().add(mainPanel, BorderLayout.NORTH);
            this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            layoutComplete = true;
            pack();
            setResizable(false);
        } else {
            setParameters();
        }
        setVisible(true);
    }

    JPanel createMainLayout() {
	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(2,2,2,2);

	// Processor Panel
        // Default processor range layout
        processorsPanel = new JPanel();
        processorsPanel.setLayout(gbl);
        validProcessorsLabel = 
	    new JLabel("Valid Processors = " + validProcessorsString,
		       JLabel.LEFT);
        processorsFieldLabel = new JLabel("Processors :", JLabel.LEFT);
        processorsField = 
	    new JSelectField(validProcessorsString, 12);
        // set listeners
        processorsField.addActionListener(this);
        // layout
        Util.gblAdd(processorsPanel, validProcessorsLabel,
                    gbc, 0,0, 2,1, 1,1);
        Util.gblAdd(processorsPanel, processorsFieldLabel,
                    gbc, 0,1, 1,1, 1,1);
        Util.gblAdd(processorsPanel, processorsField,
                    gbc, 1,1, 1,1, 1,1);

	// Time Tabbed Panel (2 components, real and virtual time)
	timePanel = new JTabbedPane();
	timePanel.setBorder(new TitledBorder(new LineBorder(Color.black),
					     "POSE TIME PARAMETER"));

	// Real Time Panel
	realTimePanel = new JPanel();
	realTimePanel.setLayout(gbl);
	realIntervalSizeFieldLabel = new JLabel("Size of Real Time Interval:",
						JLabel.LEFT);
	realIntervalSizeField = new JTimeTextField(realIntervalSize, 12);
	realIntervalSizeField.addActionListener(this);
	realIntervalSizeField.addKeyListener(this);
	realIntervalSizeField.addFocusListener(this);

	realStartFieldLabel = new JLabel("Real Time Start:", JLabel.LEFT);
	realStartTimeField = new JTimeTextField(realStartTime, 12);
	realStartTimeField.addActionListener(this);
	realStartTimeField.addKeyListener(this);
	realStartTimeField.addFocusListener(this);

	realEndFieldLabel = new JLabel("Real Time End:", JLabel.LEFT);
	realEndTimeField = new JTimeTextField(realEndTime, 12);
	realEndTimeField.addActionListener(this);
	realEndTimeField.addKeyListener(this);
	realEndTimeField.addFocusListener(this);

	validRealTimeLabel =
	    new JLabel("Valid Real Time Range: " + U.t(0) + " to " +
		       U.t(Analysis.getPoseTotalTime()));
	totalSelectedRealTimeLabel =
	    new JLabel("Selected Total Time: " + 
		       U.t(realEndTime - realStartTime));
	realStartIntervalLabel =
	    new JLabel("Start Interval: " + realStartInterval);
	realEndIntervalLabel =
	    new JLabel("End Interval: " + realEndInterval);
	totalNumRealIntervalsLabel =
	    new JLabel("Num Intervals: " + 
		       (realEndInterval - realStartInterval + 1));

        Util.gblAdd(realTimePanel, realIntervalSizeFieldLabel,
                    gbc, 0,0, 1,1, 1,1);
        Util.gblAdd(realTimePanel, realIntervalSizeField,
                    gbc, 1,0, 1,1, 1,1);
        Util.gblAdd(realTimePanel, realStartFieldLabel,
                    gbc, 0,1, 1,1, 1,1);
        Util.gblAdd(realTimePanel, realStartTimeField,
                    gbc, 1,1, 1,1, 1,1);
        Util.gblAdd(realTimePanel, realEndFieldLabel,
                    gbc, 2,1, 1,1, 1,1);
        Util.gblAdd(realTimePanel, realEndTimeField,
                    gbc, 3,1, 1,1, 1,1);
        Util.gblAdd(realTimePanel, validRealTimeLabel,
                    gbc, 0,2, 4,1, 1,1);
        Util.gblAdd(realTimePanel, totalSelectedRealTimeLabel,
                    gbc, 0,3, 4,1, 1,1);
        Util.gblAdd(realTimePanel, realStartIntervalLabel,
                    gbc, 0,4, 2,1, 1,1);
        Util.gblAdd(realTimePanel, realEndIntervalLabel,
                    gbc, 1,4, 2,1, 1,1);
        Util.gblAdd(realTimePanel, totalNumRealIntervalsLabel,
                    gbc, 0,5, 4,1, 1,1);
	
	// Virtual Time Panel
	virtTimePanel = new JPanel();
	virtTimePanel.setLayout(gbl);
	virtIntervalSizeFieldLabel = 
	    new JLabel("Units in Virtual Time Interval:", JLabel.LEFT);
	virtIntervalSizeField = new JLongTextField(virtIntervalSize, 12);
	virtIntervalSizeField.addActionListener(this);
	virtIntervalSizeField.addKeyListener(this);
	virtIntervalSizeField.addFocusListener(this);

	virtStartFieldLabel = new JLabel("Virtual Time Start:", JLabel.LEFT);
	virtStartTimeField = new JLongTextField(virtStartTime, 12);
	virtStartTimeField.addActionListener(this);
	virtStartTimeField.addKeyListener(this);
	virtStartTimeField.addFocusListener(this);

	virtEndFieldLabel = new JLabel("Virtual Time End:", JLabel.LEFT);
	virtEndTimeField = new JLongTextField(virtEndTime, 12);
	virtEndTimeField.addActionListener(this);
	virtEndTimeField.addKeyListener(this);
	virtEndTimeField.addFocusListener(this);

	validVirtTimeLabel =
	    new JLabel("Valid Virtual Time Range: " + 0 + " to " +
		       (Analysis.getPoseTotalVirtualTime()));
	totalSelectedVirtTimeLabel =
	    new JLabel("Units of Selected Total Time: " + 
		       (virtEndTime - virtStartTime));
	virtStartIntervalLabel =
	    new JLabel("Start Interval: " + virtStartInterval);
	virtEndIntervalLabel =
	    new JLabel("End Interval: " + virtEndInterval);
	totalNumVirtIntervalsLabel =
	    new JLabel("Num Intervals: " + 
		       (virtEndInterval - virtStartInterval + 1));

        Util.gblAdd(virtTimePanel, virtIntervalSizeFieldLabel,
                    gbc, 0,0, 1,1, 1,1);
        Util.gblAdd(virtTimePanel, virtIntervalSizeField,
                    gbc, 1,0, 1,1, 1,1);
        Util.gblAdd(virtTimePanel, virtStartFieldLabel,
                    gbc, 0,1, 1,1, 1,1);
        Util.gblAdd(virtTimePanel, virtStartTimeField,
                    gbc, 1,1, 1,1, 1,1);
        Util.gblAdd(virtTimePanel, virtEndFieldLabel,
                    gbc, 2,1, 1,1, 1,1);
        Util.gblAdd(virtTimePanel, virtEndTimeField,
                    gbc, 3,1, 1,1, 1,1);
        Util.gblAdd(virtTimePanel, validVirtTimeLabel,
                    gbc, 0,2, 4,1, 1,1);
        Util.gblAdd(virtTimePanel, totalSelectedVirtTimeLabel,
                    gbc, 0,3, 4,1, 1,1);
        Util.gblAdd(virtTimePanel, virtStartIntervalLabel,
                    gbc, 0,4, 2,1, 1,1);
        Util.gblAdd(virtTimePanel, virtEndIntervalLabel,
                    gbc, 1,4, 2,1, 1,1);
        Util.gblAdd(virtTimePanel, totalNumVirtIntervalsLabel,
                    gbc, 0,5, 4,1, 1,1);

	timePanel.addTab("Real Time", null, realTimePanel, 
			 "Real Time Parameters");
	timePanel.addTab("Virtual", null, virtTimePanel, 
			 "Virtual Time Parameters");

	JPanel inputPanel = new JPanel();
	inputPanel.setLayout(gbl);

	Util.gblAdd(inputPanel, processorsPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, timePanel,        gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    JPanel createButtonLayout() {
        JPanel buttonPanel = new JPanel();

        bOK     = new JButton("OK");
        bUpdate = new JButton("Update");
        bCancel = new JButton("Cancel");

        buttonPanel.add(bOK);
        buttonPanel.add(bUpdate);
        buttonPanel.add(bCancel);

        bOK.addActionListener    (this);
        bUpdate.addActionListener(this);
        bCancel.addActionListener(this);

        return buttonPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JTimeTextField) {
	    if (field == realStartTimeField) {
		realStartTime = realStartTimeField.getValue();
		realStartInterval = realStartTime/realIntervalSize;
	    } else if (field == realEndTimeField) {
		realEndTime = realEndTimeField.getValue();
		realEndInterval = realEndTime/realIntervalSize;
	    } else if (field == realIntervalSizeField) {
		realIntervalSize = realIntervalSizeField.getValue();
	    }
	} else if (field instanceof JLongTextField) {
	    if (field == virtStartTimeField) {
		virtStartTime = virtStartTimeField.getValue();
	    } else if (field == virtEndTimeField) {
		virtEndTime = virtEndTimeField.getValue();
	    } else if (field == virtIntervalSizeField) {
		virtIntervalSize = virtIntervalSizeField.getValue();
	    }
	}
	updateDerived(field);
    }

    JTextField checkConsistent() {
	// **FIXME**
	return null;
    }

    public boolean isModified() {
	// **FIXME**
	return true;
    }

    void updateDerived(JTextField field) {
	if (field instanceof JTimeTextField) {
	    if (field == realStartTimeField) {
		realStartInterval = realStartTime/realIntervalSize;
		numRealIntervals = realEndInterval - realStartInterval + 1;
	    } else if (field == realEndTimeField) {
		realEndInterval = realEndTime/realIntervalSize;
		numRealIntervals = realEndInterval - realStartInterval + 1;
	    } else if (field == realIntervalSizeField) {
		realStartInterval = realStartTime/realIntervalSize;
		realEndInterval = realEndTime/realIntervalSize;
		numRealIntervals = realEndInterval - realStartInterval + 1;
	    }
	} else if (field instanceof JLongTextField) {
	    if (field == virtStartTimeField) {
		virtStartInterval = virtStartTime/virtIntervalSize;
		numVirtIntervals = virtEndInterval - virtStartInterval + 1;
	    } else if (field == virtEndTimeField) {
		virtEndInterval = virtEndTime/virtIntervalSize;
		numVirtIntervals = virtEndInterval - virtStartInterval + 1;
	    } else if (field == virtIntervalSizeField) {
		virtStartInterval = virtStartTime/virtIntervalSize;
		virtEndInterval = virtEndTime/virtIntervalSize;
		numVirtIntervals = virtEndInterval - virtStartInterval + 1;
	    }
	} else if (field == null) {
	    // update everything
	    realStartInterval = realStartTime/realIntervalSize;
	    realEndInterval = realEndTime/realIntervalSize;
	    numRealIntervals = realEndInterval - realStartInterval + 1;
	    virtStartInterval = virtStartTime/virtIntervalSize;
	    virtEndInterval = virtEndTime/virtIntervalSize;
	    numVirtIntervals = virtEndInterval - virtStartInterval + 1;
	}
	// modify the graphics to reflect the change
	totalSelectedRealTimeLabel.setText("Selected Total Time: " + 
					   U.t(realEndTime - realStartTime));
	realStartIntervalLabel.setText("Start Interval: " + 
				       realStartInterval);
	realEndIntervalLabel.setText("End Interval: " + realEndInterval);
	totalNumRealIntervalsLabel.setText("Num Intervals: " + 
					   (realEndInterval - 
					    realStartInterval + 1));
	totalSelectedVirtTimeLabel.setText("Units of Selected Total Time: " + 
					   (virtEndTime - virtStartTime));
	virtStartIntervalLabel.setText("Start Interval: " + 
				       virtStartInterval);
	virtEndIntervalLabel.setText("End Interval: " + virtEndInterval);
	totalNumVirtIntervalsLabel.setText("Num Intervals: " + 
					   (virtEndInterval - 
					    virtStartInterval + 1));
    }

    void setParameters() {
	realIntervalSize = realIntervalSizeField.getValue();
	realStartTime = realStartTimeField.getValue();
	realEndTime = realEndTimeField.getValue();
	virtIntervalSize = virtIntervalSizeField.getValue();
	virtStartTime = virtStartTimeField.getValue();
	virtEndTime = virtEndTimeField.getValue();
	updateDerived(null);
    }

    public boolean isCancelled() {
        return (dialogState == DIALOG_CANCELLED);
    }

    // Accessor methods
    // Processors
    public OrderedIntList getValidProcessors() {
	return validProcessors;
    }

    public void setValidProcessors(OrderedIntList validProcessors) {
	this.validProcessors = validProcessors;
	validProcessorsString = this.validProcessors.listToString();
    }

    public long getRealIntervalSize() {
	return realIntervalSize;
    }

    public void setRealIntervalSize(long realIntervalSize) {
	this.realIntervalSize = realIntervalSize;
    }

    public long getRealStartTime() {
	return realStartTime;
    }
    
    public void setRealStartTime(long realStartTime) {
	this.realStartTime = realStartTime;
    }

    public long getRealEndTime() {
	return realEndTime;
    }

    public void setRealEndTime(long realEndTime) {
	this.realEndTime = realEndTime;
    }

    public long getVirtIntervalSize() {
	return virtIntervalSize;
    }

    public void setVirtIntervalSize(long virtIntervalSize) {
	this.virtIntervalSize = virtIntervalSize;
    }

    public long getVirtStartTime() {
	return virtStartTime;
    }
    
    public void setVirtStartTime(long virtStartTime) {
	this.virtStartTime = virtStartTime;
    }

    public long getVirtEndTime() {
	return virtEndTime;
    }

    public void setVirtEndTime(long virtEndTime) {
	this.virtEndTime = virtEndTime;
    }
}
