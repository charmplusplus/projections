package projections.analysis;

/**
 *  Another static utility class for supporting interval-based operations.
 *
 */
public class IntervalUtils {

    /**
     *  a general averaging strategy rebin facility.
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
}
