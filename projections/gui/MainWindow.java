package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import projections.analysis.*;
import projections.gui.graph.*;

public class MainWindow extends JFrame
    implements ScalePanel.StatusDisplay
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* **** Temporary hardcode for the number of runs supported
       in Projections **** */
    protected static final int NUM_RUNS = 1;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    /* **** Static setup data for windows ***** */
    protected static final int NUM_WINDOWS = 18;

    protected static final int GRAPH_WIN = 0;
    protected static final int TIMELINE_WIN = 1;
    protected static final int PROFILE_WIN = 2;
    protected static final int COMM_WIN = 3;
    protected static final int COMM_TIME_WIN = 4;
    protected static final int CALL_TABLE_WIN = 5;
    protected static final int LOGVIEW_WIN = 6;
    protected static final int HIST_WIN = 7;
    protected static final int OVERVIEW_WIN = 8;
    protected static final int ANIMATION_WIN = 9;
    protected static final int TIME_PROF_WIN = 10;
    protected static final int USER_EVENTS_WIN = 11;
    protected static final int OUTLIER_WIN = 12;
    protected static final int MULTI_WIN = 13;
    protected static final int FUNCTION_WIN = 14;
    //    protected static final int POSE_WIN = 15;
    protected static final int AMPI_PROFILE_WIN = 16;

    // When modifying this list also make sure you modify the list below and MainMenuManager
    public static final String[] windowMenuNames =
    {
	"Graphs",
	"Timelines",
	"Usage Profile",
	"Communication",
	"Communication vs Time",
	"Call Table",
	"View Log Files",
	"Histograms",
	"Overview",
	"Animation",
	"Time Profile Graph",
	"User Events",
	"Outlier Analysis",
	"Multirun Analysis",
	"Function Tool",
	//	"POSE Analysis",
	"AMPI Usage Profile",
	"Noise Miner"
    };

    public static final String[] windowClassNames =
    { 
	"GraphWindow",
	"Timeline.TimelineWindow",
	"ProfileWindow",
	"CommWindow",
	"CommTimeWindow",
	"CallTableWindow",
	"LogFileViewerWindow",
	"HistogramWindow",
	"StlWindow",
	"AnimationWindow",
	"TimeProfileWindow",
	"UserEventsWindow",
	"OutlierAnalysisWindow",
	"MultiRunWindow",
	"FunctionTool",
  //	"PoseAnalysisWindow",
	"AmpiProfileWindow",
	"NoiseMinerWindow"
    };

    public static final boolean[][] menuDataStates =
    {
	{
	},
	{
	},
    };

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
    MainWindow thisWindow;

    // The Analysis object from which tools derive their performance
    // data from. This is temporarily a one-element array.
    public static Analysis runObject[];

    // Indexed by number of runs (currently one) and tools available
    // This should eventually be configurable for multiple runs,
    // multiple languages with multiple user-level visualization tools.
    protected JFrame childWindows[][];

    // should be tables of objects dependent indexed by runs in the future.
    private GraphWindow          graphWindow;

    // components associated with the main window
    private MainTitlePanel        titlePanel;
    private BackGroundImagePanel  background;
    MainMenuManager       menuManager;
    MainSummaryGraphPanel summaryGraphPanel;
    private MainRunStatusPanel    runStatusPanel;

    // these should become arrays for future tabbed multirun functionality.
    SummaryDataSource    sumDataSource;
    SummaryXAxis         sumXAxis;
    SummaryYAxis         sumYAxis;
    GraphPanel           graphPanel;
    private Label                status;
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
		    ProjMain.shutdown(0);
		}
	    });

	setBackground(Color.lightGray);

	// set up the main run analysis object. This is a unique object
	// for now until a mechanism is built for handling multiple runs.
	// long timestamp = System.currentTimeMillis();
	runObject = new Analysis[NUM_RUNS];
	runObject[0] = new Analysis();
	//	System.out.println("Time taken to init Analysis = " +
	//			   (System.currentTimeMillis() - timestamp) +
	//			   "milliseconds.");

	childWindows = new JFrame[NUM_WINDOWS][DEFAULT_NUM_RUNS];

	menuManager = new MainMenuManager(this);
	createLayout();
    }

    private void createLayout()
    {
	try {
	    URL imageURL = ((Object)this).getClass().getResource("/projections/images/bgimage.jpg");
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

    /**
     *   Menu interface - changing background and foreground colors
     */
    public void changeBackground()
    {
	JColorChooser colorWindow = new JColorChooser();
	Color returnColor =
	    JColorChooser.showDialog(this, "Background Color",
				   MainWindow.runObject[myRun].background);
	if (returnColor != null) {
	    MainWindow.runObject[myRun].background = returnColor;
	    repaintAllWindows();
	}
    }

    public void changeForeground()
    {
	JColorChooser colorWindow = new JColorChooser();
	Color returnColor =
	    JColorChooser.showDialog(this, "Foreground Color",
				   MainWindow.runObject[myRun].foreground);
	if (returnColor != null) {
	    MainWindow.runObject[myRun].foreground = returnColor;
	    repaintAllWindows();
	}
    }

    public void setGrayscale() {
	MainWindow.runObject[myRun].setGrayscale();
	repaintAllWindows();
    }

    public void setFullColor() {
	MainWindow.runObject[myRun].setFullColor();
	repaintAllWindows();
    }

    // repaints all windows to reflect global drawing changes.
    private void repaintAllWindows() {
	for (int i=0; i<NUM_WINDOWS;i++) {
	    if (childWindows[i][0] != null) {
		if (childWindows[i][0] instanceof GraphWindow) {
		    ((GraphWindow)childWindows[i][0]).refreshDisplay();
		} else if (childWindows[i][0] instanceof projections.gui.Timeline.TimelineWindow) {
		    ((projections.gui.Timeline.TimelineWindow)childWindows[i][0]).refreshDisplay(false);
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
		    // System.out.println("showChildWindow ProjectionsWindow case");
		    ((ProjectionsWindow)childWindows[windowIndex][0]).showDialog();
		} else {
		    childWindows[windowIndex][0].setVisible(true);
		}
	    }
        } 
        catch (NoSuchMethodException e){
          e.printStackTrace();
        }
        catch (InstantiationException e){
          e.printStackTrace();
        }
        catch(IllegalAccessException e){
          e.printStackTrace();
        }
        catch(InvocationTargetException e){
          e.printStackTrace();
        }
        catch ( LinkageError e){
          e.printStackTrace();
        } 
        catch ( ClassNotFoundException e){
          JOptionPane.showMessageDialog(this, "Tool not available. You should use a different version of java(>=1.5) to compile projections.", "Tool Not Available", JOptionPane.ERROR_MESSAGE);
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
	
	final SwingWorker worker = new SwingWorker() {
	    public Object construct() {
	      try {
		MainWindow.runObject[myRun].initAnalysis(newfile, 
							 thisWindow);
	      } catch (IOException e) {
		InvalidFileDialog ifd =
		  new InvalidFileDialog(thisWindow, e);
		ifd.setVisible(true);
	      } catch (StringIndexOutOfBoundsException e) {
		e.printStackTrace();
		InvalidFileDialog ifd =
		  new InvalidFileDialog(thisWindow, e);
		ifd.setVisible(true);
	      }
	      return null;
	    }
	    public void finished() {
	      setTitle("Projections - " + newfile);
	      if (MainWindow.runObject[myRun].hasSummaryData()) {
		MainWindow.runObject[myRun].loadSummaryData();	  
		double[] data = MainWindow.runObject[myRun].getSummaryAverageData();
		long originalSize = MainWindow.runObject[myRun].getSummaryIntervalSize();
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
		      new PrintWriter(new FileWriter(MainWindow.runObject[myRun].getLogDirectory() + File.separator +
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
				     (long)(MainWindow.runObject[myRun].getSummaryIntervalSize()));
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
	      if (MainWindow.runObject[myRun].hasLogData()) {
		menuManager.fileOpened();
	      } else if (MainWindow.runObject[myRun].hasSummaryData()) {
		menuManager.summaryOnly();
	      }
	      /* Removed to avoid confusing readers of the manual.
		 This is a still-being-developed feature.
	      if (MainWindow.runObject[myRun].hasPoseDopData()) {
		menuManager.addPose();
	      }
	      */
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
	// do NOT call exit() here. This routine is executed by ProjMain
	// which will call exit().
	MainWindow.runObject[myRun].closeRC();
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
	MainWindow.runObject[myRun].closeRC();
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
