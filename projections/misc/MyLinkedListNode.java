package projections.misc;

import java.lang.Object;

public class MyLinkedListNode
{
Object Data;
MyLinkedListNode Next;

public MyLinkedListNode ()
{
	Next = null;
}

public void pushfront (Object O)
{
	MyLinkedListNode P;

	P = new MyLinkedListNode ();
	P.Data = O;
	P.Next = Next;
	Next = P;
}

public void pushrear (Object O)
{
	MyLinkedListNode P, Q;

	Q = new MyLinkedListNode ();
	Q.Data = O;
	Q.Next = null;

	if (Next == null)
		Next = Q;
	else
		{
			P = Next;
			while (P.Next != null)
				P = P.Next;
			P.Next = Q;
		}
}

public Object popfront ()
{
	Object P;

	P = Next.Data;
	Next = Next.Next;
	return P;
}

public Object poprear ()
{
	return null;
}

public MyLinkedListNode getnext ()
{
	return Next;
}

public Object getdata ()
{
	return Data;
}

}

