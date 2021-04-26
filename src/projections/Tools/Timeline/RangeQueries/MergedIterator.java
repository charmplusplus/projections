package projections.Tools.Timeline.RangeQueries;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class MergedIterator<E> implements Iterator<E> {
	private final Iterator<E> iterators[];
	private int index;
	private boolean internalHasNext;

	public MergedIterator(Iterator<E>... iters) {
		iterators = iters;
		index = 0;
		loadHasNext();
	}

	@Override
	public final boolean hasNext() {
		return internalHasNext;
	}

	@Override
	public final E next() {
		if (internalHasNext) {
			E next = iterators[index].next();
			loadHasNext();
			return next;
		}
		else
			throw new NoSuchElementException();
	}

	private final void loadHasNext() {
		while (index < iterators.length && !iterators[index].hasNext()) {
			index++;
		}
		internalHasNext = index < iterators.length;
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}
}
