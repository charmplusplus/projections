package projections.gui.graph;

public class MultiRunXAxis
    extends XAxis
{
    private String xLabels[];

    public MultiRunXAxis(String[] strList) {
	xLabels = strList;
    }

    /**
     * Return a human-readable string to describe this axis.
     *  e.g., "Processor Number", or "Time Interval"
     */
    public String getTitle() {
	return "run configuration (pe)[name of machine]";
    }
    
    /**
     * Return the human-readable name of this index.
     *   Indices run from 0 to DataSource.getLastIndex()-1.
     *   Not all indices will necessarily have their name displayed.
     * e.g., "7", "10-11ms"
     */
    public String getIndexName(int index) {
	return xLabels[index];
    }

    public String[] getIndexNames() {
	return xLabels;
    }
}
