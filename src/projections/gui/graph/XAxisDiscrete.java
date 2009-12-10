package projections.gui.graph;

import java.util.LinkedList;
import java.util.ListIterator;

public class XAxisDiscrete
    extends XAxis
{
    private String title;
    private String discreteNames[];
    
    public XAxisDiscrete(String title, LinkedList discreteList) {
	this.title = title;
	try {
	    discreteNames = new String[discreteList.size()];
	    for (int i=0; i<discreteNames.length; i++) {
		discreteNames[i] = (String)discreteList.get(i);
	    }
	} catch (ClassCastException e) {
	    ListIterator temp = discreteList.listIterator();
	    discreteNames = new String[discreteList.size()];
	    int count = 0;
	    try {
		while (temp.hasNext()) {
		    discreteNames[count++] = 
			((Integer)temp.next()).toString();
		}
	    } catch (ClassCastException evt) {
		System.err.println("Internal Error: XAxisDiscrete expects " +
				   "either an Integer or String LinkedList " +
				   ". Please report this error to a " +
				   "developer.");
		System.err.println(evt);
		System.exit(-1);
	    }
	}
    }

    public String getTitle() {
	return title;
    }

   /**
    *   Return the human-readable name of this index.
    *   Indices run from 0 to DataSource.getLastIndex()-1.
    *   Not all indices will necessarily have their name displayed.
    * e.g., "7", "10-11ms"
    */
    public String getIndexName(int index) { return discreteNames[index]; }
    public double getMultiplier() { return 1;}
}
