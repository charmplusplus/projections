package projections.gui;

public class GraphUtil {

    // GraphUtil will also treat interval size as microseconds.

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

    // a Utilization to Absolute time transformation method
    public static void utilToTime(double data[], double originalSize) {
	for (int i=0; i<data.length; i++) {
	    data[i] = (data[i]*originalSize)/100;
	}
    }

    // a Absolute time to Utilization transformation method
    public static void timeToUtil(double data[], double intervalSize) {
	for (int i=0; i<data.length; i++) {
	    data[i] = (data[i]/intervalSize)*100;
	}
    }

    // a general averaging strategy rebin facility.
    public static double[] rebin(double data[], double originalSize,
				 int newNumIntervals) {
	double returnArray[];

	//	System.out.println("Desired new intervals = " + newNumIntervals);
	returnArray = new double[newNumIntervals];
	double newSize = (data.length*originalSize)/newNumIntervals;
	//	System.out.println("New size = " + newSize);
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
	    //	    System.out.println((i+1)*originalSize/newSize);
	    //	    System.out.println(binStartIdx + " ," + binEndIdx);
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

    public static double[] rebin(double data[], double originalSize,
				 double newSize) {
	int newNumIntervals = 
	    (int)java.lang.Math.ceil((data.length*originalSize)/newSize);
	return rebin(data, originalSize, newNumIntervals);
    }

    public static void printArray(double data[]) {
	for (int i=0; i<data.length; i++) {
	    System.out.print(data[i] + " ");
	}
	System.out.println();
    }

    public static void sanityCheck(double data[]) {
	double outvalue = 0.0;
	for (int i=0; i<data.length; i++) {
	    outvalue += data[i];
	}
	System.out.println("Sanity: " + outvalue);
    }

    public static void main(String args[]) {
	double data[] = { 50, 50, 100, 100, 50, 50, 100, 100, 50, 50 };
	double newData[];
	/*
	newData = rebin(data, 10.0, 3.0);
	printArray(data);
	printArray(newData);
	sanityCheck(data);
	sanityCheck(newData);
	*/
	System.out.println(getBestIntervalSize(10, 1));
    }
}
