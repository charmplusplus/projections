package projections.gui;

import projections.analysis.*;

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

    /**
     *  Convenience method that simply calls the same method from
     *  IntervalUtils.java in projections.analysis.
     */
    public static double[] rebin(double data[], double originalSize,
				 int newNumIntervals) {
	return IntervalUtils.rebin(data, originalSize, newNumIntervals);
    }

    /**
     *  Convenience method that simply calls the same method from
     *  IntervalUtils.java in projections.analysis.
     */
    public static double[] rebin(double data[], double originalSize,
				 double newSize) {
	return IntervalUtils.rebin(data, originalSize, newSize);
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
