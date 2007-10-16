package projections.gui;

import java.awt.*;

class GrayPanel extends Panel
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
public GrayPanel()
   {
	  setBackground(Color.lightGray);
   }   
   public void paint(Graphics g)
   {
	  g.setColor(Color.black);
	  g.drawRect(0, 0, getSize().width-1, getSize().height-1);
	  
	  super.paint(g);
   }   
}