package projections.gui.graph;

import projections.gui.*;
import java.awt.Color;
import java.text.DecimalFormat;

public class SummaryDataSource extends DataSource
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    private int startInterval;
    private int numIntervals;
    private double dataValues[][];
    
    public SummaryDataSource(double[][] data, int startInt) {
	dataValues = data;
	numIntervals = data.length;
	startInterval = startInt;
    }
    
    public String getTitle() { return "Utilization Graph (Summary)"; }
    
    /**
     * Return the number of indices (X axis points) in the problem.
     */
    public int getIndexCount() { return numIntervals; }
    
    /**
     * Return the number of values (Y axis points) associated with each index.
     */
    public int getValueCount() { return dataValues[0].length; }
    
    public Color getColor(int valNo) { 
	switch (valNo) {
	case 0:
	    return Color.red;
	case 1:
	    return MainWindow.runObject[myRun].foreground; 
	default:
	    System.err.println("ERROR: Invalid value " + valNo + " requested for Summary colors");
	    System.exit(-1);
	}
	return null;
    }

    /**
     * Return the values associated with this index.
     *  The index passed in is between 0 and getIndexCount()-1, inclusive.
     *  The values array to be filled out is passed in, rather than returned
     *    to reduce the dynamic allocation and garbage collection overhead.  
     *  The values array has getValueCount() elements.
     *
     * Values[] will be sorted and plotted in the apropriate color along 
     *   the Y axis.  To simulate a stacked bar graph, sum the values as
     *   you fill in the array.  
     * (FIXME: might want a "getStacked()" method instead)
     */
    public void getValues(int index, double[] values) {
	for (int val=0; val<getValueCount(); val++) {
	    values[val] = dataValues[index][val];
	}
    }
    
    public String[]getPopup(int xVal, int yVal){
	String[] ret = new String[3];

	if (yVal == 0) {
	    ret[0] = "Average Processor Utilization (%)";
	} else {
	    ret[0] = "Average Idle Percentage";
	}
	ret[1] = "Interval: " + (xVal + startInterval);
	DecimalFormat numFormat = new DecimalFormat("###.00");
	ret[2] = "Percent: " + numFormat.format(dataValues[xVal][yVal]);

   	return ret;
    }
}
