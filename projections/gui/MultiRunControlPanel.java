package projections.gui;

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

    public MultiRunControlPanel(MultiRunWindow NmainWindow)
    {
	mainWindow = NmainWindow;
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

	Util.gblAdd(this, cmdLineLabel, gbc, 0, 0, 1, 1, 1, 1);
	Util.gblAdd(this, cmdLine,      gbc, 0, 1, 1, 1, 1, 1);
	Util.gblAdd(this, processCL,    gbc, 0, 2, 1, 1, 1, 1);
	Util.gblAdd(this, done,         gbc, 1, 2, 1, 1, 1, 1);
    }

    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() instanceof Button) {
	    Button b = (Button) evt.getSource();

	    if (b == processCL) {
		StringTokenizer st = new StringTokenizer(cmdLine.getText());
		String args[] = new String[st.countTokens()];
		for (int i=0; i<args.length; i++) {
		    args[i] = st.nextToken();
		}
		mainWindow.processCommandLine(args);
	    } else if (b == done) {
		mainWindow.Close();
	    }
	}
    }
}
