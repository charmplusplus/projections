package projections.gui;

import projections.misc.*;
import projections.analysis.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MultiRunControlPanel extends JPanel
{
    private ButtonGroup displayModes;
    private JPanel modePanel;
    private JRadioButton textMode;
    private JRadioButton graphMode;

    private ButtonGroup dataTypeModes;
    private JPanel dataTypePanel;
    private JRadioButton dataTypeButtons[];

    private JButton displayTable;
    private JButton done;

    private Label cmdLineLabel;
    private TextField cmdLine;

    private MultiRunWindow mainWindow;

    public MultiRunControlPanel(MultiRunWindow mainWindow, int defaultDataType)
    {
	this.mainWindow = mainWindow;

	setBackground(Color.lightGray);

	// data display modes
	displayModes = new ButtonGroup();
	modePanel = new JPanel();

	textMode = new JRadioButton("Table", false);
	textMode.addItemListener(mainWindow);
	graphMode = new JRadioButton("Graph", true);
	graphMode.addItemListener(mainWindow);

	displayModes.add(textMode);
	displayModes.add(graphMode);

	modePanel.add(textMode);
	modePanel.add(graphMode);
	modePanel.setBorder(BorderFactory.createLineBorder(Color.black));

	// data type selectors
	dataTypeModes = new ButtonGroup();
	dataTypePanel = new JPanel();
	dataTypeButtons = new JRadioButton[MultiRunData.NUM_TYPES];
	if (MultiRunData.NUM_TYPES > 0) {
	    for (int type=0; type<MultiRunData.NUM_TYPES; type++) {
		dataTypeButtons[type] = 
		    new JRadioButton(MultiRunData.getTypeName(type),type == defaultDataType);
		dataTypeButtons[type].addItemListener(mainWindow);
		dataTypeModes.add(dataTypeButtons[type]);
		dataTypePanel.add(dataTypeButtons[type]);
	    }
	}

	dataTypePanel.setBorder(BorderFactory.createLineBorder(Color.black));

	displayTable = new JButton("Display Tables");
	displayTable.addActionListener(mainWindow);
	done = new JButton("Close Window");
	done.addActionListener(mainWindow);

	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	setLayout(gbl);
	
	gbc.fill = GridBagConstraints.HORIZONTAL;

	Util.gblAdd(this, modePanel,     gbc, 0,0, 1,1, 1,0, 1,1,1,1);
	Util.gblAdd(this, dataTypePanel, gbc, 0,1, 2,1, 1,0, 1,1,1,1);

	gbc.fill = GridBagConstraints.BOTH;
	
	Util.gblAdd(this, displayTable,  gbc, 2,1, 1,1, 1,0, 1,1,1,1);
	Util.gblAdd(this, done,          gbc, 2,2, 1,1, 1,0, 1,1,1,1);
    }

    /**
     *  Returns the index of the selected radio button for types.
     *  A -1 is returned if nothing matches.
     */
    public int getSelectedIdx(ItemSelectable item) {
	JRadioButton dataTypeButton = (JRadioButton)item;
	for (int type=0; type<MultiRunData.NUM_TYPES; type++) {
	    if (dataTypeButton == dataTypeButtons[type]) {
		return type;
	    }
	}
	return -1;
    }
}


