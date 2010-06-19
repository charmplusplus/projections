package projections.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.TreeMap;
import java.awt.*;

import javax.swing.SwingWorker;
import javax.swing.JOptionPane;

import projections.analysis.IntervalData;
import projections.analysis.LogLoader;
import projections.analysis.LogReader;
import projections.analysis.PoseDopReader;
import projections.analysis.ProjMain;
import projections.analysis.ProjectionsConfigurationReader;
import projections.analysis.StsReader;
import projections.analysis.SumAnalyzer;
import projections.analysis.UsageCalc;
import projections.misc.FileUtils;
import projections.misc.LogLoadException;
import projections.misc.SummaryFormatException;

/**
 *  Analysis
 *  Modified by Chee Wai Lee
 *  7/9/2007 - Finally making it an object with non-static members
 *
 *  7/4/2003
 *  10/26/2005 - moved file manipulation abstraction from StsReader to
 *               this poor over-burdened Class.
 *
 *  Analysis is a static class used as an interface for GUI objects to talk 
 *  to the various Readers employed. Sts-related information is initialized 
 *  via the initAnalysis call.
 *
 *  Its job, then, is to provide the necessary data via its API to the
 *  various GUI tools. It will isolate the reading of data from the requests
 *  meaning that Analysis will activate the necessary Readers to service a
 *  request.
 *
 *  A requesting GUI tool may check to see if a piece of data is actually
 *  available before requesting the information from Analysis via a second
 *  set of API.
 */
public class Analysis {

  /******************* Initialization ************/
  public ProjectionsConfigurationReader rcReader;
  public Component guiRoot;
  
  private StsReader sts;
  
  public LogLoader logLoader;  //Only for .log files
  
  public SumAnalyzer sumAnalyzer; //Only for .sum files
  
  private PoseDopReader dopReader; //Only for .poselog files
  
  private IntervalData intervalData; // interval-based data
  
  
  /** Stores previous selections from a range dialog box from all tools */
  public RangeDialogPersistantData persistantRangeData; 
  
  // The total time (maxed) of a run across all processors.
  private long totalTime = 0;
  private long poseTotalTime = 0;
  private long poseTotalVirtualTime = 0;
  
  /******************* Graphs ***************/
  private int[][][] systemUsageData;
//  public int[][][][] systemMsgsData;
//  public int[][][][] userEntryData;
  
  
  /****************** Jump from Timeline to graphs ******/
  // Used for storing user defined startTime and endTime when jumping from
  // TimelineWindow to other graphs
  private long jStartTime, jEndTime;
  private boolean jTimeAvailable;

  /** ************** Constants ********************** */
  
  protected static final int NUM_ACTIVITIES = 4;
  public static final int PROJECTIONS = 0;
  public static final int USER_EVENTS = 1;
  public static final int FUNCTIONS = 2;
  //    public static final int POSE_DOP = 3;
  public static final String NAMES[] = {"PROJECTIONS", "USER_EVENTS", "FUNCTIONS"};
  
  
  /** *************** Color Maps 6/27/2002 ************ */
  
  public ColorManager colorManager;
  
  public Color background = Color.black;
  public Color foreground = Color.white;
  
  public Color[] userEventColors;
  public Color[] functionColors;
  public Color[][] activityColors =
  new Color[NUM_ACTIVITIES][];
  
  public Color[] entryColors;
  public TreeMap<Integer, Color> entryColorsMapping = new TreeMap<Integer, Color>();
  
  Paint overhead = new GradientPaint(0, 0, Color.black, 15, -25, new Color(50,50,50), true);
  Paint idle = new GradientPaint(0, 0, Color.white, 15, 25, new Color(230,230,230), true);
  public static int isOverhead = -1;
  public static int isIdle = -2;
  
  public Analysis() {
    // empty constructor for now. initAnalysis is still the "true"
    // constructor until multiple run data is supported.
  }
  
  
  FileUtils fileNameHandler;
  /** ************** Methods ********************** */
  
