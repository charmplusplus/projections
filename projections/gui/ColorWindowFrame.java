package projections.gui;

import java.awt.*;

public class ColorWindowFrame extends Dialog
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public ColorWindowFrame(Frame parent){
	super(parent, false);		// default non (*CW*) Modal dialogbox
   }
   
   public ColorWindowFrame(Frame parent, boolean modal){
        super(parent, modal);
   }

   public void applyNewColor(Color c) {}   
}
