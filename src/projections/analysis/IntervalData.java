package projections.analysis;

import java.io.IOException;
import java.util.*;

import projections.analysis.SumDetailReader.RLEBlock;
import projections.gui.MainWindow;

/**
 *  IntervalData represents the abstraction for interval-based data
 *  from data sources that include summary detail and log. This object
 *  is only created if the data sources actually exists.
 *
 *  The API provides for data access, including:
 *  1) partial data access (range of intervals, etc ...)
 *  2) "projected" data (summed across one of the dimensions)
 *
 *  The implementation stores this data in RLE compressed format.
 */
public class IntervalData
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
	private int myRun = 0;

    // Types of information available for IntervalData. Some may be supported
    // only by a subset of the possible sources of data. This subset support
    // should, as far as possible, be avoided.
    private static final int TYPE_TIME = 0;
    private static final int TYPE_NUM_MSGS = 1;

    // associated readers
    private static SumDetailReader summaryDetails[];

    // Compressed Data
    // A Vector of RLEBlocks for each Type, EP combination
    // Dim 0 - indexed by data type.
    // Dim 1 - indexed by PE
    // Dim 2 - indexed by EP, a not-quite-as dense format as the actual file
    //                 but good enough.
    private List rawData[][][];

    // Uncompressed Data (the old way of doing things) for supporting
    // old tools.
    private int systemUsageData[][][] = null;
    private int systemMsgsData[][][][] = null;
    private int userEntryData [][][][] = null;

    private int numEPs = 0;
    private int numPEs = 0;
    // numIntervals and intervalSize represents the canonical number of
    // intervals and interval size across all the data log files. This is
    // not affected by .log files but in .sumd files, there is a remote 
    // possibility that the number of intervals and interval size differs
    // between files.
    private int numIntervals = 0;
    private double intervalSize = 0;

    private int sumDetailData[][] = null;


    /**
     *  The constructor
     */
    public IntervalData() {
	this.numPEs = MainWindow.runObject[myRun].getNumProcessors();
	this.numEPs = MainWindow.runObject[myRun].getNumUserEntries();

	// load at least the summary detail data.
	if (MainWindow.runObject[myRun].hasSumDetailData()) {
	    summaryDetails = new SumDetailReader[numPEs];
	    rawData = new ArrayList[SumDetailReader.NUM_TAGS][numPEs][];
	    SortedSet<Integer> availablePEs =
		MainWindow.runObject[myRun].getValidProcessorList(ProjMain.SUMDETAIL);
	    for(Integer pe : availablePEs) {

            try {
		    summaryDetails[pe] =
			new SumDetailReader(MainWindow.runObject[myRun].getSumDetailLog(pe),
					    MainWindow.runObject[myRun].getVersion());
		    summaryDetails[pe].readStaticData();
		    summaryDetails[pe].read();
		    for (int type=0; type<SumDetailReader.NUM_TAGS; type++) {
			rawData[type][pe] = summaryDetails[pe].getData(type);
		    }
		    // get max (canonical) number of intervals
		    if (numIntervals < summaryDetails[pe].getNumIntervals()) {
			numIntervals = summaryDetails[pe].getNumIntervals();
		    }
		    // get (canonical) size of an interval.
		    // **CW** do nothing for now. No rebinning facilities in
		    // place yet.
		    intervalSize = summaryDetails[pe].getIntervalSize();

        } catch (IOException e) {
		    // This exception, in future, should simply cause the
		    // necessary adjustments to the list of available PEs
		    // listed in MainWindow.runObject for summary detail files.
		    System.err.println("Warning: Failed to read summary " +
				       "detail file for processor " + pe);
            e.printStackTrace();
		    continue;
		}
	    }
        System.out.println("IntervalData - hasSumDetailData + numIntervals: "+numIntervals+" intervalSize: " + intervalSize);
	}
    }
    public void loadSumDetailIntervalData(long intervalSize, int intervalStart,
                                          int intervalEnd,
                                          SortedSet<Integer> processorList){
        int numIntervals = intervalEnd - intervalStart + 1;

        sumDetailData = new int[numIntervals][numEPs];

        double[][] tempData;
        for(Integer curPe : processorList) {
            int ii = intervalStart;
            tempData = getData(curPe, TYPE_TIME);
            for(int i=0; i<numIntervals; i++){
                for(int e=0; e<numEPs; e++){
                    sumDetailData[i][e] += tempData[e][ii];
                }
                ii++;
            }
        }
    }
    /**
     *  This is a method for use with the older way of doing things only.
     *  Regretably, it's needed to get things working for now.
     *
     *  The method fills 3 arrays - systemUsageData, systemMsgsData
     *                              and userEntryData
     *  given time-range specifications.
     */
    public void loadIntervalData(long intervalSize, int intervalStart,
				 int intervalEnd, boolean byEntryPoint,
				 SortedSet<Integer> processorList) {
        int numIntervals = intervalEnd - intervalStart + 1;
	systemUsageData = new int[3][processorList.size()][numIntervals];
	systemMsgsData = new int[5][3][processorList.size()][numIntervals];
	if (byEntryPoint) {
	    userEntryData = 
		new int[numEPs][3][processorList.size()][numIntervals];
	}
	double tempData[][] = null;
	int processorCount = 0;
	for(Integer curPe : processorList) {
	    // get standard data
	    tempData = getData(curPe, TYPE_TIME, intervalSize, intervalStart,
			       intervalEnd-intervalStart+1);
	    // copy into userEntryData, if byEntryPoint is true,
	    // accumulate into systemUsageData.
	    for (int i=0; i<numIntervals; i++) {
		for (int ep=0; ep<numEPs; ep++) {
		    if (byEntryPoint) {
			userEntryData[ep][2][processorCount][i] =
			    (int)tempData[ep][i];
		    }
		    systemUsageData[1][processorCount][i] +=
			(int)tempData[ep][i];
		}
		// after accumulation for systemUsageData, convert to %util
		systemUsageData[1][processorCount][i] =
		    (int)IntervalUtils.timeToUtil(systemUsageData[1][processorCount][i],
						  intervalSize);
	    }

	    // get message data
	    tempData = getData(curPe, TYPE_NUM_MSGS, intervalSize,
			       intervalStart, intervalEnd-intervalStart+1);
	    // accumulate into systemMsgsData
	    for (int i=0; i<numIntervals; i++) {
		for (int ep=0; ep<numEPs; ep++) {
		    systemMsgsData[1][2][processorCount][i] +=
			(int)tempData[ep][i];
		}
	    }

	    processorCount++;
	}
    }

    public int[][] sumDetailData() {return  sumDetailData; };

    public int[][][] getSystemUsageData() {
	return systemUsageData;
    }

    public int[][][][] getSystemMsgs() {
	return systemMsgsData;
    }

    public int[][][][] getUserEntries() {
	return userEntryData;
    }

    
    /**
     *  This is an old API for tools that require the data in expanded
     *  format. 
     */
    private double[][] getData(int pe, int type) {
	double returnData[][] = new double[numEPs][numIntervals];
	for (int ep=0; ep<numEPs; ep++) {
	    Iterator<RLEBlock> blockIterator = rawData[type][pe][ep].iterator();
	    int curInterval = 0;
	    while (blockIterator.hasNext()) {
		RLEBlock nextBlock = blockIterator.next();
		for (int offset=0; offset<nextBlock.count; offset++) {
		    returnData[ep][curInterval+offset] = nextBlock.value;
		}
		curInterval += nextBlock.count;
	    }
	}
	return returnData;
    }

    /**
     *  This is a general method that offers flexibility in choosing
     *  interval bin sizes and time ranges.
     */
    private double[][] getData(int pe, int type, long destIntervalSize,
			      int destIntervalStart, int numDestIntervals) {

	// No interval size differences. Either use whatever is returned
	// by getData() or copy a part of the whole array.
	double tempData[][] = getData(pe, type);
	double returnData[][] = null;
	if (destIntervalSize == intervalSize) {
	    if ((destIntervalStart == 0) && 
		(numDestIntervals == numIntervals)) {
		return tempData;
	    } else {
		returnData = new double[numEPs][numDestIntervals];
		for (int ep=0; ep<tempData.length; ep++) {
		    for (int interval=destIntervalStart; 
			 interval<destIntervalStart+numDestIntervals;
			 interval++) {
			returnData[ep][interval-destIntervalStart] =
			    tempData[ep][interval];
		    }
		}
		return returnData;
	    }
	}

	// Interval size different, rebinning and interpolation of
	// data required.
	returnData = new double[numEPs][numDestIntervals];
	boolean discrete = false;

	if (type == TYPE_NUM_MSGS) {
	    discrete = true;
	}
	for (int ep=0; ep<tempData.length; ep++) {
	    for (int srcInt=0; srcInt<tempData[ep].length; srcInt++) {
		IntervalUtils.fillIntervals(returnData[ep], destIntervalSize,
					    destIntervalStart,
					    (long)(srcInt*intervalSize),
					    (long)((srcInt+1)*intervalSize-1),
					    tempData[ep][srcInt],
					    discrete);
	    }
	}
	return returnData;
    }

    /**
     *  Returns the canonical number of intervals for the set of data.
     */
    public int getNumIntervals() {
	return numIntervals;
    }

    /**
     *  Returns the canonical interval size for the set of data.
     */
    public double getIntervalSize() {
	return intervalSize;
    }

