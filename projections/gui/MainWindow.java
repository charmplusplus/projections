package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

//import projections.gui.count.*;

import projections.analysis.*;
import projections.gui.graph.*;
import projections.misc.*;

public class MainWindow extends JFrame
    implements ActionListener, ScalePanel.StatusDisplay
{
    protected static final int NUM_WINDOWS = 15;

    protected static final int GRAPH_WIN = 0;
    protected static final int MULTI_WIN = 1;
    protected static final int PROFILE_WIN = 2;
    protected static final int TIME_PROF_WIN = 3;
    protected static final int COMM_WIN = 4;
    protected static final int COMM_TIME_WIN = 5;
    protected static final int CALL_TABLE_WIN = 6;
    protected static final int ANIMATION_WIN = 7;
    protected static final int LOGVIEW_WIN = 8;
    protected static final int HIST_WIN = 9;
    protected static final int TIMELINE_WIN = 10;
    protected static final int OVERVIEW_WIN = 11;
    protected static final int FUNCTION_WIN = 12;
    protected static final int POSE_WIN = 13;
    protected static final int USER_EVENTS_WIN = 14;

    private static final int DEFAULT_NUM_RUNS = 1;

    // Runtime flags -
    // **CW** Note: These are now tentatively mirrored from ProjMain
    // until the new interface can be cleanly handled by other tools.
    public static double CUR_VERSION;
    public static boolean IGNORE_IDLE;
    public static boolean BLUEGENE;
    public static int BLUEGENE_SIZE[];
    public static boolean PRINT_USAGE;

    // **CW** a semi-permanent hack to provide a file onto which raw data
    // dumps may be written for further processing by other graphing tools.
    public static PrintWriter dataDump = null;

    // for SwingWorker to work
    private MainWindow thisWindow;

    // Indexed by number of runs (currently one) and tools available
    // This should eventually be configurable for multiple runs,
    // multiple languages with multiple user-level visualization tools.
    protected JFrame childWindows[][];
    // Indexed by tools available
    protected String toolDescriptions[];
    // Indexed by tools available on both dimensions
    private boolean crossToolMask[][];

    // should be tables of objects dependent indexed by runs in the future.
    private GraphWindow          graphWindow;
    private TimelineWindow       timelineWindow;
    private ProfileWindow        profileWindow;
    private CommWindow           commWindow;
    private CommTimeWindow       commTimeWindow;
    private CallTableWindow	 callTableWindow;
    private HelpWindow           helpWindow;
    private LogFileViewerWindow  logFileViewerWindow;
    private HistogramWindow      histogramWindow;
    private StlWindow            stlWindow;
    private MultiRunWindow       multiRunWindow;
    private AnimationWindow      animationWindow;
    private TimeProfileWindow    timeProfileWindow;
    // FunctionTool is still very much temporary despite first-class treatment
    private FunctionTool         functionWindow;
    // This is a tool specifically activated by the presence of additional
    // POSE data. Could form the basis for generalization to a flexible 
    // implementation of the projector multi-lingual analysis idea.
    private PoseAnalysisWindow   poseAnalysisWindow;

    // components associated with the main window
    private MainTitlePanel        titlePanel;
    private BackGroundImagePanel  background;
    private MainMenuManager       menuManager;
    private MainSummaryGraphPanel summaryGraphPanel;
    private MainRunStatusPanel    runStatusPanel;

    // these should become arrays for future tabbed multirun functionality.
    private SummaryDataSource    sumDataSource;
    private SummaryXAxis         sumXAxis;
    private SummaryYAxis         sumYAxis;
    private GraphPanel           graphPanel;
    private ScalePanel           scalePanel;
    private StlPanel             stl;
    private ScaleSlider          hor, ver;
    private Label                status;
    private ScalePanel.StatusDisplay sd;

    private Image bgimage;
    private GridBagConstraints gbc;
    private GridBagLayout gbl;   

    public MainWindow()
    {
	thisWindow = this;

	// Get information from ProjMain
	CUR_VERSION = ProjMain.CUR_VERSION;
	IGNORE_IDLE = ProjMain.IGNORE_IDLE;
	BLUEGENE = ProjMain.BLUEGENE;
	BLUEGENE_SIZE = ProjMain.BLUEGENE_SIZE;
	PRINT_USAGE = ProjMain.PRINT_USAGE;

	// static screen information.
	ScreenInfo.init();

	addWindowListener(new WindowAdapter()
	    {
		public void windowClosing(WindowEvent e)
		{
		    System.exit(0);
		}
	    });

	setBackground(Color.lightGray);

	childWindows = new JFrame[NUM_WINDOWS][DEFAULT_NUM_RUNS];
	initializeTools();

	menuManager = new MainMenuManager(this);
	createLayout();
    }

    public void actionPerformed(ActionEvent evt)
    {
    }

    /**
     *  Set up the tool descriptions and cross tool masks for this
     *  particular run. It is intended to be flexible enough in the
     *  future for user-defined tools to be dynamically added to
     *  projections (in the form of registration calls).
     */
    private void initializeTools() {

	// give the default tools descriptive names
	toolDescriptions = new String[NUM_WINDOWS];
	toolDescriptions[GRAPH_WIN] = "Graph";
	toolDescriptions[MULTI_WIN] = "Multirun";
	toolDescriptions[PROFILE_WIN] = "Usage Profile";
	toolDescriptions[COMM_WIN] = "Communication";
	toolDescriptions[COMM_TIME_WIN] = "Communication vs Time";
	toolDescriptions[CALL_TABLE_WIN] = "Call Table";
	toolDescriptions[LOGVIEW_WIN] = "View Logs";
	toolDescriptions[HIST_WIN] = "Histograms";
	toolDescriptions[ANIMATION_WIN] = "Animation";
	toolDescriptions[TIMELINE_WIN] = "Timeline";
	toolDescriptions[OVERVIEW_WIN] = "Overview";
	toolDescriptions[TIME_PROF_WIN] = "Time Profile Graph";
	toolDescriptions[FUNCTION_WIN] = "AMPI Functions";

	// cross-tool masks allow tools to decide if their parameter sets
	// are compatible and hence may "cross over" from one tool to the
	// next with the same parameters used.
	crossToolMask = new boolean[NUM_WINDOWS][NUM_WINDOWS];

    }

    private void createLayout()
    {
	try {
	    URL imageURL = ((Object)this).getClass().getResource("/projections/images/bgimage");
	    bgimage = Toolkit.getDefaultToolkit().getImage(imageURL);
	    // mainPanel is used to draw the wall paper and serves as the
	    // MainWindow's contentPane.
	    background = new BackGroundImagePanel(bgimage, true);
	} catch (Exception E) {
	    System.out.println("Error loading background image.  Continuing.");
	    background = new BackGroundImagePanel(null);
	}

	setContentPane(background);

	gbl = new GridBagLayout();
	gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	background.setLayout(gbl);

	titlePanel  = new MainTitlePanel(this);
	runStatusPanel = new MainRunStatusPanel();
	summaryGraphPanel = new MainSummaryGraphPanel(this, runStatusPanel);

	Util.gblAdd(background, titlePanel,
		    gbc, 0,0, 1,1, 1,0, 0,0,0,0);
	Util.gblAdd(background, runStatusPanel,
		    gbc, 0,1, 1,1, 1,0, 0,20,0,20);
	Util.gblAdd(background, summaryGraphPanel,
		    gbc, 0,2, 1,1, 1,1, 0,20,20,20);

	background.setPreferredSize(new Dimension(ScreenInfo.screenWidth,
						  ScreenInfo.screenHeight));
	pack();
    }

    // get child window name by number
    public String getChildName(int index) {
	if (index == GRAPH_WIN) {
	    return "GraphWindow";
	} else if (index == MULTI_WIN) {
	    return "MultiRunWindow";
	} else if (index == PROFILE_WIN) {
	    return "ProfileWindow";
	} else if (index == COMM_WIN) {
	    return "CommWindow";
	} else if (index == COMM_TIME_WIN) {
	    return "CommTimeWindow";
	} else if (index == CALL_TABLE_WIN) {
	    return "CallTableWindow";
	} else if (index == ANIMATION_WIN) {
	    return "AnimationWindow";
	} else if (index == LOGVIEW_WIN) {
	    return "LogFileViewerWindow";
	} else if (index == HIST_WIN) {
	    return "HistogramWindow";
	} else if (index == TIMELINE_WIN) {
	    return "TimelineWindow";
	} else if (index == OVERVIEW_WIN) {
	    return "StlWindow";
	} else if (index == TIME_PROF_WIN) {
	    return "TimeProfileWindow";
	} else if (index == FUNCTION_WIN) {
	    return "FunctionTool";
	} else if (index == POSE_WIN) {
	    return "PoseAnalysisWindow";
	} else {
	    return null;
	}
    }

    // interface with the menu manager
    public void menuToolSelected(String item) {
	if (item.equals("Graphs")) {
	    showChildWindow("GraphWindow", GRAPH_WIN);
	} else if (item.equals("Histograms")) {
	    showChildWindow("HistogramWindow", HIST_WIN);
	} else if (item.equals("Timelines")) {
	    showChildWindow("TimelineWindow", TIMELINE_WIN);
	} else if (item.equals("Usage Profile")) {
	    showChildWindow("ProfileWindow", PROFILE_WIN);
	} else if (item.equals("Communication")) {
	    showChildWindow("CommWindow", COMM_WIN);
	} else if (item.equals("Communication vs Time")) {
	    showChildWindow("CommTimeWindow", COMM_TIME_WIN);
	} else if (item.equals("Call Table")) {
	    showChildWindow("CallTableWindow", CALL_TABLE_WIN);
	} else if (item.equals("Animation")) {
	    showChildWindow("AnimationWindow", ANIMATION_WIN);
	} else if (item.equals("View Log Files")) {
	    showChildWindow("LogFileViewerWindow", LOGVIEW_WIN);
	} else if (item.equals("Overview")) {
	    showChildWindow("StlWindow", OVERVIEW_WIN);
	} else if (item.equals("Time Profile Graph")) {
	    showChildWindow("TimeProfileWindow", TIME_PROF_WIN);
	} else if (item.equals("User Events")) {
	    showChildWindow("UserEventsWindow", USER_EVENTS_WIN);
	} else if (item.equals("Multirun Analysis")) {
	    showChildWindow("MultiRunWindow", MULTI_WIN);
	} else if (item.equals("Function Tool")) {
	    showChildWindow("FunctionTool", FUNCTION_WIN);
	} else if (item.equals("POSE Analysis")) {
	    showChildWindow("PoseAnalysisWindow", POSE_WIN);
	}
    }

    /**
     *   Menu interface - changing background and foreground colors
     */
    public void changeBackground()
    {
	JColorChooser colorWindow = new JColorChooser();
	Color returnColor =
	    colorWindow.showDialog(this, "Background Color",
				   Analysis.background);
	if (returnColor != null) {
	    Analysis.background = returnColor;
	    repaintAllWindows();
	}
    }

    public void changeForeground()
    {
	JColorChooser colorWindow = new JColorChooser();
	Color returnColor =
	    colorWindow.showDialog(this, "Foreground Color",
				   Analysis.foreground);
	if (returnColor != null) {
	    Analysis.foreground = returnColor;
	    repaintAllWindows();
	}
    }

    public void setGrayscale() {
	Analysis.setGrayscale();
	repaintAllWindows();
    }

    public void setFullColor() {
	Analysis.setFullColor();
	repaintAllWindows();
    }

    // repaints all windows to reflect global drawing changes.
    private void repaintAllWindows() {
	for (int i=0; i<NUM_WINDOWS;i++) {
	    if (childWindows[i][0] != null) {
		if (childWindows[i][0] instanceof GraphWindow) {
		    ((GraphWindow)childWindows[i][0]).refreshDisplay();
		} else if (childWindows[i][0] instanceof TimelineWindow) {
		    ((TimelineWindow)childWindows[i][0]).refreshDisplay();
		} else {  // default
		    ((Frame)childWindows[i][0]).repaint();
		}
	    }
	}
	this.repaint();
    }

    /* show the child window
     *  if the childWindow has not been created yet, then create an
     *  object of type childClass by invoking the corresponding constructor
     *  see
     *http://developer.java.sun.com/developer/technicalArticles/ALT/Reflection/
     * for example use of Java Reflection
     */
    public void showChildWindow(String childClass, int windowIndex)
    {
	try {
	    if (childWindows[windowIndex][0] == null) {
		// get the name of the class within the current package
		// and create an instance of that class
		String className =  getClass().getPackage().getName() + 
		    "." + childClass;
		Class cls  = Class.forName(className);
		Constructor ctr = cls.getConstructor(new Class[]{this.getClass(), Class.forName("java.lang.Integer")});
		childWindows[windowIndex][0] = (ProjectionsWindow)(ctr.newInstance(new Object[] {this, new Integer(windowIndex)}));
	    } else {
		if (childWindows[windowIndex][0] instanceof ProjectionsWindow) {
		    ((ProjectionsWindow)childWindows[windowIndex][0]).showDialog();
		} else {
		    childWindows[windowIndex][0].show();
		}
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void showOpenFileDialog()
    {
	// create a file chooser with current directory set to "."
	JFileChooser d = new JFileChooser(System.getProperty("user.dir"));
	// in future when Multi-Run code is fully integrated into the scheme
	// of things, the following line should be enabled:
	//	  d.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	d.setFileFilter(new MainFileFilter());
	int returnval = d.showOpenDialog(this);
	if (returnval == JFileChooser.APPROVE_OPTION) {
	    setTitle("Projections -" + d.getSelectedFile());
	    openFile(d.getSelectedFile().getAbsolutePath());
	}
    }

    public void openFile(String filename) {
	// clear the old summary data away, otherwise chance of
	// running out of memory is great.
	final String newfile = filename;
	sumDataSource = null;
	sumXAxis = null;
	sumYAxis = null;
	graphPanel = null;

	hor = null;
	ver = null;
	final SwingWorker worker = new SwingWorker() {
		public Object construct() {
		    try {
			Analysis.initAnalysis(newfile, thisWindow);
		    } catch(IOException e) {
			InvalidFileDialog ifd =
			    new InvalidFileDialog(thisWindow);
			ifd.setVisible(true);
		    } catch(StringIndexOutOfBoundsException e) {
			e.printStackTrace();
			InvalidFileDialog ifd =
			    new InvalidFileDialog(thisWindow);
			ifd.setVisible(true);
		    }

		    if (Analysis.hasSummaryData()) {
			// see "finished()"
		    } else if (Analysis.hasLogData()) {
			/* (need to deal with visualization bug)
			status = new Label("");
			status.setBackground(Color.black);
			status.setForeground(Color.lightGray);

			hor=new ScaleSlider(Scrollbar.HORIZONTAL);
			ver=new ScaleSlider(Scrollbar.VERTICAL);

			stl = new StlPanel();
			scalePanel=new ScalePanel(hor,ver,stl);
			scalePanel.setStatusDisplay(thisWindow);
			
			OrderedIntList validPEs = 
			    Analysis.getValidProcessorList();
			long startTime = 0;
			long endTime = Analysis.getTotalTime();
			
			ColorMap utilColorMap = new ColorMap();
			utilColorMap.addBreak(0,0, 0,55, 70,255, 0,0);
			utilColorMap.addBreak(70,255, 0,0, 100,255, 255,255);
			// Overflow-- green. Should not happen for utilization.
			utilColorMap.addBreak(101,0, 255,0, 255,0, 255,0); 
			stl.setColorMap(utilColorMap);

			double horSize, verSize;

			Runtime rt = Runtime.getRuntime();
			// 4 bytes per index, 500 indexs per processor
			int memUsage = 4 * 500 * Analysis.getNumProcessors();
			int maxMem = (int)(rt.totalMemory() * .01 );
			int interval = 1;

			while (memUsage/interval > maxMem) {
			    interval++;
			}
			if (interval > 1) {
			    OrderedIntList tempPEs = validPEs.copyOf();
			    int element, tmp;
			    int count = 0;
			    validPEs.removeAll();
			    tempPEs.reset();
			    
			    while((element = tempPEs.nextElement()) != -1){
				tmp = count / interval;
				tmp = count - tmp*interval;
				if(tmp == 0){
				    validPEs.insert(element);
				}
				count++;
			    }
			}

			if (validPEs == null) {
			    horSize=Analysis.getTotalTime();
			    verSize=Analysis.getNumProcessors();
			} else {	
			    horSize = endTime-startTime;
			    if(horSize <= 0)
				horSize = Analysis.getTotalTime();
			    verSize = (double)validPEs.size();
			}	 
			scalePanel.setScales(horSize,verSize);
			
			double hMin=scalePanel.toSlider(1.0/horSize);
			//0.1ms fills screen
			double hMax=scalePanel.toSlider(0.01);
			hor.setMin(hMin); hor.setMax(hMax);
			hor.setValue(hMin);
			hor.setTicks(Math.floor(hMin),1);
			
			double vMin=scalePanel.toSlider(1.0/verSize);
			//One processor fills screen
			double vMax=scalePanel.toSlider(1.0);
			ver.setMin(vMin); ver.setMax(vMax);
			ver.setValue(vMin);
			ver.setTicks(Math.floor(vMin),1);
			
			stl.setData(validPEs,startTime,endTime);
			*/
		    }			
		    return null;
		}

		public void finished() {
		    setTitle("Projections - " + newfile);
		    if (Analysis.hasSummaryData()) {
			Analysis.loadSummaryData();	  
			double[] data = Analysis.getSummaryAverageData();
			long originalSize = Analysis.getSummaryIntervalSize();
			long bestSize =    
			    (long)IntervalUtils.getBestIntervalSize(originalSize,data.length);	 
			if (bestSize != originalSize) {
			    // if there are changes    
			    // transform the data into absolute time first.
			    IntervalUtils.utilToTime(data,	  
						     (double)originalSize);
			    double[] newdata =	       
				IntervalUtils.rebin(data, originalSize,	 
						    (double)bestSize);
			    // transform the re-binned data to utilization.
			    IntervalUtils.timeToUtil(newdata,	 
						     (double)bestSize);	 
			    try {
				dataDump = 
				    new PrintWriter(new FileWriter(Analysis.getLogDirectory() + File.separator +
								   "SummaryDump.out"));
				dataDump.println("--- Summary Graph ---");
				for (int i=0; i<newdata.length; i++) {
				    dataDump.println(newdata[i]);
				}
				dataDump.flush();
			    } catch (IOException e) {
				System.err.println("WARNING: " +
						   "Failed to handle dump " +
						   "file SummaryDump.out. " +
						   "Reason: ");
				System.err.println(e);
			    }
			    sumDataSource = new SummaryDataSource(newdata);
			    sumXAxis =	    
				new SummaryXAxis(newdata.length,	 
						 (long)bestSize);	  
			} else {		   
			    sumDataSource = new SummaryDataSource(data);
			    sumXAxis =	    
				new SummaryXAxis(data.length,	 
						 (long)(Analysis.getSummaryIntervalSize()));
			}			   
			sumYAxis = new SummaryYAxis();	 
			graphPanel =
			    new GraphPanel(new Graph(sumDataSource, 
						     sumXAxis, sumYAxis));
			summaryGraphPanel.add("data", graphPanel, "run data");
		    } else {
		    /* (bypass the visualization problem for now)
			summaryGraphPanel.add("data", scalePanel, "overview");
			Util.gblAdd(background, ver,    gbc, 1,2, 1,1, 0,1);
			Util.gblAdd(background, hor,    gbc, 0,3, 1,1, 1,0);
			Util.gblAdd(background, status, gbc, 0,4, 1,1, 1,0);
		    */
		    }
		    if (Analysis.hasLogData()) {
			menuManager.fileOpened();
		    } else if (Analysis.hasSummaryData()) {
			menuManager.summaryOnly();
		    }
		    if (Analysis.hasPoseDopData()) {
			menuManager.addPose();
		    }
		}
	    };
	    worker.start();
    }

    /* called by the childWindows to remove references to themselves */
    public void closeChildWindow(int childID)
    {
	childWindows[childID][0] = null;
    }

    public void shutdown() {
	// in future, some cleanup action might be required.
	System.exit(0);
    }

    public void closeCurrent() {
	// temporary implementation given multi run is not supported yet
	// should also close all run-associated tool windows
	//	summaryGraphPanel.removeCurrent();
	//      if (summaryGraphPanel.isEmpty()) {
	//          menuManager.lastFileClosed();
	//      }
	closeAll();
    }

    public void closeAll() {
	summaryGraphPanel.removeAll();
	menuManager.lastFileClosed();
	setTitle("Projections");
    }

    public Color getGraphColor(int e)
    {
	if(graphWindow != null)
	    return graphWindow.getGraphColor(e);
	else
	    return null;
    }

    public boolean GraphExists()
    {
	if(graphWindow != null)
	    return true;
	else  
	    return false;
    }

    public void setStatus(String msg) {
   	status.setText(msg);
    }
}
