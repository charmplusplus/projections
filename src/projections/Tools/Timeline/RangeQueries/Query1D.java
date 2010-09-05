package projections.Tools.Timeline.RangeQueries;

import java.util.Collection;
import java.util.Iterator;

/** An interface for collections whose iterators will only return Range1D objects that 
 *  overlap the range specified by setQueryRange. By default all collection items will
 *  be returned by the iterators.
 * 
 *  @author Isaac Dooley
 */
public interface Query1D<T extends Range1D> extends Iterable<T>, Collection<T> {

	/** Specify the range for all subsequent iterators to use. 
	 * 
	 *  An iterator's behavior is undefined if this is called after its creation. 
	 *  
	 */
	public void setQueryRange(long lb, long ub);

	
	public Iterator<T> iterator(long lowerBound, long upperBound);

	

	/** Clear range for all subsequent iterators to use. 
	 * 
	 *  An iterator's behavior is undefined if this is called after its creation. 
	 *  
	 */
	public void clearQueryRange();

}
