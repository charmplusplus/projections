package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class SelectField extends TextField
   implements TextListener
{
   private String lastValue;
   private int    lastCaretPosition;
   
   public SelectField(String defval, int size)
   {
      super(defval, size);
      addTextListener(this);
      addKeyListener(new KeyAdapter()
      {
         public void keyTyped(KeyEvent evt)
         {
            char ch = evt.getKeyChar();
            if(!(('0' <= ch && ch <= '9') || ch=='-' || ch==',' || Character.isISOControl(ch)))
               evt.consume();
            else
               lastCaretPosition = getCaretPosition();
         }
      });
      lastValue = "" + defval;
   }
   
   public void textValueChanged(TextEvent evt)
   {
      checkValue();
   }
   
   private void checkValue()
   {
      String tmp = getText();
      int l = tmp.length();
      
      if(l > 0)
      {
         char c = tmp.charAt(l-1);
         if(l==1)
         {
            if(!(c >= '0' && c <= '9'))
            {
               setText(lastValue);
               setCaretPosition(lastCaretPosition);
            }
         }
         else
         {
            char d = tmp.charAt(l-2);   
            if(c=='-' || c==',')
            {
               if(!(d >= '0' && d <= '9'))
               {
                  setText(lastValue);
                  setCaretPosition(lastCaretPosition);
               }
               else if(c=='-' && 
                      (tmp.substring(0,l-1).lastIndexOf("-") > tmp.substring(0,l-1).lastIndexOf(",")))
               {
                  setText(lastValue);
                  setCaretPosition(lastCaretPosition);
               }
            }        
         }     
      }
 
      lastValue = tmp; 
   }                   
                     
   
   public OrderedIntList getValue(int limit)
   {  
      limit--;

      OrderedIntList tmpList = new OrderedIntList();

      String tmp = cleanItUp(getText());
      
      if(tmp.length()==0) tmp ="0";
      
      try
      {
         char c = tmp.charAt(tmp.length()-1);
         while(!(c >= '0' && c <= '9'))
         {
            tmp = tmp.substring(0, tmp.length()-1);
            c = tmp.charAt(tmp.length()-1);
         }   
      
         int i = 0;
         int low = 0;
         int high = 0;
         int min = 0;
         int max = 0;
   
         String result = "";
     
         boolean OK = true;
         while(OK)
         {
            while(i < tmp.length() && tmp.charAt(i) >= '0' && tmp.charAt(i) <= '9')
               i++;
            high = i;
            min = Integer.parseInt(tmp.substring(low, high));
         
            if(i == tmp.length())
            {
               max = min;
               OK = false;
            }
            else if(tmp.charAt(i) == ',')
            {
               max = min;
               i++;
               low = i;
            }
            else if(tmp.charAt(i) == '-')
            {
               i++;
               low = i;
               while(i < tmp.length() && tmp.charAt(i) >= '0' && tmp.charAt(i) <= '9')
                  i++;
               high = i;
               max = Integer.parseInt(tmp.substring(low, high));
            
               if(i == tmp.length())
               {
                  OK = false;
               }
               else
               {
                  i++;
                  low = i; 
               }   
            }
            
            if(min <= limit && max <= limit && min <= max)
            {
               for(int j=min; j<=max; j++)
                  tmpList.insert(j);
            }               
         } 
      }
      catch(NumberFormatException e)
      {
         tmpList.removeAll();
         tmpList.insert(0);
      }
      catch(StringIndexOutOfBoundsException e)
      {
         tmpList.removeAll();
         tmpList.insert(0);
      }   
      if(tmpList.size()==0)
         tmpList.insert(0);
      
      lastValue = tmpList.listToString();
      setText(lastValue); 
      
      return tmpList;
   }
   
   private String cleanItUp(String old)
   {
      String tmp = "";
      char c;
      int len = old.length();
      for(int i=0; i<len; i++)
      {
         c = old.charAt(i);
         if((c >= '0' && c <= '9') || c == '-' || c == ',')
            tmp += new String("" + c);
      }
      return tmp;
   }
}         
                                       
                
