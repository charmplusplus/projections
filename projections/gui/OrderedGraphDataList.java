package projections.gui;

import java.awt.*;

public class OrderedGraphDataList
{
   private Link head;
   private Link tail;
   private Link pre;
   private int len;
   
   public void reset()
   {
      pre = null;
   }
   
   public boolean hasMoreElements()
   {
      return nextLink() != null;
   }
   
   public void nextElement()
   {
      if(pre == null) 
         pre = head;
      else
         pre = pre.next; 
   }
   
   public int currentY()
   {
      Link cur = nextLink();
      if(cur == null)
         return -1;
      else 
         return cur.height;
   }
   
   public Color currentC()
   {
      Link cur = nextLink();
      if(cur == null)
         return null;
      else
         return cur.color;
   }                  
   
   public void insert(int y, Color c)
   {
      Link newLink;
      reset();
      
      Link tmp = nextLink();
      while(tmp != null && tmp.height < y)
      {
         pre = tmp;
         tmp = nextLink();
      }
      
      
      
      if(tmp == null)
      {
         newLink = new Link(y, c, tmp);
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
      else
      {      
         if(y == tmp.height)
         {
            tmp.color = c;
         }   
         else
         {
            newLink = new Link(y, c, tmp);
            if(head == tmp)
               head = newLink;
            else
               pre.next = newLink;
            len++;      
         }   
      }       
   }
   
   public int size()
   {
      return len;
   }
   
   
   public void removeAll()
   { 
 /*     Link next = null;
      pre = head;
      while(pre != null)
      {
         next = pre.next;
         pre.color  = null;
         pre.next   = null;
         pre = next;
      }      
  */
      pre  = null;
      head = null;
      tail = null;
      len = 0;
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
      int height;
      Color color;
      Link next;
      Link(int h, Color c, Link n) {height = h; color = c; next = n;}
   }   
}                                                   


              
