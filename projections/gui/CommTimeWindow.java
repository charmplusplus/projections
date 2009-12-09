package projections.gui;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import projections.analysis.CallGraph;

public class CommTimeWindow extends GenericGraphWindow
    implements ItemListener, ActionListener, ColorSelectable
{

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    // Sent External code commented out and may be implemented later

    CommTimeWindow      thisWindow;    

    private EntrySelectionDialog entryDialog;
        
    private JPanel	   mainPanel;
    IntervalChooserPanel intervalPanel;

	private JPanel	   graphPanel;
    private JPanel	   checkBoxPanel;
    private JPanel         controlPanel;
    
    private JButton	   setRanges;
    private JButton	   epSelection;
    private JButton        saveColors;
    private JButton        loadColors;
    
    CheckboxGroup  cbg;
    private Checkbox	   sentMsgs;
    private Checkbox	   sentBytes;
    private Checkbox	   receivedMsgs;
    private Checkbox	   receivedBytes;
    //private Checkbox	   sentExternalMsgs;
    //private Checkbox	   sentExternalBytes;
    private Checkbox	   receivedExternalMsgs;
    private Checkbox	   receivedExternalBytes;
    
    private int		   startInterval;
    private int		   endInterval;
    private int		   numIntervals;
    private int		   numEPs;
    private long	   intervalSize;
    private OrderedIntList processorList;
    
    private String	   currentArrayName;
        
    // data required for entry selection dialog
    private String         typeLabelNames[] = {"Entry Points"};
    private boolean        stateArray[][];
    private boolean        existsArray[][];
    private Color          colorArray[][];
    private String         entryNames[];
    
    // stored raw data
    private double[][]	   sentMsgCount;
    private double[][]     sentByteCount;
    private double[][]	   receivedMsgCount;
    private double[][]     receivedByteCount;
    //private double[][]     sentExternalMsgCount;
    //private double[][]     sentExternalByteCount;
    private double[][]	   receivedExternalMsgCount;
    private double[][]     receivedExternalByteCount;
    
    // output arrays    
    private Color[]        outColors;
    private double[][]     sentMsgOutput;
    private double[][]     sentByteOutput;
    private double[][]     receivedMsgOutput;
    private double[][]     receivedByteOutput;
    //private double[][]     sentExternalMsgOutput;
    //private double[][]     sentExternalByteOutput;
    private double[][]     receivedExternalMsgOutput;
    private double[][]     receivedExternalByteOutput;

    // flag signifying the tool has just begun
    private boolean	   startFlag;
    
    // format for output
    private DecimalFormat  _format;


    protected CommTimeWindow(MainWindow mainWindow) {
	super("Projections Communication vs Time Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
	setGraphSpecificData();
	// the following data are statically known and can be initialized
	// here
	numEPs = MainWindow.runObject[myRun].getNumUserEntries();
	stateArray = new boolean[1][numEPs];
	existsArray = new boolean[1][numEPs];
	colorArray = new Color[1][numEPs];
	entryNames = new String[numEPs];
	for (int ep=0; ep<numEPs; ep++) {
	    colorArray[0][ep] = MainWindow.runObject[myRun].getEntryColor(ep);
	    entryNames[ep] = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
	}
	mainPanel = new JPanel();
        _format = new DecimalFormat("###,###.###");
	setLayout(mainPanel);
	//getContentPane().add(mainPanel);
	createMenus();
	createLayout();
	pack();
	thisWindow = this;
	startFlag = true;
	
	showDialog();
    }

    protected void createMenus(){
        JMenuBar mbar = new JMenuBar();
        mbar.add(Util.makeJMenu("File", new Object[]
                                {
                                    "Select Processors",
                                    null,
                                    "Close"
                                },
                                this));
	mbar.add(Util.makeJMenu("Tools", new Object[]
	                        {
		                    "Change Colors",
	                        },
                                this));
        mbar.add(Util.makeJMenu("Help", new Object[]
                                {
                                    "Index",
                                    "About"
                                },
                                this));
        setJMenuBar(mbar);
    }

    private void createLayout() {
	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();

	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);

	// checkbox panel items
	cbg = new CheckboxGroup();
	sentMsgs = new Checkbox("Msgs Sent To", cbg, true);
	sentMsgs.addItemListener(this);
	sentBytes = new Checkbox("Bytes Sent To", cbg, false);
	sentBytes.addItemListener(this);
	receivedMsgs = new Checkbox("Msgs Recv By", cbg, false);
	receivedMsgs.addItemListener(this);
	receivedBytes = new Checkbox("Bytes Recv By", cbg, false);
	receivedBytes.addItemListener(this);
	//sentExternalMsgs = new Checkbox("External Msgs Sent To", cbg, false);
	//sentExternalMsgs.addItemListener(this);
	//sentExternalBytes = new Checkbox("External Bytes Sent To", cbg, false);
	//sentExternalBytes.addItemListener(this);
	receivedExternalMsgs = new Checkbox("External Msgs Recv By", cbg, false);
	receivedExternalMsgs.addItemListener(this);
	receivedExternalBytes = new Checkbox("External Bytes Recv By", cbg, false);
	receivedExternalBytes.addItemListener(this);
	checkBoxPanel = new JPanel();
	Util.gblAdd(checkBoxPanel, sentMsgs, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, sentBytes, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedMsgs, gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedBytes, gbc, 3,0, 1,1, 1,1);
	//Util.gblAdd(checkBoxPanel2, sentExternalMsgs, gbc, 0,0, 1,1, 1,1);
	//Util.gblAdd(checkBoxPanel2, sentExternalBytes, gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedExternalMsgs, gbc, 4,0, 1,1, 1,1);
	Util.gblAdd(checkBoxPanel, receivedExternalBytes, gbc, 5,0, 1,1, 1,1);
	
	// control panel items
	setRanges = new JButton("Select New Range");
	setRanges.addActionListener(this);
	epSelection = new JButton("Select Entry Points");
	epSelection.addActionListener(this);
	saveColors = new JButton("Save Entry Colors");
	saveColors.addActionListener(this);
	loadColors = new JButton("Load Entry Colors");
	loadColors.addActionListener(this);
	controlPanel = new JPanel();
	controlPanel.setLayout(gbl);
	Util.gblAdd(controlPanel, epSelection, gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, setRanges,   gbc, 1,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, saveColors,  gbc, 2,0, 1,1, 0,0);
	Util.gblAdd(controlPanel, loadColors,  gbc, 3,0, 1,1, 0,0);	
	
	graphPanel = getMainPanel();
	Util.gblAdd(mainPanel, graphPanel,     gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(mainPanel, checkBoxPanel,  gbc, 0,2, 1,1, 0,0);
	Util.gblAdd(mainPanel, controlPanel,   gbc, 0,3, 1,0, 0,0);
    }

    public void itemStateChanged(ItemEvent ae){
	if(ae.getSource() instanceof Checkbox){
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));
	    Checkbox cb = (Checkbox)ae.getSource();
	    setCheckboxData(cb);
	    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
    }
    
    public void setCheckboxData(Checkbox cb) {
	if(cb == sentMsgs) {
	    setDataSource("Communication vs Time", sentMsgOutput, 
			  outColors, this);
	    setPopupText("sentMsgCount");
	    setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Sent", "");
	    super.refreshGraph();
	}
	else if(cb == sentBytes){
	    setDataSource("Communication vs Time", sentByteOutput, 
			  outColors, this);
	    setPopupText("sentByteCount");
	    setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Sent", "");
	    super.refreshGraph();
	}
	else if(cb == receivedMsgs){
	    setDataSource("Communication vs Time", receivedMsgOutput, 
			  outColors, this);
	    setPopupText("receivedMsgCount");
	    setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Received", "");
	    super.refreshGraph();
	}
	else if(cb == receivedBytes){
	    setDataSource("Communication vs Time", receivedByteOutput, 
			  outColors, this);
	    setPopupText("receivedByteCount");
	    setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Received", "");
	    super.refreshGraph();
	}
/*
	else if(cb == sentExternalMsgs) {
	    setDataSource("Call Graph", sentExternalMsgOutput, outColors, this);
	    setPopupText("sentExternalMsgCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Sent Externally", "");
	    super.refreshGraph();
	}
	else if(cb == sentExternalBytes){
	    setDataSource("Call Graph", sentExternalByteOutput, outColors, this);
	    setPopupText("sentExternalByteCount");
	    setXAxis("Time Interval (" + U.t(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Sent Externally", "");
	    super.refreshGraph();
        }
*/
	else if(cb == receivedExternalMsgs){
	    setDataSource("Communication vs Time", receivedExternalMsgOutput,
			  outColors, this);
	    setPopupText("receivedExternalMsgCount");
	    setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Messages Received Externally", "");
	    super.refreshGraph();
	}
	else if(cb == receivedExternalBytes){
	    setDataSource("Communication vs Time", receivedExternalByteOutput,
			  outColors, this);
	    setPopupText("receivedExternalByteCount");
	    setXAxis("Time Interval (" + U.humanReadableString(intervalSize) + ")", "",
		     startInterval, 1.0);
	    setYAxis("Bytes Received Externally", "");
	    super.refreshGraph();
	}
    }
    
    
    protected void setGraphSpecificData(){
	setXAxis("Time", "");
	setYAxis("Count", "");
    }
    
    public void showDialog() {
	if (dialog == null) {    
		intervalPanel = new IntervalChooserPanel(1000);    	// default 1ms interval width
		dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
	}
	
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
		intervalSize = intervalPanel.getIntervalSize();
    	startInterval = (int)intervalPanel.getStartInterval();
    	endInterval = (int)intervalPanel.getEndInterval();
    	numIntervals = endInterval-startInterval+1;
    	processorList = dialog.getSelectedProcessors();
    	
		final SwingWorker worker =  new SwingWorker() {
			public Object doInBackground() {
				fillGraphData();
				return null;
			}
			public void done() {
				setOutputGraphData();
				Checkbox cb = cbg.getSelectedCheckbox();
				setCheckboxData(cb);
				thisWindow.setVisible(true);
				thisWindow.repaint();
			}
		};
		worker.execute();
	}
    }

    
    private void fillGraphData() {
    	// Utilize CallGraph.java in analysis folder
 	CallGraph cg = new CallGraph(startInterval, endInterval, 
	                             intervalSize, processorList);
	cg.GatherData(this);
	
	// Arrays returned are implemented such that first half of
	//   the array shows sent and second half shows received 
	// Size = [numIntervals][2*numEPs]
	double[][] messageArray = cg.getMessageArray();
	double[][] byteArray = cg.getByteArray();
	double[][] externalMessageArray = cg.getExternalMessageArray();
	double[][] externalByteArray = cg.getExternalByteArray();

        sentMsgCount = new double[numIntervals][numEPs];
	sentByteCount = new double[numIntervals][numEPs];
	receivedMsgCount = new double[numIntervals][numEPs];
	receivedByteCount = new double[numIntervals][numEPs];
        //sentExternalMsgCount = new double[numIntervals][numEPs];
        //sentExternalByteCount = new double[numIntervals][numEPs];
	receivedExternalMsgCount = new double[numIntervals][numEPs];
	receivedExternalByteCount = new double[numIntervals][numEPs];
	for (int interval=0; interval<numIntervals; interval++) {
	    for (int ep=0; ep<numEPs; ep++) {
                sentMsgCount[interval][ep] = messageArray[interval][ep];
		sentByteCount[interval][ep] = byteArray[interval][ep];
		receivedMsgCount[interval][ep] = messageArray[interval][ep+numEPs];
		receivedByteCount[interval][ep] = byteArray[interval][ep+numEPs];
                //sentExternalMsgCount[interval][ep] = externalMessageArray[interval][ep];
		//sentExternalByteCount[interval][ep] = externalByteArray[interval][ep];
		receivedExternalMsgCount[interval][ep] = externalMessageArray[interval][ep+numEPs];
		receivedExternalByteCount[interval][ep] = externalByteArray[interval][ep+numEPs];
	    }
        }
        
	// Set the exists array to accept non-zero entries only
        // Have initial state also display all existing data.
	// Only do this once at the beginning
	if (startFlag) {
	    for (int ep=0; ep<numEPs; ep++) {
	        for (int interval=0; interval<numIntervals; interval++) {
		    if ( (messageArray[interval][ep]>0) || (messageArray[interval][ep+numEPs]>0) ) {
		        existsArray[0][ep] = true;
		        stateArray[0][ep] = true;
		        break;
		    }
	        }
	    } 
	    startFlag = false;
	}
    }

    public void applyDialogColors() {
	setOutputGraphData();
	repaint();
    }    
    
    private void setOutputGraphData() {
	// need first pass to decide the size of the outputdata
	int outSize = 0;
	for (int ep=0; ep<numEPs; ep++) {
	    if (stateArray[0][ep]) {
		outSize++;
	    }
	}
	if (outSize == 0) {
	    // do nothing, just display empty graph
	}
	else {
	    // actually create and fill the data and color arrays
	    sentMsgOutput = 
	        new double[numIntervals][outSize];
	    sentByteOutput =
	        new double[numIntervals][outSize];
	    receivedMsgOutput = 
	        new double[numIntervals][outSize];
	    receivedByteOutput =
	        new double[numIntervals][outSize];
/*
	    sentExternalMsgOutput = 
	        new double[numIntervals][outSize];
	    sentExternalByteOutput =
	        new double[numIntervals][outSize];
*/
	    receivedExternalMsgOutput = 
	        new double[numIntervals][outSize];
	    receivedExternalByteOutput =
	        new double[numIntervals][outSize];
	    outColors =
		new Color[outSize];
	    int count=0;
	    for (int ep=0; ep<numEPs; ep++) {
	        if (stateArray[0][ep]) {
	    	    for (int interval=0; interval<numIntervals; interval++) {
			sentMsgOutput[interval][count] = sentMsgCount[interval][ep];
			sentByteOutput[interval][count] = sentByteCount[interval][ep];
			receivedMsgOutput[interval][count] = receivedMsgCount[interval][ep];
			receivedByteOutput[interval][count] = receivedByteCount[interval][ep];
			//sentExternalMsgOutput[interval][count] = sentExternalMsgCount[interval][ep];
			//sentExternalByteOutput[interval][count] = sentExternalByteCount[interval][ep];
			receivedExternalMsgOutput[interval][count] = receivedExternalMsgCount[interval][ep];
			receivedExternalByteOutput[interval][count] = receivedExternalByteCount[interval][ep];
		    }
		    outColors[count++] = colorArray[0][ep];
		}
	    }
	}
    }
    
    public String[] getPopup(int xVal, int yVal) {
	//System.out.println("CommWindow.getPopup()");
	//System.out.println(xVal +", " +yVal);
	if( (xVal < 0) || (yVal <0) || currentArrayName==null)
	    return null;

	// find the ep corresponding to the yVal
	int count = 0;
	String epName = "";
	String epClassName = "";
	for (int ep=0; ep<numEPs; ep++) {
	    if (stateArray[0][ep]) {
	    	if (count++ == yVal) {
	    		epName = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
	    		epClassName = MainWindow.runObject[myRun].getEntryChareNameByIndex(ep);
	    		break;
	    	}
	    }
	}

	String[] rString = new String[4];
	
        rString[0] = "Time Interval: " +
            U.humanReadableString((xVal+startInterval)*intervalSize) + " to " +
            U.humanReadableString((xVal+startInterval+1)*intervalSize);

	if (currentArrayName.equals("sentMsgCount")) {
	    rString[1] = "Dest. Chare: " + epClassName;
	    rString[2] = "Dest. EPid: " + epName;
	    rString[3] = "Count = " + 
		_format.format(sentMsgOutput[xVal][yVal]);    	
	}
	else if(currentArrayName.equals("sentByteCount")) {
	    rString[1] = "Dest. Chare: " + epClassName;
	    rString[2] = "Dest. EPid: " + epName;	    
	    rString[3] = "Bytes = " + 
		_format.format(sentByteOutput[xVal][yVal]);
	}
	else if(currentArrayName.equals("receivedMsgCount")) {
	    rString[1] = "Dest. Chare: " + epClassName;
	    rString[2] = "Dest. EPid: " + epName;	    
	    rString[3] = "Count = " + 
		_format.format(receivedMsgOutput[xVal][yVal]);
	}
	else if(currentArrayName.equals("receivedByteCount")) {
	    rString[1] = "Dest. Chare: " + epClassName;
	    rString[2] = "Dest. EPid: " + epName;	    
	    rString[3] = "Bytes = " + 
		_format.format(receivedByteOutput[xVal][yVal]);
	}
/*
	else if (currentArrayName.equals("sentExternalMsgCount")) {
	    rString[1] = "Chare: " + epClassName;
	    rString[2] = "Destination EP: " + epName;
	    rString[3] = "Messages Sent Externally: " + 
	        sentExternalMsgOutput[xVal][yVal];    	
	}
	else if(currentArrayName.equals("sentExternalByteCount")) {
	    rString[1] = "Chare: " + epClassName;
	    rString[2] = "Destination EP: " + epName;	    
	    rString[3] = "Bytes Sent Externally: " + 
                sentExternalByteOutput[xVal][yVal];
	}
*/
	else if(currentArrayName.equals("receivedExternalMsgCount")) {
	    rString[1] = "Dest. Chare: " + epClassName;
	    rString[2] = "Dest. EPid: " + epName;	    
	    rString[3] = "Count = " + 
		_format.format(receivedExternalMsgOutput[xVal][yVal]);
	}
	else if(currentArrayName.equals("receivedExternalByteCount")) {
	    rString[1] = "Dest. Chare: " + epClassName;
	    rString[2] = "Dest. EPid: " + epName;	    
	    rString[3] = "Bytes = " + 
		_format.format(receivedExternalByteOutput[xVal][yVal]);
	}
	return rString;
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
	    JButton b = (JButton)e.getSource();
	    if (b == epSelection) {
		if (entryDialog == null) {
		    entryDialog = 
			new EntrySelectionDialog(this,
						 typeLabelNames,
						 stateArray,colorArray,
						 existsArray,entryNames);
		}
		entryDialog.showDialog();
		setOutputGraphData();
		Checkbox cb = cbg.getSelectedCheckbox();
		setCheckboxData(cb);
	    } 
	    else if (b == setRanges) {
		showDialog();
	    }
	    else if (b == saveColors) {
		// save all entry point colors to disk
		MainWindow.runObject[myRun].saveColors();
	    }
	    else if (b == loadColors) {
		// load all entry point colors from disk
		try {
		    ColorManager.loadActivityColors(Analysis.PROJECTIONS, colorArray[0]);
		    // silly inefficiency
		    setOutputGraphData();
		} 
		catch (IOException exception) {
		    System.err.println("Failed to load colors!!");
		}
	    }
	} else if (e.getSource() instanceof JMenuItem) {
	    String arg = ((JMenuItem)e.getSource()).getText();
	    if (arg.equals("Close")) {
		close();
	    } else if(arg.equals("Select Processors")) {
		showDialog();
	    }
	}
    }
    
    public void repaint() {
	super.refreshGraph();
    }
    
    private void setPopupText(String input){
	currentArrayName = input;
    }
}
