package projections.gui;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
/** This class is used for the input of processor range
*   validates for proper range */

class JSelectField extends JTextField
{

	private String lastValue;
	//   private int    lastCaretPosition;
	public RangeVerifier rangeVerifier;
   
   protected JSelectField(String defval, int size)
   {
	  super(defval, size);
	  rangeVerifier =  new RangeVerifier();
	  setInputVerifier(rangeVerifier);  
	  lastValue = "" + defval;
   } 
//  
//   private void checkValue()
//   {
//	  String tmp = getText();
//	  int l = tmp.length();
//	  
//	  if(l > 0)
//	  {
//		 char c = tmp.charAt(l-1);
//		 if(l==1)
//		 {
//			if(!(c >= '0' && c <= '9'))
//			{
//			   setText(lastValue);
//			   setCaretPosition(lastCaretPosition);
//			}
//		 }
//		 else
//		 {
//			char d = tmp.charAt(l-2);   
//			if(c=='-' || c==',' || c==':')
//			{
//			   if(!(d >= '0' && d <= '9'))
//			   {
//				  setText(lastValue);
//				  setCaretPosition(lastCaretPosition);
//			   }
//			   else if(c=='-' && 
//					  (tmp.substring(0,l-1).lastIndexOf("-") > tmp.substring(0,l-1).lastIndexOf(",")))
//			   {
//				  setText(lastValue);
//				  setCaretPosition(lastCaretPosition);
//			   }
//                           else if (c==':' && 
//					  (tmp.substring(0,l-1).lastIndexOf("-") < tmp.substring(0,l-1).lastIndexOf(",")))
//			   {
//				  setText(lastValue);
//				  setCaretPosition(lastCaretPosition);
//			   }
//			}        
//		 }     
//	  }
// 
//	  lastValue = tmp; 
//   }   
   private String cleanItUp(String old)
   {
	  String tmp = "";
	  char c;
	  int len = old.length();
	  for(int i=0; i<len; i++)
	  {
		 c = old.charAt(i);
		 if((c >= '0' && c <= '9') || c == '-' || c == ',' || c == ':')
			tmp += new String("" + c);
	  }
	  return tmp;
   }   

    private OrderedIntList getValue(int limit)
    {  
	limit--;

	OrderedIntList tmpList = new OrderedIntList();

	String tmp = cleanItUp(getText());
	  
	if (tmp.length()==0) 
	    tmp ="0";
	try {
	    char c = tmp.charAt(tmp.length()-1);
	    while (!(c >= '0' && c <= '9')) {
		tmp = tmp.substring(0, tmp.length()-1);
		c = tmp.charAt(tmp.length()-1);
	    }   
	    int i = 0;
	    int low = 0;
	    int high = 0;
	    int min = 0;
	    int max = 0;
	    int interval = 1;
   
	    boolean notDone = true;
	    while (notDone) {
		while (i < tmp.length() && 
		       tmp.charAt(i) >= '0' && 
		       tmp.charAt(i) <= '9')
		    i++;
		high = i;
		min = Integer.parseInt(tmp.substring(low, high));
		if (i == tmp.length()) {
		    interval = 1;
		    max = min;
		    notDone = false;
		} else if (tmp.charAt(i) == ',') {
		    interval = 1;
		    max = min;
		    i++;
		    low = i;
		} else if (tmp.charAt(i) == '-') {
		    i++;
		    low = i;
		    while (i < tmp.length() && 
			   tmp.charAt(i) >= '0' && 
			   tmp.charAt(i) <= '9')
			i++;
		    high = i;
		    max = Integer.parseInt(tmp.substring(low, high));

		    // get interval (skip factor)
		    interval=1;
		    if (i<tmp.length() && tmp.charAt(i)== ':') {
			i++;
			low = i;
			while (i < tmp.length() && 
			       tmp.charAt(i) >= '0' && 
			       tmp.charAt(i) <= '9')
			    i++;
			high = i;
			interval = Integer.parseInt(tmp.substring(low, high));
		    }
		    if (i == tmp.length()) {
			notDone = false;
		    } else if (tmp.charAt(i) == ',') {
			i++;
			low = i; 
		    } else {
			System.err.println("Badly formed pelist - should " +
					   "not happen! Please contact " +
					   "projections developers!");
			System.exit(-1);
		    }
		}
		for (int j=min; j<=max; j+=interval) {
		    if (j <= limit) 
			tmpList.insert(j);
		}
	    } 
	} catch(NumberFormatException e) {
	    tmpList.removeAll();
	    tmpList.insert(0);
	} catch(StringIndexOutOfBoundsException e) {
	    tmpList.removeAll();
	    tmpList.insert(0);
	}   
	if (tmpList.size()==0)
	    tmpList.insert(0);
	  
	lastValue = tmpList.listToString();
	setText(lastValue); 

	return tmpList;
    }   
    

    public OrderedIntList getValue() {
 	   return getValue(Integer.MAX_VALUE);
    }  

    /* verify if the input characters are valid or not */

    class RangeVerifier extends InputVerifier { // NO_UCD
    	public boolean verify(JComponent input) {
    		JTextField tf = (JTextField) input;
    		String procRange = tf.getText();
    		for(int i=0; i<procRange.length(); i++)
    		{
    			char ch = procRange.charAt(i);
    			if(!(('0' <= ch && ch <= '9') || ch=='-' || ch==',' || ch==':' || Character.isISOControl(ch)))
    				return false;
    		}
    		return true;
    	}
   }

}
