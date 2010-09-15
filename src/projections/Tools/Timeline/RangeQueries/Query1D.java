package projections.Tools.Timeline.RangeQueries;

import java.util.Collection;
import java.util.Iterator;


/** An interface for collections whose iterators will only return Range1D objects that 
 *  overlap the specified range. By default all collection items will
 *  be returned by the iterators.
 * 
 *  @author Isaac Dooley
 */
public interface Query1D<T extends Range1D> extends Iterable<T>, Collection<T> {

	/** Get an iterator that only returns entries overlapping the specified range. 
	 * This call is thread safe.
 	 * Behavior of existing iterators are undefined if the underlying collection is modified.
	 */
	public Iterator<T> iterator(long lowerBound, long upperBound);

	public void removeEntriesOutsideRange(long startTime, long endTime);

	public void shiftAllEntriesBy(long shift);

}
