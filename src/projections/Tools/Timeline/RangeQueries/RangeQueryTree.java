package projections.Tools.Timeline.RangeQueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/** Provides a collection interface that provides efficient range queries via iterators. 
 *  Does not allow insertion of null objects. 
 * 
 * The iterators returned by this class are not thread-safe. 
 * If modifications are made to the collection, the subsequent 
 * values returned by all iterators are undefined.
 * 
 * The query range for each iterator is bound to the iterator at its creation. 
 * Thus it is safe to create different concurrent iterators 
 * with different ranges, but only in the following usages: 
 *       1) use iterator(long lowerBound, long upperBound)
 *       2) synchronize around setQueryRange and iterator()
 *       3) synchronize around setQueryRange and for (Object o : collection)
 * 
 * The modifications are performed efficiently, always resulting in a balanced tree.
 * 
 * Each iterator is constructed as a tree of merged iterators that are 
 * that spans the set of leaf nodes that overlap the query range. 
 * 
 * The remove() method on the iterators is currently unsupported.
 * 
 * @author Isaac Dooley
 * 
 * */
public class RangeQueryTree <T extends Range1D> implements Query1D<T>{

	static int MAX_ENTRIES_PER_NODE = 100;

	TreeNode root;
	
	public RangeQueryTree(){
		root = new TreeNode();
	}
	
	/** Create a new RangeQueryTree that is initially populated with the objects from the specified collection. */
	public RangeQueryTree(Collection<? extends T> c){
		root = new TreeNode();
		addAll(c);
		// Rebalance the tree just to make sure future accesses to this tree are fast, as the user will likely not modify the tree again
		root = root.rebalanceTree();
	}
	
	
	@Override
	public Iterator<T> iterator() {
			return root.iterator(Long.MIN_VALUE, Long.MAX_VALUE);
	}
	
