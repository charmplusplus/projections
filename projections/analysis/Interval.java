package projections.analysis;

/**
 *  A data structure for storing interval information.
 *  Question: Should we preserve the context of the generation of
 *     an interval within the interval itself?
 *
 *  It is intended as a support mechanism for projections tools that
 *  need to know which interval bins to update given some time information.
 * 
 *  It can be used in the following ways:
 *  1) starting time from 0, find the bins for an interval given a begin
 *     time and end time. Use reference index 0.
 *  2) starting time from the beginning of any arbitrary interval, find the
 *     absolute bin indices with respect to the true start time. Use reference
 *     index to be the index of the starting interval.
 *  3) starting time from the beginning of any arbitrary interval, find the
 *     relative bin indices with respect to the specified starting interval.
 *     Use reerence index 0.
 */
public class Interval {
    private long beginInterval;
    private long endInterval;
    private double intervalSize; // context information.

    private double percentBegin;
    private double percentEnd;

    private long firstIntervalIndex; // context information.

    public Interval(long beginInterval, long endInterval,
		    double percentBegin, double percentEnd,
		    double intervalSize, long firstIntervalIndex) {
	this.beginInterval = beginInterval;
	this.endInterval = endInterval;
	this.percentBegin = percentBegin;
	this.percentEnd = percentEnd;
	this.intervalSize = intervalSize;
	this.firstIntervalIndex = firstIntervalIndex;
    }

    // Accessor methods
    /**
     *  This accessor returns the index of the starting interval beginning
     *  from the value returned by getReferenceIntervalIndex().
     */
    public long getStartIndex() {
	return beginInterval;
    }

    /**
     *  This accessor returns the index of the ending interval beginning
     *  from the value returned by getReferenceIntervalIndex().
     */
    public long getEndIndex() {
	return endInterval;
    }

    /**
     *  This accessor returns the proportion (in %) of the starting interval
     *  derived from the exact start point specified when the Interval
     *  object was created. (see create())
     */
    public double getStartProportion() {
	return percentBegin;
    }

    /**
     *  This accessor returns the proportion (in %) of the ending interval
     *  derived from the exact end point specified when the Interval
     *  object was created. (see create())
     */
    public double getEndProportion() {
	return percentEnd;
    }

    /**
     *  This accessor returns context information about the size of each
     *  interval in the appropriate time unit (in our case, usually micro
     *  seconds).
     */
    public double getIntervalSize() {
	return intervalSize;
    }

    /**
     *  This accessor returns the index value of the reference starting
     *  point.
     */
    public long getReferenceIntervalIndex() {
	return firstIntervalIndex;
    }

    /**
     *  creates an interval object using the given parameters. This is
     *  a wrapper class that assumes that 0 is the starting interval
     *  index prescribed.
     *
     *  startTime is assumed to be the exact start point for the
     *  reference interval firstIntervalIndex.
     */
    public static Interval create(double startTime, double intervalSize, 
				  double beginTime, double endTime) {
	return create(startTime, intervalSize, beginTime, endTime, 0);
    }

    public static Interval create(double startTime, double intervalSize,
				  double beginTime, double endTime,
				  long firstIntervalIndex) {
	// find the actual starting interval
	long beginInterval = firstIntervalIndex +
	    (long)Math.floor((beginTime - startTime)/intervalSize);
	// find the actual ending interval
	long endInterval = firstIntervalIndex +
	    (long)Math.floor((endTime - startTime)/intervalSize);
	double percentBegin;
	double percentEnd;
	// find the percentages
	// case 1: if both times are in the same interval then percentBegin
	//         and percentEnd are the same. The calling code will have 
	//         to take care NOT to double count by checking the interval
	//         range.
	if (beginInterval == endInterval) {
	    percentBegin = ((endTime - beginTime)/intervalSize)*100;
	    percentEnd = percentBegin;
	} else {
	    percentBegin =
		((beginTime - beginInterval*intervalSize)/intervalSize)*100;
	    percentEnd =
		((endTime - endInterval*intervalSize)/intervalSize)*100;
	}
	return new Interval(beginInterval, endInterval, 
			    percentBegin, percentEnd, 
			    intervalSize, firstIntervalIndex);
    }
}
