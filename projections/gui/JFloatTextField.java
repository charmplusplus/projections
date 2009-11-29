package projections.gui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.TextEvent;

import javax.swing.JTextField;

public class JFloatTextField extends JTextField
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String lastValue;
    int    lastCaretPosition;
    
    public JFloatTextField(float defval, int size)
    {
	super("" + defval, size);
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

    public boolean isValueValid() {
	try {
	    Float.valueOf(getText().trim() + "0");
	    return true;
	} catch (NumberFormatException e) {
	    return false;
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
