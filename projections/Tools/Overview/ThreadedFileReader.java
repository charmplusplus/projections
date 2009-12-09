package projections.Tools.Overview;

import projections.analysis.LogReader;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;

/** The reader threads for Time Profile tool. This class ought to be generalized for all the other tools needing similar functionality. */
public class ThreadedFileReader extends Thread  {

	int pe;
//	int p;  // Which index am I into the flattened array of potentially sparse pe's
	long intervalSize;
	int myRun;
	int startInterval;
	int endInterval;
//	boolean ampiTraceOn;

	int[][][] mySystemUsageData;   // [type][pe list index][interval]
//	int[][][][] mySystemMsgsData;  // [categoryIdx][type][][]
	int[][][][] myUserEntryData;   // [ep idx][type][pe][]
	
//	long logReaderIntervalSize;
	
	int entryData[];       // [interval]  which EP is most prevalent in each interval
	float utilizationData[]; // [interval]  Utilization for each interval

	
	/** Construct a file reading thread that will determine the best EP representative for each interval
	 *  
	 *  The resulting output data will be assigned into the array specified without synchronization
	 * @param utilizationData 
	 *  
	 *  */
	public ThreadedFileReader(int pe, int p, long intervalSize, int myRun, int startInterval, int endInterval, int[] entryData, float[] utilizationData){
		this.pe = pe;
//		this.p = p;
		this.intervalSize = intervalSize;
		this.myRun = myRun;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
//		this.ampiTraceOn = ampiTraceOn;
		this.entryData = entryData;
		this.utilizationData = utilizationData;
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
					byEntryPoint, processorList, false);
			mySystemUsageData = logReader.getSystemUsageData();
//			mySystemMsgsData = logReader.getSystemMsgs();
			myUserEntryData = logReader.getUserEntries();
//			logReaderIntervalSize = logReader.getIntervalSize();
		} else {
			System.err.println("Error: No data Files found!!");
		}
		
		
		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		int numIntervals = endInterval-startInterval;
		
		double[][] utilData = new double[numIntervals][numEPs+2];
	
		// Extract data and put it into the graph
		for (int ep=0; ep<numEPs; ep++) {
			int[][] entryData = myUserEntryData[ep][LogReader.TIME];
			for (int interval=0; interval<numIntervals; interval++) {
				utilData[interval][ep] += entryData[0][interval];
				utilData[interval][numEPs] -= entryData[0][interval]; // overhead -= work time										
			}
		}

		// Idle time SYS_IDLE=2
		int[][] idleData = mySystemUsageData[2]; //percent
		for (int interval=0; interval<numIntervals; interval++) {
			if(idleData[0] != null && idleData[0].length>interval){				
				utilData[interval][numEPs+1] += idleData[0][interval] * 0.01 * intervalSize; // idle
				utilData[interval][numEPs] -= idleData[0][interval] * 0.01 * intervalSize; //overhead -= idle time
				utilData[interval][numEPs] += intervalSize; // overhead
			}
		}
		
		
		// Condense the utilization data down
		for(int i=0; i<numIntervals; i++){
			// Because we have already computed the overhead time. The utilization is 1.0 - overhead - idle
			utilizationData[i] = (float) (1.0 - (utilData[i][numEPs] + utilData[i][numEPs+1])/(double)intervalSize);
		}
		
		// Now find the most commonly occurring EP for each interval
			
		for(int i=0; i<numIntervals; i++){
			// find max
			int maxEP = 0;
			double maxVal = utilData[i][0];
			for(int j=0; j<numEPs+2; j++) {
				if(utilData[i][j]>maxVal) {
					maxVal = utilData[i][j];
					maxEP = j;
				}
			}				
			entryData[i] = maxEP;
		}

		// Release any unneeded memory	
		utilData = null;
		mySystemUsageData = null; 
//		mySystemMsgsData = null;
		myUserEntryData = null;
	}
	
	

}





