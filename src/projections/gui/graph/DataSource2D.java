/**
 * DataSource2D-- provide simple 2D data to be graphed.
 **/

package projections.gui.graph;

import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import projections.gui.Clickable;
import projections.gui.PopUpAble;
import projections.gui.ResponsiveToMouse;

public class DataSource2D extends DataSource
{
    private String title;
    private double[][] data; /*The data to be graphed*/
    private int xValues;
    private ResponsiveToMouse parent;
    
    protected DataSource2D(String title_, double[][] data_) {
	title=title_;
	data=data_;
	xValues = data.length;
	parent = null;
    }
  
    public DataSource2D(String title_, double[][] data_, 
			ResponsiveToMouse parent_) {
	title=title_;
	data=data_;
	parent=parent_;
	xValues = data.length;
    }

//  
//    public DataSource2D(String title_, int[][] data_,
//			ResponsiveToMouse parent_) {
//	title=title_;
//	data=intToDouble(data_);
//	parent=parent_;
//	xValues = data.length;
//    }

    public String[] getPopup(int xVal, int yVal) {
    	int NUM_EXTRA_LINES = 4;
    	if (parent != null) {
    		if (parent instanceof PopUpAble) {
    			String []tempText = ((PopUpAble)parent).getPopup(xVal, yVal);
    			int offset = tempText.length;
    			int longestLength = 0;
    			String []text = new String[offset + NUM_EXTRA_LINES];
    			if (offset == 0) {
    				return tempText;
    			} else {
    				for (int i=0; i<offset; i++) {
    					if (tempText[i].length() > longestLength) {
    						longestLength = tempText[i].length();
    					}
    					text[i] = tempText[i];
    				}
    				// Draw a line
    				text[offset] = new String();
    				for (int i=0; i<longestLength; i++) {
    					text[offset] += "-";
    				}
    				// Compute Statistics
    				int count = 0;
    				double total = 0.0;
    				for (int i=0; i<data[xVal].length; i++) {
    					if (data[xVal][i] > 0.0) {
    						count++;
    						total += data[xVal][i];
    					}
    				}
    				// Display number of non-zero entries
    				//         average value
    				//         total value
    				text[offset+1] = "Number: " + count;
    				if (count > 0) {
    					DecimalFormat df = new DecimalFormat();
    					df.setMaximumFractionDigits(1);
    					text[offset+2] = "Average: " + df.format(total/count);
    				} else {
    					text[offset+2] = "No Average";
    				}
					DecimalFormat df = new DecimalFormat();
					df.setMaximumFractionDigits(2);
    				text[offset+3] = "Total: " + df.format(total);
    			}
    			return text;
    		}
    	} 
    	return null;		
    }

    public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
    	if (parent != null && parent instanceof Clickable) 
    		((Clickable)parent).toolClickResponse(e, xVal, yVal);
    }

    public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
    	if (parent != null && parent instanceof Clickable) 
    		((Clickable)parent).toolMouseMovedResponse(e, xVal, yVal);
    }
    
    
    
    public String getTitle() {
	return title;
    }

    public int getIndexCount() {
	return xValues;
    }
	
    public int getValueCount() {
	// assuming that all x values have equal number of 
	// corresponding y values 
	return data[0].length; 
    }  
  
    public void getValues(int index,double[] values)
    {
	for(int j=0;j<data[index].length;j++)
	    values[j]=data[index][j];
    }

//    private double[][] intToDouble(int[][] data) {
//	double[][] retVal;
//
//	retVal = new double[data.length][];
//	for (int i=0;i<data.length; i++) {
//	    retVal[i] = new double[data[i].length];
//	    for (int j=0;j<data[i].length;j++) {
//		retVal[i][j] = data[i][j];
//	    }
//	}
//
//	return retVal;
//    }
    
}
