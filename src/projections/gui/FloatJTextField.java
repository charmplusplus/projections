package projections.gui;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A swing version of FloatTextField
 * 
 */
public class FloatJTextField extends JTextField
implements DocumentListener
{

	private String lastValue;

	public FloatJTextField(float defval, int size)
	{
		super("" + defval, size);
		this.getDocument().addDocumentListener(this);
	}

	private void checkValue()
	{
		try {
			Float.valueOf(getText().trim() + "0");
			lastValue = getText();
		} catch(NumberFormatException e) {
			setText(lastValue);
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

	public void changedUpdate(DocumentEvent e) {
		checkValue();
	}

	public void insertUpdate(DocumentEvent e) {
		checkValue();
	}

	public void removeUpdate(DocumentEvent e) {
		checkValue();
	}   
}
