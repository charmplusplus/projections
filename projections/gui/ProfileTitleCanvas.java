package projections.gui;

import java.awt.*;

public class ProfileTitleCanvas extends Canvas
{
   FontMetrics fm;
   String text1 = " ";
   String text2 = " ";
   
   public ProfileTitleCanvas()
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
		 int sw1 = fm.stringWidth(text1);
		 int sw2 = fm.stringWidth(text2);
		 if(sw1 > sw2)
			w = sw1 + 20;
		 else
			w = sw2 + 20;
			
		 h = fm.getHeight() * 2 + 24;      
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
	  g.drawString(text1, (w-fm.stringWidth(text1))/2, (h/2 - 2));
	  g.drawString(text2, (w-fm.stringWidth(text2))/2, (h/2 + fm.getHeight() + 2));
   }   
   public void print(Graphics pg)
   {
	  setBackground(Color.white);
	  setForeground(Color.black);
	  
	  paint(pg);
	  
	  setBackground(Color.black);
	  setForeground(Color.white);
   }   
   public void setText1(String s)
   {
	  text1 = s;
   }   
   public void setText2(String s)
   {
	  text2 = s;
   }   
}