/**
 * DataSource2D-- provide simple 2D data to be graphed.
**/

package projections.gui.graph;
import projections.gui.*;
import java.awt.Color;

public class DataSource2D extends DataSource
{
  private String title;
  private double[][] data; /*The data to be graphed*/
  private int xValues;

  public DataSource2D(String title_,double[][] data_) {
    title=title_;
    data=data_;
    xValues = data.length;
  }

  public String getTitle() {return title;}

  public int getIndexCount() {return xValues;}	
  public int getValueCount() {return data[0].length; }  //assuming that all x values have equal number of corresponding y values 
  
  public void getValues(int index,double[] values)
  {
    for(int j=0;j<data[index].length;j++)
	values[j]=(double)data[index][j];
  }
}
