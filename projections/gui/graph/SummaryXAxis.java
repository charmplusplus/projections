package projections.gui.graph;

import projections.gui.*;

public class SummaryXAxis extends XAxis
{
    private long intervalSize;

    public SummaryXAxis(int numIntervals, long intervalSize) {
	this.intervalSize = intervalSize;
    }

   /**
    * Return a human-readable string to describe this axis.
    *  e.g., "Processor Number", or "Time Interval"
    */
   public String getTitle() {
       return "Time Interval (" + U.t(intervalSize) + ")";
   }

   /**
    * Return the human-readable name of this index.
    *   Indices run from 0 to DataSource.getLastIndex()-1.
    *   Not all indices will necessarily have their name displayed.
    * e.g., "7", "10-11ms"
    */
   public String getIndexName(int index) {
       return String.valueOf(index);
   }
}
