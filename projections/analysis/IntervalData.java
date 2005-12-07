package projections.analysis;

import java.io.*;
import java.util.*;

import projections.gui.*;

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
    // Types of information available for IntervalData. Some may be supported
    // only by a subset of the possible sources of data. This subset support
    // should, as far as possible, be avoided.
    public static final int NUM_TYPES = 2;
    public static final int TYPE_TIME = 0;
    public static final int TYPE_NUM_MSGS = 1;

    // associated readers
    private static SumDetailReader summaryDetails[];

    // Compressed Data
    // A Vector of RLEBlocks for each Type, EP combination
    // Dim 0 - indexed by data type.
    // Dim 1 - indexed by PE
    // Dim 2 - indexed by EP, a not-quite-as dense format as the actual file
    //                 but good enough.
    private Vector rawData[][][];

    private int numEPs = 0;
    private int numPEs = 0;
    // numIntervals and intervalSize represents the canonical number of
    // intervals and interval size across all the data log files. This is
    // not affected by .log files but in .sumd files, there is a remote 
    // possibility that the number of intervals and interval size differs
    // between files.
    private int numIntervals = 0;
    private double intervalSize = 0;

    /**
     *  The constructor
     */
    public IntervalData() {
	this.numPEs = Analysis.getNumProcessors();
	this.numEPs = Analysis.getNumUserEntries();

	// load at least the summary detail data.
	if (Analysis.hasSumDetailData()) {
	    summaryDetails = new SumDetailReader[numPEs];
	    rawData = new Vector[SumDetailReader.NUM_TAGS][numPEs][];
	    OrderedIntList availablePEs = 
		Analysis.getValidProcessorList(Analysis.SUMDETAIL);
	    availablePEs.reset();
	    while (availablePEs.hasMoreElements()) {
		int pe = availablePEs.nextElement();
		try {
		    summaryDetails[pe] = 
			new SumDetailReader(Analysis.getSumDetailName(pe),
					    Analysis.getVersion());
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
		    // listed in Analysis for summary detail files.
		    System.err.println("Warning: Failed to read summary " +
				       "detail file for processor " + pe);
		    continue;
		}
	    }
	}
    }

    /**
     *  loadData is a internal method for loading log data. Interval size
     *  is an input parameter that, if different from the current supported
     *  value, would result in the reading of log files (if smaller) or a 
     *  re-binning (if larger) of the raw data.
     */
    private void loadData(int type, int pe, int ep, 
			  int startInterval, int endInterval,
			  double intervalSize) {
	// either a re-bin or re-read of logs is required.
	if (intervalSize < this.intervalSize) {
	    if (Analysis.hasLogData()) {
		// log files exist - get more detailed info from log files.
	    } else {
		// no log files exist - the only way to provide the information
		// is to do a reverse approximate re-bin.
	    }
	} else if (intervalSize > this.intervalSize) {
	    // simply re-bin by expanding bin-size (no loss of information)
	} else {
	    // same interval size as the currently supported one, nothing
	    // needs to be done.
	}
    }
			  

    /**
     *  This is an old API for tools that require the data in expanded
     *  format. 
     */
    public double[][] getData(int pe, int type) {
	double returnData[][] = new double[numEPs][numIntervals];
	for (int ep=0; ep<numEPs; ep++) {
	    Iterator blockIterator = rawData[type][pe][ep].iterator();
	    int curInterval = 0;
	    while (blockIterator.hasNext()) {
		RLEBlock nextBlock = (RLEBlock)blockIterator.next();
		for (int offset=0; offset<nextBlock.count; offset++) {
		    returnData[ep][curInterval+offset] = nextBlock.value;
		}
		curInterval += nextBlock.count;
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

    /**
     *  Accumulate compressed interval data for a particular EP and type onto
     *  a given array. A range of intervals can be supplied.
     */
    private void accumulateIntoArray(int type, int pe, int epIdx,
				     int startInterval, int endInterval,
				     double outData[]) {
	int currentIdx = 0;
	boolean done = false;
	Iterator blockIterator = rawData[type][pe][epIdx].iterator();

	while (blockIterator.hasNext()) {
	    RLEBlock nextBlock = (RLEBlock)blockIterator.next();
	    int endIdx = currentIdx + nextBlock.count - 1;
	    // nothing to do in this block.
	    if (endIdx < startInterval) {
		currentIdx = endIdx+1;
		continue;
	    }
	    // deciding on start boundary for this block if it is the
	    // first block.
	    if (currentIdx < startInterval) {
		currentIdx = startInterval;
	    }
	    // deciding on end boundary for this block if it is the last
	    // block.
	    if (endIdx > endInterval) {
		endIdx = endInterval;
		done = true;
	    }
	    // fill in the necessary parts of the array
	    for (int i=currentIdx-startInterval; 
		 i<=endIdx-startInterval; i++) {
		outData[i] = nextBlock.value;
	    }
	    currentIdx = endIdx+1;
	    if (done) {
		break;
	    }
	}
    }

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
	int numEPs = Analysis.getNumUserEntries();
	double returnArray[][] = 
	    new double[numEPs][endInterval-startInterval+1];
	
	for (int ep=0; ep<numEPs; ep++) {
	    pes.reset();
	    for (int i=0; i<pes.size(); i++) {
		int pe = pes.nextElement();
		accumulateIntoArray(type, pe, ep, startInterval, endInterval,
				    returnArray[ep]);
	    }
	}
	return returnArray;
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
	Vector returnVector = new Vector();
	double dataArray[] = null;

	eps.reset();
	for (int i=0; i<eps.size(); i++) {
	    dataArray = new double[endInterval-startInterval+1];
	    int ep = eps.nextElement();
	    pes.reset();
	    for (int j=0; j<pes.size(); j++) {
		int pe = pes.nextElement();
		accumulateIntoArray(type, pe, ep, startInterval, endInterval,
				    dataArray);
	    }
	    returnVector.add(dataArray);
	}
	return returnVector;
    }
}
