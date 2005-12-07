package projections.gui;

import java.awt.*;
import javax.swing.*;

import java.io.*;
import java.util.*;

import projections.analysis.*;
import projections.misc.*;
import projections.guiUtils.*;

/**
 *  Analysis
 *  Modified by Chee Wai Lee
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

    // static definitions for filetypes
    public static final int NUM_FILETYPES = 3;
    public static final int FTYPE_SUMMARY = 0;
    public static final int FTYPE_SUMDETAIL = 1;
    public static final int FTYPE_LOG = 2;

    // allows user definitions for foreground and background colors of
    // gui tools.
    public static Color background = Color.black;
    public static Color foreground = Color.white;
    
    /******************* Initialization ************/
    public static Component guiRoot;
    public static ProjectionsConfigurationReader rcReader;

    private static StsReader sts;
    
    private static LogLoader logLoader;  //Only for .log files
    
    private static SumAnalyzer sumAnalyzer; //Only for .sum files

    private static SumDetailReader summaryDetails[]; // .sumd files
	
    private static PoseDopReader dopReader; //Only for .poselog files

    private static IntervalData intervalData; // interval-based data
    public static ActivityManager      activityManager;

    public static final int NUM_TYPES = 5;
    public static final int LOG = 0;
    public static final int SUMMARY = 1;
    public static final int COUNTER = 2;
    public static final int SUMDETAIL = 3;
    public static final int DOP = 4;

    private static String baseName;
    private static String logDirectory;

    private static boolean hasSum;
    private static boolean hasSumDetail;
    private static boolean hasSumAccumulated;
    private static boolean hasLog;
    private static boolean hasPoseDop;    

    private static OrderedIntList validPEs[];
    private static String validPEStrings[];

    // The total time (maxed) of a run across all processors.
    private static long totalTime = 0;
    private static long poseTotalTime = 0;
    private static long poseTotalVirtualTime = 0;

    /******************* Graphs ***************/
    private static int[][][] systemUsageData;
    private static int[][][][] systemMsgsData;
    private static int[][][][] userEntryData;
    private static int[] bgData;

    // stupid hack to compensate for the fact that LogReaders are never
    // maintained inside Analysis.
    private static long logReaderIntervalSize = -1;

    /** *************** Color Maps 6/27/2002 ************ */
    private static Color[] entryColors;
    // moved from StsReader because it does not belong there.
    // Indexed by userevent index (which can be derived from the user
    // event ID).
    private static Color[] grayColors;
    private static Color[] userEventColors;
    private static Color[] grayUserEventColors;

    private static Color[] activeColorMap;
    private static Color[] activeUserColorMap;
    
    private static Color[] functionColors;

    // *CW* Major hack - make functions and entry methods share the same 
    // color set.
    private static Color[] universalColors;

    /****************** Jump from Timeline to graphs ******/
    // Used for storing user defined startTime and endTime when jumping from
    // TimelineWindow to other graphs
    private static long jStartTime, jEndTime;
    private static boolean jTimeAvailable;

    // **CW** Hack - potentially expensive.
    private static void splitUniversalColors() {
	int numEntries = sts.getEntryCount();
	entryColors = new Color[numEntries];
	for (int i=0; i<numEntries; i++) {
	    entryColors[i] = universalColors[i];
	}
	int numFunc = sts.getNumFunctionEvents();
	functionColors = new Color[numFunc];
	for (int i=0; i<numFunc; i++) {
	    functionColors[i] = universalColors[i+numEntries];
	}
    }

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
    public static void initAnalysis(String filename, Component rootComponent) 
	throws IOException 
    {
	guiRoot = rootComponent;
	activityManager = new ActivityManager();
	try {
	    baseName = getBaseName(filename);
	    logDirectory = dirFromFile(filename);
	    sts=new StsReader(filename);
	    rcReader = 
		new ProjectionsConfigurationReader(getFilename(),
						   getLogDirectory());
	    detectFiles();

	    //	    activityManager.registrationDone();
	    // if I can find the saved color maps, then use it.
	    String colorsaved = 
		getLogDirectory() + File.separator + "savedcolors.prj";
	    ColorSaver.setLocation(colorsaved);
	    if ((new File(colorsaved)).exists()) {
		try {
		    universalColors = ColorSaver.loadColors();
		    splitUniversalColors();

		    // ORTHOGONAL SYSTEM OF COLORS - FOR NOW
		    // SPECIAL NOTE: This assumes the colors were
		    // saved using the new color system. otherwise,
		    // bad things can be expected to happen.
		    //		    activityManager.setColors(entryColors);
		} catch (IOException exception) {
		    System.err.println("Failed to load colors!!!");
		    universalColors = 
			createColorMap(sts.getEntryCount()+
				       sts.getNumFunctionEvents());
		    splitUniversalColors();
		}
	    } else {
		// create default color maps for entry methods as well as user
		// events.
		// entryColors = createColorMap(sts.getEntryCount());
		// functionColors = createColorMap(sts.getNumFunctionEvents());
		universalColors = 
		    createColorMap(sts.getEntryCount()+
				   sts.getNumFunctionEvents());
		splitUniversalColors();
	    }
	    grayColors = createGrayscaleColorMap(sts.getEntryCount());
	    userEventColors = createColorMap(sts.getNumUserDefinedEvents());
	    grayUserEventColors = 
		createGrayscaleColorMap(sts.getNumUserDefinedEvents());
	    // default to full colors
	    activeColorMap = entryColors;
	    activeUserColorMap = userEventColors;
	} catch (LogLoadException e) {
	    // if sts reader could not be created because of a log load
	    // exception, forward the error as an IOException.
	    // LogLoadException is a silly exception in the first place!
	    throw new IOException(e.toString());
	}

	// Summary Files are handled orthogonally with the current scheme
	// which should just have 2 types of data - interval data and point
	// data.

	// Read the super summarized data where available.
	if (hasSumAccumulatedFile()) { // SINGLE <stsname>.sum file
	    // clear memory first ...
	    sumAnalyzer = null;
	    sumAnalyzer = new SumAnalyzer(sts, SumAnalyzer.ACC_MODE);
	    setTotalTime(sumAnalyzer.GetTotalTime());
	}
	if( hasSumFiles() ) { //.sum files
	    try {
		// clear memory first ...
		sumAnalyzer = null;
		sumAnalyzer = new SumAnalyzer();
		setTotalTime(sumAnalyzer.GetTotalTime());
	    } catch (SummaryFormatException e) {
		System.err.println(e.toString());
	    }
	}
	// setting up interval-based data
	if (hasSumDetailFiles() || hasLogFiles()) {
	    if (intervalData == null) {
		intervalData = new IntervalData();
	    }
	    setTotalTime((long)(intervalData.getNumIntervals()*
				intervalData.getIntervalSize()*
				1000000));
	}
	// setting up point-based data.
	// **CW** for backward compatibility - initialize the log files
	// by creating the log reader so that the timing information is
	// available to the older tools.
	if (hasLogFiles()) {
	    logLoader = new LogLoader();
	}
	// if pose data exists, compute the end times
	if (hasPoseDopFiles()) {
	    dopReader = new PoseDopReader();
	    final SwingWorker worker =  new SwingWorker() {
		    public Object construct() {
			poseTotalTime = dopReader.getTotalRealTime();
			poseTotalVirtualTime = 
			    dopReader.getTotalVirtualTime();
			return null;
		    }
		    public void finished() {
			// nothing to do
		    }
		};
	    worker.start();
	}
    }
    
    /****************** Timeline ******************/
    public static Vector createTL(int p, long bt, long et, 
				  Vector timelineEvents, Vector userEvents) {
	try {
	    if (hasLogFiles()) {
		if (logLoader == null) {
		    logLoader = new LogLoader();
		}
		return logLoader.createtimeline(p, bt, et, timelineEvents, 
						userEvents);
	    } else {
		System.err.println("createTL: No log files available!");
		return null;
	    }
	} catch (LogLoadException e) {
	    System.err.println("LOG LOAD EXCEPTION");
	    return null;
	}
    }

    public static int[][] getAnimationData(long intervalSize, 
					   long startTime, long endTime, 
					   OrderedIntList desiredPEs) {
	if (intervalSize >= endTime-startTime) {
	    intervalSize = endTime-startTime;
	}
	int startI = (int)(startTime/intervalSize);
	int endI = (int)(endTime/intervalSize);
	int numPs = desiredPEs.size();
	
	LoadGraphData(intervalSize,startI,endI-1,false, null);
	int[][] animationdata = new int[ numPs ][ endI-startI ];
	
	int pInfo = desiredPEs.nextElement();
	int p = 0;
	
	while(pInfo != -1){
	    for( int t = 0; t <(endI-startI); t++ ){
		animationdata[ p ][ t ] = getSystemUsageData(1)[ pInfo ][ t ];
	    }
	    pInfo = desiredPEs.nextElement();
	    p++;
	}
	
	return animationdata;
    }

    /**************** Utility/Access *************/
    public static String[][] getLogFileText( int num ) {
	if (!(hasLogFiles())) {
	    return null;
	} else {
	    Vector v = null;
	    try {
		if (logLoader == null) {
		    logLoader = new LogLoader();
		}
		v = logLoader.view(num);
	    } catch (LogLoadException e) {
		System.err.println("Failed to load Log files");
		return null;
	    }
	    if( v == null ) {
		return null;
	    }
	    int length = v.size();
	    if( length == 0 ) {
		return null;
	    }
	    String[][] text = new String[ length ][ 2 ];
	    ViewerEvent ve;
	    for( int i = 0;i < length;i++ ) {
		ve = (ViewerEvent)v.elementAt(i);
		text[ i ][ 0 ] = "" + ve.Time;
		switch( ve.EventType ) {
		case ( ProjDefs.CREATION ):
		    text[ i ][ 1 ] = "CREATE message to be sent to " + ve.Dest;
		    break;
		case ( ProjDefs.BEGIN_PROCESSING ):
		    text[ i ][ 1 ] = "BEGIN PROCESSING of message sent to " + 
			ve.Dest;
		    text[ i ][ 1 ] += " from processor " + ve.SrcPe;
		    break;
		case ( ProjDefs.END_PROCESSING ):
		    text[ i ][ 1 ] = "END PROCESSING of message sent to " + 
			ve.Dest;
		    text[ i ][ 1 ] += " from processor " + ve.SrcPe;
		    break;
		case ( ProjDefs.ENQUEUE ):
		    text[ i ][ 1 ] = "ENQUEUEING message received from " +
			"processor " + ve.SrcPe + " destined for " + ve.Dest;
		    break;
		case ( ProjDefs.BEGIN_IDLE ):
		    text[ i ][ 1 ] = "IDLE begin";
		    break;
		case ( ProjDefs.END_IDLE ):
		    text[ i ][ 1 ] = "IDLE end";
		    break;
		case ( ProjDefs.BEGIN_PACK ):
		    text[ i ][ 1 ] = "BEGIN PACKING a message to be sent";
		    break;
		case ( ProjDefs.END_PACK ):
		    text[ i ][ 1 ] = "FINISHED PACKING a message to be sent";
		    break;
		case ( ProjDefs.BEGIN_UNPACK ):
		    text[ i ][ 1 ] = "BEGIN UNPACKING a received message";
		    break;
		case ( ProjDefs.END_UNPACK ):
		    text[ i ][ 1 ] = "FINISHED UNPACKING a received message";
		    break;
		default:
		    text[ i ][ 1 ] = "!!!! ADD EVENT TYPE " + ve.EventType +
			" !!!";
		    break;
		}
	    }
	    return text;
	}
    }

    public static int getNumPhases() {
	if (sumAnalyzer!=null)
	    return sumAnalyzer.GetPhaseCount();
	else
	    return 0;
    }

    /**
     *  categoryIdx (see LogReader) refers to the category of system messages
     *  that are logged. type refers to the time of data (eg. time) logged.
     *  note that "type" here is different from "type" in getSystemUsageData.
     */
    public static int[][] getSystemMsgsData(int categoryIdx, int type) {
	return systemMsgsData[categoryIdx][type];
    }

    /**
     *  "type" here refers to the type of usage data (ie. Idle, System Queue
     *  length or CPU usage).
     */
    public static int[][] getSystemUsageData(int type) {
	return systemUsageData[type];
    }

    public static long getTotalTime() {
	return totalTime;
    }

    public static void setTotalTime(long time) {
	totalTime = time;
    }

    public static long getPoseTotalTime() {
	return poseTotalTime;
    }

    public static long getPoseTotalVirtualTime() {
	return poseTotalVirtualTime;
    }

    public static PoseDopReader getPoseDopReader() {
	return dopReader;
    }

    // yet another interval size hack. When am I ever going to end up
    // finding the time to fix all these ...
    public static long getLogReaderIntervalSize() {
	return logReaderIntervalSize;
    }

    public static double[] getSummaryAverageData() {
	return sumAnalyzer.getSummaryAverageData();
    }

    public static long getSummaryIntervalSize() {
	return sumAnalyzer.getIntervalSize();
    }

    public static Color getEntryColor(int entryIdx) {
	if (entryIdx < sts.getEntryCount()) {
	    return activeColorMap[entryIdx];
	} else {
	    return null;
	}
    }

    public static void setEntryColor(int entryIdx, Color color) {
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
    public static float[][] GetUsageData(int pnum, long begintime, 
					 long endtime, OrderedIntList phases) {
	if( hasLogFiles()) { //.log files
	    UsageCalc u=new UsageCalc();
	    return u.usage(pnum, begintime, endtime, getVersion() );
	} else /*The files are sum files*/ {
	    int temp,numUserEntries=sts.getEntryCount();
	    long[][] data;
	    long[][] phasedata;
	    /*BAD: silently ignores begintime and endtime*/
	    if( sumAnalyzer.GetPhaseCount()>1 ) {
		phases.reset();
		data = sumAnalyzer.GetPhaseChareTime( phases.nextElement() );
		if (phases.hasMoreElements()) {
		    while (phases.hasMoreElements() && ( pnum > -1 )) {
			phasedata = 
			    sumAnalyzer.GetPhaseChareTime(phases.nextElement());
			for(int q=0; q<numUserEntries; q++) {
			    data[pnum][q] += phasedata[pnum][q];
			}
		    }
		}
	    } else {
		data = sumAnalyzer.GetChareTime();
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
    }
    
    // a == entry point index, t == type of data
    public static int[][] getUserEntryData( int a, int t ) {
	return userEntryData[ a ][ t ];
    }

    public static boolean hasSystemMsgsData( int a, int t ) {
	if (systemMsgsData==null) return false;
	return null!=systemMsgsData[a][t];
    }

    /**
       check if one user entry data of type t is there.
    */
    public static boolean hasUserEntryData( int a, int t ) {
	if (userEntryData==null) return false;
	if (userEntryData[a][t] == null) return false;
	for (int pe=0; pe<getNumProcessors(); pe++)
	    if (userEntryData[a][t][pe] != null) return true;
	return false;
    }

    public static int[] getBGData() {
	return bgData;
    }

    /**
       replace LoadGraphData(), with one more parameter containing
       a list of processors to read.
       Two more parameters - intervalStart and intervalEnd added.
    */
    public static void LoadGraphData(long intervalSize, 
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
	    logReader=null;
	} else if (hasSumDetailFiles()) {
	    // **CW**
	    // sum detail not truly available yet, so use summary for now.
	    if (hasSumFiles()) {
		loadSummaryData(intervalSize, intervalStart, intervalEnd);
	    }
	} else if (hasSumFiles()) { // no log files, so load .sum files
	    loadSummaryData(intervalSize, intervalStart, intervalEnd);
	} else {
	    System.err.println("Error: No data Files found!!");
	}
    }
    
    /**
     *  **CW** Time to stop being stupid and use the new summary reader
     *  and gain more control over the reading process.
     */
    public static void loadSummaryData(long intervalSize,
				       int intervalStart, int intervalEnd) {
	systemUsageData = new int[3][][];
	try {
	    systemUsageData[1] = 
		sumAnalyzer.GetSystemUsageData(intervalStart, intervalEnd, 
					       intervalSize);
	} catch (SummaryFormatException E) {
	    System.err.println("Caught SummaryFormatException");
	} catch (IOException e) {
	    System.err.println("Caught IOExcpetion");
	}
    }

    // wrapper method for default interval size.
    public static void loadSummaryData(int intervalStart, int intervalEnd) {
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
     */
    public static void loadSummaryData() {
	if (hasSumFiles()) { 
	    int sizeInt=(int)(sumAnalyzer.getIntervalSize());
	    int nInt=(int)(getTotalTime()/sizeInt);
	    loadSummaryData(sizeInt, 0, nInt-1);
	} else if (hasSumAccumulatedFile()) {
	    // do nothing **HACK** - action taken later.
	}
    }

    public static double[][] getSumDetailData(int pe, int type) {
	return intervalData.getData(pe, type);
    }

    public static long searchTimeline( int n, int p, int e ) 
	throws EntryNotFoundException 
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
    public static double getVersion() { 
	if (sts == null) {
	    return MainWindow.CUR_VERSION; 
	} else {
	    return sts.getVersion();
	}
    }

    // *** Data File-related accessors (from sts reader) ***
    public static boolean hasSummaryData() {
	return (hasSumFiles() || hasSumAccumulatedFile());
    }

    public static boolean hasLogData() {
	return hasLogFiles();
    }

    public static boolean hasSumDetailData() {
	return hasSumDetailFiles();
    }

    public static boolean hasPoseDopData() {
	return hasPoseDopFiles();
    }

    public static String getLogDirectory() {
	return logDirectory;
    }

    public static String getFilename() { 
	return baseName;
    }   
    
    public static String getLogName(int pnum) {
	return baseName+"."+pnum+".log";
    }   

    public static String getSumName(int pnum) {
	return baseName+"."+pnum+".sum";
    }   
    
    public static String getSumAccumulatedName() {
	return baseName+".sum";
    }

    public static String getSumDetailName(int pnum) {
	return baseName + "." + pnum + ".sumd";
    }

    public static String getPoseDopName(int pnum) {
	return baseName + "." + pnum + ".poselog";
    }

    // *** Run Data accessors (from sts reader) ***
    public static int getNumProcessors() {
	return sts.getProcessorCount();
    }

    public static int getNumUserEntries() {
	return sts.getEntryCount();
    }

    public static String[][] getEntryNames() {
	return sts.getEntryNames();
    }

    public static String getEntryName(int epIdx) {
	return (sts.getEntryNames())[epIdx][0];
    }

    public static String getEntryChareName(int epIdx) {
	return (sts.getEntryNames())[epIdx][1];
    }

    public static int getNumUserDefinedEvents() {
	return sts.getNumUserDefinedEvents();
    }

    public static int getUserDefinedEventIndex(int eventID) {
	return sts.getUserEventIndex(eventID);
    }

    public static String getUserEventName(int eventID) {
	return sts.getUserEventName(eventID); 
    }
    
    public static String[] getUserEventNames() {
	return sts.getUserEventNames();
    }

    public static int getNumPerfCounts() {
	return sts.getNumPerfCounts();
    }

    public static String[] getPerfCountNames() {
	return sts.getPerfCountNames();
    }

    public static int getNumFunctionEvents() {
	return sts.getNumFunctionEvents();
    }

    public static Color getFunctionColor(int eventID) {
	return functionColors[sts.getFunctionEventIndex(eventID)];
    }

    public static Color[] getFunctionColors() {
	return functionColors;
    }

    public static String getFunctionName(int funcID) {
	return sts.getFunctionEventDescriptor(funcID);
    }

    public static String[] getFunctionNames() {
	return sts.getFunctionEventDescriptors();
    }
    
    /**
     *  This applies to interval-based data. If none exists, an error
     *  should be generated.
     *  **CW** this error will currently only be a print error. In future
     *  an exception should be designed for this.
     */
    public static int getNumIntervals() {
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
    public static double getIntervalSize() {
	if (intervalData == null) {
	    System.err.println("No interval based data. " +
			       "Call to getIntervalSize is invalid.");
	    return -1.0;
	} else {
	    return intervalData.getIntervalSize();
	}
    }

    public static Color getUserEventColor(int eventID) {
	if (sts != null) { 
	    return userEventColors[sts.getUserEventIndex(eventID)]; 
	} else { 
	    return null; 
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
    public static double[][] getDataSummedAcrossProcessors(int type,
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
    public static Vector getDataSummedAcrossProcessors(int type,
						       OrderedIntList pes,
						       int startInterval,
						       int endInterval,
						       OrderedIntList eps) {
	return intervalData.getDataSummedAcrossProcessors(type, pes,
							  startInterval,
							  endInterval,
							  eps);
    }

    /**
     *  By default, return the first available valid processor string
     *  in the following preference order - log, summary, sumdetail.
     *  This is to support legacy codes that assume there is only
     *  one set of valid files.
     */
    public static String getValidProcessorString() {
	if (hasLogFiles()) {
	    return getValidProcessorString(LOG);
	} else if (hasSumFiles()) {
	    return getValidProcessorString(SUMMARY);
	} else if (hasSumDetailFiles()) {
	    return getValidProcessorString(SUMDETAIL);
	} else if (hasPoseDopFiles()) {
	    return getValidProcessorString(DOP);
	} else {
	    return "";
	}
    }

    public static OrderedIntList getValidProcessorList() {
	if (hasLogFiles()) {
	    return getValidProcessorList(LOG);
	} else if (hasSumFiles()) {
	    return getValidProcessorList(SUMMARY);
	} else if (hasSumDetailFiles()) {
	    return getValidProcessorList(SUMDETAIL);
	} else if (hasPoseDopFiles()) {
	    return getValidProcessorList(DOP);
	} else {
	    return null;
	}
    }

    public static String getValidProcessorString(int type) {
	switch (type) {
	case LOG:
	    if (!hasLog) {
		System.err.println("Warning: No log files.");
	    }
	    break;
	case SUMMARY:
	    if (!hasSum) {
		System.err.println("Warning: No summary files.");
	    }
	    break;
	case SUMDETAIL:
	    if (!hasSumDetail) {
		System.err.println("Warning: No summary detail files.");
	    }
	    break;
	case DOP:
	    if (!hasPoseDop) {
		System.err.println("Warning: No poselog files found.");
	    }
	    break;
	}
	return validPEStrings[type];
    }

    public static OrderedIntList getValidProcessorList(int type) {
	switch (type) {
	case LOG:
	    if (!hasLog) {
		System.err.println("Warning: No log files.");
	    }
	    break;
	case SUMMARY:
	    if (!hasSum) {
		System.err.println("Warning: No summary files.");
	    }
	    break;
	case SUMDETAIL:
	    if (!hasSumDetail) {
		System.err.println("Warning: No summary detail files.");
	    }
	    break;
	case DOP:
	    if (!hasPoseDop) {
		System.err.println("Warning: No poselog files found.");
	    }
	    break;
	}
	return validPEs[type];
    }

    public static Color[] getColorMap() {
	return activeColorMap;
    }

    public static void setFullColor() {
	activeColorMap = entryColors;
	activeUserColorMap = userEventColors;
    }

    public static void setGrayscale() {
	activeColorMap = grayColors;
	activeUserColorMap = grayUserEventColors;
    }

    public static Color[] createGrayscaleColorMap(int numColors) {
	Color[] colors = new Color[numColors];
	float H = (float)1.0;
	float S = (float)0.0;
	float B = (float)0.9; // initial white value would be bad.
	float delta = (float)(0.8/numColors); // extreme black is also avoided
	// as long as S==0, H does not matter, so scale according to B
	for (int i=0; i<numColors; i++) {
	    colors[i] = Color.getHSBColor(H, S, B);
	    B -= delta;
	    if (B < 0.1) {
		B = (float)0.1;
	    }
	}
	return colors;
    }

    public static Color[] createColorMap(int numColors) {
	Color[] colors = new Color[numColors];
	float H = (float)1.0;
	float S = (float)1.0;
	float B = (float)1.0;
	float delta = (float)(1.0/numColors);
	for(int i=0; i<numColors; i++) {
	    colors[i] = Color.getHSBColor(H, S, B);
	    H -= delta;
	    if(H < 0.0) { H = (float)1.0; }
	}
	return colors;
    }

    /**
     *  Wrapper version for using a default weight assignment.
     */
    public static Color[] createColorMap(int numEPs, int epMap[]) {
	int numSignificant = epMap.length;
	int[] weights = new int[numSignificant];
	
	if (numSignificant > 0) {
	    // default assignment of weights using an accelerating increment
	    // method (acceleration = 2; initial value = 5)
	    int acceleration = 2;
	    int increment = 7;
	    weights[numSignificant-1] = 5;
	    for (int ep=numSignificant-2; ep>=0; ep--) {
		weights[ep] = weights[ep+1] + increment;
		increment += acceleration;
	    }
	}
	return createColorMap(numEPs, epMap, weights);
    }

    /**
     *  A more advanced version of color assignment that takes a map
     *  of significant entry methods in sorted order and assigns more
     *  distinctly different (hue) colors to more significant entry
     *  methods. Significance is assigned by the tool requesting the
     *  color map. This scheme is still arbitrary.
     *
     *  numEPs give a total of color assignments required.
     */
    public static Color[] createColorMap(int numEPs, int epMap[], 
					 int weights[]) {
	Color[] colors = new Color[numEPs];

	int numSignificant = epMap.length;
	// no significant values, so return uniform color map
	if (numSignificant == 0) {
	    return createColorMap(numEPs);
	}
	int total = 0;
	for (int ep=0; ep<numSignificant; ep++) {
	    total += weights[ep];
	}
	// a linear distribution segment of the remaining color space should 
	// not be larger than the smallest final hue segment assigned to a
	// significant ep. Formula: x >= 1.0/kc+1.0 where x is the hue space
	// allocated to significant eps, k is the % weight assigned to the
	// smallest significant ep and c is the number of insignificant eps.
	// x should be at least 66% of the hue space or it wouldn't make a
	// difference (when a small number of significant elements are
	// presented).
	double k = weights[numSignificant-1]/(double)total;
	int c = numEPs-numSignificant;
	double x = 1.0/(k*c + 1.0);
	if (x < 0.67) {
	    x = 0.67;
	}

	double currentHue = 1.0;
	double saturation = 1.0;
	double brightness = 1.0;
	// assign colors to significant eps
	for (int ep=0; ep<numSignificant; ep++) {
	    colors[epMap[ep]] = Color.getHSBColor((float)currentHue, 
						  (float)saturation,
						  (float)brightness);
	    currentHue -= (weights[ep]/(double)total)*x;
	}
	// assign colors to all other eps
	double delta = currentHue/c;
	for (int ep=0; ep<numEPs; ep++) {
	    // needs assignment
	    if (colors[ep] == null) {
		colors[ep] = Color.getHSBColor((float)currentHue, 
					       (float)saturation,
					       (float)brightness);
		currentHue -= delta;
	    }
	}
	return colors;
    }
    
    /** jTimeAvailable(), getJStartTime(), getJEndTime()
     *  Used for storing user defined startTime and endTime 
     *	when jumping from TimelineWindow to other graphs
     */
    public static void setJTimeAvailable(boolean jBoo) { jTimeAvailable = jBoo; }
    public static boolean checkJTimeAvailable()        { return jTimeAvailable; }
    public static void setJTime(long start, long end)  { jStartTime = start;
    							 jEndTime = end; }
    public static long getJStart()                     { return jStartTime; }
    public static long getJEnd()                       { return jEndTime; }
    

    public static boolean hasPapi() {
	return sts.hasPapi();
    }

    /**
     *  Internal Data file(s) management routines
     */
    private static void detectFiles() {
	// determine if any of the data files exist.
	// We assume they are automatically valid and this is reflected
	// in the validPEs. 
	// **FIXME** This is expensive with large numbers of processors!
	//           Use rc-type information to do this once per dataset
	//           lifetime.

	hasLog = false;
	hasSum = false;
	hasSumDetail = false;
	hasSumAccumulated = false;
	hasPoseDop = false;

	validPEs = new OrderedIntList[NUM_TYPES];
	validPEStrings = new String[NUM_TYPES];
	for (int i=0; i<NUM_TYPES; i++) {
	    validPEs[i] = new OrderedIntList();
	}

	for (int i=0;i<sts.getProcessorCount();i++) {
	    if ((new File(getSumName(i))).isFile()) {
		hasSum = true;
		validPEs[SUMMARY].insert(i);
	    }
	    if ((new File(getSumDetailName(i))).isFile()) {
		hasSumDetail = true;
		validPEs[SUMDETAIL].insert(i);
	    }
	    if ((new File(getLogName(i))).isFile()) {
		hasLog = true;
		validPEs[LOG].insert(i);
	    }
	    if ((new File(getPoseDopName(i))).isFile()) {
		hasPoseDop = true;
		validPEs[DOP].insert(i);
	    }
	}
	for (int type=0; type<NUM_TYPES; type++) {
	    validPEStrings[type] = validPEs[type].listToString();
	}
	if ((new File(getSumAccumulatedName())).isFile()) {
	    hasSumAccumulated = true;
	}
    }
    
    private static boolean hasLogFiles() {
	return hasLog;
    }   

    private static boolean hasSumFiles() {
	return hasSum;
    }
   
    private static boolean hasSumAccumulatedFile() {
	return hasSumAccumulated;
    }

    private static boolean hasSumDetailFiles() {
	return hasSumDetail;
    }

    private static boolean hasPoseDopFiles() {
	return hasPoseDop;
    }

    private static String getBaseName(String filename) {
	String baseName = null;
	if (filename.endsWith(".sum.sts")) {
	    baseName = filename.substring(0, filename.length()-8);
	} else if (filename.endsWith(".sts")) {
	    baseName = filename.substring(0, filename.length()-4); 
	} else {
	    System.err.println("Invalid sts filename! Exiting ...");
	    System.exit(-1);
	}
	return baseName;
    }

    private static String dirFromFile(String filename) {
	// pre condition - filename is a full path name
	int index = filename.lastIndexOf(File.separator);
	if (index != -1) {
	    return filename.substring(0,index);
	}
	return(".");	// present directory
    }

}
