package projections.gui;

import projections.misc.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MultiRunControlPanel extends Panel
    implements ActionListener
{

    private Button processCL;
    private Button done;

    private Label cmdLineLabel;
    private TextField cmdLine;

    private MultiRunWindow mainWindow;
    private MultiRunController controller;

    public MultiRunControlPanel(MultiRunWindow NmainWindow,
				MultiRunController Ncontroller)
    {
	mainWindow = NmainWindow;
	controller = Ncontroller;

	controller.registerControl(this);

	setBackground(Color.lightGray);

	processCL = new Button("Process Command Line");
	done = new Button("Close Window");
	cmdLineLabel = new Label("Enter Command Line");
	cmdLine = new TextField(50);
	
	processCL.addActionListener(this);
	done.addActionListener(this);

	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	setLayout(gbl);
	
	gbc.fill = GridBagConstraints.BOTH;

	Util.gblAdd(this, done,         gbc, 1, 2, 1, 1, 1, 1);
    }

    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() instanceof Button) {
	    Button b = (Button) evt.getSource();
	    if (b == done) {
		mainWindow.Close();
	    }
	}
    }
}
