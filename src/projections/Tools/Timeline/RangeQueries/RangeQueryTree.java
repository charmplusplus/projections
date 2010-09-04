package projections.Tools.Timeline.RangeQueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/** Provides a collection interface that provides range queries via iterators. Does not yet support removal of entries */
public class RangeQueryTree <T extends Range1D> implements Query1D<T>{

	private final static int MAX_ENTRIES_PER_NODE = 100;

	TreeNode<T> root;
	
	private long lb;
	private long ub;
	private boolean hasQueryRange;
	
	public RangeQueryTree(){
		root = new TreeNode<T>();
		hasQueryRange = false;
	}
	
	/** Create a new RangeQueryTree that is initially populated with the objects from the specified collection. */
	public RangeQueryTree(Collection<? extends T> c){
		root = new TreeNode<T>();
		hasQueryRange = false;
		addAll(c);
		// Rebalance the tree just to make sure future accesses to this tree are fast, as the user will likely not modify the tree again
		root = root.rebalanceTree();
	}
	
	public void setQueryRange(long lb, long ub){
		this.lb = lb;
		this.ub = ub;
		hasQueryRange = true;
	}

	public void clearQueryRange(){
		hasQueryRange = false;
	}
	
	
	
	
	
	@Override
	public Iterator<T> iterator() {
		if(hasQueryRange){
			return root.iterator(lb, ub);
		}
		else
			return root.iterator(Long.MIN_VALUE, Long.MAX_VALUE);
	}

	
	@Override
	public boolean add(T e) {
		root.add(e);

		if( root.numEntriesInSubTree % MAX_ENTRIES_PER_NODE == 0){

			// Turn this on for debugging if data structure doesn't work well:
//			int oldCount = 0;
//			long oldhash = 101010;
//			for(Object o : root){
//				oldCount++;
//				oldhash ^= o.hashCode();
//			}
			
			root = root.rebalanceTree();

			// Turn this on for debugging if data structure doesn't work well:
//			int newCount = 0;
//			long newhash = 101010;
//			for(Object o : root){
//				newCount++;
//				newhash ^= o.hashCode();
//			}
//			if(oldCount != newCount){
//				System.out.println("ERROR: lost something in the rebalancing... old count=" + oldCount + " newCount = " + newCount);
//			}
//			if(oldhash != newhash){
//				System.out.println("ERROR: hash of objects doesn't match after rebalancing.");
//			}

			
//			System.out.println("\nAfter Rebalancing:\n");
//			root.printInfoRecursive(0);
//			System.out.println("\n\n");
		}			
		
		
		return true;
	}	

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for(T o : c){
			add(o);
		}
		return true;
	}

	@Override
	public void clear() {
		root.clear();
	}

	@Override
	public boolean contains(Object o) {
		if(o instanceof Range1D){
			Range1D c = (Range1D) o;
			Iterator iter = root.iterator(c.lowerBound(), c.lowerBound());
			while(iter.hasNext()){
				if(iter.next() == c){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c){
			if(!contains(o)){
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return root.isEmpty();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return root.size();
	}

	@Override
	public Object[] toArray() {
		Object[] retval = new Object[root.size()];
		int i = 0;
		for(Object o : root){
			retval[i++] = o;
		}
		return retval;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// Couldn't quite figure out how to implement this yet... FIXME if you want :)
//		if(a == null)
//			throw new NullPointerException();
//		
//		if(root.size() <= a.length){
//			int i = 0;
//			for(Object o : root){
//				a[i++] = (T)o;
//			}
//			a[i++] = null;
//			return a;
//		} else {
//			Object[] retval = new Object[root.size()];
//			int i = 0;
//			for(Object o : root){
//				retval[i++] = o;
//			}
//			return retval;
//		
//		}
//		
//		return null;
		throw new UnsupportedOperationException();
	}
	
	
	
	
	private class TreeNode<T extends Range1D> implements Iterable<T>{

		/** Data local to this node */
		private ArrayList<T>	data;

		/** lower bound of local data. Updated upon insertion. */
		private long lowerBound;
		private long upperBound;
		
		private int numEntriesInSubTree = 0;
		
		TreeNode<T> leftChild;
		TreeNode<T> rightChild;

		public TreeNode(){
			data = new ArrayList<T>();
			leftChild = null;
			rightChild = null;
			numEntriesInSubTree = 0;
		}
		
		public boolean isEmpty() {
			return (numEntriesInSubTree==0);
		}

		public void clear() {
			data = new ArrayList<T>();
			leftChild = null;
			rightChild = null;
			numEntriesInSubTree = 0;
		}

		/** Create an efficient iterator that just produces the entries overlapping the desired range */
		@Override
		public Iterator<T> iterator() {
			return iterator(Long.MIN_VALUE, Long.MAX_VALUE);
		}
		
		public Iterator iterator(long lb, long ub) {
			if(data == null){
				boolean useLeft = leftChild.lowerBound <= ub && leftChild.upperBound >= lb;
				boolean useRight = rightChild.lowerBound <= ub && rightChild.upperBound >= lb;

				if(useLeft && useRight)
					return new MergedIterator(leftChild.iterator(lb, ub), rightChild.iterator(lb, ub));
				else if(useLeft)
					return leftChild.iterator(lb, ub);
				else if(useRight)
					return rightChild.iterator(lb, ub);
				else
					return new NullIterator();
								
			} else {
				// A local range iterator:
				return new RangeIterator(data.iterator(), lb, ub);
			}
		}

		public int size(){
			return numEntriesInSubTree;
		}
		
		public void printInfoRecursive(int level) {
			for(int i=0; i<level; i++){
				System.out.print("    ");
			}
			if(data != null){
				System.out.println("Leaf node contains " + data.size() + " local data entries, range=(" + lowerBound + "," + upperBound + ") : " + data.toString());
			} else {
				System.out.println("non-leaf node contains " + numEntriesInSubTree + " entries in subtree, range=(" + lowerBound + "," + upperBound + ")");
				leftChild.printInfoRecursive(level+1);
				rightChild.printInfoRecursive(level+1);
			}
		}
		
		
		
		
		public class CompareRange1DByAverage implements java.util.Comparator {
			public int compare(Object a, Object b) {
				long avg_a = (((Range1D)a).lowerBound() + ((Range1D)a).lowerBound())/2;
				long avg_b = (((Range1D)b).lowerBound() + ((Range1D)b).lowerBound())/2;
				return (int) (avg_a - avg_b);
			}
		} 
		
		
		private class newSubtreeRoot {
			public int depth;
			public TreeNode newRoot;
			public newSubtreeRoot(int d, TreeNode r) {
				depth = d;
				newRoot = r;
			}
		}
		
		/**
		 * 
		 * @return depth of newly rebalanced subtree, new subtree root
		 */
		private newSubtreeRoot rebalanceTreeRecursive(){
			if (data != null){
				// Leaf node is already balanced
				return new newSubtreeRoot(0, this);
			}
		
			newSubtreeRoot l = leftChild.rebalanceTreeRecursive();
			newSubtreeRoot r = rightChild.rebalanceTreeRecursive();
			newSubtreeRoot retval = new newSubtreeRoot(1+Math.max(l.depth, r.depth), this);

			// Update tree to incorporate rotations from children
			leftChild = l.newRoot;
			rightChild = r.newRoot;
			
			if(r.depth - l.depth >= 2){
				// rotate left
//				System.out.println("Rotate Left");
				TreeNode pivot = r.newRoot;			
				rightChild = pivot.leftChild;
				pivot.leftChild = this;
				retval.newRoot = pivot;			
				retval.depth = r.depth - 1 + 1;
				
				numEntriesInSubTree = leftChild.numEntriesInSubTree + rightChild.numEntriesInSubTree;
				lowerBound = Math.min(leftChild.lowerBound, rightChild.lowerBound);
				upperBound = Math.max(leftChild.upperBound, rightChild.upperBound);

				pivot.numEntriesInSubTree = pivot.leftChild.numEntriesInSubTree + pivot.rightChild.numEntriesInSubTree;
				pivot.lowerBound = Math.min(pivot.leftChild.lowerBound, pivot.rightChild.lowerBound);
				pivot.upperBound = Math.max(pivot.leftChild.upperBound, pivot.rightChild.upperBound);

				
			} else if (l.depth - r.depth >= 2){
				// rotate right
//				System.out.println("Rotate Right");
				TreeNode pivot = l.newRoot;			
				leftChild = pivot.rightChild;
				pivot.rightChild = this;
				retval.newRoot = pivot;	
				retval.depth = l.depth - 1 + 1;
				
				numEntriesInSubTree = leftChild.numEntriesInSubTree + rightChild.numEntriesInSubTree;
				lowerBound = Math.min(leftChild.lowerBound, rightChild.lowerBound);
				upperBound = Math.max(leftChild.upperBound, rightChild.upperBound);

				pivot.numEntriesInSubTree = pivot.leftChild.numEntriesInSubTree + pivot.rightChild.numEntriesInSubTree;
				pivot.lowerBound = Math.min(pivot.leftChild.lowerBound, pivot.rightChild.lowerBound);
				pivot.upperBound = Math.max(pivot.leftChild.upperBound, pivot.rightChild.upperBound);

			}
			
			return retval;
		}

		/** Rebalance whole tree and return new root */
		public TreeNode rebalanceTree(){
			return rebalanceTreeRecursive().newRoot;
		}
		
	
		boolean add(T entry){
			
			numEntriesInSubTree++;
			
			if(data != null){
				// Leaf Node, add locally
				data.add(entry);
			
				if(data.size() == 1){
					// initialize range describing the data
					lowerBound = entry.lowerBound();
					upperBound = entry.upperBound();
				}
				
				// Split into two children if too large
				if(data.size() > MAX_ENTRIES_PER_NODE){
					leftChild = new TreeNode<T>();
					rightChild = new TreeNode<T>();
					
					// sort values in data
					Collections.sort(data, new CompareRange1DByAverage());	
					  
					// Insert half into left child
					for(int i=0; i<(data.size()/2); i++){
						leftChild.add(data.get(i));
					}
					// Insert half into right child
					for(int i=(data.size()/2); i<data.size(); i++){
						rightChild.add(data.get(i));
					}
					
					data.clear();
					data = null;
					
				}
			} else {
				// Non-leaf node
				// insert into one of the two children
				if(entry.upperBound() < leftChild.upperBound)				
					leftChild.add(entry);	
				else
					rightChild.add(entry);

			}

			
			// Update bounds
			if(entry.lowerBound() < lowerBound)
				lowerBound = entry.lowerBound();
			else if(entry.upperBound() > upperBound)
				upperBound = entry.upperBound();
			
			return true;
		}

	}


	public void printTree() {
		root.printInfoRecursive(0);
	}
	
	
}
