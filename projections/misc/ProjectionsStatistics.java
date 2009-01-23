package projections.misc;

/**
 *  Written by Chee Wai Lee
 *  2/5/2003
 *
 *  This is meant to be a statistical encapsulation of a stream of data.
 *  It can be inherited to provide more powerful statistical data from
 *  a basic stream of double values.
 */
public class ProjectionsStatistics {

    // "temporaries" for one set of data
    protected long count;
    protected double sum;
    protected double sumOfSquares;
    protected double max;
    protected double min;

    public ProjectionsStatistics() {
    }

    /**
     *  Performs a simple accumulation into the encapsulation of a single
     *  double value data, allowing for a stream of single data pieces.
     */
    public void accumulate(double item) {
	addData(item);
    }

    /**
     *  Performs the accumulation given a fixed length array.
     */
    public void accumulate(double items[]) {
	for (int i=0; i<items.length; i++) {
	    addData(items[i]);
	}
    }

    /**
     *  Resets the statistics data. Allows this object to be reused
     *  if necessary.
     */
    public void reset() {
	count = 0;
	sum = 0;
	sumOfSquares = 0;
	max = Double.MIN_VALUE;
	min = Double.MAX_VALUE;
    }

    // standard convenience utility methods.
    private void addData(double item) {
	count++;
	sum += item;
	sumOfSquares += item*item;
	if (item > max) {
	    max = item;
	}
	if (item < min) {
	    min = item;
	}
    }

    // Accessor methods (which also computes the appropriate 
    // derived statistics on demand from the accumulated data).

    public double getSum() {
	return sum;
    }

    public double getMean() {
	return sum/count;
    }

    public double getMax() {
	return max;
    }

    public double getMin() {
	return min;
    }

    /**
     *  Returns the variance of the data entered so far. Assumes
     *  count-1 degrees of freedom.
     */
    public double getVariance() {
	double mean = getMean();
	return (sumOfSquares - 2*mean*sum + mean*mean*count) / (count - 1);
    }

    /**
     *  This variation is used to avoid recalculating the mean if it has
     *  been acquired previously (since ProjectionsStatistics will not
     *  try to "remember" derived data).
     *
     *  NOTE: Care has to be taken when used asynchronously because more
     *        data could have been accumulated between the two calls.
     */
    public double getVariance(double mean) {
	return (sumOfSquares - 2*mean*sum + mean*mean*count) / (count - 1);
    }

    public double getStdDeviation() {
	double variance = getVariance();
	return java.lang.Math.sqrt(variance);
    }

    /**
     *   See comments on getVariance(double mean)
     */
    public double getStdDeviation(double variance) {
	return java.lang.Math.sqrt(variance);
    }
}