	/** For thread-safety, multiple iterators can be constructed using explicit bounds with this method */
	public Iterator<T> iterator(long lowerBound, long upperBound) {
			return root.iterator(lowerBound, upperBound);
	}


	
	@Override
	public boolean add(T e) {
		root.add(e);

		if( root.numEntriesInSubTree % MAX_ENTRIES_PER_NODE == 0) {
			root = root.rebalanceTree();
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
		if(o instanceof Range1D) {
			return root.remove((Range1D)o);	
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = false;
		for(Object o : c){
			boolean removed = true;
			// Try to remove all instances of this object from the collection
			while(removed){
//				System.out.println("removeAll: attempting to remove " + o);
				removed = remove(o);
				if(removed){
					result = true;
//					System.out.println("removeAll: removed " + o);
//					this.printTree();
				}
			}
		}
		return result;
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
	public <E> E[] toArray(E[] a) {
		throw new UnsupportedOperationException();
	}
	
	
	static int lastNodeID = 0;

	
	protected class TreeNode implements Iterable<T>{

		protected final int nodeID;
		
		/** Data local to this node */
		private ArrayList<T>	data;

		/** lower bound of local data. Updated upon insertion. */
		private long lowerBound;
		private long upperBound;
		
		private int numEntriesInSubTree = 0;
		
		protected TreeNode leftChild;
		protected TreeNode rightChild;
		protected TreeNode parent;
		
		public TreeNode(){
			data = new ArrayList<T>();
			leftChild = null;
			rightChild = null;
			parent = null;
			numEntriesInSubTree = 0;
			nodeID = lastNodeID++;
		}
	
		
		/** Rebalance whole tree and return new root */
		public TreeNode rebalanceTree(){
			TreeNode newRoot = rebalanceTreeRecursive().newRoot;
			newRoot.parent = null;
			return newRoot;
		}

		public void shiftRangeBy(long shift){
			lowerBound += shift;
			upperBound += shift;
			
			if(leftChild != null)
				leftChild.shiftRangeBy(shift);
			if(rightChild != null)
				rightChild.shiftRangeBy(shift);
			
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
//			System.out.println("Creating iterator for node " + nodeID + " data=" + data);
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
			String parentID;
			if(parent != null)
				parentID = "" + parent.nodeID;
			else
				parentID = "<no parent>";
						
			for(int i=0; i<level; i++){
				System.out.print("    ");
			}
			if(data != null){
				System.out.println("Leaf node " + nodeID + " with parent " + parentID + " contains " + data.size() + " local data entries, range=(" + lowerBound + "," + upperBound + ") : " + data.toString());
			} else {
				System.out.println("non-leaf node " + nodeID + " with parent " + parentID + " contains " + numEntriesInSubTree + " entries in subtree, range=(" + lowerBound + "," + upperBound + ")");
				leftChild.printInfoRecursive(level+1);
				rightChild.printInfoRecursive(level+1);
			}
		}
		

		public boolean verifyTreeCorrectness() {
			// First check root
			
			if(this == root){
				if (parent != null)
					return false;

				if(leftChild == null && rightChild == null && data != null)
					return true;
				
				if(leftChild == null && rightChild == null && data == null)
					return false;			
			}
			

			if(this != root && parent == null)
				return false;
		
						
			if(lowerBound == Long.MAX_VALUE || upperBound == Long.MIN_VALUE)
				return false;
			
			if(lowerBound > upperBound)
				return false;


			if(data == null) {
				// subtrees
				if(leftChild == null || rightChild == null)
					return false;				

				if(leftChild.parent != this){
					System.out.println("leftChild " + leftChild.nodeID + " has wrong parent " + leftChild.parent.nodeID + " should be " + nodeID);
					return false;
				}
				
				if(rightChild.parent != this){
					System.out.println("rightChild " + rightChild.nodeID + " has wrong parent " + rightChild.parent.nodeID + " should be " + nodeID);
					return false;
				}
				
				// make sure subtrees have ranges within this range
				if(leftChild.lowerBound < lowerBound || leftChild.upperBound > upperBound){
					System.out.println("leftChild " + leftChild.nodeID + " has range that exceeds that of parent " + nodeID);
					return false;
				}
				
				if(rightChild.lowerBound < lowerBound || rightChild.upperBound > upperBound){
					System.out.println("rightChild " + rightChild.nodeID + " has range that exceeds that of parent " + nodeID);
					return false;
				}
					
					
				
				return leftChild.verifyTreeCorrectness() && rightChild.verifyTreeCorrectness();
			} else {
				
				// no subtrees, then there ought to be some data here
				if(data.size()==0){
					System.out.println("FAILURE: node " + nodeID + " is empty");
					return false;
				}

				if(data.size() !=  numEntriesInSubTree){
					System.out.println("FAILURE: node " + nodeID + " lists numEntriesInSubTree=" + numEntriesInSubTree + " which should be " + data.size());
					return false;
				}
				
				// Verify we don't contain any nulls, and compute the bounds for this node
				long lb=Long.MAX_VALUE, ub=Long.MIN_VALUE;
				for(T o : data){
					if(o == null){
						System.out.println("FAILURE: node " + nodeID + " contains null entry");
						return false;
					}
					if(o.lowerBound() < lb)
						lb = o.lowerBound();
					if(o.upperBound() > ub)
						ub = o.upperBound();

				}
				if(lowerBound != lb || upperBound != ub){
					System.out.println("FAILURE: Bounds for node " + nodeID + " are " + lowerBound + "," + upperBound + " but should be " + lb + "," + ub);
					System.out.println("FAILURE: data is " + data);
					return false;
				}
				
				
			}

			
			return true;
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
				pivot.parent = parent;
				rightChild = pivot.leftChild;
				pivot.leftChild = this;
				retval.newRoot = pivot;
				retval.depth = r.depth - 1 + 1;
				
				numEntriesInSubTree = leftChild.numEntriesInSubTree + rightChild.numEntriesInSubTree;
				lowerBound = Math.min(leftChild.lowerBound, rightChild.lowerBound);
				upperBound = Math.max(leftChild.upperBound, rightChild.upperBound);
				leftChild.parent = this;
				rightChild.parent = this;
				
				pivot.numEntriesInSubTree = pivot.leftChild.numEntriesInSubTree + pivot.rightChild.numEntriesInSubTree;
				pivot.lowerBound = Math.min(pivot.leftChild.lowerBound, pivot.rightChild.lowerBound);
				pivot.upperBound = Math.max(pivot.leftChild.upperBound, pivot.rightChild.upperBound);
				pivot.leftChild.parent = pivot;
				pivot.rightChild.parent = pivot;
				
				
			} else if (l.depth - r.depth >= 2){
				// rotate right
//				System.out.println("Rotate Right");
				TreeNode pivot = l.newRoot;			
				pivot.parent = parent;
				leftChild = pivot.rightChild;
				pivot.rightChild = this;
				retval.newRoot = pivot;	
				retval.depth = l.depth - 1 + 1;
				
				numEntriesInSubTree = leftChild.numEntriesInSubTree + rightChild.numEntriesInSubTree;
				lowerBound = Math.min(leftChild.lowerBound, rightChild.lowerBound);
				upperBound = Math.max(leftChild.upperBound, rightChild.upperBound);
				leftChild.parent = this;
				rightChild.parent = this;

				pivot.numEntriesInSubTree = pivot.leftChild.numEntriesInSubTree + pivot.rightChild.numEntriesInSubTree;
				pivot.lowerBound = Math.min(pivot.leftChild.lowerBound, pivot.rightChild.lowerBound);
				pivot.upperBound = Math.max(pivot.leftChild.upperBound, pivot.rightChild.upperBound);
				pivot.leftChild.parent = pivot;
				pivot.rightChild.parent = pivot;


			}
			
			return retval;
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
					leftChild = new TreeNode();
					rightChild = new TreeNode();
					leftChild.parent = this;
					rightChild.parent = this;
					
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
		
		
		public boolean remove(Range1D o) {
			// Don't attempt to remove value because this subtree is empty (should only happen at root)
			if(numEntriesInSubTree == 0)
				return false;
			
			// Don't attempt to remove value that is out of the range for this subtree
			
			if(o.lowerBound() > upperBound || o.upperBound()< lowerBound){
				return false;
			}
			
			
			if(data != null){
				
//				System.out.println("Removing from leaf");

				//----------------------------------------------------------
				// Remove element from this leaf node
				boolean removedFromData = data.remove(o);
				
				if(removedFromData){
					numEntriesInSubTree = data.size();
					
					if(data.size() > 0){
						
						if(o.lowerBound() == lowerBound){
							// update this bin's lower bound
							lowerBound = data.get(0).lowerBound();
							for(T e : data) {
								if(e.lowerBound() < lowerBound)
									lowerBound = e.lowerBound();
							}
						}

						
						if(o.upperBound() == upperBound){
							// update this bin's lower bound
							upperBound = data.get(0).upperBound();
							for(T e : data) {
								if(e.upperBound() > upperBound)
									upperBound = e.upperBound();
							}
						}				
						
					} else {
						// no items left in bin
						lowerBound = Long.MAX_VALUE; // necessary so that upon traversing back up the tree, the updating of lower bounds is easier
						upperBound = Long.MIN_VALUE;
					}
					return true;
				}

				return false;
			} else {
				//----------------------------------------------------------
				// Remove element from this non-leaf node
				
//				System.out.println("Removing from non-leaf");
				
				boolean removedFromLeft = leftChild.remove(o);
				if(removedFromLeft) {
//					System.out.println("removed from left");
					// update my bounds
					lowerBound = Math.min(leftChild.lowerBound, rightChild.lowerBound);
					upperBound = Math.max(leftChild.upperBound, rightChild.upperBound);
					numEntriesInSubTree = leftChild.numEntriesInSubTree + rightChild.numEntriesInSubTree;
					
					// Check to see if left subtree is now empty
					if(leftChild.isEmpty()){
//						System.out.println("left child is now empty");

						// remove this node and put children into parent
						
						if(this != root){
//							System.out.println("Attempting to relink tree for non-root " + nodeID + " with parent " + parent.nodeID);

							if(parent.leftChild == this){
//								System.out.println("  " + parent.nodeID + ".leftChild = " + rightChild.nodeID);
								parent.leftChild = rightChild;
//								System.out.println("  " + rightChild.nodeID + ".parent = " + parent.nodeID);
								rightChild.parent = parent;
							} else {
//								System.out.println("  " + parent.nodeID + ".rightChild = " + rightChild.nodeID);
								parent.rightChild = rightChild;
//								System.out.println("  " + rightChild.nodeID + ".parent = " + parent.nodeID);
								rightChild.parent = parent;
							}
						} else {
							// new root is simply the right child
							root = rightChild;
							rightChild.parent = null;
						}
					
					}
					
					return true;
				}

				boolean removedFromRight = rightChild.remove(o);
				if(removedFromRight) {
//					System.out.println("removed from right");

					// update my bounds
					lowerBound = Math.min(leftChild.lowerBound, rightChild.lowerBound);
					upperBound = Math.max(leftChild.upperBound, rightChild.upperBound);
					numEntriesInSubTree = leftChild.numEntriesInSubTree + rightChild.numEntriesInSubTree;

					// Check to see if right subtree is now empty
					if(rightChild.isEmpty()){
//						System.out.println("right child is now empty");

						// remove this node and put children into parent
						
						if(this != root){
							if(parent.leftChild == this){
								parent.leftChild = leftChild;
								leftChild.parent = parent;
							} else {
								parent.rightChild = leftChild;
								leftChild.parent = parent;
							}
						} else {
							// new root is simply the left child
							root = leftChild;
							leftChild.parent = null;
						}
						
					}
					
					return true;
				}
				
				return false;
			}
		}

	}
	
	
	
	


	public void printTree() {
		root.printInfoRecursive(0);
	}

	
	@Override
	/** A probably non-efficient method for eliminating unused entries from the tree */
	public void removeEntriesOutsideRange(long startTime, long endTime) {
		
		Object[] a = toArray();

		clear();
		
		for(Object o : a){
			T t = (T)o;
			if(t.lowerBound() <= endTime && t.upperBound() >= startTime)
				add(t);
		}
		
	}

	@Override
	public void shiftAllEntriesBy(long shift) {
		// Shift all the objects in this collection
		for(T o : this){
			o.shiftTimesBy(shift);
		}
		
		// Then shift the bounds for the whole tree
		root.shiftRangeBy(shift);
				
	}
	
	
}
