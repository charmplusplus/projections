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

    private static final int NO_DATA = 0;
    private static final int OPENED_FILES = 1;

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
		    false,  // Communication Histogram
		    false,  // View Log Files
		    false,  // Histograms
		    false,  // Overview
		    false,  // Animation
		    false,  // General Graphs
		    true,   // Multirun Analysis
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
	    setAllTo(preferencesMenu, true);
	    setAllTo(toolMenu, true);
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
		"Communication Histogram",
		"View Log Files",
		"Histograms",
		"Overview",
		"Animation",
		"General Graph",
		"Multirun Analysis"
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
		       arg.equals("Communication Histogram") ||
		       arg.equals("View Log Files") ||
		       arg.equals("Animation") ||
		       arg.equals("General Graph") ||
		       arg.equals("Overview")) {
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
}
