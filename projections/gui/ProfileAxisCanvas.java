package projections.gui;

import java.awt.*;

public class ProfileAxisCanvas extends Canvas
{
   private ProfileData data;
   private Image offscreen;
   private FontMetrics fm;
   private int textheight;
   
   public ProfileAxisCanvas(ProfileData data)
   {
      this.data = data;
      setForeground(Color.white);
      setBackground(Color.black);
   }      

   public void print(Graphics pg)
   {
      setBackground(Color.white);
      setForeground(Color.black);
      
      int vsbval = data.profileWindow.getVSBValue();
      
      pg.translate(0,-vsbval);
      
      drawAxis(pg);
      
      setBackground(Color.black);
      setForeground(Color.white);
   }
      
   public void makeNewImage()
   {
      offscreen = null;
      if(data.plist == null)
         return;
      
      int w = getSize().width;
      
      if(w > 0 && data.dch > 0)
      {
         offscreen = createImage(w, data.dch+30);

         if(offscreen == null)
            return;
         
         Graphics og = offscreen.getGraphics();
         og.setClip(0, 0, w, data.dch);
         
         drawAxis(og);
         
         og.dispose();
         repaint(); 
      }
   }      

   private void drawAxis(Graphics g)
   {  
      int w = getSize().width;
      
      if(fm == null)
      {
         fm = g.getFontMetrics(g.getFont());
         textheight = fm.getHeight();
      }        
      
      g.setColor(getForeground());
      g.drawLine(w-1, data.offset, w-1, data.dch);
     
      float deltay = (float)((data.dch - data.offset) / 100.0);
      int labelincrement = (int)(Math.ceil(textheight / deltay));
     
      int[] indices = {1, 2, 5, 25};
      int best = -1;
      for(int i=0; i<indices.length; i++)
      {   
         int t=0;
         int sum=0;
         while((sum = (int)(indices[i] * Math.pow(10,t))) < labelincrement)
            t++;
         if((sum-labelincrement) < (best-labelincrement) || best < 0)
            best = sum;
      } 
      
      labelincrement = best; 
      
      int cury;
      String tmp;
      for(int y=0; y<=100; y++)
      {
         cury = data.dch - (int)(y * deltay)-1; 
         
         if(y % labelincrement == 0)
         {  
            tmp = "" + y;
            g.drawLine(w-10, cury, w, cury);
            cury += (int)(0.5*textheight); 
            if(y != 0)
               g.drawString(tmp, w-15-fm.stringWidth(tmp), cury);
         }
         else
         {
            g.drawLine(w-7, cury, w, cury);
         }
      }
   }                   
      
   public void update(Graphics g)
   {
      paint(g);
   }
   
   public void paint(Graphics g)
   {
      if(offscreen != null)
      {
         int y = data.profileWindow.getVSBValue();
         int w = getSize().width;
      
         g.drawImage(offscreen, 0,0, w, data.vph, 
                                0,y, w, data.vph+y, null);
      }                       
   }                                    
}
   
