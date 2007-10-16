package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;

import projections.misc.*;

public class GraphWindow extends ProjectionsWindow
    implements ActionListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    GraphDisplayPanel      displayPanel;
    GraphControlPanel      controlPanel;
    GraphLegendPanel       legendPanel;
    GraphData data;

    GraphWindow thisWindow;

    int intervalStart;
    int intervalEnd;
    private long endTime;

    long intervalsize;
    OrderedIntList processorList;
    public static boolean dumpNow = false;
    public static int dumpCount = 0;
    
    public GraphWindow(MainWindow parentWindow, Integer myWindowID)
    {
	super(parentWindow, myWindowID);

	thisWindow = this;
	setBackground(Color.lightGray);
	
	createLayout();
	createMenus();
	pack();
	  
	setTitle("Projections Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts");
	showDialog();
    }   

    public void refreshDisplay() {
	if (displayPanel != null) {
	    displayPanel.refreshDisplay();
	}
	if (legendPanel != null) {
	    legendPanel.repaint();
	}
    }

    void windowInit() {
	endTime = MainWindow.runObject[myRun].getTotalTime();

	intervalsize = 1000; // default to 1ms
	intervalStart = 0;
	if (endTime%intervalsize == 0) {
	    intervalEnd = (int)(endTime/intervalsize - 1);
	} else {
	    intervalEnd = (int)(endTime/intervalsize);
	}
	processorList = MainWindow.runObject[myRun].getValidProcessorList();
    }

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof MenuItem) {
	    MenuItem mi = (MenuItem)evt.getSource();
	    String arg = mi.getLabel();
	    if(arg.equals("Close"))
		close();
	    else if(arg.equals("Print Graph"))
		PrintGraph();   
	    else if(arg.equals("Set Interval Size"))
		showDialog();
	    else if(arg.equals("Dump Raw Data")) {
		try {
		    File dumpFile = new File("GraphDump."+dumpCount+".out");
		    while (dumpFile.exists()) {
			dumpCount++;
			dumpFile = 
			    new File("GraphDump."+dumpCount+".out");
		    }
		    MainWindow.dataDump = 
			new PrintWriter(new FileWriter(dumpFile));
		    dumpNow = true;
		    refreshDisplay();
		} catch (IOException e) {
		    System.err.println("Failure to handle dump data " +
				       "GraphDump."+dumpCount+".out");
		    System.exit(-1);
		}
	    }
	}
    }   

    private void createLayout()
    {   
	Panel p = new Panel();
	getContentPane().add("Center", p);
	p.setBackground(Color.gray);
	  
	displayPanel = new GraphDisplayPanel();
	controlPanel = new GraphControlPanel();
	legendPanel  = new GraphLegendPanel (this);
	  
	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	  
	p.setLayout(gbl);
	
	gbc.fill   = GridBagConstraints.BOTH;
	Util.gblAdd(p, displayPanel, gbc, 0,0, 1,1, 2,1, 2,2,2,2);
	Util.gblAdd(p, legendPanel,  gbc, 1,0, 1,1, 1,1, 2,2,2,2);
	Util.gblAdd(p, controlPanel, gbc, 0,1, 2,1, 1,0, 2,2,2,2);
    }   

    private void createMenus()
    {
	MenuBar mbar = new MenuBar();
	
	mbar.add(Util.makeMenu("File", new Object[]
	    {
		"Print Graph",
		null,
		"Close"
	    },
			       this));                   
	mbar.add(Util.makeMenu("Tools", new Object[]
	    {
		"Set Interval Size",
		"Dump Raw Data",
		"Timeline"
	    },
			       this));
	Menu helpMenu = new Menu("Help");
	mbar.add(Util.makeMenu(helpMenu, new Object[]
	    {
		"Index",
		 "About"
	    },
			       this)); 
	mbar.setHelpMenu(helpMenu);
	setMenuBar(mbar);                                                     
    }   

    public Color getGraphColor(int e)
    {
	if(data != null && data.userEntry != null)
	    return data.userEntry[e][0].color;
	else
	    return null;
    }   

    public long getIntervalSize()
    {
	return intervalsize;
    }
   
    private void PrintGraph()
    {
       PrinterJob pjob = PrinterJob.getPrinterJob();
       PageFormat format = new PageFormat();
       format = pjob.pageDialog(format);
       pjob.setPrintable(new PrintUtils(displayPanel), format);
       if (pjob.printDialog() == false) {
	   // user cancelled.
	   return;
       }
       try {
	   pjob.print();
       } catch (PrinterException e) {
	   System.err.println("Printer failure.");
       }
   }   

    void setChildDatas()
    {
	controlPanel.setGraphData(data);
	displayPanel.setGraphData(data);
	legendPanel.setGraphData(data);
	
	data.graphWindow  = this;
	data.controlPanel = controlPanel;
	data.displayPanel = displayPanel;
	data.legendPanel  = legendPanel;
    }   

    public void showDialog()
    {
	if (dialog == null) {
	    dialog = new IntervalRangeDialog(this, "Graph Window");
	} else {
	    // we are guaranteed a set of parameters stored in dialog.
	    setDialogData();
	}
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
	    getDialogData();
	    if (dialog.isModified()) {
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
			    MainWindow.runObject[myRun].LoadGraphData(intervalsize, 
						   intervalStart, intervalEnd,
						   true, processorList);
			    // got rid of the old optimization that new data 
			    // is only created if the start and end points of 
			    // the range is modified. This will, of course, 
			    // have to be restored, but in a far more elegant 
			    // way than currently allowed.
			    data = new GraphData(intervalsize, 
						 intervalStart, intervalEnd,
						 processorList);
			    return null;
			}
			public void finished() {
			    thisWindow.setChildDatas();
			    /* also need to close and free legendPanel */
			    if (legendPanel!=null) 
				legendPanel.closeAttributesWindow();
			    
			    controlPanel.setXMode(data.xmode);
			    controlPanel.setYMode(data.ymode);
			    
			    legendPanel.UpdateLegend();
			    displayPanel.setAllBounds();
			    displayPanel.UpdateDisplay(); 
			    
			    thisWindow.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			    thisWindow.setVisible(true);
			}
		    };
		worker.start();
	    } else {
		setVisible(true);
	    }
	} else {
	    return;
	}
    }
    
    public void showWindow() {
	// do nothing for now
    }
    
    public void getDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	intervalsize = dialog.getIntervalSize();
	intervalStart = (int)dialog.getStartInterval();
	intervalEnd = (int)dialog.getEndInterval();
	processorList = dialog.getValidProcessors();
    }

    public void setDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	dialog.setIntervalSize(intervalsize);
	dialog.setValidProcessors(processorList);
	super.setDialogData();
    }
}
