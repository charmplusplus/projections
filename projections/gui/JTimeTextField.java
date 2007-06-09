package projections.gui;
import javax.swing.*;

/** This class is meant to be used by any dialog taking time interval as input
*   It validates the input data to be in us, ms or s
*   Using JTextField instead of TextField as in TimeTextField 
*   Uses Swing Input Verifier class(available from java 1.3) instead of KeyListener for validating input
*   java 1.4 has better textfield validation
*/

public class JTimeTextField extends JTextField
{   
    public JTimeTextField(long defval, int size)
    {
	this(U.t(defval),size);
    }   
    
    public JTimeTextField(String defval, int size)
    {
	super(defval, size);
	setInputVerifier(new TimeFieldVerifier());
    }
    
    public long getValue(){
	return U.fromT(getText());
    }   
    
    public void setValue(long time) {
	setText(String.valueOf(time));
    }

    public void setText(String text) {
	super.setText(U.t(U.fromT(text)));
    }   
    
    /* if the data entered in the text field is of invalid format
     *  then focus cannot be transferred to another component */
    // this input is passed to 'U' class later which interprets any 
    // other illegal value as 0

    class TimeFieldVerifier extends InputVerifier {
	public boolean verify(JComponent input) {
	    JTextField tf = (JTextField) input;
	    String time = tf.getText();
	    for (int i=0; i<time.length(); i++) {
		char ch = time.charAt(i);
		if(!('0' <= ch && ch <= '9' || 
		     ch=='s' || ch=='m' || ch=='u' || 
		     ch=='.' || ch==' ' || Character.isISOControl(ch)))
		    return false;
	    }
	    return true;
	}
    }
}