  /**
   *  initAnalysis can be considered as Analysis's "constructor".
   *  It attempts to read the sts file associated with this run and
   *  determines the presence of all associated log files.
   *
   *  At the end of initAnalysis, the following should happen:
   *  1) SummaryReaders should be activated and summary data available.
   *  2) Sum Detail Readers should be activated and sumDetail data available.
   *  3) total time of the run should be known.
   *
   */    
  protected void initAnalysis(String filename, Component rootComponent) 
    throws IOException 
    {
	  guiRoot = rootComponent;
	  try {
		  
		  fileNameHandler = new FileUtils(filename);

		  setSts(new StsReader(filename));

		  // Version Check (Kind of a hack, since the format of the Sts file
		  // can change between versions.
		  if (getSts().getVersion() > MainWindow.CUR_VERSION) {
			  System.err.println("Projections Version [" + MainWindow.CUR_VERSION +
					  "] unable to handle files of Version [" +
					  getSts().getVersion() + "].");
			  System.err.println("Exiting.");
			  System.exit(-1);
		  }

		  rcReader = new ProjectionsConfigurationReader(fileNameHandler);

		  // Load saved color information from file
		  try {loadColors();}
		  catch(Exception e) {};

		  // Build Summary Data
		  if (hasSumFiles()) {
			  sumAnalyzer = null;
			  sumAnalyzer = new SumAnalyzer();
		  }

		  // Build Summary Detail Data
		  if (hasSumDetailFiles()) {
			  if (intervalData == null) {
				  intervalData = new IntervalData();
			  }
		  }

		  // Initialize Log Data
		  if (hasLogFiles()) {
			  logLoader = new LogLoader();
		  }

		  // Build POSE dop Data
		  if (hasPoseDopFiles()) {
			  dopReader = new PoseDopReader();
		  }

		  // Determine End Time
		  findEndTime();

		  // Flush any updated RC data to disk
		  rcReader.writeFile();

		  // Create a structure to store selected ranges from all the dialog boxes in all the tools
		  persistantRangeData = new RangeDialogPersistantData(getValidProcessorList(), 0, getTotalTime());


	  } catch (LogLoadException e) {
		  // if sts reader could not be created because of a log load
		  // exception, forward the error as an IOException.
		  // LogLoadException is a silly exception in the first place!
		  throw new IOException(e.toString());
	  } catch (SummaryFormatException e) {
		  throw new IOException(e.toString());
	  }
	  
	  
    }

  private void findEndTime() {
    // If the Configuration file has saved data, then use it!
    if (rcReader.RC_GLOBAL_END_TIME.longValue() >= 0) {
      totalTime =
	rcReader.RC_GLOBAL_END_TIME.longValue();
    } else {
      // Need to determine End Time as the max of all log information.
      // ...
      // From accumulated summary file
      if (hasSumAccumulatedFile()) {
	/* DO NOTHING FOR NOW. WILL WRITE NEW CODE FOR THIS BEHAVIOR
	// clear memory first ...
	sumAnalyzer = null;
	sumAnalyzer = new SumAnalyzer(sts, SumAnalyzer.ACC_MODE);
	setTotalTime(sumAnalyzer.getTotalTime());
	*/
      }
      // From summary files
      if (hasSumFiles()) {
	if (sumAnalyzer.getTotalTime() > totalTime) {
	  totalTime = sumAnalyzer.getTotalTime();
	}
      }
      // From summary detail files
      if (hasSumDetailFiles()) {
	long temp = ((long)(intervalData.getNumIntervals()*
			    intervalData.getIntervalSize()*
			    1000000));
	if (temp > totalTime) {
	  totalTime = temp;
	}
      }
      // From log files
      if (hasLogFiles()) {
	long temp = 
	  logLoader.determineEndTime(getValidProcessorList(ProjMain.LOG));
	if (temp > totalTime) {
	  totalTime = temp;
	}
      }
      rcReader.setValue("RC_GLOBAL_END_TIME", new Long(totalTime));
    }
    
    // Find Pose End Time Data
    if ((rcReader.RC_POSE_REAL_TIME.longValue() >= 0) &&
	(rcReader.RC_POSE_VIRT_TIME.longValue() >= 0)) {
      poseTotalTime = rcReader.RC_POSE_REAL_TIME.longValue();
      poseTotalVirtualTime = rcReader.RC_POSE_VIRT_TIME.longValue();
    } else {
    	if (hasPoseDopFiles()) {
    		final SwingWorker worker = new SwingWorker() {
    			public Object doInBackground() {
    				poseTotalTime = dopReader.getTotalRealTime();
    				poseTotalVirtualTime = dopReader.getTotalVirtualTime();
    				return null;
    			}
    			public void done() {
    				rcReader.setValue("RC_POSE_REAL_TIME", new Long(poseTotalTime));
    				rcReader.setValue("RC_POSE_VIRT_TIME", new Long(poseTotalVirtualTime));	    }
    		};
    		worker.execute();
    	}
    }
  }
  
