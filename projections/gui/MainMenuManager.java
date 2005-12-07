package projections.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/* ***************************************************
 * MainMenuManager.java
 * Chee Wai Lee - 11/7/2002
 *
 * This is the class controlling the menus for the MainWindow
 * in projections. It will implement the state machine and
 * communication interface with MainWindow.
 *
 * ***************************************************/

public class MainMenuManager extends MenuManager
    implements ActionListener, ItemListener
{
    private JMenu fileMenu;
    private JMenu preferencesMenu;
    private JMenu toolMenu;
    private JMenu counterMenu;

    private static final int NUM_STATES = 4;
    private static final int NO_DATA = 0;
    private static final int OPENED_FILES = 1;
    private static final int OPENED_SUMMARY = 2;
    private static final int ADD_POSE = 3;

    private MainWindow parent;

    public MainMenuManager(JFrame parent) {
	super(parent);
	this.parent = (MainWindow)parent;
	createMenus();
    }

    void stateChanged(int state) {
	switch (state) {
	case NO_DATA :
	    setEnabled(fileMenu,
		       new boolean[]
		{
		    true,  // open file
		    false, // close current
		    false, // close all
		    false, // separator
		    true   // quit
		});
	    setEnabled(preferencesMenu,
		       new boolean[]
		{
		    true,  // change background
		    true,  // change foreground
		    false, // use a default grayscale color set instead.
		    false  // use a full set of colors.
		});
	    setEnabled(toolMenu,
		       new boolean[]
		{
		    false,  // Graphs
		    false,  // Timelines
		    false,  // Usage Profile
		    false,  // Communication
		    false,  // Communication vs Time
		    false,  // Call Table
		    false,  // View Log Files
		    false,  // Histograms
		    false,  // Overview
		    false,  // Animation
		    false,  // Time Profile Graphs
		    true,   // Multirun Analysis
		    false,   // Function Tools (temporary)
		    false   // POSE Analysis
		});
	    break;
	case OPENED_SUMMARY:
	    setEnabled(fileMenu,
		       new boolean[]
		{
		    true,
		    true,
		    true,
		    false,
		    true
		});
	    setEnabled(preferencesMenu,
		       new boolean[]
		{
		    true,  // change background
		    true,  // change foreground
		    false, // use a default grayscale color set.
		    false  // use a full set of colors.
		});
	    setEnabled(toolMenu,
		       new boolean[]
		{
		    true,  // Graphs
		    false,  // Timelines
		    true,  // Usage Profile
		    false,  // Communication
		    false,  // Communication vs Time
		    false,  // Call Table
		    false,  // View Log Files
		    false,  // Histograms
		    true,  // Overview
		    true,  // Animation
		    false,  // Time Profile Graphs
		    true,   // Multirun Analysis
		    false,   // Function Tools (temporary)
		    false,  // POSE Analysis
		});
	    break;
	case OPENED_FILES :
	    setEnabled(fileMenu,
		       new boolean[]
		{
		    true,
		    true,
		    true,
		    false,
		    true
		});
	    setEnabled(preferencesMenu,
		       new boolean[]
		{
		    true,  // change background
		    true,  // change foreground
		    false, // use a default grayscale color set.
		    false  // use a full set of colors.
		});
	    setEnabled(toolMenu,
		       new boolean[]
		{
		    true,  // Graphs
		    true,  // Timelines
		    true,  // Usage Profile
		    true,  // Communication
		    true,  // Communication vs Time
		    true,  // Call Table
		    true,  // View Log Files
		    true,  // Histograms
		    true,  // Overview
		    true,  // Animation
		    true,  // Time Profile Graphs
		    true,   // Multirun Analysis
		    true,   // Function Tools (temporary)
		    false   // POSE Analysis
		});
	    break;
	case ADD_POSE:
	    setEnabled(toolMenu, 13, // POSE Analysis
		       true);
	    break;
	}
    }

    private void createMenus() {
	fileMenu = makeJMenu("File", 
		  new Object[] 
	    {
		"Open File(s)",
		"Close current data",
		"Close all data",
		null,
		"Quit"
	    });
	menubar.add(fileMenu);

	preferencesMenu = makeJMenu("Preferences",
				    new Object[]
	    {
		"Change Background Color",
		"Change Foreground Color",
		"Use Default Grayscale Colors",
		"Use Standard Colors"
	    });
	menubar.add(preferencesMenu);

	toolMenu = makeJMenu("Tools", 
			     new Object[]
	    {
		"Graphs",
		"Timelines",
		"Usage Profile",
		"Time Profile Graph",
		"Communication",
		"Communication vs Time",
		"Call Table",
		"View Log Files",
		"Histograms",
		"Overview",
		"Animation",
		"Multirun Analysis",
		"Function Tool",
		"POSE Analysis"
	    });
	menubar.add(toolMenu);

	stateChanged(NO_DATA);
    }

    // overrides superclass
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JMenuItem) {
	    JMenuItem mi = (JMenuItem)e.getSource();
	    String arg = mi.getText();
	
	    // Will build state machine later. For now, we simply
	    // call the main window's methods.
	    if(arg.equals("Open File(s)"))
		parent.showOpenFileDialog();
	    if (arg.equals("Close current data")) {
		parent.closeCurrent();
	    } else if (arg.equals("Close all data")) {
		parent.closeAll();
	    } else if (arg.equals("Quit")) {
		parent.shutdown();
	    } else if (arg.equals("Change Background Color")) {
		parent.changeBackground();
	    } else if (arg.equals("Change Foreground Color")) {
		parent.changeForeground();
	    } else if (arg.equals("Use Default Grayscale Colors")) {
		parent.setGrayscale();
	    } else if (arg.equals("Use Standard Colors")) {
		parent.setFullColor();
	    } else if (arg.equals("Multirun Analysis") ||
		       arg.equals("Histograms") ||
		       arg.equals("Graphs") ||
		       arg.equals("Timelines") ||
		       arg.equals("Usage Profile") ||
		       arg.equals("Communication") ||
		       arg.equals("Communication vs Time") ||
		       arg.equals("Call Table") ||
		       arg.equals("View Log Files") ||
		       arg.equals("Animation") ||
		       arg.equals("Time Profile Graph") ||
		       arg.equals("Function Tool") ||
		       arg.equals("Overview") ||
		       arg.equals("POSE Analysis")) {
		parent.menuToolSelected(arg);
	    }
	}
    }

    public void itemStateChanged(ItemEvent e) {
    }
    
    // Interface methods to MainWindow
    public void fileOpened() {
	stateChanged(OPENED_FILES);
    }

    public void lastFileClosed() {
	stateChanged(NO_DATA);
    }

    public void summaryOnly() {
	stateChanged(OPENED_SUMMARY);
    }

    public void addPose() {
	stateChanged(ADD_POSE);
    }
}
