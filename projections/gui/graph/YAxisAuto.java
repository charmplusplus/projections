/**
 * A YAxis that determines its max from a datasource.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;
import projections.gui.*;

public class YAxisAuto extends YAxisFixed
{
  private static double sourceToMax(DataSource data) {
  
	 
    double max=0;
    int ni=data.getIndexCount(),nv=data.getValueCount();
    double[] values=new double[nv];
	 
    for (int i=0;i<ni;i++) {
	   data.getValues(i,values);
      for (int v=0;v<nv;v++) {
			if (values[v]>max) max=values[v];
      }
    }
    /*delete[] values*/  
    return max;
  }

  public YAxisAuto(String title_,String units_,DataSource data) {
    super(title_,units_,sourceToMax(data));
  }
}