  /**
   *  Read or initialize color maps
   */
  public void loadColors() throws Exception {
	  String colorsaved = getLogDirectory() + File.separator + "savedcolors.prj";
	  colorManager = new ColorManager(colorsaved, this);
	  try {
		  activityColors = colorManager.initializeColors();
		  entryColors = activityColors[PROJECTIONS];
		  userEventColors = activityColors[USER_EVENTS];
		  functionColors = activityColors[FUNCTIONS];
	  }
	  catch (Exception e) {
		  setDefaultColors();
		  throw e;
	  }
  }
  
  public void setDefaultColors() {
	  activityColors = colorManager.defaultColorMap();
	  entryColors = activityColors[PROJECTIONS];
	  userEventColors = activityColors[USER_EVENTS];
	  functionColors = activityColors[FUNCTIONS];

  }
  
  public void saveColors() {
  	colorManager.saveColors();
  }

    
  /**
   * Creating AMPI usage profile
   */
  protected void createAMPIUsage(int procId, long beginTime, 
			      long endTime, Vector procThdVec){
    try {
      if (hasLogFiles()) {
	if (logLoader == null) {
	  logLoader = new LogLoader();
	}
	logLoader.createAMPIUsageProfile(procId,beginTime,endTime,procThdVec);
      } else {
	System.err.println("createAMPIUsage: No log files available!");
      }
    } catch (LogLoadException e) {
      System.err.println("LOG LOAD EXCEPTION");	    
    }
  }

    /**
     * Create AMPI Functions' Time profile
     */
  protected void createAMPITimeProfile(int procId, long beginTime, 
					     long endTime, Vector procThdVec){
        try {
	    if (hasLogFiles()) {
		if (logLoader == null) {
		    logLoader = new LogLoader();
		}
		logLoader.createAMPIFuncTimeProfile(procId,beginTime,
						    endTime,procThdVec);
	    } else {
		System.err.println("createAMPIUsage: No log files available!");
	    }
	} catch (LogLoadException e) {
	    System.err.println("LOG LOAD EXCEPTION");	    
	}
    }

//    public int getNumPhases() {
//	if (sumAnalyzer!=null)
//	    return sumAnalyzer.getPhaseCount();
//	return 0;
//    }



    /**
     *  "type" here refers to the type of usage data (ie. Idle, System Queue
     *  length or CPU usage).
     */
  protected int[][] getSystemUsageData(int type) {
	return systemUsageData[type];
    }

    public long getTotalTime() {
	return totalTime;
    }

    public void setTotalTime(long time) {
	totalTime = time;
    }

//    public long getPoseTotalTime() {
//	return poseTotalTime;
//    }
//
//    public long getPoseTotalVirtualTime() {
//	return poseTotalVirtualTime;
//    }
//
//    public PoseDopReader getPoseDopReader() {
//	return dopReader;
//    }

//    // yet another interval size hack. When am I ever going to end up
//    // finding the time to fix all these ...
//    public long getLogReaderIntervalSize() {
//	return logReaderIntervalSize;
//    }

