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
	
	/******************* Graphs ***************/
	private static int[][][] systemUsageData;
	private static int[][][][] systemMsgsData;
	private static int[][][][] userEntryData;
	
    /*****************Color Maps 6/27/2002 *************/
    private static Color[] entryColors;

	/****************** Timeline ******************/
	public static Vector createTL( int p, long bt, long et ) {
		try {
			if( logLoader != null ) {
				return logLoader.createtimeline( p, bt, et );
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

    public static String getValidProcessorString() {
	return logLoader.getValidProcessorString();
    }

	public static int getNumProcessors() {
		return sts.getProcessorCount();
	}
	public static int getNumUserEntries() {
		return sts.getEntryCount();
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

	/******************* Usage Profile ******************
	This data gives the amount of processor time used by 
	each entry point (indices 0..numUserEntries-1), idle
	(numUserEntries), packing (numUserEntries+1), and unpacking 
	(numUserEntries+2).
	
	The returned values are in percent CPU time spent. 
	*/
	public static float[] GetUsageData( int pnum, long begintime, long endtime, OrderedIntList phases ) {
		status("GetUsageData(pe "+pnum+"):");
		if( sts.hasLogFiles()) { //.log files
			UsageCalc u=new UsageCalc();
			return u.usage(sts, pnum, begintime, endtime );
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
			float ret[]=new float[numUserEntries+4];
			//Convert to percent-- .sum entries are always over the 
			// entire program run.
			double scale=100.0/sts.getTotalTime();
			for (int q=0;q<numUserEntries;q++)
				ret[q]=(float)(scale*data[pnum][q]);
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
		
		if( sts.hasSumFiles() ) { //.sum files
			try {
				sumAnalyzer = new SumAnalyzer( sts );
			}
			catch( SummaryFormatException E ) {
				System.out.println( "Caught SummaryFormatException" );
			}
		}
		if ( sts.hasLogFiles() ) { //.log files
			logLoader = new LogLoader( sts);
		}

		// set up color maps for the entire toolkit.
		entryColors = new Color[sts.getEntryCount()];
		float H = (float)1.0;
		float S = (float)1.0;
		float B = (float)1.0;
		float delta = (float)(1.0/sts.getEntryCount());
		for(int i=0; i<sts.getEntryCount(); i++)
		    {
			entryColors[i] = Color.getHSBColor(H, S, B);
			H -= delta;
			if(H < 0.0)
			    H = (float)1.0;
		    }
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
		}
		else { // .sum files
			systemUsageData = new int[ 3 ][][];
			// systemUsageData[0][][] -- queue size
			// systemUsageData[1][][] -- processor utilization
			// systemUsageData[2][][] -- idle times
			try {
				systemUsageData[ 1 ] = sumAnalyzer.GetSystemUsageData( 
					intervalStart, intervalEnd, intervalSize);
			}
			catch( SummaryFormatException E ) {
				System.out.println( "Caught SummaryFormatException" );
			}
			catch( IOException e ) {
				System.out.println( "Caught IOExcpetion" );
			}
		}
	}
	public static long searchTimeline( int n, int p, int e ) throws EntryNotFoundException {
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

}
