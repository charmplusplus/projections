package projections.Tools.Timeline.RangeQueries;

/** An interface for collections whose iterators will only return Range1D objects that 
 *  overlap the range specified by setQueryRange. By default all collection items will
 *  be returned by the iterators.
 * 
 *  @author Isaac Dooley
 */
public interface Query1D {

	/** Specify the range for all subsequent iterators to use. 
	 * 
	 *  An iterator's behavior is undefined if this is called after its creation. 
	 *  
	 */
	public void setQueryRange(long lb, long ub);


	/** Clear range for all subsequent iterators to use. 
	 * 
	 *  An iterator's behavior is undefined if this is called after its creation. 
	 *  
	 */
	public void clearQueryRange();

}
