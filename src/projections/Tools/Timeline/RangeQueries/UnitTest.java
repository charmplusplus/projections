package projections.Tools.Timeline.RangeQueries;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class UnitTest {

	public class TestRange1DObject implements Range1D {
		long lb, ub;
		
		public long lowerBound() {
			return lb;
		}

		public long upperBound() {
			return ub;
		}
		
		public TestRange1DObject(long l, long u){
			lb = l;
			ub = u;
		}
		
		public String toString(){
			return "(" + lb + "," + ub + ") ";
		}
		
		
	}
	
	Random random;
	
	
	public static void main(String[] args){
		
		UnitTest t = new UnitTest();
		boolean success = true;
		int count = 0;
		
		while(success && count++ < 50){
			success = t.doTest(count);
		}

		if(success){
			System.out.println("Unit Test Passed for projections.Tools.Timeline.RangeQueryDatabase");
		} else {
			System.out.println("Unit Test Failed for projections.Tools.Timeline.RangeQueryDatabase");
		}
		
	}
	
	public boolean doTest(int seed){
		System.out.println("==================================== seed=" + seed);
		random = new Random(seed);

		RangeQueryTree.MAX_ENTRIES_PER_NODE = (int) (2+random.nextDouble()*500);

		
		RangeQueryArrayList db = new RangeQueryArrayList();
				
		RangeQueryTree db2 = new RangeQueryTree();

		return testQuery1D(db) && testQuery1D(db2);
	}
	

	public boolean testQuery1D(Query1D db){
		System.out.println("Starting test for " + db);

		
		//////////////////////////////////////////////
		db.add(new TestRange1DObject(10,20));
		db.add(new TestRange1DObject(30,40));
		db.add(new TestRange1DObject(50,60));
		db.add(new TestRange1DObject(70,80));
		db.add(new TestRange1DObject(90,100));
		db.add(new TestRange1DObject(110,120));
		if(db.size() != 6)
			return false;
		
		//db.setQueryRange(35, 95);
		int count = 0;
		for(Object o : db){
			count++;
		}
//		System.out.println("count = " + count);
		if(count != 6)
			return false;
		
		System.out.println("Test 10 passed for " + db);
		//////////////////////////////////////////////
		db.clear();
		db.add(new TestRange1DObject(10,20));
		db.add(new TestRange1DObject(30,40));
		db.add(new TestRange1DObject(50,60));
		db.add(new TestRange1DObject(70,80));
		db.add(new TestRange1DObject(90,100));
		db.add(new TestRange1DObject(110,120));
		
		if(db instanceof RangeQueryTree){
			if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
				// good
			} else {
				System.out.println("Verifying tree correctness FAILED. The bad tree is thus:");			
				((RangeQueryTree) db).printTree();
				return false;
			}
		}
		
		
		if(db.size() != 6)
			return false;
		
	
		Iterator<Range1D> iter = db.iterator(35, 95);
		count = 0;
		while(iter.hasNext()){
			iter.next();
			count++;
		}		
		
//		System.out.println("count = " + count);
		if(count != 4)
			return false;
		

		System.out.println("Test 20 passed for " + db);
		//////////////////////////////////////////////
		db.clear();
		db.add(new TestRange1DObject(110,120));
		db.add(new TestRange1DObject(90,100));
		db.add(new TestRange1DObject(70,80));
		db.add(new TestRange1DObject(50,60));
		db.add(new TestRange1DObject(30,40));
		db.add(new TestRange1DObject(10,20));

		count = 0;
		iter = db.iterator();
		while(iter.hasNext()){
			iter.next();
			count++;
		}	
//		System.out.println("count = " + count);
		if(count != 6)
			return false;

		System.out.println("Test 30 passed for " + db);
		//////////////////////////////////////////////

		count = 0;
		iter = db.iterator(35, 95);
		while(iter.hasNext()){
			iter.next();
			count++;
		}				
		
//		System.out.println("count = " + count);
		if(count != 4)
			return false;
		
		
		System.out.println("Test 40 passed for " + db);
		//////////////////////////////////////////////
		db.clear();
		db.add(new TestRange1DObject(70,80));
		db.add(new TestRange1DObject(110,120));
		db.add(new TestRange1DObject(90,100));
		db.add(new TestRange1DObject(10,20));
		db.add(new TestRange1DObject(50,60));
		db.add(new TestRange1DObject(30,40));
	
		iter = db.iterator(10, 20);
		count = 0;
		while(iter.hasNext()){
			iter.next();
			count++;
		}		
//		System.out.println("count = " + count);
		if(count != 1)
			return false;
		
		System.out.println("Test 50 passed for " + db);
		//////////////////////////////////////////////	
		iter = db.iterator(15, 16);
		count = 0;
		while(iter.hasNext()){
			iter.next();
			count++;
		}		
		
//		System.out.println("count = " + count);
		if(count != 1)
			return false;
		
		
		System.out.println("Test 60 passed for " + db);
		//////////////////////////////////////////////	
		db.clear();
		int n = 10000;
	
		for(int i=0; i<n; i++){
			long lb = (long) (random.nextDouble()*94.0);
			db.add(new TestRange1DObject(lb, lb+5));
			
			if(db instanceof RangeQueryTree){
				if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
					// good
				} else {
					System.out.println("Verifying tree correctness FAILED. The bad tree is thus:");			
					((RangeQueryTree) db).printTree();
					return false;
				}
			}
		}
		
		iter = db.iterator(1000,2000);
		count = 0;
		while(iter.hasNext()){
			iter.next();
			count++;
		}		
		
