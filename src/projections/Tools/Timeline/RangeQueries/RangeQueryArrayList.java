package projections.Tools.Timeline.RangeQueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** Provides a collection interface that provides range queries via iterators. */
public class RangeQueryArrayList <T extends Range1D> implements Query1D<T>{
	
	ArrayList<T>	backingStorage;

	
	public RangeQueryArrayList(){
		backingStorage = new ArrayList<T>();
	}

	
	@Override
	public Iterator<T> iterator() {
			return backingStorage.iterator();
	}
	
	/** For thread-safety, multiple iterators can be constructed using explicit bounds with this method */
	public Iterator<T> iterator(long lowerBound, long upperBound) {
			return new RangeIterator(backingStorage.iterator(), lowerBound, upperBound);
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
	public <E> E[] toArray(E[] a) {
		return backingStorage.toArray(a);
	}
	
	
}
