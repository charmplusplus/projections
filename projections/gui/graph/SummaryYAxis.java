package projections.gui.graph;


public class SummaryYAxis extends YAxis
{
    /**
     * Return a human-readable string to describe this axis.
     *  e.g., "CPU Utilization(%)", or "Queue Length"
     */
    public String getTitle() {
	return "CPU Utilization(%)";
    }
    
    /**
     * Return the maximum value on this axis.  
     * This must be larger than the value returned by getMin().
     *  e.g., 100.0
     */
    public double getMax() {
	return 100.0;
    }

    /**
     * Return the human-readable name for this value.
     * e.g., "17%", "42.96"
     */
    public String getValueName(double value) {
	return String.valueOf(Math.round(value)) + "%";
    }
}


