package projections.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.JButton;
import javax.swing.JPanel;

import projections.misc.*;
import projections.analysis.*;

public class LogFileViewerWindow extends ProjectionsWindow implements ActionListener
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	static int myRun = 0;
	
	
	/** The JPanel that loads a log file and displays it as html formatted text */
	private LogFileViewerTextArea textArea;

	/** A button that can be used to choose a different processor's log file */
	private JButton bOpen;

	/** A dialog input */
	LogFileViewerDialog dialog;
	

	/** The method that gets called when the user selects this tool from the Projections menu */
	public LogFileViewerWindow(MainWindow parentWindow, Integer myWindowID)
	{
		super(parentWindow, myWindowID);

		setTitle("Projections Log File Viewer - " + MainWindow.runObject[myRun].getFilename() + ".sts");

		// Create the GUI layout:
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());	  

		// Create the html formatting log display JPanel
		textArea = new LogFileViewerTextArea();

		// Create a button. When the button is clicked, 
		// The action handler for 'this' is called
		bOpen = new JButton("Load Different PE ...");
		bOpen.addActionListener(this);
		bOpen.setPreferredSize(new Dimension(200, 40));
		
		// Put the button in its own JPanel. this way the button can be sized smaller than the entire width of the window
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.add(bOpen, BorderLayout.WEST);
		
		// Add both JPanels to the new JPanel p
		p.add(textArea, BorderLayout.CENTER);
		p.add(buttonPanel, BorderLayout.SOUTH);

		// Add our newly constructed JPanel p to this tool's window
		this.add(p);

		// Force the window to be displayed
		pack();
		setVisible(true);

		// Display the input dialog box to the user
		showDialog();

	}   

	
	/** This handles the button clicks from the user */
	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource() == bOpen){
			showDialog();		
		}
	}   



	/** Create a new dialog box if one isn't already active */	
	public void showDialog()
	{
		if(dialog == null){
			dialog = new LogFileViewerDialog(this);
			dialog.setVisible(true);
		}
	}	   

		 
	/** Load the new log file */
	public void setLogFileNum(int p)
	{
		textArea.setPE(p);
	}

	
	/** Close the dialog box and null our reference to it.
	 *  This is needed so we only open one dialog box at a time
	 */
	public void closeDialog() {
		dialog.dispose();
		dialog = null;
	}
		
	
	/** A function required for ProjectionsWindow interface. We just ignore it */
	protected void windowInit() {
		// do nothing, no parameters need to be set.
	}
	
	/** A function required for ProjectionsWindow interface. We just ignore it */
	public void showWindow() {
		// do nothing for now.
	}
	
	/** A function required for ProjectionsWindow interface. We just ignore it */
	public void getDialogData() {
		// do nothing. This tool uses its own dialog.
	}

}
