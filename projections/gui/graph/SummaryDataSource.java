package projections.gui.graph;

import projections.gui.*;
import java.awt.Color;

public class SummaryDataSource extends DataSource
{
    private int numIntervals;
    private double dataValues[];
    
    public SummaryDataSource(double[] data) {
	dataValues = data;
	numIntervals = data.length;
	//    System.out.println("SumDatSour "+numIntervals);
    }
    
    public String getTitle() { return "Utilization Graph (Summary)"; }
    
    /**
     * Return the number of indices (X axis points) in the problem.
     */
    public int getIndexCount() { return numIntervals; }
    
    /**
     * Return the number of values (Y axis points) associated with each index.
     */
    public int getValueCount() { return 1; }
    
    public Color getColor(int valNo) { return Analysis.foreground; }

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
	values[0] = dataValues[index];
    }
    
    public String[]getPopup(int xVal, int yVal){
   	return null;
    }
}
