/**
 * DataSource1D-- provide simple 1D data to be graphed.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;
import projections.gui.GenericGraphWindow;
import projections.gui.PopUpAble;

public class DataSource1D extends DataSource
{
  private String title;
  private GenericGraphWindow parent;
  private int[] data; /*The data to be graphed*/
  private boolean[] mask = null;
  
    // **CW** why oh why do I hack like this ...
    private boolean usePopUpAbleParent = false;
    private PopUpAble popParent;

  public DataSource1D(String title_,int[] data_) {
    title=title_;
    data=data_;
	 parent=null;
  }

//    // **CW** Hack-a-di-doo-doo
//    public DataSource1D(String title, int[] data, PopUpAble parent) {
//	this.title = title;
//	this.data = data;
//	popParent = parent;
//	usePopUpAbleParent = true;
//    }

  public String getTitle() {return title;}

  public int getIndexCount() {return data.length;}
  public int getValueCount() {return 1; /*Because it's 1D data*/ }
  
  public void setMask(boolean[] mask) {
    this.mask = mask;
  }

  public void getValues(int index,double[] values)
  {
    values[0] = (mask != null && !mask[index]) ? 0 : data[index];
  }
  
  public String[] getPopup(int xVal, int yVal){
      // **CW** sqwark!
      if (usePopUpAbleParent) {
	  return popParent.getPopup(xVal, yVal);
      }
    if(parent == null){
	 	return null;	
	 } 
    return parent.getPopup(xVal, yVal);
  }
}

