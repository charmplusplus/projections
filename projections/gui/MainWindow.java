package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import projections.misc.*;
import projections.gui.count.*;
import projections.gui.graph.*;

public class MainWindow extends JFrame
    implements ActionListener
{
    private static double 	CUR_VERSION = 4.0;

    // should be tables of objects dependent indexed by runs in the future.
    private GraphWindow          graphWindow;
    private TimelineWindow       timelineWindow;
    private AnimationWindow      animationWindow;
    private ProfileWindow        profileWindow;
    private CommWindow           commWindow;
    private HelpWindow           helpWindow;
    private LogFileViewerWindow  logFileViewerWindow;
    private HistogramWindow      histogramWindow;
    private StlWindow            stlWindow;
    private MultiRunWindow       multiRunWindow;
    private IntervalWindow 	intervalWindow;
    private EPCharWindow 	epCharWindow;

    // GraphingWindow will eventually be renamed and will replace both
    // GraphWindow and ProfileWindow.
    private GraphingWindow       graphingWindow;

    private BGGraphWindow       bgWindow;
    private EPAnalysis           epAnalysis;
    
    // components associated with the main window
    private MainTitlePanel       titlePanel;
    private MainWindowPanel      mainPanel;
    private MainMenuManager      menuManager;
    private MainSummaryGraphPanel summaryGraphPanel;
    private MainRunStatusPanel   runStatusPanel;

    // these should become arrays for future tabbed multirun functionality.
    private SummaryDataSource    sumDataSource;
    private SummaryXAxis         sumXAxis;
    private SummaryYAxis         sumYAxis;
    private GraphPanel           graphPanel;

    private Image bgimage;
    
    public MainWindow()
    {
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
	
	menuManager = new MainMenuManager(this);
	createLayout();
    }                              
    
    public void actionPerformed(ActionEvent evt)
    {
    }
    
    private void createLayout()
    {
	try {
	    URL imageURL = ((Object)this).getClass().getResource("/projections/images/bgimage");
	    bgimage = Toolkit.getDefaultToolkit().getImage(imageURL);
	    // mainPanel is used to draw the wall paper and serves as the
	    // MainWindow's contentPane.
	    mainPanel = new MainWindowPanel(bgimage);
	} catch (Exception E) {
	    System.out.println("Error loading background image.  Continuing.");
	}
	
	setContentPane(mainPanel);

	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	mainPanel.setLayout(gbl);
	
	titlePanel  = new MainTitlePanel(this);
	runStatusPanel = new MainRunStatusPanel();
	summaryGraphPanel = new MainSummaryGraphPanel(runStatusPanel);

	Util.gblAdd(mainPanel, titlePanel,        
		    gbc, 0,0, 1,1, 1,0, 0,0,0,0);
	Util.gblAdd(mainPanel, runStatusPanel,
		    gbc, 0,1, 1,1, 1,0, 0,20,0,20);
	Util.gblAdd(mainPanel, summaryGraphPanel, 
		    gbc, 0,2, 1,1, 1,1, 0,20,20,20);

	pack();
    }    

    // interface with the menu manager
    public void menuToolSelected(String item) {	
	if (item.equals("Graphs")) {
	    showChildWindow(graphWindow, "GraphWindow");
	} else if (item.equals("Histograms")) {
	    showChildWindow(histogramWindow, "HistogramWindow");
	} else if (item.equals("Timelines")) {
	    showTimelineWindow();
	} else if (item.equals("Animations")) {
	    showAnimationWindow();
	} else if (item.equals("Usage Profile")) {
	    showChildWindow(profileWindow, "ProfileWindow");
	} else if (item.equals("Communication Histogram")) {
		showChildWindow(commWindow, "CommWindow");
	} else if (item.equals("View Log Files")) {
	    showChildWindow(logFileViewerWindow, "LogFileViewerWindow");
	} else if (item.equals("Overview")) {
	    showStlWindow();
	} else if (item.equals("Multirun Analysis")) {
	    showChildWindow(multiRunWindow, "MultiRunWindow");
	} else if (item.equals("Performance Counters")) {
	    showCounterWindow();
	} else if (item.equals("General Graph")) {
	    showChildWindow(graphingWindow, "GraphingWindow");
	} else if (item.equals("Unified Summary Graph")) {
	    // ?
	} else if (item.equals("Interval Graph")) {
	    // ?
	} else if (item.equals("Entry Point Characteristics Graph")) {
	    // ?
	} else if (item.equals("Generate EP Data")) {
	}
    }
     
    /* show the child window
     *  if the childWindow has not been created yet, then create an object of type childClass
     *  by invoking the corresponding constructor 
     *  see http://developer.java.sun.com/developer/technicalArticles/ALT/Reflection/ for example use of Java Reflection
     */
    public void showChildWindow(Object childWindow, String childClass)
    {
	try {
	    if(childWindow == null) {
		// get the name of the class within the current package and create an instance of that class
		String className = getClass().getPackage().getName() + "." + childClass;
		Class cls  = Class.forName(className);
		Constructor ctr = cls.getConstructor(new Class[]{this.getClass()});
		childWindow = ctr.newInstance(new Object[] {this});
		//	childWindow.setVisible(true); ?? NEEDED??
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	} 
    }

    public void showTimelineWindow()
    {
	if(timelineWindow == null) {
	    timelineWindow = new TimelineWindow(this);
	    timelineWindow.setSize(640,480);
	}
    }   
    
    public void showAnimationWindow()
    {
	if(animationWindow == null)
	    new Thread(new Runnable() {public void run() {
		animationWindow = new AnimationWindow();
		animationWindow.setVisible(true);
	    }}).start();
    }
      
    public void showStlWindow()
    {
	new Thread(new Runnable() {public void run() {
	    stlWindow = new StlWindow();
	}}).start();
    }                              
    
    public void showCounterWindow()
    {
	CounterFrame f = new CounterFrame();
	ProjectionsFileMgr fileMgr = null;
	try {
	    ProjectionsFileChooser fc =
		new ProjectionsFileChooser(f, "Performance Counter Analysis",
					   ProjectionsFileChooser.MULTIPLE_FILES);
	    
	    CounterCallBack callback = new CounterCallBack(f,fc);
	    int retval = fc.showDialog(callback);
	}
	catch(Exception exc) { 
	    System.out.println("something got screwed");
	    ProjectionsFileChooser.handleException(f, exc); 
	}
    }

    public void activateEPAnalysis()
    {
	if (epAnalysis == null)
	    epAnalysis = new EPAnalysis(this);
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
	try {
	    Analysis.initAnalysis(filename);
	    Analysis.loadSummaryData();
	    if (Analysis.hasSummaryData()) {
		double[] data = Analysis.getSummaryAverageData(); 
		sumDataSource = new SummaryDataSource(data);
		sumXAxis = 
		    new SummaryXAxis(data.length,
				     (long)(Analysis.getSummaryIntervalSize()));
		sumYAxis = new SummaryYAxis();
		graphPanel = 
		    new GraphPanel(new Graph(sumDataSource, sumXAxis, sumYAxis));
		summaryGraphPanel.add("data", graphPanel, "run data");
	    }
	    menuManager.fileOpened();
	} catch(IOException e) {
	    InvalidFileDialog ifd = new InvalidFileDialog(this);
	    ifd.setVisible(true);
	} catch(StringIndexOutOfBoundsException e) {
	    e.printStackTrace();
	    InvalidFileDialog ifd = new InvalidFileDialog(this);
	    ifd.setVisible(true);
	}
    }

    /* called by the childWindows to remove references to themselves */
    public void closeChildWindow(Object childWindow)
    {
	if(childWindow.equals(timelineWindow))
	    timelineWindow = null;
	else if(childWindow.equals(profileWindow))
	    profileWindow = null;
	else if(childWindow.equals(logFileViewerWindow))
	    logFileViewerWindow = null;
	else if(childWindow.equals(graphWindow))
	    graphWindow = null;
	else if(childWindow.equals(multiRunWindow))
	    multiRunWindow = null;
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

    public void CloseEPAnalysis()
    {
	epAnalysis = null;
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
	Analysis.setVersion(CUR_VERSION);
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
		Analysis.setVersion(useVersion);
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
