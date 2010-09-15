package projections.Tools.Timeline.RangeQueries;


/** Objects that have a lower and upper bound that can be used to build a spatial datastructure queriable for overlaps with a specified range */
public interface Range1D {

	long lowerBound();
	long upperBound();
	
	/** Shift all objects by the specified amount. Used to fix Tachyons in Timeline, and possibly elsewhere */
	void shiftTimesBy(long s);
	
}
