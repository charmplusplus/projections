package projections.misc;

import java.lang.Object;

public class MyLinkedList
{

int Length;
MyLinkedListNode Head, Current;

public MyLinkedList ()
{
	Length = 0;
	Head = new MyLinkedListNode ();
	Current = Head;
}

public MyLinkedListNode next ()
{
	return Current = Current.getnext ();
}

public Object data ()
{
	return Current.getdata ();
}

public void rewind ()
{
	Current = Head;
}

public void pushrear (Object O)
{
	Length++;
	Head.pushrear (O);
}

public int length ()
{
	return Length;
}

}

