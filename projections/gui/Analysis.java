package projections.gui;

/**
 * This static class is the interface projections.gui uses
 * to talk to the projections.analysis classes.
 */
import java.io.*;
import projections.analysis.*;
import projections.misc.*;
import java.util.*;
import java.awt.*;

public class Analysis {
	/******************* Version  ************/
	private static double version;
        public static double  getVersion() { return version; }
        public static void setVersion(double v) { version = v; }

	/******************* Initialization ************/
	private static StsReader sts;
	
	private static LogLoader logLoader;  //Only for .log files
	
	private static SumAnalyzer sumAnalyzer; //Only for .sum files
    private static BGSummaryReader bgSumReader; // Only for basename.sum files

    private static GenericSumDetailReader summaryDetails[]; // .sumd files
    private static int sumDetailMaxNumIntervals = -1;
	
	/******************* Graphs ***************/
	private static int[][][] systemUsageData;
	private static int[][][][] systemMsgsData;
	private static int[][][][] userEntryData;
    private static int[] bgData;

    private static String logDirectory;

	
    /*****************Color Maps 6/27/2002 *************/
    private static Color[] entryColors;

  public static String getUserEventName(int eventID) {
    if (sts != null) { return sts.getUserEventName(eventID); }
    else { return null; }
  }

    public static String[] getUserEventNames() {
	return sts.getUserEventNames();
    }

  public static Color getUserEventColor(int eventID) {
    if (sts != null) { return sts.getUserEventColor(eventID); }
    else { return null; }
  }

