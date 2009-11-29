package projections.gui.graph;

import projections.gui.OrderedIntList;

public class XAxisDiscreteOrdered
    extends XAxis
{
    String title;
    int discreteList[];
    
    public XAxisDiscreteOrdered(String title, OrderedIntList discreteList) {
	this.title = title;
	this.discreteList = new int[discreteList.size()];
	int count = 0;
	discreteList.reset();
	while (discreteList.hasMoreElements()) {
	    this.discreteList[count++] = discreteList.nextElement();
	}
    }

    public String getTitle() {
	return title;
    }

   /**
    * Return the human-readable name of this index.
    *   Indices run from 0 to DataSource.getLastIndex()-1.
    *   Not all indices will necessarily have their name displayed.
    * e.g., "7", "10-11ms"
    */
   public String getIndexName(int index) { return "" + getIndex(index); }
   public double getIndex(int index) { return discreteList[index];}
   public double getMultiplier() { return 1;}
}
