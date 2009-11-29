package projections.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class ProfileYLabelCanvas extends Canvas
{

FontMetrics fm;
   String text = "%";
   
   public ProfileYLabelCanvas()
   {
	  setBackground(Color.black);
	  setForeground(Color.white);
	  setFont(new Font("SansSerif", Font.PLAIN, 10));
   }   
   public Dimension getMinimumSize()
   {
	  return getPreferredSize();
   }   
   public Dimension getPreferredSize()
   {
	  int w = 0;
	  int h = 0;
	  
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
			fm = g.getFontMetrics(g.getFont());
	  }
	  
	  if(fm != null)
	  {
		 w = fm.stringWidth(text) + 20;
		 h = fm.getHeight() + 20;      
	  }
	  
	  return new Dimension(w, h);
   }   
   public void paint(Graphics g)
   {
	  int w = getSize().width;
	  int h = getSize().height;
	  
	  if(fm == null)
		 fm = g.getFontMetrics(g.getFont());
	  
	  g.setColor(getForeground());
	  g.drawString(text, (w-fm.stringWidth(text))/2, (h+fm.getHeight())/2);
   }   
   public void print(Graphics pg)
   {
	  setBackground(Color.white);
	  setForeground(Color.black);
	  
	  paint(pg);
	  
	  setBackground(Color.black);
	  setForeground(Color.white);
   }   
}