	/****************** Timeline ******************/
	public static Vector createTL( int p, long bt, long et, Vector timelineEvents, Vector userEvents ) {
		try {
			if( logLoader != null ) {
				return logLoader.createtimeline( p, bt, et, timelineEvents, userEvents );
			}
			else {
                                System.out.println("createTL: logLoader is null!");
				return null;
			}
		}
		catch( LogLoadException e ) {
			System.out.println( "LOG LOAD EXCEPTION" );
			return null;
		}
	}
	public static int[][] getAnimationData( int numPs, int intervalSize ) {
		int nInt=(int)(sts.getTotalTime()/intervalSize);
		LoadGraphData(nInt,intervalSize,0,nInt-1,false, null);
		int[][] animationdata = new int[ numPs ][ nInt ];
		for( int p = 0;p < numPs; p++ ) {
			for( int t = 0;t < nInt;t++ ) {
				animationdata[ p ][ t ] = getSystemUsageData(1)[p][t];
			}
		}
		return animationdata;
	}
	/**************** Utility/Access *************/
	public static String getFilename() {return sts.getFilename();}
	public static String[][] getLogFileText( int num ) {
		if( logLoader == null ) {
			return null;
		}
		else {
			Vector v = null;
			try {
				v = logLoader.view(num);
			}
			catch( LogLoadException e ) {
				
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
					  text[ i ][ 1 ] = "BEGIN PROCESSING of message sent to " + ve.Dest;
					  text[ i ][ 1 ] += " from processor " + ve.SrcPe;
					  break;
				  
				  case ( ProjDefs.END_PROCESSING ):
					  text[ i ][ 1 ] = "END PROCESSING of message sent to " + ve.Dest;
					  text[ i ][ 1 ] += " from processor " + ve.SrcPe;
					  break;
				  
				  case ( ProjDefs.ENQUEUE ):
					  text[ i ][ 1 ] = "ENQUEUEING message received from processor " + ve.SrcPe + " destined for " + ve.Dest;
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
					  text[ i ][ 1 ] = "!!!! ADD EVENT TYPE " + ve.EventType + " !!!";
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
     *  By default, return the first available valid processor string
     *  in the following preference order - log, summary, sumdetail.
     */
    public static String getValidProcessorString() {
	if (sts.hasLogFiles()) {
	    return sts.getValidProcessorString(StsReader.LOG);
	} else if (sts.hasSumFiles()) {
	    return sts.getValidProcessorString(StsReader.SUMMARY);
	} else if (sts.hasSumDetailFiles()) {
	    return sts.getValidProcessorString(StsReader.SUMDETAIL);
	} else {
	    return "";
	}
    }

    public static String getValidProcessorString(int type) {
	return sts.getValidProcessorString(type);
    }

	public static int getNumProcessors() {
		return sts.getProcessorCount();
	}
	public static int getNumUserEntries() {
		return sts.getEntryCount();
	}
    public static int getNumUserDefinedEvents() {
	return sts.getNumUserDefinedEvents();
    }

    public static int getUserDefinedEventIndex(int eventID) {
	return sts.getUserEventIndex(eventID);
    }

	public static int[][] getSystemMsgsData( int a, int t ) {
		return systemMsgsData[ a ][ t ];
	}
	public static int[][] getSystemUsageData( int a ) {
		return systemUsageData[ a ];
	}
	public static long getTotalTime() {
		return sts.getTotalTime();
	}

    public static double[] getSummaryAverageData() {
	return sumAnalyzer.getSummaryAverageData();
    }

    public static long getSummaryIntervalSize() {
	return sumAnalyzer.getIntervalSize();
    }

    public static Color getEntryColor(int entryIdx) {
	if (entryIdx < sts.getEntryCount()) {
	    return entryColors[entryIdx];
	} else {
	    return null;
	}
    }

    public static void setEntryColor(int entryIdx, Color color) {
	if (entryIdx < sts.getEntryCount()) {
	    entryColors[entryIdx] = color;
	} else {
	    System.err.println("Warning: entry point index " + entryIdx +
			       " not found. Cannot set color");
	}
    }

    public static void setEntryColors(Color[] _entryColors) {
	entryColors = _entryColors;
    }

	/******************* Usage Profile ******************
	This data gives the amount of processor time used by 
	each entry point (indices 0..numUserEntries-1), idle
	(numUserEntries), packing (numUserEntries+1), and unpacking 
	(numUserEntries+2).
	
	The returned values are in percent CPU time spent. 
	*/
	public static float[][] GetUsageData( int pnum, long begintime, long endtime, OrderedIntList phases ) {
		status("GetUsageData(pe "+pnum+"):");
		if( sts.hasLogFiles()) { //.log files
			UsageCalc u=new UsageCalc();
			return u.usage(sts, pnum, begintime, endtime,version );
		}
		else /*The files are sum files*/ {
			int temp,numUserEntries=sts.getEntryCount();
			long[][] data;
			long[][] phasedata;
			/*BAD: silently ignores begintime and endtime*/
			if( sumAnalyzer.GetPhaseCount()>1 ) {
				phases.reset();
				data = sumAnalyzer.GetPhaseChareTime( phases.nextElement() );
				if( phases.hasMoreElements() ) {
					while( phases.hasMoreElements() && ( pnum > -1 ) ) {
						phasedata = sumAnalyzer.GetPhaseChareTime( phases.nextElement() );
						for( int q = 0;q < numUserEntries;q++ ) {
							data[ pnum ][ q ] += phasedata[ pnum ][ q ];
						}
					}
				}
			}
			else {
				data = sumAnalyzer.GetChareTime();
			}
			float ret[][]=new float[2][numUserEntries+4];
			//Convert to percent-- .sum entries are always over the 
			// entire program run.
			double scale=100.0/sts.getTotalTime();
			for (int q=0;q<numUserEntries;q++){
				ret[0][q]=(float)(scale*data[pnum][q]);
				ret[1][q] = (float )0.0; // dummy value for message send time at the moment .. summary file reader needs to be fixed first
			}
			return ret;
		}
	}
	public static int[][] getUserEntryData( int a, int t ) {
		return userEntryData[ a ][ t ];
	}

    	public static String getLogName(int peNum) {
	return sts.getLogName(peNum);
	}

	public static String[][] getUserEntryNames() {
		return sts.getEntryNames();
	}
	public static int getUserEntryCount() {
		return sts.getEntryCount();
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
	public static void initAnalysis( String filename ) throws IOException 
	{
		status("initAnalysis("+filename+"):");
		sts=new StsReader(filename);

		logDirectory = dirFromFile(filename);
		
		if (sts.hasBGSumFile()) {
		    try {
			System.out.println("Has unified summary file." +
					   sts.getBGSumName());
			bgSumReader = new BGSummaryReader(sts.getBGSumName(),
							  4.0);
			System.out.println("intervals = " + bgSumReader.numIntervals);
			System.out.println("size = " + bgSumReader.intervalSize);

			sts.setTotalTime((long)(bgSumReader.numIntervals*
						bgSumReader.intervalSize*
						1.0e6));
			bgData = bgSumReader.processorUtil;
		    } catch (Exception e) {
			System.err.println("Exception caught while reading " +
					   "unified summary file.");
		    }
		}
		if( sts.hasSumFiles() ) { //.sum files
		    try {
			sumAnalyzer = new SumAnalyzer(sts);
		    } catch (SummaryFormatException e) {
			System.out.println(e.toString());
		    }
		}
		if (sts.hasSumDetailFiles()) { // .sumd files
		    try {
			summaryDetails = 
			    new GenericSumDetailReader[getNumProcessors()];
			for (int pe=0; pe<summaryDetails.length; pe++) {
			    summaryDetails[pe] = 
				new GenericSumDetailReader(sts.getSumDetailName(pe),
							   version);
			    if (sumDetailMaxNumIntervals <
				summaryDetails[pe].getNumIntervals()) {
				sumDetailMaxNumIntervals =
				    summaryDetails[pe].getNumIntervals();
			    }
			}
			System.out.println(sumDetailMaxNumIntervals + " " +
					   summaryDetails[0].getIntervalSize());
			sts.setTotalTime((long)(sumDetailMaxNumIntervals*
						summaryDetails[0].getIntervalSize()*
						1000000));
		    } catch (IOException e) {
			System.out.println(e.toString());
		    }
		}
		if ( sts.hasLogFiles() ) { //.log files
		    logLoader = new LogLoader( sts);
		}

		// set up color maps for the entire toolkit.
		entryColors = createColorMap(sts.getEntryCount());
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
	    /* **** might be a little too extreme ****
	       // color assignment weights distributed by the fibonacci sequence
	       // starting from 5.
	       weights[numSignificant-1] = 5;
	       weights[numSignificant-2] = 8;
	       for (int ep=numSignificant-3; ep>=0; ep--) {
	       weights[ep] = weights[ep-1] + weights[ep-2];
	       }
	    */

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
	System.out.println("received color request for " + numEPs + 
			   " elements with " + epMap.length +
			   " significant elements.");
	System.out.println("Weights assigned:");
	for (int i=0; i<weights.length; i++) {
	    System.out.print(weights[i] + " ");
	}
	System.out.println();

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
	System.out.println("x has value " + x);
	if (x < 0.67) {
	    x = 0.67;
	}

	double currentHue = 1.0;
	double saturation = 1.0;
	double brightness = 1.0;
	// assign colors to significant eps
	for (int ep=0; ep<numSignificant; ep++) {
	    System.out.println("Current hue is " + currentHue);
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

    public static int[] getBGData() {
	for (int i=0; i<bgData.length; i++) {
	    System.out.println(bgData[i]);
	}
	return bgData;
    }

        /**
           replace LoadGraphData(), with one more parameter containing
           a list of processors to read.
	   Two more parameters - intervalStart and intervalEnd added.

        */
	public static void LoadGraphData(int numIntervals, long intervalSize, 
					 int intervalStart, int intervalEnd,
					 boolean byEntryPoint, 
					 OrderedIntList processorList) 
	{
		status("LoadGraphData("+intervalSize+" us):");
		if( sts.hasLogFiles()) { // .log files
			LogReader logReader = new LogReader();
			logReader.read(sts, sts.getTotalTime(), intervalSize, 
				       intervalStart, intervalEnd,
				       byEntryPoint, processorList );
			systemUsageData = logReader.getSystemUsageData();
			systemMsgsData = logReader.getSystemMsgs();
			userEntryData = logReader.getUserEntries();
			logReader=null;
		} else { // no log files, so load .sum files
		    loadSummaryData(intervalSize, intervalStart,
				    intervalEnd);
		}
	}

    public static void loadSummaryData(long intervalSize,
				       int intervalStart, int intervalEnd) {
	// works only if summary file exists
	if (sts.hasSumFiles()) {
	    systemUsageData = new int[ 3 ][][];
	    // systemUsageData[0][][] -- queue size
	    // systemUsageData[1][][] -- processor utilization
	    // systemUsageData[2][][] -- idle times
	    try {
		systemUsageData[ 1 ] = 
		    sumAnalyzer.GetSystemUsageData(intervalStart, intervalEnd, 
						   intervalSize);
	    }
	    catch( SummaryFormatException E ) {
		System.out.println( "Caught SummaryFormatException" );
	    }
	    catch( IOException e ) {
		System.out.println( "Caught IOExcpetion" );
	    }
	}
    }

    // wrapper method for default interval size.
    public static void loadSummaryData(int intervalStart, int intervalEnd) {
	loadSummaryData(sumAnalyzer.getIntervalSize(), intervalStart,
			intervalEnd);
    }

    // wrapper method for default summary load (used by main window)
    public static void loadSummaryData() {
	// check has to be conducted here because of the use of sumAnalyzer
	if (sts.hasSumFiles()) { 
	    long sizeInt = sumAnalyzer.getIntervalSize();
	    int nInt=(int)(sts.getTotalTime()/sizeInt);
	    
	    loadSummaryData(sizeInt, 0, nInt-1);
	}
    }

    public static boolean hasSummaryData() {
	return sts.hasSumFiles();
    }

    public static boolean hasLogData() {
	return sts.hasLogFiles();
    }

    public static boolean hasSumDetailData() {
	return sts.hasSumDetailFiles();
    }

    public static double[][] getSumDetailData(int pe, int type) {
	return summaryDetails[pe].getData(type);
    }

    public static long searchTimeline( int n, int p, int e ) 
	throws EntryNotFoundException 
    {
	try {
	    return logLoader.searchtimeline( p, e, n );
	}
	catch( LogLoadException lle ) {
	    System.out.println( "LogLoadException" );
	    return -1;
	}
    }

    private static void status(String msg) {
	//For debugging/profiling:
	//System.out.println("gui.Analysis> "+msg);
    }
    
    // this is a hack, people should not use this
    public static long getIntervalSize() {
	return (long)(bgSumReader.intervalSize*1.0e6);
    }

    // another hack, all these "utility" methods should be refactored
    // into some place else.
    public static int getSumDetailNumIntervals() {
	return sumDetailMaxNumIntervals;
    }

    private static String dirFromFile(String filename) {
	// pre condition - filename is a full path name
	int index = filename.lastIndexOf(File.separator);
	if(index != -1)
		return filename.substring(0,index);
	return(".");	//present directory
    }

    public static String getLogDirectory() {
	return logDirectory;
    }
}