    public double[][] getSummaryAverageData() {
	return sumAnalyzer.getSummaryAverageData();
    }

    public long getSummaryIntervalSize() {
	return sumAnalyzer.getIntervalSize();
    }

    public Color getEntryColor(int entryIdx) {
    	if (entryIdx == isIdle) {
    		Paint p = getIdleColor();
    		if (p instanceof GradientPaint)
    			return ((GradientPaint)p).getColor1();
    		else
    			return (Color)p;
    	}
    	else if (entryIdx == isOverhead) {
    		Paint p = getOverheadColor();
    		if (p instanceof GradientPaint)
    			return ((GradientPaint)p).getColor1();
    		else
    			return (Color)p;
    	}
    	else if (entryIdx < getSts().getEntryCount()) {
		    return entryColors[entryIdx];
		}
    	else
    		return null;
    }


    public void setEntryColor(int entryIdx, Color color) {
    	if (entryIdx == isIdle)
    		idle = color;
    	else if (entryIdx == isOverhead)
    		overhead = color;
    	else if (entryIdx < getSts().getEntryCount())
    		entryColors[entryIdx] = color;
    	else {
    		System.err.println("Warning: entry point index " + entryIdx +
    		" not found. Cannot set color");
    	}
    }

    /** ***************** Usage Profile ******************
	This data gives the amount of processor time used by 
	each entry point (indices 0..numUserEntries-1), idle
	(numUserEntries), packing (numUserEntries+1), and unpacking 
	(numUserEntries+2).
	
	The returned values are in percent CPU time spent. 
    */
    protected float[][] GetUsageData(int pnum, long begintime, 
					 long endtime, OrderedIntList phases) {
	if( hasLogFiles()) { //.log files
	    UsageCalc u=new UsageCalc();
	    return u.usage(pnum, begintime, endtime, getVersion() );
	}
	int numUserEntries=getSts().getEntryCount();
	long[][] data;
	long[][] phasedata;
	/*BAD: silently ignores begintime and endtime*/
	if( sumAnalyzer.getPhaseCount()>1 ) {
	phases.reset();
	data = sumAnalyzer.getPhaseChareTime( phases.nextElement() );
	if (phases.hasMoreElements()) {
	    while (phases.hasMoreElements() && ( pnum > -1 )) {
		phasedata = 
		    sumAnalyzer.getPhaseChareTime(phases.nextElement());
		for(int q=0; q<numUserEntries; q++) {
		    data[pnum][q] += phasedata[pnum][q];
		}
	    }
	}
	} else {
	data = sumAnalyzer.getChareTime();
	}
	float ret[][]=new float[2][numUserEntries+4];
	//Convert to percent-- .sum entries are always over the 
	// entire program run.
	double scale=100.0/getTotalTime();
	for (int q=0;q<numUserEntries;q++){
	ret[0][q]=(float)(scale*data[pnum][q]);
	// dummy value for message send time at the moment .. 
	// summary file reader needs to be fixed first
	ret[1][q] = (float )0.0; 
	}
	return ret;
    }
    
//    // a == entry point index, t == type of data
//    public int[][] getUserEntryData( int a, int t ) {
//	return userEntryData[ a ][ t ];
//    }

//    public boolean hasSystemMsgsData( int a, int t ) {
//	if (systemMsgsData==null) return false;
//	return null!=systemMsgsData[a][t];
//    }

//    /**
//       check if one user entry data of type t is there.
//    */
//    public boolean hasUserEntryData( int a, int t ) {
//	if (userEntryData==null) return false;
//	if (userEntryData[a][t] == null) return false;
//	for (int pe=0; pe<getNumProcessors(); pe++)
//	    if (userEntryData[a][t][pe] != null) return true;
//	return false;
//    }


