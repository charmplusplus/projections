package projections.gui;

// use this by:
//
// OrderedIntList list = new OrderedIntList();
// list.insert(8);
// list.insert(2);
// list.insert(5);
// 
// int e;
// list.reset();
// while (e=nextElement()) != -1) { 
//   System.out.println(e);
// }
// 
// will print out: 2, 5, 8 on separate lines

public class OrderedIntList
{
    private Link head;
    private Link tail;
    private Link pre;
    private int len;
    
    class Link
    {
	int data;
	Link next;
	Link(int d, Link n) {
	    data = d; next = n;
	}
    }  
    
    public OrderedIntList copyOf()
    {
	OrderedIntList listcopy = new OrderedIntList();
	reset();
	int e;
	while((e = nextElement()) != -1)
	    listcopy.insert(e);
	
	listcopy.reset();
	return listcopy;
    }   

    public int currentElement()
    {
	Link cur = nextLink();
	if(cur == null)
	    return -1;
	else
	    return cur.data;   
    }   

    public boolean equals(OrderedIntList otherlist)
    {
	if(otherlist == null)
	    return false;
	
	if(size() != otherlist.size())
	    return false;
	
	reset();
	otherlist.reset();
	
	int e;
	while((e = nextElement()) != -1) {
	    if(otherlist.nextElement() != e)
		return false;
	}
	  
	return true;
    }   

    public boolean hasMoreElements()
    {
	return nextLink() != null;
    }   

    /**
       return true if this list contain otherlist
    */
    public boolean contains(OrderedIntList otherlist)
    {
	if (otherlist == null) return true;
	if (otherlist.size() > size()) return false;
	reset();
	otherlist.reset();
	int e, me;
	while((e = otherlist.nextElement()) != -1) {
	    while (e != (me = nextElement())) {
		if (me == -1) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
       return true if this list has ele
    */
    public boolean contains(int eleValue)
    {
	int me;
	reset();
	while((me = nextElement()) != -1) {
	    if (me == eleValue) {
		return true;
	    }
	}
	return false;
    }

    public void insert(int eleValue)
    {
	Link newLink;
	reset();
	
	Link tmp = nextLink();
	while(tmp != null && tmp.data < eleValue) {
	    pre = tmp;
	    tmp = nextLink();
	}

	if (tmp == null) {
	    newLink = new Link(eleValue, tmp);
	    if (head == null) {
		head = newLink;
		tail = newLink;
	    } else {
		pre.next = newLink;
		tail = newLink;
	    }
	    len++;
	} else if (tmp.data != eleValue) {      
	    newLink = new Link(eleValue, tmp);
	    if (head == tmp) {
		head = newLink;
	    } else {
		pre.next = newLink;
	    }
	    len++;      
	}       
    }   
    
    public String listToString()
    {
	reset();
	int min, max, tmp;
	tmp = nextElement();
	  
	String result = "";
	int interval = -1;
	min = max = -1;
	while(tmp != -1) {
	    if (min == -1) {
		min = tmp;
		tmp = nextElement();
	    } else if (interval == -1) {
		interval = tmp-min;
		max = tmp;
		tmp = nextElement();
	    } else {
		while((tmp - max) == interval) {
		    max= tmp;
		    tmp = nextElement();
		}
		if (max == min+interval) {
		    if (!result.equals("")) {
			result += ",";
		    }
		    result += new String("" + min); 
		    min = max; 
		    interval = -1;
		} else {
		    if (!result.equals("")) {
			result += ",";
		    }
		    result += new String("" + min + "-" + max); 
		    if (interval > 1) {
			result += new String(":" + interval);
		    }
		    min = interval = max = -1;
		}
	    }
	}   
	// handle the leftover
	if (min != -1) {
	    if (!result.equals("")) {
		result += ",";
	    }
	    result += new String("" + min); 
	}
	if (max != -1) {
	    if (!result.equals("")) {
		result += ",";
	    }
	    result += new String("" + max); 
	}
	return result;
    }   

    public int nextElement()
    {
	if (pre == null) {
	    pre = head;
	} else {
	    pre = pre.next;
	}
	  
	if (pre == null) {
	    return -1;
	} else {
	    return pre.data;   
	}
    }   

    private Link nextLink()
    {
	if (pre == null) {
	    return head;
	} else {
	    return pre.next;
	}
    }   
    
    public void printList()
    {
	reset();
	while (hasMoreElements()) {
	    System.out.println("" + nextElement());
	}
	reset();
    }   

    public void removeAll()
    { 
	len = 0;
	head = null;
	tail = null;
	pre = null;
    }   

    public void reset()
    {
	pre = null;
    }   

    public int size()
    {
	return len;
    }   
}
