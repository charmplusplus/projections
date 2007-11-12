package projections.gui.Timeline;

import java.awt.*;
import javax.swing.*;

import projections.analysis.*;
import projections.gui.JLongTextField;
import projections.gui.LabelPanel;
import projections.gui.MainWindow;
import projections.gui.Util;

public class MessagePanel extends JPanel {
     
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    // GUI components
    private JPanel headerPanel;
    private JTable dataTable;
    
    private JLabel epLabel;
    private JPanel beginTimeField;
    private JPanel endTimeField;
    private JPanel numMsgsField;
    private JPanel createdPEField;
    private JPanel executedPEField;

    // Data objects
    private EntryMethodObject obj;
    private TimelineMessage messages[];
    private Object tableData[][];
    private Object columnNames[];

    private static final int NUM_FIELDS = 6;

    MessagePanel(EntryMethodObject obj) {
	this.obj = obj;
	messages = obj.getMessages();
	tableData = new Object[obj.getNumMsgs()][NUM_FIELDS];
	columnNames = new Object[NUM_FIELDS];
	setData();
	createLayout();
    }

    private void setData() {
	// the hard-coded column name stuff
	columnNames[0] = "Message #";
	columnNames[1] = "Message Size";
	columnNames[2] = "Time Sent";
	columnNames[3] = "Time since last Send";
	columnNames[4] = "Target EP";
	columnNames[5] = "Destination PE(s)";

	// setting the data
	for (int row=0; row<tableData.length; row++) {
	    // fill in the NUM_FIELDS columns
	    tableData[row][0] = new Integer(row);
	    tableData[row][1] = new Integer(messages[row].MsgLen);
	    tableData[row][2] = new Long(messages[row].Time);
	    tableData[row][3] = new Long((row>0) ? 
					 (messages[row].Time - 
					  messages[row-1].Time) :
					 0);
	    tableData[row][4] = MainWindow.runObject[myRun].getEntryName(messages[row].Entry);
	    if (messages[row].destPEs != null) {
		// This is a multicast.
		tableData[row][5] = "";
		for (int i=0; i<messages[row].destPEs.length-1; i++) {
		    tableData[row][5] = (String)tableData[row][5] + messages[row].destPEs[i] + ", ";
		}
		tableData[row][5] = (String)tableData[row][5] + 
		    messages[row].destPEs[messages[row].destPEs.length-1] + "";
	    } else {
		if (messages[row].numPEs > 0) {
		    // This is a broadcast of some sort.
		    if (messages[row].numPEs == 
			MainWindow.runObject[myRun].getNumProcessors()) {
			tableData[row][5] = 
			    "Group Broadcast (" + messages[row].numPEs + ")";
		    } else {
			tableData[row][5] = 
			    "Nodegroup Broadcast (" + messages[row].numPEs + 
			    ")";
		    }
		} else {
		    // This is a regular send event.
		    tableData[row][5] = "unknown";
		}
	    }
	}
    }

    private void createLayout() {
	epLabel = new JLabel(MainWindow.runObject[myRun].getEntryChareName(obj.getEntry()) +
			     " -- " +
			     MainWindow.runObject[myRun].getEntryName(obj.getEntry()),
			     JLabel.CENTER);
	beginTimeField = new LabelPanel("BEGIN TIME:",
					new JLongTextField(obj.getBeginTime(),
							   10));
	endTimeField = new LabelPanel("END TIME:",
				      new JLongTextField(obj.getEndTime(),
							 10));
	numMsgsField = new LabelPanel("MSGS:",
				      new JTextField("" + 
						     obj.getNumMsgs(),10));
	createdPEField = new LabelPanel("CREATED BY:",
					new JTextField("Processor " +
						       obj.getPCreation()));
	executedPEField = new LabelPanel("EXECUTED ON:",
					 new JTextField("Processor " +
							obj.getPCurrent()));

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	gbc.fill = GridBagConstraints.HORIZONTAL;

	headerPanel = new JPanel();
	headerPanel.setLayout(gbl);
	
	// construct header panel
	Util.gblAdd(headerPanel, epLabel,           gbc, 0,0, 3,1, 1,1);
	Util.gblAdd(headerPanel, beginTimeField,    gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(headerPanel, endTimeField,      gbc, 1,1, 1,1, 1,1);
	Util.gblAdd(headerPanel, numMsgsField,      gbc, 2,1, 1,1, 1,1);
	Util.gblAdd(headerPanel, createdPEField,    gbc, 0,2, 1,1, 1,1);
	Util.gblAdd(headerPanel, executedPEField,   gbc, 1,2, 1,1, 1,1);

	dataTable = new JTable(tableData, columnNames);
	JScrollPane scrollPane = new JScrollPane(dataTable);

	JPanel scrollPaneContainer = new JPanel();
	scrollPaneContainer.setLayout(new BoxLayout(scrollPaneContainer,
						    BoxLayout.X_AXIS));
	scrollPaneContainer.add(scrollPane);

	setLayout(gbl);

	// construct final panel
	Util.gblAdd(this, headerPanel,              gbc, 0,0, 1,1, 1,0);

	gbc.fill = GridBagConstraints.BOTH;

	Util.gblAdd(this, scrollPaneContainer,      gbc, 0,1, 1,1, 1,1);
    }
}
