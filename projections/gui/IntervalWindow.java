package projections.gui;

import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/* Aim is to record the number of sends in the given interval range
*  check for CREATION event and increment corresponding begin time  
*  At the same time record the entrypoints that exceed the threshold 
*  execution time in the corresponing beginTime.
*  It also has the ability to show the message length (in bytes) of 
*  the send events
*  along with the stretched eps.  
*/

public class IntervalWindow extends GenericGraphWindow
    implements ActionListener,ItemListener{
    
    // parameter variables in addition to superclass
    public long thresholdTime;	// to record EPs that cross this time
    public long intervalSize;
    
    private double [][] dataSource;	// to store the data
    private boolean countSends = true;	// default: draw Sends vs Eps graph
    private boolean getNewData = false;	// default: refresh graph is false 
    
					// if false, refresh graph just sets the graph to a new data source
    // constants 
    private final int NO_OF_DATASOURCES = 3;
    private final int STRETCHED_EP      = 0;
    private final int SEND_COUNT        = 1;
    private final int MSG_LEN_COUNT     = 2;

    void windowInit() {
	intervalSize = 1000; // 1 ms default
	super.windowInit();
    }

    public IntervalWindow(MainWindow mainWindow, Integer myWindowID)
    {
	super("Projections Interval Graph", mainWindow, myWindowID);
	
	setGraphSpecificData();
	createMenus();
	// no special content to add, so just add the main panel from
	// GenericGraphWindow
	getContentPane().add(getMainPanel());
	
	pack();
	showDialog();
	setVisible(true);
	refreshGraph();
    }
    
    /* Show the RangeDialog to set processor numbers and interval times */
    void showDialog()
    {
        if (dialog == null)
	    dialog = new IntervalRangeDialog(this,"Select Range");
	
        dialog.displayDialog();
        if (!dialog.isCancelled()) {
	    // Range has been changed, so get new data while refreshing
	    getDialogData();
	    getNewData = true;
	    refreshGraph();
	}
    }

    protected void getDialogData() {
	// cast it to the appropriate dialog type
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;

	// getting intervalRangeDialog data. The rest can be gotten
	// by reusing GenericGraphWindow's method.
	intervalSize = dialog.getIntervalSize();
	super.getDialogData();
    }

    protected void showWindow() {
	// nothing for now. It will eventually have to show the window
	// (by bypassing the dialog phase).
    }

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof JMenuItem) {
	    JMenuItem m = (JMenuItem)evt.getSource();
	    if(m.getText().equals("Set Range")) {
		showDialog(); 
	    } else if (m.getText().equals("Send Count vs " + 
					  "Stretched EntryPoints")) {
		countSends = (m.isSelected())?true:false;
		// no need to get new data while refreshing
		getNewData = false;
		refreshGraph();
	    } else if(m.getText().equals("Bytes Sent vs Stretched " + 
					 "EntryPoints")) {
		countSends = (m.isSelected())?false:true;	
		// no need to get new data while refreshing
		getNewData = false;
		refreshGraph();
	    } else if(m.getText().equals("Close")) {
		close();
	    }
	}
    }

    public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        String s = "Item event detected."
                   + " \n   Event source: " + source.getText()
                   + "\n    New state: "
                   + ((e.getStateChange() == ItemEvent.SELECTED) ?
                     "selected":"unselected");
        System.out.println(s);
    }

    public String[] getPopup(int xVal, int yVal) {
	return null;
    }

    /* create the menu bar */
    protected void createMenus()
    {
	JRadioButtonMenuItem countGraphButton = 
	    new JRadioButtonMenuItem("Send Count vs Stretched EntryPoints",
				     true);
	countGraphButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, 
							       ActionEvent.ALT_MASK));
	JRadioButtonMenuItem byteGraphButton = 
	    new JRadioButtonMenuItem("Bytes Sent vs Stretched EntryPoints",
				     false);	
	byteGraphButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, 
							      ActionEvent.ALT_MASK));
	ButtonGroup graphChoiceButton = new ButtonGroup();
	graphChoiceButton.add(countGraphButton);
	graphChoiceButton.add(byteGraphButton);
	
	JMenuBar mbar = new JMenuBar();
	setJMenuBar(mbar);
	
	mbar.add(Util.makeJMenu("File", 
				new Object[]
	    {
		"Set Range",
		"Close"
	    },
				null,
				this));
	mbar.add(Util.makeJMenu("Options",
				new Object[]
	    {
		countGraphButton,
		byteGraphButton	
	    },
				null,
				this)); 
    }

    protected void setGraphSpecificData(){
	setXAxis("Time Interval","");
	setYAxis("#","");		
    }
    
    protected void refreshGraph()
    {	
	// get new data only if either range or entrypoint changes. 
	// not when a change is made btw "sends vs Eps" or "bytes vs Eps"
	if(getNewData)
	    getNewData();
	
	if (dataSource != null) {
	    // if countSends is true, draw the "send vs Eps" graph, 
	    // else draw "bytes vs Eps"
	    int mode;
	    String titleString;
	    if (countSends) {
		titleString = "Stretched EntryPoints vs Send Messages";
		mode  	    = SEND_COUNT;	
	    } else {
		titleString = "Stretched EntryPoints vs Bytes Sent";
		mode 	    = MSG_LEN_COUNT;
	    }
	    
	    // show two values at a time  
	    double[][] data = new double[dataSource.length][2];
	    for (int i=0; i<dataSource.length; i++) {
		data[i][0] = dataSource[i][STRETCHED_EP];
		data[i][1] = dataSource[i][mode];
	    }
	    
	    // set values and draw the graph
	    double multiplier = 
		// in us
		(double)(endTime-startTime)/(dataSource.length-1);
	    setXAxis("Time Interval ("+U.t(startTime)+" - "+U.t(endTime) + ")",
		     "ms",startTime/1000,multiplier/1000); // to display in ms
	    setDataSource(titleString,data);
	    super.refreshGraph();
	}	
    }

    private void getNewData()
    {
        OrderedIntList tmpPEs = validPEs.copyOf();
        GenericLogReader logReader;
	
	LogEntryData logData,prevLogData;
        logData = new LogEntryData();
        prevLogData = new LogEntryData();
	
	// number of x-axis points
	int no_of_bins = (int)((endTime - startTime)/intervalSize)+1;
	
	dataSource = new double[no_of_bins][NO_OF_DATASOURCES];
	
	for(int i=0; i<no_of_bins; i++)
	    for(int j=0; j<NO_OF_DATASOURCES; j++)
		dataSource[i][j] = 0;
	
        while (tmpPEs.hasMoreElements()) {
	    int pe = tmpPEs.nextElement();
	    int binValue;
	    logReader = 
		new GenericLogReader(Analysis.getLogName(pe),
				     Analysis.getVersion());
	    try {
                logReader.nextEventOnOrAfter(startTime,logData);
                while(true) {
		    logReader.nextEvent(logData);	// find the next event
		    // this might not count the last EP. is it ok??
		    if(logData.time > endTime)
			break;
		    switch(logData.type) {
		    case ProjDefs.CREATION:		// send Event
			binValue = 
			    (int)((logData.time-startTime)/intervalSize);
			// increment corresponding value
			dataSource[binValue][SEND_COUNT]++;
			// increment the number of bytes sent
			dataSource[binValue][MSG_LEN_COUNT] += 
			    logData.msglen;	
			break;
		    case ProjDefs.BEGIN_PROCESSING:  
			// found the beginning of an entrypoint execution
			// store the data
			prevLogData = logData.copyOf();
			break;
		    case ProjDefs.END_PROCESSING:
			// found the ending of an entrypoint execution
			long executionTime = logData.time-prevLogData.time; 
			if (executionTime > thresholdTime) {
			    binValue = 
				(int)((prevLogData.time - startTime)/
				      intervalSize);
			    // increment corresponding value
			    dataSource[binValue][STRETCHED_EP]++;
			}
			break;
		    }
                }
	    } catch(EOFException e) {
                // do nothing just reached end-of-file
	    } catch(IOException e) {
                System.out.println("Exception at IntervalWindow::getData() " +
				   e);
	    }
	}
    }
}

	