    /**
     *  Load graph data for one or more processors.
     * 
     *  A parallel version of this has been created in projections.TimeProfile.ThreadedFileReader. 
     *  For other tools that need to be parallel, use that reader, or create a similar one.
     *
     */
    protected void LoadGraphData(long intervalSize, 
			      int intervalStart, int intervalEnd,
			      boolean byEntryPoint, 
			      OrderedIntList processorList) 
    {
	if( hasLogFiles()) { // .log files
	    LogReader logReader = new LogReader();
	    logReader.read(intervalSize, 
			   intervalStart, intervalEnd,
			   byEntryPoint, processorList, true, null);
	    systemUsageData = logReader.getSystemUsageData();
//	    systemMsgsData = logReader.getSystemMsgs();
//	    userEntryData = logReader.getUserEntries();
//	    logReaderIntervalSize = logReader.getIntervalSize();
	} else if (hasSumDetailFiles()) {
	    IntervalData intervalData = new IntervalData();
	    intervalData.loadIntervalData(intervalSize, intervalStart,
					  intervalEnd, byEntryPoint,
					  processorList);
	    systemUsageData = intervalData.getSystemUsageData();
//	    systemMsgsData = intervalData.getSystemMsgs();
//	    userEntryData = intervalData.getUserEntries();
	} else if (hasSumFiles()) { // no log files, so load .sum files
	    loadSummaryData(intervalSize, intervalStart, intervalEnd,
			    processorList);
	} else {
	    System.err.println("Error: No data Files found!!");
	}
    }
    

    // yet another version of summary load for processor subsets.
    private void loadSummaryData(long intervalSize, 
				int intervalStart, int intervalEnd,
				OrderedIntList processorList) {
	systemUsageData = new int[3][][];
	int[][][] temp = new int[3][][];
	temp[1] =
	    sumAnalyzer.getSystemUsageData(intervalStart, intervalEnd,
	                                   intervalSize);
	processorList.reset();
	systemUsageData[1] = 
	  new int[processorList.size()][intervalEnd-intervalStart+1];
	for (int pIdx=0; pIdx<processorList.size(); pIdx++) {
	  systemUsageData[1][pIdx] = 
	    temp[1][processorList.nextElement()];
	} 
    }
  
//    // wrapper method for default interval size.
//    public void loadSummaryData(int intervalStart, int intervalEnd) {
//	loadSummaryData(sumAnalyzer.getIntervalSize(), intervalStart,
//			intervalEnd);
//    }
//    
//    /**
//     *  wrapper method for default summary load (used by main window)
//     *  This has a "control" value to limit the number of intervals used.
//     *  GenericSummaryReader will then be used to independently read
//     *  and rebin each file (which will work for now) - summary files with
//     *  even more intervals than the 180k seen in current NAMD logs may
//     *  require dynamic rebinning on read.
//
//     *  **CWL** LOOKS LIKE THIS MIGHT BE COMPLETELY USELESS!
//     */
//    public void loadSummaryData() {
//	if (hasSumFiles()) { 
//	    int sizeInt=(int)(sumAnalyzer.getIntervalSize());
//	    int nInt=(int)(getTotalTime()/sizeInt);
//	    loadSummaryData(sizeInt, 0, nInt-1);
//	} else if (hasSumAccumulatedFile()) {
//	    // do nothing **HACK** - action taken later.
//	}
//    }

//    public double[][] getSumDetailData(int pe, int type) {
//	return intervalData.getData(pe, type);
//    }

//    public long searchTimeline( int n, int p, int e ) 
//    {
//	try {
//	    if (hasLogFiles()) {
//		if (logLoader == null) {
//		    logLoader = new LogLoader();
//		}
//		return logLoader.searchtimeline( p, e, n );
//	    } else {
//		System.err.println("No log files!");
//		return -1;
//	    }
//	}
//	catch( LogLoadException lle ) {
//	    System.err.println( "LogLoadException" );
//	    return -1;
//	}
//    }

