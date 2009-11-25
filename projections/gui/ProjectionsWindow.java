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
 *
 */

public abstract class ProjectionsWindow
extends JFrame
implements ActionListener
{
	private Button window[];


	/**
	 *  reference to the main window
	 */
	protected MainWindow parentWindow;


	/** Each window has a dialog box associated with it */
	public RangeDialog dialog;


	/**
	 *  Must implement code to show the dialog box and handle
	 *  the return values. There NEED NOT be a dialog.
	 *  The dialog blocks until the user hits OK or CANCEL.
	 *
	 *  Depending on the kind of behavior required, developers of
	 *  ProjectionsWindows should decide whether or not they wish
	 *  to call setDialogData();
	 */
	protected abstract void showDialog();


	/** Simply call the parent's constructor that sets an empty window title and listens for the window closing. */	
	public ProjectionsWindow(MainWindow parentWindow) {
		this("", parentWindow);
	}

	/** Simply set the window title and listen for the window closing. */	
	public ProjectionsWindow(String title, MainWindow parentWindow) {
		this.parentWindow = parentWindow;
		setTitle(title);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				close();
			}
		});
	}

	public void setLayout(Component mainPanel) {
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();
		gbc.fill = GridBagConstraints.BOTH;
		Container contentPane = getContentPane();
		contentPane.setLayout(gbl);
		Util.gblAdd(contentPane, mainPanel, gbc, 0,0, 1,1, 1,1);
	}

	public void actionPerformed(ActionEvent ae){
		if (ae.getSource() instanceof Button) {
			Button b = (Button)ae.getSource();
			for(int k=0; k < MainWindow.NUM_WINDOWS;k++) {
				if (b == window[k]) {
					break;
				}
			}
		}
	}

	// close (and destroy all access) to the window
	public void close(){
		dispose();
		parentWindow.closeChildWindow(this);
	}

	/** A method to be overridden by any subclasses that wish to be able to have a new PE loaded from within another tool. */
	public void addProcessor(int pe) {
	}



}
