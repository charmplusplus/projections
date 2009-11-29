package projections.gui;

import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;

public class IntTextField extends TextField
   implements TextListener
{


private String lastValue;
   int    lastCaretPosition;
   
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

    // public void setText(String text) - implemented by superclass

    /**
     *  Sets a value in the text box.
     */
    public void setValue(int num) {
	lastValue = getText();
	setText(String.valueOf(num));
	checkValue();
    }

   public void textValueChanged(TextEvent evt)
   {
	  checkValue();
   }   
}