//		System.out.println("count = " + count);
		if(count != 0)
			return false;

		iter = db.iterator(-1000,-1);
		count = 0;
		while(iter.hasNext()){
			iter.next();
			count++;
		}
		if(count != 0)
			return false;


		iter = db.iterator(0,100);
		count = 0;
		while(iter.hasNext()){
			iter.next();
			count++;
		}		
//		System.out.println("count = " + count);
		if(count != n)
			return false;

//		
//		if(db instanceof RangeQueryTree){
//			((RangeQueryTree) db).printTree();	
//		}
		
		
		
		
		//////////////////////////////////////////////	

		db.clear();
		
		for(int i=0; i<100; i++){
			TestRange1DObject o = new TestRange1DObject(i,i);
			db.add(o);

			if(db.contains(o)==false){
				System.err.println("Failure: data structure does not contain newly added entry: " + o);
				return false;
			}
			
		}
		
		
		for(int j=0; j<1000; j++){
			long lb = (long) (random.nextDouble()*40);
			long ub = (long) (lb + random.nextDouble()*40);
			
			long expectedCount = ub - lb +1;
			

			
			iter = db.iterator(lb, ub);
			count = 0;
			while(iter.hasNext()){
				iter.next();
				count++;
			}			
			if(count != expectedCount)
				return false;

		}
		
		for(int j=0; j<1000; j++){
			long lb = (long) (random.nextDouble()*40);
			long ub = lb;
			
			long expectedCount = 1;
			
			iter = db.iterator(lb, ub);
			count = 0;
			while(iter.hasNext()){
				iter.next();
				count++;
			}
			if(count != expectedCount)
				return false;
		}
		
		
		for(int j=0; j<1000; j++){
			long lb = (long) (random.nextDouble()*40);
			long ub = lb+1;
			long expectedCount = 2;
			
			
			iter = db.iterator(lb, ub);
			count = 0;
			while(iter.hasNext()){
				iter.next();
				count++;
			}			
			if(count != expectedCount)
				return false;
		}
		
		
		for(int i=0; i<100; i++){
			db.add(new TestRange1DObject(i,i));
		}
		
		

		for(int j=0; j<1000; j++){
			long lb = (long) (random.nextDouble()*40);
			long ub = (long) (lb + random.nextDouble()*40);
			
			long expectedCount = 2*(ub - lb +1);
			
			iter = db.iterator(lb, ub);
			count = 0;
			while(iter.hasNext()){
				iter.next();
				count++;
			}			
			if(count != expectedCount)
				return false;

		}
		

		for(int j=0; j<1000; j++){
			long lb = (long) (random.nextDouble()*40);
			long ub = lb;
			
			long expectedCount = 2;
			
			iter = db.iterator(lb, ub);
			count = 0;
			while(iter.hasNext()){
				iter.next();
				count++;
			}
			if(count != expectedCount)
				return false;
		}
		
		for(int j=0; j<1000; j++){
			long lb = (long) (random.nextDouble()*40);
			long ub = lb+1;
			long expectedCount = 4;
			iter = db.iterator(lb, ub);
			count = 0;
			while(iter.hasNext()){
				iter.next();
				count++;
			}
			if(count != expectedCount)
				return false;
		}
		
		if(db instanceof RangeQueryTree){
			if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
				// good
			} else {
				System.out.println("Verifying tree correctness FAILED. The bad tree is thus:");			
				((RangeQueryTree) db).printTree();
				return false;
			}
		}
		
		
		

		//////////////////////////////////////////////	
		// Make sure values are non-null in store
		
	
		for(Object o : db){
			if(o == null){
				System.err.println("Failure: db contains a null value");
				return false;
			}

		}
		

		
		//////////////////////////////////////////////	
		db.clear();	

		for(int i=0; i<100; i++){
			db.add(new TestRange1DObject(i,i));
		}

		
		if(db instanceof RangeQueryTree){
			if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
				// good
			} else {
				System.out.println("Verifying tree correctness FAILED. The bad tree is thus:");			
				((RangeQueryTree) db).printTree();
				return false;
			}
		}
		
		
