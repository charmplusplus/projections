package projections.gui.graph;

import projections.gui.*;
import java.awt.Color;

/**
 *
 *  Written by Chee Wai Lee
 *  4/4/2002
 *
 *  DataSource object used for Multirun analysis.
 *
 */

public class MultiRunDataSource extends DataSource
{
    // actual data definitions
    double dataValues[][];
    Color colorMap[]=null;       // colors associated with each value
    String title;
    
    /**
     *  Constructor. Ndata and NcolorMap are expected to be allocated
     *  by the creating object and not to be modified.
     *  MultiRunDataSource is also guaranteed not to modify the data.
     */
    public MultiRunDataSource(double Ndata[][], 
			      Color NcolorMap[],
			      String Ntitle) {
	dataValues = Ndata;
	colorMap = NcolorMap;
	title = Ntitle;
    }

    /**
     *  Returns the string describing this data source. Presumably to
     *  be used by the Graph renderer.
     *
     *  Will be used by MultiRunTextRenderer to construct appropriate
     *  sub-table entries.
     */
    public String getTitle() {
	if (title != null) {
	    return title;
	} else {
	    return "";
	}
    }
    
    /**
     * Return the number of indices (X axis points) in the problem.
     */
    public int getIndexCount() {
	if (dataValues != null) {
	    return dataValues.length;
	} else {
	    return 0;
	}
    }
  
    /**
     * Return the number of values (Y axis points) associated with each index.
     */
    public int getValueCount() {
	if (dataValues != null) {
	    return dataValues[0].length;
	} else {
	    return 0;
	}
    }
  
    /**
     *  Return the color associated with the Y value index
     *
     */
    public Color getColor(int valNo) {
	// if no color map defined, use the superclass's predefined
	// getColor method.
	if (colorMap == null) {
	    return super.getColor(valNo);
	}
	return colorMap[valNo];
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
	for (int j=0;j<getValueCount();j++) {
	    values[j] = dataValues[index][j];
	}
    }
  
    // public String[] getPopup(int index,int valNo,double value) 
    //    - implemented in superclass for now

    // public void mouseClicked(int index,int valNo,double value,
    //                          java.awt.event.MouseEvent evt)
    //    - implemented in superclass for now
}
