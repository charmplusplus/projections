package projections.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import projections.Tools.Timeline.RangeQueries.UnitTest;

/** 

  Basically a class that wraps a TreeSet<Integer> providing an old interface. 

  In the past this class implemented its own very slow sorted linked-list


  use this by:

 OrderedIntList list = new OrderedIntList();
 list.insert(8);
 list.insert(2);
 list.insert(5);
 
 int e;
 list.reset();
 while (e=nextElement()) != -1) { 
   System.out.println(e);
 }
 
 will print out: 2, 5, 8 on separate lines

 */



public class OrderedIntList implements Iterable<Integer>
{

	TreeSet<Integer> data = new TreeSet();
	Iterator<Integer> iter;


	@Override
	public Iterator<Integer> iterator() {
		return data.iterator();
	}
			
	
	
	public boolean isEmpty() {
		return data.isEmpty();
	}

	public OrderedIntList copyOf() {
		OrderedIntList listcopy = new OrderedIntList();
		listcopy.data.addAll(data);
		listcopy.reset();
		return listcopy;
	}   


	public OrderedIntList(){
		reset();
	}


	public void insert(int eleValue) {
		data.add(eleValue);
	}   

    public boolean contains(int eleValue)
    {
        return data.contains(eleValue);

    }

	public String listToString() {
		return listToString(data);
	}   
	
	public static String listToString(TreeSet<Integer> c) {
		int lower=-1;
		int prev=-1;
		boolean firsttime = true;
		
		String result = "";
		
		for(Integer i : c) {
			if(firsttime){
				lower = i;
				firsttime = false;
			} else {
				
				if(i == prev+1){
					// extend previous range
				} else {
					
					// output old range
					if(lower == prev)
						result += "," + lower;
					else
						result += "," + lower + "-" + prev;
						
					// start new range
					lower = i;
	
				}
				
			}
						
			prev = i;
		}
		
		// finish up
		if(lower == prev)
			result += "," + lower;
		else
			result += "," + lower + "-" + prev;
		
		// prune ',' at beginning if there is one
		if(result.charAt(0) == ',')
			result = result.substring(1);
				
		return result;
	}   


	protected void removeAll() { 
		data.clear();
	}   

	/** 
	 * @deprecated Replace this old iteration mechanism with the new standardized iterator provided by iterator(), or a foreach loop on this iterable collection 
	 */
	 public int nextElement() {
		return iter.next();
	}   

	/** 
	 * @deprecated Replace this old iteration mechanism with the new standardized iterator provided by iterator(), or a foreach loop on this iterable collection
	 */
	public void reset() {
		iter = data.iterator();
	}   
	
	
	/** 
	 * @deprecated Replace this old iteration mechanism with the new standardized iterator provided by iterator(), or a foreach loop on this iterable collection
	 */
	public boolean hasMoreElements() {
		return iter.hasNext();
	}   


	public int size() {
		return data.size();
	}




	public static void main(String[] args){
		
		OrderedIntList l = new OrderedIntList();
		l.insert(8);
		l.insert(2);
		l.insert(5);
		System.out.println(l.listToString());


		l.insert(9);
		l.insert(7);
		System.out.println(l.listToString());

		l.insert(1);
		System.out.println(l.listToString());

		
	}
	

}
