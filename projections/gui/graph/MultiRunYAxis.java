package projections.gui.graph;

public class MultiRunYAxis
    extends YAxis
{
    // mode constants
    public static final int TIME = 1;
    public static final int MSG = 2;
    public static final int FP_NUMBER = 3;

    private int mode = TIME; // default
    private String title;
    private double max;
    private double min = 0.0;

    public MultiRunYAxis(int Nmode, String Ntitle, double Nmax) {
	mode = Nmode;
	title = Ntitle;
	max = Nmax;
    }

    public MultiRunYAxis(int Nmode, String Ntitle, double Nmax, double Nmin) {
	mode = Nmode;
	title = Ntitle;
	max = Nmax;
	min = Nmin;
    }

    /**
     * Return a human-readable string to describe this axis.
     *  e.g., "CPU Utilization(%)", or "Queue Length"
     */
    public String getTitle() {
	return title;
    }
    
    /**
     * Return the minimum value on this axis.  This should almost
     * always be zero, which is the default implementation.
     */
    public double getMin() {
	return min;
    }
    
    /**
     * Return the maximum value on this axis.  
     * This must be larger than the value returned by getMin().
     *  e.g., 100.0
     */
    public double getMax() {
	if (max > min) {
	    return max;
	} else {
	    return min;
	}
    }
    
    /**
     * Return the smallest reasonable difference between two axis values.
     *  The default is 1/100 the difference between getMax and getMin.
     *  e.g., 1.0 for discrete values.
     */
    public double getDifference() {
	return 0.01*(getMax()-getMin());
    }

    /**
     * Return the human-readable name for this value.
     * e.g., "17%", "42.96"
     * ***CURRENT IMP***
     * The future version should have some pretty-print facilities based
     * on U.java or using techniques similar to U.java.
     * If value represents time, then it is always in microseconds.
     */
    public String getValueName(double value) {
	if (mode == TIME) {
	    return Long.toString((long)value)+"us";
	} else if (mode == MSG) {
	    return Long.toString((long)value);
	} else if (mode == FP_NUMBER) {
	    return Double.toString(value);
	}
	return null;
    }
}


