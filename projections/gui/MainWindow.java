package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import projections.analysis.*;
import projections.gui.graph.*;

public class MainWindow extends JFrame
implements ScalePanel.StatusDisplay
{

	/* **** Temporary hardcode for the number of runs supported
       in Projections **** */
	protected static final int NUM_RUNS = 1;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	/* **** Static setup data for windows ***** */
	protected static final int NUM_WINDOWS = 18;


	/** References to all open tool windows */
	private LinkedList<ProjectionsWindow> openToolWindows; 


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

	// components associated with the main window
	private MainTitlePanel        titlePanel;
	private BackGroundImagePanel  background;
	public MainMenuManager       menuManager;
	public MainSummaryGraphPanel summaryGraphPanel;
	private MainRunStatusPanel    runStatusPanel;

	// these should become arrays for future tabbed multirun functionality.
	public SummaryDataSource    sumDataSource;
	public SummaryXAxis         sumXAxis;
	public SummaryYAxis         sumYAxis;
	public GraphPanel           graphPanel;
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

		openToolWindows = new LinkedList();

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
		Iterator<ProjectionsWindow> iter = openToolWindows.iterator();
		while(iter.hasNext()){
			ProjectionsWindow w = iter.next();
			if (w instanceof GraphWindow) {
				((GraphWindow)w).refreshDisplay();
			} else if (w instanceof projections.gui.Timeline.TimelineWindow) {
				((projections.gui.Timeline.TimelineWindow)w).refreshDisplay(false);
			} else { 
				w.repaint();
			}

		}
		this.repaint();
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
					//		MainWindow.runObject[myRun].loadSummaryData();	  
					double[][] data = MainWindow.runObject[myRun].getSummaryAverageData();
					long originalSize = MainWindow.runObject[myRun].getSummaryIntervalSize();
					// if summary override, perform sanity check
					if (ProjMain.SUM_OVERRIDE) {
						if ((ProjMain.SUM_END_INT <= ProjMain.SUM_START_INT) ||
								(((long)data.length * originalSize) <
										((long)ProjMain.SUM_END_INT * ProjMain.SUM_INT_SIZE)))
						{
							// re-use defaults while printing warning message
							System.out.println("Warning: Specified Summary " +
									"parameters of [" + 
									ProjMain.SUM_START_INT + "," +
									ProjMain.SUM_END_INT + "," +
									ProjMain.SUM_INT_SIZE + "] " +
									"is inconsistent. " +
									"Real Summary Data " +
									"has [" + 0 + "," + 
									data.length + 
									"," +
									originalSize + "]. " + 
									"Reverting to default " +
							"main summary display.");
							ProjMain.SUM_OVERRIDE = false;
						}
					}
					long bestSize = 0;
					// if override is specified, make sure user choice is respected
					if (ProjMain.SUM_OVERRIDE) {
						bestSize = ProjMain.SUM_INT_SIZE;
					} else {
						bestSize = (long)IntervalUtils.getBestIntervalSize(originalSize,data.length);
					}
					// default cases
					double[][] newdata;
					if (bestSize != originalSize) {
					        double[] timeData = new double[data.length];
						double[] idleData = new double[data.length];
						for (int i=0; i<data.length; i++) {
						    timeData[i] = data[i][0];
						    idleData[i] = data[i][1];
						}
						double[] tempTimeData;
						double[] tempIdleData;
					        // if there are changes    
						// transform the data into absolute time first.
						IntervalUtils.utilToTime(timeData,	  
								(double)originalSize);
						IntervalUtils.utilToTime(idleData,	  
								(double)originalSize);

						// transform the re-binned data back to percentages.
						tempTimeData = IntervalUtils.rebin(timeData, originalSize,	        
										   (double)bestSize);
						IntervalUtils.timeToUtil(tempTimeData,	 
									 (double)bestSize);	 
						tempIdleData = IntervalUtils.rebin(idleData, originalSize,	        
										   (double)bestSize);
						IntervalUtils.timeToUtil(tempIdleData,	 
									 (double)bestSize);	 

						// default case
						newdata = new double[tempTimeData.length][2];
						for (int i=0; i<tempTimeData.length; i++) {
						    newdata[i][0] = tempTimeData[i];
						    newdata[i][1] = tempIdleData[i];
						}
						// special case
						if (ProjMain.SUM_OVERRIDE) {
						    newdata = new double[ProjMain.SUM_END_INT -
									 ProjMain.SUM_START_INT + 1][2];
						    for (int i=0; i<newdata.length; i++) {
							newdata[i][0] = tempTimeData[i+ProjMain.SUM_START_INT];
							newdata[i][1] = tempIdleData[i+ProjMain.SUM_START_INT];
						    }
						}
					} else {
					    newdata = data;
					    if (ProjMain.SUM_OVERRIDE) {
						newdata = new double[ProjMain.SUM_END_INT -
								     ProjMain.SUM_START_INT + 1][2];
						for (int i=0; i<newdata.length; i++) {
						    newdata[i][0] = data[i+ProjMain.SUM_START_INT][0];
						    newdata[i][1] = data[i+ProjMain.SUM_START_INT][1];
						}
					    }
					}
					try {
					        dataDump = 
						    new PrintWriter(new FileWriter(MainWindow.runObject[myRun].getLogDirectory() + File.separator +
										   "SummaryDump.out"));
					        dataDump.println("--- Summary Graph ---");
						for (int i=0; i<newdata.length; i++) {
							if (ProjMain.SUM_OVERRIDE) {
								dataDump.print(i+ProjMain.SUM_START_INT + " ");
							} else {
								dataDump.print(i + " ");
							}
							dataDump.println(newdata[i][0]); // **CWL** Only Dump Util data for now.
						}
						dataDump.flush();
					} catch (IOException e) {
						System.err.println("WARNING: " +
								"Failed to handle dump " +
								"file SummaryDump.out. " +
						"Reason: ");
						System.err.println(e);
					}
					if (ProjMain.SUM_OVERRIDE) {
						sumXAxis =	    
							new SummaryXAxis(ProjMain.SUM_START_INT,
									ProjMain.SUM_END_INT,
									ProjMain.SUM_INT_SIZE);
						sumDataSource = new SummaryDataSource(newdata,ProjMain.SUM_START_INT);
					} else {		  
						sumXAxis =	    
							new SummaryXAxis(0, newdata.length,	 
									(long)bestSize);	  
						sumDataSource = new SummaryDataSource(newdata,0);
					}
					sumYAxis = new SummaryYAxis();	 
					graphPanel =
						new GraphPanel(new Graph(sumDataSource, 
								sumXAxis, sumYAxis));
					summaryGraphPanel.add("data", graphPanel, "run data");
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
	public void closeChildWindow(ProjectionsWindow child)
	{	
		System.out.println("Removing window from openToolWindows");
		openToolWindows.remove(child);
	}

	public void shutdown() {
		// do NOT call exit() here. This routine is executed by ProjMain
		// which will call exit().
		MainWindow.runObject[myRun].closeRC();
	}

	public void closeAll() {
		summaryGraphPanel.removeAll();
		menuManager.lastFileClosed();
		MainWindow.runObject[myRun].closeRC();
		setTitle("Projections");
	}


	public void setStatus(String msg) {
		status.setText(msg);
	}

	public void addProcessor(int pe) {
		Iterator<ProjectionsWindow> iter = openToolWindows.iterator();
		while(iter.hasNext()){
			ProjectionsWindow w = iter.next();
			w.addProcessor(pe);
		}
	}


	public void closeCurrent() {
		closeAll();	
	}

	/** Keep a reference to a newly opened tool window  */
	public void openTool(ProjectionsWindow w){	
		Iterator<ProjectionsWindow> iter = openToolWindows.iterator();
		while(iter.hasNext()){
			ProjectionsWindow i = iter.next();
		}
		openToolWindows.add(w);
	}


}
