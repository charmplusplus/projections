package projections.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *  LabelPanel is a representation of commonly used label-field pairs 
 *  in GUIs.
 */

public class LabelPanel extends JPanel {

	JTextField field;
    JLabel label;

    public LabelPanel(String label, int alignment, JTextField fieldEntry) {
	this.label = new JLabel(label, alignment);
	field = fieldEntry;
	createLayout();
    }

    public LabelPanel(String label, JTextField fieldEntry) {
	this(label, JLabel.RIGHT, fieldEntry);
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
