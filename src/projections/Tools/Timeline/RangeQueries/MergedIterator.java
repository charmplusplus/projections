package projections.Tools.Timeline.RangeQueries;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class MergedIterator<E> implements Iterator<E>{
	private LinkedList<Iterator<E>> backingIterators;
	
	public MergedIterator(Iterator<E> i1, Iterator<E> i2){
		backingIterators = new LinkedList<Iterator<E>>();
		backingIterators.add(i1);
		backingIterators.add(i2);
	}
	
	
	@Override
	public boolean hasNext() {
		while(! backingIterators.isEmpty()){
			Iterator<E> i = backingIterators.getFirst();
			if(i.hasNext()) {
				return true;
			} else {
				backingIterators.removeFirst();
			}			
		}
		
		return false;
	}

	@Override
	public E next() {
		while(! backingIterators.isEmpty()){
			Iterator<E> i = backingIterators.getFirst();
			if(i.hasNext()) {
				return i.next();
			} else {
				backingIterators.removeFirst();
			}			
		}
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
