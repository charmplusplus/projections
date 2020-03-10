package projections.Tools.LogFileViewer;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntry;


/** This class displays a log file as html formatted text inside a JPanel 
 *  
 *   Inside this JPanel is a JScrollPane that provides a scrollbar.
 *   Inside the JScrollPane is a JTextPane that holds the formatted text.
 *   
 *   The entries in the vector are formatted differently depending on their type.
 *   
 *   A StringBuilder object is used to build up the long string because simple
 *   string concatenation is too slow(each new string must copy the entire old 
 *   string into itself).
 *
 */ 


class LogFileViewerTextArea extends JPanel
{

	private static int myRun = 0;

	private JTextPane textPane;
	private JScrollPane scrollPane;


	public LogFileViewerTextArea() {
		// Set the layout for this JPanel.
		setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension(800,500));

		// At first we don't have any data to display. 
		// Later setPE() will add the real text.
		String text = new String("<html><body>Log Not Yet Loaded</body></html>");

		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setContentType("text/html");
		textPane.setText(text);

		// The only component in this JPanel is a JScrollPane containing textPane
		scrollPane = new JScrollPane(textPane);
		this.add(scrollPane, BorderLayout.CENTER);

	}
	
	
 
	protected void setPE(int PE, long startTime, long endTime) {
 
		if (!(MainWindow.runObject[myRun].hasLogData())){
			textPane.setText("<h1>ERROR: Don't have any log data</h1>");
			return;
		}



		// Create a buffer into which we compose the html formatted text.
		// We expect the length of each log file entry to be under 150. 
		// If this buffer is large enough, then it won't have to be resized(and thus will be fast).
		StringBuilder htmlFormattedTable = new StringBuilder(50000);


		// Start composing the html formatted text
		htmlFormattedTable.append( "<html><body><font size=+2> All events in log for PE " + PE + " with times between " + startTime + " and " + endTime + "</font>");
		htmlFormattedTable.append( "<table><tr><td><h2>Time</h2><td><h2>Event type and description</h2>");

		try {	  
			GenericLogReader reader = new GenericLogReader(PE, MainWindow.runObject[myRun].getVersion());

			while (true) {
				LogEntry data = reader.nextEvent();


				if(data.time >= startTime && data.time <= endTime){

					htmlFormattedTable.append( "<tr><td>" + data.time + "<td>");
					
					htmlFormattedTable.append(data.htmlFormattedDescription());

					// Add a blank row for after the end events
					if(data.type == ProjDefs.END_PROCESSING || data.type == ProjDefs.END_IDLE){
						htmlFormattedTable.append("<tr>");	
					}

				}


			}

		} catch (Exception e) {
			
			// Put the finishing touches on the html formatted text
			htmlFormattedTable.append( "</table></body></html>");

			// Set the text in textPane to the html formatted text
			textPane.setText(htmlFormattedTable.toString());		

			// Scroll to the top
			textPane.setSelectionStart(0);
			textPane.setSelectionEnd(0);
		}
		
	}
	
}