    /** ******************* Accessor Methods ************** */

    // *** Version accessor ***
    public double getVersion() { 
	if (getSts() == null) {
	    return MainWindow.CUR_VERSION; 
	} else {
	    return getSts().getVersion();
	}
    }

    // *** Data File-related accessors (from sts reader) ***
    protected boolean hasSummaryData() {
	return (hasSumFiles() || hasSumAccumulatedFile());
    }

    public boolean hasLogData() {
	return hasLogFiles();
    }

    public boolean hasSumDetailData() {
	return hasSumDetailFiles();
    }

//    public boolean hasPoseDopData() {
//	return hasPoseDopFiles();
//    }

    public String getLogDirectory() {
    	return fileNameHandler.dirFromFile();
    }
    
    public String getLogWithoutExtensionOrDirectory() {
    	return fileNameHandler.withoutDir();
    }

    public String getFilename() { 
    	return fileNameHandler.getBaseName();
    }

    
    public String getBaseFilename() { 
    	return fileNameHandler.getBaseName();
    }   
    
    // *** Activity Management *** */

    protected int stringToActivity(String name) {
	if (name.equals("PROJECTIONS")) {
	    return PROJECTIONS;
	} else if (name.equals("USER_EVENTS")) {
	    return USER_EVENTS;
	} else if (name.equals("FUNCTIONS")) {
	    return FUNCTIONS;
	    //	} else if (name.equals("POSE_DOP")) {
	    //	    return ActivityManager.POSE_DOP;
	} else {
	    return -1;  // error condition
	}
    }

    public int getNumActivity(int type) {
	switch (type) {
	case PROJECTIONS:
	    return getNumUserEntries();
	case USER_EVENTS:
	    return getNumUserDefinedEvents();
	case FUNCTIONS:
	    return getNumFunctionEvents();
	}
	return 0;
    }


    // This version takes the contigious index used by many projections
    // tools and gets the name.
    public String getActivityNameByIndex(int type, int index) {
	String[] tempNames;
	switch (type) {
	case PROJECTIONS:
	    return getEntryNameByIndex(index);
	case USER_EVENTS:
	    tempNames = getUserEventNames();
	    return tempNames[index];
	case FUNCTIONS:
	    tempNames = getFunctionNames();
	    return tempNames[index];
	}
	return "";
    }

    // *** Run Data accessors (from sts reader) ***

    /** number of processors listed in sts file */
    public int getNumProcessors() {
	return getSts().getProcessorCount();
    }

    /** Number of entries in the STS file */
    public int getEntryCount() {
    	return getSts().getEntryCount();
    }
 
    public int getNumUserEntries() {
    	return getEntryCount();
    }
    
    public String getEntryNameByIndex(int epIdx) {
    	return getSts().getEntryNameByIndex(epIdx);
    }


	public int getEntryIDByName(String epName) {
    	return getSts().getEntryIDByName(epName);
	}
    
    /** Generate a shortened prettier version of the entry name, excluding the parameter list */
    public String getPrettyEntryNameByIndex(int epIdx) {
    	String full = getSts().getEntryNameByIndex(epIdx);
    	full = full.replace("_", " ");
    	int i = full.indexOf("(");
    	if(i!=-1){
    		return full.substring(0,i);
    	} else {
    		return full;
    	}
    }
    
    public String getEntryChareNameByIndex(int epIdx) {
    	return getSts().getEntryChareNameByIndex(epIdx);
    }

    public String getEntryFullNameByIndex(int epIdx) {
    	return getSts().getEntryFullNameByIndex(epIdx);
    }

    public String getEntryNameByID(int ID) {
    	return getSts().getEntryNameByID(ID);
    }
//
//    public String getEntryChareNameByID(int ID) {
//    	return getSts().getEntryChareNameByID(ID);
//    }

