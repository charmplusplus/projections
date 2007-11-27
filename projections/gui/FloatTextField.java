package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class FloatTextField extends TextField
    implements TextListener
{

	private String lastValue;
    int    lastCaretPosition;
    
    public FloatTextField(float defval, int size)
    {
	super("" + defval, size);
	addTextListener(this);
	addKeyListener(new KeyAdapter() 
	    {
		public void keyTyped(KeyEvent evt)
		{
		    char ch = evt.getKeyChar();
		    if(!('0' <= ch && ch <= '9' || ch == '.' || Character.isISOControl(ch)))
			evt.consume();
		    else
			lastCaretPosition = getCaretPosition();
		}
	    });
	lastValue = "" + defval;
    }

    private void checkValue()
    {
	try {
	    Float.valueOf(getText().trim() + "0");
	    lastValue = getText();
	} catch(NumberFormatException e) {
	    setText(lastValue);
	    setCaretPosition(lastCaretPosition);
	}
    }   

    public float getValue()
    {
	checkValue();
	try {
	    return (Float.valueOf(getText().trim())).floatValue();
	} catch(NumberFormatException e) {
	    return (Float.valueOf(lastValue)).floatValue();
	}
    }   

    public void textValueChanged(TextEvent evt) {
	checkValue();
    }   
}
