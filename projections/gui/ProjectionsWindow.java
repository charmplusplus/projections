package projections.gui;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

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
 *  developers can choose to use by getToolButtonPanel(). Classes which
 *  extend ProjectionsWindow now must call setLayout() funciton which
 *	shall be defined here.
 *
 */

public abstract class ProjectionsWindow
    extends JFrame
    implements ActionListener
{
	private Button window[];

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

    /**
     *  Must implement code to show the dialog box and handle
     *  the return values. There NEED NOT be a dialog.
     *  The dialog blocks until the user hits OK or CANCEL.
     *
     *  Depending on the kind of behavior required, developers of
     *  ProjectionsWindows should decide whether or not they wish
     *  to call setDialogData();
     */
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
     *  Subclasses of ProjectionsWindow are *expected* to set the
     *  dialog's parameter variables by either (or both):
     *
     *  1) using the dialog's accessors.
     *  2) directly setting the dialog's public parameter variables.
     *
     *  The exact manner this is done will depend on how the dialog
     *  is actually implemented.
     *
     *  This should be done before calling super.setDialogData();
     *
     */
    protected void setDialogData() {
	if (dialog != null) {
	    dialog.updateFields();
	}
    }

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

    public void setLayout(JPanel mainPanel) {
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		gbc.fill = GridBagConstraints.BOTH;
		JPanel contentPane = (JPanel) getContentPane();
		contentPane.setLayout(gbl);

		Util.gblAdd(contentPane, mainPanel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(contentPane, getToolButtonPanel(), gbc, 0,2, 1,1, 0,0);
		//getContentPane().add(mainPanel);
		//getContentPane().add(getToolButtonPanel());
	}

    public JPanel getToolButtonPanel() {
		//System.out.println("Start of ProjectionsWindow.getToolButtonPanel()");
		JPanel appButtonPanel = new JPanel();
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		window = new Button[parentWindow.NUM_WINDOWS];

		/*
		 * Apurva
		 * Note: currently using myWindowID to figure out which window I am
		 * As long as myWindowID continues to correspond to
		 * mainWindow.GRAPHWIN .... mainWindow.GEN_GRAPH_WIN...
		 * this should continually work
		 */
		for(int k=0; k<parentWindow.NUM_WINDOWS; k++) {
			if (k != myWindowID) {
				window[k] = new Button(parentWindow.toolDescriptions[k]);
				window[k].addActionListener(this);
				//System.out.println("Added button for "+ parentWindow.toolDescriptions[k]);
				Util.gblAdd(appButtonPanel, window[k], gbc, k,0, 1,1, 1,1);

			}
		}

		//System.out.println("End of ProjectionsWindow.getToolButtonPanel()");
		return appButtonPanel;
    }

    public void actionPerformed(ActionEvent ae){
		//System.out.println("Start of ProjectionWindow.itemStateChanged()");
		if (ae.getSource() instanceof Button) {
			Button b = (Button)ae.getSource();
			for(int k=0; k < parentWindow.NUM_WINDOWS;k++) {
				if (b == window[k]) {
					//System.out.println(parentWindow.getChildName(k)  +"'s button was pressed");
					//parentWindow.showChildWindow(parentWindow.getChildName(k), k);

					/* Apurva
					 *
					 * For now, we are not going to do anything until
					 * CW has implemented the ProjectionWindowEvent
					 *
					 */

					break;
				}
			}
		}

		//System.out.println("End of ProjectionsWindow.itemStateChanged()");
	}

    // close (and destroy all access) to the window
    public void close(){
		dispose();
		parentWindow.closeChildWindow(myWindowID);
    }
}

