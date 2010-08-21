package projections.Tools.Timeline.RangeQueries;


/** Objects that have a lower and upper bound that can be used to build a spatial datastructure queriable for overlaps with a specified range */
public interface Range1D {

	long lowerBound();
	long upperBound();
	
}
