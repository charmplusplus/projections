package projections.analysis;

import java.io.*;

public interface IntervalCapableReader {

    /**
     *  getIntervalSize acquires the natural interval size of the
     *  implementing class. This method is essentially there to
     *  force Readers that read point data to specify a natural
     *  interval size for its data.
     */
    public double getIntervalSize();

    /**
     *  loadIntervalData loads the appropriate section of data into the
     *  implementing class's data arrays. This method will make use of
     *  the implementing class's natural intervalSize.
     */
    public void loadIntervalData(long startInterval, long endInterval)
	throws IOException;

    /**
     *  loadIntervalData loads the appropriate section of data into the
     *  implementing class's arrays. intervalSize partitions the entire 
     *  range of data according to the user's requirements and overrides 
     *  the reader's natural interval specs.
     *
     *  EFFICIENCY NOTE :- implementing classes are encouraged to
     *    rebin "on the fly" rather than read, then rebin if intervalSize
     *    is different from the natural interval size.
     */
    public void loadIntervalData(double intervalSize,
				 long startInterval, long endInterval)
	throws IOException;

    /**
     *  IMPLEMENTATION EFFICIENCY HINT METHOD: -
     *    The following prototype method is suggested for implementations
     *    that allow users to provide a limit to the number of intervals
     *    (for example, the screen is incapable of displaying all the data
     *     anyway).
     *
     *    public void loadIntervalData(long intervalsLimit,
     *				 long startInterval, long endInterval)
     *	     throws IOException;
     */
}
