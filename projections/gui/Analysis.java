
package projections.gui;
import java.io.*;
import projections.analysis.*;
import projections.misc.*;
import java.util.*;
import java.awt.*;

public class Analysis {
	private static MyLinkedList timeline;
	private static int numProcessors;
	private static long totalTime;
	private static long intervalLength;
	private static double SampleLength;
	private static int versionNum;
	private static int avgTime;
	private static int charesCreated;
	private static int charesProcessed;
	private static int msgsCreated;
	private static int msgsProcessed;
	private static int BOCs;
	private static int numUserEntries;
	private static int DisplayUsageOrderSize;
	private static int phases;
	private static String[][] userEntryNames;
	private static String filename;
	private static int[][][] systemUsageData;
	private static int[][][][] systemMsgsData;
	private static int[][][][] userEntryData;
	private static int[] DisplayUsageOrder;
	private static LogAnalyzer logAnalyzer;
	private static LogReader logReader;
	private static SumAnalyzer sumAnalyzer;
	private static int SumFileType;
	
	// SumFileType = 0 when you are using .log files and it = 1 when you are using .sum files
	public static void initAnalysis( String s ) throws IOException {
		filename = s;
		File name = new File( s + ".0.sum" );
		if( name.isFile() ) {
			SumFileType = 1;
			sumAnalyzer = new SumAnalyzer( filename );
			userEntryNames = sumAnalyzer.getuserentries();
			long suminfo[];
			try {
				suminfo = sumAnalyzer.ReadInProcessorUtilization();
				numProcessors = (int)suminfo[ 0 ];
				numUserEntries = (int)suminfo[ 1 ];
				intervalLength = suminfo[ 2 ]; // in microseconds
				totalTime = suminfo[ 3 ]; //in microseconds
				phases = (int)suminfo[ 4 ];
				versionNum = (int)suminfo[ 5 ];
			}
			catch( SummaryFormatException E ) {
				System.out.println( "Caught SummaryFormatException" );
			}
			catch( IOException E ) {
				System.out.println( "Couldn't read sum file " + name );
			}
		}
		else {
			SumFileType = 0;
			logAnalyzer = new LogAnalyzer( filename );
			numProcessors = logAnalyzer.numpe();
			totalTime = logAnalyzer.totaltime();
			numUserEntries = logAnalyzer.numuserentries();
			userEntryNames = logAnalyzer.getuserentries();
		}
	}
	public static int ReturnFileType() {
		return SumFileType;
	}

	public static void QuickLoad2( GraphWindow gw, int numIntervals, long intervalSize ) {
		if( SumFileType == 0 ) { // Input .sts has corresponding .log files to analyze
			logReader = new LogReader( filename );
			logReader.ReadInLogFile( numProcessors, numUserEntries, totalTime, intervalSize );
			systemUsageData = logReader.getSystemUsageData();
			systemMsgsData = logReader.getSystemMsgs();
			userEntryData = logReader.getUserEntries();
		}
		else {
			
			// Input .sts has corresponding .sum files to analyze
			systemUsageData = new int[ 3 ][ numProcessors ][ numIntervals ];
			// systemUsageData[0][][] holds the values that display the queue size
			// systemUsageData[1][][] holds the values that display processor utilization
			// systemUsageData[2][][] holds the values that display the idle times
			try {
				systemUsageData[ 1 ] = sumAnalyzer.GetSystemUsageData( numProcessors, numIntervals, totalTime, intervalLength );
			}
			catch( SummaryFormatException E ) {
				System.out.println( "Caught SummaryFormatException" );
			}
			catch( IOException e ) {
				System.out.println( "Caught IOExcpetion" );
			}
		}
	}
	
	/******************* Usage ******************
	This data gives the amount of processor time used by 
	each entry point (indices 0..numUserEntries-1), idle
	(numUserEntries), packing (numUserEntries+1), ang unpacking 
	(numUserEntries+2).
	
	The returned values are in percent. 
	*/
	public static float[] GetUsageData( int pnum, long begintime, long endtime, OrderedIntList phases ) {
		if( 0 == SumFileType ) { //.log files
			UsageCalc usagecalculator;
			usagecalculator = new UsageCalc( filename, numUserEntries );
			usagecalculator.ReadInLogFiles( pnum, (int)begintime, (int)endtime );
			return usagecalculator.getData();
		}
		else /*The files are sum files*/ {
			int temp;
			long[][] data;
			long[][] phasedata;
			if( versionNum > 2 ) {
				phases.reset();
				data = sumAnalyzer.GetPhaseChareTime( phases.nextElement() );
				if( phases.hasMoreElements() ) {
					while( phases.hasMoreElements() && ( pnum > -1 ) ) {
						phasedata = sumAnalyzer.GetPhaseChareTime( phases.nextElement() );
						for( int q = 0;q < numUserEntries;q++ ) {
							data[ pnum ][ q ] = data[ pnum ][ q ] + phasedata[ pnum ][ q ];
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
			for (int q=0;q<numUserEntries;q++)
				ret[q]=(float)(100.0*data[pnum][q]/totalTime);
			return ret;
		}
	}
	
	/****************** Timeline ******************/
	public static MyLinkedList createTL( int p, long bt, long et ) {
		try {
			if( logAnalyzer != null ) {
				return logAnalyzer.createtimeline( p, bt, et );
			}
			else {
				return null;
			}
		}
		catch( LogLoadException e ) {
			System.out.println( "LOG LOAD EXCEPTION" );
			return null;
		}
	}
	public static long searchTimeline( int n, int p, int e ) throws EntryNotFoundException {
		try {
			return logAnalyzer.searchtimeline( p, e, n );
		}
		catch( LogLoadException lle ) {
			System.out.println( "LogLoadException" );
			return -1;
		}
	}
	
	/**************** Utility/Access *************/
	public static int getNumPhases() {
		return phases;
	}
	public static int[][] getSystemUsageData( int a ) {
		return systemUsageData[ a ];
	}
	public static int[][] getSystemMsgsData( int a, int t ) {
		return systemMsgsData[ a ][ t ];
	}
	public static int[][] getUserEntryData( int a, int t ) {
		return userEntryData[ a ][ t ];
	}
	public static int getNumProcessors() {
		return numProcessors;
	}
	public static long getTotalTime() {
		return totalTime;
	}
	public static String getFilename() {
		return filename;
	}
	public static int getNumUserEntries() {
		return numUserEntries;
	}
	public static String[][] getUserEntryNames() {
		return userEntryNames;
	}
	public static String[][] getLogFileText( int num ) {
		if( logAnalyzer == null ) {
			return null;
		}
		else {
			MyLinkedList list = null;
			try {
				list = logAnalyzer.viewlog( num );
			}
			catch( LogLoadException e ) {
				
			}
			if( list == null ) {
				return null;
			}
			int length = list.length();
			if( length == 0 ) {
				return null;
			}
			String[][] text = new String[ length ][ 2 ];
			ViewerEvent ve;
			for( int i = 0;i < length;i++ ) {
				list.next();
				ve = (ViewerEvent)list.data();
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
	public static int[][] getAnimationData( int numPs, int numIs ) {
		int[][] animationdata = new int[ numPs ][ numIs ];
		for( int i = 0;i < numPs;i++ ) {
			for( int j = 0;j < numIs;j++ ) {
				animationdata[ i ][ j ] = (int)( Math.random() * 100 );
			}
		}
		return animationdata;
	}
}









