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

		//////////////////////////////////////////////
		db.add(new TestRange1DObject(10,20));
		db.add(new TestRange1DObject(30,40));
		db.add(new TestRange1DObject(50,60));
		db.add(new TestRange1DObject(70,80));
		db.add(new TestRange1DObject(90,100));
		db.add(new TestRange1DObject(110,120));

		db.setQueryRange(35, 95);
		int count = 0;
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
		if(count != 4)
			return false;
		

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

		//////////////////////////////////////////////

		db.setQueryRange(35, 95);
		count = 0;
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
		if(count != 4)
			return false;
		
		
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
		
		
		//////////////////////////////////////////////	
		count = 0;
		db.setQueryRange(15, 16);
		for(Object o : db){
			count++;
		}
		System.out.println("count = " + count);
		if(count != 1)
			return false;
		
		
		return true;
	}
	
	
}
