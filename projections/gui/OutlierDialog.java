package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;

import projections.guiUtils.*;

public class OutlierDialog extends RangeDialog
{
    // GUI components
    protected JPanel outlierPanel;
    protected JComboBox attributeList;
    protected JComboBox activityList;
    protected JIntTextField thresholdField;
    private JTextArea errorText;

    // Dialog attributes
    protected int currentAttribute;
    protected int currentActivity;
    protected int threshold;

    private int lastAttribute = -1;
    private int lastActivity = -1;

    // This dialog is specially tied to OutlierAnalysisWindow only
    public OutlierDialog(OutlierAnalysisWindow mainWindow,
			 String titleString) {
	super(mainWindow, titleString);

	// execution time
	currentAttribute = 0;
	// projections-based data
	currentActivity = ActivityManager.PROJECTIONS;
	// initialize default threshold to display the top 10% deviants
	// for # processors 256 or less. The top 20 otherwise.
	if (Analysis.getNumProcessors() <= 256) {
	    threshold = (int)Math.ceil(0.1*Analysis.getNumProcessors());
	} else {
	    threshold = 20;
	}
    }
    
    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
	    if (b == bOK) {
		// This part is extremely unique to this dialog
		// and really should be fixed as part of an overall
		// dialog strategy
		if ((activityList.getSelectedIndex() >= 0) &&
		    (attributeList.getSelectedIndex() >= 0)) {
		    // filter out the options that may not work with 
		    // each other
		    if ( // **FIXME** Find a way to get around having to 
			 // hardcode
			// pose dop activities have no meaning here
			(activityList.getSelectedIndex() == 
			 ActivityManager.POSE_DOP) ||
			// no function support for now either ... *sigh*
			(activityList.getSelectedIndex() ==
			 ActivityManager.FUNCTIONS) ||
			// no communication properties associated with user 
			// events
			((activityList.getSelectedIndex() == 
			  ActivityManager.USER_EVENTS) &&
			 (attributeList.getSelectedIndex() >= 1))
			) {
			errorText.setText("ERROR: Attribute " +
					  ((OutlierAnalysisWindow)parentWindow).attributes[0][attributeList.getSelectedIndex()] +
					  " is incompatible with Activity " +
					  ActivityManager.NAMES[activityList.getSelectedIndex()]);
			return;
		    }
		} else {
		    errorText.setText("ERROR: no valid Attribute or " +
				      "Activity selection!");
		    return;
		}
		// point user to an inconsistent field.
		JTextField someField = checkConsistent();
		if (someField != null) {
		    someField.selectAll();
		    someField.requestFocus();
		    return;
		}
	    } else if (b == bUpdate) {
		// update all text fields
		updateData(thresholdField);
	    }
	}
	// let superclass handle its own action routines.
	super.actionPerformed(evt);
    }

    public void itemStateChanged(ItemEvent evt) {
        if (evt.getSource() instanceof JComboBox) {
	    JComboBox item = (JComboBox)evt.getSource();
	    if (item == attributeList) {
		currentAttribute = item.getSelectedIndex();
	    } else if (item == activityList) {
		currentActivity = item.getSelectedIndex();
	    }
        }
	super.itemStateChanged(evt);
    }

    JPanel createMainLayout() {
	JPanel inputPanel = new JPanel();
	JPanel baseMainPanel = super.createMainLayout();

	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(2,2,2,2);

        outlierPanel = new JPanel();
	outlierPanel.setLayout(gbl);

	attributeList = 
	    new JComboBox(((OutlierAnalysisWindow)parentWindow).attributes[0]);
	attributeList.setSelectedIndex(currentAttribute);
	JLabel attributeLabel = new JLabel("Attribute: ", JLabel.RIGHT);
	attributeList.addItemListener(this);

	activityList = new JComboBox(ActivityManager.NAMES);
	activityList.setSelectedIndex(currentActivity);
	JLabel activityLabel = new JLabel("Activity: ", JLabel.RIGHT);
	activityList.addItemListener(this);

	JLabel thresholdLabel = new JLabel("Outlier Threshold: ", 
					   JLabel.RIGHT);
	thresholdField = new JIntTextField(threshold, 8);
	JLabel thresholdPost = new JLabel("Processors", JLabel.LEFT);
	thresholdField.addActionListener(this);	

	errorText = new JTextArea();
	errorText.setRows(3);
	errorText.setEnabled(false);
	errorText.setLineWrap(true);
	errorText.setForeground(Color.red);
	JScrollPane scrollText = 
	    new JScrollPane(errorText,
			    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        Util.gblAdd(outlierPanel, attributeLabel, gbc, 0,0, 1,1, 0,1);
        Util.gblAdd(outlierPanel, attributeList,  gbc, 1,0, 2,1, 0,1);
        Util.gblAdd(outlierPanel, activityLabel,  gbc, 0,1, 1,1, 0,1);
        Util.gblAdd(outlierPanel, activityList,   gbc, 1,1, 2,1, 0,1);
        Util.gblAdd(outlierPanel, thresholdLabel, gbc, 0,2, 1,1, 0,1);
        Util.gblAdd(outlierPanel, thresholdField, gbc, 1,2, 1,1, 0,1);
        Util.gblAdd(outlierPanel, thresholdPost,  gbc, 2,2, 1,1, 0,1);
	Util.gblAdd(outlierPanel, scrollText,     gbc, 0,3, 3,3, 0,1);

	inputPanel.setLayout(gbl);

	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, outlierPanel,   gbc, 0,1, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JIntTextField) {
	    if (field == thresholdField) {
		threshold = thresholdField.getValue();
	    }
	}
	super.updateData(field);
    }

    JTextField checkConsistent() {
	if ((thresholdField.getValue() < 0) ||
	    (thresholdField.getValue() >
	     processorsField.getValue(Analysis.getNumProcessors()).size())) {
	    return thresholdField;
	}
	return super.checkConsistent();
    }

    public boolean isModified() {
	return ((threshold != thresholdField.getValue()) ||
		(currentActivity != lastActivity) ||
		(currentAttribute != lastAttribute) ||
		super.isModified());
    }

    void setParameters() {
	lastActivity = activityList.getSelectedIndex();
	lastAttribute = attributeList.getSelectedIndex();
	super.setParameters();
    }

    void updateFields() {
	super.updateFields();
    }

    void updateDerived() {
	// this class has no derived information.
	// this method is included for completeness.
    }
}
