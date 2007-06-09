/**
 * A simple implementation of a YAxis where everything is fixed.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;

public class YAxisFixed extends YAxis
{
  private String title;
  private String units;
  private double max;

  public YAxisFixed(String title_,String units_,double max_) {
    title=title_; units=units_; max=max_;
  }
  
  public String getTitle() {return title;}
  public String getUnits() {return units;}
  public double getMax() {return max;}

  public String getValueName(double value) {
    return ""+value+units;
  }
}


