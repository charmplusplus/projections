package projections.Tools.Timeline.RangeQueries;

import java.util.Iterator;

/** An iterator that never produces an entry. hasNext is always false. */
public class NullIterator<E> implements Iterator<E>{

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public E next() {
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}


}
