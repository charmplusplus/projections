package projections.Tools.Timeline.RangeQueries;

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
	
	public static void main(String[] args){
		
		UnitTest t = new UnitTest();
		boolean success = t.doTest();
		if(success){
			System.out.println("Unit Test Passed for projections.Tools.Timeline.RangeQueryDatabase");
		} else {
			System.out.println("Unit Test Failed for projections.Tools.Timeline.RangeQueryDatabase");
		}
	}
	
	public boolean doTest(){
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
		System.out.println("count = " + count);
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
		if(db.size() != 6)
			return false;
		
		db.setQueryRange(35, 95);
		count = 0;
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
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

		db.clearQueryRange();
		count = 0;
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
		if(count != 6)
			return false;

		System.out.println("Test 30 passed for " + db);
		//////////////////////////////////////////////

		db.setQueryRange(35, 95);
		count = 0;
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
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
		db.setQueryRange(10, 20);
		count = 0;
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
		if(count != 1)
			return false;
		
		System.out.println("Test 50 passed for " + db);
		//////////////////////////////////////////////	
		count = 0;
		db.setQueryRange(15, 16);
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
		if(count != 1)
			return false;
		
		
		System.out.println("Test 60 passed for " + db);
		//////////////////////////////////////////////	
		db.clear();
		int n = 10000;
	
		for(int i=0; i<n; i++){
			long lb = (long) (Math.random()*94.0);
			db.add(new TestRange1DObject(lb, lb+5));
		}
		
		db.setQueryRange(1000,2000);
		count = 0;
		for(Object o : db)
			count++;
		System.out.println("count = " + count);
		if(count != 0)
			return false;

		db.setQueryRange(-1000,-1);
		count = 0;
		for(Object o : db)
			count++;
		System.out.println("count = " + count);
		if(count != 0)
			return false;


		db.setQueryRange(0,100);
		count = 0;
		for(Object o : db)
			count++;
		System.out.println("count = " + count);
		if(count != n)
			return false;

//		
//		if(db instanceof RangeQueryTree){
//			((RangeQueryTree) db).printTree();	
//		}
		
		
		
		
		//////////////////////////////////////////////	

		db.clear();
		
		for(int i=0; i<100; i++){
			db.add(new TestRange1DObject(i,i));
		}
		
		
		for(int j=0; j<1000; j++){
			long lb = (long) (Math.random()*40);
			long ub = (long) (lb + Math.random()*40);
			
			long expectedCount = ub - lb +1;
			
			db.setQueryRange(lb, ub);
			count = 0;
			for(Object o : db)
				count++;
//			System.out.println("count = " + count);
			if(count != expectedCount)
				return false;

		}
		
		for(int j=0; j<1000; j++){
			long lb = (long) (Math.random()*40);
			long ub = lb;
			
			long expectedCount = 1;
			
			db.setQueryRange(lb, ub);
			count = 0;
			for(Object o : db)
				count++;
			if(count != expectedCount)
				return false;
		}
		
		
		for(int j=0; j<1000; j++){
			long lb = (long) (Math.random()*40);
			long ub = lb+1;
			long expectedCount = 2;
			db.setQueryRange(lb, ub);
			count = 0;
			for(Object o : db)
				count++;
			if(count != expectedCount)
				return false;
		}
		
		
		for(int i=0; i<100; i++){
			db.add(new TestRange1DObject(i,i));
		}
		
		

		for(int j=0; j<1000; j++){
			long lb = (long) (Math.random()*40);
			long ub = (long) (lb + Math.random()*40);
			
			long expectedCount = 2*(ub - lb +1);
			
			db.setQueryRange(lb, ub);
			count = 0;
			for(Object o : db)
				count++;
//			System.out.println("count = " + count);
			if(count != expectedCount)
				return false;

		}
		

		for(int j=0; j<1000; j++){
			long lb = (long) (Math.random()*40);
			long ub = lb;
			
			long expectedCount = 2;
			
			db.setQueryRange(lb, ub);
			count = 0;
			for(Object o : db)
				count++;
			if(count != expectedCount)
				return false;
		}
		
		for(int j=0; j<1000; j++){
			long lb = (long) (Math.random()*40);
			long ub = lb+1;
			long expectedCount = 4;
			db.setQueryRange(lb, ub);
			count = 0;
			for(Object o : db)
				count++;
			if(count != expectedCount)
				return false;
		}

		
		
		System.out.println("\n\n");
		return true;
	}
	
	
}
