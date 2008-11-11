package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleContext;

import projections.analysis.ProjDefs;
import projections.analysis.ViewerEvent;
import projections.misc.LogLoadException;


/** This class displays a log file as html formatted text inside a JPanel 
 *  
 *   Inside this JPanel is a JScrollPane that provides a scrollbar.
 *   Inside the JScrollPane is a JTextPane that holds the formatted text.
 *   
 *   The text is composed from the log file data obtained in a vector:
 *   	Vector v = MainWindow.runObject[myRun].logLoader.view(PE);
 *
 *   The entries in the vector are formatted differently depending on their type.
 *   
 *   A StringBuilder object is used to build up the long string because simple
 *   string concatenation is too slow(each new string must copy the entire old 
 *   string into itself).
 *
 */


public class LogFileViewerTextArea extends JPanel
{

	static int myRun = 0;

	JTextPane textPane;
	JScrollPane scrollPane;

	public LogFileViewerTextArea()
	{
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


	public void setPE(int PE, long startTime, long endTime) {

		if (!(MainWindow.runObject[myRun].hasLogData())){
			textPane.setText("<h1>ERROR: Don't have any log data</h1>");
			return;
		}


		// Load the log file entries into the vector v
		Vector v;
		try {
			v = MainWindow.runObject[myRun].logLoader.view(PE);
		} catch (LogLoadException e) {
			textPane.setText("<h1>Failed to load Log files for PE " + PE + "</h1>");
			return;
		}

		// Verify that we loaded some data in v
		if( v == null || v.size()==0) {
			textPane.setText("<h1>Don't have log data for PE " + PE+ "</h1>");
			return;
		}

		// The number of log entries we found is:
		int length = v.size();


		// Create a buffer into which we compose the html formatted text.
		// We expect the length of each log file entry to be under 150. 
		// If this buffer is large enough, then it won't have to be resized(and thus will be fast).
		StringBuilder htmlFormattedTable = new StringBuilder(length * 150);


		// Start composing the html formatted text
		htmlFormattedTable.append( "<html><body><font size=+2> All events in log for PE " + PE + " with times between " + startTime + " and " + endTime + "</font>");
		htmlFormattedTable.append( "<table><tr><td><h2>Time</h2><td><h2>Event type and description</h2>");

		// Compose the rows in the html table
		for( int i = 0;i < length;i++ ) {

			ViewerEvent ve = (ViewerEvent)v.elementAt(i);

			if(ve.Time >= startTime && ve.Time <= endTime){

				htmlFormattedTable.append( "<tr><td>" + ve.Time + "<td>");

				switch( ve.EventType ) {
				case ( ProjDefs.CREATION ):
					htmlFormattedTable.append( "<font size=+1 color=\"#660000\">CREATE</font> message to be sent to <em>" + ve.Dest + "</em>");
				break;
				case ( ProjDefs.CREATION_BCAST ):
					if (ve.numDestPEs == MainWindow.runObject[myRun].getNumProcessors()) {
						htmlFormattedTable.append( "<font size=+1 color=\"#666600\">GROUP BROADCAST</font> (" + ve.numDestPEs + " processors)");
					} else {
						htmlFormattedTable.append( "<font size=+1 color=\"#666600\">NODEGROUP BROADCAST</font> (" + ve.numDestPEs + " processors)");
					}
				break;
				case ( ProjDefs.CREATION_MULTICAST ):
					htmlFormattedTable.append( "<td><font size=+1 color=\"#666600\">MULTICAST</font> message sent to " + ve.numDestPEs + " processors");
				break;
				case ( ProjDefs.BEGIN_PROCESSING ):
					htmlFormattedTable.append( "<font size=+1 color=\"#000088\">BEGIN PROCESSING</font> of message sent to <em>" + ve.Dest + "</em> from processor " + ve.SrcPe);
				break;
				case ( ProjDefs.END_PROCESSING ):
					htmlFormattedTable.append( "<font size=+1 color=\"#000088\">END PROCESSING</font> of message sent to <em>" + ve.Dest + "</em> from processor " + ve.SrcPe);
				htmlFormattedTable.append( "<tr>"); // add an extra blank row after this end event
				break;
				case ( ProjDefs.ENQUEUE ):
					htmlFormattedTable.append( "<font size=+1>ENQUEUEING</font> message received from " + "processor " + ve.SrcPe + " destined for " + ve.Dest);
				break;
				case ( ProjDefs.BEGIN_IDLE ):
					htmlFormattedTable.append( "<font size=+1 color=\"#333333\">IDLE begin</font>");
				break;
				case ( ProjDefs.END_IDLE ):
					htmlFormattedTable.append( "<font size=+1 color=\"#333333\">IDLE end</font>");
				htmlFormattedTable.append( "<tr>"); // add an extra blank row after this end event
				break;
				case ( ProjDefs.BEGIN_PACK ):
					htmlFormattedTable.append( "<font size=+1 color=\"#008800\">BEGIN PACKING</font> a message to be sent");
				break;
				case ( ProjDefs.END_PACK ):
					htmlFormattedTable.append( "<font size=+1 color=\"#008800\">FINISHED PACKING</font> a message to be sent");
				break;
				case ( ProjDefs.BEGIN_UNPACK ):
					htmlFormattedTable.append( "<font size=+1 color=\"#880000\">BEGIN UNPACKING</font> a received message");
				break;
				case ( ProjDefs.END_UNPACK ):
					htmlFormattedTable.append( "<font size=+1 color=\"#880000\">FINISHED UNPACKING</font> a received message");
				break;
				default:
					htmlFormattedTable.append( "Unknown Event Type:" + ve.EventType + " !!!");
				break;
				}
			}
		}
		
		// Put the finishing touches on the html formatted text
		htmlFormattedTable.append( "</table></body></html>");

		// Set the text in textPane to the html formatted text
		textPane.setText(htmlFormattedTable.toString());		

		// Scroll to the top
		textPane.setSelectionStart(0);
		textPane.setSelectionEnd(0);

	}

}
