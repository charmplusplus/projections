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
  public XAxisFixed(String title_,String units_) {
    title=title_; units=units_;
  }
  
  public String getTitle() {return title;}
  public String getIndexName(int index) {
    return ""+index+units;
  }
}



