package projections.gui;

import java.awt.*;

public class ProfileLabelCanvas2 extends Canvas
{
   FontMetrics fm;
   String text = "Processor Number";
   
   public ProfileLabelCanvas2()
   {
      setBackground(Color.black);
      setForeground(Color.white);
      setFont(new Font("SansSerif", Font.PLAIN, 10));
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
   
   public Dimension getMinimumSize()
   {
      return getPreferredSize();
   }           
   
   
   
   public void print(Graphics pg)
   {
      setBackground(Color.white);
      setForeground(Color.black);
      
      paint(pg);
      
      setBackground(Color.black);
      setForeground(Color.white);
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
      
}

