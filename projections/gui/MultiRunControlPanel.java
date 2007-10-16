package projections.gui;

import projections.misc.*;
import javax.swing.*;
import java.awt.*;

public class MultiRunControlPanel extends JPanel
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ButtonGroup dataTypeModes;
    private JPanel dataTypePanel;
    private JRadioButton dataTypeButtons[];

    private JButton displayTable;
    private JButton done;

    public MultiRunControlPanel(MultiRunWindow mainWindow, int defaultDataType)
    {
	setBackground(Color.lightGray);

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

	Util.gblAdd(this, dataTypePanel,  gbc, 0,0, 1,2, 1,0, 1,1,1,1);

	gbc.fill = GridBagConstraints.BOTH;
	
	Util.gblAdd(this, displayTable,  gbc, 1,0, 1,1, 1,0, 1,1,1,1);
	Util.gblAdd(this, done,          gbc, 1,1, 1,1, 1,0, 1,1,1,1);
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


