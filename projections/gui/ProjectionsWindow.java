package projections.gui;

import java.awt.event.*;
import javax.swing.*;

/**
 *  ProjectionsWindow
 *  written by Chee Wai Lee
 *  6/28/2002
 *  changed by Sindhura Bandhakavi
 *  8/1/2002
 *  major refactoring for ease-of-use by Chee Wai Lee
 *  12/15/2003
 *
 *  This class should be inherited by all projections tools. 
 *  The user may choose to include a dialog box whose base class
 *  allows the user to input a range of processors and a time interval.
 *
 *  The implementer would need to implement (otherwise abstract) methods to 
 *  support the basic RangeDialog object (if any). This includes data
 *  communication mechanisms between dialog and window.
 *
 *  The implementer would also have to define various public global variables
 *  in the concrete class to hold data for parameters, either from range
 *  dialogs or directly set.
 *
 *  e.g. public OrderedIntList validPEs;
 *       public long startTime;
 *       public long endTime;
 *  
 *  ProjectionsWindow also implements a cross-tool button panel that
 *  developers can choose to use by getToolButtonPanel().
 * 
 */

public abstract class ProjectionsWindow 
    extends JFrame
{
    /**
     *  unique identifier to allow the main window to identify and
     *  destroy this window completely
     */
    protected int myWindowID;
    /**
     *  reference to the main window
     */
    protected MainWindow parentWindow;
    
    // NOTE: There NEED NOT be a dialog.
    protected RangeDialog dialog;

    // Must implement code to show the dialog box and handle
    // the return values. There NEED NOT be a dialog.
    // The dialog blocks until the user hits OK or CANCEL.
    abstract void showDialog();

    // Must implement code to display the window. This
    // bypasses the dialog and MUST NOT perform any serious
    // blocking computation.
    abstract void showWindow();

    /**
     *  Must implement code to set parameter data to the window
     *  (more specifically the tool) via the dialog. Because of the 
     *  arbitrary nature of parameter data in projections, this has 
     *  to be in the form of global variables:
     *  e.g.     getDialogData() {
     *              validPEs = dialog.validPEs;
     *              startTime = dialog.startTime;
     *              endTime = dialog.endTime;
     *           }
     *
     *  This is intended to be used after the showDialog code.
     *  e.g.     showDialog();
     *           if (!dialog.isCancelled()) {
     *             getDialogData();
     *             ... blah blah blah ...
     *           }
     */
    abstract void getDialogData();

    /**
     *  constructor - Wierdness. For Reflection to work on this class,
     *  the inheriting classes must use:
     *  <constructor>(MainWindow, Integer) instead of
     *  <constructor>(MainWindow, int)
     *
     *  Hence the wrapper constructor.
     */ 
    public ProjectionsWindow(MainWindow parentWindow, Integer myWindowID) {
	this(parentWindow, myWindowID.intValue());
    }
    
    public ProjectionsWindow(MainWindow parentWindow, int myWindowID) {
	this.myWindowID = myWindowID;
	this.parentWindow = parentWindow;
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e){
		    close();
		}
	    });
    }

    public JPanel getToolButtonPanel() {
	return null; // for now
    }

    // close (and destroy all access) to the window	
    public void close(){
	dispose();
	parentWindow.closeChildWindow(myWindowID);
    }
}

