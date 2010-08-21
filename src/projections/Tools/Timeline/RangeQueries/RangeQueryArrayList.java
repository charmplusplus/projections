package projections.Tools.Timeline.RangeQueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** Provides a collection interface that provides range queries via iterators. */
public class RangeQueryArrayList <T extends Range1D> implements  Iterable<T>, Collection<T>, Query1D{
	
	ArrayList<T>	backingStorage;
	private long lb;
	private long ub;
	private boolean hasQueryRange;
	
	public RangeQueryArrayList(){
		backingStorage = new ArrayList<T>();
		hasQueryRange = false;
	}
	
	public void setQueryRange(long lb, long ub){
		this.lb = lb;
		this.ub = ub;
		hasQueryRange = true;
	}

	public void clearQueryRange(){
		hasQueryRange = false;
	}
	
	public class DB1DIterator implements Iterator<T> {
		private Iterator<T> backingIterator;
		
		private T next;
		private T oldNext;
		
		private void findNextInRange(){
			next = null;
			while(backingIterator.hasNext()){
				T o = backingIterator.next();
				if((hasQueryRange==false) || (o.lowerBound() <= ub && o.upperBound() >= lb)){
					next = o;
					break;
				}
			}
		}
		
		private DB1DIterator(){
			// Must not construct without bounds
		}
		
		public DB1DIterator(long lowerBound, long upperBound){
			backingIterator = backingStorage.iterator();
			lb = lowerBound;
			ub = upperBound;
			findNextInRange();
		}

		
		public boolean hasNext() {
			// scan to find if we have another object in the range
			return next != null;
		}

		public T next() {
			oldNext = next;
			findNextInRange();
			return oldNext;
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	
	}
	
	
	
	
	@Override
	public Iterator<T> iterator() {
		return new DB1DIterator(lb, ub);
	}

	
	@Override
	public boolean add(T e) {
		return backingStorage.add(e);
	}	

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return backingStorage.addAll(c);
	}

	@Override
	public void clear() {
		backingStorage.clear();
	}

	@Override
	public boolean contains(Object o) {
		return backingStorage.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return backingStorage.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return backingStorage.isEmpty();
	}

	@Override
	public boolean remove(Object o) {
		return backingStorage.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return backingStorage.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return backingStorage.retainAll(c);
	}

	@Override
	public int size() {
		return backingStorage.size();
	}

	@Override
	public Object[] toArray() {
		return backingStorage.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return backingStorage.toArray(a);
	}
	
	
}
