package projections.Tools.TimeProfile;

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import projections.analysis.LogReader;
import projections.gui.MainWindow;

/** The reader threads for Time Profile tool. This class ought to be generalized for all the other tools needing similar functionality. */
 class ThreadedFileReader implements Runnable  {

	private int pe;
//	int p;  // Which index am I into the flattened array of potentially sparse pe's
	private long intervalSize;
	private int myRun;
	private int startInterval;
	private int endInterval;

	private int[][][] mySystemUsageData;   // [type][pe list index][interval]
//	int[][][][] mySystemMsgsData; // [categoryIdx][type][][]
	private int[][][][] myUserEntryData; // [ep idx][type][pe][]
	
	private double[][] graphData;
	
	private TreeMap<Double, String> phaseMarkers;
	
//	long logReaderIntervalSize;
	
	/** Construct a file reading thread that will measure utilization data 
	 *  for each interval associated with each EP, Idle, or Overhead. 
	 *  
	 *  graphData[interval][0 to (numEP-1)] contain time spent in the EPs.
	 *  graphData[interval][numEP] contains overhead time.
	 *  graphData[interval][numEP] contains idle time.
	 *  
	 *  The resulting output data will be accumulated into the array specified in a synchronized manner
     * @param intervalSize
     * @param phaseMarkers
     * */
	protected ThreadedFileReader(int pe, long intervalSize, int myRun, int startInterval, int endInterval,
			TreeMap<Double, String> phaseMarkers, double[][] graphData){
		this.phaseMarkers = phaseMarkers;
		this.pe = pe;
		this.intervalSize = intervalSize;
		this.myRun = myRun;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
		this.graphData = graphData;
	}


	public void run() { 
		LoadGraphDataForOnePe(intervalSize, startInterval, endInterval, true, pe);
	}


	/**	
	 *	Load graph data for one or more processors.
	 *
	 *  Written by Isaac to replace Analysis::LoadGraphData() for use in parallel file reading.
	 *
	 *  The potentially sparse data that is loaded by this function is stored into systemUsageData, 
	 *  systemMsgsData, userEntryData, and logReaderIntervalSize.
	 *  These first 2-3 dimensions of these sparse arrays ought to be allocated before this routine is called.
	 *  The last dimension of the array will be allocated from inside this function.
	 *  
	 *  No data will be copied into systemUsageData if it is null.
	 *  No data will be copied into systemMsgsData if it is null.
	 *  No data will be copied into userEntryData if it is null.
	 *  
	 *  
	 * Currently, only the userEntryData[*][A][*] fields are stored if the A portion of the array is non-null. 
	 * This is because  A=LogReader.TIME is the only field used by the TimeProfile tool. 
	 * If this class is extended for other tools, this might need to be handled differently.
	 *  
	 */
	private void LoadGraphDataForOnePe(long intervalSize,
			int intervalStart, int intervalEnd,
			boolean byEntryPoint, 
			int pe) 
	{
		LogReader logReader = new LogReader();
		SortedSet<Integer> processorList = new TreeSet<Integer>();
		processorList.add(pe);

		if( MainWindow.runObject[myRun].hasLogFiles()) { // .log files
			logReader.read(intervalSize,
					intervalStart, intervalEnd,
					byEntryPoint, processorList, false, phaseMarkers);
			mySystemUsageData = logReader.getSystemUsageData();
//			mySystemMsgsData = logReader.getSystemMsgs();
			myUserEntryData = logReader.getUserEntries();
//			logReaderIntervalSize = logReader.getIntervalSize();
		} else {
			System.err.println("Error: No log data files found!");
		}

		
		accumulateIntoShared();
			
		// Release any unneeded memory	
		mySystemUsageData = null; 
//		mySystemMsgsData = null;
		myUserEntryData = null;
	}
	
	
	/** A threadsafe way for accumulating results into graphData. */
	private void accumulateIntoShared(){

		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		int numIntervals = endInterval-startInterval+1;

		// Accumulate results into the shared array "graphData"
		synchronized (graphData) {
			
			// Extract data and put it into the graph
			for (int ep=0; ep<numEPs; ep++) {
				int[][] entryData = myUserEntryData[ep][LogReader.TIME];
				for (int interval=0; interval<numIntervals; interval++) {
					graphData[interval][ep] += entryData[0][interval];
					graphData[interval][numEPs] -= entryData[0][interval]; // overhead = -work time										
				}
			}

			// Idle time SYS_IDLE=2
			int[][] idleData = mySystemUsageData[2]; //percent
			for (int interval=0; interval<numIntervals; interval++) {
				if(idleData[0] != null && idleData[0].length>interval){				
					graphData[interval][numEPs+1] += idleData[0][interval] * 0.01 * intervalSize;
					graphData[interval][numEPs] -= idleData[0][interval] * 0.01 * intervalSize; //overhead = - idle time
					graphData[interval][numEPs] += intervalSize;  
				}
			}

		}
	}

}





