package projections.analysis;

/**
 *  Another static utility class for supporting interval-based operations.
 *
 */
public class IntervalUtils {

    /**
     *  A time-based utility for placing a SINGLE time-range of data into 
     *  a bunch of bins. This utility can be used by reading tools for
     *  "on-the-fly" binning of interval data.
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
    public static void fillIntervals(double destData[], double destSize,
				     int destOffset,
				     double sourceStartTime, 
				     double sourceEndTime, 
				     double sourceValue, 
				     boolean discretize) {
	int absStartBin = (int)(sourceStartTime/destSize);
	int absEndBin = (int)(sourceEndTime/destSize);
	int destStartBin = absStartBin - destOffset;
	int destEndBin = absEndBin - destOffset;

	// optimize away conditions with no actions to take
	if ((destStartBin >= destData.length) ||
	    (destEndBin < 0)) {
	    return;
	}

	// boundary values. Regardless of alignment, these always stay the
	// same given the parameters IFF the start and end times DO NOT fall
	// into the same interval.
	double startPercent = 
	    (1 - (sourceStartTime - absStartBin*destSize)/destSize) * 100.0;
	double endPercent = 
	    ((sourceEndTime - absEndBin*destSize)/destSize) * 100.0;
	
	if (absStartBin == absEndBin) {
	    if ((destStartBin >= 0) && (destEndBin < destData.length)) {
		destData[destStartBin] += sourceValue;
		return;
	    }
	} else {
	    // consider adjustments
	    int interveningBins = absEndBin - absStartBin - 1;
	    double fullRangePercent = startPercent + endPercent +
		interveningBins*100.0;
	    double startValue = (startPercent/fullRangePercent)*sourceValue;
	    double endValue = (endPercent/fullRangePercent)*sourceValue;
	    double intervalValue = (100/fullRangePercent)*sourceValue;
	
	    if (destStartBin < 0) {
		destStartBin = 0;
		destData[destStartBin] += intervalValue;
	    } else {
		destData[destStartBin] += startValue;
	    }
	    if (destEndBin >= destData.length) {
		destEndBin = destData.length-1;
		destData[destEndBin] += intervalValue;
	    } else {
		destData[destEndBin] += endValue;
	    }
	    for (int i=destStartBin+1; i<=destEndBin-1; i++) {
		destData[i] += intervalValue;
	    }
	}
    }

    /**
     *  Convenience method for Utilization (%) to Absolute time transformation
     *  for individual data elements.
     */
    public static double utilToTime(double utilization, double timeRange) {
	return (utilization*timeRange)/100;
    }

    /**
     *  Convenience method for Absolute time to Utilization (%) transformation
     *  for individual data elements.
     */
    public static double timeToUtil(double time, double timeRange) {
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
	double lower = 1.0;
	double upper = 10.0;
	double bestSize = 1.0; // initial best size
	int newNumIntervals = 0;

	// find the next larger size
	while (true) {
	    if ((intervalSize > lower) && (intervalSize <= upper)) {
		// good, set the bestSize and determine the new number
		// of intervals
		bestSize = upper;
		newNumIntervals = 
		    (int)java.lang.Math.ceil((intervalSize*numIntervals)/bestSize);
		break;
	    } else {
		// set a new size and try again.
		lower *= 10;
		upper *= 10;
	    }
	}

	while (true) {
	    // now that we've decided the "best" power 10 size
	    if ((newNumIntervals > 10) && (newNumIntervals <= 1000)) {
		// good, accept
		break;
	    } else if ((newNumIntervals <= 10) && (bestSize == 1.0)) {
		// too big, but we cannot go below the 1 microsecond
		// resolution. Also accept.
		break;
	    } else {
		// too big or too small
		if (newNumIntervals <= 10) {
		    // shrink interval size
		    newNumIntervals *= 10;
		    bestSize /= 10;
		} else {
		    // expand interval size
		    newNumIntervals =
			(int)java.lang.Math.ceil(newNumIntervals/10.0);
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
    public static double[] rebin(double data[], double originalSize,
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


    /**
     *   Testing method, please do not use.
     */
    public static void printArray(double data[]) {
	for (int i=0; i<data.length; i++) {
	    System.out.print(data[i] + " ");
	}
	System.out.println();
    }

    public static void main(String args[]) {
	double data[] = new double[10];
	double dataSize = 5.0;
	double startTime = 15.2;
	double endTime = 45.2;
	fillIntervals(data, dataSize, 0, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 2, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
	data = new double[10];
	fillIntervals(data, dataSize, 5, startTime, endTime, 
		      endTime-startTime, false);
	printArray(data);
    }
}
