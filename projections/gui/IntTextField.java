package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class IntTextField extends TextField
   implements TextListener
{
   private String lastValue;
   private int    lastCaretPosition;
   
   public IntTextField(int defval, int size)
   {
      super("" + defval, size);
      addTextListener(this);
      addKeyListener(new KeyAdapter()
      {
         public void keyTyped(KeyEvent evt)
         {
            char ch = evt.getKeyChar();
            if(!('0' <= ch && ch <= '9' || Character.isISOControl(ch)))
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
      try
      {
         Integer.parseInt(getText().trim() + "0");
         lastValue = getText();
      }
      catch(NumberFormatException e)
      {
         setText(lastValue);
         setCaretPosition(lastCaretPosition);
      }
   }
   
   public int getValue()
   {
      checkValue();
      try
      {
         return Integer.parseInt(getText().trim());
      }
      catch(NumberFormatException e)
      {
         return 0;
      }
   }
}         
                                       
                
