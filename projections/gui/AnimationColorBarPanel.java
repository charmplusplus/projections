package projections.gui;

import java.awt.*;

public class AnimationColorBarPanel extends Panel
{
   private float MAXHUE = (float)0.65;
   private FontMetrics fm;
   private int textwidth0;
   private int textwidth100;
   private int textheight;
   
   public AnimationColorBarPanel()
   {
      setBackground(Color.black);
      setForeground(Color.white);
   }
   
   public Dimension getPreferredSize()
   {
      int h = 60;
      int w = getSize().width;
      
      return new Dimension(w, h);
   }
   
   public Dimension getMinimumSize()
   {
      return getPreferredSize();
   }      
   
   public void paint(Graphics g)
   {
      if(fm == null)
      {
         fm = g.getFontMetrics(g.getFont());
         textheight = fm.getHeight();
         textwidth0 = fm.stringWidth("0%");
         textwidth100 = fm.stringWidth("100%");
      }   
      
      float H = MAXHUE;
      int width = getSize().width;
      int height = getSize().height;
      
      g.setColor(Color.white);
      g.drawString("0%",   20, (height+textheight)/2);
      g.drawString("100%", width-20-textwidth100, (height+textheight)/2);
      
      int cbwidth  = (int)(width - textwidth0 - textwidth100 - 60);
      int cbheight = 20;
      
      float deltaH = (float)(H/cbwidth);
      
      int leftoffset = 30 + textwidth0;
      int topoffset  = (height - cbheight)/2;
      
      for(int i=0; i<cbwidth; i++)
      {               
         g.setColor(Color.getHSBColor(H, (float)0.7, (float)1.0));
         H -= deltaH;
         g.drawLine(leftoffset+i, topoffset, leftoffset+i, topoffset+cbheight);
      }   
   }
}   
