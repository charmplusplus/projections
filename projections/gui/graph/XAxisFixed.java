/**
 * XAxisFixed-- a simple static implementation of XAxis.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;
import projections.gui.*;

public class XAxisFixed extends XAxis
{
  private String title;
  private String units;

// for XAxis starting with a value other than 0. default 0
  private double start = 0;
  private double multiplier = 1;

  public XAxisFixed(String title_,String units_) {
    title=title_; units=units_;
  }

  public void setLimits(double start_, double multiplier_){  
    start = start_;	
    multiplier = multiplier_;
  }

  public String getTitle() {return title;}
  public String getUnits() {return units;}

  public String getIndexName(int index) {
      double temp = (index * multiplier) + start;
      // Fix to finally get a proper time-based x-axis
      // it implies that if we pass time values, it must be in us.
      if (units.equals("Time")) {
	  // for the X-Axis, there is no need for too many dec places
	  // MIGHT NEED FURTHER FINE-TUNING.
	  return U.t((long)temp,2);
      }
      return ""+temp+units;
  }

  public double getIndex(int index) {
	double temp = (index * multiplier) + start;
        return temp;
  }
 
  public double getMultiplier(){
	return multiplier;
  }

}