//    /**
//     *  Accumulate compressed interval data for a particular EP and type onto
//     *  a given array. A range of intervals can be supplied.
//     */
//    private void accumulateIntoArray(int type, int pe, int epIdx,
//				     int startInterval, int endInterval,
//				     double outData[]) {
//	int currentIdx = 0;
//	boolean done = false;
//	Iterator blockIterator = rawData[type][pe][epIdx].iterator();
//
//	while (blockIterator.hasNext()) {
//	    RLEBlock nextBlock = (RLEBlock)blockIterator.next();
//	    int endIdx = currentIdx + nextBlock.count - 1;
//	    // nothing to do in this block.
//	    if (endIdx < startInterval) {
//		currentIdx = endIdx+1;
//		continue;
//	    }
//	    // deciding on start boundary for this block if it is the
//	    // first block.
//	    if (currentIdx < startInterval) {
//		currentIdx = startInterval;
//	    }
//	    // deciding on end boundary for this block if it is the last
//	    // block.
//	    if (endIdx > endInterval) {
//		endIdx = endInterval;
//		done = true;
//	    }
//	    // fill in the necessary parts of the array
//	    for (int i=currentIdx-startInterval; 
//		 i<=endIdx-startInterval; i++) {
//		outData[i] = nextBlock.value;
//	    }
//	    currentIdx = endIdx+1;
//	    if (done) {
//		break;
//	    }
//	}
//    }

    // *** "Projected" sum detail data accessors ***
    // These methods return a collapsed (accumulated across one or more
    // dimensions) part of the sum detail data.

