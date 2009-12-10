package projections.analysis;

/**
 *  Another static utility class for supporting interval-based operations.
 *
 */
public class IntervalUtils {

    private static int getDestIndex(long sourceTime, long startInterval,
				   long destSize, boolean isStart) {
	// if we are dealing with an end-point time, we should treat
	// it as belonging to the previous index.
	if (!isStart && (sourceTime%destSize == 0)) {
	    return (int)(sourceTime/destSize - startInterval -1);
	}
	return (int)(sourceTime/destSize - startInterval);
    }

    /**
     *  A time-based utility for placing a SINGLE time-range of data into 
     *  a bunch of bins. This utility can be used by reading tools for
     *  "on-the-fly" binning of interval data.
     *
     *  |--|--|--|--|--|--|--|--|--|--|--|--| == destData
     *         ^
     *         | destOffset
     *
     *  discretize controls the distribution of discrete sourceValues,
     *  ensuring that only discrete values get placed if a source range
     *  spans multiple destination bins.
     *
     *  NOTE: This WILL NOT work for derived data that depends on any
     *    of the properties of the interval itself (eg. utilization
     *    for an interval depends on its interval size). Conversion 
     *    (eg. into absolute time) is hence required for data of that 
     *    sort prior to the use of this method.
     */
    protected static void fillIntervals(double destData[], long destSize,
				     long destStartInterval,
				     long sourceStartTime, 
				     long sourceEndTime, 
				     double sourceTotalValue, 
				     boolean discrete) {
	long destEndInterval = destStartInterval + destData.length - 1;
	long destStartTime = destStartInterval*destSize;
	long destEndTime = (destEndInterval+1)*destSize;
	long sourceTotalTime = sourceEndTime-sourceStartTime;

	// temporary variables for use in trimming and boundary cases
	double adjustedValue = 0.0;
	double fractionFromStart = 0.0;
	double fractionFromEnd = 0.0;
	double contribution = 0.0;

	// CASE 1: source data outside target range, ignore
	if ((sourceEndTime <= destStartTime) ||
	    (sourceStartTime >= destEndTime)) {
	    return;
	}
	
	// Trim source data to fit destination range
	if (sourceStartTime < destStartTime) {
	    fractionFromStart =
		(destStartTime-sourceStartTime)/(double)sourceTotalTime;
	    sourceStartTime = destStartTime;
	}
	if (sourceEndTime > destEndTime) {
	    fractionFromEnd =
		(sourceEndTime-destEndTime)/(double)sourceTotalTime;
	    sourceEndTime = destEndTime;
	}
	sourceTotalTime = sourceEndTime - sourceStartTime;

	/* DEBUG
	System.out.println("---------- Trimming -------------");
	System.out.println(fractionFromStart + " " + fractionFromEnd);
	System.out.println(sourceTotalTime);
	System.out.println(sourceTotalValue);
	*/

	adjustedValue = sourceTotalValue *
	    (1 - fractionFromStart - fractionFromEnd);
	    
	if (discrete) {
	    sourceTotalValue = Math.rint(adjustedValue);
	} else {
	    sourceTotalValue = adjustedValue;
	}

	/* DEBUG
	System.out.println(sourceTotalValue);

	System.out.println("------------- Placement --------------");
	*/

	// CASE 2: source fits into target range. Handle various boundary
	//         conditions.
	int startIndex = 
	    getDestIndex(sourceStartTime, destStartInterval, destSize, true);
	int endIndex = 
	    getDestIndex(sourceEndTime, destStartInterval, destSize, false);
	
	/* DEBUG	    
	System.out.println(sourceStartTime + " " + sourceEndTime);
	System.out.println(startIndex + " " + endIndex);
	System.out.println((startIndex+destStartInterval) + " " + 
			   (endIndex+destStartInterval));
	*/

	// handle special case where the source fits into one destination
	// interval.
	if (startIndex == endIndex) {
	    destData[startIndex] += sourceTotalValue;
	    return;
	}

	// now, handle general case with at least 2 destination intervals.
	fractionFromStart = 
	    ((startIndex+destStartInterval+1)*destSize - sourceStartTime)/
	    (double)sourceTotalTime;
	fractionFromEnd =
	    (sourceEndTime - ((endIndex+destStartInterval)*destSize))/
	    (double)sourceTotalTime;

	/* DEBUG
	System.out.println(fractionFromStart + " " + fractionFromEnd);
	*/

	if (!discrete) {
	    // start boundary contributions
	    contribution += fractionFromStart*sourceTotalValue;
	    destData[startIndex] += fractionFromStart*sourceTotalValue;
	    // end boundary contributions
	    contribution += fractionFromEnd*sourceTotalValue;
	    destData[endIndex] += fractionFromEnd*sourceTotalValue;
	    // main section contributions
	    sourceTotalValue -= contribution;
	    int remainingIntervals = endIndex-startIndex-1;
	    for (int i=startIndex+1; i<endIndex; i++) {
		// **CW** assuming no errors from floating point
		//        computations. May need to be fixed later.
		destData[i] += sourceTotalValue/remainingIntervals;
	    }
	} else {
	    double mainSectionValue = 
		Math.rint(sourceTotalValue*
			  (1-fractionFromStart-fractionFromEnd));
	    double startBoundaryValue = 
		Math.rint((sourceTotalValue - mainSectionValue)*
			  fractionFromStart);
	    double endBoundaryValue =
		sourceTotalValue - mainSectionValue - startBoundaryValue;
	    destData[startIndex] += startBoundaryValue;
	    destData[endIndex] += endBoundaryValue;
	    double offset = 0.0;
	    // **CW** assuming no errors from floating point computations.
	    //        May need to be fixed later.
	    contribution = mainSectionValue/(endIndex-startIndex-1);
	    for (int i=startIndex+1; i<endIndex; i++) {
		offset += contribution;
		destData[i] += Math.floor(offset);
		offset -= Math.floor(offset);
		// handle floating point error at the end of the array.
		if (i == endIndex-1) {
		    destData[i] += Math.rint(offset);
		}
	    }
	}
    }

