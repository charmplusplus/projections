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

public class MainWindow extends JFrame
    implements ActionListener
{
    protected static final int NUM_WINDOWS = 10;

    protected static final int GRAPH_WIN = 0;
    protected static final int MULTI_WIN = 1;
    protected static final int PROFILE_WIN = 2;
    protected static final int COMM_WIN = 3;
    protected static final int ANIMATION_WIN = 4;
    protected static final int LOGVIEW_WIN = 5;
    protected static final int HIST_WIN = 6;
    protected static final int TIMELINE_WIN = 7;
    protected static final int OVERVIEW_WIN = 8;
    protected static final int TIME_PROF_WIN = 9;

    private static final int DEFAULT_NUM_RUNS = 1;

    public static double CUR_VERSION = 4.0;
    public static boolean IGNORE_IDLE = false;

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
    private HelpWindow           helpWindow;
    private LogFileViewerWindow  logFileViewerWindow;
    private HistogramWindow      histogramWindow;
    private StlWindow            stlWindow;
    private MultiRunWindow       multiRunWindow;
    private AnimationWindow      animationWindow;
    private TimeProfileWindow    timeProfileWindow;

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

    private Image bgimage;

    public MainWindow()
    {
	thisWindow = this;
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

	childWindows = new JFrame[DEFAULT_NUM_RUNS][NUM_WINDOWS];
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
	toolDescriptions[LOGVIEW_WIN] = "View Logs";
	toolDescriptions[HIST_WIN] = "Histograms";
	toolDescriptions[ANIMATION_WIN] = "Animation";
	toolDescriptions[TIMELINE_WIN] = "Timeline";
	toolDescriptions[OVERVIEW_WIN] = "Overview";
	toolDescriptions[TIME_PROF_WIN] = "Time Profile Graph";

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

	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
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

	pack();
    }

    // get child window name by number
    public String getChildName(int index) {
		if(index == GRAPH_WIN) {
			return "GraphWindow";
		} else if (index == MULTI_WIN) {
			return "MultiRunWindow";
		} else if (index == PROFILE_WIN) {
			return "ProfileWindow";
		} else if (index == COMM_WIN) {
			return "CommWindow";
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
	} else if (item.equals("Communication Histogram")) {
	    showChildWindow("CommWindow", COMM_WIN);
	} else if (item.equals("Animation")) {
	    showChildWindow("AnimationWindow", ANIMATION_WIN);
	} else if (item.equals("View Log Files")) {
	    showChildWindow("LogFileViewerWindow", LOGVIEW_WIN);
	} else if (item.equals("Overview")) {
	    showChildWindow("StlWindow", OVERVIEW_WIN);
	} else if (item.equals("Time Profile Graph")) {
	    showChildWindow("TimeProfileWindow", TIME_PROF_WIN);
	} else if (item.equals("Multirun Analysis")) {
	    showChildWindow("MultiRunWindow", MULTI_WIN);
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
	    if (childWindows[0][i] != null) {
		((Frame)childWindows[0][i]).repaint();
	    }
	}
	if (timelineWindow != null) {
	    timelineWindow.validate();
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
	    if (childWindows[0][windowIndex] == null) {
		// get the name of the class within the current package
		// and create an instance of that class
		String className =  getClass().getPackage().getName() + 
		    "." + childClass;
		Class cls  = Class.forName(className);
		Constructor ctr = cls.getConstructor(new Class[]{this.getClass(), Class.forName("java.lang.Integer")});
		childWindows[0][windowIndex] = (ProjectionsWindow)(ctr.newInstance(new Object[] {this, new Integer(windowIndex)}));
	    } else {
		if (childWindows[0][windowIndex] instanceof ProjectionsWindow) {
		    ((ProjectionsWindow)childWindows[0][windowIndex]).showDialog();
		} else {
		    childWindows[0][windowIndex].show();
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

    private void openFile(String filename) {
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
			    new GraphPanel(new Graph(sumDataSource, sumXAxis, sumYAxis));
			summaryGraphPanel.add("data", graphPanel, "run data");

		    }
		    menuManager.fileOpened();
		}
	    };
	    worker.start();
    }

    /* called by the childWindows to remove references to themselves */
    public void closeChildWindow(int childID)
    {
	childWindows[0][childID] = null;
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

    public static void help()
    {
	System.out.println("-h:		show this page");
	System.out.println("-V:		show Projections version");
	System.out.println("-u <ver>:	use old version format");
	System.exit(0);
    }

    public static void main(String args[])
    {
        int i=0;
	String loadSts=null;
        while (i < args.length) {
	    if (args[i].equals("-h")) {
		help();
	    }
	    else if (args[i].equals("-V")) {
		System.out.println("Projections version: "+Analysis.getVersion());
		System.exit(0);
	    }
	    else if (args[i].equals("-u")) {
		i++;
		if (i==args.length) help();
		double useVersion = Double.parseDouble(args[i]);
		if (useVersion > CUR_VERSION) {
		    System.out.println("Invalid (future) Projections version!");
		    System.exit(1);
		}
		CUR_VERSION = useVersion;
	    } else if (args[i].equals("-idle")) {
		IGNORE_IDLE = true;
	    }
	    else /*unrecognized argument*/
		loadSts=args[i];
	    i++;
	}

	MainWindow f = new MainWindow();
	f.pack();
	f.setTitle("Projections");
	f.setVisible(true);
	if (loadSts!=null) { f.openFile(loadSts); }
    }

}
