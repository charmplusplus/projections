package projections.gui;

import projections.gui.graph.*;
import projections.misc.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 *  Written by Chee Wai Lee
 *  3/27/2002
 *
 *  MultiRunWindow presents the main display window for multi-run analysis
 *  data. It starts by presenting a file dialog for users to select a set
 *  of log files.
 *
 */

public class MultiRunWindow extends Frame 
    implements ActionListener
{
    // Controller object
    private static MultiRunController controller;

    // Gui components
    private MainWindow mainWindow;
    private MultiRunControlPanel controlPanel;
    private MultiRunDisplayPanel displayPanel;
    private LegendPanel legendPanel;

    public MultiRunWindow(MainWindow mainWindow) 
    {
	this.mainWindow = mainWindow;

	// creates a controller object if it does not already exist
	if (controller == null) {
	    controller = new MultiRunController(this);
	}

	addWindowListener(new WindowAdapter()
	    {
		public void windowClosing(WindowEvent e)
		{
		    Close();
		}
	    });
	setBackground(Color.lightGray);

	CreateLayout();
	pack();

	setTitle("Multi-Run Analysis");
	setVisible(true);
	showFileDialog();
    }

    public void actionPerformed(ActionEvent evt)
    {
    }

    public void Close()
    {
	setVisible(false);
	dispose();
	mainWindow.CloseMultiRunWindow();
    }

    /**
     *  The file dialog implemented will restrict users to the following
     *  summary data set structure:
     *
     *  1) The root directory of the summary data set must be named using
     *     the BASENAME of the summary data where summary files are named
     *     BASENAME.0.sum.
     *  2) There is *exactly* one root directory.
     *  3) Every child of the root directory *must* also be a directory.
     *  4) Each subdirectory's name will be used to annotate the data
     *     displayed on the GUI.
     *  5) Each subdirectory *must* contain *exactly* one set of summary
     *     data. log data is ignored.
     *
     */
    public void showFileDialog() {
	// tentatively using a Swing File chooser object.
	// A more specific dialog box should be used to allow the user
	// to specify more options than just the root directory for
	// summary data sets.

	JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
	MultiRunFileDialogControl accessory = new MultiRunFileDialogControl();
	fc.setDialogTitle("Choose directories for summary data");
	fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	fc.setMultiSelectionEnabled(true);
	fc.setAccessory(accessory);
	int returnVal = fc.showDialog(this,"Choose");

	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    File inDirs[] = fc.getSelectedFiles();
	    String inDirPathnames[] = new String[inDirs.length];
	    for (int i=0;i<inDirs.length;i++) {
		inDirPathnames[i] = inDirs[i].getAbsolutePath();
	    }
	    if (inDirs.length > 0) {
		controller.processUserInput(((MultiRunFileDialogControl)fc.getAccessory()).getBaseName(), 
					    inDirPathnames,
					    ((MultiRunFileDialogControl)fc.getAccessory()).isDefault());
	    }
	}
    }

    private void CreateLayout()
    {
	Panel p = new Panel();
	add("Center", p);
	p.setBackground(Color.gray);

	displayPanel = new MultiRunDisplayPanel(this, controller);
	controlPanel = new MultiRunControlPanel(this, controller);
	legendPanel = new LegendPanel();
	controller.registerLegend(legendPanel);

	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	p.setLayout(gbl);

	gbc.fill = GridBagConstraints.BOTH;
	Util.gblAdd(p, displayPanel, gbc, 0,0, 1,1, 1,1, 2,2,2,2); 
	Util.gblAdd(p, controlPanel, gbc, 0,1, 2,1, 1,0, 2,2,2,2); 
	Util.gblAdd(p, legendPanel,  gbc, 1,0, 1,1, 1,1, 2,2,2,2); 
    }

}
