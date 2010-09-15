package projections.Tools.LogFileViewer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.ProjectionsWindow;
import projections.gui.RangeDialog;

public class LogFileViewerWindow extends ProjectionsWindow implements ActionListener
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;
	
	private JTabbedPane tabbedPane;
	
	/** A button that can be used to choose a different processor's log file */
	private JButton bOpen;
	
	/** Remember what the user put in the dialog box */
	private OrderedIntList validPEs;
	private long startTime;
	private long endTime;

	/** The method that gets called when the user selects this tool from the Projections menu */
	public LogFileViewerWindow(MainWindow parentWindow)
	{
		super(parentWindow);
 
		setTitle("Projections Log File Viewer - " + MainWindow.runObject[myRun].getFilename() + ".sts");

		// Create the GUI layout:
		// This object is a LogFileViewerWindow which is a swing Container.
		// In the Container, we can set a layout.
		// The layout will determine the position for all other components added to the Container.
		// A BorderLayout is the simplest standard layout.
		setLayout(new BorderLayout());	  

		// The overall layout will be a tabbed main area and a button at the bottom.
		// First we create these two sections, then below we will put the two together.
		
		// Create a JTabbedPane. When data is later loaded, there will be one tab per PE
		// But for now we will create a simple little tab with a single JLabel in it
		tabbedPane = new JTabbedPane();
		JLabel simpleLabel = new JLabel("<html><body><h1>Data is loading</h1></body></html>");
		tabbedPane.add("loading ...", simpleLabel);
		
		// Create a button. When the button is clicked, 
		// The action handler for 'this' is called
		bOpen = new JButton("Load Different PE ...");
		bOpen.addActionListener(this);
		bOpen.setPreferredSize(new Dimension(200, 40));
		
		// Add both the button and the JTabbedPane to this Container
		add(tabbedPane, BorderLayout.CENTER);
		add(bOpen, BorderLayout.SOUTH);

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

	
	public void showDialog() {
		if (dialog == null) {
			dialog = new RangeDialog(this, "select Range", null, false);
		}
		
		dialog.displayDialog();
		if (!dialog.isCancelled()) {
		
			// At this point the user has provided a time range and list of PEs in the dialog box 
			
			// get the time range and PE list from the dialog box
			validPEs = dialog.getSelectedProcessors();
			startTime = dialog.getStartTime();
			endTime = dialog.getEndTime();
			
			// because it may take a while to load the data, turn on the waiting cursor
			setCursor(new Cursor(Cursor.WAIT_CURSOR));

			// Remove all the tabs that previously were there
			tabbedPane.removeAll();
			
			// Access the list of PEs chosen by the user:
			for(Integer pe : validPEs){
				
				// Create a text area to put in the tab for this PE 
				LogFileViewerTextArea textArea = new LogFileViewerTextArea();
				// Have the text area load the logfile data
				textArea.setPE(pe,startTime,endTime);		
				// Add a tab for the PE
				tabbedPane.add("PE " + pe, textArea);
				
			}
			
			// Since the layout of the window has changed, 
			// we should have everything be resized. pack() does this
			this.pack();

			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			
		}
	}

}
