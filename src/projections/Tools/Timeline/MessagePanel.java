package projections.Tools.Timeline;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import projections.gui.JLongTextField;
import projections.gui.LabelPanel;
import projections.gui.MainWindow;
import projections.gui.Util;

class MessagePanel extends JPanel {
  
	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
	private int myRun = 0;

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
    private Set messages;
    private Object tableData[][];
    private Object columnNames[];

    private static final int NUM_FIELDS = 6;

    private DecimalFormat df;
    
    MessagePanel(EntryMethodObject obj) {
	this.obj = obj;
	messages = obj.getMessages();
	tableData = new Object[obj.getNumMsgs()][NUM_FIELDS];
	columnNames = new Object[NUM_FIELDS];
	df = new DecimalFormat();
	df.setGroupingUsed(true);
	df.setGroupingSize(3);
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
    	Iterator iter = messages.iterator();	
    	int row =0;
    	TimelineMessage msg=null, prev=null;
    	while(iter.hasNext()){
    		prev = msg;
    		msg = (TimelineMessage) iter.next();

    		// fill in the NUM_FIELDS columns
    		tableData[row][0] = Integer.valueOf(row);

    		tableData[row][1] = df.format(msg.MsgLen);
    		tableData[row][2] = df.format(msg.Time);
    		tableData[row][3] = df.format((row>0) ? (msg.Time - prev.Time) : (msg.Time - obj.getBeginTime()) );
    		tableData[row][4] = MainWindow.runObject[myRun].getEntryNameByID(msg.Entry);								 
    		tableData[row][5] = msg.destination(MainWindow.runObject[myRun].getNumProcessors());

    		row++;
    	}// end while
    }

    private void createLayout() {
	epLabel = new JLabel(MainWindow.runObject[myRun].getEntryFullNameByID(obj.getEntryID()), JLabel.CENTER);
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
