/**
 * YAxis-- describe the basic properties of a bar graph Y axis.
 *   Users will inherit from this class to describe their Y axis.
 *   The Y axis is continuous, in that it consists of a floating-
 *    point range of values.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;

public abstract class YAxis
{
  /**
   * Return a human-readable string to describe this axis.
   *  e.g., "CPU Utilization(%)", or "Queue Length"
   */
  public abstract String getTitle();
  public String getUnits() { return "";}

  /**
   * Return the minimum value on this axis.  This should almost
   * always be zero, which is the default implementation.
   */
  public double getMin() {return 0.0;}

  /**
   * Return the maximum value on this axis.  
   * This must be larger than the value returned by getMin().
   *  e.g., 100.0
   */
  public abstract double getMax();
   
  /**
   * Return the smallest reasonable difference between two axis values.
   *  The default is 1/100 the difference between getMax and getMin.
   *  e.g., 1.0 for discrete values.
   */
//  public double getDifference() {
//    return 0.01*(getMax()-getMin());
//  }

  /**
   * Return the human-readable name for this value.
   * e.g., "17%", "42.96"
   */
  public abstract String getValueName(double value);
}