    /**
     *  Convenience method for Utilization (%) to Absolute time transformation
     *  for individual data elements.
     */
    protected static double utilToTime(double utilization, double timeRange) {
	return (utilization*timeRange)/100;
    }

    /**
     *  Convenience method for Absolute time to Utilization (%) transformation
     *  for individual data elements.
     */
    protected static double timeToUtil(double time, double timeRange) {
	return (time/timeRange)*100;
    }

    /**
     *  Convenience method for Utilization (%) to Absolute time 
     *  transformation for arrays.
     */
    public static void utilToTime(double data[], double originalSize) {
	for (int i=0; i<data.length; i++) {
	    data[i] = (data[i]*originalSize)/100;
	}
    }

    /**
     *  Convenience method for Absolute time to Utilization 
     *  transformation for arrays.
     */
    public static void timeToUtil(double data[], double intervalSize) {
	for (int i=0; i<data.length; i++) {
	    data[i] = (data[i]/intervalSize)*100;
	}
    }

    /**
     *  getBestIntervalSize attempts to fit an interval range into
     *  some interval limit requirement by the user.
     */
    public static double getBestIntervalSize(double intervalSize, 
					     int numIntervals) {
	double bestSize = 1.0;
	int numSlices = 1;

	// find the next larger size
	bestSize = 
	    Math.pow(10.0,Math.ceil(Math.log(intervalSize)/Math.log(10.0)));
	numSlices = 
	    (int)Math.ceil((intervalSize*numIntervals)/bestSize);
	
	while (true) {
	    // now that we've decided the "best" power 10 size
	    if ((numSlices > 10) && (numSlices <= 1000)) {
		// good, accept
		break;
	    } else if ((numSlices <= 10) && (bestSize == 1.0)) {
		// too big, but we cannot go below the 1 microsecond
		// resolution. Also accept.
		break;
	    } else {
		// too big or too small
		if (numSlices <= 10) {
		    // shrink interval size
		    numSlices *= 10;
		    bestSize /= 10;
		} else {
		    // expand interval size
		    numSlices /= 10;
		    bestSize *= 10;
		}
	    }
	}
	return bestSize;
    }

    /**
     *  a general averaging strategy rebin facility.
     *
     *  NOTE: This method does not perform the rebin operation in-place
     *    and is NOT intended to be used for super-large arrays.
     */
    private static double[] rebin(double data[], double originalSize,
				 int newNumIntervals) {
	double returnArray[];
	returnArray = new double[newNumIntervals];
	double newSize = (data.length*originalSize)/newNumIntervals;
	for (int i=0; i<data.length; i++) {
	    //	    System.out.println("Processing element " + i);
	    // find new start bin to place data.
	    int binStartIdx = 
		(int)java.lang.Math.floor((i*originalSize)/newSize);
	    // the last value will always fall into the last interval, need
	    // to be special case to avoid exact value problems.
	    int binEndIdx;
	    if (i==data.length-1) {
		binEndIdx = newNumIntervals-1;
	    } else {
		binEndIdx = 
		    (int)java.lang.Math.floor(((i+1)*originalSize)/newSize);
	    }
	    // get appropriate portions of the start and end bin data to
	    // be filled into the new array.
	    if (binStartIdx == binEndIdx) {
		// CASE 1: No boundary crossing
		returnArray[binStartIdx] += data[i];
	    } else {
		// CASE 2: Boundaries crossed.
		double startProportion =
		    ((binStartIdx+1)*newSize - i*originalSize)/originalSize;
		returnArray[binStartIdx] += data[i]*startProportion;
		double endProportion =
		    ((i+1)*originalSize - binEndIdx*newSize)/originalSize;
		returnArray[binEndIdx] += data[i]*endProportion;
		// distribute the rest evenly across any remaining new
		// array sections.
		double remaining =
		    data[i]*(1-startProportion-endProportion);
		double pieSlice = 0.0; // assume no pie slice
		if (binEndIdx - binStartIdx - 1 > 0) {
		    pieSlice = remaining/(binEndIdx - binStartIdx - 1);
		}
		for (int j=binStartIdx+1; j<binEndIdx; j++) {
		    returnArray[j] += pieSlice;
		}
	    }
	}
	return returnArray;
    }

    /**
     *  A convenience method wrapper for rebin that takes size as a
     *  parameter instead of the total number of intervals.
     */
    public static double[] rebin(double data[], double originalSize,
				 double newSize) {
	int newNumIntervals = 
	    (int)java.lang.Math.ceil((data.length*originalSize)/newSize);
	return rebin(data, originalSize, newNumIntervals);
    }

//    /**
//     *  Simple method for tools that need to know the absolute time bin a
//     *  time value falls into given the interval size.
//     */
//    public static int getBin(long intervalSize, long time) {
//	return (int)(time/intervalSize);
//    }


    /**
     *   Testing method, please do not use.
     */
    private static void printArray(double data[]) {
	for (int i=0; i<data.length; i++) {
	    System.out.print(data[i] + " ");
	}
	System.out.println();
    }

    public static void main(String args[]) {
	double data[];
	long dataSize = 100;
	long startTime = 150;
	long endTime = 450;
	/*
	*/
	data = new double[10];
	fillIntervals(data, dataSize, 0, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 10, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 14, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 15, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 40, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 44, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 45, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	/*
	*/
    }
}
