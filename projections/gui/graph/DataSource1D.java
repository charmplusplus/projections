/**
 * DataSource1D-- provide simple 1D data to be graphed.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;
import projections.gui.*;
import java.awt.Color;

public class DataSource1D extends DataSource
{
  private String title;
  private GenericGraphWindow parent;
  private int[] data; /*The data to be graphed*/
  
  public DataSource1D(String title_,int[] data_) {
    title=title_;
    data=data_;
	 parent=null;
  }
  
  public DataSource1D(String title_,int[] data_, GenericGraphWindow parent_) {
    title=title_;
    data=data_;
	 parent=parent_;
  }
  public String getTitle() {return title;}

  public int getIndexCount() {return data.length;}
  public int getValueCount() {return 1; /*Because it's 1D data*/ }
  
  public void getValues(int index,double[] values)
  {
    values[0]=(double)data[index];
  }
  
  public String[] getPopup(int xVal, int yVal){
    if(parent == null){
	 	return null;	
	 } 
    return parent.getPopup(xVal, yVal);
  }
}