    /** Return the name specified in the STS file for the event with ID, as specified in STS file */
    public String getEntryFullNameByID(int ID, boolean sanitizeForHTML) {
    	if(sanitizeForHTML){
    		return SanitizeForHTML.sanitize(getSts().getEntryFullNameByID(ID));
    	} else {
    		return getSts().getEntryFullNameByID(ID);
    	}
    }
    
    public String getEntryFullNameByID(int ID) {
        return getEntryFullNameByID(ID, false);
    }
    
    public Integer getEntryIndex(Integer ID){
    	Integer result = getSts().getEntryIndex(ID);	
    	if(result != null)
    		return result;
    	else 
    		throw new RuntimeException("ERROR: log files and sts file are inconsistent. The log files refer to EP " + ID + " but the sts file doesn't contain an entry for that \"ENTRY CHARE\"\n");
    }
        
    public int getNumUserDefinedEvents() {
	return getSts().getNumUserDefinedEvents();
    }

    public int getUserDefinedEventIndex(int eventID) {
	return getSts().getUserEventIndex(eventID);
    }

    public String getUserEventName(int eventID) {
    	String name = getSts().getUserEventName(eventID);
    	if(name != null)
    		return name;		 
    	else
    		return "";
    }	

    public String[] getUserEventNames() {
    	return getSts().getUserEventNames();
    }

//    public int getNumPerfCounts() {
//	return getSts().getNumPerfCounts();
//    }

    public String[] getPerfCountNames() {
	return getSts().getPerfCountNames();
    }

    public int getNumFunctionEvents() {
	return getSts().getNumFunctionEvents();
    }

    public Color getFunctionColor(int eventID) {
	return functionColors[getSts().getFunctionEventIndex(eventID)];
    }

    public Color[] getFunctionColors() {
	return functionColors;
    }


    public String getFunctionName(int funcID) {
	return getSts().getFunctionEventDescriptor(funcID);
    }

    public String[] getFunctionNames() {
	return getSts().getFunctionEventDescriptors();
    }
    
//    /**
//     *  This applies to interval-based data. If none exists, an error
//     *  should be generated.
//     *  **CW** this error will currently only be a print error. In future
//     *  an exception should be designed for this.
//     */
//    public int getNumIntervals() {
//	if (intervalData == null) {
//	    System.err.println("No interval based data. " +
//			       "Call to getNumIntervals is invalid.");
//	    return -1;
//	} else {
//	    return intervalData.getNumIntervals();
//	}
//    }

//    /**
//     *  getIntervalSize applies only to interval-based data. The same
//     *  comments for getNumIntervals apply equally to this method.
//     */
//    public double getIntervalSize() {
//	if (intervalData == null) {
//	    System.err.println("No interval based data. " +
//			       "Call to getIntervalSize is invalid.");
//	    return -1.0;
//	} else {
//	    return intervalData.getIntervalSize();
//	}
//    }

    
    /// Get user event color given one of the potentially sparse ids used provided by the program
    public Color getUserEventColor(int eventID) {
    	if (getSts() != null) { 
    		Integer idx = getSts().getUserEventIndex(eventID);
    		if(idx!=null)
    			return userEventColors[idx.intValue()]; 
    	} 

    	return null; 
    }

    
    public void setUserEventColor(int eventID, Color c) {
    	if (getSts() != null) { 
    		Integer idx = getSts().getUserEventIndex(eventID);
    		if(idx!=null)
    			userEventColors[idx.intValue()] = c; 
    	} 
    }

    
    
    public Color[] getColorMap(int activityType) {
	return activityColors[activityType];
    }

    public Color[] getEPColorMap() {
	return entryColors;
    }


    /** jTimeAvailable(), getJStartTime(), getJEndTime()
     *  Used for storing user defined startTime and endTime 
     *	when jumping from TimelineWindow to other graphs
     */
    public void setJTimeAvailable(boolean jBoo) { jTimeAvailable = jBoo; }
    protected boolean checkJTimeAvailable()        { return jTimeAvailable; }
//    public void setJTime(long start, long end)  { jStartTime = start;
//    							 jEndTime = end; }
    public long getJStart()                     { return jStartTime; }
    public long getJEnd()                       { return jEndTime; }
    

//    public boolean hasPapi() {
//	return getSts().hasPapi();
//    }

