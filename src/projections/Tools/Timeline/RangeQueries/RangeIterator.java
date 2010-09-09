package projections.Tools.Timeline.RangeQueries;

import java.util.Iterator;

/** An iterator that only returns values from the desired range from within another provided iterator */
public class RangeIterator<T extends Range1D> implements Iterator<T> {
	private Iterator<T> backingIterator;
	
	private T next;
	
	private long lb;
	private long ub;
	
	private void findNextInRange(){
		next = null;
		while(backingIterator.hasNext()){
			T o = backingIterator.next();
			if(o.lowerBound() <= ub && o.upperBound() >= lb){
				next = o;
				break;
			}
		}
	}
	
	private RangeIterator(){
		// Must not construct without bounds
	}
	
	public RangeIterator(Iterator<T> backingIterator, long lowerBound, long upperBound){
		lb = lowerBound;
		ub = upperBound;
		this.backingIterator = backingIterator;
		findNextInRange();
	}

	
	public boolean hasNext() {
		// scan to find if we have another object in the range
		return next != null;
	}

	public T next() {
		T oldNext = next;
		findNextInRange();
		return oldNext;
	}

	public void remove(){
		throw new UnsupportedOperationException();
	}

}