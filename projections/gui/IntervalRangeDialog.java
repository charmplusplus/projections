package projections.gui;

//import java.awt.*;
import javax.swing.*;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;

public class IntervalRangeDialog extends RangeDialog
    implements ActionListener, KeyListener, FocusListener
{
    // Additional GUI objects
    JPanel sizePanel, thresholdPanel;

    JTimeTextField sizeField;
    JTimeTextField thresholdField;

    // Additional Data definitions
    long intervalSize;
    long threshold;
    boolean existsSizeField; // for the case where there is no interval size field
    boolean existsThresholdField; // for the case where there is no interval size field
    
    public IntervalRangeDialog(ProjectionsWindow parentWindow,
			       String titleString)
    {
	super(parentWindow, titleString);
	intervalSize = 1000;  // default to 1ms.
	threshold = 0;        // default to no threshold.
	existsSizeField = true;
	existsThresholdField = true;
    }
    
    public IntervalRangeDialog(ProjectionsWindow parentWindow,
			       String titleString,boolean existsSizeField, boolean existsThresholdField)
    {
	super(parentWindow, titleString);
	intervalSize = 1000;  // default to 1ms.
	threshold = 0;        // default to no threshold.
	this.existsSizeField = existsSizeField;
	this.existsThresholdField = existsThresholdField;
    }
    

    public void actionPerformed(ActionEvent evt) 
    {
	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
	    if (b == bOK) {
		// point user to an inconsistent field.
		JTextField someField = checkConsistent();
		if (someField != null) {
		    someField.selectAll();
		    someField.requestFocus();
		    return;
		} else { // parent dialog (RangeDialog) will set the right dialogState
		    setAllData();
		}
	    } else if (b == bUpdate) {
		// update all subclass text fields.
		if(existsSizeField)
			updateData(sizeField);
		if(existsThresholdField)	
			updateData(thresholdField);
	    }
	} else if (evt.getSource() instanceof JTextField) {
	    // do nothing. Everything supported in superclass.
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

	// create interval size panel
	if(existsSizeField){

		sizePanel = new JPanel();
		sizePanel.setLayout(gbl);
		JLabel sizeLabel = new JLabel("Interval Size :", JLabel.LEFT);
		sizeField = new JTimeTextField(intervalSize, 12);
		sizeField.addActionListener(this);
		sizeField.addKeyListener(this);
		sizeField.addFocusListener(this);
		Util.gblAdd(sizePanel, sizeLabel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(sizePanel, sizeField, gbc, 1,0, 1,1, 1,1);
	}
	// create threshold panel
	if(existsThresholdField){
		thresholdPanel = new JPanel();
		thresholdPanel.setLayout(gbl);
		JLabel thresholdLabel = new JLabel("EP threshold :", JLabel.LEFT);
		thresholdField = new JTimeTextField(threshold, 12);
		thresholdField.addActionListener(this);
		thresholdField.addKeyListener(this);
		thresholdField.addFocusListener(this);
		Util.gblAdd(thresholdPanel, thresholdLabel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(thresholdPanel, thresholdField, gbc, 1,0, 1,1, 1,1);
	}
	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	if(existsSizeField)
		Util.gblAdd(inputPanel, sizePanel,      gbc, 0,1, 1,1, 1,1);
	if(existsThresholdField)
		Util.gblAdd(inputPanel, thresholdPanel, gbc, 0,2, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(JTextField field) {
	if (field instanceof JTimeTextField) {
	    if (existsSizeField && field == sizeField) {
		intervalSize = sizeField.getValue();
	    } else if (existsThresholdField && field == thresholdField) {
		threshold = thresholdField.getValue();
	    }
	}
	super.updateData(field);
    }

    JTextField checkConsistent() {
	// interval size should not be less than or equal to zero us.
	// it should also not be larger than the selected time range.
	if(existsSizeField){
		if (intervalSize <= 0 || intervalSize > totalTime) {
		    return sizeField;
		}
	}	
	// the entry point execution time threshold cannot be less than zero.
	if(existsThresholdField){
		if (threshold < 0) {
		    return thresholdField;
		}
	}	
	return super.checkConsistent();
    }

    // accessor methods for getting data fields
    long getIntervalSize(){
	return intervalSize;
    }
    long getThresholdTime(){
	return threshold;
    } 

    void setAllData() {
	// set interval size & threshold in the IntervalWindow
	// then call the superclass function to set other data like processors and range
	// is there a better way to do this??
//	IntervalWindow parentWindow_ = (IntervalWindow)parentWindow;
//	parentWindow_.setIntervalSize(intervalSize);
//	parentWindow_.setThresholdTime(threshold);
	super.setAllData();
    }
}
