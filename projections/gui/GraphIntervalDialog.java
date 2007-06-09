// UNUSED FILE

//package projections.gui;
//
//import java.awt.*;
//import java.awt.event.*;
//
//public class GraphIntervalDialog extends Dialog
//    implements ActionListener, KeyListener, FocusListener
//{
//    private GraphWindow graphWindow;
//
//    private SelectField processorsField;
//
//    // time input panel  (default enabled)
//    private Panel timePanel;
//    private TimeTextField sizeField;
//    private TimeTextField startTimeField;
//    private TimeTextField endTimeField;
//
//    private Panel intervalPanel;
//    private Label numLabel;
//    private Label startLabel;
//    private Label endLabel;
//
//    private Button bOK, bCancel;
//	  
//    // data variables
//    private int numProcs;
//
//    private long intervalSize;
//    private long totalTime;
//    private long startTime;
//    private long endTime;
//    
//    private int numIntervals;
//    private int intervalStart;
//    private int intervalEnd;
//
//    private long extraTime;
//
//    private OrderedIntList processorList;
//    private String processorString;
//   
//    public GraphIntervalDialog(GraphWindow graphWindow, int nI, long iSize, 
//			       int NintStart, int NintEnd,
//			       long NstartTime, long NendTime,
//			       String pStr)
//    {
//	super((Frame)graphWindow, "Set Interval Size", true);
//		
//	numIntervals = nI;
//	intervalSize = iSize;
//	intervalStart = NintStart;
//	intervalEnd = NintEnd;
//	startTime = NstartTime;
//	endTime = NendTime;
//	
//	this.graphWindow = graphWindow;
//	
//	addWindowListener(new WindowAdapter()
//	    {
//		public void windowClosing(WindowEvent e)
//		{
//		    setVisible(false);
//		    dispose();
//		}
//	    });
//	
//	totalTime = Analysis.getTotalTime();
//	numProcs = Analysis.getNumProcessors();
//	
//	// setup the initial interval and size values
//	if (numIntervals <= 0)  {
//	    intervalSize = 1000; // default to milliseconds
//	    numIntervals = getNumIntervals(intervalSize);
//	}
//	
//	// setup the initial start and end values
//	// check to see if jumped here from TimelineWindow
//	if (Analysis.checkJTimeAvailable() == true) {
//	    startTime = Analysis.getJStart();
//	    endTime = Analysis.getJEnd();
//	    intervalStart = (int)(startTime/intervalSize);
//	    intervalEnd = (int)(endTime/intervalSize);
//	    Analysis.setJTimeAvailable(false);
//	}
//	else if (intervalStart <= 0 || intervalEnd <= 0) {
//	    intervalStart = 0;
//	    intervalEnd = numIntervals-1;
//	    startTime = 0;
//	    endTime = totalTime;
//	} 
//	// setup processor string entry (crap gets treated as "0")
//	if (pStr == null) {
//	    processorString = "0-"+Integer.toString(numProcs-1);
//	} else {
//	    processorString = pStr;
//	}
//
//	CreateLayout();
//	pack();
//	setResizable(false);
//    }   
//
//    public void actionPerformed(ActionEvent evt)
//    {
//	if(evt.getSource() instanceof Button) {
//	    Button b = (Button) evt.getSource();
//	  
//	    if(b == bOK) {
//		TextComponent someField = checkConsistent();
//		if (someField != null) {
//		    someField.selectAll();
//		    someField.requestFocus();
//		    return;
//		} else {
//		    graphWindow.setIntervalSize(intervalSize);
//		    graphWindow.setNumIntervals(numIntervals);
//		    graphWindow.setTimes(startTime, endTime);
//		    graphWindow.setIntervalRange(intervalStart,intervalEnd);
//		    processorList = 
//			processorsField.getValue(Analysis.getNumProcessors());
//		    processorString = processorList.listToString();
//		    graphWindow.setProcessorRange(processorList);
//		}
//	    }
//	    setVisible(false);
//	    dispose();
//	} else if (evt.getSource() instanceof TextComponent) {
//	    // perform an update in response to an "Enter" action event
//	    // since the Enter key is typically hit after some keyboard
//	    // input.
//	    updateData((TextComponent)evt.getSource());
//	    bOK.setEnabled(true);
//	}     
//    }   
//
//    public void focusGained(FocusEvent evt) {
//	// do nothing
//    }
//
//    public void focusLost(FocusEvent evt) {
//	// when keyboard focus is lost from a text field, it is assumed
//	// that the user has confirmed the data. Hence, perform an update and
//	// enable the OK button.
//	if (evt.getComponent() instanceof TextComponent) {
//	    updateData((TextComponent)evt.getComponent());
//	    bOK.setEnabled(true);
//	}
//    }
//
//    public void keyPressed(KeyEvent evt) {
//	// do nothing
//    }
//
//    public void keyReleased(KeyEvent evt) {
//	// do nothing
//    }
//
//    /**
//     *  Look for changes in input to text fields.
//     */
//    public void keyTyped(KeyEvent evt) {
//	int keycode = evt.getKeyCode();
//	if (evt.getComponent() instanceof TextComponent) {
//	    TextComponent field = (TextComponent)evt.getComponent();
//	    
//	    switch (keycode) {
//	    case KeyEvent.VK_ENTER:
//		// hitting enter is typical of input confirmation. Update the
//		// data and enable the OK button.
//		updateData(field);
//		bOK.setEnabled(true);
//		break;
//	    case KeyEvent.VK_TAB:
//		// leaving keyboard focus for that field. Update the data and
//		// enable the OK button.
//		updateData(field);
//		bOK.setEnabled(true);
//		break;
//	    default:
//		// any other key assumed to be input. Hence disable OK button.
//		bOK.setEnabled(false);
//		break;
//	    }
//	}
//    }
//
//    private void CreateLayout() {
//
//	processorsField = new SelectField(processorString, 12);
//	processorsField.addActionListener(this);
//
//	Panel inputPanel = new Panel();
//	Panel buttonPanel = new Panel();
//	
//	timePanel = new Panel();
//	sizeField = new TimeTextField(intervalSize, 12);
//	startTimeField = new TimeTextField(startTime, 12);
//	endTimeField = new TimeTextField(endTime, 12);
//
//	sizeField.addActionListener(this);
//	startTimeField.addActionListener(this);
//	endTimeField.addActionListener(this);
//	sizeField.addKeyListener(this);
//	startTimeField.addKeyListener(this);
//	endTimeField.addKeyListener(this);
//	sizeField.addFocusListener(this);
//	startTimeField.addFocusListener(this);
//	endTimeField.addFocusListener(this);
//
//	intervalPanel = new Panel();
//	numLabel = new Label("" + numIntervals, Label.LEFT);
//	startLabel = new Label("" + intervalStart, Label.LEFT);
//	endLabel = new Label("" + intervalEnd, Label.LEFT);
//	
//	GridBagLayout      gbl = new GridBagLayout();
//	GridBagConstraints gbc = new GridBagConstraints();
//	  
//	inputPanel.setLayout(gbl);
//	buttonPanel.setLayout(new FlowLayout());
//	
//	timePanel.setLayout(gbl);
//	intervalPanel.setLayout(gbl);
//
//	gbc.fill = GridBagConstraints.BOTH;
//	gbc.insets = new Insets(5, 5, 5, 5);
//	  
//	// the main parts of the panel
//	Util.gblAdd(inputPanel, 
//		    new Label("Total Time:", Label.RIGHT), gbc, 0,0, 1,1, 1,1);
//	Util.gblAdd(inputPanel, 
//		    new Label(U.t(totalTime),Label.RIGHT), gbc, 1,0, 1,1, 1,1);
//	Util.gblAdd(inputPanel, 
//		    new Label("Total Processors:",Label.RIGHT), 
//		                                           gbc, 0,1, 1,1, 1,1);
//	Util.gblAdd(inputPanel, 
//		    new Label(Integer.toString(numProcs), Label.RIGHT), 
//		                                           gbc, 1,1, 1,1, 1,1);
//	Util.gblAdd(inputPanel, 
//		    new Label("Processors:", Label.RIGHT), gbc, 0,2, 1,1, 1,1);
//	Util.gblAdd(inputPanel, processorsField,           gbc, 1,2, 1,1, 1,1);
//
//	// TIME PANEL
//	Util.gblAdd(timePanel, 
//		    new Label("Interval Size:", Label.RIGHT), 
//   		                                   gbc, 0,0, 1,1, 1,1);
//	Util.gblAdd(timePanel, sizeField,          gbc, 1,0, 1,1, 1,1);
//	Util.gblAdd(timePanel, 
//		    new Label("Start time:", Label.RIGHT), 
//		                                   gbc, 0,1, 1,1, 1,1);
//	Util.gblAdd(timePanel, startTimeField,     gbc, 1,1, 1,1, 1,1);
//	Util.gblAdd(timePanel, 
//		    new Label("End time:", Label.RIGHT), 
//		                                   gbc, 2,1, 1,1, 1,1);
//	Util.gblAdd(timePanel, endTimeField,       gbc, 3,1, 1,1, 1,1);
//
//	Util.gblAdd(inputPanel, timePanel,                 gbc, 0,4, 2,1, 1,1);
//
//	// INTERVAL PANEL
//	Util.gblAdd(intervalPanel, new Label("# of Intervals:", Label.RIGHT), 
//		                                   gbc, 0,0, 1,1, 1,1);
//	Util.gblAdd(intervalPanel, numLabel,       gbc, 1,0, 1,1, 1,1); 
//	Util.gblAdd(intervalPanel, new Label("Start Interval:", Label.RIGHT), 
//		                                   gbc, 0,1, 1,1, 1,1);
//	Util.gblAdd(intervalPanel, startLabel,     gbc, 1,1, 1,1, 1,1); 
//	Util.gblAdd(intervalPanel, new Label("End Interval:", Label.RIGHT), 
//		                                   gbc, 2,1, 1,1, 1,1);
//	Util.gblAdd(intervalPanel, endLabel,       gbc, 3,1, 1,1, 1,1); 
//
//	Util.gblAdd(inputPanel, intervalPanel,             gbc, 0,6, 2,1, 1,1);
//	  
//	bOK     = new Button("OK");
//	bCancel = new Button("Cancel");
//	
//        buttonPanel.add(bOK);
//	buttonPanel.add(bCancel);
//	
//	bOK.addActionListener    (this);
//	bCancel.addActionListener(this);
//	
//	add(inputPanel, "Center");
//	add(buttonPanel, "South" );
//    }
//
//    private int getNumIntervals(long size) {
//	if (size <= 0) {
//	    // nonsensical value, assume 1
//	    size = 1;
//	}
//	return (int)Math.ceil((double)totalTime/size);
//    }
//
//    private int getIntervalIndex(long time) {
//	return (int)(time/intervalSize);
//    }
//
//    /**
//     *  updateData stores the data values from text into the variables.
//     *  and attempts to make the various data fields consistent by
//     *  updating the values of other data items based on the changes of
//     *  the current data item.
//     */
//    private void updateData(TextComponent field) {
//	if (field instanceof TimeTextField) {
//	    // if the field is a time-based field
//	    if (field == sizeField) {
//		// user only allowed to use nice round numbers
//		intervalSize = sizeField.getValue();
//		intervalSize = U.makeEven(intervalSize);
//		sizeField.setValue(intervalSize);
//		numIntervals = getNumIntervals(intervalSize);
//		numLabel.setText("" + numIntervals);
//		intervalStart = getIntervalIndex(startTime);
//		startLabel.setText("" + intervalStart);
//		intervalEnd = getIntervalIndex(endTime);
//		endLabel.setText("" + intervalEnd);
//	    } else if (field == startTimeField) {
//		startTime = startTimeField.getValue();
//		intervalStart = getIntervalIndex(startTime);
//		startLabel.setText("" + intervalStart);
//	    } else if (field == endTimeField) {
//		endTime = endTimeField.getValue();
//		intervalEnd = getIntervalIndex(endTime);
//		endLabel.setText("" + intervalEnd);
//	    }
//	}
//    }
//
//    /**
//     *  checkConsistent verifies the consistency of the various data fields
//     *  and returns any one of the fields that are in violation.
//     */
//    private TextComponent checkConsistent() {
//	
//	// start time cannot be greater or equal to end time
//	if (startTime >= endTime) {
//	    return startTimeField;
//	}
//	// starting time cannot be less than zero
//	if (startTime < 0) {
//	    return startTimeField;
//	}
//	// ending time cannot be greater than total time
//	if (endTime > totalTime) {
//	    return endTimeField;
//	}
//
//	return null;
//    }
//}
