package projections.gui;

public class OrderedIntList
{
   private Link head;
   private Link tail;
   private Link pre;
   private int len;
   
   public void reset()
   {
      pre = null;
   }
   
   public OrderedIntList copyOf()
   {
      OrderedIntList listcopy = new OrderedIntList();
      reset();
      int e;
      while((e = nextElement()) != -1)
         listcopy.insert(e);
      
      return listcopy;
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
      while((e = nextElement()) != -1)
      {
         if(otherlist.nextElement() != e)
            return false;
      }
      
      return true;
   }            
   
   public boolean hasMoreElements()
   {
      return nextLink() != null;
   }
   
   public int nextElement()
   {
      if(pre == null) 
         pre = head;
      else
         pre = pre.next;
      
      if(pre == null)
         return -1;
      else
         return pre.data;   
   }
   
   public int currentElement()
   {
      Link cur = nextLink();
      if(cur == null)
         return -1;
      else
         return cur.data;   
   }
   
   public void insert(int n)
   {
      Link newLink;
      reset();
      
      Link tmp = nextLink();
      while(tmp != null && tmp.data < n)
      {
         pre = tmp;
         tmp = nextLink();
      }
      
      
      if(tmp == null)
      {
         newLink = new Link(n, tmp);
         if(head == null)
         {
            head = newLink;
            tail = newLink;
         }
         else
         {
            pre.next = newLink;
            tail = newLink;
         }
         len++;
      }
      else if(tmp.data != n)
      {      
         newLink = new Link(n, tmp);
         if(head == tmp)
            head = newLink;
         else
            pre.next = newLink;
         len++;      
      }       
   }
   
   public int size()
   {
      return len;
   }
   
   
   public void removeAll()
   { 
      len = 0;
      head = null;
      tail = null;
      pre = null;
   }
      
      
   public void printList()
   {
      reset();
      while(hasMoreElements())
      {
         System.out.println("" + nextElement());
      }
   }      
      
   public String listToString()
   {
      reset();
      int min, max, tmp;
      tmp = nextElement();
      
      String result = "";
      
      while(tmp != -1)
      {
         min = tmp;
         while((currentElement() - tmp) == 1)
         {
            tmp = nextElement();
         }
         
         max = tmp;
         tmp = nextElement();

         if(!result.equals(""))
            result += ",";
         if(min == max)
            result += min;
         else
            result += new String("" + min + "-" + max); 
      }   
      return result;
   }   
         
         
   private Link nextLink()
   {
      if(pre == null)
         return head;
      else
         return pre.next;
   }
   
   class Link
   {
      int data;
      Link next;
      Link(int d, Link n) {data = d; next = n;}
   }  
}                                                   


             
