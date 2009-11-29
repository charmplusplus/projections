package projections.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Panel;

class GrayLWPanel extends Panel
{
 
public void paint(Graphics g)
   {
	  g.setColor(Color.lightGray);
	  g.fillRect(0, 0, getSize().width, getSize().height);
		 
	  g.setColor(Color.black);
	  g.drawRect(0, 0, getSize().width-1, getSize().height-1);
	  
	  super.paint(g);
   }   
}