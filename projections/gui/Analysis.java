package projections.gui;

import java.awt.*;
import java.io.*;
import java.util.*;

import projections.analysis.*;
import projections.guiUtils.*;
import projections.misc.*;

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
  
  private SumAnalyzer sumAnalyzer; //Only for .sum files
  
  PoseDopReader dopReader; //Only for .poselog files
  
  private IntervalData intervalData; // interval-based data

  private String baseName;
  
  // The total time (maxed) of a run across all processors.
  private long totalTime = 0;
  long poseTotalTime = 0;
  long poseTotalVirtualTime = 0;
  
  /******************* Graphs ***************/
  private int[][][] systemUsageData;
  private int[][][][] systemMsgsData;
  private int[][][][] userEntryData;
  private int[] bgData;
  
  // stupid hack to compensate for the fact that LogReaders are never
  // maintained inside Analysis.
  private long logReaderIntervalSize = -1;
  
  /****************** Jump from Timeline to graphs ******/
  // Used for storing user defined startTime and endTime when jumping from
  // TimelineWindow to other graphs
  private long jStartTime, jEndTime;
  private boolean jTimeAvailable;
  
  /** *************** Color Maps 6/27/2002 ************ */
  
  public Color background = Color.black;
  public Color foreground = Color.white;
  
  private Color[] entryColors;
  private Color[] userEventColors;
  private Color[] functionColors;
  private Color[][] activityColors =
  new Color[ActivityManager.NUM_ACTIVITIES][];
  
  private Color[] grayColors;
  private Color[] grayUserEventColors;
  
  private Color[] activeColorMap;
  protected Color[] activeUserColorMap;
  
  public Analysis() {
    // empty constructor for now. initAnalysis is still the "true"
    // constructor until multiple run data is supported.
  }
  
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
  public void initAnalysis(String filename, Component rootComponent) 
    throws IOException 
  {
    guiRoot = rootComponent;
    try {
      baseName = FileUtils.getBaseName(filename);
      sts = new StsReader(filename);
      
      // Version Check (Kind of a hack, since the format of the Sts file
      // can change between versions.
      if (sts.getVersion() > MainWindow.CUR_VERSION) {
	System.err.println("Projections Version [" + MainWindow.CUR_VERSION +
			   "] unable to handle files of Version [" +
			   sts.getVersion() + "].");
	System.err.println("Exiting.");
	System.exit(-1);
      }

      rcReader = 
	new ProjectionsConfigurationReader(filename);
      FileUtils.detectFiles(sts, baseName);
      
      // Projections Colors
      String colorsaved = 
	getLogDirectory() + File.separator + "savedcolors.prj";
      initColors(colorsaved);

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
	    public Object construct() {
	      poseTotalTime = dopReader.getTotalRealTime();
	      poseTotalVirtualTime = 
		dopReader.getTotalVirtualTime();
	      rcReader.setValue("RC_POSE_REAL_TIME", new Long(poseTotalTime));
	      rcReader.setValue("RC_POSE_VIRT_TIME", new Long(poseTotalVirtualTime));
	      return null;
	    }
	    public void finished() {
	      // nothing to do
	    }
	  };
	worker.start();
      }
    }
  }
  
  /**
   *  Read or initialize color maps
   */
  private void initColors(String colorsaved) 
    throws IOException
  {
    ColorManager.setDefaultLocation(colorsaved);
    if ((new File(colorsaved)).exists()) {
      activityColors = ColorManager.initializeColors(colorsaved);
      // fall back on error
      if (activityColors == null) {
	activityColors = ColorManager.initializeColors();
      }
    } else {
      activityColors = ColorManager.initializeColors();
    }
    entryColors = activityColors[ActivityManager.PROJECTIONS];
    userEventColors = activityColors[ActivityManager.USER_EVENTS];
    functionColors = activityColors[ActivityManager.FUNCTIONS];
    grayColors = 
      ColorManager.createGrayscaleColorMap(sts.getEntryCount());
    grayUserEventColors = 
      ColorManager.createGrayscaleColorMap(sts.getNumUserDefinedEvents());
    // default to full colors
    activeColorMap = entryColors;
    activeUserColorMap = userEventColors;
  }
    
  /**
   * Creating AMPI usage profile
   */
  public void createAMPIUsage(int procId, long beginTime, 
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
    public void createAMPITimeProfile(int procId, long beginTime, 
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

    public int getNumPhases() {
	if (sumAnalyzer!=null)
	    return sumAnalyzer.getPhaseCount();
	return 0;
    }

    /**
     *  categoryIdx (see LogReader) refers to the category of system messages
     *  that are logged. type refers to the time of data (eg. time) logged.
     *  note that "type" here is different from "type" in getSystemUsageData.
     */
    public int[][] getSystemMsgsData(int categoryIdx, int type) {
	return systemMsgsData[categoryIdx][type];
    }

    /**
     *  "type" here refers to the type of usage data (ie. Idle, System Queue
     *  length or CPU usage).
     */
    public int[][] getSystemUsageData(int type) {
	return systemUsageData[type];
    }

    public long getTotalTime() {
	return totalTime;
    }

    public void setTotalTime(long time) {
	totalTime = time;
    }

    public long getPoseTotalTime() {
	return poseTotalTime;
    }

    public long getPoseTotalVirtualTime() {
	return poseTotalVirtualTime;
    }

    public PoseDopReader getPoseDopReader() {
	return dopReader;
    }

    // yet another interval size hack. When am I ever going to end up
    // finding the time to fix all these ...
    public long getLogReaderIntervalSize() {
	return logReaderIntervalSize;
    }

    public double[] getSummaryAverageData() {
	return sumAnalyzer.getSummaryAverageData();
    }

    public long getSummaryIntervalSize() {
	return sumAnalyzer.getIntervalSize();
    }

    public Color getEntryColor(int entryIdx) {
	if (entryIdx < sts.getEntryCount()) {
	    return activeColorMap[entryIdx];
	}
	return null;
    }

    public void setEntryColor(int entryIdx, Color color) {
	if (entryIdx < sts.getEntryCount()) {
	    activeColorMap[entryIdx] = color;
	} else {
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
    public float[][] GetUsageData(int pnum, long begintime, 
					 long endtime, OrderedIntList phases) {
	if( hasLogFiles()) { //.log files
	    UsageCalc u=new UsageCalc();
	    return u.usage(pnum, begintime, endtime, getVersion() );
	}
	int numUserEntries=sts.getEntryCount();
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
    
    // a == entry point index, t == type of data
    public int[][] getUserEntryData( int a, int t ) {
	return userEntryData[ a ][ t ];
    }

    public boolean hasSystemMsgsData( int a, int t ) {
	if (systemMsgsData==null) return false;
	return null!=systemMsgsData[a][t];
    }

    /**
       check if one user entry data of type t is there.
    */
    public boolean hasUserEntryData( int a, int t ) {
	if (userEntryData==null) return false;
	if (userEntryData[a][t] == null) return false;
	for (int pe=0; pe<getNumProcessors(); pe++)
	    if (userEntryData[a][t][pe] != null) return true;
	return false;
    }

    public int[] getBGData() {
	return bgData;
    }

    /**
       replace LoadGraphData(), with one more parameter containing
       a list of processors to read.
       Two more parameters - intervalStart and intervalEnd added.
    */
    public void LoadGraphData(long intervalSize, 
			      int intervalStart, int intervalEnd,
			      boolean byEntryPoint, 
			      OrderedIntList processorList) 
    {
	if( hasLogFiles()) { // .log files
	    LogReader logReader = new LogReader();
	    logReader.read(intervalSize, 
			   intervalStart, intervalEnd,
			   byEntryPoint, processorList);
	    systemUsageData = logReader.getSystemUsageData();
	    systemMsgsData = logReader.getSystemMsgs();
	    userEntryData = logReader.getUserEntries();
	    logReaderIntervalSize = logReader.getIntervalSize();
	} else if (hasSumDetailFiles()) {
	    IntervalData intervalData = new IntervalData();
	    intervalData.loadIntervalData(intervalSize, intervalStart,
					  intervalEnd, byEntryPoint,
					  processorList);
	    systemUsageData = intervalData.getSystemUsageData();
	    systemMsgsData = intervalData.getSystemMsgs();
	    userEntryData = intervalData.getUserEntries();
	} else if (hasSumFiles()) { // no log files, so load .sum files
	    loadSummaryData(intervalSize, intervalStart, intervalEnd,
			    processorList);
	} else {
	    System.err.println("Error: No data Files found!!");
	}
    }
    
    /**
     *  **CW** Time to stop being stupid and use the new summary reader
     *  and gain more control over the reading process.
     */
    public void loadSummaryData(long intervalSize,
				int intervalStart, int intervalEnd) {
	systemUsageData = new int[3][][];
	systemUsageData[1] = 
	    sumAnalyzer.getSystemUsageData(intervalStart, intervalEnd, 
					   intervalSize);
    }

    // yet another version of summary load for processor subsets.
    public void loadSummaryData(long intervalSize, 
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
  
    // wrapper method for default interval size.
    public void loadSummaryData(int intervalStart, int intervalEnd) {
	loadSummaryData(sumAnalyzer.getIntervalSize(), intervalStart,
			intervalEnd);
    }

    /**
     *  wrapper method for default summary load (used by main window)
     *  This has a "control" value to limit the number of intervals used.
     *  GenericSummaryReader will then be used to independently read
     *  and rebin each file (which will work for now) - summary files with
     *  even more intervals than the 180k seen in current NAMD logs may
     *  require dynamic rebinning on read.

     *  **CWL** LOOKS LIKE THIS MIGHT BE COMPLETELY USELESS!
     */
    public void loadSummaryData() {
	if (hasSumFiles()) { 
	    int sizeInt=(int)(sumAnalyzer.getIntervalSize());
	    int nInt=(int)(getTotalTime()/sizeInt);
	    loadSummaryData(sizeInt, 0, nInt-1);
	} else if (hasSumAccumulatedFile()) {
	    // do nothing **HACK** - action taken later.
	}
    }

    public double[][] getSumDetailData(int pe, int type) {
	return intervalData.getData(pe, type);
    }

    public long searchTimeline( int n, int p, int e ) 
    {
	try {
	    if (hasLogFiles()) {
		if (logLoader == null) {
		    logLoader = new LogLoader();
		}
		return logLoader.searchtimeline( p, e, n );
	    } else {
		System.err.println("No log files!");
		return -1;
	    }
	}
	catch( LogLoadException lle ) {
	    System.err.println( "LogLoadException" );
	    return -1;
	}
    }

    /** ******************* Accessor Methods ************** */

    // *** Version accessor ***
    public double getVersion() { 
	if (sts == null) {
	    return MainWindow.CUR_VERSION; 
	} else {
	    return sts.getVersion();
	}
    }

    // *** Data File-related accessors (from sts reader) ***
    public boolean hasSummaryData() {
	return (hasSumFiles() || hasSumAccumulatedFile());
    }

    public boolean hasLogData() {
	return hasLogFiles();
    }

    public boolean hasSumDetailData() {
	return hasSumDetailFiles();
    }

    public boolean hasPoseDopData() {
	return hasPoseDopFiles();
    }

    public String getLogDirectory() {
    	return FileUtils.dirFromFile(baseName);
    }

    public String getFilename() { 
    	return baseName;
    }   
    
    // *** Activity Management *** */

    public int stringToActivity(String name) {
	if (name.equals("PROJECTIONS")) {
	    return ActivityManager.PROJECTIONS;
	} else if (name.equals("USER_EVENTS")) {
	    return ActivityManager.USER_EVENTS;
	} else if (name.equals("FUNCTIONS")) {
	    return ActivityManager.FUNCTIONS;
	    //	} else if (name.equals("POSE_DOP")) {
	    //	    return ActivityManager.POSE_DOP;
	} else {
	    return -1;  // error condition
	}
    }

    public int getNumActivity(int type) {
	switch (type) {
	case ActivityManager.PROJECTIONS:
	    return getNumUserEntries();
	case ActivityManager.USER_EVENTS:
	    return getNumUserDefinedEvents();
	case ActivityManager.FUNCTIONS:
	    return getNumFunctionEvents();
	}
	return 0;
    }

    // This version takes the event id (as logged, which may not be
    // contigious) and gets the name.
    public String getActivityNameByID(int type, int id) {
	switch (type) {
	case ActivityManager.PROJECTIONS:
	    return getEntryNameByID(id);
	case ActivityManager.USER_EVENTS:
	    return getUserEventName(id);
	case ActivityManager.FUNCTIONS:
	    return getFunctionName(id);
	}
	return "";
    }

    // This version takes the contigious index used by many projections
    // tools and gets the name.
    public String getActivityNameByIndex(int type, int index) {
	String[] tempNames;
	switch (type) {
	case ActivityManager.PROJECTIONS:
	    return getEntryNameByIndex(index);
	case ActivityManager.USER_EVENTS:
	    tempNames = getUserEventNames();
	    return tempNames[index];
	case ActivityManager.FUNCTIONS:
	    tempNames = getFunctionNames();
	    return tempNames[index];
	}
	return "";
    }

    // *** Run Data accessors (from sts reader) ***

    /** number of processors listed in sts file */
    public int getNumProcessors() {
	return sts.getProcessorCount();
    }

    /** Number of entries in the STS file */
    public int getEntryCount() {
    	return sts.getEntryCount();
    }
 
    public int getNumUserEntries() {
    	return getEntryCount();
    }
    
    public String getEntryNameByIndex(int epIdx) {
    	return sts.getEntryNameByIndex(epIdx);
    }

    public String getEntryChareNameByIndex(int epIdx) {
    	return sts.getEntryChareNameByIndex(epIdx);
    }

    public String getEntryFullNameByIndex(int epIdx) {
    	return sts.getEntryFullNameByIndex(epIdx);
    }

    public String getEntryNameByID(int ID) {
    	return sts.getEntryNameByID(ID);
    }

    public String getEntryChareNameByID(int ID) {
    	return sts.getEntryChareNameByID(ID);
    }

    public String getEntryFullNameByID(int ID) {
    	return sts.getEntryFullNameByID(ID);
    }
    
    public Integer getEntryIndex(Integer ID){
    	return sts.getEntryIndex(ID);
    }
    
    
    public int getNumUserDefinedEvents() {
	return sts.getNumUserDefinedEvents();
    }

    public int getUserDefinedEventIndex(int eventID) {
	return sts.getUserEventIndex(eventID);
    }

    public String getUserEventName(int eventID) {
	return sts.getUserEventName(eventID); 
    }
    
    public String[] getUserEventNames() {
	return sts.getUserEventNames();
    }

    public int getNumPerfCounts() {
	return sts.getNumPerfCounts();
    }

    public String[] getPerfCountNames() {
	return sts.getPerfCountNames();
    }

    public int getNumFunctionEvents() {
	return sts.getNumFunctionEvents();
    }

    public Color getFunctionColor(int eventID) {
	return functionColors[sts.getFunctionEventIndex(eventID)];
    }

    public Color[] getFunctionColors() {
	return functionColors;
    }

    public void saveColors() {
	ColorManager.saveColors(activityColors);
    }

    public String getFunctionName(int funcID) {
	return sts.getFunctionEventDescriptor(funcID);
    }

    public String[] getFunctionNames() {
	return sts.getFunctionEventDescriptors();
    }
    
    /**
     *  This applies to interval-based data. If none exists, an error
     *  should be generated.
     *  **CW** this error will currently only be a print error. In future
     *  an exception should be designed for this.
     */
    public int getNumIntervals() {
	if (intervalData == null) {
	    System.err.println("No interval based data. " +
			       "Call to getNumIntervals is invalid.");
	    return -1;
	} else {
	    return intervalData.getNumIntervals();
	}
    }

    /**
     *  getIntervalSize applies only to interval-based data. The same
     *  comments for getNumIntervals apply equally to this method.
     */
    public double getIntervalSize() {
	if (intervalData == null) {
	    System.err.println("No interval based data. " +
			       "Call to getIntervalSize is invalid.");
	    return -1.0;
	} else {
	    return intervalData.getIntervalSize();
	}
    }

    public Color getUserEventColor(int eventID) {
    	if (sts != null) { 
    		Integer idx = sts.getUserEventIndex(eventID);
    		if(idx!=null)
    			return userEventColors[idx.intValue()]; 
    	} 

    	return null; 
    }

    
    public void setUserEventColor(int eventID, Color c) {
    	if (sts != null) { 
    		Integer idx = sts.getUserEventIndex(eventID);
    		if(idx!=null)
    			userEventColors[idx.intValue()] = c; 
    	} 
    }

    
    // *** Derived Data accessors ***

    // *** "Projected" sum detail data accessors ***
    // These methods return a collapsed (accumulated across one or more
    // dimensions) part of the sum detail data.

    /**
     *  This version of getDataSummedAcrossProcessors outputs a 2D array
     *  of double values with the first dimension indexed by ep id and
     *  the second dimension indexed by interval id.
     *
     *  This should be slightly more efficient when acquiring data for
     *  the full range of EPs.
     */
    public double[][] getDataSummedAcrossProcessors(int type,
							   OrderedIntList pes,
							   int startInterval,
							   int endInterval) {
	return intervalData.getDataSummedAcrossProcessors(type, pes,
							  startInterval,
							  endInterval);
    }

    /**
     *  getDataSummedAcrossProcessors outputs a vector of double[] with
     *  the vector representing possibly non-contigious EPs. The arrays
     *  are indexed by interval id.
     */
    public Vector getDataSummedAcrossProcessors(int type,
						       OrderedIntList pes,
						       int startInterval,
						       int endInterval,
						       OrderedIntList eps) {
	return intervalData.getDataSummedAcrossProcessors(type, pes,
							  startInterval,
							  endInterval,
							  eps);
    }

    public Color[] getColorMap(int activityType) {
	return activityColors[activityType];
    }

    public Color[] getColorMap() {
	return activeColorMap;
    }

    public void setFullColor() {
	activeColorMap = entryColors;
	activeUserColorMap = userEventColors;
    }

    public void setGrayscale() {
	activeColorMap = grayColors;
	activeUserColorMap = grayUserEventColors;
    }

    /** jTimeAvailable(), getJStartTime(), getJEndTime()
     *  Used for storing user defined startTime and endTime 
     *	when jumping from TimelineWindow to other graphs
     */
    public void setJTimeAvailable(boolean jBoo) { jTimeAvailable = jBoo; }
    public boolean checkJTimeAvailable()        { return jTimeAvailable; }
    public void setJTime(long start, long end)  { jStartTime = start;
    							 jEndTime = end; }
    public long getJStart()                     { return jStartTime; }
    public long getJEnd()                       { return jEndTime; }
    

    public boolean hasPapi() {
	return sts.hasPapi();
    }

    // ************** Public Accessors to File Information *************
    public String getValidProcessorString(int type) {
	return FileUtils.getValidProcessorString(type);
    }

    public OrderedIntList getValidProcessorList(int type) {
	return FileUtils.getValidProcessorList(type);
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

    public String getLogName(int pnum) {
	return FileUtils.getFileName(baseName, pnum, ProjMain.LOG);
    }   

    public String getSumName(int pnum) {
	return FileUtils.getFileName(baseName, pnum, ProjMain.SUMMARY);
    }   
    
    public String getSumAccumulatedName() {
	return FileUtils.getSumAccumulatedName(baseName);
    }

    public String getSumDetailName(int pnum) {
	return FileUtils.getFileName(baseName, pnum, ProjMain.SUMDETAIL);
    }

    public String getPoseDopName(int pnum) {
	return FileUtils.getFileName(baseName, pnum, ProjMain.DOP);
    }

    public void closeRC() {
	if (rcReader != null) {
	    rcReader.close();
	}
    }

    // ************** Internal Data file(s) management routines ********

    private boolean hasLogFiles() {
	return FileUtils.hasLogFiles();
    }   

    private boolean hasSumFiles() {
	return FileUtils.hasSumFiles();
    }
   
    private boolean hasSumAccumulatedFile() {
	return FileUtils.hasSumAccumulatedFile();
    }

    private boolean hasSumDetailFiles() {
	return FileUtils.hasSumDetailFiles();
    }

    private boolean hasPoseDopFiles() {
	return FileUtils.hasPoseDopFiles();
    }
}
