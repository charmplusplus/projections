package projections.gui;

import java.awt.*;
import java.awt.event.*;
/** Validates Time input fields
* @deprecated As a part of conversion to Swing .... Use JTimeTextField instead */

public class TimeTextField extends TextField
   implements TextListener
{   
   public TimeTextField(long defval, int size)
   {
	   this(U.t(defval),size);
   }   
   public TimeTextField(String defval, int size)
   {
	  super(defval, size);
	  addTextListener(this);
	  addKeyListener(new KeyAdapter()
	  {
		 public void keyTyped(KeyEvent evt)
		 {
			char ch = evt.getKeyChar();
			if(!('0' <= ch && ch <= '9' || 
				ch=='s' || ch=='m' || ch=='u' || ch=='.' || ch==' ' ||
				Character.isISOControl(ch)))
			   evt.consume();
		 }
	  });
   }   
   public long getValue()
   {
		return U.fromT(getText());
   }   
    
    public void setValue(long time) {
	setText(String.valueOf(time));
    }

   public void setText(String text) {
		super.setText(U.t(U.fromT(text)));
   }   
   public void textValueChanged(TextEvent evt) {}   
}
