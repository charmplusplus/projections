package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class IntervalRangeDialog extends RangeDialog
    implements ActionListener, KeyListener, FocusListener
{
    // Additional GUI objects
    Panel sizePanel, thresholdPanel;

    TimeTextField sizeField;
    TimeTextField thresholdField;

    // Additional Data definitions
    long intervalSize;
    long threshold;

    public IntervalRangeDialog(ProjectionsWindow parentWindow,
			       String titleString)
    {
	super(parentWindow, titleString);
	intervalSize = 1000;  // default to 1ms.
	threshold = 0;        // default to no threshold.
    }

    public void actionPerformed(ActionEvent evt) 
    {
	if (evt.getSource() instanceof Button) {
	    Button b = (Button) evt.getSource();
	    if (b == bOK) {
		// point user to an inconsistent field.
		TextComponent someField = checkConsistent();
		if (someField != null) {
		    someField.selectAll();
		    someField.requestFocus();
		    return;
		} else {
		    setAllData();
		}
	    } else if (b == bUpdate) {
		// update all subclass text fields.
		updateData(sizeField);
		updateData(thresholdField);
	    }
	} else if (evt.getSource() instanceof TextComponent) {
	    // do nothing. Everything supported in superclass.
	}
	// let superclass handle its own action routines.
	super.actionPerformed(evt);
    }

    Panel createMainLayout() {
	Panel inputPanel = new Panel();
	Panel baseMainPanel = super.createMainLayout();

	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(2,2,2,2);

	// create interval size panel
	sizePanel = new Panel();
	sizePanel.setLayout(gbl);
	Label sizeLabel = new Label("Interval Size :", Label.LEFT);
	sizeField = new TimeTextField(intervalSize, 12);
	sizeField.addActionListener(this);
	Util.gblAdd(sizePanel, sizeLabel, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(sizePanel, sizeField, gbc, 1,0, 1,1, 1,1);

	// create threshold panel
	thresholdPanel = new Panel();
	thresholdPanel.setLayout(gbl);
	Label thresholdLabel = new Label("EP threshold :", Label.LEFT);
	thresholdField = new TimeTextField(threshold, 12);
	thresholdField.addActionListener(this);
	Util.gblAdd(thresholdPanel, thresholdLabel, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(thresholdPanel, thresholdField, gbc, 1,0, 1,1, 1,1);

	inputPanel.setLayout(gbl);
	Util.gblAdd(inputPanel, baseMainPanel,  gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(inputPanel, sizePanel,      gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(inputPanel, thresholdPanel, gbc, 0,2, 1,1, 1,1);

	return inputPanel;
    }

    void updateData(TextComponent field) {
	if (field instanceof TimeTextField) {
	    if (field == sizeField) {
		intervalSize = sizeField.getValue();
	    } else if (field == thresholdField) {
		threshold = thresholdField.getValue();
	    }
	}
	super.updateData(field);
    }

    TextComponent checkConsistent() {
	// interval size should not be less than or equal to zero us.
	// it should also not be larger than the selected time range.
	if (intervalSize <= 0 || intervalSize > totalTime) {
	    return sizeField;
	}
	// the entry point execution time threshold cannot be less than zero.
	if (threshold < 0) {
	    return thresholdField;
	}
	return super.checkConsistent();
    }

    void setAllData() {
	// SINDHURA, FILL THIS IN ACCORDING TO WHAT YOUR WINDOW REQUIRES.
	super.setAllData();
    }
}
