package projections.gui.graph;

import java.util.SortedSet;

public class XAxisDiscreteOrdered
    extends XAxis
{
    private String title;
    private int discreteList[];
    
    public XAxisDiscreteOrdered(String title, SortedSet<Integer> discreteList) {
    	this.title = title;
    	this.discreteList = new int[discreteList.size()];
    	int count = 0;

    	for(Integer n : discreteList){
    		this.discreteList[count++] = n;		
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
   public String getIndexName(int index) { return "" + discreteList[index]; }
   public double getIndex(int index) { return discreteList[index];}
   public double getMultiplier() { return 1;}
}
