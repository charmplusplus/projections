package projections.gui;

import java.awt.*;
import javax.swing.*;

/**
 *  LabelPanel is a representation of commonly used label-field pairs 
 *  in GUIs.
 */

public class LabelPanel extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JTextField field;
    JLabel label;

    LabelPanel(String label, int alignment, JTextField fieldEntry) {
	this.label = new JLabel(label, alignment);
	field = fieldEntry;
	createLayout();
    }

    LabelPanel(String label, JTextField fieldEntry) {
	this(label, JLabel.RIGHT, fieldEntry);
    }

    public JTextField getField() {
	return field;
    }

    private void createLayout() {
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	setLayout(gbl);

	gbc.fill = GridBagConstraints.NONE;
	Util.gblAdd(this, label, gbc, 0,0, 1,1, 0,1);

	gbc.fill = GridBagConstraints.HORIZONTAL;
	Util.gblAdd(this, field, gbc, 1,0, 1,1, 1,1);
    }
}