//    /**
//     *  This version of getDataSummedAcrossProcessors outputs a 2D array
//     *  of double values with the first dimension indexed by ep id and
//     *  the second dimension indexed by interval id.
//     *
//     *  This should be slightly more efficient when acquiring data for
//     *  the full range of EPs.
//     */
//    public double[][] getDataSummedAcrossProcessors(int type,
//						    OrderedIntList pes,
//						    int startInterval,
//						    int endInterval) {
//	int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
//	double returnArray[][] = 
//	    new double[numEPs][endInterval-startInterval+1];
//	
//	for (int ep=0; ep<numEPs; ep++) {
//	    pes.reset();
//	    for (int i=0; i<pes.size(); i++) {
//		int pe = pes.nextElement();
//		accumulateIntoArray(type, pe, ep, startInterval, endInterval,
//				    returnArray[ep]);
//	    }
//	}
//	return returnArray;
//    }

//    /**
//     *  getDataSummedAcrossProcessors outputs a vector of double[] with
//     *  the vector representing possibly non-contigious EPs. The arrays
//     *  are indexed by interval id.
//     */
//    public Vector getDataSummedAcrossProcessors(int type,
//						OrderedIntList pes,
//						int startInterval,
//						int endInterval,
//						OrderedIntList eps) {
//	Vector returnVector = new Vector();
//	double dataArray[] = null;
//
//	eps.reset();
//	for (int i=0; i<eps.size(); i++) {
//	    dataArray = new double[endInterval-startInterval+1];
//	    int ep = eps.nextElement();
//	    pes.reset();
//	    for (int j=0; j<pes.size(); j++) {
//		int pe = pes.nextElement();
//		accumulateIntoArray(type, pe, ep, startInterval, endInterval,
//				    dataArray);
//	    }
//	    returnVector.add(dataArray);
//	}
//	return returnVector;
//    }
}
