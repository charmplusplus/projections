/**
 * DataSource-- provide the actual data to be graphed.
 *
 *   A DataSource maps an "index" (integer, from the X axis) 
 * onto a list of "values" (doubles, for the Y axis), which get
 * plotted as bars or connected lines.
 *   The indices run from 0 to getLastIndex()-1.  Each
 * index must produce the same number of values with 
 * the same set of colors.
 * 
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;
import projections.gui.*;
import java.awt.Color;

public abstract class DataSource
{
  /**
   * Return a human-readable string to describe this data.
   *  e.g., "Usage Profile"
   */
  public String getTitle() {return "";}

  /**
   * Return the number of indices (X axis points) in the problem.
   */
  public abstract int getIndexCount();
  
  /**
   * Return the number of values (Y axis points) associated with each index.
   */
  public abstract int getValueCount();
  
  /**
   * Return the Color of this value.
   *  The valNo passed in is between 0 and getValueCount()-1, inclusive.
   *  The default is to cycle through white, red, blue, and gray.
   *  Entry points should be colored with the standard entry point colors.
   */
  public Color getColor(int valNo) {
    switch (valNo%4) {
    case 0: return Color.white;
    case 1: return Color.red;
    case 2: return Color.blue;
    case 3: return Color.gray;
    }
    return Color.green;/*<- for whining compilers*/
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
   *   you fill in the array.  (FIXME: might want a "getStacked()" method instead)
   */
  public abstract void getValues(int index,double[] values);
  
  
  /**
   * Return a set of strings to show in a popup when the mouse 
   *     is over this bar.
   * Return null (the default) if you have nothing to display.
   */
  public String[] getPopup(int index,int valNo,double value) {
    return null;
  }
  
  /**
   * The user just clicked at this location on this bar.
   *    The default is to ignore the click.
   */
  public void mouseClicked(int index,int valNo,double value,java.awt.event.MouseEvent evt)
    {}
}

