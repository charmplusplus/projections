package projections.gui;

import javax.swing.*;
import java.awt.*;

public class MultiRunFileDialogControl
    extends JPanel
{
    // Gui components
    private JLabel label;
    private JTextField field;
    private JCheckBox chkbox;

    public MultiRunFileDialogControl() {
	label = new JLabel("Enter Basename here:");
	field = new JTextField(10);
	chkbox = new JCheckBox("Using default root for multiple data sets",
			       true);

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	this.setLayout(gbl);
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.anchor = GridBagConstraints.NORTH;
	Util.gblAdd(this, label, gbc, 0,0, 1,1, 0,0, 2,2,2,2);
	Util.gblAdd(this, field, gbc, 0,1, 1,1, 0,0, 2,2,2,2);
	Util.gblAdd(this, chkbox, gbc, 0,2, 1,1, 0,1, 2,2,2,2);
    }

    public String getBaseName() {
	return field.getText();
    }

    public boolean isDefault() {
	return chkbox.isSelected();
    }
}
