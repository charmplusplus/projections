package projections.TimeProfile;

import java.util.Iterator;

import projections.analysis.IntervalData;
import projections.analysis.LogReader;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;

/** The reader threads for Time Profile tool. This class ought to be generalized for all the other tools needing similar functionality. */
public class ThreadedFileReader extends Thread  {

	int pe;
	int p;  // Which index am I into the flattened array of potentially sparse pe's
	long intervalSize;
	int myRun;
	int startInterval;
	int endInterval;
	boolean ampiTraceOn;

	int[][][] mySystemUsageData;   // [type][pe list index][interval]
	int[][][][] mySystemMsgsData; // [categoryIdx][type][][]
	int[][][][] myUserEntryData; // [ep idx][type][pe][]

	
	int numIntervals;
	int numUserEntries;
	int numProcessors;
	
	double[][] graphData;
	
	long logReaderIntervalSize;
	int numEPs;
	
	public ThreadedFileReader(int pe, int p, long intervalSize, int myRun, int startInterval, int endInterval, 
			boolean ampiTraceOn, int numIntervals, int numUserEntries, int numPEs, int numEPs, double[][] graphData){
		this.pe = pe;
		this.p = p;
		this.intervalSize = intervalSize;
		this.myRun = myRun;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
		this.ampiTraceOn = ampiTraceOn;
		this.numIntervals = numIntervals;
		this.numUserEntries = numUserEntries;
		this.graphData = graphData;
		this.numProcessors = numPEs;
		this.numEPs = numEPs;
	}

	
	public void run() { 
		OrderedIntList tempList = new OrderedIntList();
		tempList.insert(pe);
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
	public void LoadGraphDataForOnePe(long intervalSize, 
			int intervalStart, int intervalEnd,
			boolean byEntryPoint, 
			int pe) 
	{
		LogReader logReader = new LogReader();
		OrderedIntList processorList = new OrderedIntList();
		processorList.insert(pe);

		if( MainWindow.runObject[myRun].hasLogFiles()) { // .log files
			logReader.read(intervalSize, 
					intervalStart, intervalEnd,
					byEntryPoint, processorList);
			mySystemUsageData = logReader.getSystemUsageData();
			mySystemMsgsData = logReader.getSystemMsgs();
			myUserEntryData = logReader.getUserEntries();
			logReaderIntervalSize = logReader.getIntervalSize();
		} else if (MainWindow.runObject[myRun].hasSumDetailFiles()) {
			IntervalData intervalData = new IntervalData();
			intervalData.loadIntervalData(intervalSize, intervalStart,
					intervalEnd, byEntryPoint,
					processorList);
			mySystemUsageData = intervalData.getSystemUsageData();
			mySystemMsgsData = intervalData.getSystemMsgs();
			myUserEntryData = intervalData.getUserEntries();
		} else if (MainWindow.runObject[myRun].hasSumFiles()) { // no log files, so load .sum files
			System.err.println("Error: This case should never be reached ?!");
		} else {
			System.err.println("Error: No data Files found!!");
		}

		accumulateIntoShared();
	
		// Release any unneeded memory	
		mySystemUsageData = null; 
		mySystemMsgsData = null;
		myUserEntryData = null;
	}
	
	
	/** A threadsafe way for accumulating results into graphData. */
	private void accumulateIntoShared(){

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





