package projections.gui;

public class OrderedUsageList
{
	private Link head;
//	private Link tail;
	private Link pre;
	private int len;
	class Link
	{
		float usage;
		Link next;
		private Link(float u, Link n) {usage = u; next = n;}
	}   


	public void insert(float u)
	{
		Link newLink;
		reset();

		Link tmp = nextLink();
		while(tmp != null && tmp.usage > u)
		{
			pre = tmp;
			tmp = nextLink();
		}

		if(tmp == null)
		{
			newLink = new Link(u,  tmp);
			if(head == null)
			{
				head = newLink;
//				tail = newLink;
			}
			else
			{
				pre.next = newLink;
//				tail = newLink;
			}
			len++;
		}
		else
		{      
			newLink = new Link(u, tmp);
			if(head == tmp)
				head = newLink;
			else
				pre.next = newLink;
			len++;       
		}       
	}   
//	public int insert2(float u, int e, int[] DisplayOrder, int listsize)
//	{
//		int DisplayIndex = 0;
//		Link curr = head2;
//		Link prev = null;
//		while (DisplayIndex < listsize)
//		{ 
//			if ((curr != null) && (curr.entry == DisplayOrder[DisplayIndex]))
//			{ prev = curr;
//			curr = curr.next;
//			}
//			else if (e == DisplayOrder[DisplayIndex])
//			{
//				len++;
//				Link newLink;
//				if (head2 == null)
//				{ 
//					newLink = new Link(u,e, null);
//					head2 = newLink;
//					tail2 = newLink;
//				}
//				else if (curr == null) 
//				{
//					newLink = new Link(u,e, null);
//					prev.next = newLink;
//					tail2  = newLink;
//				}	      
//				else if (head2 == curr)
//				{
//					newLink = new Link(u, e, head2);
//					newLink.next = head2;
//					head2 = newLink;
//				}
//
//				else
//				{
//					newLink = new Link(u, e, curr);
//					newLink.next = curr;
//					prev.next = newLink;
//				}
//				return 1;
//			}
//			DisplayIndex++;
//		}
//		return -1;
//	}
	
//	public void nextElement()
//	{
//		if(pre == null) 
//			pre = head;
//		else
//			pre = pre.next; 
//	}   
	private Link nextLink()
	{
		if(pre == null)
			return head;
		else
			return pre.next;
	}   
//	public void removeAll()
//	{ 
//		pre  = null;
//		head = null;
//		tail = null;
//		len = 0;
//	}   
	private void reset()
	{
		pre = null;
	}   
//	public int size()
//	{
//		return len;
//	}   
}