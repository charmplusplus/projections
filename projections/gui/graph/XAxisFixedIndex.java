/**
 * XAxisFixed-- a simple static implementation of XAxis.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;

public class XAxisFixedIndex extends XAxis
{
  private String title;
  private String units;
  private int minIndex;
  private int maxIndex;

  public int getAxisType() { return Axis.DISCRETE; }

  public XAxisFixedIndex(String title_,String units_, int min, int max) {
    title=title_; 
    units=units_;
    minIndex=min;
    maxIndex=max;
  }
  
  public String getTitle() {return title;}
  public String getUnits() { return units; }
  public int getMaxIndex() { return maxIndex; }
  public int getMinIndex() { return minIndex; }
  public double getMax() { return maxIndex; }
  public double getMin() { return minIndex; }
  public String getValueName(double d) { return null; }
}