//		if(db instanceof RangeQueryTree){
//			((RangeQueryTree) db).printTree();
//		}

		

		int size = db.size();
		
		for(int i=size; i>0; i--){
//			System.out.println("Testing removing: i=" + i);
			// remove one item, using a fresh iterator
			iter = db.iterator();
			Object o = iter.next();
			
//			System.out.println("Testing if we can remove : " + o );
//
//			System.out.println("Tree before removing: ");			
//			if(db instanceof RangeQueryTree){
//				((RangeQueryTree) db).printTree();
//			}

//			System.out.println("Verifying tree correctness before removing: ");			
			if(db instanceof RangeQueryTree){
				if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
					// good
				} else {
					System.out.println("Verifying tree correctness FAILED");			
					return false;
				}
			}

			
			if(o == null){
				System.err.println("Failure iterator returned a NULL object: db.size()=" + db.size() + " o=" + o + " i=" + i);
				return false;
			}
			
			
			if(db.remove(o)==false || db.size()!=i-1){
				System.err.println("Failure: db.size()=" + db.size() + " (should be " + (i-1) + ") o=" + o + " i=" + i);
				
				if(db.contains(o)==true){
					System.err.println("Failure: data structure still contains removed entry: " + o);
					if(db instanceof RangeQueryTree){
						((RangeQueryTree) db).printTree();
					}
					return false;
				}
				
				
				
				return false;
			}
			
			
//			System.out.println("Verifying tree correctness after removing: ");			
			if(db instanceof RangeQueryTree){
				if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
					// good
//					System.out.println("Verifying tree correctness SUCCESS");			
				} else {
					System.out.println("Verifying tree correctness FAILED");			
					return false;
				}
			}
			
						
		}

		
		
		System.out.println("Test 60 passed for " + db);
		//////////////////////////////////////////////	
		db.clear();
		
		n = 1000;
	
		for(int i=0; i<n; i++){
			long lb = (long) ( Math.pow(random.nextDouble()*5.0-3.0, 3)*100.0);
			db.add(new TestRange1DObject(lb, lb+5));
		
			// count values produced by iterator
			count = 0;
			for(Object e : db){
				count++;
			}

			
			if( (count != (i+1)) || count != db.size() || db.size()!=(i+1)) {
				System.out.println("FAILURE: There should be " + (i+1) + " entries in the structure, but iterator produced " + count + " and data structure thinks it is " + db.size());
				return false;
			}

			
			
			if(db instanceof RangeQueryTree){
				if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
					// good
				} else {
					System.out.println("Verifying tree correctness FAILED. The bad tree is thus:");			
					((RangeQueryTree) db).printTree();
					return false;
				}
			}
		}
		
		
		System.out.println("Test 70 passed for " + db);

		
		//////////////////////////////////////////////	
		db.clear();
		
		n = (int) (random.nextDouble() * 5000.0); // entries to start with
		int m = (int) (random.nextDouble() * n);   // max entries to remove
		int l = (int) (random.nextDouble() * n);   // starting location to start removing
		
		LinkedList<TestRange1DObject> toRemove = new LinkedList();
		
		for(int i=0; i<n; i++){
			long lb = (long) ( Math.pow(random.nextDouble()*5.0-3.0, 3)*100.0 + 3000.0);
			db.add(new TestRange1DObject(lb, lb+5));
		}
//		
//		if(db instanceof RangeQueryTree){
//			((RangeQueryTree) db).printTree();
//		}

		
		// remove nothing 
		if(db.removeAll(new LinkedList()) != false){
			System.out.println("FAILURE: removeAll() should have returned false for " + db);
		}
				
		
		// remove m entries from the middle of the data structure
		count = 0;
		for(Object e : db){
			TestRange1DObject o = (TestRange1DObject) e;
			count++;
			if(toRemove.size() < m && (count > l)){
				toRemove.add(o);
			}
		}

		boolean result = db.removeAll(toRemove);
		if(result == false && toRemove.size()>0){
			System.out.println("FAILURE: removeAll() should have returned true for " + db);
			if(db instanceof RangeQueryTree){
				((RangeQueryTree) db).printTree();
			}
			return false;
		}
		
	
		for(TestRange1DObject o : toRemove){
			if(db.contains(o)==true){
				System.err.println("Failure: data structure still contains removed (via removeAll) entry: " + o);
				return false;
			}		
		}
		
		
		
		count = 0;
		for(Object e : db){
			count++;
		}
		

	
		if(db instanceof RangeQueryTree){
			if(((RangeQueryTree) db).root.verifyTreeCorrectness()){
				// good
			} else {
				System.out.println("Verifying tree correctness FAILED. The bad tree is thus:");			
				((RangeQueryTree) db).printTree();
				return false;
			}
		}


		if( (count != (n-toRemove.size())) || count != db.size() || db.size()!=(n-toRemove.size())) {
			System.out.println("FAILURE: There should be " + (n-toRemove.size()) + " entries in the structure, but iterator produced " + count + " and data structure thinks it is " + db.size());
			if(db instanceof RangeQueryTree){
				((RangeQueryTree) db).printTree();
			}
			return false;
		}

		
		
		System.out.println("Test 80 passed for " + db);

		
		
		
		System.out.println("\n\n");
		return true;
	}
	
	
}
