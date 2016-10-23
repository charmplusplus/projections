package projections.Tools.Timeline.RangeQueries;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class MergedIterator<E> implements Iterator<E>{
	private final Iterator<E> iterators[];
	private int index;

	public MergedIterator(Iterator<E>... iters){
		iterators = iters;
		index = 0;
	}
	
	
	@Override
	public boolean hasNext() {
		while (index < iterators.length && !iterators[index].hasNext()) {
			index++;
		}

		return index < iterators.length;
	}

	@Override
	public E next() {
		if (hasNext()) {
			return iterators[index].next();
		}

		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