    // ************** Public Accessors to File Information *************
    private String getValidProcessorString(int type) {
	return fileNameHandler.getValidProcessorString(type);
    }

    public OrderedIntList getValidProcessorList(int type) {
	return fileNameHandler.getValidProcessorList(type);
    }

    /**
     *  By default, return the first available valid processor string
     *  in the following preference order - log, summary, sumdetail.
     *  This is to support legacy codes that assume there is only
     *  one set of valid files.
     */
    public String getValidProcessorString() {
	if (hasLogFiles()) {
	    return getValidProcessorString(ProjMain.LOG);
	} else if (hasSumFiles()) {
	    return getValidProcessorString(ProjMain.SUMMARY);
	} else if (hasSumDetailFiles()) {
	    return getValidProcessorString(ProjMain.SUMDETAIL);
	} else if (hasPoseDopFiles()) {
	    return getValidProcessorString(ProjMain.DOP);
	} else {
	    return "";
	}
    }

    public OrderedIntList getValidProcessorList() {
	if (hasLogFiles()) {
	    return getValidProcessorList(ProjMain.LOG);
	} else if (hasSumFiles()) {
	    return getValidProcessorList(ProjMain.SUMMARY);
	} else if (hasSumDetailFiles()) {
	    return getValidProcessorList(ProjMain.SUMDETAIL);
	} else if (hasPoseDopFiles()) {
	    return getValidProcessorList(ProjMain.DOP);
	} else {
	    return null;
	}
    }

    /** This should be the only function to provide the log file name to the tools */
    public String getLogName(int pnum) {
    	return fileNameHandler.getCanonicalFileName(pnum, ProjMain.LOG);
    }   
    
    public File getLog(int pe) {
    	return fileNameHandler.getLogFile(pe);
    }   


    public String getSumName(int pnum) {
    	return fileNameHandler.getCanonicalFileName(pnum, ProjMain.SUMMARY);
    }

    public String getSumDetailName(int pnum) {
    	return fileNameHandler.getCanonicalFileName(pnum, ProjMain.SUMDETAIL);
    }


	public File getSumDetailLog(int pe) {
		File f = new File(getSumDetailName(pe));
		if(! f.isFile()){
			System.err.println("Sum Detail log does not appear to be a regular file: " + f.getAbsolutePath());
			return null;
		}
		return f;
	}
    
    public String getPoseDopName(int pnum) {
    	return fileNameHandler.getCanonicalFileName(pnum, ProjMain.DOP);
    }

    protected void closeRC() {
    	if (rcReader != null) {
    		rcReader.close();
    	}
    }

    // ************** Internal Data file(s) management routines ********

    public boolean hasLogFiles() {
	return fileNameHandler.hasLogFiles();
    }   

    public boolean hasSumFiles() {
	return fileNameHandler.hasSumFiles();
    }
   
    private boolean hasSumAccumulatedFile() {
	return fileNameHandler.hasSumAccumulatedFile();
    }

    public boolean hasSumDetailFiles() {
	return fileNameHandler.hasSumDetailFiles();
    }

    private boolean hasPoseDopFiles() {
	return fileNameHandler.hasPoseDopFiles();
    }

	public void setSts(StsReader sts) {
		this.sts = sts;
	}

	public StsReader getSts() {
		return sts;
	}


	public Paint getIdleColor() {
		return idle;
	}

	public Paint getOverheadColor() {
		return overhead;
	}
	
	public void setIdleColor(Color c) {
		idle = c;
	}
	
	public void setOverheadColor(Color c) {
		overhead = c;
	}

	public String getOutlierFilename() {
		return getBaseFilename() + ".outlier";	
	}


	
	
}
